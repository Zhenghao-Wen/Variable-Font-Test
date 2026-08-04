package moe.echo.variablefonttest_n

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider

/**
 * A [Preference] that displays a Material 3 [Slider] instead of the legacy SeekBar.
 *
 * Correctly resolves ?attr/colorPrimary for active track and thumb,
 * ensuring proper saturation with dynamic colors (system_accent1_200 in dark mode).
 */
class SliderPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    var valueFrom: Float = 0f
    var valueTo: Float = 100f
    var stepSize: Float = 1f
    var sliderValue: Float = 0f
    var showLabel: Boolean = false
    var floatingLabelEnabled: Boolean = false
    /** 文本模式换算系数：text = rawValue × valueScale + valueOffset */
    var valueScale: Float = 1f
    var valueOffset: Float = 0f
    /** 显示小数位数：0 = 整数，1 = 一位小数（根据步进值决定） */
    var decimalPlaces: Int = 0

    private var slider: Slider? = null
    private var valueText: TextView? = null

    /**
     * Label behavior constants mirroring the values defined in
     * com.google.android.material.slider.LabelBehavior (source-only annotation,
     * stripped from the compiled AAR by @Retention(RetentionPolicy.SOURCE)).
     *
     * These values are stable ABI constants used by Slider.setLabelBehavior(int).
     * Reference: material-components-android LabelBehavior.java
     */
    private companion object {
        /**
         * 不显示拖动标签（Label formatter）。
         * LabelBehavior.GONE = 2；该注解为 @Retention(SOURCE)，编译后不在 AAR 中，无法导入，故用本地常量。
         */
        const val LABEL_GONE = 2

        /** LabelBehavior.FLOATING = 0 */
        const val LABEL_FLOATING = 0

        // ── 两端对齐微调量（dp），经 Android 12/15 实机调试确定，用于对齐 MD2 SeekBar 观感 ──
        /** 全部滑块左端右移量：轨道左缘相对"贴齐标题左缘"再右移此值 */
        const val LEFT_SHIFT_DP = 6f
        /** wght 数值区整体左移量：轨道右缘连带数值文字一起左移此值 */
        const val WGHT_ASSEMBLY_SHIFT_DP = 16f
        /** 非 wght/自定义右端右移量：右端留距 = W − 此值（W 为数值区宽度） */
        const val OTHER_RIGHT_SHIFT_DP = 16f
    }

    init {
        layoutResource = R.layout.preference_widget_slider
        isIconSpaceReserved = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        // 修复：super 从私有字段读取 divider 标志（主题默认 true），
        // 在 PreferenceViewHolder 上覆盖为 false，与 Preference.Material 样式一致。
        holder.setDividerAllowedAbove(false)
        holder.setDividerAllowedBelow(false)

        val s = holder.findViewById(R.id.preference_slider) as? Slider ?: return
        val vt = holder.findViewById(R.id.slider_value) as? TextView

        slider = s
        valueText = vt

        // ── 修复视图回收导致的 listener 泄漏 ──
        (s.tag as? Slider.OnChangeListener)?.let { s.removeOnChangeListener(it) }

        // ── 设置 Slider 参数（此时无 listener，安全）──
        s.valueFrom = valueFrom
        s.valueTo = valueTo
        s.stepSize = stepSize

        // ── 拖动浮动标签：受 floatingLabelEnabled 控制 ──
        if (floatingLabelEnabled) {
            s.labelBehavior = LABEL_FLOATING
            s.setLabelFormatter { value ->
                val display = value * valueScale + valueOffset
                formatValue(display)
            }
        } else {
            s.labelBehavior = LABEL_GONE
            s.setLabelFormatter(null)
        }
        // ── 隐藏轨道步进圆点（tick marks）：步进值小则圆点密布，隐藏后更整洁 ──
        // 此处统一生效，预设滑块与自定义参数滑块均经 SliderPreference 渲染，一并覆盖。
        s.setTickVisible(false)

        // ── 轨道两端对齐补偿（留距一致 + 呼吸间距 δ）──
        // 第一步：负 margin 抵消 trackSidePadding 内缩——左缘对齐标题左缘、
        //         wght 右缘贴合数值区、其余右缘回退一个 inset（与 wght 数值区留距观感一致）。
        // 第二步：两端各加 δ 呼吸间距——左端右移 δ、其余右端再左移 δ（wght 右端贴合数值区，不加 δ）。
        try {
            var cls: Class<*>? = s.javaClass
            var inset = 0
            while (cls != null) {
                try {
                    val f = cls.getDeclaredField("trackSidePadding")
                    f.isAccessible = true
                    inset = f.getInt(s)
                    break
                } catch (_: Exception) { }
                cls = cls.superclass
            }
            if (inset > 0) {
                val density = s.resources.displayMetrics.density
                val leftShiftPx = (LEFT_SHIFT_DP * density).toInt()
                val otherRightShiftPx = (OTHER_RIGHT_SHIFT_DP * density).toInt()
                // 数值区宽度 W（与改动 2 中设置的 width 同口径），供非 wght 右端留距计算
                val valueTextWidth = valueText?.let { tv ->
                    (tv.paint.measureText("1000") + (tv.paddingStart + tv.paddingEnd) + 2 * density).toInt()
                } ?: 0
                (s.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
                    // 左端：全部滑块右移 LEFT_SHIFT_DP
                    lp.marginStart = -(inset - leftShiftPx)
                    // 右端：
                    //   wght   → -inset，轨道右缘贴合数值文字左缘（整组已由改动 2 的 marginEnd 左移）
                    //   非 wght → 留距 = W - OTHER_RIGHT_SHIFT_DP
                    lp.marginEnd = if (showLabel) -inset else (valueTextWidth - otherRightShiftPx - inset)
                    s.layoutParams = lp
                }
            }
        } catch (_: Exception) { }

        // ── 设置当前值（无 listener，安全）──
        s.value = sliderValue.coerceIn(valueFrom, valueTo)

        // ── 附加本实例的 listener 并记录到 tag ──
        s.addOnChangeListener(onChangeListener)
        s.tag = onChangeListener

        s.isEnabled = isEnabled

        // ── 右侧数值显示：tnum 等宽 + 按本机字体自适应宽度（完整容纳 1000）+ 居中 ──
        vt?.let { tv ->
            tv.fontFeatureSettings = "tnum"
            val density = tv.resources.displayMetrics.density
            val paddingH = (tv.paddingStart + tv.paddingEnd).toFloat()
            tv.layoutParams = tv.layoutParams.apply {
                width = (tv.paint.measureText("1000") + paddingH + 2 * density).toInt()
                // wght：数值文字右缘内缩 WGHT_ASSEMBLY_SHIFT_DP，
                // 使轨道右缘连带数值文字整体左移，对齐 MD2 SeekBar 观感。
                // （仅 showLabel=true 即 wght 生效；此处 this 为 layoutParams）
                if (showLabel) {
                    (this as? android.view.ViewGroup.MarginLayoutParams)?.marginEnd =
                        (WGHT_ASSEMBLY_SHIFT_DP * density).toInt()
                }
            }
            tv.gravity = Gravity.CENTER
            if (showLabel) {
                tv.visibility = View.VISIBLE
                tv.text = formatValue(sliderValue)
            } else {
                tv.visibility = View.GONE
            }
        }
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val default = (defaultValue as? Number)?.toFloat() ?: valueFrom
        sliderValue = getPersistedFloat(default)
    }

    private val onChangeListener = Slider.OnChangeListener { _, value, fromUser ->
        if (fromUser) {
            sliderValue = value
            valueText?.text = formatValue(value)
            if (callChangeListener(value)) {
                if (isPersistent) {
                    persistFloat(value)
                }
            }
        }
    }

    override fun onDependencyChanged(dependency: Preference, disableDependent: Boolean) {
        super.onDependencyChanged(dependency, disableDependent)
        slider?.isEnabled = !disableDependent
    }

    private fun formatValue(v: Float): String {
        // 四舍五入到指定小数位数，避免浮点精度误差
        val multiplier = when (decimalPlaces) {
            0 -> 1f
            1 -> 10f
            else -> 10f.pow(decimalPlaces)
        }
        val rounded = Math.round(v * multiplier) / multiplier
        
        return when (decimalPlaces) {
            0 -> rounded.toLong().toString()  // 整数：400
            1 -> String.format("%.1f", rounded)  // 一位小数：0.0、1.0、144.3
            else -> String.format("%.${decimalPlaces}f", rounded)
        }
    }
}
