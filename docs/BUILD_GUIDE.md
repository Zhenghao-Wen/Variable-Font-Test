# 构建与发布指南

本文档说明 Variable Font Test N 应用的构建、签名和发布流程。

## 系统要求

- **JDK**: 17 或更高版本
- **Android SDK**: API 35 (Android 15)
- **Gradle**: 项目自带的 Gradle Wrapper
- **操作系统**: Windows/macOS/Linux

## 本地构建

### 环境准备

1. **安装 JDK 17**
   ```bash
   # Ubuntu/Debian
   sudo apt install openjdk-17-jdk
   
   # macOS (使用 Homebrew)
   brew install openjdk@17
   
   # Windows
   # 从 https://adoptium.net/ 下载 Temurin JDK 17
   ```

2. **配置 JAVA_HOME**
   ```bash
   # Linux/macOS
   export JAVA_HOME=/path/to/jdk-17
   
   # Windows (PowerShell)
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
   ```

3. **验证安装**
   ```bash
   java -version
   # 应显示：openjdk version "17.x.x"
   ```

### Debug 构建

无需签名即可构建 Debug 版本：

```bash
# 进入项目目录
cd /workspace

# 授予执行权限（仅首次）
chmod +x gradlew

# 构建 Debug APK
./gradlew assembleDebug
```

**输出位置**: `app/build/outputs/apk/debug/app-debug.apk`

**特点**:
- 自动签名（使用 Android Debug Keystore）
- 未优化，包含调试信息
- 可直接安装到开发设备

### Release 构建

Release 版本需要配置签名。

#### 方法一：本地配置签名

1. **创建或获取密钥库**
   ```bash
   # 创建新密钥库（如果没有）
   keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
   ```

2. **配置 gradle.properties**
   
   在项目根目录创建 `gradle.properties`（不提交到 Git）：
   ```properties
   KEYSTORE_FILE=/absolute/path/to/release-key.jks
   KEYSTORE_PASSWORD=your_keystore_password
   KEY_ALIAS=your_alias
   KEY_PASSWORD=your_key_password
   ```

3. **修改 build.gradle.kts**（可选，如果尚未配置）
   
   当前项目已在 `app/build.gradle.kts` 中配置了签名：
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file(System.getenv("KEYSTORE_FILE") ?: "/dev/null")
           storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
           keyAlias = System.getenv("KEY_ALIAS") ?: ""
           keyPassword = System.getenv("KEY_PASSWORD") ?: ""
       }
   }
   ```

4. **构建 Release APK**
   ```bash
   # 设置环境变量并构建
   export KEYSTORE_FILE=/path/to/release-key.jks
   export KEYSTORE_PASSWORD=your_password
   export KEY_ALIAS=your_alias
   export KEY_PASSWORD=your_key_password
   
   ./gradlew assembleRelease
   ```

#### 方法二：命令行参数

```bash
./gradlew assembleRelease \
  -PKEYSTORE_FILE=/path/to/release-key.jks \
  -PKEYSTORE_PASSWORD=your_password \
  -PKEY_ALIAS=your_alias \
  -PKEY_PASSWORD=your_key_password
```

**输出位置**: `app/build/outputs/apk/release/app-release.apk`

**特点**:
- 需要有效签名
- 已优化（如果启用 minify）
- 可发布到应用商店

## GitHub Actions CI/CD

### Workflow 配置

文件位置：`.github/workflows/build.yml`

**触发方式**: 手动触发 (`workflow_dispatch`)

**构建产物**: arm64-v8a 架构的 Release APK

### 配置 Secrets

在 GitHub 仓库设置中配置以下 Secrets：

1. **进入 Settings → Secrets and variables → Actions**

2. **添加 Repository secrets**:

| Secret 名称 | 说明 | 示例值 |
|------------|------|--------|
| `KEYSTORE_BASE64` | Base64 编码的密钥库文件 | `$(base64 -i release-key.jks)` |
| `KEYSTORE_PASSWORD` | 密钥库密码 | `your_keystore_password` |
| `KEY_ALIAS` | 密钥别名 | `your_alias` |
| `KEY_PASSWORD` | 密钥密码 | `your_key_password` |

### 生成 KEYSTORE_BASE64

```bash
# macOS/Linux
base64 -i release-key.jks | pbcopy  # macOS，直接复制到剪贴板

