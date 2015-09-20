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
import java.util.Collections;
import java.util.List;

/**
 * A TweenManager updates all your tweens and timelines at once.
 * Its main interest is that it handles the tween/timeline life-cycles for you,
 * as well as releasing pooled objects.
 * <p/>
 *
 * If you don't use a TweenManager, you must make sure to release the tween
 * objects back to the pool manually {@link BaseTween#free()}
 * <p/>
 *
 * Just give it a bunch of tweens or timelines and call update() periodically,
 * you don't need to care for anything else! Relax and enjoy your animations.
 *
 * @see Tween
 * @see Timeline
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
@SuppressWarnings("unused")
public
class TweenManager {
    // -------------------------------------------------------------------------
	// Static API
	// -------------------------------------------------------------------------

	/**
	 * Disables or enables the "auto remove" mode of any tween manager for a
	 * particular tween or timeline. This mode is activated by default. The
	 * interest of deactivating it is to prevent some tweens or timelines from
	 * being automatically removed from a manager once they are finished.
	 * Therefore, if you update a manager backwards, the tweens or timelines
	 * will be played again, even if they were finished.
	 */
    public static
    void setAutoRemove(final BaseTween<?> tween, final boolean value) {
        tween.isAutoRemoveEnabled = value;
        tween.flushWrite();
    }

    /**
	 * Disables or enables the "auto start" mode of any tween manager for a
	 * particular tween or timeline. This mode is activated by default. If it
	 * is not enabled, add a tween or timeline to any manager won't start it
	 * automatically, and you'll need to call .start() manually on your object.
	 */
    public static
    void setAutoStart(final BaseTween<?> tween, final boolean value) {
        tween.isAutoStartEnabled = value;
        tween.flushWrite();
    }

    // -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

    private volatile long lightSyncObject = System.currentTimeMillis();

	private final ArrayList<BaseTween<?>> tweenArrayList = new ArrayList<BaseTween<?>>(20);
	private BaseTween<?>[] childrenArray = new BaseTween<?>[0];

	private boolean isPaused = false;

    private UpdateAction startEvent = BaseTween.NULL_ACTION;
    private UpdateAction endEvent = BaseTween.NULL_ACTION;


    /**
     * Sets an event handler so that a notification is broadcast when the manager starts updating the current frame of animation.
     */
    public
    void setOnStartEvent(final UpdateAction<TweenManager> startEvent) {
        this.startEvent = startEvent;
        flushWrite();
    }

    /**
     * Sets an event handler so that a notification is broadcast when the manager finishes updating the current frame of animation.
     */
    public
    void setOnEndEvent(final UpdateAction<TweenManager> endEvent) {
        this.endEvent = endEvent;
        flushWrite();
    }

    /**
     * Flushes the visibility of all tween fields from the cache for access/use from different threads.
     * <p>
     * This does not block and does not prevent race conditions.
     *
     * @return the last time (in millis) that the field modifications were flushed
     */
    public final long flushRead() {
        return lightSyncObject;
    }

    /**
     * Flushes the visibility of all tween field modifications from the cache for access/use from different threads.
     * <p>
     * This does not block and does not prevent race conditions.
     */
    public final void flushWrite() {
        lightSyncObject = System.currentTimeMillis();
    }

	/**
	 * Adds a tween or timeline to the manager and starts or restarts it.
	 *
	 * @return The manager, for instruction chaining.
	 */
	public
    TweenManager add(final BaseTween<?> tween) {
        flushRead();
        if (!tweenArrayList.contains(tween)) {
            tweenArrayList.add(tween);
        }

        // setup our children array, so update iterations are faster  (marginal improvement)
        childrenArray = new BaseTween[tweenArrayList.size()];
        tweenArrayList.toArray(childrenArray);

        flushWrite();
        if (tween.isAutoStartEnabled) {
            tween.start();
        }
        return this;
	}

	/**
	 * Returns true if the manager contains any valid interpolation associated
	 * to the given target object.
	 */
	public
    boolean containsTarget(final Object target) {
        flushRead();
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            if (tween.containsTarget(target)) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Returns true if the manager contains any valid interpolation associated
	 * to the given target object and to the given tween type.
	 */
	public
    boolean containsTarget(final Object target, final int tweenType) {
        flushRead();
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
        flushRead();
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            tween.kill();
        }
    }

	/**
	 * Kills every tweens associated to the given target. Will also kill every
	 * timelines containing a tween associated to the given target.
	 */
	public
    void killTarget(final Object target) {
        flushRead();
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            tween.killTarget(target);
        }
    }

	/**
	 * Kills every tweens associated to the given target and tween type. Will
	 * also kill every timelines containing a tween associated to the given
	 * target and tween type.
	 */
	public
    void killTarget(final Object target, final int tweenType) {
        flushRead();
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            tween.killTarget(target, tweenType);
        }
    }

	/**
	 * Increases the minimum capacity of the manager. Defaults to 20.
	 */
	public void ensureCapacity(final int minCapacity) {
        flushRead();
        tweenArrayList.ensureCapacity(minCapacity);
        flushWrite();
	}

	/**
	 * Pauses the manager. Further update calls won't have any effect.
	 */
    public
    void pause() {
		isPaused = true;
        flushWrite();
	}

	/**
	 * Resumes the manager, if paused.
	 */
	public
    void resume() {
		isPaused = false;
        flushWrite();
	}

	/**
	 * Updates every tweens with a delta time in SECONDS and handles the
     * tween life-cycles automatically.
     * <p/>
     * If a tween is finished, it will be removed from the manager.
     * <p/>
     * The delta time represents the elapsed time in SECONDS between now and the
	 * last update call. Each tween or timeline manages its local time, and adds
	 * this delta to its local time to update itself.
	 * <p/>
	 *
	 * Slow motion, fast motion and backward play can be easily achieved by
	 * tweaking this delta time. Multiply it by -1 to play the animation
	 * backward, or by 0.5 to play it twice slower than its normal speed.
     * <p>
     * <p>
     * <b>THIS IS NOT PREFERRED</b>
	 */
	public
    void update(final float delta) {
        // from: http://nicolas.limare.net/pro/notes/2014/12/12_arit_speed/
        //       https://software.intel.com/en-us/forums/watercooler-catchall/topic/306267
        // Floating-point operations are always slower than integer ops at same data size.
        // internally we use INTEGER, since we want consistent timelines & events, as floats will drift (they are approximations)
        final int deltaMSeconds = (int) (delta * 1000F);
        update(deltaMSeconds);
    }

    /**
     * Updates every tweens with a delta time in MILLI-SECONDS and handles the
     * tween life-cycles automatically.
     * <p/>
     * If a tween is finished, it will be removed from the manager.
     * <p/>
     * The delta time represents the elapsed time in MILLI-SECONDS between now and the
     * last update call. Each tween or timeline manages its local time, and adds
     * this delta to its local time to update itself.
     * <p/>
     *
     * Slow motion, fast motion and backward play can be easily achieved by
     * tweaking this delta time. Multiply it by -1 to play the animation
     * backward, or by 0.5 to play it twice slower than its normal speed.
     *
     * @param elapsedMillis A delta time in MILLI-SECONDS between now and the last call.
     */
    @SuppressWarnings("unchecked")
    public
    void update(final int elapsedMillis) {
        flushRead();
        boolean needsRefresh = false;
        for (int i = childrenArray.length - 1; i >= 0; i--) {
            final BaseTween<?> obj = childrenArray[i];
            if (obj.isFinished() && obj.isAutoRemoveEnabled) {
                needsRefresh = true;
                tweenArrayList.remove(i);
                obj.free();
            }
        }

        if (needsRefresh) {
            // setup our children array, so update iterations are faster  (marginal improvement)
            childrenArray = new BaseTween[tweenArrayList.size()];
            tweenArrayList.toArray(childrenArray);
            flushWrite();
        }

        if (!isPaused) {
            // on start sync
            startEvent.update(this);

            for (int i = 0, n = childrenArray.length; i < n; i++) {
                BaseTween<?> tween = childrenArray[i];

                tween.flushRead();
                tween.updateState(elapsedMillis);
                // we completely update the state of everything before we update the tween values. This is because
                // multiple state changes can occur in a single frame, so we make sure that the expensive math only
                // happens once.
                tween.updateValues();
                tween.flushWrite();
            }

            // on finish sync
            endEvent.update(this);
        }
    }

	/**
	 * Gets the number of managed objects. An object may be a tween or a
	 * timeline. A timeline only counts for 1 object, since it
	 * manages its children itself.
	 * <p/>
	 * To get the count of running tweens, see {@link #getRunningTweensCount()}.
	 */
	public
    int size() {
		flushRead();
        return childrenArray.length;
	}

	/**
	 * Gets the number of running tweens. This number includes the tweens
	 * located inside timelines (and nested timelines).
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    int getRunningTweensCount() {
        flushRead();
        return getTweensCount(tweenArrayList);
	}

	/**
	 * Gets the number of running timelines. This number includes the timelines
	 * nested inside other timelines.
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    int getRunningTimelinesCount() {
        flushRead();
        return getTimelinesCount(tweenArrayList);
    }

    /**
	 * Gets an immutable list of every managed object.
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    List<BaseTween<?>> getObjects() {
        flushRead();
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
