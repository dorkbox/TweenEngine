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

import java.lang.ref.SoftReference;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A non-blocking pool which will grow as much as needed. If the pool is empty, new objects will be created. The items in the
 * pool will expire and be automatically Garbage Collected in response to memory demand.
 * @author dorkbox, llc
 */
public
class ObjectPoolSoft<T> extends ObjectPool<T> {

    ObjectPoolSoft(final PoolableObject<T> poolableObject) {
        this(poolableObject, new ConcurrentLinkedQueue<SoftReference<T>>());
    }

    public
    ObjectPoolSoft(final PoolableObject<T> poolableObject, final Queue<SoftReference<T>> queue) {
        super(poolableObject, queue);
    }

    /**
     * Takes an object from the pool.
     */
    @Override
    public
    T take() {
        T obj;
        SoftReference<T> ref;
        while((ref = (SoftReference<T>) queue.poll()) != null) {
            if((obj = ref.get()) != null) {
                // poolableObject.onTake(obj);
                return obj;
            }
        }

        final T take = (T) poolableObject.create();
        // poolableObject.onTake(take);
        return take;
    }

    /**
     * Return object to the pool.
     */
    @Override
    public
    void put(T object) {
        poolableObject.onReturn(object);
        this.queue.offer(new SoftReference<T>(object));
    }
}

