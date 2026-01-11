# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# --- Firebase General ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# --- Firebase Auth & Firestore ---
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# --- Glide ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# --- Preserve your own data models (ShoppingItem, ShoppingList) ---
# This is crucial because Firestore uses reflection to map documents to these classes.
-keep class com.CapyCode.ShoppingList.ShoppingItem { *; }
-keep class com.CapyCode.ShoppingList.ShoppingList { *; }
-keep class com.CapyCode.ShoppingList.User { *; }

# --- Remove Logs in Release ---
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# If you have other POJOs/Data models, add them here.
