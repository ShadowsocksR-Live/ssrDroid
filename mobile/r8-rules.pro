-keepnames class kotlin.Pair
-allowaccessmodification
-repackageclasses
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-assumenosideeffects class android.util.Log {
    public static int d(...);
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}
