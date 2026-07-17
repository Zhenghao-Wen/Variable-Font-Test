# Material Design 3 实现指南

本文档详细说明 Variable Font Test N 应用中 Material Design 3 (MD3) 的实现细节。

## MD3 组件清单

### ✅ 已完全实现的 MD3 组件

#### 1. 主题系统
- **父主题**: `Theme.Material3.DayNight.NoActionBar`
- **动态色彩**: 通过 `materialThemeOverlay` 支持 Android 12+ Material You
- **日夜模式**: 完整的亮色/暗色主题适配

#### 2. 应用栏
- **MaterialToolbar**: 
  - 位置：`activity_main.xml`
  - 样式：使用 `?attr/colorSurface` 背景
  - 标题：自动适配主题色（移除硬编码白色）

#### 3. 卡片组件
- **MaterialCardView**:
  - 位置：`main_fragment_content.xml` (预览区域)
  - 样式：2dp elevation，0dp 圆角（全出血设计）
  - 内容：包含 TextInputLayout

#### 4. 文本输入框
- **TextInputLayout**:
  - 样式：`Widget.Material3.TextInputLayout.OutlinedBox`
  - 位置：
    - 主预览区域 (`main_fragment_content.xml`)
    - 添加偏好对话框 (`add_preference_dialog.xml`)
  - 特性：outlined box 边框，MD3 动效

#### 5. 对话框
- **MaterialAlertDialogBuilder**:
  - 位置：`OptionsFragment.kt`
  - 用途：添加自定义字体变化轴/特性
  - 布局：自定义视图 (`add_preference_dialog.xml`)
  - 所有 EditText 均包裹在 TextInputLayout 中

#### 6. 开关组件
- **SwitchPreference**:
  - 位置：`options.xml` (CHWS, HALT, FRAC 特性开关)
  - 样式：MD3 圆角开关（非 MD2 胶囊形）
  - 颜色：通过主题自动适配

#### 7. 滑块组件
- **SeekBarPreference**:
  - 位置：`options.xml` (ital, opsz, slnt, wdth, wght 变化轴)
  - 样式：MD3 滑块样式
  - 特性：连续更新 (`updatesContinuously="true"`)
  - 自定义范围：支持 min/max/step 配置

#### 8. 分隔线
- **Full-Bleed Divider**:
  - 由 `PreferenceFragmentCompat` 自动渲染
  - 样式：MD3 全出血分隔线
  - 颜色：从主题自动获取

#### 9. 下拉菜单
- **TextInputLayout + Spinner**:
  - 样式：`Widget.Material3.TextInputLayout.OutlinedBox.ExposedDropdownMenu`
  - 位置：`add_preference_dialog.xml` (类型选择器)

### 🎨 颜色系统

#### 亮色主题颜色
```xml
colorPrimary: #00BCD4 (cyan_500)
colorOnPrimary: 自动对比色
colorPrimaryContainer: #80DEEA (cyan_200)
colorOnPrimaryContainer: #0097A7 (cyan_700)
colorSecondary: #FF4081 (pink_A200)
colorSecondaryContainer: #FF4081 (pink_A200)
colorOnSecondaryContainer: #E91E63 (pink_500)
colorSurface: #00BCD4 (cyan_500)
```

#### 暗色主题颜色
```xml
colorPrimary: #80DEEA (cyan_200)
colorOnPrimary: 自动对比色
colorPrimaryContainer: #0097A7 (cyan_700)
colorOnPrimaryContainer: #80DEEA (cyan_200)
colorSecondary: #FF4081 (pink_A200)
colorSecondaryContainer: #E91E63 (pink_500)
colorOnSecondaryContainer: #FF4081 (pink_A200)
colorSurface: #1E1E1E
colorSurfaceVariant: #49454F
colorOnSurfaceVariant: #CAC4D0
```

### 🔧 关键配置

#### 1. 主题定义 (`values/themes.xml`)
```xml
<style name="Theme.VariableFontTest" parent="@style/Theme.Material3.DayNight.NoActionBar">
    <!-- 基础颜色 -->
    <item name="colorPrimary">@color/cyan_500</item>
    <item name="colorSecondary">@color/pink_A200</item>
    <item name="colorSurface">@color/cyan_500</item>
    
    <!-- MD3 Overlay -->
    <item name="materialThemeOverlay">@style/ThemeOverlay.App.Material3</item>
</style>
```

