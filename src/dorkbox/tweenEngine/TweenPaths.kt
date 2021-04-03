/*
 * Copyright 2012 Aurelien Ribon
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
package dorkbox.tweenEngine

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Collection of built-in paths.
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
enum class TweenPaths(computeFunction: (Float, FloatArray, Int) -> Float) {
    Linear({ tweenValue: Float, points: FloatArray, pointsCount: Int ->
            var segment = floor(((pointsCount - 1) * tweenValue)).toInt()
            segment = max(segment, 0)
            segment = min(segment, pointsCount - 2)

            val value = tweenValue * (pointsCount - 1) - segment
            points[segment] + value * (points[segment + 1] - points[segment])
        }),

    CatmullRom({ tweenValue: Float, points: FloatArray, pointsCount: Int ->
        var segment = floor(((pointsCount - 1) * tweenValue)).toInt()
        segment = max(segment, 0)
        segment = min(segment, pointsCount - 2)

        val value = tweenValue * (pointsCount - 1) - segment
        when (segment) {
            0 -> {
                catmullRomSpline(
                    points[0],
                    points[0],
                    points[1],
                    points[2],
                    value)
            }
            pointsCount - 2 -> {
                catmullRomSpline(
                    points[pointsCount - 3],
                    points[pointsCount - 2],
                    points[pointsCount - 1],
                    points[pointsCount - 1],
                    value
                )
            }
            else -> catmullRomSpline(
                points[segment - 1],
                points[segment],
                points[segment + 1],
                points[segment + 2],
                value
            )
        }
    })

    ;

    val path: TweenPath = InnerTweenPath(computeFunction)

    companion object {
        private fun catmullRomSpline(a: Float, b: Float, c: Float, d: Float, t: Float): Float {
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

        private class InnerTweenPath(val computeFunction: (Float, FloatArray, Int) -> Float) : TweenPath {
            override fun compute(tweenValue: Float, points: FloatArray, pointsCount: Int): Float {
                return computeFunction(tweenValue, points, pointsCount)
            }
        }
    }
}
