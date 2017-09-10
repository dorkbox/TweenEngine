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

/**
 * BaseTween is the base class of Tween and Timeline. It defines the iteration engine used to play animations for any number of times,
 * and in any direction, at any speed.
 * <p/>
 * <p/>
 * It is responsible for calling the different callbacks at the right moments, and for making sure that every callbacks are triggered,
 * even if the update engine gets a big delta time at once.
 * <p/>
 *
 * @see Tween
 * @see Timeline
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
@SuppressWarnings({"ForLoopReplaceableByForEach", "WeakerAccess", "unused", "ResultOfMethodCallIgnored", "UnusedReturnValue"})
public
abstract class BaseTween<T> {
    // if there is a DELAY, the tween will remain inside "START" until it's finished with the delay
    protected static final int INVALID = 0;
    protected static final int START = 1;
    protected static final int RUN = 2;
    protected static final int FINISHED = 3;


    // manages the pool and other (previously) static fields
    protected final TweenEngine animator;

    // we are a simple state machine...
    protected int state = 0;

    // General
    protected int repeatCountOrig;
    private int repeatCount;

    protected boolean canAutoReverse;
    private boolean isPaused;

    /** Used by tween */
    protected boolean isCanceled;

    /** Used by tween */
    protected boolean isInitialized;

    // Timings
    private float startDelay;  // this is the initial delay at the start of a timeline/tween (only happens once). (doesn't change)
    private float repeatDelay; // this is the delay when a timeline/tween is repeating (doesn't change)

    /** Used by tween */
    protected float duration; // how long the timeline/tween runs (doesn't change)


    // represents the amount of time spent in the current iteration or delay
    // protected because our timeline has to be able to adjust for delays when initially building the system.
    // when FORWARDS - if <= 0, it is a delay
    // when REVERSE - if >= duration, it is a delay
    /** Used by timeline */
    protected float currentTime;

    private static final boolean START_VALUES = true;
    private static final boolean TARGET_VALUES = false;

    // Direction state
    private static final boolean FORWARDS = true;
    private static final boolean REVERSE = false;
    private boolean direction = FORWARDS; // default state is forwards

    /** Depending on the state, sometimes we trigger begin events */
    private boolean canTriggerBeginEvent;
    protected boolean isInAutoReverse;

    // Misc
    private Object userData;

    /** Used by tween manager */
    protected boolean isAutoRemoveEnabled;
    protected boolean isAutoStartEnabled;

    private UpdateAction startEventCallback = TweenEngine.NULL_ACTION;
    private UpdateAction endEventCallback = TweenEngine.NULL_ACTION;

    // callbacks (optimized for fast call w/ many callbacks). Verification for multiple triggers is on add.
    private static final TweenCallback[] TEMP_EMPTY = new TweenCallback[0];
    private TweenCallback[] forwards_Begin = new TweenCallback[0];
    private TweenCallback[] forwards_Start = new TweenCallback[0];
    private TweenCallback[] forwards_End = new TweenCallback[0];
    private TweenCallback[] forwards_Complete = new TweenCallback[0];

    private TweenCallback[] reverse_Begin = new TweenCallback[0];
    private TweenCallback[] reverse_Start = new TweenCallback[0];
    private TweenCallback[] reverse_End = new TweenCallback[0];
    private TweenCallback[] reverse_Complete = new TweenCallback[0];

    public
    BaseTween(final TweenEngine animator) {
        this.animator = animator;
    }


    // -------------------------------------------------------------------------

    /**
     * Reset the tween/timeline to it's initial state. It will be as if the tween/timeline has never run before. If it was already
     * initialized, it will *not* redo the initialization.
     * <p>
     * The paused state is preserved.
     */
    protected
    void reset() {
        state = START;
        direction = FORWARDS;
        canTriggerBeginEvent = true; // this is so init can happen if necessary
        currentTime = -startDelay;
        isInAutoReverse = false;
        repeatCount = repeatCountOrig;
    }

    // destroys all information about the object
    protected
    void destroy() {
        repeatCount = repeatCountOrig = 0;
        state = INVALID;

        duration = startDelay = repeatDelay = currentTime = 0.0F;
        isPaused = isCanceled = isInAutoReverse = isInitialized = false;
        canTriggerBeginEvent = true;

        clearCallbacks_();
        userData = null;
        endEventCallback = startEventCallback = TweenEngine.NULL_ACTION;

        isAutoRemoveEnabled = isAutoStartEnabled = true;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Clears all of the callback.
     *
     * Thread/Concurrent use Safe
     */
    @SuppressWarnings("unchecked")
    public
    T clearCallbacks() {
        // ALSO have to prevent anyone from updating/changing callbacks while this is occurring.
        synchronized (TEMP_EMPTY) {
            clearCallbacks_();
        }

        return (T) this;
    }

    /**
     * Clears all of the callback.
     */
    private
    void clearCallbacks_() {
        forwards_Begin = new TweenCallback[0];
        forwards_Start = new TweenCallback[0];
        forwards_End = new TweenCallback[0];
        forwards_Complete = new TweenCallback[0];

        reverse_Begin = new TweenCallback[0];
        reverse_Start = new TweenCallback[0];
        reverse_End = new TweenCallback[0];
        reverse_Complete = new TweenCallback[0];
    }

    /**
     * Adds a callback. By default, it will be fired at the completion of the tween or timeline (event COMPLETE). If you want to change
     * this behavior use the {@link TweenCallback#TweenCallback(int)} constructor.
     *
     * Thread/Concurrent use Safe
     *
     * @see TweenCallback
     */
    @SuppressWarnings("unchecked")
    public final
    T addCallback(final TweenCallback callback) {
        int triggers = callback.triggers;

        // ALSO have to prevent anyone from updating/changing callbacks while this is occurring.
        // not necessary to call flushRead/Write
        synchronized (TEMP_EMPTY) {
            if ((triggers & TweenCallback.Events.BEGIN) == TweenCallback.Events.BEGIN) {
                int currentLength = forwards_Begin.length;

                int newLength = currentLength + 1;
                TweenCallback[] copy = new TweenCallback[newLength];
                System.arraycopy(forwards_Begin, 0, copy, 0, Math.min(currentLength, newLength));

                copy[currentLength] = callback;
                forwards_Begin = copy;
            }
            if ((triggers & TweenCallback.Events.START) == TweenCallback.Events.START) {
                int currentLength = forwards_Start.length;

                int newLength = currentLength + 1;
                TweenCallback[] copy = new TweenCallback[newLength];
                System.arraycopy(forwards_Start, 0, copy, 0, Math.min(currentLength, newLength));

                copy[currentLength] = callback;
                forwards_Start = copy;
            }
            if ((triggers & TweenCallback.Events.END) == TweenCallback.Events.END) {
                int currentLength = forwards_End.length;

                int newLength = currentLength + 1;
                TweenCallback[] copy = new TweenCallback[newLength];
                System.arraycopy(forwards_End, 0, copy, 0, Math.min(currentLength, newLength));

                copy[currentLength] = callback;
                forwards_End = copy;
            }
            if ((triggers & TweenCallback.Events.COMPLETE) == TweenCallback.Events.COMPLETE) {
                int currentLength = forwards_Complete.length;

                int newLength = currentLength + 1;
                TweenCallback[] copy = new TweenCallback[newLength];
                System.arraycopy(forwards_Complete, 0, copy, 0, Math.min(currentLength, newLength));

                copy[currentLength] = callback;
                forwards_Complete = copy;
            }



            if ((triggers & TweenCallback.Events.BACK_BEGIN) == TweenCallback.Events.BACK_BEGIN) {
                int currentLength = reverse_Begin.length;

                int newLength = currentLength + 1;
                TweenCallback[] copy = new TweenCallback[newLength];
                System.arraycopy(reverse_Begin, 0, copy, 0, Math.min(currentLength, newLength));

                copy[currentLength] = callback;
                reverse_Begin = copy;
            }
            if ((triggers & TweenCallback.Events.BACK_START) == TweenCallback.Events.BACK_START) {
                int currentLength = reverse_Start.length;

                int newLength = currentLength + 1;
                TweenCallback[] copy = new TweenCallback[newLength];
                System.arraycopy(reverse_Start, 0, copy, 0, Math.min(currentLength, newLength));

                copy[currentLength] = callback;
                reverse_Start = copy;
            }
            if ((triggers & TweenCallback.Events.BACK_END) == TweenCallback.Events.BACK_END) {
                int currentLength = reverse_End.length;

                int newLength = currentLength + 1;
                TweenCallback[] copy = new TweenCallback[newLength];
                System.arraycopy(reverse_End, 0, copy, 0, Math.min(currentLength, newLength));

                copy[currentLength] = callback;
                reverse_End = copy;
            }
            if ((triggers & TweenCallback.Events.BACK_COMPLETE) == TweenCallback.Events.BACK_COMPLETE) {
                int currentLength = reverse_Complete.length;

                int newLength = currentLength + 1;
                TweenCallback[] copy = new TweenCallback[newLength];
                System.arraycopy(reverse_Complete, 0, copy, 0, Math.min(currentLength, newLength));

                copy[currentLength] = callback;
                reverse_Complete = copy;
            }
        }

        return (T) this;
    }

    /**
     * Adds a start delay to the tween or timeline in seconds.
     *
     * @param delay A duration in seconds for the delay
     *
     * @return The current object, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public
    T delay(final float delay) {
        animator.flushRead();

        delay__(delay);

        animator.flushWrite();
        return (T) this;
    }

    /**
     * doesn't sync on anything.
     * <p>
     * Adds a start delay to the tween or timeline in seconds.
     *
     * @param delay A duration in seconds for the delay
     *
     * @return The current object, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    protected
    T delay__(final float delay) {
        this.startDelay += delay;
        this.currentTime -= delay;

        return (T) this;
    }

    /**
     * Cancels the tween or timeline. If you are starting via {@link Tween#start()}, this object will be removed automatically. If
     * starting via {@link Tween#startUnmanaged()} you must manage the lifecycle automatically.
     */
    public
    void cancel() {
        isCanceled = true;
        animator.flushWrite();
    }

    /**
     * Stops and resets the tween or timeline, and sends it to its pool, for later reuse.
     * <p>
     * If started normally (instead of un-managed), the {@link TweenEngine} will automatically call this method once the animation is complete.
     */
    public
    void free() {
    }

    /**
     * Pauses the tween or timeline. Further update calls won't have any effect.
     */
    public
    void pause() {
        isPaused = true;
        animator.flushWrite();
    }

    /**
     * Resumes the tween or timeline to it's previous state. Has no effect is it was not already paused.
     */
    public
    void resume() {
        isPaused = false;
        animator.flushWrite();
    }

    /**
     * Repeats the tween or timeline for a given number of times.
     *
     * @param count The number of repetitions. For infinite repetition, use {@link Tween#INFINITY} or -1.
     * @param delay A delay between each iteration, in seconds.
     *
     * @return The current tween or timeline, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public
    T repeat(final int count, final float delay) {
        repeat__(count, delay);

        animator.flushWrite();
        return (T) this;
    }

    /**
     * doesn't sync on anything.
     * <p>
     * Repeats the tween or timeline for a given number of times.
     *
     * @param count The number of repetitions. For infinite repetition, use {@link Tween#INFINITY} or -1.
     * @param delay A delay between each iteration, in seconds.
     *
     * @return The current tween or timeline, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    private
    T repeat__(final int count, final float delay) {
        if (count < -1) {
            throw new RuntimeException("Count " + count + " is an invalid option. It must be -1 (Tween.INFINITY) for infinite or > 0 for " +
                                       "finite.");
        }

        repeatCountOrig = count;
        repeatCount = count;
        repeatDelay = delay;
        canAutoReverse = false;

        return (T) this;
    }

    /**
     * Repeats the tween or timeline for a given number of times.
     * </p>
     * Once an iteration is complete, it will be played in reverse.
     *
     * @param count The number of repetitions. For infinite repetition, use {@link Tween#INFINITY} or -1.
     * @param delay A delay before each repetition, in seconds.
     *
     * @return The current tween or timeline, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public
    T repeatAutoReverse(final int count, final float delay) {
        repeat__(count, delay);

        canAutoReverse = true;

        animator.flushWrite();
        return (T) this;
    }

    /**
     * Sets the "start" callback, which is called when the tween/timeline starts running, NULL to remove.
     *
     * @param startCallback this is the object that will be notified when the tween/timeline starts running. NULL to unset.
     *
     * @return The current tween or timeline, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public final
    T setStartCallback(final UpdateAction<T> startCallback) {
        if (startCallback == null) {
            this.startEventCallback = TweenEngine.NULL_ACTION;
        }
        else {
            this.startEventCallback = startCallback;
        }

        animator.flushWrite();
        return (T) this;
    }

    /**
     * Sets the "end" callback, which is called when the tween/timeline finishes running, NULL to remove.
     *
     * @param endCallback this is the object that will be notified when the tween/timeline finishes running. NULL to unset.
     *
     * @return The current tween or timeline, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public final
    T setEndCallback(final UpdateAction<T> endCallback) {
        if (endCallback == null) {
            this.endEventCallback = TweenEngine.NULL_ACTION;
        }
        else {
            this.endEventCallback = endCallback;
        }


        animator.flushWrite();
        return (T) this;
    }

    /**
     * Starts or restarts the object unmanaged. You will need to take care of its life-cycle.
     *
     * @return The current object, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public
    T startUnmanaged() {
        startUnmanaged__();

        animator.flushWrite();
        return (T) this;
    }

    /**
     * Starts or restarts the object unmanaged. You will need to take care of its life-cycle.
     */
    void startUnmanaged__() {
        setup__();
    }

    /**
     * Convenience method to add an object to a manager where it's life-cycle will be automatically handled .
     *
     * @return The current object, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public
    T start() {
        animator.flushRead();

        animator.add__(this);

        animator.flushWrite();
        return (T) this;
    }

    /**
     * doesn't sync on anything.
     * <p>
     * Prepares the state of the tween before running (or initializing)
     */
    protected
    void setup__() {
        canTriggerBeginEvent = true;
        state = START;
    }


    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Gets the current time point of a Timeline/Tween in seconds
     */
    public final
    float getCurrentTime() {
        animator.flushRead();
        return currentTime;
    }

    /**
     * Gets the delay of the Timeline/Tween in seconds. Nothing will happen until this delay is complete.
     */
    public final
    float getStartDelay() {
        animator.flushRead();
        return startDelay;
    }

    /**
     * Gets the duration of a Timeline/Tween "single iteration" (not counting repeats) in seconds
     */
    public
    float getDuration() {
        animator.flushRead();
        return duration;
    }

    /**
     * Returns the complete duration, including initial delay and repetitions in seconds
     * <p>
     * The formula is as follows:
     * <pre>
     * fullDuration = delay + duration + ((repeatDelay + duration) * repeatCount)
     * </pre>
     */
    public
    float getFullDuration() {
        animator.flushRead();
        return getFullDuration__();
    }

    /**
     * doesn't sync on anything.
     * <p>
     * Returns the complete duration, including initial delay and repetitions in seconds
     * <p>
     * The formula is as follows:
     * <pre>
     * fullDuration = delay + duration + ((repeatDelay + duration) * repeatCount)
     * </pre>
     */
    final
    float getFullDuration__() {
        if (repeatCountOrig < 0) {
            return -1;
        }
        return startDelay + duration + ((repeatDelay + duration) * repeatCountOrig);
    }

    /**
     * Gets the number of iterations that will be played.
     */
    public final
    int getRepeatCount() {
        animator.flushRead();
        return repeatCountOrig;
    }

    /**
     * Gets the delay occurring between two iterations in seconds
     */
    public final
    float getRepeatDelay() {
        animator.flushRead();
        return repeatDelay;
    }

    /**
     * Returns the direction the tween/timeline currently is in.
     * <p/>
     * Reverse direction can be impacted by a negative value for {@link #update(float)} or via a tween reversing direction because of
     * {@link #repeatAutoReverse(int, float)}
     *
     * @return true if the current tween stage is in the forwards direction, false if reverse (or Backwards)
     */
    public final
    boolean getDirection() {
        animator.flushRead();
        return direction;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * @return true if the Timeline/Tween is waiting inside of a delay.
     */
    public final
    boolean isInDelay() {
        animator.flushRead();
        return state == START;
    }

    /**
     * @return true if the timeline/tween is currently "auto-reversing" in it's direction.
     */
    public final
    boolean isInAutoReverse() {
        animator.flushRead();
        return isInAutoReverse;
    }


    /**
     * Returns true if the Timeline/Tween has been initialized. This is the most accurate method to determine if a Timeline/Tween has
     * been started.
     */
    public
    boolean isInitialized() {
        animator.flushRead();
        return isInitialized;
    }

    /**
     * Returns true if the Timeline/Tween is finished (i.e. if the tween has reached its end or has been canceled). A tween may be restarted
     * by a timeline when there is a direction change in the timeline.
     * </p>
     * If the Tween/Timeline is un-managed, you should call {@link BaseTween#free()} to reuse the object later.
     */
    public
    boolean isFinished() {
        animator.flushRead();
        return state == FINISHED || isCanceled;
    }

    /**
     * Returns true if the tween automatically reverse when complete.
     */
    public
    boolean canAutoReverse() {
        animator.flushRead();
        return canAutoReverse;
    }

    /**
     * Returns true if the tween or timeline is currently paused.
     */
    public
    boolean isPaused() {
        animator.flushRead();
        return isPaused;
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
    public
    void disableAutoRemove() {
        this.isAutoRemoveEnabled = false;
        animator.flushWrite();
    }

    /**
     * Disables the "auto start" mode of any tween manager for a particular tween or timeline. Tweens/Timelines are auto-started by
     * default. If it is not enabled, add a tween or timeline to any manager won't start it automatically, and you'll need to
     * call .start() manually on your object.
     */
    public
    void disableAutoStart() {
        this.isAutoStartEnabled = false;
        animator.flushWrite();
    }

    // -------------------------------------------------------------------------
    // User Data
    // -------------------------------------------------------------------------

    /**
     * Attaches an object to this tween or timeline. It can be useful in order
     * to retrieve some data from a TweenCallback.
     *
     * @param data Any kind of object.
     *
     * @return The current tween or timeline, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public
    T setUserData(final Object data) {
        userData = data;
        animator.flushWrite();
        return (T) this;
    }

    /**
     * Gets the attached data, or null if none.
     */
    @SuppressWarnings("unchecked")
    public
    T getUserData() {
        animator.flushRead();
        return (T) userData;
    }

    // -------------------------------------------------------------------------
    // Abstract API
    // -------------------------------------------------------------------------

    protected abstract
    boolean containsTarget(final Object target);

    protected abstract
    boolean containsTarget(final Object target, final int tweenType);

    /**
     * Updates a timeline's children. Only called during State.RUN
     */
    protected abstract
    void update(final boolean updateDirection, final float delta);

    /**
     * Forces a Timeline/Tween to have it's start/target values
     *
     * @param updateDirection direction in which the force is happening. Affects children iteration order (timelines) and start/target
     *                        values (tweens)
     * @param updateValue this is the start (true) or end/target (false) to set the tween to.
     */
    protected abstract
    void setValues(final boolean updateDirection, final boolean updateValue);


    /**
     * Sets the tween or timeline to a specific point in time based on it's duration + delays. Callbacks are not notified and the change is
     * immediate. The tween/timeline will continue in it's original direction
     * For example:
     * <ul>
     * <li> setProgress(0F, true) : set it to the starting position just after the start delay in the forward direction</li>
     * <li> setProgress(.5F, true) : set it to the middle position in the forward direction</li>
     * <li> setProgress(.5F, false) : set it to the middle position in the reverse direction</li>
     * <li> setProgress(1F, false) : set it to the end position in the reverse direction</li>
     * </ul>
     * <p>
     * Caveat: If the timeline/tween is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished state
     * (end position). If the timeline/tween is in repeat mode then it will end up in the same position if it was going forwards.
     *
     * @param percentage the percentage (of it's duration) from 0-1, that the tween/timeline be set to
     */
    public
    T setProgress(final float percentage) {
        animator.flushRead();
        return setProgress(percentage, this.direction);
    }

    /**
     * Sets the tween or timeline to a specific point in time based on it's duration + delays. Callbacks are not notified and the change is
     * immediate.
     * For example:
     * <ul>
     *     <li> setProgress(0F, true) : set it to the starting position just after the start delay in the forward direction</li>
     *     <li> setProgress(.5F, true) : set it to the middle position in the forward direction</li>
     *     <li> setProgress(.5F, false) : set it to the middle position in the reverse direction</li>
     *     <li> setProgress(1F, false) : set it to the end position in the reverse direction</li>
     * </ul>
     * <p>
     * Caveat: If the timeline/tween is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished state
     *          (end position). If the timeline/tween is in repeat mode then it will end up in the same position if it was going forwards.
     *
     * @param percentage the percentage (of it's duration) from 0-1, that the tween/timeline be set to
     * @param direction sets the direction of the timeline when it updates next: forwards (true) or reverse (false).
     */
    @SuppressWarnings("unchecked")
    public
    T setProgress(final float percentage, final boolean direction) {
        if (percentage < -0.0F || percentage > 1.0F) {
            throw new RuntimeException("Cannot set the progress <0 or >1");
        }

        //flushRead();   // synchronize takes care of this

        // have to SAVE all of the callbacks (to stop all from executing).
        // ALSO have to prevent anyone from updating/changing callbacks while this is occurring.
        synchronized (TEMP_EMPTY) {
            // always have to reset, because of issues with delays and repetitions. (also sets the direction to "forwards")
            reset();

            // how much time is represented by the delta in percentage of time?
            final float duration = this.duration;
            final float percentageValue = duration * percentage;
            final float adjustmentTime;

            // Caveat: If the timeline/tween is set to end in reverse, and it CANNOT go in reverse, then it will end up in the finished/end position
            // if we specify to "go in reverse" and we are in a "repeat" mode (instead of a "flip-to-reverse" mode), then just pretend we
            // specified to "go forwards".
            boolean goesReverse = !direction && canAutoReverse;
            if (goesReverse) {
                // we want the tween/timeline in the REVERSE state when finished, so the next delta update will move it in that direction
                // to do this, we "wrap around" the timeline/tween times to the correct time, in a single update.

                final float timeSpentToGetToEnd = duration + startDelay;
                final float timeSpentInReverseFromEnd = (duration - percentageValue);

                adjustmentTime = timeSpentToGetToEnd + timeSpentInReverseFromEnd;
            } else {
                // we just go from the absolute start (including the delay) to where we should end up
                adjustmentTime = percentageValue + startDelay;
            }

            TweenCallback[] forwards_Begin_saved = forwards_Begin;
            TweenCallback[] forwards_Start_saved = forwards_Start;
            TweenCallback[] forwards_End_saved = forwards_End;
            TweenCallback[] forwards_Complete_saved = forwards_Complete;
            TweenCallback[] reverse_Begin_saved = reverse_Begin;
            TweenCallback[] reverse_Start_saved = reverse_Start;
            TweenCallback[] reverse_End_saved = reverse_End;
            TweenCallback[] reverse_Complete_saved = reverse_Complete;

            forwards_Begin = TEMP_EMPTY;
            forwards_Start = TEMP_EMPTY;
            forwards_End = TEMP_EMPTY;
            forwards_Complete = TEMP_EMPTY;
            reverse_Begin = TEMP_EMPTY;
            reverse_Start = TEMP_EMPTY;
            reverse_End = TEMP_EMPTY;
            reverse_Complete = TEMP_EMPTY;


            // update by the timeline/tween this amount (always starting from "scratch"). It will automatically end up in the correct direction.
            update__(adjustmentTime);


            // have to RESTORE all of the callbacks
            forwards_Begin = forwards_Begin_saved;
            forwards_Start = forwards_Start_saved;
            forwards_End = forwards_End_saved;
            forwards_Complete = forwards_Complete_saved;
            reverse_Begin = reverse_Begin_saved;
            reverse_Start = reverse_Start_saved;
            reverse_End = reverse_End_saved;
            reverse_Complete = reverse_Complete_saved;
        }

        // flushWrite();   // synchronize takes care of this
        return (T) this;
    }

    // -------------------------------------------------------------------------
    // Protected API
    // -------------------------------------------------------------------------

    protected
    void initializeValues() {
    }

    /**
     * Kills every tweens associated to the given target. Will also kill every timeline containing a tween associated to the given target.
     *
     * @return true if the target was killed, false if we do not contain the target, and it was not killed
     */
    protected
    boolean cancelTarget(final Object target) {
        if (containsTarget(target)) {
            cancel();
            return true;
        }

        return false;
    }

    /**
     * Kills every tweens associated to the given target and tween type. Will also kill every timelines containing a tween associated to
     * the given target and tween type.
     *
     * @return true if the target was killed, false if we do not contain the target, and it was not killed
     */
    protected
    boolean cancelTarget(final Object target, final int tweenType) {
        if (containsTarget(target, tweenType)) {
            cancel();
            return true;
        }

        return false;
    }

    /**
     * Adjust the tween for when repeat + auto-reverse is used
     *
     * @param newDirection the new direction for all children
     */
    protected
    void adjustForRepeat_AutoReverse(final boolean newDirection) {
        state = START;

        if (newDirection) {
            currentTime = 0;
        }
        else {
            currentTime = duration;
        }
    }

    /**
     * Adjust the current time (set to the start value for the tween) and change state to DELAY.
     * </p>
     * For timelines, this also changes what the current tween is (for when iterating over tweens)
     *
     * @param newDirection the new direction for all children
     */
    protected
    void adjustForRepeat_Linear(final boolean newDirection) {
        state = START;

        if (newDirection) {
            currentTime = 0;
        }
        else {
            currentTime = duration;
        }
    }


    // -------------------------------------------------------------------------
    // Update engine
    // -------------------------------------------------------------------------

    /**
     * Updates the tween or timeline state and values.
     * <p>
     * <b>The preferred way to update a tween is via {@link TweenEngine#update(float)}.</b>
     * <p>
     * Slow motion, fast motion and backward play can be easily achieved by tweaking this delta time.
     * <p>
     * Multiply it by -1 to play the animation backward, or by 0.5 to play it twice-as-slow than its normal speed.
     * <p>
     * <p>
     * The tween manager doesn't call this method, it correctly calls updateState + updateValues on timeline/tweens
     * <p>
     * Copyright dorkbox, llc
     *
     * @param delta the time in SECONDS that has elapsed since the last update
     *
     * @return true if this tween/timeline is finished (STATE = FINISHED)
     */
    // this method was completely rewritten.
    @SuppressWarnings({"unchecked", "Duplicates", "ConstantConditions"})
    public final
    float update(float delta) {
        animator.flushRead();
        float v = update__(delta);
        animator.flushWrite();

        return v;
    }
    /**
     * doesn't sync on anything.
     * <p>
     * Updates the tween or timeline state and values.
     * <p>
     * <b>The preferred way to update a tween is via {@link TweenEngine#update(float)}.</b>
     * <p>
     * Slow motion, fast motion and backward play can be easily achieved by tweaking this delta time.
     * <p>
     * Multiply it by -1 to play the animation backward, or by 0.5 to play it twice-as-slow than its normal speed.
     * <p>
     * <p>
     * The tween manager doesn't call this method, it correctly calls updateState + updateValues on timeline/tweens
     * <p>
     * Copyright dorkbox, llc
     *
     * @param delta the time in SECONDS that has elapsed since the last update
     *
     * @return the amount of time remaining (this is the amount of delta that wasn't processed)
     */
    // this method was completely rewritten.
    @SuppressWarnings({"unchecked", "Duplicates", "ConstantConditions"})
    protected final
    float update__(float delta) {
        if (isPaused || isCanceled) {
            return delta;
        }

        if (isInAutoReverse) {
            delta = -delta;
        }

        // the INITIAL, incoming delta from the app, will be positive or negative.
        // Specifically check for +0.0F so that -0.0F will let us go in reverse
        boolean direction = delta >= +0.0F;
        this.direction = direction;

        final float duration = this.duration;

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

        startEventCallback.onEvent(this);

        do {
            float newTime = currentTime + delta;

            if (direction) {
                // {FORWARDS}
                // <editor-fold>

                // FORWARDS: 0 > time <= duration
                switch (state) {
                    case START: {
                        if (newTime <= 0.0F) {
                            // still in start delay
                            currentTime = newTime;

                            endEventCallback.onEvent(this);
                            return 0.0F;
                        }

                        currentTime = 0.0F;

                        if (canTriggerBeginEvent) {
                            canTriggerBeginEvent = false;

                            // initialize during start (but after delay), so that it's at the same point in either direction
                            if (!isInitialized) {
                                isInitialized = true;
                                initializeValues();
                            }

                            final TweenCallback[] callbacks = this.forwards_Begin;
                            for (int i = 0, n = callbacks.length; i < n; i++) {
                                callbacks[i].onEvent(TweenCallback.Events.BEGIN, this);
                            }
                        }

                        final TweenCallback[] callbacks = this.forwards_Start;
                        for (int i = 0, n = callbacks.length; i < n; i++) {
                            callbacks[i].onEvent(TweenCallback.Events.START, this);
                        }

                        // goto next state
                        state = RUN;

                        // -- update is REVERSE so that the FIRST tween data takes priority, if there are
                        //    multiple tweens that have the same target
                        setValues(REVERSE, START_VALUES);

                        // adjust the delta so that it is shifted based on the length of (previous) iteration
                        delta = newTime;

                        // FALLTHROUGH
                    }
                    case RUN: {
                        if (newTime <= duration) {
                            // still in running forwards
                            currentTime = newTime;

                            update(FORWARDS, delta);

                            endEventCallback.onEvent(this);
                            return 0.0F;
                        }


                        state = FINISHED;
                        currentTime = duration;

                        final int repeatCountStack = repeatCount;
                        ////////////////////////////////////////////
                        ////////////////////////////////////////////
                        // 1) we are done running completely
                        // 2) we flip to auto-reverse repeat mode
                        // 3) we are in linear repeat mode
                        if (repeatCountStack == 0) {
                            // {FORWARDS}{FINISHED}

                            // -- update is REVERSE so that the FIRST tween data takes priority, if there are
                            //    multiple tweens that have the same target

                            // "instant" tweens (duration 0) cannot trigger a set-to-startpoint (since they are always [enabled] while
                            // running). They are [disabled] by their parent timeline when the parent reaches the end of it's duration
                            // in the FORWARDS direction, this doesn't matter, but in REVERSE, it does.
                            setValues(REVERSE, TARGET_VALUES);


                            final TweenCallback[] callbacks = this.forwards_End;
                            for (int i = 0, n = callbacks.length; i < n; i++) {
                                callbacks[i].onEvent(TweenCallback.Events.END, this);
                            }

                            final TweenCallback[] callbacks2 = this.forwards_Complete;
                            for (int i = 0, n = callbacks2.length; i < n; i++) {
                                callbacks2[i].onEvent(TweenCallback.Events.COMPLETE, this);
                            }

                            // don't do this, because it will xfer to the next tween (if a timeline), or will get added in the FINISHED
                            // case (if not a timeline, to record "overflow" of time)
                            //    currentTime = newTime;

                            // we're done going forwards
                            canTriggerBeginEvent = true;
                            isInAutoReverse = false;

                            // have to reset our repeat count, so outside repeats will start us in the correct state
                            repeatCount = repeatCountOrig;

                            endEventCallback.onEvent(this);

                            // return the time that is remaining (the remaining amount of delta that wasn't processed)
                            return newTime - duration;
                        }
                        else {
                            // must always update all of the children
                            update(FORWARDS, delta);

                            if (repeatCountStack > 0) {
                                // -1 means repeat forever
                                repeatCount--;
                            }

                            final TweenCallback[] callbacks = this.forwards_End;
                            for (int i = 0, n = callbacks.length; i < n; i++) {
                                callbacks[i].onEvent(TweenCallback.Events.END, this);
                            }

                            if (canAutoReverse) {
                                // {FORWARDS}{AUTO_REVERSE}

                                final TweenCallback[] callbacks2 = this.forwards_Complete;
                                for (int i = 0, n = callbacks2.length; i < n; i++) {
                                    callbacks2[i].onEvent(TweenCallback.Events.COMPLETE, this);
                                }

                                // we're done going forwards
                                canTriggerBeginEvent = true;
                                isInAutoReverse = !isInAutoReverse; // if we are NOT in autoReverse, then "isInAutoReverse" is true if we reverse

                                // make sure any checks after this returns accurately reflect the correct REVERSE direction
                                direction = REVERSE;

                                // any extra time (what's left in delta) will be applied/calculated on the next loop around
                                adjustForRepeat_AutoReverse(REVERSE);
                                currentTime += repeatDelay;

                                // because we always continue the loop, we must adjust the delta so that it is shifted (in REVERSE)
                                // delta = newTime - duration;
                                // delta = -delta
                                delta = -newTime + duration;

                                // loop to new state
                                continue;
                            }
                            else {
                                // {FORWARDS}{LINEAR}

                                isInAutoReverse = false;

                                // any extra time (what's left in delta) will be applied/calculated on the next loop around
                                adjustForRepeat_Linear(FORWARDS);

                                // because we always continue the loop, we must adjust the delta so that it is shifted
                                delta = newTime - duration;

                                currentTime = -repeatDelay + delta;

                                // loop to new state
                                continue;
                            }
                        }
                    }
                    case FINISHED: {
                        if (newTime <= 0.0F || newTime > duration) {
                            // still in the "finished" state, and haven't been reversed somewhere
                            currentTime = newTime;

                            endEventCallback.onEvent(this);
                            return 0.0F;
                        }

                        // restart the timeline, since we've had our time adjusted to a point where we are running again.
                        state = START;

                        update(FORWARDS, delta);

                        // loop to new state
                        continue;
                    }
                    default: {
                        throw new RuntimeException("Unexpected state!! '" + state + "'");
                    }
                }

                // </editor-fold>
            }
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            else {
                // {REVERSE}
                // <editor-fold>

                // REVERSE:  0 >= time < duration   (reverse always goes from duration -> 0)
                switch (state) {
                    case START: {
                        if (newTime >= duration) {
                            // still in delay
                            currentTime = newTime;

                            endEventCallback.onEvent(this);
                            return 0.0F;
                        }

                        currentTime = duration;

                        if (canTriggerBeginEvent) {
                            canTriggerBeginEvent = false;

                            // initialize during start (but after delay), so that it's at the same point in either direction
                            if (!isInitialized) {
                                isInitialized = true;
                                initializeValues();
                            }

                            final TweenCallback[] callbacks = this.reverse_Begin;
                            for (int i = 0, n = callbacks.length; i < n; i++) {
                                callbacks[i].onEvent(TweenCallback.Events.BACK_BEGIN, this);
                            }
                        }

                        final TweenCallback[] callbacks = this.reverse_Start;
                        for (int i = 0, n = callbacks.length; i < n; i++) {
                            callbacks[i].onEvent(TweenCallback.Events.BACK_START, this);
                        }

                        // goto next state
                        state = RUN;

                        // -- update is FORWARDS so that the LAST tween data takes priority, if there are
                        //    multiple tweens that have the same target
                        // this is opposite of the logic in FORWARDS.START
                        setValues(FORWARDS, TARGET_VALUES);

                        // adjust the delta so that it is shifted based on the length of (previous) iteration
                        delta = -(duration - newTime);

                        // FALLTHROUGH
                    }
                    case RUN: {
                        if (newTime >= 0.0F) {
                            // still in running reverse
                            currentTime = newTime;

                            update(REVERSE, delta);

                            endEventCallback.onEvent(this);
                            return 0.0F;
                        }

                        state = FINISHED;
                        currentTime = 0.0F;

                        final int repeatCountStack = repeatCount;
                        ////////////////////////////////////////////
                        ////////////////////////////////////////////
                        // 1) we are done running completely
                        // 2) we flip to auto-reverse
                        // 3) we are in linear repeat mode
                        if (repeatCountStack == 0) {
                            // {REVERSE}{FINISHED}

                            // set the "start" values, backwards because values are relative to forwards

                            // -- update is FORWARDS so that the LAST tween data takes priority, if there are
                            //    multiple tweens that have the same target
                            if (duration <= 0.000001F) {
                                // "instant" tweens (duration 0) cannot trigger a set-to-startpoint (since they are always [enabled] while
                                // running). They are [disabled] by their parent timeline when the parent reaches the end of it's duration
                                // This is why it's always set to target value (even though it's reverse)
                                setValues(FORWARDS, TARGET_VALUES);
                            }
                            else {
                                // set the "start" values, flipped because we are in reverse
                                setValues(FORWARDS, START_VALUES);
                            }

                            final TweenCallback[] callbacks = this.reverse_End;
                            for (int i = 0, n = callbacks.length; i < n; i++) {
                                callbacks[i].onEvent(TweenCallback.Events.BACK_END, this);
                            }

                            final TweenCallback[] callbacks2 = this.reverse_Complete;
                            for (int i = 0, n = callbacks2.length; i < n; i++) {
                                callbacks2[i].onEvent(TweenCallback.Events.BACK_COMPLETE, this);
                            }

                            // don't do this, because it will xfer to the next tween (if a timeline), or will get added in the FINISHED
                            // case (if not a timeline, to record "overflow" of time)
                            //    currentTime = newTime;

                            // we're done going reverse
                            canTriggerBeginEvent = true;
                            isInAutoReverse = false;

                            // have to reset our repeat count, so outside repeats will start us in the correct state
                            repeatCount = repeatCountOrig;

                            endEventCallback.onEvent(this);

                            // return the time that is remaining (the remaining amount of delta that wasn't processed)
                            return newTime;
                        }
                        else {
                            // must always update all of the children
                            update(REVERSE, delta);

                            if (repeatCountStack > 0) {
                                // -1 means repeat forever
                                repeatCount--;
                            }

                            final TweenCallback[] callbacks = this.reverse_End;
                            for (int i = 0, n = callbacks.length; i < n; i++) {
                                callbacks[i].onEvent(TweenCallback.Events.BACK_END, this);
                            }

                            if (canAutoReverse) {
                                // {REVERSE}{AUTO_REVERSE}

                                final TweenCallback[] callbacks2 = this.reverse_Complete;
                                for (int i = 0, n = callbacks2.length; i < n; i++) {
                                    callbacks2[i].onEvent(TweenCallback.Events.BACK_COMPLETE, this);
                                }

                                // we're done going forwards
                                canTriggerBeginEvent = true;
                                isInAutoReverse = !isInAutoReverse; // if we are NOT in autoReverse, then "isInAutoReverse" is true if we reverse

                                // make sure any checks after this returns accurately reflect the correct FORWARDS direction
                                direction = FORWARDS;

                                // any extra time (what's left in delta) will be applied/calculated on the next loop around
                                adjustForRepeat_AutoReverse(FORWARDS);
                                currentTime -= repeatDelay;

                                // because we always continue the loop, we must adjust the delta so that it is shifted (in FORWARDS)
                                // delta = newTime;
                                // delta = -delta
                                delta = -newTime;

                                // loop to new state
                                continue;
                            }
                            else {
                                // {REVERSE}{LINEAR}

                                isInAutoReverse = false;

                                // any extra time (what's left in delta) will be applied/calculated on the next loop around
                                adjustForRepeat_Linear(REVERSE);

                                // because we always continue the loop, we must adjust the delta so that it is shifted
                                // delta = newTime;

                                currentTime = repeatDelay + newTime;

                                // loop to new state
                                continue;
                            }
                        }
                    }
                    case FINISHED: {
                        if (newTime < 0.0F || newTime >= duration) {
                            // still in the "finished" state, and haven't been reversed somewhere
                            currentTime = newTime;

                            endEventCallback.onEvent(this);
                            return 0.0F;
                        }

                        // restart the timeline, since we've had our time adjusted to a point where we are running again.
                        state = START;

                        update(REVERSE, delta);

                        // loop to new state
                        continue;
                    }
                    default: {
                        throw new RuntimeException("Unexpected state!! '" + state + "'");
                    }
                }

                // </editor-fold>
            }
        } while (true);
    }
}
