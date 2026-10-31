package moe.echo.variablefonttest_n

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider
import kotlin.math.pow

/**
 * A [Preference] that displays the Material 3 [Slider] used for every slider in the app
 * (the legacy MD2 SeekBar has been removed).
 *
 * The floating value label and tick marks are controlled by toolbar toggles
 * (app:labelBehavior / app:tickVisibilityMode equivalents), and the widget correctly
 * resolves ?attr/colorPrimary for active track and thumb so it keeps proper saturation
 * with dynamic colors (system_accent1_200 in dark mode).
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
        /** LabelBehavior.FLOATING = 0 */
        const val LABEL_FLOATING = 0

        /** LabelBehavior.GONE = 2（@Retention(SOURCE)，无法导入，使用稳定 ABI 常量） */
        const val LABEL_GONE = 2

        /** TickVisibilityMode.TICK_VISIBILITY_AUTO_LIMIT = 0 */
        const val TICK_VISIBILITY_AUTO_LIMIT = 0

        /**
         * TickVisibilityMode.TICK_VISIBILITY_HIDDEN = 2。
         * 该注解是 @Retention(SOURCE)，运行时不可引用，故用本地常量。
         */
        const val TICK_VISIBILITY_HIDDEN = 2

        // ── 两端对齐微调量（dp），经 Android 12/15 实机调试确定 ──
        /** 全部滑块左端右移量：轨道左缘相对"贴齐标题左缘"再右移此值 */
        const val LEFT_SHIFT_DP = 6f
        /** wght 数值区整体左移量：轨道右缘连带数值文字一起左移此值 */
        const val WGHT_ASSEMBLY_SHIFT_DP = 16f
        /** 非 wght/自定义右端右移量：右端留距 = W − 此值（W 为数值区宽度） */
        const val OTHER_RIGHT_SHIFT_DP = 16f
    }

    init {
        layoutResource = R.layout.preference_widget_slider
        // MD3 Legacy 样式需要与旧 MD2 SeekBar 行完全对齐（无图标占位）；
        // Expressive 保持 Material 3 偏好行的图标区留白。
        isIconSpaceReserved = !isLegacySliderStyle()
    }

    /** 当前主题下的 Slider 样式是否为 MD3 Legacy（Widget.Material3.Slider.Legacy）。 */
    private fun isLegacySliderStyle(): Boolean = try {
        val attrs = context.obtainStyledAttributes(
            intArrayOf(com.google.android.material.R.attr.sliderStyle)
        )
        val styleRes = attrs.getResourceId(0, 0)
        attrs.recycle()
        styleRes != 0 && context.resources.getResourceEntryName(styleRes) == "Widget.Material3.Slider.Legacy"
    } catch (_: Exception) {
        false
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
        // 数值归一化：valueTo 必须大于 valueFrom、stepSize 必须为正且整除整个区间，
        // 且 value 必须落在 valueFrom + n × stepSize 的步进网格上，否则 Material Slider 会抛异常。
        val (from, to, step) = sanitizedRange()
        s.valueFrom = from
        s.valueTo = to
        s.stepSize = step

        // ── 数值浮标与刻度标签：由工具栏开关控制（对所有滑块生效）──
        val displayPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val showValueLabel = displayPrefs.getBoolean(
            Constants.PREF_SHOW_SLIDER_VALUE_LABEL, true
        )
        val showTicks = displayPrefs.getBoolean(
            Constants.PREF_SHOW_SLIDER_TICKS, false
        )

        // app:labelBehavior 等价：FLOATING → 显示拖动数值浮标，GONE → 隐藏
        if (showValueLabel) {
            s.labelBehavior = LABEL_FLOATING
            s.setLabelFormatter { value ->
                val display = value * valueScale + valueOffset
                formatValue(display)
            }
        } else {
            s.labelBehavior = LABEL_GONE
            s.setLabelFormatter(null)
        }

        // app:tickVisibilityMode 等价：AUTO_LIMIT → 按密度自动限制刻度，HIDDEN → 不显示
        s.setTickVisibilityMode(
            if (showTicks) TICK_VISIBILITY_AUTO_LIMIT else TICK_VISIBILITY_HIDDEN
        )

        // ── 轨道两端对齐补偿（仅 MD3 Expressive 样式需要）──
        // MD3 Legacy 样式目标是与 MD2 SeekBar 度量对齐，直接使用其默认布局即可，
        // 不再做额外负 margin 补偿，避免与 SeekBar 观感产生偏差。
        //
        // Expressive：负 margin 抵消 trackSidePadding 内缩——左缘对齐标题左缘、
        // wght 右缘贴合数值区、其余右缘回退一个 inset。
        // 必须用公开的 getTrackSidePadding()，不能反射读取私有字段 trackSidePadding：
        // R8 会重命名私有字段，反射取不到值（inset 恒为 0），补偿被跳过，
        // 表现为 R8 包 Slider 两端比非 R8 包内缩（约 4dp）。
        val isLegacyStyle = isLegacySliderStyle()
        val inset = if (isLegacyStyle) 0 else s.trackSidePadding
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

        // ── 设置当前值（无 listener，安全；先对齐步进网格）──
        val snapped = from + Math.round((sliderValue - from) / step) * step
        s.value = snapped.coerceIn(from, to)

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
                // 使轨道右缘连带数值文字整体左移。
                // （仅 showLabel=true 即 wght 生效；此处 this 为 layoutParams）
                if (showLabel && !isLegacyStyle) {
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

    /**
     * 重新把当前的 range/step/value 应用到已绑定的 Slider 上。
     * 用于外部（如字体元数据长按更新轴范围）在绑定后修改数值域时刷新界面。
     */
    fun refresh() {
        val s = slider ?: return
        val (from, to, step) = sanitizedRange()
        s.valueFrom = from
        s.valueTo = to
        s.stepSize = step
        val snapped = from + Math.round((sliderValue - from) / step) * step
        s.value = snapped.coerceIn(from, to)
    }

    /**
     * Material Slider 要求 valueTo > valueFrom，且 stepSize 必须整除整个区间；
     * 否则 setStepSize / setValueFrom 会抛 IllegalArgumentException。
     * 这里把区间按 step 分成整数份并重新计算 step，保证三项约束同时成立。
     */
    private fun sanitizedRange(): Triple<Float, Float, Float> {
        val from = valueFrom
        val rawTo = if (valueTo > from) valueTo else from + 1f
        val range = rawTo - from
        val requested = if (stepSize > 0f) stepSize else 1f
        val divisions = Math.max(1.0, Math.ceil(range / requested.toDouble()))
        val step = (range / divisions).toFloat()
        val to = (from.toDouble() + step.toDouble() * divisions).toFloat()
        return Triple(from, to, step)
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
