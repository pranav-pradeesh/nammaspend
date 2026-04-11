# ProGuard rules for NammaSpend AI

# Keep Room entities
-keep class com.nammaspendai.data.model.** { *; }

# Keep Retrofit models
-keep class com.nammaspendai.data.remote.** { *; }

# Apache POI
-dontwarn org.apache.**
-keep class org.apache.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Hilt
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    @dagger.hilt.android.lifecycle.HiltViewModel *;
}