#### 2. MD3 Overlay (`values/themes.xml`)
```xml
<style name="ThemeOverlay.App.Material3" parent="">
    <item name="colorPrimaryContainer">@color/cyan_200</item>
    <item name="colorOnPrimaryContainer">@color/cyan_700</item>
    <item name="colorSecondaryContainer">@color/pink_A200</item>
    <item name="colorOnSecondaryContainer">@color/pink_500</item>
    <item name="colorSwitchThumbNormal">@color/cyan_500</item>
</style>
```

#### 3. 依赖配置 (`app/build.gradle.kts`)
```kotlin
dependencies {
    implementation(libs.androidx.material3)  // MD3 核心库
    implementation(libs.google.material)     // Material Components
    implementation(libs.androidx.preference) // Preference 支持
}
```

## MD3 vs MD2 差异说明

### Switch 开关
- **MD2**: 胶囊形状，较大圆角
- **MD3**: 更圆润的轨道，较小的开关头，正确的颜色状态

### Slider 滑块
- **MD2**: 简单的圆形滑块头
- **MD3**: 更大的滑块头，正确的按压态和焦点态

### Dialog 对话框
- **MD2**: 标准 AlertDialog
- **MD3**: MaterialAlertDialogBuilder，正确的圆角和按钮样式

### 颜色系统
- **MD2**: 主要使用 primary/secondary
- **MD3**: 引入 container/onContainer 颜色对，更丰富的语义化颜色

## 动态色彩支持

### Android 12+ (API 31+)
应用通过 `materialThemeOverlay` 支持 Material You 动态色彩：
- 系统会根据壁纸自动生成配色方案
- 应用会自动采用系统的颜色方案
- 无需额外代码，主题系统自动处理

### Android 11 及以下
使用应用定义的静态颜色方案：
- 亮色模式：青色 + 粉色配色
- 暗色模式：优化的深色配色

## 验证清单

在真机上验证以下项目：

### 基础 UI
- [ ] Toolbar 背景色正确（非纯黑/纯白）
- [ ] Toolbar 标题颜色自动适配主题
- [ ] 页面背景色随主题变化

### 组件样式
- [ ] 预览卡片有轻微阴影（elevation）
- [ ] 文本输入框有 outlined 边框
- [ ] 输入框获得焦点时边框高亮

### 对话框
- [ ] 对话框圆角符合 MD3 规范
- [ ] 按钮样式为 MD3 填充/文字按钮
- [ ] 所有输入框都有 outlined 样式

### 开关
- [ ] 开关轨道为圆角矩形（非胶囊形）
- [ ] 开关头为圆形
- [ ] 开启/关闭状态颜色正确

### 滑块
- [ ] 滑块头大小符合 MD3 规范
- [ ] 滑动时有正确的视觉反馈
- [ ] 激活/非激活段颜色区分明显

### 分隔线
- [ ] 分类之间有全出血分隔线
- [ ] 分隔线颜色与主题协调

### 暗色模式
- [ ] 切换到暗色模式后所有颜色正确
- [ ] 无纯黑背景（使用深灰色）
- [ ] 文字对比度足够

### 动态色彩（Android 12+）
- [ ] 更换系统壁纸后应用颜色变化
- [ ] 颜色搭配和谐
- [ ] 所有组件都应用了新配色

## 故障排查

### 问题：组件仍显示 MD2 样式
**原因**: 可能使用了错误的父主题或组件类
**解决**: 
1. 确认父主题为 `Theme.Material3.DayNight.NoActionBar`
2. 检查是否使用了 `androidx.preference.SwitchPreference`（非 Compat 版本）
3. 确认添加了 `androidx.material3` 依赖

### 问题：动态色彩不生效
**原因**: Android 版本低于 12 或主题配置错误
**解决**:
1. 确认设备运行 Android 12+
2. 检查 `materialThemeOverlay` 是否正确配置
3. 尝试更换系统壁纸测试

### 问题：暗色模式颜色异常
**原因**: 缺少夜间模式资源或颜色引用错误
**解决**:
1. 确认 `values-night/themes.xml` 存在
2. 检查颜色资源是否正确引用夜间版本
3. 验证 `colorSurface` 等关键颜色已定义

## 参考资源

- [Material Design 3 官方文档](https://m3.material.io/)
- [Android Material3 组件](https://developer.android.com/reference/com/google/android/material/categories)
- [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/)
- [动态色彩指南](https://m3.material.io/styles/color/dynamic-color/overview)
