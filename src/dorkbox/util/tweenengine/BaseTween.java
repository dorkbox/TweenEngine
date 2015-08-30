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
    protected boolean forceRestart; // necessary for a parent timeline to tell a tween to restart because of direction change

	// Timings
	protected float delay;  // this is the delay at the start of a timeline/tween. Is reapplied on repeats & change direction
	protected float duration; // doesn't change while running

	private float repeatDelay;

    // represents the amount of time spent in the current iteration or delay
    // package local because our timeline has to be able to adjust for delays when switching to reverse
    protected float currentTime;

    // Direction state
    protected static final boolean FORWARDS = true;
    private static final boolean REVERSE = false;
    private boolean cachedDirection;

	private boolean isStarted; // TRUE when the object is started
	private boolean isInitialized; // TRUE after the delay
	private boolean isKilled; // TRUE if kill() was called
	private boolean isPaused; // TRUE if pause() was called

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
        forceRestart = false;
        repeatCount = repeatCountOrig = 0;
        isTweenRunning = canAutoReverse = false;
        cachedDirection = FORWARDS;

        delay = duration = repeatDelay = currentTime = 0;
        isStarted = isInitialized = isFinished = isKilled = isPaused = false;

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
	 * @param delay A duration in seconds, for example .2F for 200 milliseconds.
     *
	 * @return The current object, for chaining instructions.
	 */
    @SuppressWarnings("unchecked")
	public
    T delay(final float delay) {
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
	 * @param delay A delay between each iteration.
     *
	 * @return The current tween or timeline, for chaining instructions.
	 */
    @SuppressWarnings("unchecked")
	public
    T repeat(final int count, final float delay) {
        if (isStarted) {
            throw new RuntimeException("You can't change the repetitions of a tween or timeline once it is started");
        }
        repeatCount = count;
        repeatCountOrig = count;
        repeatDelay = delay >= 0F ? delay : 0F;
        canAutoReverse = false;
        return (T) this;
	}

	/**
	 * Repeats the tween or timeline for a given number of times.
	 * Once an iteration is complete, it will be played in reverse.
	 *
	 * @param count The number of repetitions. For infinite repetition,
	 *              use {@link Tween#INFINITY} or -1.
	 * @param delay A delay before each repetition.
     *
	 * @return The current tween or timeline, for chaining instructions.
	 */
    @SuppressWarnings("unchecked")
	public
    T repeatAutoReverse(final int count, final float delay) {
        if (isStarted) {
            throw new RuntimeException("You can't change the repetitions of a tween or timeline once it is started");
        }
        repeatCount = count;
        repeatCountOrig = count;
        repeatDelay = delay >= 0F ? delay : 0F;
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
    float getCurrentTime() {
        return currentTime;
    }

	/**
	 * Gets the delay of the tween or timeline. Nothing will happen before
	 * this delay.
	 */
	public
    float getDelay() {
		return delay;
	}

	/**
	 * Gets the duration of a single iteration.
	 */
	public
    float getDuration() {
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
    float getFullDuration() {
        if (repeatCountOrig < 0) {
            return -1;
        }
        return delay + duration + (repeatDelay + duration) * repeatCountOrig;
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
    float getRepeatDelay() {
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
     * Reverse direction can be impacted by a negative value for {@link #update(float)},
     * or via a tween reversing direction because of {@link #repeatAutoReverse(int, float)}
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
    void forceStartValues();

    protected abstract
    void forceEndValues();

    protected abstract
    void forceEndValues(final float time);

    protected abstract
    boolean containsTarget(final Object target);

    protected abstract
    boolean containsTarget(final Object target, final int tweenType);

    protected abstract
    void doUpdate(final boolean animationDirection, final boolean changedDirection, final float restartAdjustment, float delta);

	// -------------------------------------------------------------------------
	// Protected API
	// -------------------------------------------------------------------------

	protected
    void initializeOverride() {
	}

    /**
     * Force tween to start values and adjust time to point of tween start
     */
    protected
    void forceToStart() {
        currentTime = 0;

        if (cachedDirection) {
            // {FORWARDS}
            forceStartValues();
        }
        else {
            forceEndValues();
        }
    }

    /**
     * Force tween to end values and adjust time to point of tween end (ignoring delay/r.delay)
     * @param time The end-point time, so that reverse delays properly work
     */
	protected
    void forceToEnd(final float time) {
        currentTime = time;

        if (cachedDirection) {
            // {FORWARDS}
            forceEndValues();
        }
        else {
            forceStartValues();
        }
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
	 *
	 * @param delta A delta time between now and the last call.
	 */
	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    public
    void update(float delta) {
        if (!isStarted || isPaused || isKilled)
            return;

        // the INITIAL, incoming delta from the app, will be positive or negative.
        boolean direction = delta > 0F;

        // This is NOT final, because it's possible to change directions
        float originalDelta = delta;


        /* DELAY start is INCLUSIVE, the end is EXCLUSIVE, meaning:
         * delay = 0-5, the delay is over when time=5
         *
         *    0          <5
         *    v           v
         *    [---DELAY---]
         */


        boolean restart = false;
        float restartAdjustment = 0F;
        final boolean isInitialized = this.isInitialized;
        if (!isInitialized) {
            final float newTime = currentTime + delta;

            // only start running if we have passed the specified delay in the FORWARDS direction. (tweens must always start off forwards)
            if (newTime >= delay) {
                initializeOverride();

                this.forceRestart = false;
                this.isInitialized = true;
                this.isTweenRunning = true;

                cachedDirection = direction;

                restart = true;  // to trigger a BEGIN state callback

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


        // Manage direction change transition points, so that we can keep track of which direction we are currently stepping
        if (forceRestart) {
            float currentTime = this.currentTime;
            final float newTime = currentTime + delta;

            // Specified via the delta which direction our reset affects
            if (direction) {
                if (newTime >= 0) {
                    // adjust the delta so that it is shifted
                    delta += this.currentTime;

                    // reset the currentTime so that we always start at 0
                    this.currentTime = 0;
                }
                else {
                    // shortcut out so we don't have to worry about any other checks
                    this.currentTime += delta;
                    return;
                }
            }
            else {
                if (newTime <= 0) {
                    // adjust the delta so that it is shifted
                    delta = newTime;

                    // reset the currentTime so that we always start where it should
                    this.currentTime = duration;
                }
                else {
                    // shortcut out so we don't have to worry about any other checks
                    this.currentTime += delta;
                    return;
                }
            }

            // we successfully waited for the delay
            // this is here, instead of inside each block above, to cut down on repeated code

            // if we change directions, we have to reset our state, because clearly we are now going in the opposite direction (as specified
            // by the negative delta
            forceRestart = false;
            isFinished = false;
            isTweenRunning = true;

            restart = true;  // to trigger a BEGIN state callback
        }


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

        // first we have to fire all of our events and adjust our state. Once done adjusting state and firing events to our callbacks
        // it will break from this loop. If we are finished, it will run update directly (and then return, instead of breaking from loop)
        if (!this.isFinished) {
            while (true) {
                final float newTime = currentTime + delta;

                if (direction) {
                    // {FORWARDS}

                    if (isTweenRunning) {
                        // {FORWARDS} [ITERATION CHECK]

                        // detect when we are BEGIN or START
                        if (currentTime == 0F) {
                            if (restart) {
                                callCallbacks(TweenCallback.Events.BEGIN);

                                if (!isInitialized) {
                                    // at init, don't pass this along (it complicates logic later on)
                                    restart = false;
                                }
                            }

                            callCallbacks(TweenCallback.Events.START);
                        }

                        if (newTime <= duration) {
                            // still inside our iteration, done with events.

                            // adjust our time by our original delta value
                            currentTime += delta;
                            break;
                        }


                        // we have gone past our iteration point

                        // adjust the delta so that it is shifted based on the length of (previous) iteration
                        delta -= duration - currentTime;

                        // set our currentTime for the callbacks to be accurate
                        currentTime = duration;

                        callCallbacks(TweenCallback.Events.END);

                        // now set our currentTime to start measuring the delay/iteration (both start at 0)
                        currentTime = 0F;

                        // flip our state
                        isTweenRunning = !isTweenRunning;

                        // we loop to continue into [DELAY CHECK], where it will determine what to do next
                    }
                    else {
                        // {FORWARDS} [DELAY CHECK]

                        // tween is NOT running, we are in:
                        //  - COMPLETE state
                        //  - DELAY state

                        if (repeatCount > 0) {
                            // we can repeat OR autoReverse

                            if (newTime <= repeatDelay) {
                                // still inside our repeat delay, done with events.

                                // adjust our time by our original delta value
                                currentTime += delta;
                                break;
                            }

                            // we are done waiting through the delay, we can progress to adjusting for iteration
                            repeatCount--;

                            // adjust the delta so that it is shifted based on the length of (previous) repeat delay
                            delta -= repeatDelay - currentTime;

                            // flip our state
                            isTweenRunning = !isTweenRunning;

                            // specify that our children will need to restart, if necessary
                            restart = true;

                            // we can only autoReverse when in combination with repeatCount.
                            if (canAutoReverse) {
                                restartAdjustment = repeatDelay;

                                currentTime = duration + restartAdjustment;
                                callCallbacks(TweenCallback.Events.COMPLETE);

                                // setup state correctly for reverse direction iteration -- delay happens after iteration, and don't
                                // want to "double-dip" our delay time
                                currentTime = duration;

                                // flip direction to {REVERSE}

                                direction = REVERSE;
                                cachedDirection = REVERSE;

                                delta = -delta;
                                originalDelta = -originalDelta;
                            } else {
                                // restartAdjustment = 0F;

                                // now set our currentTime to start measuring the next repeat iteration
                                currentTime = 0F;
                            }

                            // we loop to continue into [ITERATION CHECK]
                        }
                        else {
                            // {FINISHED}
                            // no repeats left, so we're done

                            // really are done (so no more event notification loops)
                            this.isFinished = true;

                            // save, because we have to continue counting this value, so reversing/etc still correctly tracks
                            final float currentTime = this.currentTime;

                            // force it to the end value
                            this.currentTime = duration;

                            // make sure that we manage our children BEFORE we mark as complete!
                            // we use originalDelta here because we have to trickle-down the logic to all children. If we use delta, the incorrect value
                            // will trickle-down
                            doUpdate(FORWARDS, restart, restartAdjustment, originalDelta);

                            // when AUTO-REVERSE, COMPLETE is at the end of each cycle, or if we are out of repeats.
                            if (!canAutoReverse || repeatCount <= 0) {
                                // we're done going forwards
                                callCallbacks(TweenCallback.Events.COMPLETE);
                            }

                            // now restore the time
                            this.currentTime = currentTime;

                            return;
                        }
                    }

                    // end {FORWARDS}
                }
                else {
                    // {REVERSE}

                    // in reverse, all of the checks are OPPOSITE

                    if (isTweenRunning) {
                        // {REVERSE} [ITERATION CHECK]

                        // detect when we are BEGIN or START
                        if (currentTime == duration) {
                            if (restart) {
                                callCallbacks(TweenCallback.Events.BACK_BEGIN);
                            }

                            callCallbacks(TweenCallback.Events.BACK_START);
                        }

                        if (newTime >= 0F) {
                            // still inside our iteration, done with events.

                            // adjust our time by our original delta value
                            currentTime += delta;
                            break;
                        }


                        // we have gone past our iteration point

                        // adjust the delta so that it is shifted based on the length of (previous) iteration
                        // this is easy, because the amount of time past 0 is the new delta (because we start at 0).
                        delta += currentTime;

                        // set our currentTime for the callbacks to be accurate
                        currentTime = 0F;

                        callCallbacks(TweenCallback.Events.BACK_END);

                        // set our currentTime for the callbacks to be accurate
                        if (repeatCount > 0) {
                            currentTime = repeatDelay;
                        }
                        else {
                            currentTime = duration;
                        }

                        // flip our state
                        isTweenRunning = !isTweenRunning;

                        // we loop to continue into [DELAY CHECK], where it will determine what to do next
                    }
                    else {
                        // {REVERSE} [DELAY CHECK]

                        // tween is NOT running, we are in:
                        //  - COMPLETE state
                        //  - DELAY state

                        if (repeatCount > 0) {
                            // we can repeat (and maybe autoReverse)

                            if (newTime >= 0F) {
                                // still inside our repeat delay, done with events.

                                // adjust our time by our original delta value
                                currentTime += delta;
                                break;
                            }

                            // we are done waiting through the delay, we can progress to adjusting for iteration
                            repeatCount--;

                            // adjust the delta so that it is shifted based on the length of (previous) iteration
                            // this is easy, because the amount of time past 0 is the new delta (because we start at 0).
                            delta += currentTime;

                            // flip our state
                            isTweenRunning = !isTweenRunning;

                            // specify that our children will need to restart, if necessary
                            restart = true;

                            // we can only autoReverse when in combination with repeatCount.
                            if (canAutoReverse) {
                                restartAdjustment = repeatDelay;

                                // setup state correctly for reverse direction iteration -- delay happens after iteration, and don't
                                // want to "double-dip" our delay time
                                currentTime = 0F;
                                callCallbacks(TweenCallback.Events.BACK_COMPLETE);

                                // flip direction to {FORWARDS}
                                direction = FORWARDS;
                                cachedDirection = FORWARDS;

                                delta = -delta;
                                originalDelta = -originalDelta;
                            } else {
                                restartAdjustment = 0F;

                                // now set our currentTime to start measuring the iteration
                                currentTime = 0F;
                            }

                            // we loop to continue into [ITERATION CHECK]
                        }
                        else {
                            // {FINISHED}
                            // no repeats left, so we're done

                            // really are done (so no more event notification loops)
                            this.isFinished = true;

                            // force it to the end value (don't save old value, it's not necessary)
                            this.currentTime = 0F;

                            // make sure that we manage our children BEFORE we mark as complete!
                            // we use originalDelta here because we have to trickle-down the logic to all children. If we use delta, the incorrect value
                            // will trickle-down
                            doUpdate(REVERSE, restart, restartAdjustment, originalDelta);

                            // we're done going reverse
                            callCallbacks(TweenCallback.Events.BACK_COMPLETE);

                            return;
                        }
                    }

                    // end {REVERSE}
                }
            }

            // when done with all the adjustments and notifications, update the object
            // we use originalDelta here because we have to trickle-down the logic to all children. If we use delta, the incorrect value
            // will trickle-down
            doUpdate(direction, restart, restartAdjustment, originalDelta);
        }
        else {
            // otherwise, we don't run (since we've finished running)
            // -- however we still have to keep track of currentTime, so reversing/etc still correctly tracks delays, etc
            currentTime += originalDelta;
        }
    }

    @Override
    public
    String toString() {
        return this.name + "";
    }
}
