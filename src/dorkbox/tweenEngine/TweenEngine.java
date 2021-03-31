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
package dorkbox.tweenEngine;

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dorkbox.objectPool.ObjectPool;
import dorkbox.objectPool.Pool;
import dorkbox.objectPool.PoolObject;

/**
 * The TweenEngine is responsible for creating Tweens and Timelines, and can be either managed, or un-managed.
 * <p>
 * If managed, {@link TweenEngine#update(float)} will update all your tweens and timelines at once, as well as managing the tween/timeline
 * life-cycles for you and releasing pooled objects.
 * <p>
 * If un-managed, then you must update the tween/timeline manually, and make sure to release the tween objects back to the pool manually via
 * {@link BaseTween#free()}
 * <p>
 * Just give it a bunch of tweens or timelines and call {@link #update()} periodically, you don't need to do anything else!
 * <p/>
 * More fine-grained control is available as well via {@link #update(float)} to update via seconds (1.0F == 1.0 seconds), or
 * {@link #update(long)} to update via nano-seconds.
 * <p/>
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 * @see Tween
 * @see Timeline
 */
@SuppressWarnings({"unused", "ForLoopReplaceableByForEach", "WeakerAccess"})
public
class TweenEngine {

    /**
     * @return a builder for creating the TweenEngine.
     */
    public static
    EngineBuilder create() {
        return new EngineBuilder();
    }

    /**
     * Builds a new, thread-safe TweenEngine using the default parameters.
     *  - NOTE: Threadsafe in this instance means that objects are visible between threads. There are no protections against race conditions.
     *  - Combined Attribute Limits = 3
     *  - Waypoint limit = 0
     */
    public static
    TweenEngine build() {
        // defaults
        return new TweenEngine(true, 3, 0, new HashMap<Class<?>, TweenAccessor<?>>());
    }


    // for creating arrays slightly faster...
    private static final BaseTween[] BASE_TWEENS = new BaseTween[0];

    /**
     * The default update event, which does nothing.
     */
    public static final UpdateAction<?> NULL_ACTION = new UpdateAction<Object>() {
        @Override
        public
        void onEvent(final Object tween) {
        }
    };

    /**
     * Gets the version number.
     */
    public static
    String getVersion() {
        return "8.3";
    }

    private final Map<Class<?>, TweenAccessor<?>> registeredAccessors = new HashMap<Class<?>, TweenAccessor<?>>();

    private final Pool<Timeline> poolTimeline;
    private final Pool<Tween> poolTween;

    // cannot change these once the animation system is built
    private final int combinedAttrsLimit;
    private final int waypointsLimit;


    private final ArrayList<BaseTween<?>> newTweens = new ArrayList<BaseTween<?>>(20);
    private final ArrayList<BaseTween<?>> tweenArrayList = new ArrayList<BaseTween<?>>(20);
    // private BaseTween<?>[] childrenArray = new BaseTween<?>[0];

    private boolean isPaused = false;

    private UpdateAction startEventCallback = NULL_ACTION;
    private UpdateAction endEventCallback = NULL_ACTION;

    private long lastTime = 0L;


    TweenEngine(final boolean threadSafe,
                final int combinedAttrsLimit,
                final int waypointsLimit,
                final Map<Class<?>, TweenAccessor<?>> registeredAccessors) {

        this.combinedAttrsLimit = combinedAttrsLimit;
        this.waypointsLimit = waypointsLimit;
        this.registeredAccessors.putAll(registeredAccessors);

        PoolObject<Timeline> timelinePoolableObject = new PoolObject<Timeline>() {
            @Override
            public
            Timeline newInstance() {
                return new Timeline(TweenEngine.this);
            }

            @Override
            public
            void onReturn(final Timeline object) {
                object.destroy();
            }
        };

        PoolObject<Tween> tweenPoolableObject = new PoolObject<Tween>() {
            @Override
            public
            void onReturn(final Tween object) {
                object.destroy();
            }

            @Override
            public
            Tween newInstance() {
                return new Tween(TweenEngine.this, TweenEngine.this.combinedAttrsLimit, TweenEngine.this.waypointsLimit);
            }
        };

        if (threadSafe) {
            poolTimeline = ObjectPool.INSTANCE.nonBlockingSoftReference(timelinePoolableObject);
            poolTween = ObjectPool.INSTANCE.nonBlockingSoftReference(tweenPoolableObject);
        }
        else {
            poolTimeline = ObjectPool.INSTANCE.nonBlockingSoftReference(timelinePoolableObject,
                                                                        new ArrayDeque<SoftReference<Timeline>>());
            poolTween = ObjectPool.INSTANCE.nonBlockingSoftReference(tweenPoolableObject,
                                                                     new ArrayDeque<SoftReference<Tween>>());
        }
    }

