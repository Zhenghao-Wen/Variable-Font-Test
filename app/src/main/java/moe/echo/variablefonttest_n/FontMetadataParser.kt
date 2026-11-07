package moe.echo.variablefonttest_n

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.atan2

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

    /** OS/2 表 fsType：字体嵌入权限（OpenType 规范）。 */
    data class EmbeddingInfo(
        val fsType: Int,
        val installable: Boolean,
        val restricted: Boolean,
        val previewPrint: Boolean,
        val editable: Boolean,
        val noSubsetting: Boolean,
        val bitmapOnly: Boolean
    )

    /** 字体度量（head/hhea/OS/2/post，default master / regular 的取值）。 */
    data class MetricsInfo(
        val unitsPerEm: Int? = null,
        val ascender: Int? = null,
        val descender: Int? = null,
        val lineGap: Int? = null,
        val typoAscender: Int? = null,
        val typoDescender: Int? = null,
        val typoLineGap: Int? = null,
        val winAscent: Int? = null,
        val winDescent: Int? = null,
        val capHeight: Int? = null,
        val xHeight: Int? = null,
        val caretOffset: Int? = null,
        val caretSlopeDegrees: Float? = null,
        val strikeoutPosition: Int? = null,
        val strikeoutSize: Int? = null,
        val underlinePosition: Int? = null,
        val underlineThickness: Int? = null,
        val subscriptXOffset: Int? = null,
        val subscriptXSize: Int? = null,
        val subscriptYOffset: Int? = null,
        val subscriptYSize: Int? = null,
        val superscriptXOffset: Int? = null,
        val superscriptXSize: Int? = null,
        val superscriptYOffset: Int? = null,
        val superscriptYSize: Int? = null
    )

    data class Metadata(
        val nameInfo: NameInfo,
        val axes: List<AxisInfo>,
        val features: List<String>,
        val embedding: EmbeddingInfo? = null,
        val metrics: MetricsInfo? = null
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
        val embedding = tables["OS/2"]?.let { parseOs2Embedding(buf, it.first) }
        val metrics = parseMetrics(
            buf,
            tables["head"]?.first,
            tables["hhea"]?.first,
            tables["OS/2"]?.first,
            tables["post"]?.first
        )

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
            features = features.sorted(),
            embedding = embedding,
            metrics = metrics
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
    //  head / hhea / OS/2 / post 度量
    // ──────────────────────────────────────────────

    private fun readU16At(buf: ByteBuffer, offset: Int): Int? =
        if (offset >= 0 && offset + 2 <= buf.limit()) buf.getShort(offset).toInt() and 0xFFFF else null

    private fun readS16At(buf: ByteBuffer, offset: Int): Int? =
        if (offset >= 0 && offset + 2 <= buf.limit()) buf.getShort(offset).toInt() else null

    private fun parseMetrics(
        buf: ByteBuffer,
        headOffset: Int?,
        hheaOffset: Int?,
        os2Offset: Int?,
        postOffset: Int?
    ): MetricsInfo? {
        if (headOffset == null && hheaOffset == null && os2Offset == null && postOffset == null) {
            return null
        }
        val os2Version = os2Offset?.let { readU16At(buf, it) }
        val caretRise = hheaOffset?.let { readS16At(buf, it + 18) }
        val caretRun = hheaOffset?.let { readS16At(buf, it + 20) }
        val caretSlope = if (caretRise != null && caretRun != null && (caretRise != 0 || caretRun != 0)) {
            Math.toDegrees(atan2(caretRun.toDouble(), caretRise.toDouble())).toFloat()
        } else {
            null
        }
        return MetricsInfo(
            unitsPerEm = headOffset?.let { readU16At(buf, it + 18) },
            ascender = hheaOffset?.let { readS16At(buf, it + 4) },
            descender = hheaOffset?.let { readS16At(buf, it + 6) },
            lineGap = hheaOffset?.let { readS16At(buf, it + 8) },
            typoAscender = os2Offset?.let { readS16At(buf, it + 68) },
            typoDescender = os2Offset?.let { readS16At(buf, it + 70) },
            typoLineGap = os2Offset?.let { readS16At(buf, it + 72) },
            winAscent = os2Offset?.let { readU16At(buf, it + 74) },
            winDescent = os2Offset?.let { readU16At(buf, it + 76) },
            capHeight = if (os2Version != null && os2Version >= 2) os2Offset?.let { readS16At(buf, it + 88) } else null,
            xHeight = if (os2Version != null && os2Version >= 2) os2Offset?.let { readS16At(buf, it + 86) } else null,
            caretOffset = hheaOffset?.let { readS16At(buf, it + 22) },
            caretSlopeDegrees = caretSlope,
            strikeoutPosition = os2Offset?.let { readS16At(buf, it + 28) },
            strikeoutSize = os2Offset?.let { readS16At(buf, it + 26) },
            underlinePosition = postOffset?.let { readS16At(buf, it + 8) },
            underlineThickness = postOffset?.let { readS16At(buf, it + 10) },
            subscriptXOffset = os2Offset?.let { readS16At(buf, it + 14) },
            subscriptXSize = os2Offset?.let { readS16At(buf, it + 10) },
            subscriptYOffset = os2Offset?.let { readS16At(buf, it + 16) },
            subscriptYSize = os2Offset?.let { readS16At(buf, it + 12) },
            superscriptXOffset = os2Offset?.let { readS16At(buf, it + 22) },
            superscriptXSize = os2Offset?.let { readS16At(buf, it + 18) },
            superscriptYOffset = os2Offset?.let { readS16At(buf, it + 24) },
            superscriptYSize = os2Offset?.let { readS16At(buf, it + 20) }
        )
    }

    // ──────────────────────────────────────────────
    //  OS/2 表（嵌入权限 fsType）
    // ──────────────────────────────────────────────

    private fun parseOs2Embedding(buf: ByteBuffer, offset: Int): EmbeddingInfo? {
        return try {
            if (offset + 10 > buf.limit()) return null
            val fsType = buf.getShort(offset + 8).toInt() and 0xFFFF
            val base = fsType and 0x000F
            EmbeddingInfo(
                fsType = fsType,
                installable = base == 0,
                restricted = (base and 0x0002) != 0,
                previewPrint = (base and 0x0004) != 0,
                editable = (base and 0x0008) != 0,
                noSubsetting = (fsType and 0x0100) != 0,
                bitmapOnly = (fsType and 0x0200) != 0
            )
        } catch (_: Exception) {
            null
        }
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
