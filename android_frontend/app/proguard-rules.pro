# Preserve runtime JSON models. Gson reads these fields reflectively.
-keepattributes Signature,*Annotation*
-keep class com.vocalbharat.app.data.model.** { *; }

# Gson's TypeToken reads its generic type at runtime. In full R8 mode, retain
# the TypeToken class and generated anonymous subclasses as signature endpoints.
-if class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowoptimization class com.google.gson.reflect.TypeToken
-if class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowoptimization class <1>

# Razorpay loads selected integration classes dynamically.
-keep class com.razorpay.** { *; }
