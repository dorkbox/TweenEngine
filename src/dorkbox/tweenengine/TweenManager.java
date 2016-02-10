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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A TweenManager updates all your tweens and timelines at once. Its main interest is that it handles the tween/timeline life-cycles
 * for you, as well as releasing pooled objects.
 * <p/>
 * If you don't use a TweenManager, you must make sure to release the tween objects back to the pool manually {@link BaseTween#free()}
 * <p/>
 * Just give it a bunch of tweens or timelines and call {@link TweenManager#update()} periodically, you don't need to do anything else!
 * Relax and enjoy your animations.
 * <p/>
 * More fine-grained control is available as well via {@link TweenManager#update(float)} to update via seconds (1.0F == 1.0 seconds).
 * <p/>
 * <p/>
 * WARNING: <p/>
 * Individual tweens and timelines are NOT THREAD SAFE. Do not access any part of them outside of the render (or animation) thread.
 * <p/>
 * For object visibility in different threads, use {@link Tween#flushWrite()} inside an endCallback (so the objects are flushed), and
 * then before you access those objects, call {@link Tween#flushRead()} which will then correctly make those objects (which were
 * changed by the tween engine), visible to your thread.
 * <p/>
 * A "heavyweight" synchronization technique is not required in a modern JVM, flushWrite/Read is sufficient
 *
 * @see Tween
 * @see Timeline
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
public
class TweenManager {
    public static final BaseTween[] BASE_TWEENS = new BaseTween[0];
    // -------------------------------------------------------------------------
	// Static API
	// -------------------------------------------------------------------------

	/**
	 * Disables or enables the "auto remove" mode of any tween manager for a particular tween or timeline. This mode is activated by
     * default. The interest of deactivating it is to prevent some tweens or timelines from being automatically removed from a manager
     * once they are finished. Therefore, if you update a manager backwards, the tweens or timelines will be played again, even if they
     * were finished.
	 */
    public static
    void setAutoRemove(final BaseTween<?> tween, final boolean value) {
        tween.isAutoRemoveEnabled = value;
    }

    /**
	 * Disables or enables the "auto start" mode of any tween manager for a particular tween or timeline. This mode is activated
     * by default. If it is not enabled, add a tween or timeline to any manager won't start it automatically, and you'll need to
     * call .start() manually on your object.
	 */
    public static
    void setAutoStart(final BaseTween<?> tween, final boolean value) {
        tween.isAutoStartEnabled = value;
    }

    /**
     * Gets the version number.
     */
    public static
    String getVersion() {
        return "7.0";
    }

    // -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	private final ArrayList<BaseTween<?>> tweenArrayList = new ArrayList<BaseTween<?>>(20);
	private BaseTween<?>[] childrenArray = new BaseTween<?>[0];

	private boolean isPaused = false;

    private UpdateAction startEventCallback = BaseTween.NULL_ACTION;
    private UpdateAction endEventCallback = BaseTween.NULL_ACTION;

    private long lastTime = 0L;

    /**
     * Sets an event handler so that a notification is broadcast when the manager starts updating the current frame of animation.
     *
     * @return The manager, for instruction chaining.
     */
    public
    TweenManager setStartCallback(final UpdateAction<TweenManager> startCallback) {
        if (startCallback == null) {
            throw new RuntimeException("Callback cannot be null! Use BaseTween.NULL_ACTION if you wish to 'unset' the callback");
        }

        this.startEventCallback = startCallback;
        return this;
    }

    /**
     * Sets an event handler so that a notification is broadcast when the manager finishes updating the current frame of animation.
     *
     * @return The manager, for instruction chaining.
     */
    public
    TweenManager setEndCallback(final UpdateAction<TweenManager> endCallback) {
        if (endCallback == null) {
            throw new RuntimeException("Callback cannot be null! Use BaseTween.NULL_ACTION if you wish to 'unset' the callback");
        }

        this.endEventCallback = endCallback;
        return this;
    }

    /**
     * Necessary only if another thread can modify your tweens/timelines. You must call {@link Tween#flushWrite()} in the other thread,
     * so the tween/timelines can be aware of the changes.
     * * </p>
     * This sets the {@link TweenManager#setStartCallback(UpdateAction)}, so if you implement your own, you should call
     * {@link Tween#flushRead()} in your callback implementation.
     * </p>
     * <b>This is only necessary to set on ONE tween/timeline/manager, as all threads and tween objects will be correct after this call
     *    is complete.</b>
     *
     * @return The manager, for instruction chaining.
     */
    public TweenManager syncOnStart() {
        startEventCallback = BaseTween.FLUSH_READ_ACTION;
        return this;
    }

    /**
     * Necessary if another thread will be reading the values set by these tweens/timelines. You must call {@link Tween#flushRead()}
     * in the other thread, before accessing the target object's values.
     * </p>
     * This sets the {@link TweenManager#setEndCallback(UpdateAction)}, so if you implement your own, you should call
     * {@link Tween#flushWrite()} in your callback implementation.
     * </p>
     * <b>This is only necessary to set on ONE tween/timeline/manager, as all threads and tween objects will be correct after this call
     *    is complete.</b>
     *
     * @return The manager, for instruction chaining.
     */
    public TweenManager syncOnEnd() {
        endEventCallback = BaseTween.FLUSH_WRITE_ACTION;
        return this;
    }


	/**
	 * Adds a tween or timeline to the manager and starts or restarts it.
	 *
	 * @return The manager, for instruction chaining.
	 */
	public
    TweenManager add(final BaseTween<?> tween) {
        if (!tweenArrayList.contains(tween)) {
            tweenArrayList.add(tween);
        }

        // setup our children array, so update iterations are faster  (marginal improvement)
        childrenArray =  tweenArrayList.toArray(BASE_TWEENS);

        if (tween.isAutoStartEnabled) {
            tween.start();
        }

        return this;
	}

	/**
	 * Returns true if the manager contains any valid interpolation associated to the given target object.
	 */
	public
    boolean containsTarget(final Object target) {
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            if (tween.containsTarget(target)) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Returns true if the manager contains any valid interpolation associated to the given target object and to the given tween type.
	 */
	public
    boolean containsTarget(final Object target, final int tweenType) {
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            if (tween.containsTarget(target, tweenType)) {
                return true;
            }
        }
		return false;
	}

	/**
	 * Kills every managed tweens and timelines.
	 */
	public
    void killAll() {
        boolean needsRefresh = false;

        final Iterator<BaseTween<?>> iterator = tweenArrayList.iterator();
        while (iterator.hasNext()) {
            final BaseTween<?> tween = iterator.next();
            tween.kill();

            // always kill (if not during an update)
            if (!tween.isDuringUpdate) {
                needsRefresh = true;
                iterator.remove();
                tween.free();
            }
        }

        if (needsRefresh) {
            // setup our children array, so update iterations are faster  (marginal improvement)
            childrenArray = tweenArrayList.toArray(BASE_TWEENS);
        }
    }

    /**
	 * Kills every tweens associated to the given target. Will also kill every timelines containing a tween associated to the given target.
     *
     * @return true if the target was killed, false if we do not contain the target, and it was not killed
	 */
	@SuppressWarnings("Duplicates")
    public
    boolean killTarget(final Object target) {
        boolean needsRefresh = false;

        final Iterator<BaseTween<?>> iterator = tweenArrayList.iterator();
        while (iterator.hasNext()) {
            final BaseTween<?> tween = iterator.next();
            boolean killTarget = tween.killTarget(target);

            // kill if not during an update, and if specified
            if (killTarget && !tween.isDuringUpdate && tween.isFinished()) {
                needsRefresh = true;
                iterator.remove();
                tween.free();
            }
        }

        if (needsRefresh) {
            // setup our children array, so update iterations are faster  (marginal improvement)
            childrenArray = tweenArrayList.toArray(BASE_TWEENS);

            return true;
        }

        return false;
    }

	/**
	 * Kills every tweens associated to the given target and tween type. Will also kill every timelines containing a tween associated
     * to the given target and tween type.
     *
     * @return true if the target was killed, false if we do not contain the target, and it was not killed
	 */
	@SuppressWarnings("Duplicates")
    public
    boolean killTarget(final Object target, final int tweenType) {
        boolean needsRefresh = false;

        final Iterator<BaseTween<?>> iterator = tweenArrayList.iterator();
        while (iterator.hasNext()) {
            final BaseTween<?> tween = iterator.next();
            boolean killTarget = tween.killTarget(target, tweenType);

            // kill if not during an update, and if specified
            if (killTarget && !tween.isDuringUpdate && tween.isFinished()) {
                needsRefresh = true;
                iterator.remove();
                tween.free();
            }
        }

        if (needsRefresh) {
            // setup our children array, so update iterations are faster  (marginal improvement)
            childrenArray =  tweenArrayList.toArray(BASE_TWEENS);

            return true;
        }

        return false;
    }

	/**
	 * Increases the minimum capacity of the manager. Defaults to 20.
	 */
	public void ensureCapacity(final int minCapacity) {
        tweenArrayList.ensureCapacity(minCapacity);
	}

	/**
	 * Pauses the manager. Further update calls won't have any effect.
	 */
    public
    void pause() {
		isPaused = true;
	}

	/**
	 * Resumes the manager, if paused.
	 */
	public
    void resume() {
		isPaused = false;
	}

    /**
     * Resets the last time this tweenManager had "update" called. This is useful when the timer (that {@link TweenManager#update()}) is
     * usually called on, has been stopped for a while. This prevents the "first" update call to 'snap' to the target values because the
     * time delta update was so large.
     */
    public
    void resetUpdateTime() {
        this.lastTime = System.nanoTime();
    }

    /**
     * Updates every added tween/timeline based upon the elapsed time between now and the previous time this method was called. This
     * method also handles the tween life-cycles automatically.
     * <p/>
     * If a tween is finished, it will be removed from the manager.
     */
    public
    void update() {
        update(System.nanoTime());
    }

    /**
     * Updates every added tween with a new time in NANO-SECONDS and handles the tween life-cycles automatically.
     * <p/>
     * If a tween is finished, it will be removed from the manager.
     */
    public
    void update(long newTimeInNanos) {
        final float deltaTime = (newTimeInNanos - lastTime) / 1.0E9F;
        this.lastTime = newTimeInNanos;

        update(deltaTime);
    }

    /**
     * Updates every added tween with a delta time in SECONDS and handles the tween life-cycles automatically.
     * <p/>
     * If a tween is finished, it will be removed from the manager.
     * <p/>
     * The delta time represents the elapsed time in seconds between now and the previous update call. Each tween or timeline manages its
     * local time, and adds this delta to its local time to update itself.
     * <p/>
     * Slow motion, fast motion and backward play can be easily achieved by tweaking this delta time. Multiply it by -1 to play the
     * animation backward, or by 0.5 to play it twice slower than its normal speed.
     *
     * @param delta A delta time in SECONDS between now and the previous call.
     */
    @SuppressWarnings("unchecked")
    public
    void update(final float delta) {
        if (!isPaused) {
            // on start sync
            startEventCallback.onEvent(this);

            for (int i = 0, n = childrenArray.length; i < n; i++) {
                BaseTween<?> tween = childrenArray[i];
                tween.update(delta);
            }

            boolean needsRefresh = false;

            for (int i = childrenArray.length - 1; i >= 0; i--) {
                final BaseTween<?> tween = childrenArray[i];
                if (tween.isAutoRemoveEnabled && tween.isFinished()) {
                    // guarantee the tween/timeline values are set at the end
                    tween.setValues(true, false);

                    needsRefresh = true;
                    tweenArrayList.remove(i);
                    tween.free();
                }
            }

            if (needsRefresh) {
                // setup our children array, so update iterations are faster  (marginal improvement)
                childrenArray = tweenArrayList.toArray(BASE_TWEENS);
            }

            // on finish sync
            endEventCallback.onEvent(this);
        }
    }

    /**
	 * Gets the number of managed objects. An object may be a tween or a timeline. A timeline only counts for 1 object, since it manages
     * its children itself.
	 * <p/>
	 * To get the count of running tweens, see {@link #getRunningTweensCount()}.
	 */
	public
    int size() {
        return childrenArray.length;
	}

	/**
	 * Gets the number of running tweens. This number includes the tweens located inside timelines (and nested timelines).
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    int getRunningTweensCount() {
        return getTweensCount(tweenArrayList);
	}

	/**
	 * Gets the number of running timelines. This number includes the timelines nested inside other timelines.
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    int getRunningTimelinesCount() {
        return getTimelinesCount(tweenArrayList);
    }

    /**
	 * Gets an immutable list of every managed object.
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    List<BaseTween<?>> getObjects() {
		return Collections.unmodifiableList(tweenArrayList);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

    private static
    int getTweensCount(final List<BaseTween<?>> objs) {
        int count = 0;
        for (int i = 0, n = objs.size(); i < n; i++) {
            final BaseTween<?> obj = objs.get(i);
            if (obj instanceof Tween) {
                count += 1;
            }
            else {
                count += getTweensCount(((Timeline) obj).getChildren());
            }
        }
        return count;
    }

    private static
    int getTimelinesCount(final List<BaseTween<?>> objs) {
        int count = 0;
        for (int i = 0, n = objs.size(); i < n; i++) {
            final BaseTween<?> obj = objs.get(i);
            if (obj instanceof Timeline) {
                count += 1 + getTimelinesCount(((Timeline) obj).getChildren());
            }
        }
        return count;
    }
}
