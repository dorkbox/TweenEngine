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
@file:Suppress("FunctionName")

package dorkbox.tweenEngine

/**
 * ## Core class of the Tween Engine.
 *
 * A Tween is basically an interpolation between two values of an object attribute. However, the main
 * interest of a Tween is that you can apply an easing formula on this interpolation, in order to smooth the transitions or to achieve
 * cool effects like springs or bounces.
 *
 * The Tween Engine is also "universal" because it is able to apply interpolations on every attribute from every possible object. Therefore,
 * every object in your application can be animated with cool effects: it does not matter if your application is a game, a desktop
 * interface or even a console program! If it makes sense to animate something, then it can be animated through this engine.
 *
 * This class contains many static factory methods to create and instantiate new interpolations easily. The common way to create a Tween is
 * by using one of these factories:
 *
 *
 * - TweenEngine.to(...)
 *
 * - TweenEngine.from(...)
 *
 * - TweenEngine.set(...)
 *
 * - TweenEngine.call(...)
 *
 *
 * ## Example - firing a Tween
 *
 * The following example will move the target horizontal position from its current value to x=200 and y=300, during 500ms, but only after
 * a delay of 1000ms. The animation will also be repeated 2 times (the starting position is registered at the end of the delay, so the
 * animation will automatically restart from this registered position).
 *
 * ```
 * Tween.to(myObject, POSITION_XY, 500)
 *      .value(200, 300)
 *      .ease(Quad_InOut)
 *      .delay(1.0F)
 *      .repeat(2, 0.02F)
 *      .start(myManager);
 *```
 *
 * Tween life-cycles can be automatically managed for you, thanks to the [TweenEngine] class. If you choose to manage your tween
 * when you start it, then you don't need to care about it anymore. **Tweens are *fire-and-forget*: don't think about them anymore
 * once you started them (if they are managed of course).**
 *
 * You need to periodically update the tween engine, in order to compute the new values. If your tweens are managed, only update the
 * manager; else you need to call [Tween.updateUnsafe] on your tweens periodically.
 *
 * ## Example - setting up the engine
 *
 * The engine cannot directly change your objects attributes, since it doesn't know them. Therefore, you need to tell him how to get and
 * set the different attributes of your objects: **you need to implement the [TweenAccessor] interface for each object class you
 * will animate**. Once done, don't forget to register these implementations, using the static method
 * [EngineBuilder.registerAccessor], when you start your application.
 *
 * @see TweenAccessor
 * @see TweenEngine
 * @see TweenEquation
 * @see Timeline
 *
 *
 * @author dorkbox, llc
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class Tween<T> internal constructor(animator: TweenEngine, private val combinedAttrsLimit: Int, private val waypointsLimit: Int) : BaseTween<Tween<T>>(animator) {

    companion object {
        /**
         * Gets the version number.
         */
        const val version = TweenEngine.version

        /**
         * Used as parameter in [repeat] and [repeatAutoReverse] methods.
         */
        const val INFINITY = -1
    }

    private var target: T? = null
    private var targetClass: Class<*>? = null
    private var accessor: TweenAccessor<T>? = null
    private var type = 0
    private var equation: TweenEquation? = null
    private var path: TweenPath? = null

    internal var isFrom = false
    private var isRelative = false
    private var combinedAttrsCnt = 0
    private var waypointsCount = 0

    // Values
    private val startValues = FloatArray(combinedAttrsLimit)
    private val targetValues = FloatArray(combinedAttrsLimit)
    private val waypoints = FloatArray(waypointsLimit * combinedAttrsLimit)

    // Buffers
    private val accessorBuffer = FloatArray(combinedAttrsLimit)
    private val pathBuffer = FloatArray((2 + waypointsLimit) * combinedAttrsLimit)

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------
    init {
        destroy()
    }

    @Suppress("DuplicatedCode")
    public override fun destroy() {
        super.destroy()

        target = null
        targetClass = null
        accessor = null
        type = -1
        equation = null
        path = null
        isRelative = false
        isFrom = false

        waypointsCount = 0
        combinedAttrsCnt = 0


        var i = 0
        var n = startValues.size
        while (i < n) {
            startValues[i] = 0.0f
            i++
        }

        i = 0
        n = targetValues.size
        while (i < n) {
            targetValues[i] = 0.0f
            i++
        }

        i = 0
        n = waypoints.size
        while (i < n) {
            waypoints[i] = 0.0f
            i++
        }

        i = 0
        n = accessorBuffer.size
        while (i < n) {
            accessorBuffer[i] = 0.0f
            i++
        }

        i = 0
        n = pathBuffer.size
        while (i < n) {
            pathBuffer[i] = 0.0f
            i++
        }
    }

    /**
     * doesn't sync on anything.
     */
    internal fun setupEmpty__() {
        this.target = null
        this.accessor = null
        this.targetClass = null

        type = -1
        this.duration = 0.0f
        this.setup__()
    }

    /**
     * doesn't sync on anything.
     */
    internal fun setup__(target: T, tweenType: Int, targetAccessor: TweenAccessor<T>?, duration: Float) {
        if (duration < 0.0f) {
            throw IllegalArgumentException("Duration can not be negative")
        }

        this.target = target
        if (targetAccessor != null) {
            accessor = targetAccessor
        } else {
            targetClass = if (target != null) findTargetClass__() else null
        }

        type = tweenType
        this.duration = duration
        this.setup__()
    }

    /**
     * look to see if we have a registered accessor somewhere in the object hierarchy
     *
     * doesn't sync on anything.
     */
    private fun findTargetClass__(): Class<*> {
        val target = target!!
        val javaClass = target.javaClass

        if (target is TweenAccessor<*>) {
            // the target is of the expected type
            return javaClass
        }

        if (animator.containsAccessor(javaClass)) {
            // the target is registered with an accessor
            return javaClass
        }


        var parentClass: Class<*>? = javaClass.superclass
        while (parentClass != null && !animator.containsAccessor(parentClass)) {
            parentClass = parentClass.superclass
        }

        @Suppress("IfThenToElvis")
        return if (parentClass != null) {
            // we found a class that has an accessor assigned to it (and our target can use it)
            parentClass
        } else {
            // no accessor for this class.
            javaClass
        }
    }


    // -------------------------------------------------------------------------
    // Common Public API
    // -------------------------------------------------------------------------
    /**
     * Adds a callback. By default, it will be fired at the completion of the tween (event COMPLETE). If you want to change
     * this behavior use the [TweenEvents] constructor.
     *
     * Thread/Concurrent safe
     *
     * @see TweenEvents
     *
     * @return The current tween
     */
    public override fun addCallback(triggers: Int, callback: Tween<T>.()->Unit): Tween<T> {
        super.addCallback(triggers, callback)
        return this
    }

    /**
     * Clears all the callbacks.
     *
     * Thread/Concurrent safe
     *
     * @return The current tween
     */
    public override fun clearCallbacks(): Tween<T> {
        super.clearCallbacks()
        return this
    }

    /**
     * Stops and resets the tween, and sends it to its pool, for later reuse.
     *
     *
     * If started normally (instead of un-managed), the [TweenEngine] will automatically call this method once the animation is complete.
     */
    override fun free() {
        animator.free(this)
    }

    /**
     * Adds a start delay to the tween or timeline in seconds.
     *
     * @param delay A duration in seconds for the delay
     *
     * @return The current tween
     */
    public override fun delay(delay: Float): Tween<T> {
        super.delay(delay)
        return this
    }

    /**
     * Repeats the tween for a given number of times.
     *
     * @param count The number of repetitions. For infinite repetition, use [Tween.INFINITY] or -1.
     * @param delay A delay between each iteration, in seconds.
     *
     * @return The current tween
     */
    public override fun repeat(count: Int, delay: Float): Tween<T> {
        super.repeat(count, delay)
        return this
    }

    /**
     * Repeats the tween for a given number of times.
     *
     * Once an iteration is complete, it will be played in reverse.
     *
     * @param count The number of repetitions. For infinite repetition, use [Tween.INFINITY] or -1.
     * @param delay A delay before each repetition, in seconds.
     *
     * @return The current tween
     */
    public override fun repeatAutoReverse(count: Int, delay: Float): Tween<T> {
        super.repeatAutoReverse(count, delay)
        return this
    }

    /**
     * Sets the "start" callback, which is called when the tween starts running, NULL to remove.
     *
     * @param startCallback this is the object that will be notified when the tween starts running. NULL to unset.
     *
     * @return The current tween
     */
    override fun setStartCallback(startCallback: ((updatedObject: Tween<T>) -> Unit)?): Tween<T> {
        super.setStartCallback(startCallback)
        return this
    }

    /**
     * Sets the "end" callback, which is called when the tween finishes running, NULL to remove.
     *
     * @param endCallback this is the object that will be notified when the tween finishes running. NULL to unset.
     *
     * @return The current tween
     */
    override fun setEndCallback(endCallback: ((updatedObject: Tween<T>) -> Unit)?): Tween<T> {
        super.setEndCallback(endCallback)
        return this
    }

    /**
     * Sets the tween to a specific point in time based on its duration + delays. Callbacks are not notified and the change is
     * immediate. The tween will continue in its original direction
     * For example:
     *
     *  *  setProgress(0F, true) : set it to the starting position just after the start delay in the forward direction
     *  *  setProgress(.5F, true) : set it to the middle position in the forward direction
     *  *  setProgress(.5F, false) : set it to the middle position in the reverse direction
     *  *  setProgress(1F, false) : set it to the end position in the reverse direction
     *
     * Caveat: If the tween is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished state
     * (end position). If the tween is in repeat mode then it will end up in the same position if it was going forwards.
     *
     * @param percentage the percentage (of its duration) from 0-1, that the tween be set to
     */
    public override fun setProgress(percentage: Float): Tween<T> {
        super.setProgress(percentage)
        return this
    }

    /**
     * Sets the tween to a specific point in time based on its duration + delays. Callbacks are not notified and the change is
     * immediate.
     * For example:
     *
     *  *  setProgress(0F, true) : set it to the starting position just after the start delay in the forward direction
     *  *  setProgress(.5F, true) : set it to the middle position in the forward direction
     *  *  setProgress(.5F, false) : set it to the middle position in the reverse direction
     *  *  setProgress(1F, false) : set it to the end position in the reverse direction
     *
     *
     *
     * Caveat: If the tween is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished state
     * (end position). If the timeline/tween is in repeat mode then it will end up in the same position if it was going forwards.
     *
     * @param percentage the percentage (of its duration) from 0-1, that the tween be set to
     * @param direction sets the direction of the timeline when it updates next: forwards (true) or reverse (false).
     */
    public override fun setProgress(percentage: Float, direction: Boolean): Tween<T> {
        super.setProgress(percentage, direction)
        return this
    }

    /**
     * Starts or restarts the tween unmanaged. You will need to take care of its life-cycle.
     *
     * @return The current tween
     */
    public override fun startUnmanaged(): Tween<T> {
        animator.flushRead()
        startUnmanaged__()
        animator.flushWrite()
        return this
    }

    override fun startUnmanaged__() {
        super.startUnmanaged__()
        val target = target ?: return


        if (accessor == null) {
            accessor = if (target is TweenAccessor<*>) {
                @Suppress("UNCHECKED_CAST")
                target as TweenAccessor<T>
            } else {
                // target cannot be null, so we know that targetClass *also* cannot be null
                animator.getAccessor(targetClass!!)
            }
        }

        combinedAttrsCnt = if (accessor != null) {
            accessor!!.getValues(target, type, accessorBuffer)
        } else {
            throw NullPointerException("No TweenAccessor was found for the target")
        }
        verifyCombinedAttrs(combinedAttrsCnt)
    }

    /**
     * Convenience method to add an object to a tween where it's life-cycle will be automatically handled .
     *
     * @return The current tween
     */
    override fun start(): Tween<T> {
        super.start()
        return this
    }


    // -------------------------------------------------------------------------
    // User Data
    // -------------------------------------------------------------------------
    /**
     * Attaches an object to this tween. It can be useful in order
     * to retrieve some data from a TweenEvent Callback.
     *
     * @param data Any kind of object.
     *
     * @return The current tween
     */
    public override fun setUserData(data: Any?): Tween<T> {
        super.setUserData(data)
        return this
    }


    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sets the easing equation of the tween. Existing equations are located in [TweenEquations], but you can of course implement your
     * own, see [TweenEquation].
     *
     *
     * Default equation is Quad_InOut.
     *
     *
     *
     * @return The current tween
     * @see TweenEquation
     *
     * @see TweenEquations
     */
    fun ease(easeEquation: TweenEquation?): Tween<T> {
        equation = easeEquation
        animator.flushWrite()
        return this
    }

    /**
     * Sets the easing equation of the tween. Existing equations are located in [TweenEquations], but you can of course implement
     * your own, see [TweenEquation].
     *
     *
     * Default equation is Quad_InOut.
     *
     *
     *
     * @return The current tween
     * @see TweenEquation
     *
     * @see TweenEquations
     */
    fun ease(easeEquation: TweenEquations): Tween<T> {
        equation = easeEquation.equation
        animator.flushWrite()
        return this
    }

    /**
     * doesn't sync on anything.
     *
     *
     * Sets the easing equation of the tween. Existing equations are located in [TweenEquations], but you can of course implement
     * your own, see [TweenEquation].
     *
     *
     * Default equation is Quad_InOut.
     *
     *
     *
     * @return The current tween
     * @see TweenEquation
     *
     * @see TweenEquations
     */
    internal fun ease__(easeEquation: TweenEquations): Tween<T> {
        equation = easeEquation.equation
        return this
    }

    /**
     * Forces the tween to use the TweenAccessor registered with the given target class. Useful if you want to use a specific accessor
     * associated to an interface, for instance.
     *
     * @param targetClass A class registered with an accessor.
     *
     * @return The current tween
     */
    fun cast(targetClass: Class<*>?): Tween<T> {
        animator.flushRead()
        if (isInitialized) {
            throw IllegalArgumentException("You can't cast the target of a tween once it has been initialized")
        }
        this.targetClass = targetClass
        animator.flushWrite()
        return this
    }

    /**
     * Sets the target value of the interpolation. The interpolation will run from the **value at start time (after the delay, if any)**
     * to this target value.
     *
     * ### To sum-up:
     * - start value: value at start time, after delay
     * - end value: param
     *
     * @param targetValue The target value of the interpolation.
     *
     * @return The current tween
     */
    fun value(targetValue: Float): Tween<T> {
        targetValues[0] = targetValue

        animator.flushWrite()
        return this
    }

    /**
     * Sets the target values of the interpolation. The interpolation will run from the **values at start time (after the delay, if
     * any)** to these target values.
     *
     * ### To sum-up:
     * - start values: values at start time, after delay<
     * - end values: params
     *
     * @param targetValue1 The 1st target value of the interpolation.
     * @param targetValue2 The 2nd target value of the interpolation.
     *
     * @return The current tween
     */
    fun value(targetValue1: Float, targetValue2: Float): Tween<T> {
        targetValues[0] = targetValue1
        targetValues[1] = targetValue2

        animator.flushWrite()
        return this
    }

    /**
     * Sets the target values of the interpolation. The interpolation will run from the **values at start time (after the delay, if
     * any)** to these target values.
     *
     * ### To sum-up:
     * - start values: values at start time, after delay
     * - end values: params
     *
     * @param targetValue1 The 1st target value of the interpolation.
     * @param targetValue2 The 2nd target value of the interpolation.
     * @param targetValue3 The 3rd target value of the interpolation.
     *
     * @return The current tween
     */
    fun value(targetValue1: Float, targetValue2: Float, targetValue3: Float): Tween<T> {
        targetValues[0] = targetValue1
        targetValues[1] = targetValue2
        targetValues[2] = targetValue3

        animator.flushWrite()
        return this
    }

    /**
     * Sets the target values of the interpolation. The interpolation will run from the **values at start time (after the delay, if
     * any)** to these target values.
     *
     * ### To sum-up:
     * - start values: values at start time, after delay
     * - end values: params
     *
     * @param targetValues The target values of the interpolation.
     *
     * @return The current tween
     */
    fun value(vararg targetValues: Float): Tween<T> {
        animator.flushRead()
        val length = targetValues.size
        verifyCombinedAttrs(length)
        System.arraycopy(targetValues, 0, this.targetValues, 0, length)

        animator.flushWrite()
        return this
    }

    /**
     * Sets the target value of the interpolation, relatively to the **value at start time (after the delay, if any)**.
     *
     * ### To sum-up:
     * - start value: value at start time, after delay
     * - end value: param + value at start time, after delay
     *
     * @param targetValue The relative target value of the interpolation.
     *
     * @return The current tween
     */
    fun valueRelative(targetValue: Float): Tween<T> {
        animator.flushRead()
        isRelative = true

        targetValues[0] = if (isInitialized) targetValue + startValues[0] else targetValue

        animator.flushWrite()
        return this
    }

    /**
     * Sets the target value of the interpolation, relatively to the **value at start time (after the delay, if any)**.
     *
     * ### To sum-up:
     * - start values: values at start time, after delay
     * - end values: params + values at start time, after delay
     *
     * @param targetValue1 The 1st relative target value of the interpolation.
     * @param targetValue2 The 2nd relative target value of the interpolation.
     *
     * @return The current tween
     */
    fun valueRelative(targetValue1: Float, targetValue2: Float): Tween<T> {
        animator.flushRead()
        isRelative = true

        val initialized = isInitialized
        targetValues[0] = if (initialized) targetValue1 + startValues[0] else targetValue1
        targetValues[1] = if (initialized) targetValue2 + startValues[1] else targetValue2

        animator.flushWrite()
        return this
    }

    /**
     * Sets the target value of the interpolation, relatively to the **value at start time (after the delay, if any)**.
     *
     * ### To sum-up:
     * - start values: values at start time, after delay
     * - end values: params + values at start time, after delay
     *
     * @param targetValue1 The 1st relative target value of the interpolation.
     * @param targetValue2 The 2nd relative target value of the interpolation.
     * @param targetValue3 The 3rd relative target value of the interpolation.
     *
     * @return The current tween
     */
    fun valueRelative(targetValue1: Float, targetValue2: Float, targetValue3: Float): Tween<T> {
        animator.flushRead()
        isRelative = true

        val initialized = this.isInitialized
        val startValues = startValues
        targetValues[0] = if (initialized) targetValue1 + startValues[0] else targetValue1
        targetValues[1] = if (initialized) targetValue2 + startValues[1] else targetValue2
        targetValues[2] = if (initialized) targetValue3 + startValues[2] else targetValue3

        animator.flushWrite()
        return this
    }

    /**
     * Sets the target value of the interpolation, relatively to the **value at start time (after the delay, if any)**.
     *
     * ### To sum-up:
     * - start values: values at start time, after delay
     * - end values: params + values at start time, after delay
     *
     * @param targetValues The relative target values of the interpolation.
     *
     * @return The current tween
     */
    fun valueRelative(vararg targetValues: Float): Tween<T> {
        animator.flushRead()
        isRelative = true

        val length = targetValues.size
        verifyCombinedAttrs(length)
        val initialized = isInitialized
        val startValues = startValues
        for (i in 0 until length) {
            this.targetValues[i] = if (initialized) targetValues[i] + startValues[i] else targetValues[i]
        }

        animator.flushWrite()
        return this
    }

    /**
     * Adds a waypoint to the path. The default path runs from the start values to the end values linearly. If you add waypoints, the
     * default path will use a smooth catmull-rom spline to navigate between the waypoints, but you can change this behavior by using the
     * [path] method.
     *
     * @param targetValue The target of this waypoint.
     *
     * @return The current tween
     */
    fun waypoint(targetValue: Float): Tween<T> {
        animator.flushRead()
        val waypointsCount = waypointsCount
        verifyWaypoints(waypointsCount)
        waypoints[waypointsCount] = targetValue
        this.waypointsCount += 1
        animator.flushWrite()
        return this
    }

    /**
     * Adds a waypoint to the path. The default path runs from the start values to the end values linearly. If you add waypoints, the
     * default path will use a smooth catmull-rom spline to navigate between the waypoints, but you can change this behavior by using the
     * [path] method.
     *
     * Note that if you want waypoints relative to the start values, use one of the [valueRelative] methods to define your target.
     *
     * @param targetValue1 The 1st target of this waypoint.
     * @param targetValue2 The 2nd target of this waypoint.
     *
     * @return The current tween
     */
    fun waypoint(targetValue1: Float, targetValue2: Float): Tween<T> {
        animator.flushRead()

        val waypointsCount = waypointsCount
        verifyWaypoints(waypointsCount)
        val count = waypointsCount * 2
        val waypoints = waypoints
        waypoints[count] = targetValue1
        waypoints[count + 1] = targetValue2
        this.waypointsCount += 1

        animator.flushWrite()
        return this
    }

    /**
     * Adds a waypoint to the path. The default path runs from the start values to the end values linearly. If you add waypoints, the
     * default path will use a smooth catmull-rom spline to navigate between the waypoints, but you can change this behavior by using the
     * [path] method.
     *
     * Note that if you want waypoints relative to the start values, use one of the [valueRelative] methods to define your target.
     *
     * @param targetValue1 The 1st target of this waypoint.
     * @param targetValue2 The 2nd target of this waypoint.
     * @param targetValue3 The 3rd target of this waypoint.
     *
     * @return The current tween
     */
    fun waypoint(targetValue1: Float, targetValue2: Float, targetValue3: Float): Tween<T> {
        animator.flushRead()

        val waypointsCount = waypointsCount
        verifyWaypoints(waypointsCount)
        val count = waypointsCount * 3
        val waypoints = waypoints
        waypoints[count] = targetValue1
        waypoints[count + 1] = targetValue2
        waypoints[count + 2] = targetValue3
        this.waypointsCount += 1

        animator.flushWrite()
        return this
    }

    /**
     * Adds a waypoint to the path. The default path runs from the start values to the end values linearly. If you add waypoints, the
     * default path will use a smooth catmull-rom spline to navigate between the waypoints, but you can change this behavior by using the
     * [path] method.
     *
     * Note that if you want waypoints relative to the start values, use one of the [valueRelative] methods to define your target.
     *
     * @param targetValues The targets of this waypoint.
     *
     * @return The current tween
     */
    fun waypoint(vararg targetValues: Float): Tween<T> {
        animator.flushRead()

        val waypointsCount = waypointsCount
        verifyWaypoints(waypointsCount)
        System.arraycopy(targetValues, 0, waypoints, waypointsCount * targetValues.size, targetValues.size)
        this.waypointsCount += 1

        animator.flushWrite()
        return this
    }

    /**
     * Sets the algorithm that will be used to navigate through the waypoints, from the start values to the end values. Default is a
     * catmull-rom spline, but you can find other paths in the [TweenPaths] class.
     *
     * @param path A TweenPath implementation.
     *
     * @return The current tween
     * @see TweenPath
     *
     * @see TweenPaths
     */
    fun path(path: TweenPath?): Tween<T> {
        this.path = path
        animator.flushWrite()
        return this
    }

    /**
     * doesn't sync on anything.
     *
     *
     * Sets the algorithm that will be used to navigate through the waypoints, from the start values to the end values. Default is a
     * catmull-rom spline, but you can find other paths in the [TweenPaths] class.
     *
     * @param path A TweenPath implementation.
     *
     * @return The current tween
     * @see TweenPath
     *
     * @see TweenPaths
     */
    internal fun path__(path: TweenPaths): Tween<T> {
        this.path = path.path
        return this
    }

    /**
     * doesn't sync on anything.
     *
     *
     * Sets the algorithm that will be used to navigate through the waypoints, from the start values to the end values. Default is a
     * catmull-rom spline, but you can find other paths in the [TweenPaths] class.
     *
     * @param path A TweenPath implementation.
     *
     * @return The current tween
     * @see TweenPath
     *
     * @see TweenPaths
     */
    internal fun path__(path: TweenPath?): Tween<T> {
        this.path = path
        return this
    }



    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------
    /**
     * Gets the target object.
     *
     * This is useful when getting the tween target object during different animation state callbacks
     */
    fun getTarget(): T {
        animator.flushRead()
        return target!!
    }

    /**
     * Gets the type of the tween.
     */
    fun getType(): Int {
        animator.flushRead()
        return type
    }

    /**
     * Gets the easing equation.
     */
    fun easing(): TweenEquation? {
        animator.flushRead()
        return equation
    }

    /**
     * Gets the target values. The returned buffer is as long as the maximum allowed combined values. Therefore, you're surely not
     * interested in all its content. Use [combinedAttributesCount] to get the number of interesting slots.
     */
    fun getTargetValues(): FloatArray {
        animator.flushRead()
        return targetValues
    }

    /**
     * Gets the number of combined animations.
     */
    fun combinedAttributesCount(): Int {
        animator.flushRead()
        return combinedAttrsCnt
    }

    /**
     * Gets the TweenAccessor used with the target.
     */
    fun getAccessor(): TweenAccessor<*>? {
        animator.flushRead()
        return accessor
    }

    /**
     * Gets the class that was used to find the associated TweenAccessor.
     */
    fun getTargetClass(): Class<*>? {
        animator.flushRead()
        return targetClass
    }


    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------
    /**
     * Forces a timeline/tween to have its start/target values. Repeat behavior is also correctly modeled in the decision process
     *
     * @param updateDirection direction in which the update is happening. Affects children iteration order (timelines)
     * @param updateValue this is the start (true) or target (false) to set the tween to.
     */
    override fun setValues(updateDirection: Boolean, updateValue: Boolean) {
        val target = target

        if (target == null || !this.isInitialized || isCanceled) {
            return
        }
        if (updateValue) {
            // and want the "start" (always relative to forwards)
            accessor!!.setValues(target, type, startValues)
        } else {
            // and want the "end" (always relative to forwards)
            if (canAutoReverse && repeatCountOrig and 1 != 0) { // odd
                // repeats REALLY make this complicated, because reverse auto-repeats flip the logic, but only if an odd count
                // so if we are ODD, then we are actually at the "start" value. If EVEN, we are at the "target" value
                accessor!!.setValues(target, type, startValues)
            } else {
                accessor!!.setValues(target, type, targetValues)
            }
        }
    }

    override fun initializeValues() {
        val target = target
        if (target == null || isCanceled) {
            return
        }

        // NOTICE: can't updateValues before we initialize them
        val startValues = startValues
        val targetValues = targetValues
        val combinedAttrsCnt = combinedAttrsCnt
        accessor!!.getValues(target, type, startValues)

        // expanded form of "isRelative" + "isFrom"
        //  isRelative is target += start
        // !isRelative is target = 0;
        // isFrom FLIPS start & target values
        if (isRelative) {
            val waypointsCount = waypointsCount
            val waypoints = waypoints
            if (isFrom) {
                for (i in 0 until combinedAttrsCnt) {
                    targetValues[i] += startValues[i]
                    for (ii in 0 until waypointsCount) {
                        waypoints[ii * combinedAttrsCnt + i] += startValues[i]
                    }

                    // unique for "isFrom"
                    val tmp = startValues[i]
                    startValues[i] = targetValues[i]
                    targetValues[i] = tmp
                }
            } else {
                for (i in 0 until combinedAttrsCnt) {
                    targetValues[i] += startValues[i]
                    for (ii in 0 until waypointsCount) {
                        waypoints[ii * combinedAttrsCnt + i] += startValues[i]
                    }
                }
            }
        } else if (isFrom) {
            for (i in 0 until combinedAttrsCnt) {
                // unique for "isFrom"
                val tmp = startValues[i]
                startValues[i] = targetValues[i]
                targetValues[i] = tmp
            }
        }
    }

    /**
     * When done with all the adjustments and notifications, update the object. Only called during [BaseTween.RUN] Other state updates will
     * use [BaseTween.setValues]
     *
     *
     * values will ONLY be updated if the tween was initialized (reached START state at least once)
     *
     *
     * If a timeline/tween is outside its animation cycle time, it will "snap" to the start/end points via
     * [BaseTween.setValues]
     *
     * @param updateDirection not used (only used by the timeline). It is necessary here because of how the methods are overloaded.
     * @param delta the time in SECONDS that has elapsed since the last update
     */
    override fun updateUnsafe(updateDirection: Boolean, delta: Float) {
        val target = target
        val equation = equation

        // be aware that a tween can ONLY have its values updated IFF it has been initialized (reached START state at least once)
        if (target == null || equation == null || !this.isInitialized || isCanceled) {
            return
        }
        val duration = duration
        val time = currentTime


        // Normal behavior. Have to convert to at this point
        val tweenValue = equation.compute(time / duration)
        val accessorBuffer = accessorBuffer
        val combinedAttrsCnt = combinedAttrsCnt
        val waypointsCnt = waypointsCount
        val path = path
        val startValues = startValues
        val targetValues = targetValues

        if (waypointsCnt == 0 || path == null) {
            for (i in 0 until combinedAttrsCnt) {
                accessorBuffer[i] = startValues[i] + (tweenValue * (targetValues[i] - startValues[i]))
            }
        } else {
            val waypoints = waypoints
            val pathBuffer = pathBuffer
            for (i in 0 until combinedAttrsCnt) {
                pathBuffer[0] = startValues[i]
                pathBuffer[1 + waypointsCnt] = targetValues[i]
                for (ii in 0 until waypointsCnt) {
                    pathBuffer[ii + 1] = waypoints[ii * combinedAttrsCnt + i]
                }
                accessorBuffer[i] = path.compute(tweenValue, pathBuffer, waypointsCnt + 2)
            }
        }

        accessor!!.setValues(target, type, accessorBuffer)
    }

    // -------------------------------------------------------------------------
    // BaseTween impl.
    // -------------------------------------------------------------------------
    override fun containsTarget(target: Any): Boolean {
        return this.target === target
    }

    override fun containsTarget(target: Any, tweenType: Int): Boolean {
        return this.target === target && type == tweenType
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    /**
     * @return true if the combined attributes limit been reached
     */
    private fun verifyCombinedAttrs(length: Int) {
        if (length > combinedAttrsLimit) {
            val msg = "You cannot combine more than " + combinedAttrsLimit + " " + "attributes in a tween. You can raise this limit with " +
                    "Tween.setCombinedAttributesLimit(), which should be called once in application initialization code."
            throw IllegalArgumentException(msg)
        }
    }

    /**
     * @return true if the waypoints limit been reached
     */
    private fun verifyWaypoints(waypointsCount: Int) {
        if (waypointsCount == waypointsLimit) {
            val msg = "You cannot add more than " + waypointsLimit + " " + "waypoints to a tween. You can raise this limit with " +
                    "Tween.setWaypointsLimit(), which should be called once in application initialization code."
            throw IllegalArgumentException(msg)
        }
    }
}
