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
package dorkbox.tweenEngine

import dorkbox.objectPool.ObjectPool.nonBlockingSoftReference
import dorkbox.objectPool.Pool
import dorkbox.objectPool.PoolObject
import java.util.*

/**
 * The TweenEngine is responsible for creating Tweens and Timelines, and can be either managed, or un-managed.
 *
 * If managed, [TweenEngine.update] will update all your tweens and timelines at once, as well as managing the tween/timeline
 * life-cycles for you and releasing pooled objects.
 *
 * If un-managed, then you must update the tween/timeline manually, and make sure to release the tween objects back to the pool manually via
 * [BaseTween.free]
 *
 * Just give it a bunch of tweens or timelines and call [update] periodically, you don't need to do anything else!
 *
 * More fine-grained control is available as well via [update] to update via seconds (1.0F == 1.0 seconds), or
 * [update] to update via nano-seconds.
 *
 *
 * @author dorkbox, llc
 *
 * @see Tween
 * @see Timeline
 */
@Suppress("unused", "unused")
open class TweenEngine internal constructor(
    threadSafe: Boolean,
    private val combinedAttrsLimit: Int = 3,
    private val waypointsLimit: Int = 0,
    private val registeredAccessors: Map<Class<*>, TweenAccessor<*>> = hashMapOf()
) {
    companion object {
        /**
         * @return a builder for creating the TweenEngine.
         */
        fun create(): EngineBuilder {
            return EngineBuilder()
        }

        /**
         * The default update event, which does nothing.
         */
        val NULL_ACTION : ((updatedObject: TweenEngine) -> Unit) = { }

        /**
         * Gets the version number.
         */
        const val version = "8.3.1"


        // for creating arrays slightly faster...
        private val BASE_TWEENS: Array<BaseTween<*>> = emptyArray()

        /**
         * NOTE:
         * The flush() methods are overwritten in the "unsafe" operating mode (along with a non-thread-safe pool), so that there is
         * better performance at the cost of thread visibility/safety
         */
        @Volatile
        private var lightSyncObject = System.nanoTime()


        private fun getTweensCount(objs: List<BaseTween<*>>): Int {
            var count = 0
            var i = 0
            val n = objs.size

            while (i < n) {
                val obj = objs[i]
                count += if (obj is Tween<*>) {
                    1
                } else {
                    getTweensCount((obj as Timeline).children)
                }
                i++
            }
            return count
        }

        private fun getTimelinesCount(objs: List<BaseTween<*>>): Int {
            var count = 0
            var i = 0
            val n = objs.size

            while (i < n) {
                val obj = objs[i]
                if (obj is Timeline) {
                    count += 1 + getTimelinesCount(obj.children)
                }
                i++
            }
            return count
        }

        init {
            // Add this project to the updates system, which verifies this class + UUID + version information
            dorkbox.updates.Updates.add(TweenEngine::class.java, "051b680cf7b34a6fb6a373f1c322da38", version)
        }
    }

    private var poolTimeline: Pool<Timeline>
    private var poolTween: Pool<Tween<*>>

    private val newTweens = ArrayList<BaseTween<*>>(20)
    private val tweenArrayList = ArrayList<BaseTween<*>>(20)

    private var isPaused = false
    private var startEventCallback = NULL_ACTION
    private var endEventCallback = NULL_ACTION
    private var lastTime = 0L


    init {
        val timelinePoolObject: PoolObject<Timeline> = object : PoolObject<Timeline>() {
            override fun newInstance(): Timeline {
                return Timeline(this@TweenEngine)
            }

            override fun onReturn(`object`: Timeline) {
                `object`.destroy()
            }
        }
        val tweenPoolObject: PoolObject<Tween<*>> = object : PoolObject<Tween<*>>() {
            override fun onReturn(`object`: Tween<*>) {
                `object`.destroy()
            }

            override fun newInstance(): Tween<*> {
                return Tween<Any>(this@TweenEngine, combinedAttrsLimit, waypointsLimit)
            }
        }

        if (threadSafe) {
            poolTimeline = nonBlockingSoftReference(timelinePoolObject)
            poolTween = nonBlockingSoftReference(tweenPoolObject)
        } else {
            poolTimeline = nonBlockingSoftReference(timelinePoolObject, ArrayDeque())
            poolTween = nonBlockingSoftReference(tweenPoolObject, ArrayDeque())
        }
    }



    /**
     * Only on public methods.
     *
     * Flushes the visibility of all tween fields from the cache for access/use from different threads.
     *
     * ### This does not block and does not prevent race conditions.
     *
     * @return the last time (in nanos) that the field modifications were flushed
     */
    open fun flushRead(): Long {
        return lightSyncObject
    }

    /**
     * Only on public methods.
     *
     * Flushes the visibility of all tween field modifications from the cache for access/use from different threads.
     *
     * ### This does not block and does not prevent race conditions.
     */
    open fun flushWrite() {
        lightSyncObject = System.nanoTime()
    }



    /**
     * Gets the registered TweenAccessor associated with the given object class.
     *
     * @param someClass An object class.
     */
    fun getRegisteredAccessor(someClass: Class<*>): TweenAccessor<*>? {
        flushRead()
        return registeredAccessors[someClass]
    }

    /**
     * Sets an event handler so that a notification is broadcast when the manager starts updating the current frame of animation.
     *
     * @param startCallback the callback for when the manager starts updating everything. NULL to unset.
     * @return The manager, for instruction chaining.
     */
    fun setStartCallback(startCallback: ((updatedObject: TweenEngine) -> Unit)?): TweenEngine {
        startEventCallback = startCallback ?: NULL_ACTION
        flushWrite()
        return this
    }

    /**
     * Sets an event handler so that a notification is broadcast when the manager finishes updating the current frame of animation.
     *
     * @param endCallback the callback for when the manager finishes updating everything. NULL to unset.
     * @return The manager, for instruction chaining.
     */
    fun setEndCallback(endCallback: ((updatedObject: TweenEngine) -> Unit)?): TweenEngine {
        endEventCallback = endCallback ?: NULL_ACTION
        flushWrite()
        return this
    }

    /**
     * Adds a tween or timeline to the manager and starts or restarts it.
     *
     * @return The manager, for instruction chaining.
     */
    fun add(tween: BaseTween<*>): TweenEngine {
        flushRead()

        addUnsafe(tween)

        flushWrite()
        return this
    }

    /**
     * doesn't sync on anything.
     *
     * Adds a tween or timeline to the manager and starts or restarts it.
     */
    fun addUnsafe(tween: BaseTween<*>) {
        newTweens.add(tween)
        if (tween.isAutoStartEnabled) {
            tween.startUnmanaged__()
        }
    }

    /**
     * Returns true if the manager contains any valid interpolation associated to the given target object.
     */
    fun containsTarget(target: Any): Boolean {
        flushRead()

        tweenArrayList.forEach {
            if (it.containsTarget(target)) {
                return true
            }
        }

        return false
    }

    /**
     * Returns true if the manager contains any valid interpolation associated to the given target object and to the given tween type.
     */
    fun containsTarget(target: Any, tweenType: Int): Boolean {
        flushRead()

        tweenArrayList.forEach {
            if (it.containsTarget(target, tweenType)) {
                return true
            }
        }
        return false
    }

    /**
     * Cancels all managed tweens and timelines.
     */
    fun cancelAll() {
        flushRead()

        tweenArrayList.forEach {
            it.cancel_()
            // can only remove/resize the array list during update, otherwise modification of the list can happen during iteration.
        }

        flushWrite()
    }

    /**
     * Cancels all tweens associated to the given target. Will also cancel timelines containing a tween associated to the given
     * target.
     *
     * @return true if the target was canceled, false if we do not contain the target, and it was not canceled
     */
    fun cancelTarget(target: Any): Boolean {
        flushRead()

        var wasCanceled = false
        tweenArrayList.forEach {
            if (it.cancelTarget(target)) {
                wasCanceled = true
            }
            // can only remove/resize the array list during update, otherwise modification of the list can happen during iteration.
        }

        // flushWrite()  flush is called by cancel.
        return wasCanceled
    }

    /**
     * Cancels every tween associated to the given target and tween type. Will also cancel every timeline containing a tween associated
     * to the given target and tween type.
     *
     * @return true if the target was canceled, false if we do not contain the target, and it was not canceled
     */
    fun cancelTarget(target: Any, tweenType: Int): Boolean {
        flushRead()

        var wasCanceled = false
        tweenArrayList.forEach {
            if (it.cancelTarget(target, tweenType)) {
                wasCanceled = true
            }
            // can only remove/resize the array list during update, otherwise modification of the list can happen during iteration.
        }

        // flushWrite()  flush is called by cancel.
        return wasCanceled
    }

    /**
     * Increases the minimum capacity of the manager. Defaults to 20.
     */
    fun ensureCapacity(minCapacity: Int) {
        flushRead()
        tweenArrayList.ensureCapacity(minCapacity)
        flushWrite()
    }

    /**
     * Pauses the manager. Further update calls won't have any effect.
     */
    fun pause() {
        isPaused = true
        flushWrite()
    }

    /**
     * Resumes the manager, if paused.
     */
    fun resume() {
        isPaused = false
        flushWrite()
    }

    /**
     * Resets the last time this tweenManager had "update" called. This is useful when the timer (that [update]) is
     * usually called on, has been stopped for a while. This prevents the "first" update call to 'snap' to the target values because the
     * time delta update was so large.
     */
    fun resetUpdateTime() {
        lastTime = System.nanoTime()
        flushWrite()
    }

    /**
     * Updates every added tween/timeline based upon the elapsed time between now and the previous time this method was called. This
     * method also handles the tween life-cycles automatically.
     *
     * If a tween is finished, it will be removed from the manager.
     */
    fun update() {
        flushRead()

        val newTime = System.nanoTime()
        val deltaTime = (newTime - lastTime) / 1.0E9f
        lastTime = newTime
        runUpdateUnsafe(deltaTime)

        flushWrite()
    }

    /**
     * Updates every added tween with a delta time in NANO-SECONDS and handles the tween life-cycles automatically. This converts the
     * delta time in nano-seconds to seconds.
     *
     * If a tween is finished, it will be removed from the manager.
     */
    fun update(deltaTimeInNanos: Long) {
        flushRead()
        val deltaTimeInSec = deltaTimeInNanos / 1.0E9f
        runUpdateUnsafe(deltaTimeInSec)
        flushWrite()
    }

    /**
     * Updates every added tween with a delta time in SECONDS and handles the tween life-cycles automatically.
     *
     * If a tween is finished, it will be removed from the manager.
     *
     * The delta time represents the elapsed time in seconds between now and the previous update call. Each tween or timeline manages its
     * local time, and adds this delta to its local time to update itself.
     *
     * Slow motion, fast motion and backward play can be easily achieved by tweaking this delta time. Multiply it by -1 to play the
     * animation backward, or by 0.5 to play it twice slower than its normal speed.
     *
     * @param delta A delta time in SECONDS between now and the previous call.
     */
    fun update(delta: Float) {
        flushRead()
        runUpdateUnsafe(delta)
        flushWrite()
    }

    /**
     * doesn't sync on anything.
     *
     * Updates every added tween with a delta time in SECONDS and handles the tween life-cycles automatically.
     *
     * If a tween is finished, it will be removed from the manager.
     *
     * The delta time represents the elapsed time in seconds between now and the previous update call. Each tween or timeline manages its
     * local time, and adds this delta to its local time to update itself.
     *
     * Slow motion, fast motion and backward play can be easily achieved by tweaking this delta time. Multiply it by -1 to play the
     * animation backward, or by 0.5 to play it twice slower than its normal speed.
     */
    private fun runUpdateUnsafe(delta: Float) {
        if (!isPaused) {
            // on start sync
            startEventCallback.invoke(this)
            val size = newTweens.size
            if (size > 0) {
                tweenArrayList.addAll(newTweens)
                newTweens.clear()
            }

            // this is the only place that REMOVING tweens from their list can occur, otherwise there can be issues with removing tweens
            // in the middle of iteration (above) because of callbacks/etc
            tweenArrayList.forEach {
                it.updateUnsafe(delta)
            }

            val iterator = tweenArrayList.iterator()
            while (iterator.hasNext()) {
                val tween = iterator.next()
                if (tween.isAutoRemoveEnabled) {
                    if (tween.state == BaseTween.FINISHED) {
                        // guarantee the tween/timeline values are set at the end
                        tween.setValues(updateDirection = true, updateValue = false)

                        // needsRefresh = true;
                        iterator.remove()
                        tween.free()
                    } else if (tween.isCanceled) {
                        iterator.remove()
                        tween.free()
                    }
                }
            }

            // on finish sync
            endEventCallback.invoke(this)
        }
    }

    /**
     * Gets the number of managed objects. An object may be a tween or a timeline. A timeline only counts for 1 object, since it manages
     * its children itself.
     *
     * To get the count of running tweens, see [runningTweensCount].
     */
    fun size(): Int {
        flushRead()
        return tweenArrayList.size
    }

    /**
     * Gets the number of running tweens. This number includes the tweens located inside timelines (and nested timelines).
     *
     * **Provided for debug purpose only.**
     */
    @Deprecated("Provided for debug purpose only", ReplaceWith(""))
    fun runningTweensCount(): Int {
        flushRead()
        return getTweensCount(tweenArrayList)
    }

    /**
     * Gets the number of running timelines. This number includes the timelines nested inside other timelines.
     *
     * **Provided for debug purpose only.**
     */
    @Deprecated("Provided for debug purpose only", ReplaceWith(""))
    fun runningTimelinesCount(): Int {
        flushRead()
        return getTimelinesCount(tweenArrayList)
    }

    /**
     * Gets an immutable list of every managed object.
     *
     * **Provided for debug purpose only.**
     */
    @Deprecated("Provided for debug purpose only", ReplaceWith(""))
    fun objects(): List<BaseTween<*>> {
        flushRead()
        return Collections.unmodifiableList(tweenArrayList)
    }



    /**
     * Creates a new timeline with a 'sequential' (A then B) behavior. Its children will be updated one after the other in a sequence.
     *
     * It is not necessary to call [Timeline.end] to close this timeline.
     */
    fun createSequential(): Timeline {
        val timeline = poolTimeline.take()
        flushRead()

        timeline.setupUnsafe(Timeline.Mode.SEQUENTIAL)

        flushWrite()
        return timeline
    }

    /**
     * Creates a new timeline with a 'parallel' (A + B at the same time) behavior. Its children will be updated all at once.
     *
     *
     * It is not necessary to call [Timeline.end] to close this timeline.
     */
    fun createParallel(): Timeline {
        val timeline = poolTimeline.take()
        flushRead()

        timeline.setupUnsafe(Timeline.Mode.PARALLEL)

        flushWrite()
        return timeline
    }

    /**
     * Factory creating a new standard interpolation. This is the most common type of interpolation. The starting values are
     * retrieved automatically after the delay (if any).
     *
     *
     * **You need to set the target values of the interpolation by using one of the target() methods**. The interpolation will run
     * from the starting values to these target values.
     *
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * [Tween.start], as they will be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ([Tween.startUnmanaged] then you will have to manage its lifecycle manually.
     *
     *
     * ```
     * TweenEngine.build().to(myObject, POSITION, 1.0F)
     *                    .value(50, 70)
     *                    .ease(Quad_InOut)
     *                    .start();
     *```
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    fun <T> to(target: T, tweenType: Int, duration: Float): Tween<T> {
        return to(target, tweenType, null, duration)
    }

    /**
     * Factory creating a new standard interpolation. This is the most common type of interpolation. The starting values are
     * retrieved automatically after the delay (if any).
     *
     * **You need to set the target values of the interpolation by using one of the target() methods**. The interpolation will run
     * from the starting values to these target values.
     *
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * [Tween.start], as they will be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ([Tween.startUnmanaged] then you will have to manage its lifecycle manually.
     *
     *
     * ```
     * TweenEngine engine = TweenEngine.build();
     * engine.to(myObject, POSITION, accessorObject, 1.0F)
     *       .value(50, 70)
     *       .ease(Quad_InOut)
     *       .start();
     *```
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
    fun <T> to(target: T, tweenType: Int, targetAccessor: TweenAccessor<T>?, duration: Float): Tween<T> {
        val tween: Tween<T> = takeTween()

        tween.setup__(target, tweenType, targetAccessor, duration)
        tween.ease__(TweenEquations.Quad_InOut)
        tween.path__(TweenPaths.CatmullRom)

        flushWrite()
        return tween
    }

    /**
     * Factory creating a new reversed interpolation. The ending values are retrieved automatically after the delay (if any).
     *
     *
     * **You need to set the starting values of the interpolation by using one of the target() methods**. The interpolation will run
     * from the starting values to these target values.
     *
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * [Tween.start], as they will be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ([Tween.startUnmanaged] then you will have to manage its lifecycle manually.
     *
     *
     *```
     * TweenEngine engine = TweenEngine.build();
     * engine.from(myObject, POSITION, 1.0F)
     *       .value(0, 0)
     *       .ease(Quad_InOut)
     *       .start();
     *```
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    fun <T> from(target: T, tweenType: Int, duration: Float): Tween<T> {
        return from(target, tweenType, null, duration)
    }

    /**
     * Factory creating a new reversed interpolation. The ending values are retrieved automatically after the delay (if any).
     *
     *
     * **You need to set the starting values of the interpolation by using one of the target() methods**. The interpolation will run
     * from the starting values to these target values.
     *
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * [Tween.start], as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ([Tween.startUnmanaged] then you will have to manage it's lifecycle manually.
     *
     *
     * ```
     * TweenEngine engine = TweenEngine.build();
     * engine.from(myObject, POSITION, 1.0F)
     *       .value(0, 0)
     *       .ease(Quad_InOut)
     *       .start();
     *```
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
    fun <T> from(target: T, tweenType: Int, targetAccessor: TweenAccessor<T>?, duration: Float): Tween<T> {
        val tween: Tween<T> = takeTween()

        tween.setup__(target, tweenType, targetAccessor, duration)
        tween.ease__(TweenEquations.Quad_InOut)
        tween.path__(TweenPaths.CatmullRom)
        tween.isFrom = true

        flushWrite()
        return tween
    }

    /**
     * Factory creating a new instantaneous interpolation (thus this is not really an interpolation).
     *
     *
     * **You need to set the target values of the interpolation by using one of the target() methods**. The interpolation will set
     * the target attribute to these values after the delay (if any).
     *
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * [Tween.start], as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ([Tween.startUnmanaged] then you will have to manage it's lifecycle manually.
     *
     *
     * ```
     * TweenEngine engine = TweenEngine.build();
     * engine.set(myObject, POSITION)
     *       .value(50, 70)
     *       .delay(1.0F)
     *       .start();
     *```
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     *
     * @return The generated Tween.
     */
    operator fun <T> set(target: T, tweenType: Int): Tween<T> {
        return set(target, tweenType, null)
    }

    /**
     * Factory creating a new instantaneous interpolation (thus this is not really an interpolation).
     *
     *
     * **You need to set the target values of the interpolation by using one of the target() methods**. The interpolation will set
     * the target attribute to these values after the delay (if any).
     *
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * [Tween.start], as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ([Tween.startUnmanaged] then you will have to manage it's lifecycle manually.
     *
     *
     * ```
     * TweenEngine engine = TweenEngine.build();
     * engine.set(myObject, POSITION)
     *       .value(50, 70)
     *       .delay(1.0F)
     *       .start();
     *```
     *
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param targetAccessor The accessor object (optional) that is used to modify the target values (based on the tween type).
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     *
     * @return The generated Tween.
     */
    operator fun <T> set(target: T, tweenType: Int, targetAccessor: TweenAccessor<T>?): Tween<T> {
        val tween: Tween<T> = takeTween()

        tween.setup__(target, tweenType, targetAccessor, 0.0f)
        tween.ease__(TweenEquations.Quad_In)

        flushWrite()
        return tween
    }

    /**
     * Factory creating a new timer. The given callback will be triggered on each iteration start, after the delay.
     *
     *
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * [Tween.start], as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ([Tween.startUnmanaged] then you will have to manage it's lifecycle manually.
     *
     *
     * ```
     * TweenEngine engine = TweenEngine.build();
     * engine.call(myCallback)
     *       .delay(1.0F)
     *       .repeat(10, 1.0F)
     *       .start();
     *```
     *
     * @param callback The callback that will be triggered on each iteration start.
     *
     * @return The generated Tween.
     *
     * @see TweenEvents
     */
    fun call(callback: Tween<Any>.()->Unit): Tween<Any> {
        val tween: Tween<Any> = takeTween()
        flushRead()

        tween.setupEmpty__()
        tween.addCallback(TweenEvents.START, callback) // Thread/Concurrent safe

        flushWrite()
        return tween
    }

    /**
     * Convenience method to create an empty tween. Such object is only useful when placed inside animation sequences
     * (see [Timeline]), in which it may act as a beacon, so you can set a callback on it in order to trigger some action at the
     * right moment.
     *
     * @return The generated Tween.
     *
     * @see Timeline
     */
    fun mark(): Tween<Int> {
        val tween: Tween<Int> = takeTween()
        tween.setupEmpty__()

        flushWrite()
        return tween
    }

    /**
     * doesn't sync on anything.
     *
     * Convenience method to create an empty tween. Such object is only useful when placed inside animation sequences
     * (see [Timeline]), in which it may act as a beacon, so you can set a callback on it in order to trigger some action at the
     * right moment.
     *
     * @return The generated Tween.
     *
     * @see Timeline
     */
    internal fun mark__(): Tween<Int> {
        val tween: Tween<Int> = takeTween()
        tween.setupEmpty__()
        return tween
    }


    /**
     * This doesn't sync on anything.
     * - After assigning values to this tween, you must call [flushWrite].
     * - When DONE with this object, you should return it to the pool via [free]
     */
    fun takeTimeline(): Timeline {
        return poolTimeline.take()
    }

    /**
     * This doesn't sync on anything.
     * - After assigning values to this tween, you must call [flushWrite].
     * - When DONE with this object, you should return it to the pool via [free]
     */
    fun <T> takeTween(): Tween<T> {
        @Suppress("UNCHECKED_CAST")
        return poolTween.take() as Tween<T>
    }


    /**
     * Returns this object to the pool for use later on
     */
    fun free(timeline: Timeline) {
        poolTimeline.put(timeline)
    }

    /**
     * Returns this object to the pool for use later on
     */
    fun <T> free(tween: Tween<T>) {
        poolTween.put(tween)
    }

    fun containsAccessor(accessorClass: Class<*>): Boolean {
        return registeredAccessors.containsKey(accessorClass)
    }

    fun <T> getAccessor(accessorClass: Class<*>): TweenAccessor<T>? {
        val accessor = registeredAccessors[accessorClass]
        return if (accessor == null) {
            null
        } else {
            @Suppress("UNCHECKED_CAST")
            accessor as TweenAccessor<T>
        }
    }
}
