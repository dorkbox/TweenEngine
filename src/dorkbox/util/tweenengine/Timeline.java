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

import dorkbox.util.objectPool.ObjectPool;
import dorkbox.util.objectPool.ObjectPoolFactory;
import dorkbox.util.objectPool.PoolableObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Timeline can be used to create complex animations made of sequences and
 * parallel sets of Tweens.
 * <p/>
 *
 * The following example will create an animation sequence composed of 5 parts:
 * <p/>
 *
 * 1. First, opacity and scale are set to 0 (with Tween.set() calls).<br/>
 * 2. Then, opacity and scale are animated in parallel.<br/>
 * 3. Then, the animation is paused for 1s.<br/>
 * 4. Then, position is animated to x=100.<br/>
 * 5. Then, rotation is animated to 360°.
 * <p/>
 *
 * This animation will be repeated 5 times, with a 500ms delay between each
 * iteration:
 * <br/><br/>
 *
 * <pre> {@code
 * Timeline.createSequence()
 *     .push(Tween.set(myObject, OPACITY).target(0))
 *     .push(Tween.set(myObject, SCALE).target(0, 0))
 *     .beginParallel()
 *          .push(Tween.to(myObject, OPACITY, 0.5f).target(1).ease(Quad.INOUT))
 *          .push(Tween.to(myObject, SCALE, 0.5f).target(1, 1).ease(Quad.INOUT))
 *     .end()
 *     .pushPause(1.0f)
 *     .push(Tween.to(myObject, POSITION_X, 0.5f).target(100).ease(Quad.INOUT))
 *     .push(Tween.to(myObject, ROTATION, 0.5f).target(360).ease(Quad.INOUT))
 *     .repeat(5, 0.5f)
 *     .start(myManager);
 * }</pre>
 *
 * @see Tween
 * @see TweenManager
 * @see TweenCallback
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
@SuppressWarnings("unused")
public final
class Timeline extends BaseTween<Timeline> {
	// -------------------------------------------------------------------------
	// Static -- pool
	// -------------------------------------------------------------------------

    @SuppressWarnings("StaticNonFinalField")
    private static final Thread constructorThread = Thread.currentThread();
    private static final PoolableObject<Timeline> poolableObject = new PoolableObject<Timeline>() {
        @Override
        public
        void reset(final Timeline object) {
            object.reset();
        }

        @Override
        public
        Timeline create() {
            return new Timeline();
        }
    };

    static ObjectPool<Timeline> pool = ObjectPoolFactory.create(poolableObject, 256);


	/**
	 * Used for debug purpose. Gets the current number of empty timelines that
	 * are waiting in the Timeline pool.
	 */
	public static
    int getPoolSize() {
		return pool.size();
	}

    /**
     * Increases the minimum capacity of the pool. Capacity defaults to 256.
     * <p>
     * Needs to be set before any threads access or use the timeline. This is not thread safe!
     */
    public static
    void setPoolSize(final int poolSize) {
        if (constructorThread != Thread.currentThread()) {
            throw new RuntimeException("Timeline pool capacity must be changed during engine initialization!");
        }
        pool =  ObjectPoolFactory.create(poolableObject, poolSize);
	}

	// -------------------------------------------------------------------------
	// Static -- factories
	// -------------------------------------------------------------------------

	/**
	 * Creates a new timeline with a 'sequence' behavior. Its children will
	 * be delayed so that they are triggered one after the other.
	 */
	public static
    Timeline createSequence() {
		Timeline tl = pool.takeUninterruptibly();
		tl.setup(Modes.SEQUENCE);
		return tl;
	}

	/**
	 * Creates a new timeline with a 'parallel' behavior. Its children will be
	 * triggered all at once.
	 */
	public static
    Timeline createParallel() {
		Timeline tl = pool.takeUninterruptibly();
		tl.setup(Modes.PARALLEL);
		return tl;
	}

    // -------------------------------------------------------------------------
	// Attributes
	// -------------------------------------------------------------------------

	private enum Modes {SEQUENCE, PARALLEL}


	private final List<BaseTween<?>> children = new ArrayList<BaseTween<?>>(10);
    private BaseTween<?>[] childrenArray = null;
    protected Timeline parent;
    private Timeline current;
	private Modes mode;
	private boolean isBuilt;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	Timeline() {
		reset();
	}

	@Override
	protected
    void reset() {
		super.reset();

		children.clear();
        childrenArray = null;
		current = parent = null;
		isBuilt = false;
	}

	private
    void setup(final Modes mode) {
		this.mode = mode;
		this.current = this;
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Adds a Tween to the current timeline.
	 *
	 * @return The current timeline, for chaining instructions.
	 */
	public
    Timeline push(final Tween tween) {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }

        current.children.add(tween);
        return this;
	}

	/**
	 * Nests a Timeline in the current one.
	 *
	 * @return The current timeline, for chaining instructions.
	 */
	public
    Timeline push(final Timeline timeline) {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }
        if (timeline.current != timeline) {
            throw new RuntimeException("You forgot to call a few 'end()' statements in your pushed timeline");
        }

