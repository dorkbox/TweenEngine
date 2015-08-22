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
 */
package aurelienribon.tweenengine;

import java.util.ArrayList;

/**
 * A light pool of objects that can be reused to avoid allocation. Based on Nathan Sweet pool implementation
 */
abstract
class Pool<T> {
    private final ArrayList<T> objects;
    private final Callback<T> callback;

    public
    Pool(int initCapacity, Callback<T> callback) {
        this.objects = new ArrayList<T>(initCapacity);
        this.callback = callback;
    }

    protected abstract
    T create();

    public
    T get() {
        T obj = objects.isEmpty() ? create() : objects.remove(objects.size() - 1);
        if (callback != null) {
            callback.onUnPool(obj);
        }
        return obj;
    }

    public
    void free(T obj) {
        if (!objects.contains(obj)) {
            if (callback != null) {
                callback.onPool(obj);
            }
            objects.add(obj);
        }
    }

    public
    void clear() {
        objects.clear();
    }

    public
    int size() {
        return objects.size();
    }

    public
    void ensureCapacity(int minCapacity) {
        objects.ensureCapacity(minCapacity);
    }

    public
    interface Callback<T> {
        public
        void onPool(T obj);

        public
        void onUnPool(T obj);
    }
}
