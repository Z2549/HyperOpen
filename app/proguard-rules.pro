-keep class com.hyperos.hyperopen.MainHook { *; }
-keep class com.hyperos.hyperopen.** { *; }
-keepclassmembers class com.hyperos.hyperopen.** { *; }

# 保留 Miuix 反射调用的图标扩展属性
-keep class top.yukonga.miuix.kmp.icon.** { *; }
-keepclassmembers class top.yukonga.miuix.kmp.icon.** { *; }
-keep class top.yukonga.miuix.kmp.icon.extended.** { *; }
-keepclassmembers class top.yukonga.miuix.kmp.icon.extended.** { *; }

# 保留 Compose 运行时规则
-keepattributes *Annotation*
-dontwarn de.robv.android.xposed.**
