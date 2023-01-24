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
 * A Timeline can be used to create complex animations made of sequences and parallel sets of Tweens.
 *
 *
 *
 * The following example will create an animation sequence composed of 5 parts:
 *
 *
 *
 * 1. First, opacity and scale are set to 0 (with Tween.set() calls).<br></br>
 * 2. Then, opacity and scale are animated in parallel.<br></br>
 * 3. Then, the animation is paused for 1s.<br></br>
 * 4. Then, position is animated to x=100.<br></br>
 * 5. Then, rotation is animated to 360°.
 *
 *
 *
 * This animation will be repeated 5 times, with a 500ms delay between each
 * iteration:
 * <br></br><br></br>
 *
 * <pre> `Timeline.createSequential()
 * .push(Tween.set(myObject, OPACITY).target(0))
 * .push(Tween.set(myObject, SCALE).target(0, 0))
 * .beginParallel()
 * .push(Tween.to(myObject, OPACITY, 0.5F).target(1).ease(Quad_InOut))
 * .push(Tween.to(myObject, SCALE, 0.5F).target(1, 1).ease(Quad_InOut))
 * .end()
 * .pushPause(1.0F)
 * .push(Tween.to(myObject, POSITION_X, 0.5F).target(100).ease(Quad_InOut))
 * .push(Tween.to(myObject, ROTATION, 0.5F).target(360).ease(Quad_InOut))
 * .repeat(5, 0.5F)
 * .start(myManager);
`</pre> *
 *
 * @see Tween
 *
 * @see TweenCallback
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
class Timeline internal constructor(animator: TweenEngine) : BaseTween<Timeline>(animator) {
    enum class Mode {
        SEQUENTIAL, PARALLEL
    }

    companion object {
        /**
         * Gets the version number.
         */
        const val version = "8.3"

        val INVALID_TIMELINE = Timeline(TweenEngine(false))
        val INVALID_CHILDREN = emptyArray<BaseTween<*>>()
    }

    /** The backing list of the timeline children. */
    val children = mutableListOf<BaseTween<*>>()


    // children optimization values
    private var childrenArray: Array<BaseTween<*>> = INVALID_CHILDREN
    private var childrenSize = 0
    private var childrenSizeMinusOne = 0

    private var mode: Mode = Mode.SEQUENTIAL
    private var parent: Timeline = INVALID_TIMELINE

    // current is used for TWO things.
    //  - Tracking what to start/end during construction
    //  - Tracking WHICH tween/timeline (of the children) is currently being run.
    private var current: BaseTween<*> = INVALID_TIMELINE
    private var currentIndex = 0

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------
    init {
        destroy()
    }

    /**
     * Reset the tween/timeline to it's initial state. It will be as if the tween/timeline has never run before. If it was already
     * initialized, it will *not* redo the initialization.
     *
     *
     * The paused state is preserved.
     */
    override fun reset() {
        super.reset()
        currentIndex = 0
        current = childrenArray[0]

        var i = 0
        val n = childrenSize
        while (i < n) {
            val tween = childrenArray[i]
            // this can be a tween or a timeline.
            tween.reset()
            i++
        }
    }

    override fun destroy() {
        super.destroy()

        children.clear()
        childrenArray = INVALID_CHILDREN
        parent = INVALID_TIMELINE
        current = INVALID_TIMELINE
        currentIndex = 0
    }

    /**
     * doesn't sync on anything.
     */
    internal fun setupUnsafe(mode: Mode) {
        this.mode = mode
        current = this
    }


    /**
     * Adds a callback. By default, it will be fired at the completion of the timeline (event COMPLETE). If you want to change
     * this behavior use the [TweenCallback] constructor.
     *
     * Thread/Concurrent safe
     *
     * @see TweenCallback
     */
    public override fun addCallback(callback: TweenCallback<Timeline>): Timeline {
        super.addCallback(callback)
        return this
    }

    /**
     * Clears all of the callback.
     *
     * Thread/Concurrent safe
     */
    public override fun clearCallbacks(): Timeline {
        super.clearCallbacks()
        return this
    }

    /**
     * Stops and resets the timeline, and sends it to its pool, for later reuse.
     *
     *
     * If started normally (instead of un-managed), the [TweenEngine] will automatically call this method once the animation is complete.
     */
    override fun free() {
        // free all children tweens as well.
        var tween: BaseTween<*>
        for (i in children.indices.reversed()) {
            tween = children.removeAt(i)
            if (tween.isAutoRemoveEnabled) {
                // only release to the pool if auto-remove is enabled (since that is the contract with tweens)
                tween.free()
            }
        }
        animator.free(this)
    }

