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
 * Base class for every paths. You can create your own paths and directly use
 * them in the Tween engine by inheriting from this class.
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com
 * @author dorkbox, llc
 */
interface TweenPath {
    companion object {
        fun catmullRomSpline(a: Float, b: Float, c: Float, d: Float, t: Float): Float {
            val t1 = (c - a) * 0.5f
            val t2 = (d - b) * 0.5f

            val _t2 = t * t
            val _t3 = _t2 * t
            val _2t3 = 2 * _t3
            val _3t2 = 3 * _t2

            val h1 = +_2t3 - _3t2 + 1
            val h2 = -_2t3 + _3t2
            val h3 = _t3 - 2 * _t2 + t
            val h4 = _t3 - _t2

            return b * h1 + c * h2 + t1 * h3 + t2 * h4
        }
    }

    /**
     * Computes the next value of the interpolation, based on its waypoints and
     * the current progress.
     *
     * @param tweenValue The progress of the interpolation, between 0 and 1. May be out
     * of these bounds if the easing equation involves some kind of rebounds.
     * @param points The waypoints of the tween, from start to target values.
     * @param pointsCount The number of valid points in the array.
     *
     * @return The next value of the interpolation.
     */
    fun compute(tweenValue: Float, points: FloatArray, pointsCount: Int): Float
}
