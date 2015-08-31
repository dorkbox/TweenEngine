package dorkbox.util.tweenengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A TweenManager updates all your tweens and timelines at once.
 * Its main interest is that it handles the tween/timeline life-cycles for you,
 * as well as releasing pooled objected.
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
    void setAutoRemove(final BaseTween<?> object, final boolean value) {
        object.isAutoRemoveEnabled = value;
    }

    /**
	 * Disables or enables the "auto start" mode of any tween manager for a
	 * particular tween or timeline. This mode is activated by default. If it
	 * is not enabled, add a tween or timeline to any manager won't start it
	 * automatically, and you'll need to call .start() manually on your object.
	 */
    public static
    void setAutoStart(final BaseTween<?> object, final boolean value) {
        object.isAutoStartEnabled = value;
    }

    // -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	private final ArrayList<BaseTween<?>> objects = new ArrayList<BaseTween<?>>(20);
	private boolean isPaused = false;

	/**
	 * Adds a tween or timeline to the manager and starts or restarts it.
	 *
	 * @return The manager, for instruction chaining.
	 */
	public
    TweenManager add(final BaseTween<?> object) {
        if (!objects.contains(object)) {
            objects.add(object);
        }
        if (object.isAutoStartEnabled) {
            object.start();
        }
        return this;
	}

	/**
	 * Returns true if the manager contains any valid interpolation associated
	 * to the given target object.
	 */
	public
    boolean containsTarget(final Object target) {
        for (int i = 0, n = objects.size(); i < n; i++) {
            final BaseTween<?> obj = objects.get(i);
            if (obj.containsTarget(target)) {
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
        for (int i = 0, n = objects.size(); i < n; i++) {
            final BaseTween<?> obj = objects.get(i);
            if (obj.containsTarget(target, tweenType)) {
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
        for (int i = 0, n = objects.size(); i < n; i++) {
            final BaseTween<?> obj = objects.get(i);
            obj.kill();
        }
    }

	/**
	 * Kills every tweens associated to the given target. Will also kill every
	 * timelines containing a tween associated to the given target.
	 */
	public
    void killTarget(final Object target) {
        for (int i = 0, n = objects.size(); i < n; i++) {
            final BaseTween<?> obj = objects.get(i);
            obj.killTarget(target);
        }
    }

	/**
	 * Kills every tweens associated to the given target and tween type. Will
	 * also kill every timelines containing a tween associated to the given
	 * target and tween type.
	 */
	public
    void killTarget(final Object target, final int tweenType) {
        for (int i = 0, n = objects.size(); i < n; i++) {
            final BaseTween<?> obj = objects.get(i);
            obj.killTarget(target, tweenType);
        }
    }

	/**
	 * Increases the minimum capacity of the manager. Defaults to 20.
	 */
	public void ensureCapacity(final int minCapacity) {
		objects.ensureCapacity(minCapacity);
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
        //    Floating-point operations are always slower than integer ops at same data size.
        // internally we also want to use INTEGER, since we want consistent timelines as well
        final int deltaMSeconds = (int) (delta * 1000F);
        update(delta);
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
     * @param delta A delta time in MILLI-SECONDS between now and the last call.
     */
    public
    void update(final int delta) {
        for (int i = objects.size() - 1; i >= 0; i--) {
            final BaseTween<?> obj = objects.get(i);
            if (obj.isFinished() && obj.isAutoRemoveEnabled) {
                objects.remove(i);
                obj.free();
            }
        }

        if (!isPaused) {
            // when running in reverse, we change the order at which we iterate over objects
            //noinspection Duplicates
            if (delta >= 0) {
                for (int i = 0, n = objects.size(); i < n; i++) {
                    objects.get(i)
                           .update(delta);
                }
            }
            else {
                for (int i = objects.size() - 1; i >= 0; i--) {
                    objects.get(i)
                           .update(delta);
                }
            }
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
		return objects.size();
	}

	/**
	 * Gets the number of running tweens. This number includes the tweens
	 * located inside timelines (and nested timelines).
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    int getRunningTweensCount() {
		return getTweensCount(objects);
	}

	/**
	 * Gets the number of running timelines. This number includes the timelines
	 * nested inside other timelines.
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    int getRunningTimelinesCount() {
        return getTimelinesCount(objects);
    }

    /**
	 * Gets an immutable list of every managed object.
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public
    List<BaseTween<?>> getObjects() {
		return Collections.unmodifiableList(objects);
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