    /**
     * Adds a start delay to the timeline in seconds.
     *
     * @param delay A duration in seconds for the delay
     *
     * @return The current timeline
     */
    public override fun delay(delay: Float): Timeline {
        super.delay(delay)
        return this
    }

    /**
     * Repeats the timeline for a given number of times.
     *
     * @param count The number of repetitions. For infinite repetition, use [Tween.INFINITY] or -1.
     * @param delay A delay between each iteration, in seconds.
     *
     * @return The current timeline
     */
    public override fun repeat(count: Int, delay: Float): Timeline {
        super.repeat(count, delay)
        return this
    }

    /**
     * Repeats the timeline for a given number of times.
     *
     * Once an iteration is complete, it will be played in reverse.
     *
     * @param count The number of repetitions. For infinite repetition, use [Tween.INFINITY] or -1.
     * @param delay A delay before each repetition, in seconds.
     *
     * @return The current timeline
     */
    public override fun repeatAutoReverse(count: Int, delay: Float): Timeline {
        super.repeatAutoReverse(count, delay)
        return this
    }

    /**
     * Sets the "start" callback, which is called when the timeline starts running, NULL to remove.
     *
     * @param startCallback this is the object that will be notified when the timeline starts running. NULL to unset.
     *
     * @return The current timeline
     */
    override fun setStartCallback(startCallback: UpdateAction<Timeline>?): Timeline {
        super.setStartCallback(startCallback)
        return this
    }

    /**
     * Sets the "end" callback, which is called when the timeline finishes running, NULL to remove.
     *
     * @param endCallback this is the object that will be notified when the timeline finishes running. NULL to unset.
     *
     * @return The current timeline
     */
    override fun setEndCallback(endCallback: UpdateAction<Timeline>?): Timeline {
        super.setEndCallback(endCallback)
        return this
    }

    /**
     * Sets the timeline to a specific point in time based on it's duration + delays. Callbacks are not notified and the change is
     * immediate. The timeline will continue in it's original direction
     * For example:
     *
     *  *  setProgress(0F, true) : set it to the starting position just after the start delay in the forward direction
     *  *  setProgress(.5F, true) : set it to the middle position in the forward direction
     *  *  setProgress(.5F, false) : set it to the middle position in the reverse direction
     *  *  setProgress(1F, false) : set it to the end position in the reverse direction
     *
     *
     *
     * Caveat: If the timeline is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished state
     * (end position). If the timeline is in repeat mode then it will end up in the same position if it was going forwards.
     *
     * @param percentage the percentage (of it's duration) from 0-1, that the timeline be set to
     */
    public override fun setProgress(percentage: Float): Timeline {
        super.setProgress(percentage)
        return this
    }

    /**
     * Sets the timeline to a specific point in time based on it's duration + delays. Callbacks are not notified and the change is
     * immediate.
     * For example:
     *
     *  *  setProgress(0F, true) : set it to the starting position just after the start delay in the forward direction
     *  *  setProgress(.5F, true) : set it to the middle position in the forward direction
     *  *  setProgress(.5F, false) : set it to the middle position in the reverse direction
     *  *  setProgress(1F, false) : set it to the end position in the reverse direction
     *
     * Caveat: If the timeline is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished state
     * (end position). If the timeline/tween is in repeat mode then it will end up in the same position if it was going forwards.
     *
     * @param percentage the percentage (of it's duration) from 0-1, that the timeline be set to
     * @param direction sets the direction of the timeline when it updates next: forwards (true) or reverse (false).
     */
    public override fun setProgress(percentage: Float, direction: Boolean): Timeline {
        animator.flushRead()

        super.setProgress(percentage, direction)
        return this
    }

    /**
     * Starts or restarts the timeline unmanaged. You will need to take care of its life-cycle.
     *
     * @return The current timeline
     */
    public override fun startUnmanaged(): Timeline {
        animator.flushRead()
        startUnmanaged__()
        animator.flushWrite()
        return this
    }

    override fun startUnmanaged__() {
        super.startUnmanaged__()
        for (i in 0 until childrenSize) {
            val obj = childrenArray[i]
            if (obj.repeatCountOrig < 0) {
                throw RuntimeException("You can't push an object with infinite repetitions in a timeline")
            }
            obj.startUnmanaged__()
        }
    }