    /**
     * NOTE:
     * The flush() methods are overwritten in the "unsafe" operating mode (along with a non-thread-safe pool), so that there is
     * better performance at the cost of thread visibility/safety
     */
    private static volatile long lightSyncObject = System.nanoTime();

    /**
     * Only on public methods.
     * <p>
     * Flushes the visibility of all tween fields from the cache for access/use from different threads.
     * <p>
     * This does not block and does not prevent race conditions.
     *
     * @return the last time (in nanos) that the field modifications were flushed
     */
    @SuppressWarnings("UnusedReturnValue")
    long flushRead() {
        return lightSyncObject;
    }

    /**
     * Only on public methods.
     * <p>
     * Flushes the visibility of all tween field modifications from the cache for access/use from different threads.
     * <p>
     * This does not block and does not prevent race conditions.
     */
    void flushWrite() {
        lightSyncObject = System.nanoTime();
    }


    // -------------------------------------------------------------------------
    // MANAGER actions
    // -------------------------------------------------------------------------

    /**
     * Gets the registered TweenAccessor associated with the given object class.
     *
     * @param someClass An object class.
     */
    public
    TweenAccessor<?> getRegisteredAccessor(final Class<?> someClass) {
        flushRead();
        return registeredAccessors.get(someClass);
    }

    /**
     * Sets an event handler so that a notification is broadcast when the manager starts updating the current frame of animation.
     *
     * @param startCallback the callback for when the manager starts updating everything. NULL to unset.
     * @return The manager, for instruction chaining.
     */
    public
    TweenEngine setStartCallback(final UpdateAction<TweenEngine> startCallback) {
        if (startCallback == null) {
            this.startEventCallback = TweenEngine.NULL_ACTION;
        } else {
            this.startEventCallback = startCallback;

        }

        flushWrite();
        return this;
    }

    /**
     * Sets an event handler so that a notification is broadcast when the manager finishes updating the current frame of animation.
     *
     * @param endCallback the callback for when the manager finishes updating everything. NULL to unset.
     * @return The manager, for instruction chaining.
     */
    public
    TweenEngine setEndCallback(final UpdateAction<TweenEngine> endCallback) {
        if (endCallback == null) {
            this.endEventCallback = TweenEngine.NULL_ACTION;
        } else {
            this.endEventCallback = endCallback;
        }

        flushWrite();
        return this;
    }

    /**
     * Adds a tween or timeline to the manager and starts or restarts it.
     *
     * @return The manager, for instruction chaining.
     */
    public
    TweenEngine add(final BaseTween<?> tween) {
        flushRead();

        add__(tween);

        flushWrite();
        return this;
    }

    /**
     * doesn't sync on anything.
     * <p>
     * Adds a tween or timeline to the manager and starts or restarts it.
     */
    void add__(final BaseTween<?> tween) {

        newTweens.add(tween);

        if (tween.isAutoStartEnabled) {
            tween.startUnmanaged__();
        }
    }

