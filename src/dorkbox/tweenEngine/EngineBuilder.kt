/*
 * Copyright 2021 dorkbox, llc
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

package dorkbox.tweenEngine

/** Creates a TweenEngine using the builder pattern. */
class EngineBuilder internal constructor() {

    companion object {
        /** Gets the version number. */
        const val version = TweenEngine.version
    }


    private val registeredAccessors = mutableMapOf<Class<*>, TweenAccessor<*>>()
    private var threadSafe = true // by default, we are thread-safe
    private var combinedAttrsLimit = 3
    private var waypointsLimit = 0


    fun build(): TweenEngine {
        return if (threadSafe) {
            TweenEngine(true, combinedAttrsLimit, waypointsLimit, registeredAccessors)
        } else {
            // skip the volatile field access for faster performance, but NO THREAD VISIBILITY
            object : TweenEngine(false, combinedAttrsLimit, waypointsLimit, registeredAccessors) {
                override fun flushRead(): Long {
                    return 0
                }

                override fun flushWrite() {}
            }
        }
    }

    /**
     * By default, all Tweens/Timelines are thread safe for VISIBILITY. They are not thread-safe for concurrency, meaning there will be
     * race conditions if one tries to modify/read something from different threads.
     *
     * What is read/written will always be correct, but is not protected against concurrent modification.
     */
    fun unsafe(): EngineBuilder {
        threadSafe = false
        return this
    }

    /**
     * **Must be called **before** Tweens are created**
     *
     * Changes the limit for combined attributes. Defaults to 3 to reduce memory footprint.
     */
    fun setCombinedAttributesLimit(limit: Int): EngineBuilder {
        combinedAttrsLimit = limit
        return this
    }

    /**
     * **Must be called **before** Tweens are created**
     *
     * Changes the limit of allowed waypoints for each tween. Defaults to 0 to reduce memory footprint.
     */
    fun setWaypointsLimit(limit: Int): EngineBuilder {
        waypointsLimit = limit
        return this
    }

    /**
     * **Must be called **before** Tweens are created**
     *
     * Registers an accessor with the class of an object. This accessor will be used by tweens applied to every objects implementing the
     * registered class, or inheriting from it.
     *
     * @param someClass An object class.
     * @param defaultAccessor The accessor that will be used to tween any object of class "someClass".
     */
    fun registerAccessor(someClass: Class<*>, defaultAccessor: TweenAccessor<*>): EngineBuilder {
        registeredAccessors[someClass] = defaultAccessor
        return this
    }
}