    /**
     * Convenience method to add an object to a timeline where it's life-cycle will be automatically handled .
     *
     * @return The current timeline
     */
    override fun start(): Timeline {
        super.start()
        return this
    }
    // -------------------------------------------------------------------------
    // User Data
    // -------------------------------------------------------------------------
    /**
     * Attaches an object to this timeline. It can be useful in order
     * to retrieve some data from a TweenCallback.
     *
     * @param data Any kind of object.
     *
     * @return The current timeline
     */
    public override fun setUserData(data: Any?): Timeline {
        super.setUserData(data)
        return this
    }

    /**
     * Adds a Tween to the current timeline.
     *
     * @return The current timeline
     */
    fun push(tween: Tween<*>): Timeline {
        tween.startUnmanaged()
        animator.flushRead()

        children.add(tween)
        setupTimeline__(tween)

        animator.flushWrite()
        return this
    }

    /**
     * Nests a Timeline in the current one.
     *
     * @return The current timeline
     */
    fun push(timeline: Timeline): Timeline {
        animator.flushRead()

        timeline.parent = this
        children.add(timeline)
        setupTimeline__(timeline)

        animator.flushWrite()
        return this
    }

    /**
     * Adds a pause to the timeline. The pause may be negative if you want to overlap the preceding and following children.
     *
     * @param time A positive or negative duration in seconds
     *
     * @return The current timeline
     */
    fun pushPause(time: Float): Timeline {
        if (time < 0.0f) {
            throw RuntimeException(
                "You can't push a negative pause to a timeline. Just make the last entry's duration shorter or use" +
                        " with a parallel timeline and appropriate delays in place."
            )
        }

        val tween: Tween<Int> = animator.mark__()
        animator.flushRead()

        tween.delay__(time)
        tween.startUnmanaged__()
        children.add(tween)
        setupTimeline__(tween)

        animator.flushWrite()
        return this
    }

    /**
     * Starts a nested timeline with a 'sequential' behavior. Don't forget to call [Timeline.end] to close this nested timeline.
     *
     * @return The new sequential timeline
     */
    fun beginSequential(): Timeline {
        val timeline = animator.takeTimeline()

        animator.flushRead()

        children.add(timeline)
        timeline.parent = this
        timeline.mode = Mode.SEQUENTIAL

        // keep track of which timeline we are on
        current = timeline

        // animator.flushWrite() // called on end

        // our timeline info is setup when the sequenced timeline is "ended", so we can retrieve it's children
        return timeline
    }

    /**
     * Starts a nested timeline with a 'parallel' behavior. Don't forget to call [Timeline.end] to close this nested timeline.
     *
     * @return The new parallel timeline
     */
    fun beginParallel(): Timeline {
        val timeline = animator.takeTimeline()
        animator.flushRead()

        children.add(timeline)
        timeline.parent = this
        timeline.mode = Mode.PARALLEL

        // keep track of which timeline we are on
        current = timeline

        // flushWrite(); called on end()

        // our timeline info is setup when the sequenced timeline is "ended", so we can retrieve it's children
        return timeline
    }

    /**
     * Closes the last nested timeline.
     *
     * @return The original (parent) timeline
     */
    fun end(): Timeline {
        if (current === this) {
            throw RuntimeException("Nothing to end, calling end before begin!")
        }

        // flushRead();  called on begin...()

        // now prep everything (from the parent perspective), since we are now considered "done"
        parent.setupTimeline__(this)
        current = parent

        if (current === INVALID_TIMELINE) {
            throw RuntimeException("Whoops! Shouldn't be invalid!")
        }

        animator.flushWrite()
        return current as Timeline
    }

    /**
     * doesn't sync on anything.
     *
     *
     * Creates/prepares array for children. This array is used for iteration during update
     */
    private fun setupTimeline__(tweenOrTimeline: BaseTween<*>) {
        when (mode) {
            Mode.SEQUENTIAL -> duration += tweenOrTimeline.fullDuration__
            Mode.PARALLEL -> duration = duration.coerceAtLeast(tweenOrTimeline.fullDuration__)
        }
        childrenSize = children.size
        if (childrenSize == 0) {
            throw RuntimeException("Creating a timeline with zero children. This is likely unintended, and is not permitted.")
        }

        childrenSizeMinusOne = childrenSize - 1

        // setup our children array, so update iterations are faster
        val toTypedArray = children.toTypedArray()
        childrenArray = toTypedArray
        current = toTypedArray[0]
    }

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------
    /**
     * Recursively adjust the tweens for when repeat + auto-reverse is used
     *
     * @param newDirection the new direction for all children
     */
    override fun adjustForRepeat_AutoReverse(newDirection: Boolean) {
        super.adjustForRepeat_AutoReverse(newDirection)
        var i = 0
        val n = childrenArray.size
        while (i < n) {
            val tween = childrenArray[i]
            tween.adjustForRepeat_AutoReverse(newDirection)
            i++
        }
    }

