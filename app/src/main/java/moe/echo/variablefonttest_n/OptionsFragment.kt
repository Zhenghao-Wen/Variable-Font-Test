package moe.echo.variablefonttest_n

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.forEach
import androidx.preference.PreferenceManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.preference.SimpleMenuPreference
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.collections.MutableMap
import kotlin.collections.contains
import kotlin.collections.filter
import kotlin.collections.get
import kotlin.collections.joinToString
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toMap
import kotlin.collections.zip
import kotlin.math.abs
import kotlin.math.pow

private const val TAG = "OptionsFragment"


class OptionsFragment : PreferenceFragmentCompat() {

    private companion object {
        const val PREF_VARIATION_STATE = "pref_variation_state"
        const val PREF_FEATURE_STATE = "pref_feature_state"
    }

    private val fontVariationSettings = mutableMapOf<String, String>()
    private val fontFeatureSettings = mutableMapOf<String, String>()

    private val getFont =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) changeFontFromUri(uri)
        }

    private fun createAddPreferenceDialog(
        context: Context,
        preferences: PreferenceCategory,
        setSetting: (tagName: String, value: String) -> Unit
    ) = MaterialAlertDialogBuilder(context).apply {
        // https://developer.android.com/develop/ui/views/components/dialogs#CustomLayout
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val dialogLayout = View.inflate(context, R.layout.add_preference_dialog, null)

        val autoCompleteTextView = dialogLayout.findViewById<AutoCompleteTextView>(R.id.tagType)
        val typeValues = resources.getStringArray(R.array.font_feature_type_values)

        val seekBarMinLayout = dialogLayout.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tagSeekBarMinLayout)
        val seekBarMaxLayout = dialogLayout.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tagSeekBarMaxLayout)
        val seekBarStepLayout = dialogLayout.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tagSeekBarStepLayout)
        
        val seekBarMin = dialogLayout.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tagSeekBarMin)
        val seekBarMax = dialogLayout.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tagSeekBarMax)
        val seekBarStep = dialogLayout.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tagSeekBarStep)

        val adapter = ArrayAdapter.createFromResource(
            context,
            R.array.font_feature_types,
            android.R.layout.simple_dropdown_item_1line
        )

        autoCompleteTextView.setAdapter(adapter)
        // Set default selection to the first item (Switch)
        if (autoCompleteTextView.text.toString().isEmpty()) {
            autoCompleteTextView.setText(adapter.getItem(0), false)
        }
        
        // Helper function to show/hide seek bar fields based on selected type
        fun updateSeekBarFieldsVisibility(selectedPosition: Int) {
            when (typeValues[selectedPosition]) {
                Constants.ADD_FEATURE_TYPE_SEEK_BAR -> {
                    seekBarMinLayout.isVisible = true
                    seekBarMaxLayout.isVisible = true
                    seekBarStepLayout.isVisible = true
                }
                else -> {
                    seekBarMinLayout.isVisible = false
                    seekBarMaxLayout.isVisible = false
                    seekBarStepLayout.isVisible = false
                }
            }
        }
        
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            updateSeekBarFieldsVisibility(position)
        }
        
        // Initialize visibility based on current selection
        val currentPosition = adapter.getPosition(autoCompleteTextView.text.toString())
        if (currentPosition >= 0) {
            updateSeekBarFieldsVisibility(currentPosition)
        }

        setView(dialogLayout)

        setPositiveButton(android.R.string.ok) { _, _ ->
            val tagNameEditText = dialogLayout.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.tagName)
            val tagName = tagNameEditText.text.toString()

            val selectedItemPosition = adapter.getPosition(autoCompleteTextView.text.toString())
            
            // Validate position to prevent ArrayIndexOutOfBoundsException
            if (selectedItemPosition < 0 || selectedItemPosition >= typeValues.size) {
                Toast.makeText(context, R.string.invalid_tag_type, Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            
            val preference = when (typeValues[selectedItemPosition]) {
                Constants.ADD_FEATURE_TYPE_SWITCH ->
                    SwitchPreferenceCompat(preferenceScreen.context).apply {

                        setOnPreferenceChangeListener { _, _ ->
                            setSetting(tagName, if (!isChecked) "1" else "0")
                            true
                        }
                    }
                Constants.ADD_FEATURE_TYPE_SEEK_BAR -> {
                    MD3SeekBarPreference(preferenceScreen.context).apply {
                        val rawMin = seekBarMin.text.toString()
                        val rawMax = seekBarMax.text.toString()
                        val rawStep = seekBarStep.text.toString()

                        val minSetting = rawMin.toFloatOrNull() ?: 0F
                        val maxSetting = rawMax.toFloatOrNull() ?: 0F

                        val minimum = minSetting.coerceAtMost(maxSetting)
                        val maximum = minSetting.coerceAtLeast(maxSetting)
                        val step = rawStep.toFloatOrNull() ?: 0F

                        var offset = 0F
                        var multiplier = 1F

                        if (rawStep.contains(".")) {
                            val decimalWithDot = rawStep
                                .substring(rawStep.indexOf("."))
                            val decimalWithDotLength = decimalWithDot.length

                            if (decimalWithDotLength > 1) {
                                if (decimalWithDot.substring(1).toInt() > 0) {
                                    multiplier *= 10F.pow(decimalWithDotLength - 1)
                                }
                            }
                        }

                        if (minimum < 0) {
                            offset += abs(minimum)
                        }

                        min = ((minimum + offset) * multiplier).toInt()
                        max = ((maximum + offset) * multiplier).toInt()
                        seekBarIncrement = (step * multiplier).toInt()

                        Log.i(TAG, "createAddPreferenceDialog: $tagName: minimum: $minimum")
                        Log.i(TAG, "createAddPreferenceDialog: $tagName: maximum: $maximum")
                        Log.i(TAG, "createAddPreferenceDialog: $tagName: step: $step")

                        Log.i(TAG, "createAddPreferenceDialog: $tagName: offset: $offset")
                        Log.i(TAG, "createAddPreferenceDialog: $tagName: multiplier: $multiplier")

                        Log.i(TAG, "createAddPreferenceDialog: $tagName: seekBar.min: $min")
                        Log.i(TAG, "createAddPreferenceDialog: $tagName: seekBar.max: $max")
                        Log.i(
                            TAG,
                            "createAddPreferenceDialog: $tagName: seekBar.seekBarIncrement: $seekBarIncrement"
                        )

                        updatesContinuously = true

                        setOnPreferenceChangeListener { _, newValue ->
                            val value = newValue.toString().toFloatOrNull()

                            if (value != null) {
                                setSetting(
                                    tagName,
                                    ((value - offset * multiplier) / multiplier).toString()
                                )
                                persistSettings()
                                true
                            } else false
                        }
                    }
                }
                Constants.ADD_FEATURE_TYPE_EDIT_TEXT ->
                    EditTextPreference(preferenceScreen.context).apply {
                        dialogTitle = tagName

                        setOnPreferenceChangeListener { _, newValue ->
                            try {
                                summary = newValue.toString()
                                setSetting(tagName, newValue.toString())
                                return@setOnPreferenceChangeListener true
                            } catch (e: IllegalArgumentException) {
                                Toast.makeText(
                                    context,
                                    e.message.toString(),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            false
                        }
                    }
                else -> null
            } ?: return@setPositiveButton

            preference.apply {
                key = tagName
                title = tagName

                isPersistent = false
            }

            val duplicateKeyPreference = findPreference<Preference>(tagName)
            if (duplicateKeyPreference != null) {
                preferences.removePreference(duplicateKeyPreference)
            }
            // ── 如果 MD3 Slider 已开启且当前添加的是拖动条，替换为 SliderPreference ──
            val useMd3Slider = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.PREF_USE_MD3_SLIDER, false)
            val finalPreference: Preference = if (
                useMd3Slider && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                typeValues[selectedItemPosition] == Constants.ADD_FEATURE_TYPE_SEEK_BAR
            ) {
                SliderPreference(context).apply {
                    key = tagName
                    title = tagName
                    summary = tagName
                    valueFrom = (preference as SeekBarPreference).min.toFloat()
                    valueTo = preference.max.toFloat()
                    stepSize = preference.seekBarIncrement.toFloat().coerceAtLeast(1f)
                    sliderValue = preference.value.toFloat()
                    showLabel = false
                    isPersistent = false
                    setOnPreferenceChangeListener { _, newValue ->
                        val value = newValue.toString().toFloatOrNull()
                        if (value != null) {
                            setSetting(tagName, value.toString())
                            persistSettings()
                            true
                        } else false
                    }
                }
            } else {
                preference
            }
            preferences.addPreference(finalPreference)

            // ── 保存自定义参数元数据（用于 recreate 后重建 UI）──
            saveCustomPrefMeta(
                context = context,
                key = tagName,
                type = typeValues[selectedItemPosition],
                category = if (preferences.key == Constants.PREF_CATEGORY_VARIATIONS) "variations" else "fontFeatures",
                min = if (typeValues[selectedItemPosition] == Constants.ADD_FEATURE_TYPE_SEEK_BAR)
                    (finalPreference as? SeekBarPreference)?.min ?: 0 else 0,
                max = if (typeValues[selectedItemPosition] == Constants.ADD_FEATURE_TYPE_SEEK_BAR)
                    (finalPreference as? SeekBarPreference)?.max ?: 100 else 100,
                step = if (typeValues[selectedItemPosition] == Constants.ADD_FEATURE_TYPE_SEEK_BAR)
                    (finalPreference as? SeekBarPreference)?.seekBarIncrement ?: 1 else 1
            )

            // Reorganize preferences to make add & edit preference always at bottom
            preferences.forEach {
                when (it.key) {
                    preference.key -> it.order = preferences.preferenceCount - 3
                    Constants.PREF_ADD_FONT_VARIATION, Constants.PREF_ADD_FONT_FEATURE ->
                        it.order = preferences.preferenceCount - 2
                    Constants.PREF_EDIT_VARIATION, Constants.PREF_EDIT_FEATURE ->
                        it.order = preferences.preferenceCount - 1
                }
            }
        }
        setNegativeButton(android.R.string.cancel) { _, _ -> return@setNegativeButton }
    }


    // ═══ MD3 EditTextPreference 对话框接管 ═══
    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is EditTextPreference) {
            showMd3EditTextDialog(preference)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun showMd3EditTextDialog(preference: EditTextPreference) {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_text_md3, null)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.md3_edit_text_layout)
        val textInputEditText = dialogView.findViewById<TextInputEditText>(R.id.md3_edit_text)

        // Hint: 使用 Preference 的 title（天然 i18n）
        textInputLayout.hint = preference.title

        // 预设值回显
        val currentValue = preference.text
        if (!currentValue.isNullOrEmpty()) {
            textInputEditText.setText(currentValue)
            textInputEditText.setSelection(currentValue.length)
        }

        // 根据 Preference key 设置 InputType
        when (preference.key) {
            Constants.PREF_TTC_INDEX, Constants.PREF_TEXT_SIZE -> {
                textInputEditText.inputType = InputType.TYPE_CLASS_NUMBER
            }
            else -> {
                textInputEditText.inputType = InputType.TYPE_CLASS_TEXT
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(preference.dialogTitle ?: preference.title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newValue = textInputEditText.text?.toString() ?: ""
                if (preference.callChangeListener(newValue)) {
                    preference.text = newValue
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
            .apply {
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }
    }
    // ═══ MD3 EditTextPreference 对话框接管结束 ═══

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.options, rootKey)
    }

    override fun onPause() {
        super.onPause()
        persistSettings()
    }

    private fun persistSettings() {
        val variationJson = org.json.JSONObject()
        fontVariationSettings.forEach { (k, v) -> variationJson.put(k, v) }
        val featureJson = org.json.JSONObject()
        fontFeatureSettings.forEach { (k, v) -> featureJson.put(k, v) }
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
            .putString(PREF_VARIATION_STATE, variationJson.toString())
            .putString(PREF_FEATURE_STATE, featureJson.toString())
            .apply()
    }

    private fun restoreSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // 判断本次 onCreate 是"模式切换"还是"真正的应用启动"
        val isModeSwitch = prefs.getBoolean(Constants.PREF_IS_MODE_SWITCH, false)
        if (isModeSwitch) {
            // 消费标志，防止下次启动误判
            prefs.edit().remove(Constants.PREF_IS_MODE_SWITCH).apply()
        }

        // 模式切换：无条件恢复（SeekBar↔Slider 必须无缝）
        // 应用启动：仅在用户开启"下次启动不重置参数"时恢复
        val keepParams = prefs.getBoolean(Constants.PREF_KEEP_PARAMS, false)
        if (!isModeSwitch && !keepParams) {
            prefs.edit()
                .remove(PREF_VARIATION_STATE)
                .remove(PREF_FEATURE_STATE)
                .remove(Constants.PREF_CUSTOM_PREFS_META)  // 清除自定义参数元数据
                .apply()
            return
        }

        // ── 恢复逻辑（不变）──
        prefs.getString(PREF_VARIATION_STATE, null)?.let { raw ->
            try {
                val json = org.json.JSONObject(raw)
                json.keys().forEach { key ->
                    fontVariationSettings[key] = json.getString(key)
                }
            } catch (_: Exception) { }
        }
        prefs.getString(PREF_FEATURE_STATE, null)?.let { raw ->
            try {
                val json = org.json.JSONObject(raw)
                json.keys().forEach { key ->
                    fontFeatureSettings[key] = json.getString(key)
                }
            } catch (_: Exception) { }
        }
    }

    /** 保存自定义参数元数据到 SharedPreferences */
    private fun saveCustomPrefMeta(
        context: Context, key: String, type: String, category: String,
        min: Int, max: Int, step: Int
    ) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val raw = prefs.getString(Constants.PREF_CUSTOM_PREFS_META, "[]") ?: "[]"
        try {
            val arr = org.json.JSONArray(raw)
            // 移除同 key 旧条目
            val filtered = org.json.JSONArray()
            for (i in 0 until arr.length()) {
                if (arr.getJSONObject(i).optString("key") != key) {
                    filtered.put(arr.getJSONObject(i))
                }
            }
            // 添加新条目
            filtered.put(org.json.JSONObject().apply {
                put("key", key)
                put("type", type)
                put("category", category)
                put("min", min)
                put("max", max)
                put("step", step)
            })
            prefs.edit().putString(Constants.PREF_CUSTOM_PREFS_META, filtered.toString()).apply()
        } catch (_: Exception) { }
    }

    /** 从 SharedPreferences 恢复自定义参数 UI 控件 */
    private fun restoreCustomPrefs(useMd3Slider: Boolean, applyVariation: (String) -> Unit) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val raw = prefs.getString(Constants.PREF_CUSTOM_PREFS_META, "[]") ?: "[]"
        try {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                val meta = arr.getJSONObject(i)
                val key = meta.getString("key")
                val type = meta.getString("type")
                val categoryKey = meta.getString("category")
                val category: PreferenceCategory = when (categoryKey) {
                    "variations" -> findPreference(Constants.PREF_CATEGORY_VARIATIONS) ?: continue
                    else -> findPreference(Constants.PREF_CATEGORY_FONT_FEATURES) ?: continue
                }
                // 跳过已存在的（避免重复）
                if (findPreference<Preference>(key) != null) continue
                val setSetting: (String, String) -> Unit = if (categoryKey == "variations") {
                    { tagName, value ->
                        fontVariationSettings[tagName] = value
                        applyVariation(fontVariationSettings.toFeatures())
                    }
                } else {
                    { tagName, value ->
                        fontFeatureSettings[tagName] = value
                    }
                }
                when (type) {
                    Constants.ADD_FEATURE_TYPE_SWITCH -> {
                        SwitchPreferenceCompat(requireContext()).apply {
                            this.key = key
                            title = key
                            isPersistent = false
                            isChecked = fontFeatureSettings[key] == "1"
                            setOnPreferenceChangeListener { _, _ ->
                                fontFeatureSettings[key] = if (!isChecked) "1" else "0"
                                persistSettings()
                                true
                            }
                        }.also { category.addPreference(it) }
                    }
                    Constants.ADD_FEATURE_TYPE_SEEK_BAR -> {
                        val min = meta.optInt("min", 0)
                        val max = meta.optInt("max", 100)
                        val step = meta.optInt("step", 1)
                        val savedValue = fontVariationSettings[key]?.toFloatOrNull()
                        if (useMd3Slider && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            SliderPreference(requireContext()).apply {
                                this.key = key
                                title = key
                                summary = key
                                valueFrom = min.toFloat()
                                valueTo = max.toFloat()
                                stepSize = step.toFloat().coerceAtLeast(1f)
                                sliderValue = savedValue ?: min.toFloat()
                                showLabel = false
                                isPersistent = false
                                setOnPreferenceChangeListener { _, newValue ->
                                    val v = newValue.toString().toFloatOrNull()
                                    if (v != null) {
                                        fontVariationSettings[key] = v.toString()
                                        applyVariation(fontVariationSettings.toFeatures())
                                        persistSettings()
                                        true
                                    } else false
                                }
                            }.also { category.addPreference(it) }
                        } else {
                            MD3SeekBarPreference(requireContext()).apply {
                                this.key = key
                                title = key
                                this.min = min
                                this.max = max
                                seekBarIncrement = step
                                updatesContinuously = true
                                isPersistent = false
                                if (savedValue != null) value = savedValue.toInt()
                                setOnPreferenceChangeListener { _, newValue ->
                                    val v = newValue.toString().toFloatOrNull()
                                    if (v != null) {
                                        fontVariationSettings[key] = v.toString()
                                        applyVariation(fontVariationSettings.toFeatures())
                                        persistSettings()
                                        true
                                    } else false
                                }
                            }.also { category.addPreference(it) }
                        }
                    }
                    Constants.ADD_FEATURE_TYPE_EDIT_TEXT -> {
                        EditTextPreference(requireContext()).apply {
                            this.key = key
                            title = key
                            dialogTitle = key
                            isPersistent = false
                            text = fontVariationSettings[key] ?: fontFeatureSettings[key]
                            setOnPreferenceChangeListener { _, newValue ->
                                fontVariationSettings[key] = newValue.toString()
                                applyVariation(fontVariationSettings.toFeatures())
                                persistSettings()
                                true
                            }
                        }.also { category.addPreference(it) }
                    }
                }
            }
            // ── 重排序：确保自定义参数在预设滑块之后、"添加/文本模式"按钮之前 ──
            listOf(
                findPreference<PreferenceCategory>(Constants.PREF_CATEGORY_VARIATIONS),
                findPreference<PreferenceCategory>(Constants.PREF_CATEGORY_FONT_FEATURES)
            ).forEach { cat ->
                cat ?: return@forEach
                var idx = 0
                cat.forEach { pref ->
                    when (pref.key) {
                        Constants.PREF_ADD_FONT_VARIATION,
                        Constants.PREF_ADD_FONT_FEATURE ->
                            pref.order = Int.MAX_VALUE - 1
                        Constants.PREF_EDIT_VARIATION,
                        Constants.PREF_EDIT_FEATURE ->
                            pref.order = Int.MAX_VALUE
                        else ->
                            pref.order = idx++
                    }
                }
            }
        } catch (_: Exception) { }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── 从 SharedPreferences 恢复参数状态 ──
        restoreSettings()

        val fontFamilyOptions = resources.getStringArray(R.array.font_family_values)
        // Drop custom font option
        val fontFamilyValues = fontFamilyOptions.filter { s -> s != Constants.OPTION_CUSTOM_VALUE }
        val fontFamilyList = arrayOf(
            Typeface.DEFAULT, Typeface.DEFAULT_BOLD, Typeface.MONOSPACE,
            Typeface.SANS_SERIF, Typeface.SERIF
        )
        // Pair options and typefaces
        val valueToTypeface = fontFamilyValues.zip(fontFamilyList).toMap()

        val previewContent: EditText? =
            parentFragment?.view?.findViewById(R.id.preview_content)

        val textSize: EditTextPreference? = findPreference(Constants.PREF_TEXT_SIZE)
        val fontFamilies: SimpleMenuPreference? = findPreference(Constants.PREF_FONT_FAMILIES)
        val ttcIndex: EditTextPreference? = findPreference(Constants.PREF_TTC_INDEX)
        val customFont: Preference? = findPreference(Constants.PREF_CUSTOM_FONT)

        val variations: PreferenceCategory? = findPreference(Constants.PREF_CATEGORY_VARIATIONS)
        val ital: MD3SeekBarPreference? = findPreference(Constants.PREF_VARIATION_ITALIC)
        val opsz: MD3SeekBarPreference? = findPreference(Constants.PREF_VARIATION_OPTICAL_SIZE)
        val slnt: MD3SeekBarPreference? = findPreference(Constants.PREF_VARIATION_SLANT)
        val wdth: MD3SeekBarPreference? = findPreference(Constants.PREF_VARIATION_WIDTH)
        val wght: MD3SeekBarPreference? = findPreference(Constants.PREF_VARIATION_WEIGHT)
        val variationEditor: EditTextPreference? = findPreference(Constants.PREF_VARIATION_EDITOR)
        val addVariation: Preference? = findPreference(Constants.PREF_ADD_FONT_VARIATION)
        val editVariation: Preference? = findPreference(Constants.PREF_EDIT_VARIATION)

        val fontFeatures: PreferenceCategory? = findPreference(Constants.PREF_CATEGORY_FONT_FEATURES)
        val chws: SwitchPreferenceCompat? = findPreference(Constants.PREF_FEATURE_CHWS)
        val halt: SwitchPreferenceCompat? = findPreference(Constants.PREF_FEATURE_HALT)
        val frac: SwitchPreferenceCompat? = findPreference(Constants.PREF_FEATURE_FRAC)
        val featureEditor: EditTextPreference? = findPreference(Constants.PREF_FEATURE_EDITOR)
        val addFeature: Preference? = findPreference(Constants.PREF_ADD_FONT_FEATURE)
        val editFeatures: Preference? = findPreference(Constants.PREF_EDIT_FEATURE)

        // https://developer.android.com/develop/ui/views/layout/edge-to-edge
        // https://medium.com/androiddevelopers/gesture-navigation-handling-gesture-conflicts-8ee9c2665c69#eaaa
        listView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(listView) { v, windowInsets ->
            val insets = WindowInsetsUtil.safeDrawing(windowInsets)

            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            v.updatePadding(bottom = insets.bottom)

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        fun setVariation(settings: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                previewContent?.fontVariationSettings = settings
            }
        }

        Log.d(TAG, "onViewCreated: prviewContent == null? " + (previewContent == null))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            variations?.isEnabled = true

            val unsupportedAndroid = findPreference<Preference>(Constants.PREF_UNSUPPORTED_ANDROID)
            unsupportedAndroid?.isVisible = false
        }
        textSize?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue.toString().toFloatOrNull() != null) {
                    previewContent?.textSize = newValue.toString().toFloat()
                    true
                } else false
            }
        }

        fontFamilies?.setOnPreferenceChangeListener { _, newValue ->
            // 持久化字体选择
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                .putString(Constants.PREF_FONT_FAMILY, newValue.toString())
                .apply()
            when {
                valueToTypeface.contains(newValue) -> {
                    customFont?.isVisible = false
                    ttcIndex?.isVisible = false
                    previewContent?.typeface = valueToTypeface[newValue]
                    setVariation(fontVariationSettings.toFeatures())
                    true
                }
                newValue == Constants.OPTION_CUSTOM_VALUE -> {
                    customFont?.isVisible = true
                    ttcIndex?.isVisible = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ttcIndex?.isEnabled = true
                    }
                    true
                }
                else -> false
            }
        }

        customFont?.setOnPreferenceClickListener {
            getFont.launch("font/*")
            true
        }

        // ── 恢复字体选择（模式切换 / 启动时均适用）──
        val savedFontFamily = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString(Constants.PREF_FONT_FAMILY, null)
        if (savedFontFamily != null && savedFontFamily != "default") {
            fontFamilies?.value = savedFontFamily
            when {
                valueToTypeface.contains(savedFontFamily) -> {
                    customFont?.isVisible = false
                    ttcIndex?.isVisible = false
                    previewContent?.typeface = valueToTypeface[savedFontFamily]
                }
                savedFontFamily == Constants.OPTION_CUSTOM_VALUE -> {
                    customFont?.isVisible = true
                    ttcIndex?.isVisible = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ttcIndex?.isEnabled = true
                    }
                    val savedUri = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString(Constants.PREF_CUSTOM_FONT_URI, null)
                    if (savedUri != null) {
                        try {
                            changeFontFromUri(Uri.parse(savedUri))
                        } catch (_: Exception) { }
                    }
                }
            }
            setVariation(fontVariationSettings.toFeatures())
        }

        // ── Variation axis change handlers (shared between SeekBar & Slider) ──
        val italHandler: (Any?) -> Boolean = { newValue ->
            val value = newValue.toString().toFloatOrNull()

            if (value != null) {
                fontVariationSettings[Constants.VARIATION_AXIS_ITALIC] = (value / 10).toString()
                setVariation(fontVariationSettings.toFeatures())
                persistSettings()
                true
            } else false
        }

        val opszHandler: (Any?) -> Boolean = { newValue ->
            val value = newValue.toString().toFloatOrNull()

            if (value != null) {
                fontVariationSettings[Constants.VARIATION_AXIS_OPTICAL_SIZE] = (value / 10).toString()
                setVariation(fontVariationSettings.toFeatures())
                persistSettings()
                true
            } else false
        }

        val slntHandler: (Any?) -> Boolean = { newValue ->
            val value = newValue.toString().toFloatOrNull()

            if (value != null) {
                fontVariationSettings[Constants.VARIATION_AXIS_SLANT] = (value - 90).toString()
                setVariation(fontVariationSettings.toFeatures())
                persistSettings()
                true
            } else false
        }

        val wdthHandler: (Any?) -> Boolean = { newValue ->
            val value = newValue.toString().toFloatOrNull()

            if (value != null) {
                fontVariationSettings[Constants.VARIATION_AXIS_WIDTH] = (value / 10).toString()
                setVariation(fontVariationSettings.toFeatures())
                persistSettings()
                true
            } else false
        }

        val wghtHandler: (Any?) -> Boolean = { newValue ->
            val value = newValue.toString().toFloatOrNull()

            if (value != null) {
                fontVariationSettings[Constants.VARIATION_AXIS_WEIGHT] = value.toString()
                setVariation(fontVariationSettings.toFeatures())
                persistSettings()
                true
            } else false
        }

        // ── Check MD3 Slider toggle ──
        val useMd3Slider = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean(Constants.PREF_USE_MD3_SLIDER, false)

        if (useMd3Slider && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Replace SeekBarPreferences with MD3 SliderPreferences
            variations?.let { category ->
                val replacements = listOf(
                    SliderReplacement(
                        seekBarKey = Constants.PREF_VARIATION_ITALIC,
                        titleRes = R.string.variation_italic,
                        summary = "ital",
                        valueFrom = 0f,
                        valueTo = 10f,
                        stepSize = 1f,
                        defaultValue = 0f,
                        showLabel = false,
                        handler = italHandler
                    ),
                    SliderReplacement(
                        seekBarKey = Constants.PREF_VARIATION_OPTICAL_SIZE,
                        titleRes = R.string.variation_optical_size,
                        summary = "opsz",
                        valueFrom = 1f,
                        valueTo = 1440f,
                        stepSize = 1f,
                        defaultValue = 1f,
                        showLabel = false,
                        handler = opszHandler
                    ),
                    SliderReplacement(
                        seekBarKey = Constants.PREF_VARIATION_SLANT,
                        titleRes = R.string.variation_slant,
                        summary = "slnt",
                        valueFrom = 0f,
                        valueTo = 180f,
                        stepSize = 1f,
                        defaultValue = 90f,
                        showLabel = false,
                        handler = slntHandler
                    ),
                    SliderReplacement(
                        seekBarKey = Constants.PREF_VARIATION_WIDTH,
                        titleRes = R.string.variation_width,
                        summary = "wdth",
                        valueFrom = 0f,
                        valueTo = 2000f,
                        stepSize = 1f,
                        defaultValue = 1000f,
                        showLabel = false,
                        handler = wdthHandler
                    ),
                    SliderReplacement(
                        seekBarKey = Constants.PREF_VARIATION_WEIGHT,
                        titleRes = R.string.variation_weight,
                        summary = "wght",
                        valueFrom = 1f,
                        valueTo = 1000f,
                        stepSize = 1f,
                        defaultValue = 400f,
                        showLabel = true,
                        handler = wghtHandler
                    )
                )

                val replacementMap = replacements.associateBy { it.seekBarKey }
                // ── 第一步：快照当前所有 Preference（保留原始顺序）──
                val snapshot = mutableListOf<Preference>()
                category.forEach { snapshot.add(it) }
                // ── 第二步：一次性清空（仅 1 次 notifyChanged）──
                category.removeAll()
                // ── 第三步：按原始顺序重建，SeekBar 替换为 Slider，其余原样放回 ──
                var orderIdx = 0
                for (pref in snapshot) {
                    val r = replacementMap[pref.key]
                    if (r != null) {
                        // 从已保存的状态中读取当前值，实现无缝切换
                        val currentValue = when (r.seekBarKey) {
                            Constants.PREF_VARIATION_ITALIC ->
                                fontVariationSettings[Constants.VARIATION_AXIS_ITALIC]
                                    ?.toFloatOrNull()?.let { it * 10 } ?: r.defaultValue
                            Constants.PREF_VARIATION_OPTICAL_SIZE ->
                                fontVariationSettings[Constants.VARIATION_AXIS_OPTICAL_SIZE]
                                    ?.toFloatOrNull()?.let { it * 10 } ?: r.defaultValue
                            Constants.PREF_VARIATION_SLANT ->
                                fontVariationSettings[Constants.VARIATION_AXIS_SLANT]
                                    ?.toFloatOrNull()?.let { it + 90 } ?: r.defaultValue
                            Constants.PREF_VARIATION_WIDTH ->
                                fontVariationSettings[Constants.VARIATION_AXIS_WIDTH]
                                    ?.toFloatOrNull()?.let { it * 10 } ?: r.defaultValue
                            Constants.PREF_VARIATION_WEIGHT ->
                                fontVariationSettings[Constants.VARIATION_AXIS_WEIGHT]
                                    ?.toFloatOrNull() ?: r.defaultValue
                            else -> r.defaultValue
                        }
                        val sliderPref = SliderPreference(requireContext()).apply {
                            key = r.seekBarKey
                            title = getString(r.titleRes)
                            summary = r.summary
                            valueFrom = r.valueFrom
                            valueTo = r.valueTo
                            stepSize = r.stepSize
                            sliderValue = currentValue
                            showLabel = r.showLabel
                            isPersistent = false
                            setOnPreferenceChangeListener { _, newValue -> r.handler(newValue) }
                            order = orderIdx++
                        }
                        category.addPreference(sliderPref)
                    } else {
                        pref.order = orderIdx++
                        category.addPreference(pref)
                    }
                }
            }
        } else {
            // ── 从 fontVariationSettings 恢复 SeekBar 值（逆转换）──
            fontVariationSettings[Constants.VARIATION_AXIS_ITALIC]?.toFloatOrNull()?.let {
                ital?.value = (it * 10).toInt()
            }
            fontVariationSettings[Constants.VARIATION_AXIS_OPTICAL_SIZE]?.toFloatOrNull()?.let {
                opsz?.value = (it * 10).toInt()
            }
            fontVariationSettings[Constants.VARIATION_AXIS_SLANT]?.toFloatOrNull()?.let {
                slnt?.value = (it + 90).toInt()
            }
            fontVariationSettings[Constants.VARIATION_AXIS_WIDTH]?.toFloatOrNull()?.let {
                wdth?.value = (it * 10).toInt()
            }
            fontVariationSettings[Constants.VARIATION_AXIS_WEIGHT]?.toFloatOrNull()?.let {
                wght?.value = it.toInt()
            }

            // ── 恢复 Feature 开关状态 ──
            fontFeatureSettings[Constants.FEATURE_CHWS]?.let {
                chws?.isChecked = (it == "1")
            }
            fontFeatureSettings[Constants.FEATURE_HALT]?.let {
                halt?.isChecked = (it == "1")
            }
            fontFeatureSettings[Constants.FEATURE_FRAC]?.let {
                frac?.isChecked = (it == "1")
            }

            // ── 设置 listeners ──
            ital?.setOnPreferenceChangeListener { _, newValue -> italHandler(newValue) }
            opsz?.setOnPreferenceChangeListener { _, newValue -> opszHandler(newValue) }
            slnt?.setOnPreferenceChangeListener { _, newValue -> slntHandler(newValue) }
            wdth?.setOnPreferenceChangeListener { _, newValue -> wdthHandler(newValue) }
            wght?.setOnPreferenceChangeListener { _, newValue -> wghtHandler(newValue) }
        }

        // ── 恢复自定义参数 UI 控件 ──
        restoreCustomPrefs(useMd3Slider) { settings -> setVariation(settings) }

        variationEditor?.setOnPreferenceChangeListener { _, newValue ->
            try {
                setVariation(newValue.toString())
                return@setOnPreferenceChangeListener true
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, e.message.toString(), Toast.LENGTH_LONG).show()
            }
            false
        }

        addVariation?.setOnPreferenceClickListener {
            if (variations != null) {
                createAddPreferenceDialog(view.context, variations) { tagName, value ->
                    fontVariationSettings[tagName] = value
                    setVariation(fontVariationSettings.toFeatures())
                }.apply {
                    setTitle(R.string.add_font_variation)
                    show()
                }
                true
            } else false
        }

        editVariation?.setOnPreferenceClickListener {
            variationEditor?.apply {
                text = fontVariationSettings.toFeatures()

                if (isVisible) {
                    variations?.forEach {
                        if (
                            it.key !in setOf(
                                Constants.PREF_ADD_FONT_VARIATION,
                                Constants.PREF_EDIT_VARIATION,
                                Constants.PREF_UNSUPPORTED_ANDROID
                            )
                        ) {
                            it.isVisible = true
                        }
                    }

                    editVariation.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_edit_24)
                    editVariation.title = getString(R.string.edit_variation_text)
                } else {
                    variations?.forEach {
                        if (
                            it.key !in setOf(
                                Constants.PREF_ADD_FONT_VARIATION,
                                Constants.PREF_EDIT_VARIATION,
                                Constants.PREF_UNSUPPORTED_ANDROID
                            )
                        ) {
                            it.isVisible = false
                        }
                    }

                    editVariation.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_build_24)
                    editVariation.title = getString(R.string.edit_variation_ui)
                }

                isVisible = !isVisible
            }
            true
        }

        chws?.apply {
            // `chws` is disabled by default when SDK < 33
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                isChecked = false
            }

            setOnPreferenceChangeListener { _, _ ->
                fontFeatureSettings[Constants.FEATURE_CHWS] = if (!isChecked) "1" else "0"
                previewContent?.fontFeatureSettings = fontFeatureSettings.toFeatures()
                persistSettings()
                true
            }
        }

        halt?.apply {
            setOnPreferenceChangeListener { _, _ ->
                fontFeatureSettings[Constants.FEATURE_HALT] = if (!isChecked) "1" else "0"
                previewContent?.fontFeatureSettings = fontFeatureSettings.toFeatures()
                persistSettings()
                true
            }
        }

        frac?.apply {
            setOnPreferenceChangeListener { _, _ ->
                fontFeatureSettings[Constants.FEATURE_FRAC] = if (!isChecked) "1" else "0"
                previewContent?.fontFeatureSettings = fontFeatureSettings.toFeatures()
                persistSettings()
                true
            }
        }

        featureEditor?.setOnPreferenceChangeListener { _, newValue ->
            try {
                previewContent?.fontFeatureSettings = newValue.toString()
                return@setOnPreferenceChangeListener true
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, e.message.toString(), Toast.LENGTH_LONG).show()
            }
            false
        }

        addFeature?.setOnPreferenceClickListener {
            if (fontFeatures != null) {
                createAddPreferenceDialog(view.context, fontFeatures) { tagName, value ->
                    fontFeatureSettings[tagName] = value
                    previewContent?.fontFeatureSettings = fontFeatureSettings.toFeatures()
                }.apply {
                    setTitle(R.string.add_font_feature)
                    show()
                }
                true
            } else false
        }

        editFeatures?.setOnPreferenceClickListener {
            featureEditor?.apply {
                text = fontFeatureSettings.toFeatures()

                if (isVisible) {
                    fontFeatures?.forEach {
                        if (
                            it.key !in setOf(
                                Constants.PREF_ADD_FONT_FEATURE,
                                Constants.PREF_EDIT_FEATURE
                            )
                        ) {
                            it.isVisible = true
                        }
                    }

                    editFeatures.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_edit_24)
                    editFeatures.title = getString(R.string.edit_feature_text)
                } else {
                    fontFeatures?.forEach {
                        if (
                            it.key !in setOf(
                                Constants.PREF_ADD_FONT_FEATURE,
                                Constants.PREF_EDIT_FEATURE
                            )
                        ) {
                            it.isVisible = false
                        }
                    }

                    editFeatures.icon =
                        ContextCompat.getDrawable(context, R.drawable.ic_baseline_build_24)
                    editFeatures.title = getString(R.string.edit_feature_ui)
                }

                isVisible = !isVisible
            }

            true
        }

        // ── 恢复预览文本的字体可变/特性设置 ──
        if (fontVariationSettings.isNotEmpty()) {
            setVariation(fontVariationSettings.toFeatures())
            persistSettings()
        }
        if (fontFeatureSettings.isNotEmpty()) {
            previewContent?.fontFeatureSettings = fontFeatureSettings.toFeatures()
            persistSettings()
        }
    }

    private fun MutableMap<String, String>.toFeatures(): String =
        this.toList().joinToString { "'${it.first}' ${it.second}" }

    private fun copyStreamToFile(inputStream: InputStream, outputFile: File) {
        inputStream.use { input ->
            val outputStream = FileOutputStream(outputFile)
            outputStream.use { output ->
                val buffer = ByteArray(4 * 1024) // buffer size
                while (true) {
                    val byteCount = input.read(buffer)
                    if (byteCount < 0) break
                    output.write(buffer, 0, byteCount)
                }
                output.flush()
            }
        }
    }

    private fun changeFontFromUri(uri: Uri) {
        // 持久化自定义字体 URI
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
            .putString(Constants.PREF_CUSTOM_FONT_URI, uri.toString())
            .apply()

        activity?.runOnUiThread {
            val previewContent: EditText? = parentFragment?.view?.findViewById(R.id.preview_content)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ttcIndex: EditTextPreference? = findPreference(Constants.PREF_TTC_INDEX)
                val ttcIndexValue = ttcIndex?.text?.toIntOrNull() ?: 0

                activity?.contentResolver?.openFileDescriptor(uri, "r")?.use {
                    val builder = Typeface.Builder(it.fileDescriptor)
                    builder.setFontVariationSettings(fontVariationSettings.toFeatures())
                    builder.setTtcIndex(ttcIndexValue)
                    previewContent?.typeface = builder.build()
                    return@runOnUiThread
                } ?: {
                    Log.w(TAG, "changeFontFromUri: Failed to set font.")
                    Log.w(TAG, "changeFontFromUri: Uri: $uri")
                    Log.w(TAG, "changeFontFromUri: Uri?.path: ${uri.path}")
                    Log.w(TAG, "changeFontFromUri: activity == null? ${activity == null}")
                    Log.w(
                        TAG,
                        "changeFontFromUri: activity?.contentResolver == null? ${activity?.contentResolver == null}"
                    )
                }
            } else {
                val cacheDir = context?.cacheDir

                if (cacheDir != null) {
                    val font = File.createTempFile("font", "ByVFT")

                    val inputStream = activity?.contentResolver?.openInputStream(uri)

                    if (inputStream != null) {
                        copyStreamToFile(inputStream, font)
                        previewContent?.typeface = Typeface.createFromFile(font)
                        return@runOnUiThread
                    } else {
                        Log.w(TAG, "changeFontFromUri: Failed to openInputStream to set font.")
                        Log.w(TAG, "changeFontFromUri: Uri: $uri")
                        Log.w(TAG, "changeFontFromUri: context?.cacheDir: $cacheDir")
                        Log.w(TAG, "changeFontFromUri: activity == null? ${activity == null}")
                        Log.w(
                            TAG,
                            "changeFontFromUri: activity?.contentResolver == null? ${activity?.contentResolver == null}"
                        )
                    }
                } else {
                    Log.w(TAG, "changeFontFromUri: Failed to set font.")
                    Log.w(TAG, "changeFontFromUri: Uri: $uri")
                }
            }

            Toast.makeText(context, R.string.font_import_failed, Toast.LENGTH_LONG).show()
        }
    }
}

/**
 * Describes a SeekBarPreference → SliderPreference replacement.
 */
private data class SliderReplacement(
    val seekBarKey: String,
    val titleRes: Int,
    val summary: String,
    val valueFrom: Float,
    val valueTo: Float,
    val stepSize: Float,
    val defaultValue: Float,
    val showLabel: Boolean,
    val handler: (Any?) -> Boolean
)
