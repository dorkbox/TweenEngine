/*
 * Copyright 2016 dorkbox, llc
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
package dorkbox.tweenengine.pool;

import java.util.Queue;

/**
 * A non-blocking pool which will grow as much as needed. If the pool is empty, new objects will be created. The items in the
 * pool will never expire or be automatically garbage collected, unless the queue type has soft references.
 *
 * @author dorkbox, llc
 */
public
class ObjectPool<T> {
    protected final Queue queue;
    protected final PoolableObject poolableObject;


    public
    ObjectPool(final PoolableObject poolableObject, final Queue queue) {
        this.poolableObject = poolableObject;
        this.queue = queue;
    }

    /**
     * Takes an object from the pool.
     */
    @SuppressWarnings("unchecked")
    public
    T take() {
        T take = (T) this.queue.poll();
        if (take == null) {
            take = (T) poolableObject.create();
        }

        // poolableObject.onTake(take);
        return take;
    }

    /**
     * Return object to the pool.
     */
    @SuppressWarnings("unchecked")
    public
    void put(final T object) {
        poolableObject.onReturn(object);
        this.queue.offer(object);
    }
}