# 或输出到文件
base64 -i release-key.jks > keystore.base64

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release-key.jks")) | Set-Clipboard
```

### 手动触发构建

1. 进入 GitHub 仓库页面
2. 点击 **Actions** 标签
3. 选择 **Build Release APK** workflow
4. 点击 **Run workflow** 按钮
5. 选择分支（通常为 `main`）
6. 点击 **Run workflow**

### 下载构建产物

1. 等待构建完成（绿色勾）
2. 点击 workflow 运行记录
3. 在页面底部找到 **Artifacts** 部分
4. 点击 `app-release-arm64-v8a` 下载 APK

**注意**: Artifacts 保留 90 天

## 架构支持

### 当前 CI 构建

GitHub Actions 仅构建 **arm64-v8a** 架构：
- ✅ arm64-v8a (ARM 64 位)
- ❌ armeabi-v7a (ARM 32 位)
- ❌ x86 (Intel 32 位)
- ❌ x86_64 (Intel 64 位)

### 本地构建所有架构

如需构建所有架构的 APK：

```bash
# 构建所有架构的 Release APK
./gradlew assembleRelease

# 或构建特定架构
./gradlew assembleRelease -PabiFilters=armeabi-v7a,arm64-v8a,x86,x86_64
```

**输出位置**: `app/build/outputs/apk/release/`
- `app-universal-release.apk` (包含所有架构)
- 或按架构分离的 APK 文件

## 安装测试

### ADB 安装

```bash
# 连接设备后
adb install app/build/outputs/apk/debug/app-debug.apk

# 覆盖安装（保留数据）
adb install -r app/build/outputs/apk/release/app-release.apk

# 卸载应用
adb uninstall moe.echo.variablefonttest_n
```

### 无线调试

```bash
# 启用无线调试（设备上）
adb tcpip 5555

# 连接设备
adb connect DEVICE_IP:5555

# 安装应用
adb install app-release.apk
```

## 版本管理

### 版本号配置

在 `app/build.gradle.kts` 中修改：

```kotlin
android {
    defaultConfig {
        versionCode = 20      // 内部版本号（整数，递增）
        versionName = "3.7"   // 用户可见版本（语义化版本）
    }
}
```

### 版本命名规范

- **versionCode**: 每次发布递增 1
- **versionName**: 遵循语义化版本 (MAJOR.MINOR.PATCH)
  - MAJOR: 重大更新，可能不兼容
  - MINOR: 新功能，向后兼容
  - PATCH: Bug 修复，向后兼容

## 代码混淆（可选）

当前配置未启用代码混淆。如需启用：

1. **修改 build.gradle.kts**
   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = true
           proguardFiles(
               getDefaultProguardFile("proguard-android-optimize.txt"),
               "proguard-rules.pro"
           )
       }
   }
   ```

2. **配置 ProGuard 规则** (`app/proguard-rules.pro`)
   ```proguard
   # 保留 Kotlin 相关
   -keep class kotlin.** { *; }
   
   # 保留 Preference
   -keep class androidx.preference.** { *; }
   
   # 保留 Material Components
   -keep class com.google.android.material.** { *; }
   ```

## 故障排查

### 问题：签名失败

**错误信息**: `Keystore file does not exist` 或 `Invalid keystore password`

**解决方案**:
1. 检查密钥库路径是否正确（使用绝对路径）
2. 验证密码是否正确
3. 确认密钥库文件格式正确（JKS 或 BKS）

### 问题：构建内存不足

**错误信息**: `OutOfMemoryError` 或 `GC overhead limit exceeded`

**解决方案**:
```bash
# 增加 Gradle 堆内存
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m"
./gradlew assembleRelease
```

或在 `gradle.properties` 中添加：
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
```

### 问题：SDK 未安装

**错误信息**: `Failed to install the following Android SDK packages`

**解决方案**:
```bash
# 使用 sdkmanager 安装缺失的 SDK
sdkmanager "platforms;android-35" "build-tools;35.0.0"
```

或通过 Android Studio 的 SDK Manager 安装。

### 问题：Gradle 同步失败

**错误信息**: `Could not resolve all dependencies`

**解决方案**:
1. 检查网络连接
2. 清除 Gradle 缓存：
   ```bash
   ./gradlew clean --refresh-dependencies
   ```
3. 检查 `settings.gradle.kts` 中的仓库配置

## 发布检查清单

在发布新版本前，请确认：

- [ ] 版本号已更新（versionCode 和 versionName）
- [ ] 所有功能已测试通过
- [ ] 无崩溃和严重 Bug
- [ ] Release 构建成功
- [ ] APK 签名有效
- [ ] 更新日志已编写
- [ ] 源代码已提交和推送
- [ ] Git 标签已创建（可选）

```bash
# 创建 Git 标签
git tag -a v3.7 -m "Release version 3.7"
git push origin v3.7
```

## 相关资源

- [Android 应用签名](https://developer.android.com/studio/publish/app-signing)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Gradle 构建优化](https://developer.android.com/studio/build/optimize-your-build)
- [ProGuard 使用指南](https://www.guardsquare.com/proguard)
