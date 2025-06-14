-keep public class MainKt { public static final void main(java.lang.String[]); }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material.icons.** { *; }
-keep class org.jetbrains.skiko.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod
-dontwarn org.jetbrains.skia.**
-dontwarn org.jetbrains.skiko.context.**
-dontwarn org.jetbrains.skiko.redrawer.**
-dontwarn javax.swing.**
-dontwarn java.awt.**
