# Variable Font Test N - 项目文档

## 项目概述

**Variable Font Test N** 是一个 Android 应用程序，用于测试和预览可变字体（Variable Fonts）的各种属性。应用支持调整字体的变化轴（Variation Axes）、字体特性（Font Features），并提供实时预览功能。

### 基本信息

- **应用名称**: Variable Font Test N
- **包名**: `moe.echo.variablefonttest_n`
- **最低 SDK**: 21 (Android 5.0)
- **目标 SDK**: 35 (Android 15)
- **编译 SDK**: 35
- **版本代码**: 20
- **版本名称**: 3.7

## 技术栈

### 核心依赖

- **Kotlin**: 主要开发语言
- **AndroidX**: 
  - `androidx.core.ktx`: Kotlin 扩展
  - `androidx.activity.compose`: Activity Compose 支持
  - `androidx.material3`: Material Design 3 组件库
  - `androidx.preference`: Preference 支持
- **Material Components**: `google.material` - Material Design 组件
- **RikkaX Preference**: `rikkax.preference.simplemenu` - SimpleMenuPreference 实现

### 构建配置

- **Gradle Plugin**: Android Application Plugin + JetBrains Kotlin Android Plugin
- **Java 兼容性**: Java 8
- **Compose**: 启用但未使用（保留用于未来扩展）
- **签名配置**: 支持 Release 签名（通过环境变量配置）

