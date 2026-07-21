package moe.echo.variablefonttest_n

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentContainerView
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(this)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        // https://developer.android.com/about/versions/15/behavior-changes-15#custom-background-protection
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.tappableElement())

            insets.apply {
                val topInsetsBackground: View? = findViewById(R.id.top_insets_background)
                topInsetsBackground?.let {
                    it.layoutParams.height = top
                    it.requestLayout()
                }

                val bottomInsetsBackground: View? = findViewById(R.id.bottom_insets_background)
                bottomInsetsBackground?.let {
                    it.layoutParams.height = bottom
                    it.requestLayout()
                }

                val leftInsetsBackground: View? = findViewById(R.id.left_insets_background)
                leftInsetsBackground?.let {
                    val layoutParams = it.layoutParams as ViewGroup.MarginLayoutParams
                    layoutParams.width = left
                    layoutParams.topMargin = top
                    layoutParams.bottomMargin = bottom
                    it.requestLayout()
                }

                val rightInsetsBackground: View? = findViewById(R.id.right_insets_background)
                rightInsetsBackground?.let {
                    val layoutParams = it.layoutParams as ViewGroup.MarginLayoutParams
                    layoutParams.width = right
                    layoutParams.topMargin = top
                    layoutParams.bottomMargin = bottom
                    it.requestLayout()
                }
            }

            windowInsets
        }

        val appBarLayout: AppBarLayout? = findViewById(R.id.app_bar_layout)
        appBarLayout?.apply {
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, windowInsets ->
                val insets = WindowInsetsUtil.safeDrawing(windowInsets)

                v.updatePadding(top = insets.top, left = insets.left, right = insets.right)

                windowInsets
            }
        }

        val mainContainer: FragmentContainerView? = findViewById(R.id.main_container)
        if (mainContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { v, windowInsets ->
                val insets = WindowInsetsUtil.safeDrawing(windowInsets)

                v.updatePadding(left = insets.left, right = insets.right)

                windowInsets
            }
        }

        // ── Toolbar 菜单：仅操作 toolbar.menu，不使用 onCreateOptionsMenu ──
        val toolbar: MaterialToolbar? = findViewById(R.id.toolbar)
        toolbar?.let { tb ->
            // 从 SharedPreferences 同步 checkbox 状态到 Toolbar 的菜单实例
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val useMd3Slider = prefs.getBoolean(Constants.PREF_USE_MD3_SLIDER, false)
            tb.menu.findItem(R.id.action_enable_md3_slider)?.isChecked = useMd3Slider
            tb.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_enable_md3_slider -> {
                        val newState = !menuItem.isChecked
                        menuItem.isChecked = newState
                        prefs.edit()
                            .putBoolean(Constants.PREF_USE_MD3_SLIDER, newState)
                            .apply()
                        recreate()
                        true
                    }
                    else -> false
                }
            }
        }
    }
    // ── 已删除 onCreateOptionsMenu ──
    // 原因：MaterialToolbar 的 app:menu 由 Toolbar 自行 inflate，
    // onCreateOptionsMenu 会创建第二个独立 Menu 实例，
    // 导致 checkbox 状态写入错误的 Menu 对象。
}
