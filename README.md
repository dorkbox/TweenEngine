![logo](https://git.dorkbox.com/dorkbox/TweenEngine-demo/raw/branch/master/src/tween-engine-big-logo.jpg)

Forked from [JavaUniversalTweenEngine](http://www.aurelienribon.com/blog/projects/universal-tween-engine), by Aurelien Ribon

###### [![Dorkbox](https://badge.dorkbox.com/dorkbox.svg "Dorkbox")](https://git.dorkbox.com/dorkbox/TweenEngine) [![Github](https://badge.dorkbox.com/github.svg "Github")](https://github.com/dorkbox/TweenEngine) [![Gitlab](https://badge.dorkbox.com/gitlab.svg "Gitlab")](https://gitlab.com/dorkbox/TweenEngine)

# Check out the demo! #

  * [Android application](https://play.google.com/store/apps/details?id=aurelienribon.tweenengine.demo)
  * [Desktop application](https://xxxxxxxxx)
  * [WebGL html5 page](http://www.aurelienribon.com/universal-tween-engine/gwt/demo.html) (requires a WebGL enabled browser)

# Changelog #

See the [Changelog](https://github.com/dorkbox/TweenEngine/wiki) page.

# Introduction #

The Tween Engine enables the interpolation of every attribute from any object in any Java project (being Swing, SWT, OpenGL or even Console-based). Implement the TweenAccessor interface, register it to the engine, and animate anything you want!

In one line, send your objects to another position (here x=20 and y=30), with a smooth elastic transition, during 1 second).
```
 
val engine = TweenEngine.build()

// Arguments are (1) the target, (2) the type of interpolation,
// and (3) the duration in seconds. Additional methods specify
// the target values, and the easing function.

engine.to(mySprite, Type.POSITION_XY, 1.0f).value(20, 30).ease(Elastic.INOUT);

// Possibilities are:

myTween = engine.to(...); // interpolates from the current values to the targets
myTween = engine.from(...); // interpolates from the given values to the current ones
myTween = engine.set(...); // apply the target values without animation (useful with a delay)
myTween = engine.call(...); // calls a method (useful with a delay)

// Current options are:

callback.setTriggers(flags);

myTween.delay(0.5f);
myTween.repeat(2, 0.5f);
myTween.repeatAutoReverse(2, 0.5f);
myTween.pause();
myTween.resume();
myTween.addCallback(callback);
myTween.setUserData(obj);

// You can of course chain everything:

engine.to(...).delay(1.0f).repeat(2, 0.5f).start();

// Moreover, slow-motion, fast-motion and reverse play is easy,
// you just need to change the speed of the update:

engine.update(delta * speed);
```

Create some powerful animation sequences!
```
val engine = TweenEngine.build()
engine.createSequence()
    // First, set all objects to their initial positions
    .push(engine.set(...))
    .push(engine.set(...))
    .push(engine.set(...))

    // Wait 1s
    .pushPause(1.0f)

    // Move the objects around, one after the other
    .push(engine.to(...))
    .push(engine.to(...))
    .push(engine.to(...))

    // Then, move the objects around at the same time
    .beginParallel()
        .push(engine.to(...))
        .push(engine.to(...))
        .push(engine.to(...))
    .end()

    // And repeat the whole sequence 2 times
    // with a 0.5s pause between each iteration
    .repeatAutoReverse(2, 0.5f)

    // Let's go!
    .start();
```

You can also quickly create timers:
```
TweenEngine.build().call(myCallback).delay(3000).start();
```

Main features are:

  * Supports every interpolation function defined by Robert Penner: http://www.robertpenner.com/easing/
  * Can be used with any object. You just have to implement the TweenAccessor interface when you want interpolation capacities.
  * Any attribute can be interpolated. The only requirement is that what you want to interpolate can be represented as a float number.
  * One line is sufficient to create and start a simple interpolation.
  * Delays can be specified, to trigger the interpolation only after some time.
  * Many callbacks can be specified (when tweens complete, start, end, etc.).
  * Tweens and Timelines are pooled - there won't be any object allocation during runtime! You can safely use it in Android game
  development without fearing the garbage collector.
  * Tweens can be sequenced when used in Timelines.
  * Tweens can be run in parallel when used in Timelines.
  * Tweens can act on more than one value at a time, so a single tween can change the whole position (X and Y) of a sprite for instance!
  * Tweens and Timelines can have their "current" position (as a percentage) specified.
  * Tweens and Timelines can be repeated, and can automatically auto-reverse for a smooth "back-and-forth" animation.
  * Simple timers can be built with Tween.call().
  * Source code extensively documented!
  * All Tweens and Timelines are ThreadSafe.

# Get started and documentation index #

Detailed documentation with code snippets and examples is available for the following topics:
  * [Get started](https://github.com/dorkbox/TweenEngine/wiki/GetStarted) --- A step-by-step example to get you started, with code
  * [The TweenAccessor interface](https://github.com/dorkbox/TweenEngine/wiki/TweenAccessor) --- Know how to implement it
  * [Tweens and options](https://github.com/dorkbox/TweenEngine/wiki/Tween) --- See what are the possibilities
  * [Timelines and options](https://github.com/dorkbox/TweenEngine/wiki/Timeline) --- Learn how to build powerful sequences
  * [Animating Android apps](https://github.com/dorkbox/TweenEngine/wiki/AndroidUI) --- See how to use the TweenEngine with Android UIs

# Where can I ask for help? #

**Use github issues**

&nbsp; 
&nbsp; 

Maven Info
---------
```
<dependencies>
    ...
    <dependency>
      <groupId>com.dorkbox</groupId>
      <artifactId>TweenEngine</artifactId>
      <version>9.2</version>
    </dependency>
</dependencies>
```

Gradle Info
---------
````
dependencies {
    ...
    compile "com.dorkbox:TweenEngine:8.3"
}
````

Or if you don't want to use Maven, you can access the files directly here:  
https://repo1.maven.org/maven2/com/dorkbox/TweenEngine/
  
https://repo1.maven.org/maven2/com/dorkbox/ObjectPool/    
https://repo1.maven.org/maven2/org/slf4j/slf4j-api/    


License
---------
This project is © 2015 dorkbox llc, and is distributed under the terms of the Apache v2.0 License. See file "LICENSE" for further references.
