package moe.echo.variablefonttest_n

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.SeekBar
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.google.android.material.color.MaterialColors

/**
 * [SeekBarPreference] 子类，在标准库布局基础上程序化修正 SeekBar 的色彩。
 *
 * 不修改任何布局文件，不改变 view type，不影响行距和 Divider。
 * 仅在 super.onBindViewHolder 完成标准绑定后，将 progressTint/thumbTint
 * 从 PreferenceThemeOverlay 解析到的 colorSecondary (system_accent2_200)
 * 修正为 colorPrimary (system_accent1_200)。
 */
class SeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    // 关键修复：必须传入 seekBarPreferenceStyle，否则布局回退为
    // preference_material（不含 @+id/seekbar），导致 onBindViewHolder 闪退。
    // 标准 SeekBarPreference(Context, AttributeSet) 构造函数内部也是传入此值。
    defStyleAttr: Int = androidx.preference.R.attr.seekBarPreferenceStyle
) : androidx.preference.SeekBarPreference(context, attrs, defStyleAttr) {

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        // 先让父类完成标准绑定（SeekBar 值、min/max、showSeekBarValue 等）
        super.onBindViewHolder(view)

        // 程序化修正 tint：直接操作 SeekBar 实例，优先级高于任何 theme/style
        val seekBar = view.findViewById(androidx.preference.R.id.seekbar) as? SeekBar ?: return
        val primaryColor = MaterialColors.getColor(
            seekBar.context,
            // Material 1.13.0 起不再重复声明 colorPrimary 属性（R.attr.colorPrimary 被移除），
            // 该属性由 AppCompat 提供，主题中设置的也是同一个 attr。
            androidx.appcompat.R.attr.colorPrimary,
            Color.MAGENTA
        )
        val primaryColorStateList = ColorStateList.valueOf(primaryColor)
        seekBar.progressTintList = primaryColorStateList
        seekBar.thumbTintList = primaryColorStateList

        // ── wght 数值显示区：与 MD3 slider_value 统一样式（外观/颜色/等宽/自适应宽度/居中）──
        // seekbar_value 来自库布局不可直接改 XML，故在 super.onBindViewHolder 之后程序化统一，
        // 使其与 MD3 slider_value 的 XML 定义（textAppearanceListItemSecondary + textColorSecondary）同源。
        (view.findViewById(androidx.preference.R.id.seekbar_value) as? android.widget.TextView)?.let { tv ->
            // 统一文字外观与颜色（解析主题属性）
            val ta = tv.context.obtainStyledAttributes(
                intArrayOf(android.R.attr.textAppearanceListItemSecondary, android.R.attr.textColorSecondary)
            )
            val appearanceRes = ta.getResourceId(0, 0)
            val textColor = ta.getColor(1, tv.currentTextColor)
            ta.recycle()
            if (appearanceRes != 0) {
                androidx.core.widget.TextViewCompat.setTextAppearance(tv, appearanceRes)
            }
            tv.setTextColor(textColor)
            // 等宽数字 + 自适应宽度（完整容纳 1000）+ 居中
            tv.fontFeatureSettings = "tnum"
            val density = tv.resources.displayMetrics.density
            val paddingH = (tv.paddingStart + tv.paddingEnd).toFloat()
            tv.layoutParams = tv.layoutParams.apply {
                width = (tv.paint.measureText("1000") + paddingH + 2 * density).toInt()
            }
            tv.gravity = Gravity.CENTER
        }

        // TODO: 临时诊断，定位 4dp 问题后删除
        seekBar.postDelayed({
            val parent = seekBar.parent as? android.view.View
            val row = parent?.parent as? android.view.View
            val item = row?.parent as? android.view.View
            val loc = IntArray(2)
            seekBar.getLocationInWindow(loc)
            val msg = "SeekBar key=$key " +
                "density=${seekBar.resources.displayMetrics.density} " +
                "w=${seekBar.width} h=${seekBar.height} left=${seekBar.left} right=${seekBar.right} screenX=${loc[0]} " +
                "trackLeft=${loc[0] + seekBar.paddingLeft} trackRight=${loc[0] + seekBar.width - seekBar.paddingRight} " +
                "padding=${seekBar.paddingLeft}/${seekBar.paddingRight} " +
                "thumbOffset=${seekBar.thumbOffset} progress=${seekBar.progress}/${seekBar.max} " +
                "parentW=${parent?.width} rowW=${row?.width} itemW=${item?.width} " +
                "itemPad=${item?.paddingLeft}/${item?.paddingRight} rootW=${seekBar.rootView.width}"
            Log.d("VFT_DIAG", msg)
        }, 600)
    }
}