    /**
     * Adjust the current time (set to the start value for the tween) and change state to DELAY.
     *
     * For timelines, this also changes what the current tween is (for when iterating over tweens)
     *
     * @param newDirection the new direction for all children
     */
    override fun adjustForRepeat_Linear(newDirection: Boolean) {
        super.adjustForRepeat_Linear(newDirection)
        var i = 0
        val n = childrenArray.size
        while (i < n) {
            val tween = childrenArray[i]
            tween.adjustForRepeat_Linear(newDirection)
            i++
        }

        // this only matters if we are a sequence, because PARALLEL operates on all of them at the same time
        if (mode == Mode.SEQUENTIAL) {
            currentIndex = if (newDirection) {
                0
            } else {
                childrenSize - 1
            }
            current = childrenArray[currentIndex]
        }
    }

    /**
     * Updates a timeline's children, in different orders.
     *
     * @param updateDirection what is the current direction of the update. This is used to determine what order to update the
     * timeline children (tweens)
     * @param delta the time in SECONDS that has elapsed since the last update
     */
    override fun updateUnsafe(updateDirection: Boolean, delta: Float) {
        @Suppress("NAME_SHADOWING")
        var delta = delta

        if (mode == Mode.SEQUENTIAL) {
            // update children one at a time.
            if (updateDirection) {
                while (delta != 0.0f) {
                    delta = current.updateUnsafe(delta)

                    if (current.state == FINISHED) {
                        // iterate to the next one when it's finished, but don't go beyond the last child
                        if (currentIndex < childrenSizeMinusOne) {
                            currentIndex++
                            current = childrenArray[currentIndex]
                        } else if (parent !== INVALID_TIMELINE) {
                            // keep track of implicit time "overflow", where currentTime + delta > duration.
                            // This logic is so that this is recorded only on the outermost timeline for when timelines reverse direction
                            return
                        }
                    }
                }
            } else {
                while (delta != 0.0f) {
                    delta = current.updateUnsafe(delta)

                    if (current.state == FINISHED) {
                        // iterate to the previous one (because we are in reverse) when it's finished, but don't go beyond the first child
                        if (currentIndex > 0) {
                            currentIndex--
                            current = childrenArray[currentIndex]
                        } else if (parent !== INVALID_TIMELINE) {
                            // keep track of implicit time "overflow", where currentTime + delta > duration.
                            // This logic is so that this is recorded only on the outermost timeline for when timelines reverse direction
                            return
                        }
                    }
                }
            }
        } else {
            if (updateDirection) {
                var i = 0
                val n = childrenSize

                while (i < n) {
                    val tween = childrenArray[i]
                    val returned = tween.updateUnsafe(delta)
                    if (tween.state == FINISHED) {
                        // each child has to track "overflow" info to set delay's correctly when the timeline reverses
                        tween.currentTime += returned
                    }
                    i++
                }
            } else {
                var i = childrenSizeMinusOne
                val n = 0

                while (i >= n) {
                    val tween = childrenArray[i]
                    val returned = tween.updateUnsafe(delta)
                    if (tween.state == FINISHED) {
                        // each child has to track "overflow" info to set delay's correctly when the timeline reverses
                        tween.currentTime += returned
                    }
                    i--
                }
            }
        }
    }

    /**
     * Forces a timeline/tween to have it's start/target values
     *
     * @param updateDirection direction in which the force is happening. Affects children iteration order (timelines) and start/target
     * values (tweens)
     * @param updateValue this is the start (true) or target (false) to set the tween to.
     */
    override fun setValues(updateDirection: Boolean, updateValue: Boolean) {
        if (updateDirection) {
            var i = 0
            val n = childrenSize

            while (i < n) {
                val tween = childrenArray[i]
                tween.setValues(true, updateValue)
                i++
            }
        } else {
            var i = childrenSizeMinusOne
            val n = 0

            while (i >= n) {
                val tween = childrenArray[i]
                tween.setValues(false, updateValue)
                i--
            }
        }
    }

    override fun containsTarget(target: Any): Boolean {
        var i = 0
        val n = childrenSize

        while (i < n) {
            val tween = childrenArray[i]
            if (tween.containsTarget(target)) {
                return true
            }
            i++
        }

        return false
    }

    override fun containsTarget(target: Any, tweenType: Int): Boolean {
        var i = 0
        val n = childrenSize
        while (i < n) {
            val tween = childrenArray[i]
            if (tween.containsTarget(target, tweenType)) {
                return true
            }
            i++
        }

        return false
    }
}
