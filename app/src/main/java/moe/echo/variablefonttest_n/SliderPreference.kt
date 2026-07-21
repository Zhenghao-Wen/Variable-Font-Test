package moe.echo.variablefonttest_n

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.LabelBehavior
import com.google.android.material.slider.Slider

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

    init {
        layoutResource = R.layout.preference_widget_slider
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val s = holder.findViewById(R.id.preference_slider) as? Slider ?: return
        val vt = holder.findViewById(R.id.slider_value) as? TextView
        
        slider = s
        valueText = vt
        
        // ── 关键修复：清除视图回收残留的所有 listener ──
        // 使用 tag 追踪上一次附加的 listener，确保回收时正确移除
        (s.tag as? Slider.OnChangeListener)?.let { s.removeOnChangeListener(it) }
        
        // ── 设置 Slider 参数（此时无 listener 附加，不会触发回调）──
        s.valueFrom = valueFrom
        s.valueTo = valueTo
        s.stepSize = stepSize
        
        // ── 修复：使用正确的 LabelBehavior 常量 ──
        s.labelBehavior = if (showLabel) {
            LabelBehavior.FLOATING // 值 = 0，拖动时拇指上方浮动标签
        } else {
            LabelBehavior.GONE // 值 = 2，完全隐藏标签
        }
        
        // ── 设置当前值（无 listener，安全）──
        s.value = sliderValue.coerceIn(valueFrom, valueTo)
        
        // ── 附加本实例的 listener 并记录到 tag ──
        s.addOnChangeListener(onChangeListener)
        s.tag = onChangeListener
        
        s.isEnabled = isEnabled
        
        // ── 右侧数值显示 ──
        vt?.let {
            if (showLabel) {
                it.visibility = android.view.View.VISIBLE
                it.text = formatValue(sliderValue)
            } else {
                it.visibility = android.view.View.GONE
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
            // 更新右侧数值
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
