/*
 * Copyright 2012 Aurelien Ribon
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
 *
 *
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
package dorkbox.util.tweenengine;

import dorkbox.util.objectPool.ObjectPool;
import dorkbox.util.objectPool.ObjectPoolFactory;
import dorkbox.util.objectPool.PoolableObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Core class of the Tween Engine. A Tween is basically an interpolation
 * between two values of an object attribute. However, the main interest of a
 * Tween is that you can apply an easing formula on this interpolation, in
 * order to smooth the transitions or to achieve cool effects like springs or
 * bounces.
 * <p/>
 *
 * The Universal Tween Engine is called "universal" because it is able to apply
 * interpolations on every attribute from every possible object. Therefore,
 * every object in your application can be animated with cool effects: it does
 * not matter if your application is a game, a desktop interface or even a
 * console program! If it makes sense to animate something, then it can be
 * animated through this engine.
 * <p/>
 *
 * This class contains many static factory methods to create and instantiate
 * new interpolations easily. The common way to create a Tween is by using one
 * of these factories:
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
 * The following example will move the target horizontal position from its
 * current value to x=200 and y=300, during 500ms, but only after a delay of
 * 1000ms. The animation will also be repeated 2 times (the starting position
 * is registered at the end of the delay, so the animation will automatically
 * restart from this registered position).
 * <p/>
 *
 * <pre> {@code
 * Tween.to(myObject, POSITION_XY, 500)
 *      .target(200, 300)
 *      .ease(Quad.INOUT)
 *      .delay(1.0f)
 *      .repeat(2, 200)
 *      .start(myManager);
 * }</pre>
 *
 * Tween life-cycles can be automatically managed for you, thanks to the
 * {@link TweenManager} class. If you choose to manage your tween when you start
 * it, then you don't need to care about it anymore. <b>Tweens are
 * <i>fire-and-forget</i>: don't think about them anymore once you started
 * them (if they are managed of course).</b>
 * <p/>
 *
 * You need to periodicaly update the tween engine, in order to compute the new
 * values. If your tweens are managed, only update the manager; else you need
 * to call {@link Tween#update(float)} on your tweens periodically.
 * <p/>
 *
 * <h2>Example - setting up the engine</h2>
 *
 * The engine cannot directly change your objects attributes, since it doesn't
 * know them. Therefore, you need to tell him how to get and set the different
 * attributes of your objects: <b>you need to implement the {@link
 * TweenAccessor} interface for each object class you will animate</b>. Once
 * done, don't forget to register these implementations, using the static method
 * {@link Tween#registerAccessor(Class, TweenAccessor)}, when you start your application.
 *
 * @see TweenAccessor
 * @see TweenManager
 * @see TweenEquation
 * @see Timeline
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
@SuppressWarnings({"unused", "StaticNonFinalField"})
public final
class Tween extends BaseTween<Tween> {
    private static final Thread constructorThread = Thread.currentThread();

	// -------------------------------------------------------------------------
	// Static -- misc
	// -------------------------------------------------------------------------

	/**
	 * Used as parameter in {@link #repeat(int, float)} and
	 * {@link #repeatAutoReverse(int, float)} methods.
	 */
	public static final int INFINITY = -1;

	private static int combinedAttrsLimit = 3;
	private static int waypointsLimit = 0;

    /**
     * Gets the version number of the library.
     */
    public static
    String getVersion() {
        return "7.0.0";
    }

	/**
	 * Changes the limit for combined attributes. Defaults to 3 to reduce
	 * memory footprint.
	 */
    public static
    void setCombinedAttributesLimit(int limit) {
        if (constructorThread != Thread.currentThread()) {
            throw new RuntimeException("Tween combined attribute limits must be changed during engine initialization!");
        }
        Tween.combinedAttrsLimit = limit;
    }

    /**
	 * Changes the limit of allowed waypoints for each tween. Defaults to 0 to
	 * reduce memory footprint.
	 */
    public static
    void setWaypointsLimit(int limit) {
        if (constructorThread != Thread.currentThread()) {
            throw new RuntimeException("Tween waypoint limits must be changed during engine initialization!");
        }
        Tween.waypointsLimit = limit;
    }

	// -------------------------------------------------------------------------
	// Static -- pool
	// -------------------------------------------------------------------------

    private static final PoolableObject<Tween> poolableObject = new PoolableObject<Tween>() {
        @Override
        public
        void reset(final Tween object) {
            object.reset();
        }

        @Override
        public
        Tween create() {
            return new Tween();
        }
    };

    static ObjectPool<Tween> pool = ObjectPoolFactory.create(poolableObject, 1024);

	/**
	 * Used for debug purpose. Gets the current number of objects that are
	 * waiting in the Tween pool.
	 */
	public static int getPoolSize() {
		return pool.size();
	}

	/**
	 * Increases the minimum capacity of the pool. Capacity defaults to 1024.
	 */
	public static void setPoolSize(final int poolSize) {
        if (constructorThread != Thread.currentThread()) {
            throw new RuntimeException("Tween pool capacity must be changed during engine initialization!");
        }
        pool = ObjectPoolFactory.create(poolableObject, poolSize);
	}

	// -------------------------------------------------------------------------
	// Static -- tween accessors
	// -------------------------------------------------------------------------

	private static final Map<Class<?>, TweenAccessor<?>> registeredAccessors = new HashMap<Class<?>, TweenAccessor<?>>();

	/**
	 * Registers an accessor with the class of an object. This accessor will be
	 * used by tweens applied to every objects implementing the registered
	 * class, or inheriting from it.
	 *
	 * @param someClass An object class.
	 * @param defaultAccessor The accessor that will be used to tween any
	 * object of class "someClass".
	 */
    public static
    void registerAccessor(final Class<?> someClass, final TweenAccessor<?> defaultAccessor) {
        if (constructorThread != Thread.currentThread()) {
            throw new RuntimeException("Tween accessors must be accessed by the same thread as engine initialization!");
        }

        registeredAccessors.put(someClass, defaultAccessor);
    }

    /**
	 * Gets the registered TweenAccessor associated with the given object class.
	 *
	 * @param someClass An object class.
	 */
	public static
    TweenAccessor<?> getRegisteredAccessor(final Class<?> someClass) {
        if (constructorThread != Thread.currentThread()) {
            throw new RuntimeException("Tween accessors must be accessed by the same thread as engine initialization!");
        }

        return registeredAccessors.get(someClass);
	}

	// -------------------------------------------------------------------------
	// Static -- factories
	// -------------------------------------------------------------------------

	/**
	 * Factory creating a new standard interpolation. This is the most common
	 * type of interpolation. The starting values are retrieved automatically
	 * after the delay (if any).
	 * <br/><br/>
	 *
	 * <b>You need to set the target values of the interpolation by using one
	 * of the target() methods</b>. The interpolation will run from the
	 * starting values to these target values.
	 * <br/><br/>
	 *
	 * The common use of Tweens is "fire-and-forget": you do not need to care
	 * for tweens once you added them to a TweenManager, they will be updated
	 * automatically, and cleaned once finished.
	 * <br/><br/>
	 *
	 * <pre> {@code
	 * Tween.to(myObject, POSITION, 1.0F)
	 *      .target(50, 70)
	 *      .ease(Quad.INOUT)
	 *      .start(myManager);
	 * }</pre>
	 *
	 * Several options such as delay, repetitions and callbacks can be added to
	 * the tween.
	 *
	 * @param target The target object of the interpolation.
	 * @param tweenType The desired type of interpolation, used for TweenAccessor methods.
	 * @param duration The duration of the interpolation, in seconds.
     *
	 * @return The generated Tween.
	 */
	public static
    Tween to(final Object target, final int tweenType, final float duration) {
		Tween tween = pool.takeUninterruptibly();
		tween.setup(target, tweenType, duration);
		tween.ease(TweenEquations.Quad_InOut);
		tween.path(TweenPaths.CatmullRom);
		return tween;
	}

	/**
	 * Factory creating a new reversed interpolation. The ending values are
	 * retrieved automatically after the delay (if any).
	 * <br/><br/>
	 *
	 * <b>You need to set the starting values of the interpolation by using one
	 * of the target() methods</b>. The interpolation will run from the
	 * starting values to these target values.
	 * <br/><br/>
	 *
	 * The common use of Tweens is "fire-and-forget": you do not need to care
	 * for tweens once you added them to a TweenManager, they will be updated
	 * automatically, and cleaned once finished. Common call:
	 * <br/><br/>
	 *
	 * <pre> {@code
	 * Tween.from(myObject, POSITION, 1.0F)
	 *      .target(0, 0)
	 *      .ease(Quad.INOUT)
	 *      .start(myManager);
	 * }</pre>
	 *
	 * Several options such as delay, repetitions and callbacks can be added to
	 * the tween.
	 *
	 * @param target The target object of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @param duration The duration of the interpolation, in seconds.
     *
	 * @return The generated Tween.
	 */
	public static
    Tween from(final Object target, final int tweenType, final float duration) {
		Tween tween = pool.takeUninterruptibly();
		tween.setup(target, tweenType, duration);
		tween.ease(TweenEquations.Quad_InOut);
		tween.path(TweenPaths.CatmullRom);
		tween.isFrom = true;
		return tween;
	}

	/**
	 * Factory creating a new instantaneous interpolation (thus this is not
	 * really an interpolation).
	 * <br/><br/>
	 *
	 * <b>You need to set the target values of the interpolation by using one
	 * of the target() methods</b>. The interpolation will set the target
	 * attribute to these values after the delay (if any).
	 * <br/><br/>
	 *
	 * The common use of Tweens is "fire-and-forget": you do not need to care
	 * for tweens once you added them to a TweenManager, they will be updated
	 * automatically, and cleaned once finished. Common call:
	 * <br/><br/>
	 *
	 * <pre> {@code
	 * Tween.set(myObject, POSITION)
	 *      .target(50, 70)
	 *      .delay(1.0F)
	 *      .start(myManager);
	 * }</pre>
	 *
	 * Several options such as delay, repetitions and callbacks can be added to
	 * the tween.
	 *
	 * @param target The target object of the interpolation.
	 * @param tweenType The desired type of interpolation.
	 * @return The generated Tween.
	 */
	public static
    Tween set(final Object target, final int tweenType) {
		Tween tween = pool.takeUninterruptibly();
		tween.setup(target, tweenType, 0F);
		tween.ease(TweenEquations.Quad_In);
		return tween;
	}

	/**
	 * Factory creating a new timer. The given callback will be triggered on
	 * each iteration start, after the delay.
	 * <br/><br/>
	 *
	 * The common use of Tweens is "fire-and-forget": you do not need to care
	 * for tweens once you added them to a TweenManager, they will be updated
	 * automatically, and cleaned once finished. Common call:
	 * <br/><br/>
	 *
	 * <pre> {@code
	 * Tween.call(myCallback)
	 *      .delay(1.0F)
	 *      .repeat(10, 1.0F)
	 *      .start(myManager);
	 * }</pre>
	 *
	 * @param callback The callback that will be triggered on each iteration
	 * start.
	 * @return The generated Tween.
	 * @see TweenCallback
	 */
	public static
    Tween call(final TweenCallback callback) {
		Tween tween = pool.takeUninterruptibly();
		tween.setup(null, -1, 0F);
        callback.triggers = TweenCallback.Events.START;
		tween.addCallback(callback);
		return tween;
	}

	/**
	 * Convenience method to create an empty tween. Such object is only useful
	 * when placed inside animation sequences (see {@link Timeline}), in which
	 * it may act as a beacon, so you can set a callback on it in order to
	 * trigger some action at the right moment.
	 *
	 * @return The generated Tween.
	 * @see Timeline
	 */
	public static
    Tween mark() {
		Tween tween = pool.takeUninterruptibly();
		tween.setup(null, -1, 0F);
		return tween;
	}

	// -------------------------------------------------------------------------
	// Attributes
	// -------------------------------------------------------------------------

	// Main
	private Object target;
	private Class<?> targetClass;
	private TweenAccessor<Object> accessor;
	private int type;
	private TweenEquation equation;
	private TweenPath path;

	// General
	private boolean isFrom;
	private boolean isRelative;
	private int combinedAttrsCnt;
	private int waypointsCount;

	// Values
	private final float[] startValues = new float[combinedAttrsLimit];
	private final float[] targetValues = new float[combinedAttrsLimit];
	private final float[] waypoints = new float[waypointsLimit * combinedAttrsLimit];

	// Buffers
	private float[] accessorBuffer = new float[combinedAttrsLimit];
	private float[] pathBuffer = new float[(2+waypointsLimit)*combinedAttrsLimit];

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	Tween() {
		reset();
	}

	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    @Override
	protected
    void reset() {
		super.reset();

		target = null;
		targetClass = null;
		accessor = null;
		type = -1;
		equation = null;
		path = null;

		isFrom = isRelative = false;
        combinedAttrsCnt = waypointsCount = 0;

        if (accessorBuffer.length != combinedAttrsLimit) {
            accessorBuffer = new float[combinedAttrsLimit];
        }

        if (pathBuffer.length != (2 + waypointsLimit) * combinedAttrsLimit) {
            pathBuffer = new float[(2 + waypointsLimit) * combinedAttrsLimit];
        }
        flushWrite();
    }

    private void
    setup(final Object target, final int tweenType, final float duration) {
        if (duration < 0) {
            throw new RuntimeException("Duration can't be negative");
        }

        this.target = target;
        this.targetClass = target != null ? findTargetClass() : null;
        this.type = tweenType;
        this.duration = duration;
	}

	private
    Class<?> findTargetClass() {
        final Object target = this.target;

        if (registeredAccessors.containsKey(target.getClass())) {
            return target.getClass();
        }
        if (target instanceof TweenAccessor) {
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
     * Sets the easing equation of the tween. Existing equations are located in
     * {@link TweenEquations}, but you can of course implement your own, see
     * {@link TweenEquation}.
     * <p/>
     * Default equation is Quad.INOUT.
     * <p/>
     *
     * <b>Provided Equations are:</b><br/>
     * - Linear.INOUT,<br/>
     * - Quad.IN | OUT | INOUT,<br/>
     * - Cubic.IN | OUT | INOUT,<br/>
     * - Quart.IN | OUT | INOUT,<br/>
     * - Quint.IN | OUT | INOUT,<br/>
     * - Circle.IN | OUT | INOUT,<br/>
     * - Sine.IN | OUT | INOUT,<br/>
     * - Expo.IN | OUT | INOUT,<br/>
     * - Back.IN | OUT | INOUT,<br/>
     * - Bounce.IN | OUT | INOUT,<br/>
     * - Elastic.IN | OUT | INOUT
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
     * Sets the easing equation of the tween. Existing equations are located in
     * {@link TweenEquations}, but you can of course implement your own, see
     * {@link TweenEquation}.
     * <p/>
     * Default equation is Quad.INOUT.
     * <p/>
     *
     * <b>Provided Equations are:</b><br/>
     * - Linear.INOUT,<br/>
     * - Quad.IN | OUT | INOUT,<br/>
     * - Cubic.IN | OUT | INOUT,<br/>
     * - Quart.IN | OUT | INOUT,<br/>
     * - Quint.IN | OUT | INOUT,<br/>
     * - Circle.IN | OUT | INOUT,<br/>
     * - Sine.IN | OUT | INOUT,<br/>
     * - Expo.IN | OUT | INOUT,<br/>
     * - Back.IN | OUT | INOUT,<br/>
     * - Bounce.IN | OUT | INOUT,<br/>
     * - Elastic.IN | OUT | INOUT
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
	 * Forces the tween to use the TweenAccessor registered with the given
	 * target class. Useful if you want to use a specific accessor associated
	 * to an interface, for instance.
	 *
	 * @param targetClass A class registered with an accessor.
     *
	 * @return The current tween, for chaining instructions.
	 */
	public
    Tween cast(final Class<?> targetClass) {
        if (isStarted()) {
            throw new RuntimeException("You can't cast the target of a tween once it is started");
        }
        this.targetClass = targetClass;

        flushWrite();
        return this;
	}

	/**
	 * Sets the target value of the interpolation. The interpolation will run
	 * from the <b>value at start time (after the delay, if any)</b> to this
	 * target value.
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
	 * Sets the target values of the interpolation. The interpolation will run
	 * from the <b>values at start time (after the delay, if any)</b> to these
	 * target values.
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
	 * Sets the target values of the interpolation. The interpolation will run
	 * from the <b>values at start time (after the delay, if any)</b> to these
	 * target values.
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
	 * Sets the target values of the interpolation. The interpolation will run
	 * from the <b>values at start time (after the delay, if any)</b> to these
	 * target values.
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
        final int length = targetValues.length;
        if (length > combinedAttrsLimit) {
            throwCombinedAttrsLimitReached();
        }

        System.arraycopy(targetValues, 0, this.targetValues, 0, length);

        flushWrite();
        return this;
	}

	/**
	 * Sets the target value of the interpolation, relatively to the <b>value
	 * at start time (after the delay, if any)</b>.
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
		isRelative = true;
		targetValues[0] = isStarted() ? targetValue + startValues[0] : targetValue;

        flushWrite();
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>values
	 * at start time (after the delay, if any)</b>.
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
		isRelative = true;
        final boolean started = isStarted();
        targetValues[0] = started ? targetValue1 + startValues[0] : targetValue1;
		targetValues[1] = started ? targetValue2 + startValues[1] : targetValue2;

        flushWrite();
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>values
	 * at start time (after the delay, if any)</b>.
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
		isRelative = true;
        final boolean started = isStarted();

        final float[] startValues = this.startValues;
        targetValues[0] = started ? targetValue1 + startValues[0] : targetValue1;
		targetValues[1] = started ? targetValue2 + startValues[1] : targetValue2;
		targetValues[2] = started ? targetValue3 + startValues[2] : targetValue3;

        flushWrite();
		return this;
	}

	/**
	 * Sets the target values of the interpolation, relatively to the <b>values
	 * at start time (after the delay, if any)</b>.
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
        final int length = targetValues.length;

        final boolean started = isStarted();
        final float[] startValues = this.startValues;

        if (length > combinedAttrsLimit) throwCombinedAttrsLimitReached();
		for (int i = 0; i < length; i++) {
            this.targetValues[i] = started ? targetValues[i] + startValues[i] : targetValues[i];
		}

		isRelative = true;

        flushWrite();
		return this;
	}

	/**
	 * Adds a waypoint to the path. The default path runs from the start values
	 * to the end values linearly. If you add waypoints, the default path will
	 * use a smooth catmull-rom spline to navigate between the waypoints, but
	 * you can change this behavior by using the {@link #path(TweenPath)}
	 * method.
	 *
	 * @param targetValue The target of this waypoint.
     *
	 * @return The current tween, for chaining instructions.
	 */
	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    public
    Tween waypoint(final float targetValue) {
        flushRead();
        if (waypointsCount == waypointsLimit) {
            throwWaypointsLimitReached();
        }

        waypoints[waypointsCount] = targetValue;
        waypointsCount += 1;

        flushWrite();
        return this;
	}

	/**
	 * Adds a waypoint to the path. The default path runs from the start values
	 * to the end values linearly. If you add waypoints, the default path will
	 * use a smooth catmull-rom spline to navigate between the waypoints, but
	 * you can change this behavior by using the {@link #path(TweenPath)}
	 * method.
	 * <p/>
	 * Note that if you want waypoints relative to the start values, use one of
	 * the .targetRelative() methods to define your target.
	 *
	 * @param targetValue1 The 1st target of this waypoint.
	 * @param targetValue2 The 2nd target of this waypoint.
     *
	 * @return The current tween, for chaining instructions.
	 */
	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    public
    Tween waypoint(final float targetValue1, final float targetValue2) {
        flushRead();
        if (waypointsCount == waypointsLimit) {
            throwWaypointsLimitReached();
        }

        final int count = waypointsCount << 1;
        final float[] waypoints = this.waypoints;

        waypoints[count] = targetValue1;
        waypoints[count + 1] = targetValue2;
        waypointsCount += 1;

        flushWrite();
        return this;
	}

	/**
	 * Adds a waypoint to the path. The default path runs from the start values
	 * to the end values linearly. If you add waypoints, the default path will
	 * use a smooth catmull-rom spline to navigate between the waypoints, but
	 * you can change this behavior by using the {@link #path(TweenPath)}
	 * method.
	 * <p/>
	 * Note that if you want waypoints relative to the start values, use one of
	 * the .targetRelative() methods to define your target.
	 *
	 * @param targetValue1 The 1st target of this waypoint.
	 * @param targetValue2 The 2nd target of this waypoint.
	 * @param targetValue3 The 3rd target of this waypoint.
     *
	 * @return The current tween, for chaining instructions.
	 */
	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    public
    Tween waypoint(final float targetValue1, final float targetValue2, final float targetValue3) {
        flushRead();
        if (waypointsCount == waypointsLimit) {
            throwWaypointsLimitReached();
        }

        final int count = waypointsCount * 3;
        final float[] waypoints = this.waypoints;

        waypoints[count] = targetValue1;
        waypoints[count + 1] = targetValue2;
        waypoints[count + 2] = targetValue3;
        waypointsCount += 1;

        flushWrite();
        return this;
	}

	/**
	 * Adds a waypoint to the path. The default path runs from the start values
	 * to the end values linearly. If you add waypoints, the default path will
	 * use a smooth catmull-rom spline to navigate between the waypoints, but
	 * you can change this behavior by using the {@link #path(TweenPath)}
	 * method.
	 * <p/>
	 * Note that if you want waypoints relative to the start values, use one of
	 * the .targetRelative() methods to define your target.
	 *
	 * @param targetValues The targets of this waypoint.
     *
	 * @return The current tween, for chaining instructions.
	 */
	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    public
    Tween waypoint(final float... targetValues) {
        flushRead();
        if (waypointsCount == waypointsLimit) {
            throwWaypointsLimitReached();
        }
        System.arraycopy(targetValues, 0, waypoints, waypointsCount * targetValues.length, targetValues.length);
        waypointsCount += 1;

        flushWrite();
        return this;
	}

	/**
	 * Sets the algorithm that will be used to navigate through the waypoints,
	 * from the start values to the end values. Default is a catmull-rom spline,
	 * but you can find other paths in the {@link TweenPaths} class.
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
     * Sets the algorithm that will be used to navigate through the waypoints,
     * from the start values to the end values. Default is a catmull-rom spline,
     * but you can find other paths in the {@link TweenPaths} class.
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
	 * Gets the target values. The returned buffer is as long as the maximum
	 * allowed combined values. Therefore, you're surely not interested in all
	 * its content. Use {@link Tween#getCombinedAttributesCount()} to get the number of
	 * interesting slots.
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

	@SuppressWarnings({"unchecked", "FieldRepeatedlyAccessedInMethod"})
    @Override
	public
    Tween build() {
        flushRead();
        final Object target = this.target;
        if (target == null) {
            return this;
        }

        accessor = (TweenAccessor<Object>) registeredAccessors.get(targetClass);
        if (accessor == null && target instanceof TweenAccessor) {
            accessor = (TweenAccessor<Object>) target;
        }
        if (accessor != null) {
            combinedAttrsCnt = accessor.getValues(target, type, accessorBuffer);
        }
        else {
            throw new RuntimeException("No TweenAccessor was found for the target");
        }

        if (combinedAttrsCnt > combinedAttrsLimit) {
            throwCombinedAttrsLimitReached();
        }

        flushWrite();
        return this;
	}

    @Override
    public
    void free() {
        pool.release(this);
    }

	@Override
	protected
    void initialize() {
        flushRead();
        if (target == null) {
            return;
        }

        accessor.getValues(target, type, startValues);

        final int combinedAttrsCnt = this.combinedAttrsCnt;
        final boolean isFrom = this.isFrom;
        final boolean isRelative = this.isRelative;

        for (int i = 0; i < combinedAttrsCnt; i++) {
            targetValues[i] += isRelative ? startValues[i] : 0;

            for (int ii = 0; ii < waypointsCount; ii++) {
                waypoints[ii * combinedAttrsCnt + i] += isRelative ? startValues[i] : 0;
            }

            if (isFrom) {
                float tmp = startValues[i];
                startValues[i] = targetValues[i];
                targetValues[i] = tmp;
            }
        }

        flushWrite();
	}

    /**
     * Updates a timeline's children. Base impl does nothing.
     */
    protected
    void updateChildrenState(final float delta) {
    }


    /**
     * Updates just the values of this tween
     */
    protected
    void updateValues() {
        final Object target = this.target;
        final TweenEquation equation = this.equation;

        if (target == null || equation == null) {
            return;
        }

        final float duration = this.duration;
        final float time = getCurrentTime();
        final boolean direction = getDirection();

        /*
         * When DURATION is not specified (is 0), it means that this object is either START value or END value. Delay still applies
         * to this. This is via Tween.set()
         */
        if (duration == 0F || isFinished()) {
            if (direction) {
                if (time <= 0F) {
                    accessor.setValues(target, type, startValues);
                }
                else {
                    accessor.setValues(target, type, targetValues);
                }
            }
            else {
                if (time > 0F) {
                    accessor.setValues(target, type, targetValues);
                }
                else {
                    accessor.setValues(target, type, startValues);
                }
            }

            return;
        }
        else {
            // do we lock to start/end values when we are at or beyond start/end time
            // --  don't even bother with calculating the tween equation value
            // FORWARDS and REVERSE are different conditions
            //
            // FORWARDS: 0 >= time < duration
            // REVERSE:  0 > time <= duration   (reverse always goes from duration -> 0)
            boolean insideLow;
            boolean insideHigh;
            if (direction) {
                insideLow = time >= 0F;
                insideHigh = time < duration;
            }
            else {
                insideLow = time > 0F;
                insideHigh = time <= duration;
            }

            // outside our time bounds
            if (!insideLow) {
                accessor.setValues(target, type, startValues);
                return;
            }
            if (!insideHigh) {
                accessor.setValues(target, type, targetValues);
                return;
            }
        }

        // Normal behavior
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

    private static
    void throwCombinedAttrsLimitReached() {
        String msg = "You cannot combine more than " + combinedAttrsLimit + " " + "attributes in a tween. You can raise this limit with " +
                     "Tween.setCombinedAttributesLimit(), which should be called once in application initialization code.";
        throw new RuntimeException(msg);
    }

    private static
    void throwWaypointsLimitReached() {
        String msg = "You cannot add more than " + waypointsLimit + " " + "waypoints to a tween. You can raise this limit with " +
                     "Tween.setWaypointsLimit(), which should be called once in application initialization code.";
        throw new RuntimeException(msg);
    }
}
