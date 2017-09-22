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

import java.util.ArrayDeque;

import com.google.gwt.core.client.Duration;

import dorkbox.tweenengine.pool.ObjectPool;
import dorkbox.tweenengine.pool.PoolableObject;

/**
 * GWT compatible class
 */
public
class EngineUtils {
    public static
    long nanoTime() {
        return ((long) Duration.currentTimeMillis() * 1000);
    }

    public static <T> ObjectPool<T> getPool(final boolean threadSafe, final PoolableObject<T> poolableObject) {
        return new ObjectPool<T>(poolableObject, new ArrayDeque<T>());
    }
}
