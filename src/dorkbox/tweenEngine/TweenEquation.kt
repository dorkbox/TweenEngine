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
package dorkbox.tweenEngine

/**
 * Base interface for every easing equation. You can create your own equations
 * and directly use them in the Tween engine by implementing this.
 *
 * @see Tween
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com
 * @author dorkbox, llc
 */
interface TweenEquation {
    /**
     * Computes the next value of the interpolation.
     *
     * @param time The current time, between 0 and 1.
     *
     * @return The corresponding value.
     */
    fun compute(time: Float): Float

    /**
     * @return the assign name of this TweenEquation, used for serialization
     */
    fun name(): String
}
