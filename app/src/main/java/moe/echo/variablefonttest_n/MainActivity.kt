package moe.echo.variablefonttest_n

import android.os.Build
import android.view.Menu
import android.view.MenuItem
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
        // Apply dynamic colors for Android 12+ before setContentView
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

        // ── Toolbar menu setup ──
        val toolbar: MaterialToolbar? = findViewById(R.id.toolbar)
        toolbar?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_enable_md3_slider -> {
                    menuItem.isChecked = !menuItem.isChecked
                    PreferenceManager.getDefaultSharedPreferences(this)
                        .edit()
                        .putBoolean(Constants.PREF_USE_MD3_SLIDER, menuItem.isChecked)
                        .apply()
                    // Recreate to rebuild preference screen with new slider mode
                    recreate()
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        // Sync checkbox state with stored preference
        val useMd3Slider = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.PREF_USE_MD3_SLIDER, false)
        menu.findItem(R.id.action_enable_md3_slider)?.isChecked = useMd3Slider
        return true
    }
}
