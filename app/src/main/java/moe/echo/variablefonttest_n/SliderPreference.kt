package moe.echo.variablefonttest_n

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
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
        /** Label floats above the thumb during drag. */
        const val LABEL_FLOATING = 0
        /** Label is shown within slider bounds during drag. */
        const val LABEL_WITHIN_BOUNDS = 1
        /** No label is shown. */
        const val LABEL_GONE = 2
    }

    init {
        layoutResource = R.layout.preference_widget_slider
        isIconSpaceReserved = true
        // 修复：代码动态创建的 Preference 不经过 PreferenceInflater，
        // 不会应用 Preference.Material 样式中的 allowDivider=false。
        // PreferenceThemeOverlay 主题默认 allowDivider=true，
        // 导致 DividerDecoration 在每项上下方绘制多余分隔线。
        // 手动设为 false，与 Preference.Material 样式行为一致。
        isDividerAllowedAbove = false
        isDividerAllowedBelow = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

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

        // ── 使用本地命名常量（LabelBehavior 不可导入）──
        s.labelBehavior = if (showLabel) LABEL_FLOATING else LABEL_GONE

        // ── 设置当前值（无 listener，安全）──
        s.value = sliderValue.coerceIn(valueFrom, valueTo)

        // ── 附加本实例的 listener 并记录到 tag ──
        s.addOnChangeListener(onChangeListener)
        s.tag = onChangeListener

        s.isEnabled = isEnabled

        // ── 右侧数值显示 ──
        vt?.let {
            if (showLabel) {
                it.visibility = View.VISIBLE
                it.text = formatValue(sliderValue)
            } else {
                it.visibility = View.GONE
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
        return if (v == v.toLong().toFloat()) {
            v.toLong().toString()
        } else {
            v.toString()
        }
    }
}
