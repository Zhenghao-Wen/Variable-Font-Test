package moe.echo.variablefonttest_n

import android.content.Context

/** OpenType 特性 4 字母 tag → 本地化友好名称（打表；ss/cv 使用编号模板）。 */
object OtFeatureNames {

    private val EXACT: Map<String, Int> = mapOf(
        "liga" to R.string.ot_feature_liga,
        "clig" to R.string.ot_feature_clig,
        "dlig" to R.string.ot_feature_dlig,
        "hlig" to R.string.ot_feature_hlig,
        "rlig" to R.string.ot_feature_rlig,
        "calt" to R.string.ot_feature_calt,
        "salt" to R.string.ot_feature_salt,
        "aalt" to R.string.ot_feature_aalt,
        "swsh" to R.string.ot_feature_swsh,
        "titl" to R.string.ot_feature_titl,
        "kern" to R.string.ot_feature_kern,
        "cpsp" to R.string.ot_feature_cpsp,
        "mark" to R.string.ot_feature_mark,
        "mkmk" to R.string.ot_feature_mkmk,
        "curs" to R.string.ot_feature_curs,
        "dist" to R.string.ot_feature_dist,
        "jalt" to R.string.ot_feature_jalt,
        "lnum" to R.string.ot_feature_lnum,
        "onum" to R.string.ot_feature_onum,
        "pnum" to R.string.ot_feature_pnum,
        "tnum" to R.string.ot_feature_tnum,
        "frac" to R.string.ot_feature_frac,
        "afrc" to R.string.ot_feature_afrc,
        "numr" to R.string.ot_feature_numr,
        "dnom" to R.string.ot_feature_dnom,
        "ordn" to R.string.ot_feature_ordn,
        "zero" to R.string.ot_feature_zero,
        "smcp" to R.string.ot_feature_smcp,
        "c2sc" to R.string.ot_feature_c2sc,
        "pcap" to R.string.ot_feature_pcap,
        "c2pc" to R.string.ot_feature_c2pc,
        "unic" to R.string.ot_feature_unic,
        "case" to R.string.ot_feature_case,
        "sups" to R.string.ot_feature_sups,
        "subs" to R.string.ot_feature_subs,
        "sinf" to R.string.ot_feature_sinf,
        "vert" to R.string.ot_feature_vert,
        "vrt2" to R.string.ot_feature_vrt2,
        "cpct" to R.string.ot_feature_cpct,
        "halt" to R.string.ot_feature_halt,
        "fwid" to R.string.ot_feature_fwid,
        "hwid" to R.string.ot_feature_hwid,
        "ruby" to R.string.ot_feature_ruby,
        "trad" to R.string.ot_feature_trad,
        "smpl" to R.string.ot_feature_smpl,
        "jp78" to R.string.ot_feature_jp78,
        "jp90" to R.string.ot_feature_jp90,
        "jp04" to R.string.ot_feature_jp04,
        "hojo" to R.string.ot_feature_hojo,
        "locl" to R.string.ot_feature_locl,
    )

    private val NUMBERED = Regex("^(ss|cv)(\\d{2})$")

    /** 返回该 tag 的友好名称；表中没有（或编号越界）时返回 null。 */
    fun friendlyName(context: Context, tag: String): String? {
        val normalized = tag.trim().lowercase()
        EXACT[normalized]?.let { return context.getString(it) }
        val match = NUMBERED.matchEntire(normalized) ?: return null
        val number = match.groupValues[2].toIntOrNull() ?: return null
        return when (match.groupValues[1]) {
            "ss" -> if (number in 1..20) context.getString(R.string.ot_feature_ss, number) else null
            "cv" -> if (number in 1..99) context.getString(R.string.ot_feature_cv, number) else null
            else -> null
        }
    }
}
