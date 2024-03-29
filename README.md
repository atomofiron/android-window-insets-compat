[![](https://jitpack.io/v/atomofiron/android-window-insets-compat.svg)](https://jitpack.io/#atomofiron/android-window-insets-compat)

# Demo
![screenshot](https://github.com/Atomofiron/android-window-insets-compat/blob/main/stuff/insets_demo.png)

# How to use for example
```gradle
repositories {
    // ...
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.atomofiron:android-window-insets-compat:1.1.1'
}
```

```kotlin
fun onViewCreated(view: View) {

    // ...

    view as ViewGroup

    view.insetsProxying()
    
    swipeRefreshLayout.insetsProxying()
    
    appBar.applyPaddingInsets(start = true, top = true, end = true)
    
    recyclerView.applyPaddingInsets()
    
    bottomBar.applyPaddingInsets(horizontal = true, withProxying = true)
    
    fab.applyPaddingInsets(end = true, bottom = true)
}
```

# How and why it works
[<img width="645" alt="Window Insets" src="https://github.com/atomofiron/android-window-insets-compat/assets/14147217/384b2c52-1145-4a53-867c-ed17baf2471e">](https://www.youtube.com/watch?v=hJveJ8MtMJ4 "Window Insets")
