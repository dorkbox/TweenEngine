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
	private int step;
	private int repeatCount;
	private boolean isIterationStep;
	private boolean isAutoReverse;

	// Timings
	protected float delay;
	protected float duration;
	private float repeatDelay;
	private float currentTime;
	private float deltaTime;

	private boolean isStarted; // true when the object is started
	private boolean isInitialized; // true after the delay
	private boolean isFinished; // true when all repetitions are done
	private boolean isKilled; // true if kill() was called
	private boolean isPaused; // true if pause() was called

    private boolean snapToEndpoints;

	// Misc
	private List<TweenCallback> callbacks = new ArrayList<TweenCallback>();
	private Object userData;

	// Package access
	boolean isAutoRemoveEnabled;
	boolean isAutoStartEnabled;

	// -------------------------------------------------------------------------

	protected
    void reset() {
        step = -2;
        repeatCount = 0;
        isIterationStep = isAutoReverse = false;

        delay = duration = repeatDelay = currentTime = deltaTime = 0;
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
	 * later reuse. Note that if you use a {@link TweenManager}, this method
	 * is automatically called once the animation is finished.
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
        repeatDelay = delay >= 0 ? delay : 0;
        isAutoReverse = false;
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
        repeatDelay = delay >= 0 ? delay : 0;
        isAutoReverse = true;
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
     * Specifies for the update function, when an endpoint is reached, to force
     * the value of the target to the start (or end) values. This is to guarantee
     * that the values at the start/end of a tween are as defined, instead of
     * calculated
     */
    @SuppressWarnings("unchecked")
    public
    T snapToEndpoints() {
        this.snapToEndpoints = true;
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
	 * Gets the number of iterations that will be played.
	 */
	public
    int getRepeatCount() {
		return repeatCount;
	}

	/**
	 * Gets the delay occurring between two iterations.
	 */
	public
    float getRepeatDelay() {
		return repeatDelay;
	}

	/**
	 * Returns the complete duration, including initial delay and repetitions.
	 * The formula is as follows:
	 * <pre>
	 * fullDuration = delay + duration + (repeatDelay + duration) * repeatCount
	 * </pre>
	 */
	public
    float getFullDuration() {
        if (repeatCount < 0) {
            return -1;
        }
        return delay + duration + (repeatDelay + duration) * repeatCount;
    }

	/**
	 * Gets the attached data, or null if none.
	 */
	public
    Object getUserData() {
		return userData;
	}

	/**
	 * Gets the id of the current step. Values are as follows:<br/>
	 * <ul>
	 * <li>even numbers mean that an iteration is playing,<br/>
	 * <li>odd numbers mean that we are between two iterations,<br/>
	 * <li>-2 means that the initial delay has not ended,<br/>
	 * <li>-1 means that we are before the first iteration,<br/>
	 * <li>repeatCount*2 + 1 means that we are after the last iteration
	 */
	public
    int getStep() {
		return step;
	}

	/**
	 * Gets the local time.
	 */
	public
    float getCurrentTime() {
		return currentTime;
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
	 * its end or has been killed). If you don't use a TweenManager, you may
	 * want to call {@link BaseTween#free()} to reuse the object later.
	 */
	public
    boolean isFinished() {
		return isFinished || isKilled;
	}

	/**
	 * Returns true if the tween automatically reverse when complete.
	 */
	public
    boolean isAutoReverse() {
		return isAutoReverse;
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
    boolean containsTarget(final Object target);

    protected abstract
    boolean containsTarget(final Object target, final int tweenType);

	// -------------------------------------------------------------------------
	// Protected API
	// -------------------------------------------------------------------------

	protected
    void initializeOverride() {
	}

	protected
    void updateOverride(final int step, final int lastStep, final boolean isIterationStep, final float delta) {
	}

	protected
    void forceToStart() {
        currentTime = -delay;
        step = -1;
        isIterationStep = false;

        if (isStepAutoReverse(0)) {
            forceEndValues();
        }
        else {
            forceStartValues();
        }
    }

	protected
    void forceToEnd(final float time) {
        currentTime = time - getFullDuration();
        int count = repeatCount << 1;

        step = count + 1;
        isIterationStep = false;

        if (isStepAutoReverse(count)) {
            forceStartValues();
        }
        else {
            forceEndValues();
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

    /**
     * @return true if the step is in the middle of the reverse iteration
     */
    public final
    boolean isStepAutoReverse(final int step) {
        return isAutoReverse && Math.abs(step % 4) == 2;
    }

    /**
     * @return true if the step is in the middle of an iteration. See {@link BaseTween#getStep()}
     */
    public final
    boolean isInIteration(final int step) {
        return Math.abs(step % 2) == 0;
    }

    protected
    boolean isValid(final int step) {
        return repeatCount < 0 || (step >= 0 && step <= repeatCount << 1);
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
	 *
	 * Slow motion, fast motion and backward play can be easily achieved by
	 * tweaking this delta time. Multiply it by -1 to play the animation
	 * backward, or by 0.5 to play it twice slower than its normal speed.
	 *
	 * @param delta A delta time between now and the last call.
	 */
	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    public
    void update(final float delta) {
		if (!isStarted || isPaused || isKilled) return;

		deltaTime = delta;

		if (!isInitialized) {
			initialize();
		}

		if (isInitialized) {
			relaunch();
			updateStep();

            // calculate if our timeline/tween is finished
            isFinished = repeatCount >= 0 && (step < 0 || step > repeatCount << 1);
		}

		currentTime += deltaTime;
		deltaTime = 0;
	}

	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    private
    void initialize() {
        final float time = currentTime + deltaTime;
        if (time >= delay) {
            initializeOverride();
            isInitialized = true;
            isIterationStep = true;
            step = 0;
            deltaTime -= delay - currentTime;
            currentTime = 0;

            callCallbacks(TweenCallback.Events.BEGIN);
            callCallbacks(TweenCallback.Events.START);
        }
    }

	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    private
    void relaunch() {
        // restart an iteration when it used to be in reverse
		if (!isIterationStep && repeatCount >= 0 ) {
            final float time = currentTime + deltaTime;

            if (step < 0 && time >= 0) {
                assert step == -1;

                isIterationStep = true;
                step = 0;
                float delta = 0 - currentTime;
                deltaTime -= delta;
                currentTime = 0;

                callCallbacks(TweenCallback.Events.BEGIN);
                callCallbacks(TweenCallback.Events.START);
                updateOverride(step, step - 1, isIterationStep, delta);
            }
            else {
                int count = repeatCount << 1;
                if (step > count && time < 0) {
                    assert step == count + 1;
                    isIterationStep = true;
                    step = count;
                    float delta = 0 - currentTime;
                    deltaTime -= delta;
                    currentTime = duration;

                    callCallbacks(TweenCallback.Events.BACK_BEGIN);
                    callCallbacks(TweenCallback.Events.BACK_START);
                    updateOverride(step, step + 1, isIterationStep, delta);
                }
            }
        }
	}

	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    private
    void updateStep() {
        while (isValid(step)) {
            final float time = currentTime + deltaTime;

            if (!isIterationStep) {
                if (time <= 0) {
                    // start REVERSE
                    isIterationStep = true;
                    step -= 1;

                    float delta = 0 - currentTime;
                    deltaTime -= delta;
                    currentTime = duration;

                    if (isStepAutoReverse(step)) {
                        forceStartValues();
                    }
                    else {
                        forceEndValues();
                    }

                    callCallbacks(TweenCallback.Events.BACK_START);
                    updateOverride(step, step + 1, isIterationStep, delta);
                }
                else if (time >= repeatDelay) {
                    // start FORWARDS
                    isIterationStep = true;
                    step += 1;

                    float delta = repeatDelay - currentTime;
                    deltaTime -= delta;
                    currentTime = 0;

                    if (isStepAutoReverse(step)) {
                        forceEndValues();
                    }
                    else {
                        forceStartValues();
                    }

//                    if (snapToEndpoints) {
//                        forceStartValues();
//                    } else {
//                        updateOverride(step, step, isIterationStep, delta);
//                    }

                    callCallbacks(TweenCallback.Events.START);
                    updateOverride(step, step - 1, isIterationStep, delta);
                } else {
                    // no transition change, just regular updates
                    float delta = deltaTime;
                    deltaTime -= delta;
                    currentTime += delta;

                    break;
                }
            }
            else {
                // isIterationStep = true
                if (time < 0) {
                    // finish REVERSE
                    isIterationStep = false;
                    step -= 1;

                    float delta = 0 - currentTime;
                    deltaTime -= delta;
                    currentTime = 0;

//                    if (snapToEndpoints) {
                        forceStartValues();
//                    } else {
//                        updateOverride(step, step, isIterationStep, delta);
//                    }

                    callCallbacks(TweenCallback.Events.BACK_END);

                    if (step < 0 && repeatCount >= 0) {
                        callCallbacks(TweenCallback.Events.BACK_COMPLETE);
                    }
                    else {
                        currentTime = repeatDelay;
                    }
                }
                else if (time > duration) {
                    // finish FORWARDS
                    isIterationStep = false;
                    step += 1;

                    float delta = duration - currentTime;
                    deltaTime -= delta;
                    currentTime = duration;

                    updateOverride(step, step - 1, isIterationStep, delta);
//                    forceEndValues();

                    callCallbacks(TweenCallback.Events.END);

                    if (step > repeatCount << 1 && repeatCount >= 0) {
                        callCallbacks(TweenCallback.Events.COMPLETE);
                    }
                    currentTime = 0;
                }
                else {
                    // no transition change, just regular updates
                    float delta = deltaTime;
                    deltaTime -= delta;
                    currentTime += delta;

                    updateOverride(step, step, isIterationStep, delta);

                    break;
                }
            }
        }
    }
}
