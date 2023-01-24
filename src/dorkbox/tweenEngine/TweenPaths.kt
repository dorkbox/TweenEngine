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

import dorkbox.tweenEngine.TweenPath.Companion.catmullRomSpline
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

    }
}
