#Discrete Slider 

##OVERVIEW
Discrete Slider is an attempt to implement material style [Discrete Slider]. This project was forked from  AnderWeb’s [discreteSeekBar] and made quiet a bit of modifications to make it comply with [Discrete Slider] component from the Google Material Design Guidelines.

##GOALS
The goal of this attempt was to Create a component which could exactly replicate the purpose of [Discrete Slider] 
*  Comply with and create a component which can be used as Material Style Slider (both Continuous slider and Discrete slider.)
*  Provide compatibility with maximum possible devices

##SPECIFICATIONS
Supports Android SDK 7 and above
Dependencies : **com.android.support:support-v4**

##STYLEABLE ATTRIBUTES
###Attributes

* **dsb_min** (integer/dimension) : minimum value
* **dsb_max** (integer/dimension) : maximum value
* **dsb_value** (integer/dimension) : current value
* **dsb_mirrorFroRTL** (boolean) : reverse the Discrete Slider for RTL locales
* **dsb_allowTrackClickToDrag** (boolean) : allows clicking outside the thumb circle to initiate drag. Default TRUE
* **dsb_indicatorFormatter** (string/reference) : a string Format to apply to the value inside the bubble indicator.
###Design

* **dsb_progressColor** (color/reference) : color/colorStateList for the progress bar and thumb drawable
* **dsb_trackColor** (color/reference) : color/colorStateList for the track drawable
* **dsb_indicatorTextAppearance** (reference) : TextAppearance for the bubble indicator
* **dsb_indicatorColor** (color/reference) : color/colorStateList for the bubble shaped drawable
* **dsb_indicatorElevation** (dimension) : related to android:elevation. Will only be used on API level 21+
* **dsb_rippleColor** (color/reference) : color/colorStateList for the ripple drawable seen when pressing the thumb. (Yes, it does a kind of "ripple" on API levels lower than 21 and a real RippleDrawable for 21+.
* **dsb_indicatorPopupEnabled** (boolean) : 
* **dsb_indicatorTextFromArray** (boolean) : 
* **dsb_discretePointsEnabled** (boolean) : 
* **dsb_discretePointsShowAlways** (boolean) : 
* **dsb_discretePointsColor**  (color) : 
* **dsb_discretePointsSize** (integer/dimension) : 
* **dsb_textColor** (color) : 
* **dsb_TextSize** (dimension) : 
* **dsb_TextPaddingTop** (dimension) : 
* **dsb_TextStyle** (enum) : 
       * none
       * odd
       * even
       * extremes
       * all


![screenshot](https://lh6.googleusercontent.com/-JjvxVMCm1ug/VHUPWVBfpbI/AAAAAAAAHtQ/TPtoOjHI5MA/w639-h356/seekbar2.gif)

![screenshot](https://lh3.googleusercontent.com/-7nbVPXxUhYk/VG-rO64pMWI/AAAAAAAAHsM/aMRglt2Vzrk/w639-h480/animation.gif)

##USAGE
<!--
This is published in jCenter so you need to use the appropiate repo:

```groovy
repositories {
    jcenter()
}

dependencies {
    compile 'in.sc9:discrete-slider:1.0.0'
}
```
-->

Once imported into your project, you just need to put them into your layous like:

```xml
<in.sc9.adw.library.widgets.discreteseekbar.DiscreteSeekBar
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:dsb_min="2"
        app:dsb_max="15"
/>
```


##LICENSE
```
Copyright 2016 Sony Cyriac

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[Discrete Slider]:http://www.google.com/design/spec/components/sliders.html#sliders-discrete-slider
[VectorDrawableMagic]:https://developer.android.com/reference/android/graphics/drawable/AnimatedVectorDrawable.html
[Animatable Drawable]:https://developer.android.com/reference/android/graphics/drawable/Animatable.html
[PopupWindow]:https://developer.android.com/reference/android/widget/PopupWindow.html
[Format]:https://developer.android.com/reference/java/util/Formatter.html
[discreteSeekBar]:https://github.com/AnderWeb/discreteSeekBar
