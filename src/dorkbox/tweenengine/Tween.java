/*
 * Copyright 2012 Aurelien Ribon
 * Copyright 2015 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.tweenengine;

import java.util.HashMap;
import java.util.Map;

import dorkbox.objectPool.ObjectPool;
import dorkbox.objectPool.PoolableObject;

/**
 * Core class of the Tween Engine. A Tween is basically an interpolation between two values of an object attribute. However, the main
 * interest of a Tween is that you can apply an easing formula on this interpolation, in order to smooth the transitions or to achieve
 * cool effects like springs or bounces.
 * <p/>
 *
 * The Tween Engine is also "universal" because it is able to apply interpolations on every attribute from every possible object. Therefore,
 * every object in your application can be animated with cool effects: it does not matter if your application is a game, a desktop
 * interface or even a console program! If it makes sense to animate something, then it can be animated through this engine.
 * <p/>
 *
 * This class contains many static factory methods to create and instantiate new interpolations easily. The common way to create a Tween is
 * by using one of these factories:
 * <p/>
 *
 * - Tween.to(...)<br/>
 * - Tween.from(...)<br/>
 * - Tween.set(...)<br/>
 * - Tween.call(...)
 * <p/>
 *
 * <h2>Example - firing a Tween</h2>
 *
 * The following example will move the target horizontal position from its current value to x=200 and y=300, during 500ms, but only after
 * a delay of 1000ms. The animation will also be repeated 2 times (the starting position is registered at the end of the delay, so the
 * animation will automatically restart from this registered position).
 * <p/>
 *
 * <pre> {@code
 * Tween.to(myObject, POSITION_XY, 500)
 *      .target(200, 300)
 *      .ease(Quad_InOut)
 *      .delay(1.0F)
 *      .repeat(2, 0.02F)
 *      .start(myManager);
 * }</pre>
 *
 * Tween life-cycles can be automatically managed for you, thanks to the {@link TweenManager} class. If you choose to manage your tween
 * when you start it, then you don't need to care about it anymore. <b>Tweens are <i>fire-and-forget</i>: don't think about them anymore
 * once you started them (if they are managed of course).</b>
 * <p/>
 *
 * You need to periodically update the tween engine, in order to compute the new values. If your tweens are managed, only update the
 * manager; else you need to call {@link Tween#update(float)} on your tweens periodically.
 * <p/>
 *
 * <h2>Example - setting up the engine</h2>
 *
 * The engine cannot directly change your objects attributes, since it doesn't know them. Therefore, you need to tell him how to get and
 * set the different attributes of your objects: <b>you need to implement the {@link TweenAccessor} interface for each object class you
 * will animate</b>. Once done, don't forget to register these implementations, using the static method
 * {@link Tween#registerAccessor(Class, TweenAccessor)}, when you start your application.
 *
 * @see TweenAccessor
 * @see TweenManager
 * @see TweenEquation
 * @see Timeline
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final
class Tween extends BaseTween<Tween> {
    // -------------------------------------------------------------------------
    // Static -- misc
    // -------------------------------------------------------------------------

    /**
     * Used as parameter in {@link #repeat(int, float)} and {@link #repeatAutoReverse(int, float)} methods.
     */
    public static final int INFINITY = -1;

    private static int combinedAttrsLimit = 3;
    private static int waypointsLimit = 0;

    /**
     * Gets the version number.
     */
    public static
    String getVersion() {
        return "7.15";
    }

    /**
     * <b>Must be called **before** Tweens are created</b>
     * <p>
     * Changes the limit for combined attributes. Defaults to 3 to reduce memory footprint.
     */
    public static
    void setCombinedAttributesLimit(final int limit) {
        Tween.combinedAttrsLimit = limit;
        flushWrite();
    }

    /**
     * <b>Must be called **before** Tweens are created</b>
     * <p>
     * Changes the limit of allowed waypoints for each tween. Defaults to 0 to reduce memory footprint.
     */
    public static
    void setWaypointsLimit(final int limit) {
        Tween.waypointsLimit = limit;
        flushWrite();
    }

    // -------------------------------------------------------------------------
    // Static -- pool
    // -------------------------------------------------------------------------

    private static final PoolableObject<Tween> poolableObject = new PoolableObject<Tween>() {
        @Override
        public
        void onReturn(final Tween object) {
            object.destroy();
        }

        @Override
        public
        Tween create() {
            return new Tween();
        }
    };

    private static final ObjectPool<Tween> pool = ObjectPool.NonBlockingSoftReference(poolableObject);


    // -------------------------------------------------------------------------
    // Static -- tween accessors
    // -------------------------------------------------------------------------

    private static final Map<Class<?>, TweenAccessor<?>> registeredAccessors = new HashMap<Class<?>, TweenAccessor<?>>();

    /**
     * Registers an accessor with the class of an object. This accessor will be used by tweens applied to every objects implementing the
     * registered class, or inheriting from it.
     *
     * @param someClass An object class.
     * @param defaultAccessor The accessor that will be used to tween any object of class "someClass".
     */
    public static
    void registerAccessor(final Class<?> someClass, final TweenAccessor<?> defaultAccessor) {
        registeredAccessors.put(someClass, defaultAccessor);
        flushWrite();
    }

    /**
     * Gets the registered TweenAccessor associated with the given object class.
     *
     * @param someClass An object class.
     */
    public static
    TweenAccessor<?> getRegisteredAccessor(final Class<?> someClass) {
        flushRead();
        return registeredAccessors.get(someClass);
    }

    // -------------------------------------------------------------------------
    // Static -- factories
    // -------------------------------------------------------------------------

    /**
     * Factory creating a new standard interpolation. This is the most common type of interpolation. The starting values are
     * retrieved automatically after the delay (if any).
     * <br/><br/>
     *
     * <b>You need to set the target values of the interpolation by using one of the target() methods</b>. The interpolation will run
     * from the starting values to these target values.
     * <br/><br/>
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens once you added them to a TweenManager, they will
     * be updated automatically, and cleaned once finished.
     * <br/><br/>
     *
     * <pre> {@code
     * Tween.to(myObject, POSITION, 1.0F)
     *      .target(50, 70)
     *      .ease(Quad_InOut)
     *      .start(myManager);
     * }</pre>
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    public static
    Tween to(final Object target, final int tweenType, final float duration) {
        return to(target, tweenType, null, duration);
    }

    /**
     * Factory creating a new standard interpolation. This is the most common type of interpolation. The starting values are
     * retrieved automatically after the delay (if any).
     * <br/><br/>
     *
     * <b>You need to set the target values of the interpolation by using one of the target() methods</b>. The interpolation will run
     * from the starting values to these target values.
     * <br/><br/>
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens once you added them to a TweenManager, they will
     * be updated automatically, and cleaned once finished.
     * <br/><br/>
     *
     * <pre> {@code
     * Tween.to(myObject, POSITION, accessorObject, 1.0F)
     *      .target(50, 70)
     *      .ease(Quad_InOut)
     *      .start(myManager);
     * }</pre>
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param targetAccessor The accessor object (optional) that is used to modify the target values (based on the tween type).
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    public static
    <T> Tween to(final T target, final int tweenType, final TweenAccessor<T> targetAccessor, final float duration) {
        Tween tween = pool.take();
        flushRead();

        tween.setup(target, tweenType, targetAccessor, duration);
        tween.ease(TweenEquations.Quad_InOut);
        tween.path(TweenPaths.CatmullRom);

        flushWrite();
        return tween;
    }

    /**
     * Factory creating a new reversed interpolation. The ending values are retrieved automatically after the delay (if any).
     * <br/><br/>
     *
     * <b>You need to set the starting values of the interpolation by using one of the target() methods</b>. The interpolation will run
     * from the starting values to these target values.
     * <br/><br/>
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens once you added them to a TweenManager, they will
     * be updated automatically, and cleaned once finished. Common call:
     * <br/><br/>
     *
     * <pre> {@code
     * Tween.from(myObject, POSITION, 1.0F)
     *      .target(0, 0)
     *      .ease(Quad_InOut)
     *      .start(myManager);
     * }</pre>
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    public static
    <T> Tween from(final T target, final int tweenType, final float duration) {
        return from(target, tweenType, null, duration);
    }

    /**
     * Factory creating a new reversed interpolation. The ending values are retrieved automatically after the delay (if any).
     * <br/><br/>
     *
     * <b>You need to set the starting values of the interpolation by using one of the target() methods</b>. The interpolation will run
     * from the starting values to these target values.
     * <br/><br/>
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens once you added them to a TweenManager, they will
     * be updated automatically, and cleaned once finished. Common call:
     * <br/><br/>
     *
     * <pre> {@code
     * Tween.from(myObject, POSITION, 1.0F)
     *      .target(0, 0)
     *      .ease(Quad_InOut)
     *      .start(myManager);
     * }</pre>
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param targetAccessor The accessor object (optional) that is used to modify the target values (based on the tween type).
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    public static
    <T> Tween from(final T target, final int tweenType, final TweenAccessor<T> targetAccessor, final float duration) {
        Tween tween = pool.take();
        flushRead();

        tween.setup(target, tweenType, targetAccessor, duration);
        tween.ease(TweenEquations.Quad_InOut);
        tween.path(TweenPaths.CatmullRom);
        tween.isFrom = true;

        flushWrite();
        return tween;
    }

    /**
     * Factory creating a new instantaneous interpolation (thus this is not really an interpolation).
     * <br/><br/>
     *
     * <b>You need to set the target values of the interpolation by using one of the target() methods</b>. The interpolation will set
     * the target attribute to these values after the delay (if any).
     * <br/><br/>
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens once you added them to a TweenManager, they will
     * be updated automatically, and cleaned once finished. Common call:
     * <br/><br/>
     *
     * <pre> {@code
     * Tween.set(myObject, POSITION)
     *      .target(50, 70)
     *      .delay(1.0F)
     *      .start(myManager);
     * }</pre>
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     *
     * @return The generated Tween.
     */
    public static
    <T> Tween set(final T target, final int tweenType) {
        return set(target, tweenType, null);
    }

    /**
     * Factory creating a new instantaneous interpolation (thus this is not really an interpolation).
     * <br/><br/>
     *
     * <b>You need to set the target values of the interpolation by using one of the target() methods</b>. The interpolation will set
     * the target attribute to these values after the delay (if any).
     * <br/><br/>
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens once you added them to a TweenManager, they will
     * be updated automatically, and cleaned once finished. Common call:
     * <br/><br/>
     *
     * <pre> {@code
     * Tween.set(myObject, POSITION)
     *      .target(50, 70)
     *      .delay(1.0F)
     *      .start(myManager);
     * }</pre>
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param targetAccessor The accessor object (optional) that is used to modify the target values (based on the tween type).
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     *
     * @return The generated Tween.
     */
    public static
    <T> Tween set(final T target, final int tweenType, final TweenAccessor<T>  targetAccessor) {
        Tween tween = pool.take();
        flushRead();

        tween.setup(target, tweenType, targetAccessor, 0.0F);
        tween.ease(TweenEquations.Quad_In);

        flushWrite();
        return tween;
    }

    /**
     * Factory creating a new timer. The given callback will be triggered on each iteration start, after the delay.
     * <br/><br/>
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens once you added them to a TweenManager, they will
     * be updated automatically, and cleaned once finished. Common call:
     * <br/><br/>
     *
     * <pre> {@code
     * Tween.call(myCallback)
     *      .delay(1.0F)
     *      .repeat(10, 1.0F)
     *      .start(myManager);
     * }</pre>
     *
     * @param callback The callback that will be triggered on each iteration start.
     *
     * @return The generated Tween.
     * @see TweenCallback
     */
    public static
    Tween call(final TweenCallback callback) {
        Tween tween = pool.take();
        flushRead();

        tween.setup(null, -1, null, 0.0F);
        callback.triggers = TweenCallback.Events.START;
        tween.addCallback(callback);

        flushWrite();
        return tween;
    }

    /**
     * Convenience method to create an empty tween. Such object is only useful when placed inside animation sequences
     * (see {@link Timeline}), in which it may act as a beacon, so you can set a callback on it in order to trigger some action at the
     * right moment.
     *
     * @return The generated Tween.
     * @see Timeline
     */
    public static
    Tween mark() {
        Tween tween = pool.take();
        flushRead();

        tween.setup(null, -1, null, 0.0F);

        flushWrite();
        return tween;
    }

    // -------------------------------------------------------------------------
    // Attributes
    // -------------------------------------------------------------------------

    // Main
    private Object target;
    private Class<?> targetClass;
    private TweenAccessor accessor;

    private int type;
    private TweenEquation equation;
    private TweenPath path;

    // General
    private boolean isFrom;
    private boolean isRelative;
    private int combinedAttrsCnt;
    private int waypointsCount;

    // Values
    private final float[] startValues;
    private final float[] targetValues;
    private final float[] waypoints;

    // Buffers
    private final float[] accessorBuffer;
    private final float[] pathBuffer;

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    Tween() {
        startValues = new float[combinedAttrsLimit];
        targetValues = new float[combinedAttrsLimit];
        waypoints = new float[waypointsLimit * combinedAttrsLimit];

        accessorBuffer = new float[combinedAttrsLimit];
        pathBuffer = new float[(2 + waypointsLimit) * combinedAttrsLimit];

        destroy();
    }

    @Override
    protected
    void destroy() {
        super.destroy();

        target = null;
        targetClass = null;
        accessor = null;
        type = -1;
        equation = null;
        path = null;

        isFrom = isRelative = false;
        combinedAttrsCnt = waypointsCount = 0;

        for (int i = 0, n = startValues.length; i < n; i++) {
            startValues[i] = 0.0F;
        }
        for (int i = 0, n = targetValues.length; i < n; i++) {
            targetValues[i] = 0.0F;
        }
        for (int i = 0, n = waypoints.length; i < n; i++) {
            waypoints[i] = 0.0F;
        }

        for (int i = 0, n = accessorBuffer.length; i < n; i++) {
            accessorBuffer[i] = 0.0F;
        }
        for (int i = 0, n = pathBuffer.length; i < n; i++) {
            pathBuffer[i] = 0.0F;
        }
    }

    private
    void setup(final Object target, final int tweenType, final TweenAccessor targetAccessor, final float duration) {
        if (duration < 0.0F) {
            throw new RuntimeException("Duration can not be negative");
        }

        this.target = target;
        if (targetAccessor != null) {
            this.accessor = targetAccessor;
        } else {
            this.targetClass = target != null ? this.findTargetClass() : null;
        }
        this.type = tweenType;
        this.duration = duration;

        this.setup();
    }

    private
    Class<?> findTargetClass() {
        final Object target = this.target;

        if (target instanceof TweenAccessor) {
            return target.getClass();
        }

        if (registeredAccessors.containsKey(target.getClass())) {
            return target.getClass();
        }

        Class<?> parentClass = target.getClass()
                                     .getSuperclass();
        while (parentClass != null && !registeredAccessors.containsKey(parentClass)) {
            parentClass = parentClass.getSuperclass();
        }

        return parentClass;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sets the easing equation of the tween. Existing equations are located in {@link TweenEquations}, but you can of course implement your
     * own, see {@link TweenEquation}.
     * <p/>
     * Default equation is Quad_InOut.
     * <p/>
     *
     * <b>Provided Equations are:</b><br/>
     * - Linear,<br/>
     * - Quad.In | Out | InOut,<br/>
     * - Cubic.In | Out | InOut,<br/>
     * - Quart.In | Out | InOut,<br/>
     * - QuInt.In | Out | InOut,<br/>
     * - Circle.In | Out | InOut,<br/>
     * - SIne.In | Out | InOut,<br/>
     * - Expo.In | Out | InOut,<br/>
     * - Back.In | Out | InOut,<br/>
     * - Bounce.In | Out | InOut,<br/>
     * - Elastic.In | Out | InOut
     *
     * @return The current tween, for chaining instructions.
     * @see TweenEquation
     * @see TweenEquations
     */
    public
    Tween ease(final TweenEquation easeEquation) {
        this.equation = easeEquation;

        flushWrite();
        return this;
    }

    /**
     * Sets the easing equation of the tween. Existing equations are located in {@link TweenEquations}, but you can of course implement
     * your own, see {@link TweenEquation}.
     * <p/>
     * Default equation is Quad_InOut.
     * <p/>
     *
     * <b>Provided Equations are:</b><br/>
     * - Linear,<br/>
     * - Quad.In | Out | InOut,<br/>
     * - Cubic.In | Out | InOut,<br/>
     * - Quart.In | Out | InOut,<br/>
     * - QuInt.In | Out | InOut,<br/>
     * - Circle.In | Out | InOut,<br/>
     * - SIne.In | Out | InOut,<br/>
     * - Expo.In | Out | InOut,<br/>
     * - Back.In | Out | InOut,<br/>
     * - Bounce.In | Out | InOut,<br/>
     * - Elastic.In | Out | InOut
     *
     * @return The current tween, for chaining instructions.
     * @see TweenEquation
     * @see TweenEquations
     */
    public
    Tween ease(final TweenEquations easeEquation) {
        this.equation = easeEquation.getEquation();

        flushWrite();
        return this;
    }


    /**
     * Forces the tween to use the TweenAccessor registered with the given target class. Useful if you want to use a specific accessor
     * associated to an interface, for instance.
     *
     * @param targetClass A class registered with an accessor.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween cast(final Class<?> targetClass) {
        flushRead();

        if (isInitialized) {
            throw new RuntimeException("You can't cast the target of a tween once it has been initialized");
        }
        this.targetClass = targetClass;

        flushWrite();
        return this;
    }

    /**
     * Sets the target value of the interpolation. The interpolation will run from the <b>value at start time (after the delay, if any)</b>
     * to this target value.
     * <p/>
     *
     * To sum-up:<br/>
     * - start value: value at start time, after delay<br/>
     * - end value: param
     *
     * @param targetValue The target value of the interpolation.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween target(final float targetValue) {
        targetValues[0] = targetValue;

        flushWrite();
        return this;
    }

    /**
     * Sets the target values of the interpolation. The interpolation will run from the <b>values at start time (after the delay, if
     * any)</b> to these target values.
     * <p/>
     *
     * To sum-up:<br/>
     * - start values: values at start time, after delay<br/>
     * - end values: params
     *
     * @param targetValue1 The 1st target value of the interpolation.
     * @param targetValue2 The 2nd target value of the interpolation.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween target(final float targetValue1, final float targetValue2) {
        targetValues[0] = targetValue1;
        targetValues[1] = targetValue2;

        flushWrite();
        return this;
    }

    /**
     * Sets the target values of the interpolation. The interpolation will run from the <b>values at start time (after the delay, if
     * any)</b> to these target values.
     * <p/>
     *
     * To sum-up:<br/>
     * - start values: values at start time, after delay<br/>
     * - end values: params
     *
     * @param targetValue1 The 1st target value of the interpolation.
     * @param targetValue2 The 2nd target value of the interpolation.
     * @param targetValue3 The 3rd target value of the interpolation.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween target(final float targetValue1, final float targetValue2, final float targetValue3) {
        targetValues[0] = targetValue1;
        targetValues[1] = targetValue2;
        targetValues[2] = targetValue3;

        flushWrite();
        return this;
    }

    /**
     * Sets the target values of the interpolation. The interpolation will run from the <b>values at start time (after the delay, if
     * any)</b> to these target values.
     * <p/>
     *
     * To sum-up:<br/>
     * - start values: values at start time, after delay<br/>
     * - end values: params
     *
     * @param targetValues The target values of the interpolation.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween target(final float... targetValues) {
        flushRead();

        final int length = targetValues.length;
        verifyCombinedAttrs(length);

        System.arraycopy(targetValues, 0, this.targetValues, 0, length);

        flushWrite();
        return this;
    }

    /**
     * Sets the target value of the interpolation, relatively to the <b>value at start time (after the delay, if any)</b>.
     * <p/>
     *
     * To sum-up:<br/>
     * - start value: value at start time, after delay<br/>
     * - end value: param + value at start time, after delay
     *
     * @param targetValue The relative target value of the interpolation.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween targetRelative(final float targetValue) {
        flushRead();

        isRelative = true;
        targetValues[0] = isInitialized ? targetValue + startValues[0] : targetValue;

        flushWrite();
        return this;
    }

    /**
     * Sets the target value of the interpolation, relatively to the <b>value at start time (after the delay, if any)</b>.
     * <p/>
     *
     * To sum-up:<br/>
     * - start values: values at start time, after delay<br/>
     * - end values: params + values at start time, after delay
     *
     * @param targetValue1 The 1st relative target value of the interpolation.
     * @param targetValue2 The 2nd relative target value of the interpolation.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween targetRelative(final float targetValue1, final float targetValue2) {
        flushRead();

        isRelative = true;
        final boolean initialized = isInitialized;
        targetValues[0] = initialized ? targetValue1 + startValues[0] : targetValue1;
        targetValues[1] = initialized ? targetValue2 + startValues[1] : targetValue2;

        flushWrite();
        return this;
    }

    /**
     * Sets the target value of the interpolation, relatively to the <b>value at start time (after the delay, if any)</b>.
     * <p/>
     *
     * To sum-up:<br/>
     * - start values: values at start time, after delay<br/>
     * - end values: params + values at start time, after delay
     *
     * @param targetValue1 The 1st relative target value of the interpolation.
     * @param targetValue2 The 2nd relative target value of the interpolation.
     * @param targetValue3 The 3rd relative target value of the interpolation.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween targetRelative(final float targetValue1, final float targetValue2, final float targetValue3) {
        flushRead();

        this.isRelative = true;
        final boolean initialized = this.isInitialized;

        final float[] startValues = this.startValues;
        targetValues[0] = initialized ? targetValue1 + startValues[0] : targetValue1;
        targetValues[1] = initialized ? targetValue2 + startValues[1] : targetValue2;
        targetValues[2] = initialized ? targetValue3 + startValues[2] : targetValue3;

        flushWrite();
        return this;
    }

    /**
     * Sets the target value of the interpolation, relatively to the <b>value at start time (after the delay, if any)</b>.
     * <p/>
     *
     * To sum-up:<br/>
     * - start values: values at start time, after delay<br/>
     * - end values: params + values at start time, after delay
     *
     * @param targetValues The relative target values of the interpolation.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween targetRelative(final float... targetValues) {
        flushRead();

        final int length = targetValues.length;
        verifyCombinedAttrs(length);

        final boolean initialized = isInitialized;
        final float[] startValues = this.startValues;

        for (int i = 0; i < length; i++) {
            this.targetValues[i] = initialized ? targetValues[i] + startValues[i] : targetValues[i];
        }

        this.isRelative = true;

        flushWrite();
        return this;
    }

    /**
     * Adds a waypoint to the path. The default path runs from the start values to the end values linearly. If you add waypoints, the
     * default path will use a smooth catmull-rom spline to navigate between the waypoints, but you can change this behavior by using the
     * {@link #path(TweenPath)} method.
     *
     * @param targetValue The target of this waypoint.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween waypoint(final float targetValue) {
        flushRead();

        final int waypointsCount = this.waypointsCount;
        verifyWaypoints(waypointsCount);

        waypoints[waypointsCount] = targetValue;
        this.waypointsCount += 1;

        flushWrite();
        return this;
    }

    /**
     * Adds a waypoint to the path. The default path runs from the start values to the end values linearly. If you add waypoints, the
     * default path will use a smooth catmull-rom spline to navigate between the waypoints, but you can change this behavior by using the
     * {@link #path(TweenPath)} method.
     * <p/>
     * Note that if you want waypoints relative to the start values, use one of the .targetRelative() methods to define your target.
     *
     * @param targetValue1 The 1st target of this waypoint.
     * @param targetValue2 The 2nd target of this waypoint.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween waypoint(final float targetValue1, final float targetValue2) {
        flushRead();

        final int waypointsCount = this.waypointsCount;
        verifyWaypoints(waypointsCount);

        final int count = waypointsCount << 1;  //*2
        final float[] waypoints = this.waypoints;

        waypoints[count] = targetValue1;
        waypoints[count + 1] = targetValue2;
        this.waypointsCount += 1;

        flushWrite();
        return this;
    }

    /**
     * Adds a waypoint to the path. The default path runs from the start values to the end values linearly. If you add waypoints, the
     * default path will use a smooth catmull-rom spline to navigate between the waypoints, but you can change this behavior by using the
     * {@link #path(TweenPath)} method.
     * <p/>
     * Note that if you want waypoints relative to the start values, use one of the .targetRelative() methods to define your target.
     *
     * @param targetValue1 The 1st target of this waypoint.
     * @param targetValue2 The 2nd target of this waypoint.
     * @param targetValue3 The 3rd target of this waypoint.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween waypoint(final float targetValue1, final float targetValue2, final float targetValue3) {
        flushRead();

        final int waypointsCount = this.waypointsCount;
        verifyWaypoints(waypointsCount);

        final int count = waypointsCount * 3;
        final float[] waypoints = this.waypoints;

        waypoints[count] = targetValue1;
        waypoints[count + 1] = targetValue2;
        waypoints[count + 2] = targetValue3;
        this.waypointsCount += 1;

        flushWrite();
        return this;
    }

    /**
     * Adds a waypoint to the path. The default path runs from the start values to the end values linearly. If you add waypoints, the
     * default path will use a smooth catmull-rom spline to navigate between the waypoints, but you can change this behavior by using the
     * {@link #path(TweenPath)} method.
     * <p/>
     * Note that if you want waypoints relative to the start values, use one of the .targetRelative() methods to define your target.
     *
     * @param targetValues The targets of this waypoint.
     *
     * @return The current tween, for chaining instructions.
     */
    public
    Tween waypoint(final float... targetValues) {
        flushRead();

        final int waypointsCount = this.waypointsCount;
        verifyWaypoints(waypointsCount);

        System.arraycopy(targetValues, 0, waypoints, waypointsCount * targetValues.length, targetValues.length);
        this.waypointsCount += 1;

        flushWrite();
        return this;
    }

    /**
     * Sets the algorithm that will be used to navigate through the waypoints, from the start values to the end values. Default is a
     * catmull-rom spline, but you can find other paths in the {@link TweenPaths} class.
     *
     * @param path A TweenPath implementation.
     *
     * @return The current tween, for chaining instructions.
     * @see TweenPath
     * @see TweenPaths
     */
    public
    Tween path(final TweenPaths path) {
        this.path = path.path();

        flushWrite();
        return this;
    }

    /**
     * Sets the algorithm that will be used to navigate through the waypoints, from the start values to the end values. Default is a
     * catmull-rom spline, but you can find other paths in the {@link TweenPaths} class.
     *
     * @param path A TweenPath implementation.
     *
     * @return The current tween, for chaining instructions.
     * @see TweenPath
     * @see TweenPaths
     */
    public
    Tween path(final TweenPath path) {
        this.path = path;

        flushWrite();
        return this;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Gets the target object.
     */
    public
    Object getTarget() {
        flushRead();
        return target;
    }

    /**
     * Gets the type of the tween.
     */
    public
    int getType() {
        flushRead();
        return type;
    }

    /**
     * Gets the easing equation.
     */
    public
    TweenEquation getEasing() {
        flushRead();
        return equation;
    }

    /**
     * Gets the target values. The returned buffer is as long as the maximum allowed combined values. Therefore, you're surely not
     * interested in all its content. Use {@link Tween#getCombinedAttributesCount()} to get the number of interesting slots.
     */
    public float[]
    getTargetValues() {
        flushRead();
        return targetValues;
    }

    /**
     * Gets the number of combined animations.
     */
    public int
    getCombinedAttributesCount() {
        flushRead();
        return combinedAttrsCnt;
    }

    /**
     * Gets the TweenAccessor used with the target.
     */
    public
    TweenAccessor<?> getAccessor() {
        flushRead();
        return accessor;
    }

    /**
     * Gets the class that was used to find the associated TweenAccessor.
     */
    public
    Class<?> getTargetClass() {
        flushRead();
        return targetClass;
    }

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public
    Tween start() {
        flushRead();

        super.start();

        final Object target = this.target;
        if (target == null) {
            return this;
        }

        if (accessor == null) {
            if (target instanceof TweenAccessor) {
                accessor = (TweenAccessor<Object>) target;
            }
            else {
                accessor = registeredAccessors.get(targetClass);
            }
        }

        if (accessor != null) {
            combinedAttrsCnt = accessor.getValues(target, type, accessorBuffer);
        }
        else {
            throw new RuntimeException("No TweenAccessor was found for the target");
        }

        verifyCombinedAttrs(combinedAttrsCnt);

        flushWrite();
        return this;
    }

    @Override
    public
    void free() {
        pool.put(this);
    }

    /**
     * Forces a timeline/tween to have it's start/target values. Repeat behavior is also correctly modeled in the decision process
     *
     * @param updateDirection direction in which the update is happening. Affects children iteration order (timelines)
     * @param updateValue this is the start (true) or target (false) to set the tween to.
     */
    @SuppressWarnings("unchecked")
    protected
    void setValues(final boolean updateDirection, final boolean updateValue) {
        if (target == null || !this.isInitialized || this.isKilled) {
            return;
        }

        if (updateValue) {
            // and want the "start" (always relative to forwards)
            accessor.setValues(target, type, startValues);
        }
        else {
            // and want the "end" (always relative to forwards)
            if (canAutoReverse && (repeatCountOrig & 1) != 0) { // odd
                // repeats REALLY make this complicated, because reverse auto-repeats flip the logic, but only if an odd count
                // so if we are ODD, then we are actually at the "start" value. If EVEN, we are at the "target" value
                accessor.setValues(target, type, startValues);
            } else {
                accessor.setValues(target, type, targetValues);
            }
        }
    }

    @Override
    protected
    void initializeValues() {
        final Object target = this.target;
        if (target == null || this.isKilled) {
            return;
        }

        // NOTICE: can't updateValues before we initialize them

        final float[] startValues = this.startValues;
        final float[] targetValues = this.targetValues;
        final int combinedAttrsCnt = this.combinedAttrsCnt;

        //noinspection unchecked
        accessor.getValues(target, this.type, startValues);

        // expanded form of "isRelative" + "isFrom"
        //  isRelative is target += start
        // !isRelative is target = 0;
        // isFrom FLIPS start & target values

        if (isRelative) {
            final int waypointsCount = this.waypointsCount;
            final float[] waypoints = this.waypoints;

            if (isFrom) {
                for (int i = 0; i < combinedAttrsCnt; i++) {
                    targetValues[i] += startValues[i];

                    for (int ii = 0; ii < waypointsCount; ii++) {
                        waypoints[ii * combinedAttrsCnt + i] += startValues[i];
                    }

                    // unique for "isFrom"
                    final float tmp = startValues[i];
                    startValues[i] = targetValues[i];
                    targetValues[i] = tmp;
                }
            } else {
                for (int i = 0; i < combinedAttrsCnt; i++) {
                    targetValues[i] += startValues[i];

                    for (int ii = 0; ii < waypointsCount; ii++) {
                        waypoints[ii * combinedAttrsCnt + i] += startValues[i];
                    }
                }
            }
        }
        else if (isFrom) {
            for (int i = 0; i < combinedAttrsCnt; i++) {
                // unique for "isFrom"
                final float tmp = startValues[i];
                startValues[i] = targetValues[i];
                targetValues[i] = tmp;
            }
        }
    }

    /**
     * When done with all the adjustments and notifications, update the object. Only called during State.RUN. Other state updates will
     * use {@link BaseTween#setValues(boolean, boolean)}
     * <p>
     * values will ONLY be updated if the tween was initialized (reached START state at least once)
     * <p>
     * If a timeline/tween is outside it's animation cycle time, it will "snap" to the start/end points via
     * {@link BaseTween#setValues(boolean, boolean)}
     *
     * @param updateDirection not used (only used by the timeline). It is necessary here because of how the methods are overloaded.
     * @param delta the time in SECONDS that has elapsed since the last update
     */
    protected
    void update(final boolean updateDirection, final float delta) {
        final Object target = this.target;
        final TweenEquation equation = this.equation;

        // be aware that a tween can ONLY have it's values updated IFF it has been initialized (reached START state at least once)
        if (target == null || equation == null || !this.isInitialized || this.isKilled) {
            return;
        }

        final float duration = this.duration;
        final float time = this.currentTime;


        // Normal behavior. Have to convert to at this point
        final float tweenValue = equation.compute(time / duration);

        final float[] accessorBuffer = this.accessorBuffer;
        final int combinedAttrsCnt = this.combinedAttrsCnt;

        final int waypointsCnt = this.waypointsCount;
        final TweenPath path = this.path;

        final float[] startValues = this.startValues;
        final float[] targetValues = this.targetValues;

        if (waypointsCnt == 0 || path == null) {
            for (int i = 0; i < combinedAttrsCnt; i++) {
                accessorBuffer[i] = startValues[i] + tweenValue * (targetValues[i] - startValues[i]);
            }
        }
        else {
            final float[] waypoints = this.waypoints;
            final float[] pathBuffer = this.pathBuffer;

            for (int i = 0; i < combinedAttrsCnt; i++) {
                pathBuffer[0] = startValues[i];
                pathBuffer[1 + waypointsCnt] = targetValues[i];
                for (int ii = 0; ii < waypointsCnt; ii++) {
                    pathBuffer[ii + 1] = waypoints[ii * combinedAttrsCnt + i];
                }

                accessorBuffer[i] = path.compute(tweenValue, pathBuffer, waypointsCnt + 2);
            }
        }

        //noinspection unchecked
        accessor.setValues(target, type, accessorBuffer);
    }

    // -------------------------------------------------------------------------
    // BaseTween impl.
    // -------------------------------------------------------------------------


    @Override
    protected
    boolean containsTarget(final Object target) {
        return this.target == target;
    }

    @Override
    protected
    boolean containsTarget(final Object target, final int tweenType) {
        return this.target == target && this.type == tweenType;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // has the combined attributes limit been reached?
    private static
    void verifyCombinedAttrs(final int length) {
        if (length > combinedAttrsLimit) {
            String msg = "You cannot combine more than " + combinedAttrsLimit + " " + "attributes in a tween. You can raise this limit with " +
                         "Tween.setCombinedAttributesLimit(), which should be called once in application initialization code.";
            throw new RuntimeException(msg);
        }
    }

    // has the waypoints limit been reached?
    private static
    void verifyWaypoints(final int waypointsCount) {
        if (waypointsCount == waypointsLimit) {
            String msg = "You cannot add more than " + waypointsLimit + " " + "waypoints to a tween. You can raise this limit with " +
                         "Tween.setWaypointsLimit(), which should be called once in application initialization code.";
            throw new RuntimeException(msg);
        }
    }
}

