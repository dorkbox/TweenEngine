/*
 * Copyright 2017 dorkbox, llc
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

import java.lang.ref.SoftReference;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import dorkbox.tweenengine.pool.ObjectPool;
import dorkbox.tweenengine.pool.ObjectPoolSoft;
import dorkbox.tweenengine.pool.PoolableObject;

/**
 * Methods that are not compatible with GWT, and are changed when compiling GWT
 */
class EngineUtils {
    static
    long nanoTime() {
        return System.nanoTime();
    }

    static <T> ObjectPool<T> getPool(final boolean threadSafe, final PoolableObject<T> poolableObject) {
        if (threadSafe) {
            return new ObjectPoolSoft<T>(poolableObject, new ConcurrentLinkedQueue<SoftReference<T>>());
        }
        else {
            return new ObjectPoolSoft<T>(poolableObject, new ArrayDeque<SoftReference<T>>());
        }
    }
}
