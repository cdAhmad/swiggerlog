# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# 保护库内的所有数据类，防止 Gson 反序列化失败
-keep class com.cdahmad.swiggerlog.SwaggerDoc** { *; }
-keep class com.cdahmad.swiggerlog.SwaggerDoc$** { *; }

# 确保辅助类不会被删除
-keep class com.cdahmad.swiggerlog.ObfuscateHelper { *; }