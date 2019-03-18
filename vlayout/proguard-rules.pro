# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/villadora/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes InnerClasses
-keep class androidx.recyclerview.widget.RecyclerView$LayoutParams {
    *;
}

-keep class androidx.recyclerview.widget.RecyclerView$ViewHolder {
    *;
}

-keep class android.support.v7.widget.ChildHelper {
    *;
}

-keep class androidx.recyclerview.widget.RecyclerView$LayoutManager {
    *;
}

-keep class androidx.recyclerview.widget.LinearLayoutManager {
    void ensureLayoutState();
    void resolveShouldLayoutReverse();
}