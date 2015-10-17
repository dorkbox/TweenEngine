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
 *          .push(Tween.to(myObject, OPACITY, .5F).target(1).ease(Quad.INOUT))
 *          .push(Tween.to(myObject, SCALE, .5F).target(1, 1).ease(Quad.INOUT))
 *     .end()
 *     .pushPause(1.0F)
 *     .push(Tween.to(myObject, POSITION_X, .5F).target(100).ease(Quad.INOUT))
 *     .push(Tween.to(myObject, ROTATION, .5F).target(360).ease(Quad.INOUT))
 *     .repeat(5, .5F)
 *     .start(myManager);
 * }</pre>
 *
 * @see Tween
 * @see TweenManager
 * @see TweenCallback
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
@SuppressWarnings({"unused", "Duplicates"})
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
        void onReturn(final Timeline object) {
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
        pool = ObjectPoolFactory.create(poolableObject, poolSize);
	}

	// -------------------------------------------------------------------------
	// Static -- factories
	// -------------------------------------------------------------------------

	/**
	 * Creates a new timeline with a 'sequence' behavior. Its children will
	 * be updated one after the other.
	 */
	public static
    Timeline createSequence() {
		Timeline tl = pool.takeUninterruptibly();
		tl.setup(Modes.SEQUENCE);
		return tl;
	}

	/**
	 * Creates a new timeline with a 'parallel' behavior. Its children will be
	 * updated all at once.
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
    private int childrenSize;
    private int childrenSizeMinusOne;

    private Modes mode;
    private boolean isBuilt;

    protected Timeline parent;
    // current is used for TWO things.
    //  - Tracking what to start/end during construction
    //  - Tracking WHICH tween/timeline (of the children) is currently being run.
    private BaseTween<?> current;
    private int currentIndex;

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
        currentIndex = 0;
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

        children.add(tween);

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

        timeline.parent = this;
        children.add(timeline);

        return this;
    }

	/**
	 * Adds a pause to the timeline. The pause may be negative if you want to
	 * overlap the preceding and following children.
	 *
	 * @param time A positive or negative duration in seconds
     *
	 * @return The current timeline, for chaining instructions.
	 */
	public
    Timeline pushPause(final float time) {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }

        children.add(Tween.mark()
                          .delay(time));

        return this;
	}

	/**
	 * Starts a nested timeline with a 'sequence' behavior. Don't forget to
	 * call {@link Timeline#end()} to close this nested timeline.
	 *
	 * @return The new sequential timeline, for chaining instructions.
	 */
    public
    Timeline beginSequence() {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }

        Timeline timeline = pool.takeUninterruptibly();
        children.add(timeline);

        timeline.parent = this;
        timeline.mode = Modes.SEQUENCE;

        // keep track of which timeline we are on
        current = timeline;

        return timeline;
	}

	/**
	 * Starts a nested timeline with a 'parallel' behavior. Don't forget to
	 * call {@link Timeline#end()} to close this nested timeline.
	 *
	 * @return The new parallel timeline, for chaining instructions.
	 */
    public
    Timeline beginParallel() {
        if (isBuilt) {
            throw new RuntimeException("You can't push anything to a timeline once it is started");
        }

        Timeline timeline = pool.takeUninterruptibly();
        children.add(timeline);

        timeline.parent = this;
        timeline.mode = Modes.PARALLEL;

        // keep track of which timeline we are on
        current = timeline;

        return timeline;
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
            throw new RuntimeException("Nothing to end, calling end before begin!");
        }

        current = parent;

        if (current == null) {
            throw new RuntimeException("Whoops! Shouldn't be null!");
        }
        if (current.getClass() != Timeline.class) {
            throw new RuntimeException("You cannot end something other than a Timeline!");
        }

        return (Timeline) current;
	}

	/**
	 * Gets a list of the timeline children. If the timeline is started, the
	 * list will be immutable.
	 */
	public
    List<BaseTween<?>> getChildren() {
        if (isBuilt) {
            return Collections.unmodifiableList(children);
        }
        else {
            return children;
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

        for (int i = 0, n = children.size(); i < n; i++) {
            final BaseTween<?> obj = children.get(i);

            if (obj.getRepeatCount() < 0) {
                throw new RuntimeException("You can't push an object with infinite repetitions in a timeline");
            }
            obj.build();

            switch (mode) {
                case SEQUENCE:
                    duration += obj.getFullDuration();
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

        childrenSize = children.size();
        childrenSizeMinusOne = childrenSize - 1;

        // setup our children array, so update iterations are faster
        childrenArray = new BaseTween[childrenSize];
        children.toArray(childrenArray);

        for (int i = 0; i < childrenSize; i++) {
            final BaseTween<?> obj = childrenArray[i];
            obj.start();
        }

        if (childrenSize > 0) {
            current = childrenArray[0];
        } else {
            throw new RuntimeException("Creating a timeline with zero children. This is likely unintended, and if it is, this is not " +
                                       "permitted.");
        }

        return this;
    }

	@Override
	public
    void free() {
        for (int i = children.size() - 1; i >= 0; i--) {
            final BaseTween<?> tween = children.remove(i);
            tween.free();
        }

        pool.release(this);
    }

	// -------------------------------------------------------------------------
	// BaseTween impl.
	// -------------------------------------------------------------------------


    /**
     * Recursively adjust the tweens for when repeat + auto-reverse is used
     *
     * @param updateDirection the future direction for all children
     */
    @Override
    protected
    void adjustForRepeat_AutoReverse(final boolean updateDirection) {
        super.adjustForRepeat_AutoReverse(updateDirection);

        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            tween.adjustForRepeat_AutoReverse(updateDirection);
        }
    }

    /**
     * Adjust the current time (set to the start value for the tween) and change state to DELAY.
     * </p>
     * For timelines, this also changes what the current tween is (for when iterating over tweens)
     *
     * @param updateDirection the future direction for all children
     */
    protected
    void adjustForRepeat_Linear(final boolean updateDirection) {
        super.adjustForRepeat_Linear(updateDirection);

        for (int i = 0, n = childrenArray.length; i < n; i++) {
            final BaseTween<?> tween = childrenArray[i];
            tween.adjustForRepeat_Linear(updateDirection);
        }

        // this only matters if we are a sequence, because PARALLEL operates on all of them at the same time
        if (mode == Modes.SEQUENCE) {
            if (updateDirection) {
                currentIndex = 0;
            }
            else {
                currentIndex = childrenSize-1;
            }

            current = childrenArray[currentIndex];
        }
    }

    /**
     * Updates a timeline's children, in different orders.
     */
    protected
    void update(final boolean updateDirection, final DeltaHolder deltaHolder) {
        if (mode == Modes.SEQUENCE) {
            // update children one at a time.

            if (updateDirection) {
                while (deltaHolder.delta != 0) {
                    final boolean finished = current.update(deltaHolder);
                    if (finished) {
                        // iterate to the next one when it's finished, but don't go beyond the last child
                        if (currentIndex < childrenSizeMinusOne) {
                            currentIndex++;
                            current = childrenArray[currentIndex];
                        } else if (this.parent != null) {
                            // keep track of implicit time "overflow", where currentTime + delta > duration.
                            // This logic is so that this is recorded only on the outermost timeline for when timelines reverse direction
                            return;
                        }
                    }
                }
            }
            else {
                while (deltaHolder.delta != 0) {
                    final boolean finished = current.update(deltaHolder);
                    if (finished) {
                        // iterate to the previous one (because we are in reverse) when it's finished, but don't go beyond the first child
                        if (currentIndex > 0) {
                            currentIndex--;
                            current = childrenArray[currentIndex];
                        } else if (this.parent != null) {
                            // keep track of implicit time "overflow", where currentTime + delta > duration.
                            // This logic is so that this is recorded only on the outermost timeline for when timelines reverse direction
                            return;
                        }
                    }
                }
            }
        }

        // update all children at once
        else {
            final float saved = deltaHolder.delta;

            if (updateDirection) {
                for (int i = 0, n = childrenArray.length; i < n; i++) {
                    final BaseTween<?> tween = childrenArray[i];
                    deltaHolder.delta = saved;
                    final boolean finished = tween.update(deltaHolder);
                    if (finished) {
                        // each child has to track "overflow" info to set delay's correctly when the timeline reverses
                        tween.currentTime += deltaHolder.delta;
                    }
                }
            }
            else {
                for (int i = childrenArray.length - 1; i >= 0; i--) {
                    final BaseTween<?> tween = childrenArray[i];
                    deltaHolder.delta = saved;
                    final boolean finished = tween.update(deltaHolder);
                    if (finished) {
                        // each child has to track "overflow" info to set delay's correctly when the timeline reverses
                        tween.currentTime += deltaHolder.delta;
                    }
                }
            }
        }
    }

    /**
     * Forces a timeline/tween to have it's start/target values
     *
     * @param updateDirection direction in which the force is happening. Affects children iteration order (timelines) and start/target
     *                        values (tweens)
     *
     * @param updateValue this is the start (true) or target (false) to set the tween to.
     */
    protected
    void setValues(final boolean updateDirection, final boolean updateValue) {
        if (updateDirection) {
            for (int i = 0, n = childrenArray.length; i < n; i++) {
                final BaseTween<?> tween = childrenArray[i];
                tween.setValues(true, updateValue);
            }
        }
        else {
            for (int i = childrenArray.length - 1; i >= 0; i--) {
                final BaseTween<?> tween = childrenArray[i];
                tween.setValues(false, updateValue);
            }
        }
    }

	@Override
	protected final
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
    protected final
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