## 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/moe/echo/variablefonttest_n/
│   │   │   ├── MainActivity.kt          # 主 Activity
│   │   │   ├── MainFragment.kt          # 主 Fragment（容器）
│   │   │   ├── OptionsFragment.kt       # 设置选项 Fragment
│   │   │   ├── Constants.kt             # 常量定义
│   │   │   └── WindowInsetsUtil.kt      # 窗口 insets 工具类
│   │   ├── res/
│   │   │   ├── layout/                  # 布局文件
│   │   │   │   ├── activity_main.xml    # 主 Activity 布局
│   │   │   │   ├── main_fragment.xml    # 主 Fragment 布局
│   │   │   │   ├── main_fragment_content.xml  # 主 Fragment 内容
│   │   │   │   └── add_preference_dialog.xml  # 添加偏好对话框
│   │   │   ├── xml/
│   │   │   │   └── options.xml          # Preference 配置
│   │   │   ├── values/                  # 资源值
│   │   │   │   ├── themes.xml           # 主题定义（亮色）
│   │   │   │   ├── colors.xml           # 颜色定义
│   │   │   │   ├── strings.xml          # 字符串资源
│   │   │   │   ├── arrays.xml           # 数组资源
│   │   │   │   └── font_families_array.xml  # 字体家族数组
│   │   │   ├── values-night/            # 夜间模式资源
│   │   │   │   ├── themes.xml           # 主题定义（暗色）
│   │   │   │   └── colors.xml           # 颜色定义
│   │   │   ├── drawable/                # 可绘制资源
│   │   │   └── mipmap-anydpi-v26/       # 应用图标
│   │   └── AndroidManifest.xml          # 应用清单
│   ├── test/                            # 单元测试
│   └── androidTest/                     # 仪器测试
└── build.gradle.kts                     # 模块构建配置
```

## 核心功能

### 1. 字体预览

- **实时预览**: 支持多行文本输入，实时显示字体效果
- **TextInputLayout**: 使用 MD3 风格的 outlined box 样式
- **MaterialCardView**: 预览区域采用卡片设计

### 2. 字体设置

#### 基础设置
- **字号调整**: 通过 EditTextPreference 调整文本大小
- **字体家族选择**: 支持默认、粗体、等宽、无衬线、衬线字体
- **自定义字体**: 支持从文件系统选择自定义字体文件
- **TTC 索引**: 支持 TTC（TrueType Collection）字体索引选择

#### 可变字体变化轴（Variation Axes）
- **Italic (ital)**: 斜体轴，范围 0-10
- **Optical Size (opsz)**: 光学尺寸轴，范围 1-1440
- **Slant (slnt)**: 倾斜轴，范围 0-180
- **Width (wdth)**: 宽度轴，范围 0-2000
- **Weight (wght)**: 重量轴，范围 1-1000
- **自定义变化轴**: 支持添加自定义变化轴（Switch、SeekBar、EditText 三种类型）
- **变化轴编辑器**: 直接编辑字体变化设置字符串

#### 字体特性（Font Features）
- **CHWS**: 上下文替代特性开关
- **HALT**: 半角替代特性开关
- **FRAC**: 分数形式特性开关
- **自定义特性**: 支持添加自定义字体特性
- **特性编辑器**: 直接编辑字体特性设置字符串

### 3. UI/UX 特性

#### Edge-to-Edge 支持
- 全面支持全面屏手势导航
- 正确处理系统栏 insets（状态栏、导航栏、切口区域）
- 使用 `WindowCompat.setDecorFitsSystemWindows(window, false)` 实现沉浸式体验

#### 响应式布局
- 支持多种屏幕尺寸断点：
  - `layout-sw600dp`: 小平板
  - `layout-sw905dp`: 中平板
  - `layout-sw1240dp`: 大平板/桌面

#### Material Design 3
- **主题**: `Theme.Material3.DayNight.NoActionBar`
- **动态取色**: Android 12+ 支持 Material You 动态色彩
- **亮暗色模式**: 完整的日夜模式支持
- **MD3 组件**:
  - MaterialToolbar
  - MaterialCardView
  - TextInputLayout (OutlinedBox 样式)
  - MaterialAlertDialogBuilder
  - SwitchPreference (MD3 圆角样式)
  - SeekBarPreference (MD3 样式)

## 主题系统

### 亮色主题 (`values/themes.xml`)

```xml
父主题：Theme.Material3.DayNight.NoActionBar
主要颜色：
- colorPrimary: cyan_500 (#00BCD4)
- colorSurface: cyan_500
- colorSecondary: pink_A200 (#FF4081)
- colorPrimaryContainer: cyan_200
- colorOnPrimaryContainer: cyan_700
```

### 暗色主题 (`values-night/themes.xml`)

```xml
父主题：Theme.Material3.DayNight.NoActionBar
主要颜色：
- colorPrimary: cyan_200 (#80DEEA)
- colorSurface: #1E1E1E
- colorSecondary: pink_A200
- colorPrimaryContainer: cyan_700
- colorOnPrimaryContainer: cyan_200
- colorSurfaceVariant: #49454F
```

### Material Theme Overlay

通过 `materialThemeOverlay` 应用 MD3 颜色叠加层，确保所有 MD3 组件正确应用主题色和动态色彩。

## 构建与发布

### 本地构建

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本（需要配置签名）
./gradlew assembleRelease
```

### GitHub Actions CI/CD

Workflow 文件：`.github/workflows/build.yml`

**触发方式**: 手动触发 (`workflow_dispatch`)

**构建流程**:
1. 检出代码
2. 设置 JDK 17
3. 解码签名密钥库（从 secrets）
4. 授予 gradlew 执行权限
5. 构建 arm64-v8a Release APK
6. 上传构建产物

**环境变量**（通过 GitHub Secrets 配置）:
- `KEYSTORE_FILE`: 密钥库路径
- `KEYSTORE_PASSWORD`: 密钥库密码
- `KEY_ALIAS`: 密钥别名
- `KEY_PASSWORD`: 密钥密码
- `KEYSTORE_BASE64`: Base64 编码的密钥库文件

**构建产物**: `app-release-arm64-v8a` artifact，包含签名的 APK 文件

## 关键实现细节

### 1. Fragment 引用修复

XML 布局中的 Fragment 引用已更新为正确的包名：
- `activity_main.xml`: `moe.echo.variablefonttest_n.MainFragment`
- `main_fragment_content.xml`: `moe.echo.variablefonttest_n.OptionsFragment`

### 2. 窗口 Insets 处理

使用 `WindowInsetsUtil.safeDrawing()` 安全地处理系统 insets，确保内容不被系统栏遮挡：
- AppBarLayout: 应用顶部、左右 insets 作为 padding
- FragmentContainerView: 应用左右 insets 作为 padding
- ListView: 应用底部 insets 作为 padding

### 3. 字体变化设置管理

使用 `MutableMap<String, String>` 存储字体变化和特性设置：
- `fontVariationSettings`: 存储变化轴设置
- `fontFeatureSettings`: 存储字体特性设置
- 通过 `toFeatures()` 扩展函数转换为 Android 系统格式

### 4. 动态偏好添加

支持运行时动态添加偏好项：
- 使用 `MaterialAlertDialogBuilder` 创建自定义对话框
- 支持三种类型：Switch、SeekBar、EditText
- 自动去重和重新排序偏好项

## 国际化

支持多语言：
- **简体中文**: `values-zh-rCN/strings.xml`
- **繁体中文**: `values-zh-rTW/values.xml`
- **默认语言**: 英文（`values/strings.xml`）

## 关于页面

- **源代码**: 链接到项目主页
- **翻译者**: 链接到翻译者列表

## 注意事项

1. **可变字体支持**: Android 8.0 (API 26) 及以上版本完整支持可变字体
2. **动态色彩**: Android 12 (API 31) 及以上版本支持 Material You 动态色彩
3. **签名配置**: Release 构建需要配置签名密钥库，可通过 GitHub Secrets 或本地配置
4. **ABI 过滤**: CI 仅构建 arm64-v8a 架构，其他架构需本地构建

## 相关资源

- [OpenType 可变字体规范](https://docs.microsoft.com/en-us/typography/opentype/spec/dvaraxisreg)
- [Google Fonts 可变字体](https://fonts.google.com/variablefonts)
- [Material Design 3 指南](https://m3.material.io/)
- [Android Edge-to-Edge 开发](https://developer.android.com/develop/ui/views/layout/edge-to-edge)
