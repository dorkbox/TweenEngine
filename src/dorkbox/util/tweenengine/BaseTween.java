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

import java.util.ArrayList;
import java.util.List;

/**
 * BaseTween is the base class of Tween and Timeline. It defines the
 * iteration engine used to play animations for any number of times, and in
 * any direction, at any speed.
 * <p/>
 *
 * It is responsible for calling the different callbacks at the right moments,
 * and for making sure that every callbacks are triggered, even if the update
 * engine gets a big delta time at once.
 *
 * @see Tween
 * @see Timeline
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
@SuppressWarnings("unused")
public
abstract class BaseTween<T> {
	// General
	private int repeatCountOrig;
	private int repeatCount;
	private boolean isTweenRunning;
	private boolean canAutoReverse;

	// Timings
	protected int delay;  // this is the initial delay at the start of a timeline/tween (only happens once).
	protected int duration; // doesn't change while running

	private int repeatDelay;

    // represents the amount of time spent in the current iteration or delay
    // package local because our timeline has to be able to adjust for delays when switching to reverse
    protected int currentTime;

    // Direction state
    protected static final boolean FORWARDS = true;
    private static final boolean REVERSE = false;
    private boolean cachedDirection;

	private boolean isStarted; // TRUE when the object is started
	private boolean isInitialized; // TRUE after the delay
	private boolean isKilled; // TRUE if kill() was called
	private boolean isPaused; // TRUE if pause() was called

    private boolean isInCycle; // FALSE until BEGIN notification, TRUE until COMPLETE notification

    // TRUE when all repetitions are done, changed by timeline if direction
    // changes since a tween might not have repetitions, but a timeline can
    protected boolean isFinished;

	// Misc
	private List<TweenCallback> callbacks = new ArrayList<TweenCallback>();
	private Object userData;

	// Package access
	boolean isAutoRemoveEnabled;
	boolean isAutoStartEnabled;
    public char name = '.';

    // -------------------------------------------------------------------------

	protected
    void reset() {
        repeatCount = repeatCountOrig = 0;
        isTweenRunning = canAutoReverse = false;
        cachedDirection = FORWARDS;

        delay = duration = repeatDelay = currentTime = 0;
        isStarted = isInitialized = isFinished = isKilled = isPaused = isInCycle = false;

        callbacks.clear();
        userData = null;

        isAutoRemoveEnabled = isAutoStartEnabled = true;
    }

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

    /**
     * Adds a callback. By default, it will be fired at the completion of the
     * tween or timeline (event COMPLETE). If you want to change this behavior
     * use the {@link TweenCallback#setTriggers(int)} method.
     *
     * @see TweenCallback
     */
    @SuppressWarnings("unchecked")
    public
    T addCallback(final TweenCallback callback) {
        this.callbacks.add(callback);
        return (T) this;
    }


	/**
	 * Builds and validates the object. Only needed if you want to finalize a
	 * tween or timeline without starting it, since a call to ".start()" also
	 * calls this method.
	 *
	 * @return The current object, for chaining instructions.
	 */
    @SuppressWarnings("unchecked")
	public
    T build() {
		return (T) this;
	}


    /**
     * Clears all of the callback.
     */
    @SuppressWarnings("unchecked")
    public
    T clearCallbacks() {
        this.callbacks.clear();
        return (T) this;
    }

	/**
	 * Adds a delay to the tween or timeline in seconds.
	 *
	 * @param delay A duration in MilliSeconds
     *
	 * @return The current object, for chaining instructions.
	 */
    @SuppressWarnings("unchecked")
	public
    T delay(final int delay) {
		this.delay += delay;
		return (T) this;
	}

	/**
	 * Kills the tween or timeline. If you are using a TweenManager, this object
	 * will be removed automatically.
	 */
	public
    void kill() {
		isKilled = true;
	}

	/**
	 * Stops and resets the tween or timeline, and sends it to its pool, for
	 * later reuse.
     * <p>
     * If you use a {@link TweenManager}, this method is automatically called
     * once the animation is complete.
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
	}

	/**
	 * Resumes the tween or timeline. Has no effect is it was no already paused.
	 */
	public
    void resume() {
		isPaused = false;
	}

	/**
	 * Repeats the tween or timeline for a given number of times.
     *
	 * @param count The number of repetitions. For infinite repetition,
	 *              use {@link Tween#INFINITY} or -1.
	 * @param delayMilliSeconds A delay between each iteration, in MILLI-SECONDS.
     *
	 * @return The current tween or timeline, for chaining instructions.
	 */
    @SuppressWarnings("unchecked")
	public
    T repeat(final int count, final int delayMilliSeconds) {
        if (isStarted) {
            throw new RuntimeException("You can't change the repetitions of a tween or timeline once it is started");
        }
        if (delay < 0) {
            throw new RuntimeException("You can't have a negative delay");
        }

        repeatCount = count;
        repeatCountOrig = count;
        repeatDelay = delayMilliSeconds;
        canAutoReverse = false;
        return (T) this;
	}

	/**
	 * Repeats the tween or timeline for a given number of times.
	 * Once an iteration is complete, it will be played in reverse.
	 *
	 * @param count The number of repetitions. For infinite repetition,
	 *              use {@link Tween#INFINITY} or -1.
	 * @param delayMilliSeconds A delay before each repetition, in MILLI-SECONDS.
     *
	 * @return The current tween or timeline, for chaining instructions.
	 */
    @SuppressWarnings("unchecked")
	public
    T repeatAutoReverse(final int count, final int delayMilliSeconds) {
        repeat(count, delayMilliSeconds);

        canAutoReverse = true;
        return (T) this;
	}

	/**
	 * Attaches an object to this tween or timeline. It can be useful in order
	 * to retrieve some data from a TweenCallback.
	 *
	 * @param data Any kind of object.
	 * @return The current tween or timeline, for chaining instructions.
	 */
    @SuppressWarnings("unchecked")
	public
    T setUserData(final Object data) {
		userData = data;
		return (T) this;
	}

    /**
     * Starts or restarts the object unmanaged. You will need to take care of
     * its life-cycle. If you want the tween to be managed for you, use a
     * {@link TweenManager}.
     *
     * @return The current object, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public
    T start() {
        build();
        currentTime = 0;
        isStarted = true;
        return (T) this;
    }

    /**
     * Convenience method to add an object to a manager. Its life-cycle will be
     * handled for you. Relax and enjoy the animation.
     *
     * @return The current object, for chaining instructions.
     */
    @SuppressWarnings("unchecked")
    public
    T start(final TweenManager manager) {
        manager.add(this);
        return (T) this;
    }

	// -------------------------------------------------------------------------
	// Getters
	// -------------------------------------------------------------------------

    /**
     * Gets the current time point of a Timeline/Tween.
     */
    public
    int getCurrentTime() {
        return currentTime;
    }

	/**
	 * Gets the delay of the tween or timeline. Nothing will happen before
	 * this delay.
	 */
	public
    int getDelay() {
		return delay;
	}

	/**
	 * Gets the duration of a single iteration.
	 */
	public
    int getDuration() {
		return duration;
	}

    /**
     * Returns the complete duration, including initial delay and repetitions.
     * <p>
     * The formula is as follows:
     * <pre>
     * fullDuration = delay + duration + ((repeatDelay + duration) * repeatCount)
     * </pre>
     */
    public
    int getFullDuration() {
        if (repeatCountOrig < 0) {
            return -1;
        }
        return delay + duration + ((repeatDelay + duration) * repeatCountOrig);
    }

	/**
	 * Gets the number of iterations that will be played.
	 */
	public
    int getRepeatCount() {
		return repeatCountOrig;
	}

	/**
	 * Gets the delay occurring between two iterations.
	 */
	public
    int getRepeatDelay() {
		return repeatDelay;
	}

	/**
	 * Gets the attached data, or null if none.
	 */
	@SuppressWarnings("unchecked")
    public
    T getUserData() {
		return (T) userData;
	}

    /**
     * @return true if the current step is in the forwards iteration, and it's not waiting during a delay.
     */
    public final
    boolean isTweenRunning() {
        return isTweenRunning;
    }

    /**
     * Reverse direction can be impacted by a negative value for {@link #update(float)}
     * or {@link #update(int), or via a tween reversing direction because
     * of {@link #repeatAutoReverse(int, int)}
     *
     * @return true if the current tween stage is in the reverse direction.
     */
    public final
    boolean isInReverse() {
        // direction => TRUE == (FORWARDS)
        return !cachedDirection;
    }

    /**
	 * Returns true if the tween or timeline has been started.
	 */
	public
    boolean isStarted() {
		return isStarted;
	}

	/**
	 * Returns true if the tween or timeline has been initialized. Starting
	 * values for tweens are stored at initialization time. This initialization
	 * takes place right after the initial delay, if any.
	 */
	public
    boolean isInitialized() {
		return isInitialized;
	}

	/**
	 * Returns true if the tween is finished (i.e. if the tween has reached
	 * its end or has been killed). A tween may be restarted by a timeline
     * when there is a direction change in the timeline.
     *
     * If you don't use a TweenManager, you may want to call
     * {@link BaseTween#free()} to reuse the object later.
	 */
	public
    boolean isFinished() {
		return isFinished || isKilled;
	}

	/**
	 * Returns true if the tween automatically reverse when complete.
	 */
	public
    boolean canAutoReverse() {
		return canAutoReverse;
	}

	/**
	 * Returns true if the tween or timeline is currently paused.
	 */
	public
    boolean isPaused() {
		return isPaused;
	}

	// -------------------------------------------------------------------------
	// Abstract API
	// -------------------------------------------------------------------------

    protected abstract
    boolean containsTarget(final Object target);

    protected abstract
    boolean containsTarget(final Object target, final int tweenType);

    protected abstract
    void doUpdate(final boolean animationDirection, final int delta);

	// -------------------------------------------------------------------------
	// Protected API
	// -------------------------------------------------------------------------

	protected
    void initializeOverride() {
	}

	@SuppressWarnings("Convert2streamapi")
    protected
    void callCallbacks(final int type) {
        for (int i = 0, n = callbacks.size(); i < n; i++) {
            final TweenCallback callback = callbacks.get(i);
            if ((callback.triggers & type) > 0) {
                callback.onEvent(type, this);
            }
        }
    }

    protected
    void killTarget(final Object target) {
        if (containsTarget(target)) {
            kill();
        }
    }


    protected
    void killTarget(final Object target, final int tweenType) {
        if (containsTarget(target, tweenType)) {
            kill();
        }
    }

    // -------------------------------------------------------------------------
	// Update engine
	// -------------------------------------------------------------------------

    /**
     * Updates the tween or timeline state. <b>You may want to use a
     * TweenManager to update objects for you.</b>
     * <p>
     * Slow motion, fast motion and backward play can be easily achieved by
     * tweaking this delta time.
     * <p>
     * Multiply it by -1 to play the animation backward, or by 0.5
     * to play it twice-as-slow than its normal speed.
     * <p>
     * <p>
     * <b>THIS IS NOT PREFERRED</b>
     *
     * @param delta A delta time in SECONDS between now and the last call.
     */
    public
    void update(final float delta) {
        // from: http://nicolas.limare.net/pro/notes/2014/12/12_arit_speed/
        //    Floating-point operations are always slower than integer ops at same data size.
        // internally we also want to use INTEGER, since we want consistent timelines
        final int deltaMilliSeconds = (int) (delta * 1000F);

        update(deltaMilliSeconds);
    }


    // Manage direction change transition points, so that we can keep track of which direction we are currently stepping
    protected
    void forceRestart(final boolean direction, final int restartAdjustment) {
        // reset to beginning
        isTweenRunning = true;
        isFinished = false;

        if (direction) {
            // reset the currentTime so that we always start at 0
            currentTime = 0;
        }
        else {
            // reset current time to starting position
            currentTime = duration;
        }
    }

    /**
	 * Updates the tween or timeline state. <b>You may want to use a
	 * TweenManager to update objects for you.</b>
	 * <p>
	 * Slow motion, fast motion and backward play can be easily achieved by
	 * tweaking this delta time.
     * <p>
     * Multiply it by -1 to play the animation backward, or by 0.5
     * to play it twice-as-slow than its normal speed.
	 *
	 * @param delta A delta time in MILLI-SECONDS between now and the last call.
	 */
	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    public
    void update(int delta) {
        if (!isStarted || isPaused || isKilled)
            return;

        // the INITIAL, incoming delta from the app, will be positive or negative.
        boolean direction = delta > 0;

        // This is NOT final, because it's possible to change directions
        int originalDelta = delta;


        if (!isInitialized) {
            final int newTime = currentTime + delta;

            // only start running if we have passed the specified delay in the FORWARDS direction. (tweens must always start off forwards)
            if (newTime >= delay) {
                initializeOverride();

                this.isInitialized = true;
                this.isTweenRunning = true;

                cachedDirection = direction;

                // adjust the delta so that it is shifted based on the length of (previous) delay
                delta -= delay - currentTime;

                // reset the currentTime so that we always start at 0
                currentTime = 0;
            }
            else {
                // shortcut out so we don't have to worry about any other checks
                currentTime += delta;
                return;
            }
        }

        final int duration = this.duration;

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
         *                BEGIN                   COMPLETE
         *                START      END
         *                v          v            v
         * |---[DELAY]----[XXXXXXXXXX]->>-[R.DELAY]  ╶╮
         *            ╭╴  [R.DELAY]-<<-[XXXXXXXXXX] <─╯
         *            │   ^            ^          ^
         *            │                bEND       bSTART
         *            │   bCOMPLETE               bBEGIN
         *            │
         *            ╰╴> [XXXXXXXXXX]->>-[R.DELAY]  ╶╮
         *            ╭╴  [R.DELAY]-<<-[XXXXXXXXXX] <─╯
         *            ╰╴> [XXXXXXXXXX]->>-[R.DELAY]  ...
         *
         */

        // forwards always goes from 0 -> duration
        // reverse always goes from duration -> 0
        // canAutoReverse - only present with repeatDelay, and will cause an animation to reverse once iteration + repeatDelay is complete

        /* DELAY: start is INCLUSIVE, end is EXCLUSIVE, meaning:
         * delay = 0-5, the delay is over when time=5
         *
         *    0          <5
         *    v           v
         *    [---DELAY---]
         */


        // first we have to fire all of our events and adjust our state. Once done adjusting state and firing events to our callbacks
        // it will break from this loop. If we are finished, it will run update directly (and then return, instead of breaking from loop)
        if (!this.isFinished) {
            while (true) {
                int newTime = currentTime + delta;

                // are we still allowed to run, are we waiting for a REPEAT delay to complete?
                if (!isTweenRunning) {
                    if (direction) {
                        // {FORWARDS}
                        if (newTime <= 0) {
                            // still inside our repeat delay

                            // adjust our time
                            currentTime = newTime;
                            // break because we have to make sure that our children are updated (to preserve reversing delays/behavior)
                            break;
                        } else {
                            // reset to beginning
                            isTweenRunning = true;
                            currentTime = 0;

                            // have to specify that the children should restart
                            forceRestart(FORWARDS, -repeatDelay);
                        }
                    }
                    else {
                        // {REVERSE}
                        if (newTime >= 0) {
                            // still inside our repeat delay

                            // adjust our time
                            this.currentTime = newTime;
                            // break because we have to make sure that our children are updated (to preserve reversing delays/behavior)
                            break;
                        } else {
                            isTweenRunning = true;
                            currentTime = duration;

                            delta = newTime;
                            newTime = duration+delta; // delta is negative here

                            // have to specify that the children should restart
                            forceRestart(REVERSE, repeatDelay); // have to account for update w/ originalDelta
                        }
                    }
                }


                // TWEEN IS RUNNING.

                if (direction) {
                    // {FORWARDS}

                    // detect when we are BEGIN or START
                    if (currentTime == 0) {
                        if (!isInCycle) {
                            isInCycle = true;
                            callCallbacks(TweenCallback.Events.BEGIN);
                        }

                        callCallbacks(TweenCallback.Events.START);
                    }

                    if (newTime < duration) {
                        // still inside our iteration, done with events.

                        // adjust our time
                        currentTime = newTime;
                        break;
                    }

                    // we have gone past our iteration point

                    // adjust the delta so that it is shifted based on the length of (previous) iteration
                    delta = newTime - duration;

                    // set our currentTime for the callbacks to be accurate and updates to lock to start/end values
                    currentTime = duration;

                    // make sure that we manage our children BEFORE we do anything else!
                    // we use originalDelta here because we have to trickle-down the logic to all children. If we use delta, the incorrect value
                    // will trickle-down
                    doUpdate(FORWARDS, originalDelta);

                    callCallbacks(TweenCallback.Events.END);

                    // flip our state
                    isTweenRunning = !isTweenRunning;

                    ////////////////////////////////////////////
                    ////////////////////////////////////////////
                    // THREE possible outcomes
                    // 1: we are done running completely
                    // 2: we flip to auto-reverse
                    // 3: we are in linear repeat mode
                    if (repeatCount <= 0) {
                        // {FINISHED}
                        // no repeats left, so we're done

                        // really are done (so no more event notification loops)
                        this.isFinished = true;

                        // we're done going forwards
                        isInCycle = false;
                        callCallbacks(TweenCallback.Events.COMPLETE);

                        // now adjust the time so PARENT reversing/etc works
                        this.currentTime = delta;

                        return;
                    }
                    else if (canAutoReverse) {
                        // {AUTO_REVERSE}

                        // we're done going forwards
                        isInCycle = false;
                        callCallbacks(TweenCallback.Events.COMPLETE);

                        // flip direction to {REVERSE}
                        cachedDirection = REVERSE;
                        repeatCount--;

                        // setup delays, if there are any
                        currentTime = repeatDelay;
                        return;
                    } else {
                        // LINEAR

                        repeatCount--;
                        currentTime = -delta;

                        // keeps going forwards this cycle until done!


                    }
                }
                else {
                    // {REVERSE}

                    // detect when we are BEGIN or START
                    if (currentTime == duration) {
                        if (!isInCycle) {
                            isInCycle = true;
                            callCallbacks(TweenCallback.Events.BACK_BEGIN);
                        }

                        callCallbacks(TweenCallback.Events.BACK_START);
                    }

                    if (newTime > 0) {
                        // still inside our iteration, done with events.

                        // adjust our time by our delta value
                        currentTime = newTime;
                        break;
                    }

                    // we have gone past our iteration point

                    // adjust the delta so that it is shifted based on the length of (previous) iteration
                    // this is easy, because the amount of time past 0 is the new delta (because we start at 0).
                    delta += currentTime;

                    // set our currentTime for the callbacks to be accurate
                    currentTime = 0;

                    // make sure that we manage our children BEFORE we do anything else!
                    // we use originalDelta here because we have to trickle-down the logic to all children. If we use delta, the incorrect value
                    // will trickle-down
                    doUpdate(REVERSE,  originalDelta);

                    callCallbacks(TweenCallback.Events.BACK_END);

                    // flip our state
                    isTweenRunning = !isTweenRunning;

                    ////////////////////////////////////////////
                    ////////////////////////////////////////////
                    // THREE possible outcomes
                    // 1: we are done running completely
                    // 2: we flip to auto-reverse
                    // 3: we are in linear repeat mode
                    if (repeatCount <= 0) {
                        // {FINISHED}
                        // no repeats left, so we're done

                        // really are done (so no more event notification loops)
                        this.isFinished = true;

                        // we're done going forwards
                        callCallbacks(TweenCallback.Events.BACK_COMPLETE);
                        isInCycle = false;

                        // now adjust the time so PARENT reversing/etc works
                        this.currentTime = delta;

                        return;
                    }
                    else if (canAutoReverse) {
                        // {AUTO_REVERSE}

                        // we're done going forwards
                        isInCycle = false;
                        callCallbacks(TweenCallback.Events.BACK_COMPLETE);

                        // flip direction to {REVERSE}
                        direction = FORWARDS;
                        cachedDirection = FORWARDS;

                        repeatCount--;
                        delta = -delta;

                        // setup delays, if there are any
                        currentTime = -repeatDelay;
                    } else {
                        // LINEAR
                        // cannot go linear in reverse... nothing to do.
                    }
                }
            }

            // when done with all the adjustments and notifications, update the object
            // we use originalDelta here because we have to trickle-down the logic to all children. If we use delta, the incorrect value
            // will trickle-down
            doUpdate(direction, originalDelta);
        }
        else {
            // the time that a tween/timeline runs OVER (when it is done running), must always have consideration of the
            // direction it was going, WHEN it ran over.
            // this is so reversing still correctly tracks delays, etc
            if (direction) {
                currentTime += originalDelta;
            }
            else {
                currentTime -= originalDelta;
            }
        }
    }

    @Override
    public
    String toString() {
        return this.name + "";
    }
}
