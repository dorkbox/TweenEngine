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

import java.util.concurrent.locks.*
import kotlin.concurrent.write

/**
 * BaseTween is the base class of Tween and Timeline. It defines the iteration engine used to play animations for any number of times,
 * and in any direction, at any speed.
 *
 *
 * It is responsible for calling the different callbacks at the right moments, and for making sure that every callbacks are triggered,
 * even if the update engine gets a big delta time at once.
 *
 * @see Tween
 * @see Timeline
 *
 * @author dorkbox, llc
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseTween<T : BaseTween<T>>(protected val animator: TweenEngine) {

    companion object {
        // if there is a DELAY, the tween will remain inside "START" until it's finished with the delay
        internal const val INVALID = 0
        internal const val START = 1
        internal const val RUN = 2
        internal const val FINISHED = 3

        private const val START_VALUES = true
        private const val TARGET_VALUES = false

        // Direction state
        private const val FORWARDS = true
        private const val REVERSE = false

    }

    // callbacks (optimized for fast call w/ many callbacks). Verification for multiple triggers is on add.
    private val callbackLock = ReentrantReadWriteLock()
    private val emptyCallback = emptyArray<T.(triggers: Int)->Unit>()

    /**
     * The default update event, which does nothing.
     */
    val emptyAction: (updatedObject: T)->Unit = { }

    // we are a simple state machine...
    var state = 0

    // General
    var repeatCountOrig = 0
    private var repeatCount = 0
    protected var canAutoReverse = false
    private var isPaused = false

    /** Used by tween  */
    var isCanceled = false

    /**
     * Returns true if the Timeline/Tween has been initialized. This is the most accurate method to determine if a Timeline/Tween has
     * been started.
     */
    protected var isInitialized = false
        get() {
            animator.flushRead()
            return field
        }



    // Timings
    private var startDelay = 0f // this is the initial delay at the start of a timeline/tween (only happens once). (doesn't change)
    private var repeatDelay = 0f // this is the delay when a timeline/tween is repeating (doesn't change)



    /**
     * Gets the duration of a Timeline/Tween "single iteration" (not counting repeats) in seconds
     */
    var duration = 0f // how long the timeline/tween runs (doesn't change)
        get() {
            animator.flushRead()
            return field
        }

    // represents the amount of time spent in the current iteration or delay
    // protected because our timeline has to be able to adjust for delays when initially building the system.
    // when FORWARDS - if <= 0, it is a delay
    // when REVERSE - if >= duration, it is a delay

    /** Used by timeline  */


   /**
    * UNSAFE
    * Gets the current time point of a Timeline/Tween in seconds
    */
    internal var currentTime = 0f

    /**
     * Gets the current time point of a Timeline/Tween in seconds
     */
    fun getCurrentTime(): Float {
        animator.flushRead()
        return currentTime
    }





    private var direction = FORWARDS // default state is forwards

    /** Depending on the state, sometimes we trigger begin events  */
    private var canTriggerBeginEvent = false


    /**
     * UNSAFE
     * @return true if the timeline/tween is currently "auto-reversing" in its direction.
     */
    internal var isInAutoReverse = false

    /**
     * @return true if the timeline/tween is currently "auto-reversing" in its direction.
     */
    fun getIisInAutoReverse(): Boolean {
        animator.flushRead()
        return isInAutoReverse
    }

    // Misc
    private var userData: Any? = null

    /** Used by tween manager  */
    var isAutoRemoveEnabled = false
    var isAutoStartEnabled = false

    private var startEventCallback = emptyAction
    private var endEventCallback = emptyAction

    private var forwards_Begin =    emptyArray<T.(triggers: Int)->Unit>()
    private var forwards_Start =    emptyArray<T.(triggers: Int)->Unit>()
    private var forwards_End =      emptyArray<T.(triggers: Int)->Unit>()
    private var forwards_Complete = emptyArray<T.(triggers: Int)->Unit>()
    private var reverse_Begin =     emptyArray<T.(triggers: Int)->Unit>()
    private var reverse_Start =     emptyArray<T.(triggers: Int)->Unit>()
    private var reverse_End =       emptyArray<T.(triggers: Int)->Unit>()
    private var reverse_Complete =  emptyArray<T.(triggers: Int)->Unit>()

    /**
     * Reset the tween/timeline to it's initial state. It will be as if the tween/timeline has never run before. If it was already
     * initialized, it will *not* redo the initialization.
     *
     *
     * The paused state is preserved.
     */
    open fun reset() {
        state = START
        direction = FORWARDS
        canTriggerBeginEvent = true // this is so init can happen if necessary
        currentTime = -startDelay
        isInAutoReverse = false
        repeatCount = repeatCountOrig
    }

    // destroys all information about the object
    internal open fun destroy() {
        repeatCountOrig = 0
        repeatCount = 0
        state = INVALID
        currentTime = 0.0f
        repeatDelay = currentTime
        startDelay = repeatDelay
        duration = startDelay
        isInitialized = false
        isInAutoReverse = isInitialized
        isCanceled = isInAutoReverse
        isPaused = isCanceled
        canTriggerBeginEvent = true

        clearCallbacks()
        userData = null
        startEventCallback = emptyAction
        endEventCallback = startEventCallback
        isAutoStartEnabled = true
        isAutoRemoveEnabled = true
    }


    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun callbackHelper(array: Array<T.(triggers: Int)->Unit>, callback: T.(triggers: Int)->Unit): Array<T.(triggers: Int)->Unit> {
        val currentLength = array.size
        val newLength = currentLength + 1

        val copy = arrayOfNulls<T.(triggers: Int)->Unit>(newLength)
        System.arraycopy(array, 0, copy, 0, currentLength.coerceAtMost(newLength))
        copy[currentLength] = callback

        @Suppress("UNCHECKED_CAST")
        return copy as Array<T.(triggers: Int)->Unit>
    }

    /**
     * Adds a callback. By default, it will be fired at the completion of the tween or timeline (event COMPLETE). If you want to change
     * this behavior use the [TweenEvents] constructor.
     *
     * Thread/Concurrent safe
     *
     * @return The current tween/timeline
     *
     * @see TweenEvents
     */
    protected open fun addCallback(triggers: Int = TweenEvents.COMPLETE, callback: T.(triggers: Int)->Unit): BaseTween<T> {
        val isAny = triggers and TweenEvents.ANY == TweenEvents.ANY

        // ALSO have to prevent anyone from updating/changing callbacks while this is occurring.
        // not necessary to call flushRead/Write
        callbackLock.write {
            if (triggers and TweenEvents.BEGIN == TweenEvents.BEGIN || isAny) {
                forwards_Begin = callbackHelper(forwards_Begin, callback)
            }
            if (triggers and TweenEvents.START == TweenEvents.START || isAny) {
                forwards_Start = callbackHelper(forwards_Start, callback)
            }
            if (triggers and TweenEvents.END == TweenEvents.END || isAny) {
                forwards_End = callbackHelper(forwards_End, callback)
            }
            if (triggers and TweenEvents.COMPLETE == TweenEvents.COMPLETE || isAny) {
                forwards_Complete = callbackHelper(forwards_Complete, callback)
            }
            if (triggers and TweenEvents.BACK_BEGIN == TweenEvents.BACK_BEGIN || isAny) {
                reverse_Begin = callbackHelper(reverse_Begin, callback)
            }
            if (triggers and TweenEvents.BACK_START == TweenEvents.BACK_START || isAny) {
                reverse_Start = callbackHelper(reverse_Start, callback)
            }
            if (triggers and TweenEvents.BACK_END == TweenEvents.BACK_END || isAny) {
                reverse_End = callbackHelper(reverse_End, callback)
            }
            if (triggers and TweenEvents.BACK_COMPLETE == TweenEvents.BACK_COMPLETE || isAny) {
                reverse_Complete = callbackHelper(reverse_Complete, callback)
            }
        }
        return this
    }

    /**
     * Clears all the callbacks.
     *
     * Thread/Concurrent safe
     *
     * @return The current tween/timeline
     */
    protected open fun clearCallbacks(): BaseTween<T> {
        // ALSO have to prevent anyone from updating/changing callbacks while this is occurring.
        callbackLock.write {
            forwards_Begin = emptyArray()
            forwards_Start = emptyArray()
            forwards_End = emptyArray()
            forwards_Complete = emptyArray()
            reverse_Begin = emptyArray()
            reverse_Start = emptyArray()
            reverse_End = emptyArray()
            reverse_Complete = emptyArray()
        }
        return this
    }

    /**
     * Adds a start delay to the tween or timeline in seconds.
     *
     * @param delay A duration in seconds for the delay
     *
     * @return The current tween/timeline
     */
    protected open fun delay(delay: Float): BaseTween<T> {
        animator.flushRead()
        delay__(delay)
        animator.flushWrite()
        return this
    }

    /**
     * doesn't sync on anything.
     *
     *
     * Adds a start delay to the tween or timeline in seconds.
     *
     * @param delay A duration in seconds for the delay
     */
    internal fun delay__(delay: Float) {
        startDelay += delay
        currentTime -= delay
    }

    /**
     * Cancels the tween or timeline. If you are starting via [Tween.start], this object will be removed automatically. If
     * starting via [Tween.startUnmanaged] you must manage the lifecycle automatically.
     */
    fun cancel() {
        cancel_()
        animator.flushWrite()
    }

    /**
     * Cancels the tween or timeline. If you are starting via [Tween.start], this object will be removed automatically. If
     * starting via [Tween.startUnmanaged] you must manage the lifecycle automatically.
     */
    internal fun cancel_() {
        isCanceled = true
    }

    /**
     * Stops and resets the tween or timeline, and sends it to its pool, for later reuse.
     *
     *
     * If started normally (instead of un-managed), the [TweenEngine] will automatically call this method once the animation is complete.
     */
    abstract fun free()

    /**
     * Pauses the tween or timeline. Further update calls won't have any effect.
     */
    fun pause() {
        isPaused = true
        animator.flushWrite()
    }

    /**
     * Resumes the tween or timeline to it's previous state. Has no effect is it was not already paused.
     */
    fun resume() {
        isPaused = false
        animator.flushWrite()
    }

    /**
     * Repeats the tween or timeline for a given number of times.
     *
     * @param count The number of repetitions. For infinite repetition, use [Tween.INFINITY] or -1.
     * @param delay A delay between each iteration, in seconds.
     *
     * @return The current tween/timeline
     */
    protected open fun repeat(count: Int, delay: Float): BaseTween<T>? {
        repeat__(count, delay)
        animator.flushWrite()
        return this
    }

    /**
     * doesn't sync on anything.
     *
     *
     * Repeats the tween or timeline for a given number of times.
     *
     * @param count The number of repetitions. For infinite repetition, use [Tween.INFINITY] or -1.
     * @param delay A delay between each iteration, in seconds.
     */
    private fun repeat__(count: Int, delay: Float) {
        if (count < -1) {
            throw RuntimeException(
                "Count " + count + " is an invalid option. It must be -1 (Tween.INFINITY) for infinite or > 0 for " +
                        "finite."
            )
        }
        repeatCountOrig = count
        repeatCount = count
        repeatDelay = delay
        canAutoReverse = false
    }

    /**
     * Repeats the tween or timeline for a given number of times.
     *
     * Once an iteration is complete, it will be played in reverse.
     *
     * @param count The number of repetitions. For infinite repetition, use [Tween.INFINITY] or -1.
     * @param delay A delay before each repetition, in seconds.
     *
     * @return The current tween or timeline
     */
    protected open fun repeatAutoReverse(count: Int, delay: Float): BaseTween<T> {
        repeat__(count, delay)
        canAutoReverse = true
        animator.flushWrite()
        return this
    }

    /**
     * Sets the "start" callback, which is called when the tween/timeline starts running, NULL to remove.
     *
     * @param startCallback this is the object that will be notified when the tween/timeline starts running. NULL to unset.
     *
     * @return The current tween or timeline
     */
    protected open fun setStartCallback(startCallback: ((updatedObject: T)->Unit)?): BaseTween<T> {
        startEventCallback = startCallback ?: emptyAction
        animator.flushWrite()
        return this
    }

    /**
     * Sets the "end" callback, which is called when the tween/timeline finishes running, NULL to remove.
     *
     * @param endCallback this is the object that will be notified when the tween/timeline finishes running. NULL to unset.
     *
     * @return The current tween or timeline
     */
    protected open fun setEndCallback(endCallback: ((updatedObject: T)->Unit)?): BaseTween<T> {
        endEventCallback = endCallback ?: emptyAction
        animator.flushWrite()
        return this
    }

    /**
     * Starts or restarts the object unmanaged. You will need to take care of its life-cycle.
     *
     * @return The current object
     */
    internal open fun startUnmanaged(): BaseTween<T> {
        animator.flushRead()
        startUnmanaged__()
        animator.flushWrite()
        return this
    }

    /**
     * Starts or restarts the object unmanaged. You will need to take care of its life-cycle.
     */
    internal open fun startUnmanaged__() {
        setup__()
    }

    /**
     * Convenience method to add an object to a manager where it's life-cycle will be automatically handled .
     *
     * @return The current object
     */
    open fun start(): BaseTween<T>? {
        animator.flushRead()
        animator.addUnsafe(this)
        animator.flushWrite()
        return this
    }

    /**
     * doesn't sync on anything.
     *
     *
     * Prepares the state of the tween before running (or initializing)
     */
    internal fun setup__() {
        canTriggerBeginEvent = true
        state = START
    }


    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Gets the delay of the Timeline/Tween in seconds. Nothing will happen until this delay is complete.
     */
    fun getStartDelay(): Float {
        animator.flushRead()
        return startDelay
    }

    /**
     * Returns the complete duration, including initial delay and repetitions in seconds
     *
     *
     * The formula is as follows:
     * ```
     * fullDuration = delay + duration + ((repeatDelay + duration) * repeatCount)
     * ```
     */
    fun fullDuration(): Float {
        animator.flushRead()
        return fullDuration__()
    }

    /**
     * doesn't sync on anything.
     *
     * Returns the complete duration, including initial delay and repetitions in seconds
     *
     * The formula is as follows:
     * ```
     * fullDuration = delay + duration + ((repeatDelay + duration) * repeatCount)
     * ```
     */
    internal fun fullDuration__(): Float {
        return if (repeatCountOrig < 0) {
            -1f
        } else {
            startDelay + duration + (repeatDelay + duration) * repeatCountOrig
        }
    }

    /**
     * Gets the number of iterations that will be played.
     */
    fun getRepeatCount(): Int {
        animator.flushRead()
        return repeatCountOrig
    }

    /**
     * Gets the delay occurring between two iterations in seconds
     */
    fun getRepeatDelay(): Float {
        animator.flushRead()
        return repeatDelay
    }

    /**
     * Returns the direction the tween/timeline currently is in.
     *
     *
     * Reverse direction can be impacted by a negative value for [.update] or via a tween reversing direction because of
     * [.repeatAutoReverse]
     *
     * @return true if the current tween stage is in the forwards direction, false if reverse (or Backwards)
     */
    fun getDirection(): Boolean {
        animator.flushRead()
        return direction
    }

    /**
     * @return true if the Timeline/Tween is waiting inside of a delay.
     */
    fun isInDelay(): Boolean {
        animator.flushRead()
        return state == START
    }

    /**
     * Returns true if the Timeline/Tween is finished (i.e. if the tween has reached its end or has been canceled). A tween may be restarted
     * by a timeline when there is a direction change in the timeline.
     *
     * If the Tween/Timeline is un-managed, you should call [BaseTween.free] to reuse the object later.
     */
    fun isFinished(): Boolean {
        animator.flushRead()
        return state == FINISHED || isCanceled
    }

    /**
     * Returns true if the tween automatically reverse when complete.
     */
    fun canAutoReverse(): Boolean {
        animator.flushRead()
        return canAutoReverse
    }

    /**
     * Returns true if the tween or timeline is currently paused.
     */
    fun isPaused(): Boolean {
        animator.flushRead()
        return isPaused
    }


    // -------------------------------------------------------------------------
    // Manager behavior
    // -------------------------------------------------------------------------
    /**
     * Disables the "auto remove" mode of the tween manager for a particular tween or timeline. Tweens/Timelines are auto-removed by
     * default. The interest of deactivating it is to prevent some tweens or timelines from being automatically removed from a manager
     * once they are finished. Therefore, if you update a manager backwards, the tweens or timelines will be played again, even if they
     * were finished.
     */
    fun disableAutoRemove() {
        isAutoRemoveEnabled = false
        animator.flushWrite()
    }

    /**
     * Disables the "auto start" mode of any tween manager for a particular tween or timeline. Tweens/Timelines are auto-started by
     * default. If it is not enabled, add a tween or timeline to any manager won't start it automatically, and you'll need to
     * call .start() manually on your object.
     */
    fun disableAutoStart() {
        isAutoStartEnabled = false
        animator.flushWrite()
    }


    // -------------------------------------------------------------------------
    // User Data
    // -------------------------------------------------------------------------
    /**
     * Attaches an object to this tween or timeline. It can be useful in order
     * to retrieve some data from a TweenEvent Callback.
     *
     * @param data Any kind of object.
     *
     * @return The current tween or timeline
     */
    protected open fun setUserData(data: Any?): BaseTween<T> {
        userData = data
        animator.flushWrite()
        return this
    }

    /**
     * Gets the attached data, or null if none.
     */
    fun getUserData(): Any? {
        animator.flushRead()
        return userData
    }






    // -------------------------------------------------------------------------
    // Abstract API
    // -------------------------------------------------------------------------
    abstract fun containsTarget(target: Any): Boolean
    abstract fun containsTarget(target: Any, tweenType: Int): Boolean

    /**
     * Updates a timeline's children. Only called during State.RUN
     */
    internal abstract fun updateUnsafe(updateDirection: Boolean, delta: Float)

    /**
     * Forces a Timeline/Tween to have it's start/target values
     *
     * @param updateDirection direction in which the force is happening. Affects children iteration order (timelines) and start/target
     * values (tweens)
     * @param updateValue this is the start (true) or end/target (false) to set the tween to.
     */
    abstract fun setValues(updateDirection: Boolean, updateValue: Boolean)

    /**
     * Sets the tween or timeline to a specific point in time based on it's duration + delays. Callbacks are not notified and the change is
     * immediate. The tween/timeline will continue in it's original direction
     * For example:
     *
     *  *  setProgress(0F, true) : set it to the starting position just after the start delay in the forward direction
     *  *  setProgress(.5F, true) : set it to the middle position in the forward direction
     *  *  setProgress(.5F, false) : set it to the middle position in the reverse direction
     *  *  setProgress(1F, false) : set it to the end position in the reverse direction
     *
     *
     *
     * Caveat: If the timeline/tween is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished state
     * (end position). If the timeline/tween is in repeat mode then it will end up in the same position if it was going forwards.
     *
     * @param percentage the percentage (of it's duration) from 0-1, that the tween/timeline be set to
     */
    internal open fun setProgress(percentage: Float): BaseTween<T> {
        animator.flushRead()
        return setProgress(percentage, direction)
    }

    /**
     * Sets the tween or timeline to a specific point in time based on it's duration + delays. Callbacks are not notified and the change is
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
     * Caveat: If the timeline/tween is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished state
     * (end position). If the timeline/tween is in repeat mode then it will end up in the same position if it was going forwards.
     *
     * @param percentage the percentage (of it's duration) from 0-1, that the tween/timeline be set to
     * @param direction sets the direction of the timeline when it updates next: forwards (true) or reverse (false).
     */
    internal open fun setProgress(percentage: Float, direction: Boolean): BaseTween<T> {
        if (percentage < -0.0f || percentage > 1.0f) {
            throw RuntimeException("Cannot set the progress <0 or >1")
        }

        //flushRead();   // synchronize takes care of this

        // have to SAVE all the callbacks (to stop all from executing).
        // ALSO have to prevent anyone from updating/changing callbacks while this is occurring.
        callbackLock.write {
            // always have to reset, because of issues with delays and repetitions. (also sets the direction to "forwards")
            reset()

            // how much time is represented by the delta in percentage of time?
            val duration = duration
            val percentageValue = duration * percentage
            val adjustmentTime: Float

            // Caveat: If the timeline/tween is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished/end position
            // if we specify to "go in reverse" and we are in a "repeat" mode (instead of a "flip-to-reverse" mode), then just pretend we
            // specified to "go forwards".
            val goesReverse = !direction && canAutoReverse
            adjustmentTime = if (goesReverse) {
                // we want the tween/timeline in the REVERSE state when finished, so the next delta update will move it in that direction
                // to do this, we "wrap around" the timeline/tween times to the correct time, in a single update.
                val timeSpentToGetToEnd = duration + startDelay
                val timeSpentInReverseFromEnd = duration - percentageValue
                timeSpentToGetToEnd + timeSpentInReverseFromEnd
            } else {
                // we just go from the absolute start (including the delay) to where we should end up
                percentageValue + startDelay
            }

            val forwards_Begin_saved = forwards_Begin
            val forwards_Start_saved = forwards_Start
            val forwards_End_saved = forwards_End
            val forwards_Complete_saved = forwards_Complete
            val reverse_Begin_saved = reverse_Begin
            val reverse_Start_saved = reverse_Start
            val reverse_End_saved = reverse_End
            val reverse_Complete_saved = reverse_Complete

            forwards_Begin = emptyCallback
            forwards_Start = emptyCallback
            forwards_End = emptyCallback
            forwards_Complete = emptyCallback
            reverse_Begin = emptyCallback
            reverse_Start = emptyCallback
            reverse_End = emptyCallback
            reverse_Complete = emptyCallback

            // update by the timeline/tween this amount (always starting from "scratch"). It will automatically end up in the correct direction.
            updateUnsafe(adjustmentTime)

            // have to RESTORE all of the callbacks
            forwards_Begin = forwards_Begin_saved
            forwards_Start = forwards_Start_saved
            forwards_End = forwards_End_saved
            forwards_Complete = forwards_Complete_saved
            reverse_Begin = reverse_Begin_saved
            reverse_Start = reverse_Start_saved
            reverse_End = reverse_End_saved
            reverse_Complete = reverse_Complete_saved
        }

        // flushWrite();   // synchronize takes care of this
        return this
    }

    internal open fun initializeValues() {}

    /**
     * Kills every tweens associated to the given target. Will also kill every timeline containing a tween associated to the given target.
     *
     * @return true if the target was killed, false if we do not contain the target, and it was not killed
     */
    internal fun cancelTarget(target: Any): Boolean {
        if (containsTarget(target)) {
            cancel()
            return true
        }
        return false
    }

    /**
     * Kills every tweens associated to the given target and tween type. Will also kill every timelines containing a tween associated to
     * the given target and tween type.
     *
     * @return true if the target was killed, false if we do not contain the target, and it was not killed
     */
    internal fun cancelTarget(target: Any, tweenType: Int): Boolean {
        if (containsTarget(target, tweenType)) {
            cancel()
            return true
        }
        return false
    }

    /**
     * Adjust the tween for when repeat + auto-reverse is used
     *
     * @param newDirection the new direction for all children
     */
    internal open fun adjustForRepeat_AutoReverse(newDirection: Boolean) {
        state = START
        currentTime = if (newDirection) {
            0f
        } else {
            duration
        }
    }

    /**
     * Adjust the current time (set to the start value for the tween) and change state to DELAY.
     *
     * For timelines, this also changes what the current tween is (for when iterating over tweens)
     *
     * @param newDirection the new direction for all children
     */
    internal open fun adjustForRepeat_Linear(newDirection: Boolean) {
        state = START
        currentTime = if (newDirection) {
            0f
        } else {
            duration
        }
    }

    /**
     * Updates the tween or timeline state and values.
     *
     * **The preferred way to update a tween is via [TweenEngine.update].**
     *
     * Slow motion, fast motion and backward play can be easily achieved by tweaking this delta time.
     *
     * Multiply it by -1 to play the animation backward, or by 0.5 to play it twice-as-slow than its normal speed.
     *
     *
     * The tween manager doesn't call this method, it correctly calls updateState + updateValues on timeline/tweens
     *
     * @param delta the time in SECONDS that has elapsed since the last update
     *
     * @return true if this tween/timeline is finished (STATE = FINISHED)
     */
    fun update(delta: Float): Float {
        animator.flushRead()
        val v = updateUnsafe(delta)
        animator.flushWrite()
        return v
    }

    /**
     * doesn't sync on anything.
     *
     *
     * Updates the tween or timeline state and values.
     *
     *
     * **The preferred way to update a tween is via [TweenEngine.update].**
     *
     *
     * Slow motion, fast motion and backward play can be easily achieved by tweaking this delta time.
     *
     *
     * Multiply it by -1 to play the animation backward, or by 0.5 to play it twice-as-slow than its normal speed.
     *
     *
     * The tween manager doesn't call this method, it correctly calls updateState + updateValues on timeline/tweens
     *
     * @param delta the time in SECONDS that has elapsed since the last update
     *
     * @return the amount of time remaining (this is the amount of delta that wasn't processed)
     */
    internal fun updateUnsafe(delta: Float): Float {
        if (isPaused || isCanceled) {
            return delta
        }

        @Suppress("NAME_SHADOWING")
        var delta = delta
        if (isInAutoReverse) {
            delta = -delta
        }

        // the INITIAL, incoming delta from the app, will be positive or negative.
        // Specifically check for +0.0F so that -0.0F will let us go in reverse
        var direction = delta >= +0.0f
        this.direction = direction
        val duration = duration

        // tween/timeline classes are a subclass of us, so this is safe
        @Suppress("UNCHECKED_CAST")
        this as T

        /*
         * DELAY - (delay) initial start delay, only happens once, during init
         * R.DELAY - (repeatDelay) delay between repeat iterations, if there are more than one.
         *
         * there are two modes for repeat. LINEAR and AUTO_REVERSE
         *
         * LINEAR:
         *                BEGIN                                     COMPLETE
         *                START      END                 START      END
         *                v          v                   v          v
         * |---[DELAY]----[XXXXXXXXXX]->>-[R.DELAY]-->>--[XXXXXXXXXX]
         *
         *
         * AUTO_REVERSE
         *                BEGIN      COMPLETE
         *                START      END
         *                v          v
         * |---[DELAY]----[XXXXXXXXXX]──────────-─────╮
         *            ╭╴  [XXXXXXXXXX]-<<-[R.DELAY] <─╯
         *            │   ^          ^
         *            │   bEND       bSTART
         *            │   bCOMPLETE  bBEGIN
         *            │
         *            ╰╴> [R.DELAY]->>-[XXXXXXXXXX]  ╶╮
         *            ╭╴  [XXXXXXXXXX]-<<-[R.DELAY] <─╯
         *            ╰╴> [R.DELAY]->>-[XXXXXXXXXX]  ...
         *
         * Time must "cross the finish line" in order for the tween to be considered finished.
         */

        // The LAST tween (in a timeline) that was modified is what keeps track of "overflow" of time, which is when an animation runs
        // longer that the tween duration. This is necessary in order to accurately reverse the animation and have the correct delays

        // FORWARDS: 0 > time <= duration
        // REVERSE:  0 >= time < duration   (reverse always goes from duration -> 0)
        startEventCallback.invoke(this)

        var callbacks: Array<T.(triggers: Int)->Unit>
        var i: Int
        var n: Int

        do {
            val newTime = currentTime + delta

            return if (direction) {
                // {FORWARDS}
                // <editor-fold>

                // FORWARDS: 0 > time <= duration
                when (state) {
                    START -> {
                        if (newTime <= 0.0f) {
                            // still in start delay
                            currentTime = newTime
                            endEventCallback.invoke(this)
                            return 0.0f
                        }

                        currentTime = 0.0f
                        if (canTriggerBeginEvent) {
                            canTriggerBeginEvent = false

                            // initialize during start (but after delay), so that it's at the same point in either direction
                            if (!isInitialized) {
                                isInitialized = true
                                initializeValues()
                            }

                            callbacks = this.forwards_Begin
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.BEGIN)
                                i++
                            }
                        }

                        callbacks = this.forwards_Start
                        i = 0
                        n = callbacks.size
                        while (i < n) {
                            callbacks[i].invoke(this, TweenEvents.START)
                            i++
                        }

                        // goto next state
                        state = RUN

                        // -- update is REVERSE so that the FIRST tween data takes priority, if there are
                        //    multiple tweens that have the same target
                        setValues(REVERSE, START_VALUES)

                        // adjust the delta so that it is shifted based on the length of (previous) iteration
                        delta = newTime


                        if (newTime <= duration) {
                            // still in running forwards
                            currentTime = newTime
                            updateUnsafe(FORWARDS, delta)
                            endEventCallback.invoke(this)
                            return 0.0f
                        }

                        state = FINISHED
                        currentTime = duration
                        val repeatCountStack = repeatCount

                        ////////////////////////////////////////////
                        ////////////////////////////////////////////
                        // 1) we are done running completely
                        // 2) we flip to auto-reverse repeat mode
                        // 3) we are in linear repeat mode
                        return if (repeatCountStack == 0) {
                            // {FORWARDS}{FINISHED}

                            // -- update is REVERSE so that the FIRST tween data takes priority, if there are
                            //    multiple tweens that have the same target

                            // "instant" tweens (duration 0) cannot trigger a set-to-startpoint (since they are always [enabled] while
                            // running). They are [disabled] by their parent timeline when the parent reaches the end of it's duration
                            // in the FORWARDS direction, this doesn't matter, but in REVERSE, it does.
                            setValues(REVERSE, TARGET_VALUES)

                            callbacks = this.forwards_End
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.END)
                                i++
                            }

                            callbacks = this.forwards_Complete
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.COMPLETE)
                                i++
                            }

                            // don't do this, because it will xfer to the next tween (if a timeline), or will get added in the FINISHED
                            // case (if not a timeline, to record "overflow" of time)
                            //    currentTime = newTime;

                            // we're done going forwards
                            canTriggerBeginEvent = true
                            isInAutoReverse = false

                            // have to reset our repeat count, so outside repeats will start us in the correct state
                            repeatCount = repeatCountOrig
                            endEventCallback.invoke(this)

                            // return the time that is remaining (the remaining amount of delta that wasn't processed)
                            newTime - duration
                        } else {
                            // must always update all the children
                            updateUnsafe(FORWARDS, delta)
                            if (repeatCountStack > 0) {
                                // -1 means repeat forever
                                repeatCount--
                            }

                            callbacks = this.forwards_End
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.END)
                                i++
                            }

                            if (canAutoReverse) {
                                // {FORWARDS}{AUTO_REVERSE}
                                callbacks = this.forwards_Complete
                                i = 0
                                n = callbacks.size
                                while (i < n) {
                                    callbacks[i].invoke(this, TweenEvents.COMPLETE)
                                    i++
                                }

                                // we're done going forwards
                                canTriggerBeginEvent = true
                                isInAutoReverse = !isInAutoReverse // if we are NOT in autoReverse, then "isInAutoReverse" is true if we reverse

                                // make sure any checks after this returns accurately reflect the correct REVERSE direction
                                direction = REVERSE

                                // any extra time (what's left in delta) will be applied/calculated on the next loop around
                                adjustForRepeat_AutoReverse(REVERSE)
                                currentTime += repeatDelay

                                // because we always continue the loop, we must adjust the delta so that it is shifted (in REVERSE)
                                // delta = newTime - duration;
                                // delta = -delta
                                delta = -newTime + duration

                                // loop to new state
                                continue
                            } else {
                                // {FORWARDS}{LINEAR}
                                isInAutoReverse = false

                                // any extra time (what's left in delta) will be applied/calculated on the next loop around
                                adjustForRepeat_Linear(FORWARDS)

                                // because we always continue the loop, we must adjust the delta so that it is shifted
                                delta = newTime - duration
                                currentTime = -repeatDelay + delta

                                // loop to new state
                                continue
                            }
                        }
                    }
                    RUN -> {
                        if (newTime <= duration) {
                            currentTime = newTime
                            updateUnsafe(FORWARDS, delta)
                            endEventCallback.invoke(this)
                            return 0.0f
                        }
                        state = FINISHED
                        currentTime = duration

                        val repeatCountStack = repeatCount
                        if (repeatCountStack == 0) {
                            setValues(REVERSE, TARGET_VALUES)

                            callbacks = forwards_End
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.END)
                                i++
                            }

                            callbacks = forwards_Complete
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.COMPLETE)
                                i++
                            }
                            canTriggerBeginEvent = true
                            isInAutoReverse = false
                            repeatCount = repeatCountOrig
                            endEventCallback.invoke(this)
                            newTime - duration
                        } else {
                            updateUnsafe(FORWARDS, delta)
                            if (repeatCountStack > 0) {
                                repeatCount--
                            }

                            callbacks = forwards_End
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.END)
                                i++
                            }

                            if (canAutoReverse) {
                                callbacks = forwards_Complete
                                i = 0
                                n = callbacks.size
                                while (i < n) {
                                    callbacks[i].invoke(this, TweenEvents.COMPLETE)
                                    i++
                                }

                                canTriggerBeginEvent = true
                                isInAutoReverse = !isInAutoReverse
                                direction = REVERSE
                                adjustForRepeat_AutoReverse(REVERSE)
                                currentTime += repeatDelay
                                delta = -newTime + duration
                                continue
                            } else {
                                isInAutoReverse = false
                                adjustForRepeat_Linear(FORWARDS)
                                delta = newTime - duration
                                currentTime = -repeatDelay + delta
                                continue
                            }
                        }
                    }
                    FINISHED -> {
                        if (newTime <= 0.0f || newTime > duration) {
                            // still in the "finished" state, and haven't been reversed somewhere
                            currentTime = newTime
                            endEventCallback.invoke(this)
                            return 0.0f
                        }

                        // restart the timeline, since we've had our time adjusted to a point where we are running again.
                        state = START
                        updateUnsafe(FORWARDS, delta)

                        // loop to new state
                        continue
                    }
                    else -> {
                        throw RuntimeException("Unexpected state!! '$state'")
                    }
                }

                // </editor-fold>
            } else {
                // {REVERSE}
                // <editor-fold>

                // REVERSE:  0 >= time < duration   (reverse always goes from duration -> 0)
                when (state) {
                    START -> {
                        run {
                            if (newTime >= duration) {
                                // still in delay
                                currentTime = newTime
                                endEventCallback.invoke(this)
                                return 0.0f
                            }
                            currentTime = duration
                            if (canTriggerBeginEvent) {
                                canTriggerBeginEvent = false

                                // initialize during start (but after delay), so that it's at the same point in either direction
                                if (!isInitialized) {
                                    isInitialized = true
                                    initializeValues()
                                }

                                callbacks = this.reverse_Begin
                                i = 0
                                n = callbacks.size
                                while (i < n) {
                                    callbacks[i].invoke(this, TweenEvents.BACK_BEGIN)
                                    i++
                                }
                            }

                            callbacks = this.reverse_Start
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.BACK_START)
                                i++
                            }

                            // goto next state
                            state = RUN

                            // -- update is FORWARDS so that the LAST tween data takes priority, if there are
                            //    multiple tweens that have the same target
                            // this is opposite of the logic in FORWARDS.START
                            setValues(FORWARDS, TARGET_VALUES)

                            // adjust the delta so that it is shifted based on the length of (previous) iteration
                            delta = -(duration - newTime)
                        }

                        if (newTime >= 0.0f) {
                            // still in running reverse
                            currentTime = newTime
                            updateUnsafe(REVERSE, delta)
                            endEventCallback.invoke(this)
                            return 0.0f
                        }

                        state = FINISHED
                        currentTime = 0.0f
                        val repeatCountStack = repeatCount

                        ////////////////////////////////////////////
                        ////////////////////////////////////////////
                        // 1) we are done running completely
                        // 2) we flip to auto-reverse
                        // 3) we are in linear repeat mode
                        return if (repeatCountStack == 0) {
                            // {REVERSE}{FINISHED}

                            // set the "start" values, backwards because values are relative to forwards

                            // -- update is FORWARDS so that the LAST tween data takes priority, if there are
                            //    multiple tweens that have the same target
                            if (duration <= 0.000001f) {
                                // "instant" tweens (duration 0) cannot trigger a set-to-startpoint (since they are always [enabled] while
                                // running). They are [disabled] by their parent timeline when the parent reaches the end of it's duration
                                // This is why it's always set to target value (even though it's reverse)
                                setValues(FORWARDS, TARGET_VALUES)
                            } else {
                                // set the "start" values, flipped because we are in reverse
                                setValues(FORWARDS, START_VALUES)
                            }

                            callbacks = this.reverse_End
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.BACK_END)
                                i++
                            }

                            callbacks = this.reverse_Complete
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.BACK_COMPLETE)
                                i++
                            }

                            // don't do this, because it will xfer to the next tween (if a timeline), or will get added in the FINISHED
                            // case (if not a timeline, to record "overflow" of time)
                            //    currentTime = newTime;

                            // we're done going reverse
                            canTriggerBeginEvent = true
                            isInAutoReverse = false

                            // have to reset our repeat count, so outside repeats will start us in the correct state
                            repeatCount = repeatCountOrig
                            endEventCallback.invoke(this)

                            // return the time that is remaining (the remaining amount of delta that wasn't processed)
                            newTime
                        } else {
                            // must always update all the children
                            updateUnsafe(REVERSE, delta)
                            if (repeatCountStack > 0) {
                                // -1 means repeat forever
                                repeatCount--
                            }

                            callbacks = this.reverse_End
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.BACK_END)
                                i++
                            }

                            if (canAutoReverse) {
                                // {REVERSE}{AUTO_REVERSE}
                                callbacks = this.reverse_Complete
                                i = 0
                                n = callbacks.size
                                while (i < n) {
                                    callbacks[i].invoke(this, TweenEvents.BACK_COMPLETE)
                                    i++
                                }

                                // we're done going forwards
                                canTriggerBeginEvent = true
                                isInAutoReverse = !isInAutoReverse // if we are NOT in autoReverse, then "isInAutoReverse" is true if we reverse

                                // make sure any checks after this returns accurately reflect the correct FORWARDS direction
                                direction = FORWARDS

                                // any extra time (what's left in delta) will be applied/calculated on the next loop around
                                adjustForRepeat_AutoReverse(FORWARDS)
                                currentTime -= repeatDelay

                                // because we always continue the loop, we must adjust the delta so that it is shifted (in FORWARDS)
                                // delta = newTime;
                                // delta = -delta
                                delta = -newTime

                                // loop to new state
                                continue
                            } else {
                                // {REVERSE}{LINEAR}
                                isInAutoReverse = false

                                // any extra time (what's left in delta) will be applied/calculated on the next loop around
                                adjustForRepeat_Linear(REVERSE)

                                // because we always continue the loop, we must adjust the delta so that it is shifted
                                // delta = newTime;
                                currentTime = repeatDelay + newTime

                                // loop to new state
                                continue
                            }
                        }
                    }
                    RUN -> {
                        if (newTime >= 0.0f) {
                            currentTime = newTime
                            updateUnsafe(REVERSE, delta)
                            endEventCallback.invoke(this)
                            return 0.0f
                        }

                        state = FINISHED
                        currentTime = 0.0f

                        val repeatCountStack = repeatCount
                        if (repeatCountStack == 0) {
                            if (duration <= 0.000001f) {
                                setValues(FORWARDS, TARGET_VALUES)
                            } else {
                                setValues(FORWARDS, START_VALUES)
                            }

                            callbacks = reverse_End
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.BACK_END)
                                i++
                            }

                            callbacks = reverse_Complete
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.BACK_COMPLETE)
                                i++
                            }

                            canTriggerBeginEvent = true
                            isInAutoReverse = false
                            repeatCount = repeatCountOrig
                            endEventCallback.invoke(this)
                            newTime
                        } else {
                            updateUnsafe(REVERSE, delta)
                            if (repeatCountStack > 0) {
                                repeatCount--
                            }

                            callbacks = reverse_End
                            i = 0
                            n = callbacks.size
                            while (i < n) {
                                callbacks[i].invoke(this, TweenEvents.BACK_END)
                                i++
                            }

                            if (canAutoReverse) {
                                callbacks = reverse_Complete
                                i = 0
                                n = callbacks.size
                                while (i < n) {
                                    callbacks[i].invoke(this, TweenEvents.BACK_COMPLETE)
                                    i++
                                }

                                canTriggerBeginEvent = true
                                isInAutoReverse = !isInAutoReverse
                                direction = FORWARDS
                                adjustForRepeat_AutoReverse(FORWARDS)
                                currentTime -= repeatDelay
                                delta = -newTime
                                continue
                            } else {
                                isInAutoReverse = false
                                adjustForRepeat_Linear(REVERSE)
                                currentTime = repeatDelay + newTime
                                continue
                            }
                        }
                    }

                    FINISHED -> {
                        if (newTime < 0.0f || newTime >= duration) {
                            // still in the "finished" state, and haven't been reversed somewhere
                            currentTime = newTime
                            endEventCallback.invoke(this)
                            return 0.0f
                        }

                        // restart the timeline, since we've had our time adjusted to a point where we are running again.
                        state = START
                        updateUnsafe(REVERSE, delta)

                        // loop to new state
                        continue
                    }
                    else -> {
                        throw RuntimeException("Unexpected state!! '$state'")
                    }
                }

                // </editor-fold>
            }
        } while (true)
    }
}
