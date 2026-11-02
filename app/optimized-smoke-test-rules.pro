# Error Prone's annotation-only compiler enum is absent from the Android runtime.
# This applies only to the instrumentation APK, never to the optimized app.
-dontwarn javax.lang.model.element.Modifier
-keep class com.steadfast.app.smoke.OptimizedSmokeRunner { *; }
