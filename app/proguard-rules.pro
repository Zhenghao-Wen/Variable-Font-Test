# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============================================================
# R8 相关规则（仅 r8 构建类型会真正启用压缩，release 不受影响）
# ============================================================

# 保留行号等信息，便于线上崩溃堆栈定位（对体积影响很小）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 通过 XML 按类名反射实例化的自定义 View / Preference / Fragment
# （PreferenceInflater、FragmentContainerView、LayoutInflater 均依赖类名）
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep public class * extends androidx.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keep class moe.echo.variablefonttest_n.SeekBarPreference { *; }
-keep class moe.echo.variablefonttest_n.SliderPreference { *; }
-keep class moe.echo.variablefonttest_n.MainFragment { *; }
-keep class moe.echo.variablefonttest_n.OptionsFragment { *; }

# 应用自身代码量很小，整体保留以最大限度避免反射/序列化相关的运行时回归，
# 体积收益主要来自依赖库裁剪与资源压缩
-keep class moe.echo.variablefonttest_n.** { *; }

# Parcelable / Serializable 的状态保存
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
