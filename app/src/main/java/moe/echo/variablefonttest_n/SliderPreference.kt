package moe.echo.variablefonttest_n

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.slider.Slider

/**
 * A [Preference] that displays a Material 3 [Slider] instead of the legacy [android.widget.SeekBar].
 *
 * This preference correctly resolves `?attr/colorPrimary` for its active track and thumb,
 * ensuring proper saturation with dynamic colors (system_accent1_200 in dark mode)
 * unlike the MD2 SeekBar which may resolve to colorPrimaryContainer (system_accent1_100).
 */
class SliderPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    /** Minimum value of the slider. */
    var valueFrom: Float = 0f

    /** Maximum value of the slider. */
    var valueTo: Float = 100f

    /** Step size. Use 0f for continuous sliding. */
    var stepSize: Float = 1f

    /** Current slider value. */
    var sliderValue: Float = 0f

    /** Whether to show a floating label above the thumb during interaction. */
    var showLabel: Boolean = false

    private var slider: Slider? = null

    init {
        layoutResource = R.layout.preference_widget_slider
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val s = holder.findViewById(R.id.preference_slider) as? Slider ?: return
        slider = s
        s.valueFrom = valueFrom
        s.valueTo = valueTo
        s.stepSize = stepSize
        // Set label behavior: FLOATING for showLabel=true, GONE otherwise
        s.setLabelBehavior(if (showLabel) { 1 } else { 0 })
        // Remove listener before setting value to avoid spurious callbacks
        s.removeOnChangeListener(onChangeListener)
        s.value = sliderValue.coerceIn(valueFrom, valueTo)
        s.addOnChangeListener(onChangeListener)
        s.isEnabled = isEnabled
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val default = (defaultValue as? Number)?.toFloat() ?: valueFrom
        sliderValue = getPersistedFloat(default)
    }

    private val onChangeListener = Slider.OnChangeListener { _, value, fromUser ->
        if (fromUser) {
            sliderValue = value
            // Trigger OnPreferenceChangeListener; only persist if accepted
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
}