        timeline.parent = current;
        current.children.add(timeline);
        return this;
    }

	/**
	 * Adds a pause to the timeline. The pause may be negative if you want to
	 * overlap the preceding and following children.
	 *
	 * @param time A positive or negative duration in milliseconds
     *
	 * @return The current timeline, for chaining instructions.
	 */
	public
    Timeline pushPause(final int time) {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }

        current.children.add(Tween.mark()
                                  .delay(time));
        return this;
	}

	/**
	 * Starts a nested timeline with a 'sequence' behavior. Don't forget to
	 * call {@link Timeline#end()} to close this nested timeline.
	 *
	 * @return The current timeline, for chaining instructions.
	 */
    public
    Timeline beginSequence() {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }

        Timeline tl = pool.takeUninterruptibly();
        tl.parent = current;
        tl.mode = Modes.SEQUENCE;
        current.children.add(tl);
        current = tl;
        return this;
	}

	/**
	 * Starts a nested timeline with a 'parallel' behavior. Don't forget to
	 * call {@link Timeline#end()} to close this nested timeline.
	 *
	 * @return The current timeline, for chaining instructions.
	 */
    public
    Timeline beginParallel() {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }

        Timeline tl = pool.takeUninterruptibly();
        tl.parent = current;
        tl.mode = Modes.PARALLEL;
        current.children.add(tl);
        current = tl;
        return this;
	}

	/**
	 * Closes the last nested timeline.
	 *
	 * @return The current timeline, for chaining instructions.
	 */
    public
    Timeline end() {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }
        if (current == this) {
            throw new RuntimeException("Nothing to end...");
        }

        current = current.parent;
        return this;
	}

	/**
	 * Gets a list of the timeline children. If the timeline is started, the
	 * list will be immutable.
	 */
	public
    List<BaseTween<?>> getChildren() {
        if (isBuilt) {
            return Collections.unmodifiableList(current.children);
        }
        else {
            return current.children;
        }
    }

	// -------------------------------------------------------------------------
	// Overrides
	// -------------------------------------------------------------------------

	@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
    @Override
	public
    Timeline build() {
        if (isBuilt) {
            return this;
        }

        duration = 0;

        for (int i = 0, n = children.size(); i < n; i++) {
            final BaseTween<?> obj = children.get(i);

            if (obj.getRepeatCount() < 0) {
                throw new RuntimeException("You can't push an object with infinite repetitions in a timeline");
            }
            obj.build();

            switch (mode) {
                case SEQUENCE:
                    final int tDelay = duration;
                    duration += obj.getFullDuration();
                    obj.delay += tDelay;
                    break;

                case PARALLEL:
                    duration = Math.max(duration, obj.getFullDuration());
                    break;
            }
        }

        isBuilt = true;
		return this;
	}

	@Override
	public
    Timeline start() {
        super.start();

        if (this.parent == null) {
            this.name = '*';
        }

        int size = children.size();
        for (int i = 0; i < size; i++) {
            final BaseTween<?> obj = children.get(i);
            obj.start();
        }

        // setup our children array, so update iterations are faster
        childrenArray = new BaseTween[size];
        children.toArray(childrenArray);

        return this;
    }

	@Override
	public
    void free() {
        for (int i = children.size() - 1; i >= 0; i--) {
            final BaseTween<?> obj = children.remove(i);
            obj.free();
        }

        pool.release(this);
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
        // internally we use INTEGER, since we want consistent timelines & events

        if (this.parent == null) {
            // ONLY modify the incoming delta if we are the parent timeline!
            if (getDirection()) {
                super.update((int) (delta * 1000F));
            } else {
                // if we are now in reverse, flip the incoming delta.
                super.update((int) (delta * -1000F));
            }
        }
        else {
            super.update((int) (delta * 1000F));
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
    public
    void update(final int delta) {
        if (this.parent == null) {
//            if (delta >= 0 != getDirection()) {
//                super.update(-delta);
//            } else {
//                super.update(delta);
//            }


            // ONLY modify the incoming delta if we are the parent timeline!
            if (getDirection()) {
                super.update(delta);
            } else {
                // if we are now in reverse, flip the incoming delta.
                super.update(-delta);
            }
        }
        else {
            super.update(delta);
        }
    }

	// -------------------------------------------------------------------------
	// BaseTween impl.
	// -------------------------------------------------------------------------

    @Override
    protected
    void addRepeatDelay(final int repeatDelay) {
        super.addRepeatDelay(repeatDelay);

        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            tween.addRepeatDelay(repeatDelay);
        }
    }

    @Override
    protected
    void forceRestart(final int restartAdjustment) {
//        final int adjustment = getRepeatDelay() + restartAdjustment;

        super.forceRestart(restartAdjustment);

        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            tween.forceRestart(restartAdjustment);
        }
    }

    @Override
    protected
    void doUpdate(final boolean animationDirection, final int delta) {
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            tween.update(delta);
        }
    }

	@Override
	protected
    boolean containsTarget(final Object target) {
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            if (tween.containsTarget(target)) {
                return true;
            }
        }
        return false;
	}

	@Override
    protected
    boolean containsTarget(final Object target, final int tweenType) {
        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            if (tween.containsTarget(target, tweenType)) {
                return true;
            }
        }
        return false;
    }
}
