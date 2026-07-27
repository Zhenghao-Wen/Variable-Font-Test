# 应用图标升级方案：MD2 → MD3 自适应图标

## 项目背景
本项目需要从 MD2 风格的静态图标升级为 MD3 风格的自适应图标，支持：
- 新前景图案（基于提供的 SVG 路径）
- 跟随系统亮色/暗色模式（Android 10+）
- 跟随系统壁纸动态取色（Android 12+）
- 多版本 fallback 机制（Android 9 及以下使用静态位图）

---

## 一、需要处理的文件清单

### 1.1 核心图标定义文件（必须修改）

| 文件路径 | 说明 | 操作 |
|---------|------|------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 自适应图标前景矢量图 | **重写**：将现有旧图案替换为新 SVG 路径转换的 VectorDrawable |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Android 8.0+ 自适应图标定义 | **保留**：结构无需修改，已正确引用背景和前景 |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Android 8.0+ 圆形自适应图标定义 | **保留**：结构无需修改，已正确引用背景和前景 |

### 1.2 颜色资源文件（必须修改/新增）

| 文件路径 | 说明 | 操作 |
|---------|------|------|
| `app/src/main/res/values/ic_launcher_background.xml` | 定义图标背景色 | **扩展**：添加 MD3 PrimaryContainer 颜色定义作为默认 fallback |
| `app/src/main/res/values/colors.xml` | 通用颜色资源 | **新增**：添加 MD3 Baseline 亮色模式 Primary (#6750A4) 和 PrimaryContainer (#EADDFF) |
| `app/src/main/res/values-night/ic_launcher_background.xml` | 暗色模式图标背景色 | **新建**：定义暗色模式下 PrimaryContainer (#4F378B) |
| `app/src/main/res/values-night/colors.xml` | 暗色模式通用颜色 | **新增**：添加 MD3 Baseline 暗色模式 Primary (#D0BCFF) 和 PrimaryContainer (#4F378B) |

### 1.3 动态取色资源目录（必须新建）

| 文件路径 | 说明 | 操作 |
|---------|------|------|
| `app/src/main/res/values-v31/ic_launcher_background.xml` | Android 12+ 动态取色背景 | **新建**：使用 `@android:color/system_accent1_300` (PrimaryContainer) |
| `app/src/main/res/values-v31/colors.xml` | Android 12+ 动态取色前景 | **新建**：使用 `@android:color/system_accent1_600` (Primary) |
| `app/src/main/res/values-night-v31/ic_launcher_background.xml` | Android 12+ 暗色动态取色背景 | **新建**：使用 `@android:color/system_accent1_200` |
| `app/src/main/res/values-night-v31/colors.xml` | Android 12+ 暗色动态取色前景 | **新建**：使用 `@android:color/system_accent1_500` |

### 1.4 遗留位图图标文件（保留作为 fallback）

以下文件需**保留**，用于 Android 7.x 及以下或不支持自适应图标的设备 fallback：

| 目录 | 文件 |
|------|------|
| `app/src/main/res/mipmap-mdpi/` | `ic_launcher.webp`, `ic_launcher_round.webp` |
| `app/src/main/res/mipmap-hdpi/` | `ic_launcher.webp`, `ic_launcher_round.webp` |
| `app/src/main/res/mipmap-xhdpi/` | `ic_launcher.webp`, `ic_launcher_round.webp` |
| `app/src/main/res/mipmap-xxhdpi/` | `ic_launcher.webp`, `ic_launcher_round.webp` |
| `app/src/main/res/mipmap-xxxhdpi/` | `ic_launcher.webp`, `ic_launcher_round.webp` |

### 1.5 应用清单文件（无需修改）

| 文件路径 | 说明 | 操作 |
|---------|------|------|
| `app/src/main/AndroidManifest.xml` | 应用清单 | **保留**：已正确引用 `@mipmap/ic_launcher` 和 `@mipmap/ic_launcher_round` |

---

## 二、当前文件内容快照

### 2.1 前景矢量图（待重写）
**文件**: `app/src/main/res/drawable/ic_launcher_foreground.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="168"
    android:viewportHeight="215.22656">
  <group android:scaleX="0.35906348"
      android:scaleY="0.46"
      android:translateX="53.838665"
      android:translateY="58.11117">
    <group android:translateY="173.03906">
      <path android:pathData="M0,-102.375L38.25,0.703125L50.34375,0.703125L88.59375,-102.375L74.8125,-102.375L47.390625,-26.578125Q46.546875,-24.265625,45.953125,-22.46875Q45.359375,-20.671875,44.71875,-18.5625L44.09375,-18.5625Q43.453125,-20.671875,42.890625,-24.265625Q42.328125,-24.265625,41.484375,-26.578125L14.34375,-102.375L0,-102.375Z"
          android:fillColor="#000000"/>
      <path android:pathData="M100.953125,0L114.453125,0L114.453125,-45.21875L157.42188,-45.21875L157.42188,-56.328125L114.453125,-56.328125L114.453125,-91.265625L164.375,-91.265625L164.375,-102.375L100.953125,-102.375L100.953125,0Z"
          android:fillColor="#000000"/>
    </group>
  </group>
</vector>
```

### 2.2 自适应图标定义（保留）
**文件**: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

**文件**: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

### 2.3 当前背景色定义（待扩展）
**文件**: `app/src/main/res/values/ic_launcher_background.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#E0E0E0</color>
</resources>
```

### 2.4 当前颜色资源
**文件**: `app/src/main/res/values/colors.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    
    <color name="cyan_200">#80DEEA</color>
    <color name="cyan_500">#00BCD4</color>
    <color name="cyan_700">#0097A7</color>
    <color name="pink_A200">#FF4081</color>
    <color name="pink_500">#E91E63</color>
    
    <color name="preview_content_background">@color/white</color>
</resources>
```

**文件**: `app/src/main/res/values-night/colors.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="preview_content_background">#303030</color>
</resources>
```

---

## 三、目标新图标规格

### 3.1 新前景图案 SVG
```svg
<svg xmlns="http://www.w3.org/2000/svg" width="192" height="192" fill="none">
  <path stroke="#000" stroke-linecap="round" stroke-width="14" 
        d="M59 142 L23 54m36 88 l36-88m28 88V60a6 6 0 0 1 6-6h40m-46 44h38"/>
</svg>
```

### 3.2 MD3 颜色规范（Baseline Purple）

| 模式 | Primary (前景) | PrimaryContainer (背景) |
|------|----------------|------------------------|
| 亮色静态 fallback | `#6750A4` | `#EADDFF` |
| 暗色静态 fallback | `#D0BCFF` | `#4F378B` |
| Android 12+ 动态亮色 | `@android:color/system_accent1_600` | `@android:color/system_accent1_300` |
| Android 12+ 动态暗色 | `@android:color/system_accent1_500` | `@android:color/system_accent1_200` |

---

## 四、API 分级 Fallback 策略

| API Level | 前景颜色来源 | 背景颜色来源 | 图标类型 |
|-----------|-------------|-------------|----------|
| 31+ (Android 12+) | 动态取色 Primary | 动态取色 PrimaryContainer | 自适应图标 |
| 29-30 (Android 10-11) | MD3 Baseline Primary | MD3 Baseline PrimaryContainer | 自适应图标 |
| 26-28 (Android 8-9) | MD3 Baseline Primary (亮色) | MD3 Baseline PrimaryContainer (亮色) | 自适应图标 |
| 21-25 (Android 5-7) | 位图内置颜色 | 位图内置颜色 | 传统位图 fallback |

**注意**: 
- Android 10 (API 29) 开始支持系统级暗色模式
- Android 12 (API 31) 开始支持 Material You 动态取色
- 本项目的 `minSdk = 21`, `targetSdk = 35`

---

## 五、待执行任务总结

### 5.1 必须修改的文件
1. `app/src/main/res/drawable/ic_launcher_foreground.xml` - 重写为新的 SVG 路径
2. `app/src/main/res/values/ic_launcher_background.xml` - 更新为 MD3 PrimaryContainer
3. `app/src/main/res/values/colors.xml` - 新增 MD3 Primary 颜色定义

### 5.2 必须新建的文件
1. `app/src/main/res/values-night/ic_launcher_background.xml` - 暗色模式背景色
2. `app/src/main/res/values-night/colors.xml` - 暗色模式 MD3 颜色
3. `app/src/main/res/values-v31/ic_launcher_background.xml` - Android 12+ 动态背景
4. `app/src/main/res/values-v31/colors.xml` - Android 12+ 动态前景
5. `app/src/main/res/values-night-v31/ic_launcher_background.xml` - Android 12+ 暗色动态背景
6. `app/src/main/res/values-night-v31/colors.xml` - Android 12+ 暗色动态前景

### 5.3 保留不变的文件
1. `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
2. `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
3. `app/src/main/AndroidManifest.xml`
4. 所有 `mipmap-*/` 目录下的位图 fallback 文件

---

## 六、技术要点

- **项目配置**: minSdk = 21, targetSdk = 35, compileSdk = 35
- **主题**: Material 3 (`Theme.Material3.DayNight.NoActionBar`)
- **放弃上架 Google Play**: 无需考虑 Play Store 的图标规范限制
- **矢量图适配**: 新 SVG 路径需要转换为适合 108dp 自适应图标画布的 VectorDrawable，注意 viewport 和缩放比例

---

*文档生成时间: 供专业模型进行方案设计参考*