    /**
     * Returns true if the manager contains any valid interpolation associated to the given target object.
     */
    public
    boolean containsTarget(final Object target) {
        flushRead();

        for (BaseTween tween : tweenArrayList) {
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
        flushRead();

        for (BaseTween tween : tweenArrayList) {
            if (tween.containsTarget(target, tweenType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancels all managed tweens and timelines.
     */
    public
    void cancelAll() {
        flushRead();

        boolean wasCanceled = false;
        for (BaseTween tween : tweenArrayList) {
            tween.cancel();

            // can only remove/resize the array list during update, otherwise modification of the list can happen during iteration.
        }

        flushWrite();
    }

    /**
     * Cancels all tweens associated to the given target. Will also cancel timelines containing a tween associated to the given
     * target.
     *
     * @return true if the target was canceled, false if we do not contain the target, and it was not canceled
     */
    @SuppressWarnings({"Duplicates", "UnusedReturnValue"})
    public
    boolean cancelTarget(final Object target) {
        flushRead();

        boolean wasCanceled = false;
        for (BaseTween tween : tweenArrayList) {
            if (tween.cancelTarget(target)) {
                wasCanceled = true;
            }

            // can only remove/resize the array list during update, otherwise modification of the list can happen during iteration.
        }

        flushWrite();
        return wasCanceled;
    }

    /**
     * Cancels every tween associated to the given target and tween type. Will also cancel every timeline containing a tween associated
     * to the given target and tween type.
     *
     * @return true if the target was canceled, false if we do not contain the target, and it was not canceled
     */
    @SuppressWarnings("Duplicates")
    public
    boolean cancelTarget(final Object target, final int tweenType) {
        flushRead();


        boolean wasCanceled = false;
        for (BaseTween tween : tweenArrayList) {
            if (tween.cancelTarget(target, tweenType)) {
                wasCanceled = true;
            }

            // can only remove/resize the array list during update, otherwise modification of the list can happen during iteration.
        }

        flushWrite();
        return wasCanceled;
    }

    /**
     * Increases the minimum capacity of the manager. Defaults to 20.
     */
    public
    void ensureCapacity(final int minCapacity) {
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
     * Resets the last time this tweenManager had "update" called. This is useful when the timer (that {@link #update()}) is
     * usually called on, has been stopped for a while. This prevents the "first" update call to 'snap' to the target values because the
     * time delta update was so large.
     */
    public
    void resetUpdateTime() {
        this.lastTime = System.nanoTime();
        flushWrite();
    }

    /**
     * Updates every added tween/timeline based upon the elapsed time between now and the previous time this method was called. This
     * method also handles the tween life-cycles automatically.
     * <p/>
     * If a tween is finished, it will be removed from the manager.
     */
    public
    void update() {
        flushRead();

        final long newTime = System.nanoTime();
        final float deltaTime = (newTime - lastTime) / 1.0E9F;
        this.lastTime = newTime;

        update__(deltaTime);

        flushWrite();
    }

    /**
     * Updates every added tween with a delta time in NANO-SECONDS and handles the tween life-cycles automatically. This converts the
     * delta time in nano-seconds to seconds.
     * <p/>
     * If a tween is finished, it will be removed from the manager.
     */
    public
    void update(final long deltaTimeInNanos) {
        flushRead();

        final float deltaTimeInSec = deltaTimeInNanos / 1.0E9F;
        update__(deltaTimeInSec);

        flushWrite();
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
    public final
    void update(final float delta) {
        flushRead();

        update__(delta);

        flushWrite();
    }

    /**
     * doesn't sync on anything.
     * <p>
     * Updates every added tween with a delta time in SECONDS and handles the tween life-cycles automatically.
     * <p/>
     * If a tween is finished, it will be removed from the manager.
     * <p/>
     * The delta time represents the elapsed time in seconds between now and the previous update call. Each tween or timeline manages its
     * local time, and adds this delta to its local time to update itself.
     * <p/>
     * Slow motion, fast motion and backward play can be easily achieved by tweaking this delta time. Multiply it by -1 to play the
     * animation backward, or by 0.5 to play it twice slower than its normal speed.
     */
    @SuppressWarnings("unchecked")
    private
    void update__(final float delta) {
        if (!isPaused) {
            // on start sync
            startEventCallback.onEvent(this);


            int size = newTweens.size();
            if (size > 0) {
                tweenArrayList.addAll(newTweens);
                newTweens.clear();
            }

            // this is the only place that REMOVING tweens from their list can occur, otherwise there can be issues with removing tweens
            // in the middle of iteration (above) because of callbacks/etc
            // boolean needsRefresh = false;
            
            for (BaseTween<?> tween : tweenArrayList) {
                tween.update__(delta);
            }
            // for (int i = 0, n = childrenArray.length; i < n; i++) {
            //     tween = childrenArray[i];
            //     tween.update__(delta);
            // }

            for (Iterator<BaseTween<?>> iterator = tweenArrayList.iterator(); iterator.hasNext(); ) {
                final BaseTween<?> tween = iterator.next();
                if (tween.isAutoRemoveEnabled) {
                    if (tween.state == BaseTween.FINISHED) {
                        // guarantee the tween/timeline values are set at the end
                        tween.setValues(true, false);

                        // needsRefresh = true;
                        iterator.remove();
                        tween.free();
                    }
                    else if (tween.isCanceled) {
                        // needsRefresh = true;
                        iterator.remove();
                        tween.free();
                    }
                }
            }

            // if (needsRefresh) {
            //     // setup our children array, so update iterations are faster  (marginal improvement)
            //     // childrenArray = tweenArrayList.toArray(BASE_TWEENS);
            // }

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
        flushRead();
        return tweenArrayList.size();
    }

    /**
     * Gets the number of running tweens. This number includes the tweens located inside timelines (and nested timelines).
     * <p/>
     * <b>Provided for debug purpose only.</b>
     */
    public
    int getRunningTweensCount() {
        flushRead();
        return getTweensCount(tweenArrayList);
    }

    /**
     * Gets the number of running timelines. This number includes the timelines nested inside other timelines.
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
    // TIMELINE actions
    // -------------------------------------------------------------------------

    /**
     * Creates a new timeline with a 'sequential' (A then B) behavior. Its children will be updated one after the other in a sequence.
     * <p>
     * It is not necessary to call {@link Timeline#end()} to close this timeline.
     */
    public
    Timeline createSequential() {
        Timeline timeline = poolTimeline.take();
        flushRead();
        timeline.setup__(Timeline.Mode.SEQUENTIAL);

        flushWrite();
        return timeline;
    }

    /**
     * Creates a new timeline with a 'parallel' (A + B at the same time) behavior. Its children will be updated all at once.
     * <p>
     * It is not necessary to call {@link Timeline#end()} to close this timeline.
     */
    public
    Timeline createParallel() {
        Timeline timeline = poolTimeline.take();
        flushRead();
        timeline.setup__(Timeline.Mode.PARALLEL);

        flushWrite();
        return timeline;
    }





    // -------------------------------------------------------------------------
    // TWEEN actions
    // -------------------------------------------------------------------------

    /**
     * Factory creating a new standard interpolation. This is the most common type of interpolation. The starting values are
     * retrieved automatically after the delay (if any).
     * <p>
     * <b>You need to set the target values of the interpolation by using one of the target() methods</b>. The interpolation will run
     * from the starting values to these target values.
     * <p>
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * {@link Tween#start()}, as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ({@link Tween#startUnmanaged()} then you will have to manage it's lifecycle manually.
     * <p>
     * <pre> {@code
     * TweenEngine.build().to(myObject, POSITION, 1.0F)
     *                    .target(50, 70)
     *                    .ease(Quad_InOut)
     *                    .start();
     * }</pre>
     * <p>
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    public
    Tween to(final Object target, final int tweenType, final float duration) {
        return to(target, tweenType, null, duration);
    }

    /**
     * Factory creating a new standard interpolation. This is the most common type of interpolation. The starting values are
     * retrieved automatically after the delay (if any).
     * <p>
     * <b>You need to set the target values of the interpolation by using one of the target() methods</b>. The interpolation will run
     * from the starting values to these target values.
     * <p>
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * {@link Tween#start()}, as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ({@link Tween#startUnmanaged()} then you will have to manage it's lifecycle manually.
     * <p>
     * <pre> {@code
     * TweenEngine engine = TweenEngine.build();
     * engine.to(myObject, POSITION, accessorObject, 1.0F)
     *       .target(50, 70)
     *       .ease(Quad_InOut)
     *       .start();
     * }</pre>
     * <p>
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param targetAccessor The accessor object (optional) that is used to modify the target values (based on the tween type).
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    public
    <T> Tween to(final T target, final int tweenType, final TweenAccessor<T> targetAccessor, final float duration) {
        Tween tween = takeTween();
        flushRead();

        tween.setup__(target, tweenType, targetAccessor, duration);
        tween.ease__(TweenEquations.Quad_InOut);
        tween.path__(TweenPaths.CatmullRom);

        flushWrite();
        return tween;
    }

    /**
     * Factory creating a new reversed interpolation. The ending values are retrieved automatically after the delay (if any).
     * <p>
     * <b>You need to set the starting values of the interpolation by using one of the target() methods</b>. The interpolation will run
     * from the starting values to these target values.
     * <p>
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * {@link Tween#start()}, as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ({@link Tween#startUnmanaged()} then you will have to manage it's lifecycle manually.
     * <p>
     * <pre> {@code
     * TweenEngine engine = TweenEngine.build();
     * engine.from(myObject, POSITION, 1.0F)
     *       .target(0, 0)
     *       .ease(Quad_InOut)
     *       .start();
     * }</pre>
     * <p>
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    public
    <T> Tween from(final T target, final int tweenType, final float duration) {
        return from(target, tweenType, null, duration);
    }

    /**
     * Factory creating a new reversed interpolation. The ending values are retrieved automatically after the delay (if any).
     * <p>
     * <b>You need to set the starting values of the interpolation by using one of the target() methods</b>. The interpolation will run
     * from the starting values to these target values.
     * <p>
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * {@link Tween#start()}, as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ({@link Tween#startUnmanaged()} then you will have to manage it's lifecycle manually.
     * <p>
     * <pre> {@code
     * TweenEngine engine = TweenEngine.build();
     * engine.from(myObject, POSITION, 1.0F)
     *       .target(0, 0)
     *       .ease(Quad_InOut)
     *       .start();
     * }</pre>
     * <p>
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param targetAccessor The accessor object (optional) that is used to modify the target values (based on the tween type).
     * @param duration The duration of the interpolation, in seconds.
     *
     * @return The generated Tween.
     */
    public
    <T> Tween from(final T target, final int tweenType, final TweenAccessor<T> targetAccessor, final float duration) {
        Tween tween = takeTween();
        flushRead();

        tween.setup__(target, tweenType, targetAccessor, duration);
        tween.ease__(TweenEquations.Quad_InOut);
        tween.path__(TweenPaths.CatmullRom);
        tween.isFrom = true;

        flushWrite();
        return tween;
    }

    /**
     * Factory creating a new instantaneous interpolation (thus this is not really an interpolation).
     * <p>
     * <b>You need to set the target values of the interpolation by using one of the target() methods</b>. The interpolation will set
     * the target attribute to these values after the delay (if any).
     * <p>
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * {@link Tween#start()}, as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ({@link Tween#startUnmanaged()} then you will have to manage it's lifecycle manually.
     * <p>
     * <pre> {@code
     * TweenEngine engine = TweenEngine.build();
     * engine.set(myObject, POSITION)
     *       .target(50, 70)
     *       .delay(1.0F)
     *       .start();
     * }</pre>
     * <p>
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     *
     * @return The generated Tween.
     */
    public
    <T> Tween set(final T target, final int tweenType) {
        return set(target, tweenType, null);
    }

    /**
     * Factory creating a new instantaneous interpolation (thus this is not really an interpolation).
     * <p>
     * <b>You need to set the target values of the interpolation by using one of the target() methods</b>. The interpolation will set
     * the target attribute to these values after the delay (if any).
     * <p>
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * {@link Tween#start()}, as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ({@link Tween#startUnmanaged()} then you will have to manage it's lifecycle manually.
     * <p>
     * <pre> {@code
     * TweenEngine engine = TweenEngine.build();
     * engine.set(myObject, POSITION)
     *       .target(50, 70)
     *       .delay(1.0F)
     *       .start();
     * }</pre>
     * <p>
     * Several options such as delay, repetitions and callbacks can be added to the tween.
     *
     * @param target The target object of the interpolation.
     * @param targetAccessor The accessor object (optional) that is used to modify the target values (based on the tween type).
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     *
     * @return The generated Tween.
     */
    public
    <T> Tween set(final T target, final int tweenType, final TweenAccessor<T> targetAccessor) {
        Tween tween = takeTween();
        flushRead();

        tween.setup__(target, tweenType, targetAccessor, 0.0F);
        tween.ease__(TweenEquations.Quad_In);

        flushWrite();
        return tween;
    }

    /**
     * Factory creating a new timer. The given callback will be triggered on each iteration start, after the delay.
     * <p>
     * The common use of Tweens is "fire-and-forget": you do not need to care for tweens if they are started normally via
     * {@link Tween#start()}, as they will be be updated and cleaned/etc automatically once finished. If started unmanaged via
     * ({@link Tween#startUnmanaged()} then you will have to manage it's lifecycle manually.
     * <p>
     * <pre> {@code
     * TweenEngine engine = TweenEngine.build();
     * engine.call(myCallback)
     *       .delay(1.0F)
     *       .repeat(10, 1.0F)
     *       .start();
     * }</pre>
     *
     * @param callback The callback that will be triggered on each iteration start.
     *
     * @return The generated Tween.
     *
     * @see TweenCallback
     */
    public
    Tween call(final TweenCallback callback) {
        Tween tween = takeTween();
        flushRead();

        tween.setup__(null, -1, null, 0.0F);
        callback.triggers = TweenCallback.Events.START;
        tween.addCallback(callback); // Thread/Concurrent use Safe

        flushWrite();
        return tween;
    }

    /**
     * Convenience method to create an empty tween. Such object is only useful when placed inside animation sequences
     * (see {@link Timeline}), in which it may act as a beacon, so you can set a callback on it in order to trigger some action at the
     * right moment.
     *
     * @return The generated Tween.
     *
     * @see Timeline
     */
    public
    Tween mark() {
        Tween tween = takeTween(); // calls flushRead

        tween.setup__(null, -1, null, 0.0F);

        flushWrite();
        return tween;
    }

    /**
     * doesn't sync on anything.
     * <p>
     * Convenience method to create an empty tween. Such object is only useful when placed inside animation sequences
     * (see {@link Timeline}), in which it may act as a beacon, so you can set a callback on it in order to trigger some action at the
     * right moment.
     *
     * @return The generated Tween.
     *
     * @see Timeline
     */
    Tween mark__() {
        Tween tween = takeTween__();

        tween.setup__(null, -1, null, 0.0F);

        return tween;
    }





    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Also flushRead();
     */
    Timeline takeTimeline() {
        Timeline take = poolTimeline.take();
        flushRead();
        return take;
    }

    /**
     * Also flushRead();
     */
    Tween takeTween() {
        Tween take = poolTween.take();
        flushRead();
        return take;
    }

    /**
     * doesn't sync on anything.
     */
    Tween takeTween__() {
        return poolTween.take();
    }

    void free(final Timeline timeline) {
        poolTimeline.put(timeline);
    }

    void free(final Tween tween) {
        poolTween.put(tween);
    }

    boolean containsAccessor(final Class<?> accessorClass) {
        return registeredAccessors.containsKey(accessorClass);
    }

    TweenAccessor getAccessor(final Class<?> accessorClass) {
        return registeredAccessors.get(accessorClass);
    }

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

