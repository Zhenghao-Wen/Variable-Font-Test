package moe.echo.variablefonttest_n

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 轻量级 TTF / OTF / TTC 字体元数据解析器。
 * 解析 name table（名称与版权）、fvar table（可变轴）、GSUB/GPOS（OT 特性）。
 * 仅做只读解析，不依赖任何第三方库。
 */
object FontMetadataParser {

    data class NameInfo(
        val fullName: String = "",
        val fontFamily: String = "",
        val fontSubfamily: String = "",
        val uniqueId: String = "",
        val postScriptName: String = "",
        val version: String = "",
        val manufacturer: String = "",
        val designer: String = "",
        val copyright: String = ""
    )

    data class AxisInfo(
        val tag: String,
        val name: String,
        val min: Float,
        val default: Float,
        val max: Float
    )

    data class Metadata(
        val nameInfo: NameInfo,
        val axes: List<AxisInfo>,
        val features: List<String>
    )

    // ──────────────────────────────────────────────
    //  公开入口
    // ──────────────────────────────────────────────

    /**
     * 解析字体元数据。
     * @param inputStream 字体文件输入流（TTF / OTF / TTC）
     * @param ttcIndex    TTC 集合中的字体索引（非 TTC 文件忽略此参数）
     */
    fun parse(inputStream: InputStream, ttcIndex: Int = 0): Metadata {
        val bytes = inputStream.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

        // 判断 TTF/OTF 还是 TTC
        val sfVersion = buf.getInt(0)
        val fontOffset = if (sfVersion == 0x74746366) { // "ttcf"
            val numFonts = buf.getInt(8)
            val idx = ttcIndex.coerceIn(0, numFonts - 1)
            buf.getInt(12 + idx * 4)
        } else {
            0
        }

        // 读取 Table Directory
        val numTables = buf.getShort(fontOffset + 4).toInt() and 0xFFFF
        val tables = mutableMapOf<String, Pair<Int, Int>>() // tag → (offset, length)
        for (i in 0 until numTables) {
            val entry = fontOffset + 12 + i * 16
            if (entry + 16 > buf.limit()) break
            val tag = readTag(buf, entry)
            val off = buf.getInt(entry + 8)
            val len = buf.getInt(entry + 12)
            tables[tag] = off to len
        }

        // 解析各 table
        val nameMap = tables["name"]?.let { parseNameTable(buf, it.first) } ?: emptyMap()
        val axes = tables["fvar"]?.let { parseFvarTable(buf, it.first, nameMap) } ?: emptyList()
        val features = mutableSetOf<String>()
        tables["GSUB"]?.let { parseFeatureList(buf, it.first, features) }
        tables["GPOS"]?.let { parseFeatureList(buf, it.first, features) }

        return Metadata(
            nameInfo = NameInfo(
                fullName = nameMap[4] ?: "",
                fontFamily = nameMap[1] ?: "",
                fontSubfamily = nameMap[2] ?: "",
                uniqueId = nameMap[3] ?: "",
                postScriptName = nameMap[6] ?: "",
                version = nameMap[5] ?: "",
                manufacturer = nameMap[8] ?: "",
                designer = nameMap[9] ?: "",
                copyright = nameMap[0] ?: ""
            ),
            axes = axes,
            features = features.sorted()
        )
    }

    // ──────────────────────────────────────────────
    //  name table（英文优先，两遍扫描）
    // ──────────────────────────────────────────────

    private fun parseNameTable(buf: ByteBuffer, offset: Int): Map<Int, String> {
        val english = mutableMapOf<Int, String>()
        val other = mutableMapOf<Int, String>()
        try {
            val count = buf.getShort(offset + 2).toInt() and 0xFFFF
            val stringBase = offset + (buf.getShort(offset + 4).toInt() and 0xFFFF)

            for (i in 0 until count) {
                val rec = offset + 6 + i * 12
                if (rec + 12 > buf.limit()) break
                val platformID = buf.getShort(rec).toInt() and 0xFFFF
                val encodingID = buf.getShort(rec + 2).toInt() and 0xFFFF
                val languageID = buf.getShort(rec + 4).toInt() and 0xFFFF
                val nameID = buf.getShort(rec + 6).toInt() and 0xFFFF
                val length = buf.getShort(rec + 8).toInt() and 0xFFFF
                val strOff = buf.getShort(rec + 10).toInt() and 0xFFFF

                val pos = stringBase + strOff
                if (pos + length > buf.limit()) continue
                val raw = ByteArray(length)
                for (j in 0 until length) raw[j] = buf.get(pos + j)

                when {
                    // Windows Unicode BMP → UTF-16BE
                    platformID == 3 && encodingID == 1 -> {
                        val s = String(raw, Charsets.UTF_16BE)
                        if (languageID == 0x0409) english[nameID] = s   // en-US 优先
                        else if (nameID !in other) other[nameID] = s
                    }
                    // Unicode platform → UTF-16BE
                    platformID == 0 -> {
                        if (nameID !in english && nameID !in other)
                            other[nameID] = String(raw, Charsets.UTF_16BE)
                    }
                    // Mac Roman → Latin-1
                    platformID == 1 -> {
                        if (nameID !in english && nameID !in other)
                            other[nameID] = String(raw, Charsets.ISO_8859_1)
                    }
                }
            }
        } catch (_: Exception) { }

        val result = other.toMutableMap()
        result.putAll(english) // 英文覆盖
        return result
    }

    // ──────────────────────────────────────────────
    //  fvar table（可变轴）
    // ──────────────────────────────────────────────

    private fun parseFvarTable(
        buf: ByteBuffer, offset: Int, nameMap: Map<Int, String>
    ): List<AxisInfo> {
        val axes = mutableListOf<AxisInfo>()
        try {
            val axesArrOff = offset + (buf.getShort(offset + 4).toInt() and 0xFFFF)
            val axisCount = buf.getShort(offset + 8).toInt() and 0xFFFF

            for (i in 0 until axisCount) {
                val a = axesArrOff + i * 20
                if (a + 20 > buf.limit()) break
                val tag = readTag(buf, a)
                val min = buf.getInt(a + 4) / 65536f       // Fixed 16.16
                val def = buf.getInt(a + 8) / 65536f
                val max = buf.getInt(a + 12) / 65536f
                val nameID = buf.getShort(a + 18).toInt() and 0xFFFF
                axes.add(AxisInfo(tag, nameMap[nameID] ?: tag, min, def, max))
            }
        } catch (_: Exception) { }
        return axes
    }

    // ──────────────────────────────────────────────
    //  GSUB / GPOS FeatureList
    // ──────────────────────────────────────────────

    private fun parseFeatureList(
        buf: ByteBuffer, tableOffset: Int, out: MutableSet<String>
    ) {
        try {
            val flOff = tableOffset + (buf.getShort(tableOffset + 6).toInt() and 0xFFFF)
            if (flOff + 2 > buf.limit()) return
            val count = buf.getShort(flOff).toInt() and 0xFFFF
            for (i in 0 until count) {
                val rec = flOff + 2 + i * 6
                if (rec + 4 > buf.limit()) break
                out.add(readTag(buf, rec))
            }
        } catch (_: Exception) { }
    }

    // ──────────────────────────────────────────────

    private fun readTag(buf: ByteBuffer, offset: Int): String =
        String(
            byteArrayOf(
                buf.get(offset), buf.get(offset + 1),
                buf.get(offset + 2), buf.get(offset + 3)
            ),
            Charsets.US_ASCII
        )
}
