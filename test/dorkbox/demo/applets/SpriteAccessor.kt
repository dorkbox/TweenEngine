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
package dorkbox.demo.applets

import dorkbox.tweenEngine.TweenAccessor

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 */
class SpriteAccessor : TweenAccessor<Sprite> {
    override fun getValues(target: Sprite, tweenType: Int, returnValues: FloatArray): Int {
        return when (tweenType) {
            POSITION_XY -> {
                returnValues[0] = target.x
                returnValues[1] = target.y
                2
            }

            SCALE_XY -> {
                returnValues[0] = target.scaleX
                returnValues[1] = target.scaleY
                2
            }

            VISIBILITY -> {
                returnValues[0] = (if (target.isVisible) 1 else 0).toFloat()
                1
            }

            else -> {
                assert(false)
                -1
            }
        }
    }

    override fun setValues(target: Sprite, tweenType: Int, newValues: FloatArray) {
        when (tweenType) {
            POSITION_XY -> target.setPosition(newValues[0], newValues[1])
            SCALE_XY -> target.setScale(newValues[0], newValues[1])
            VISIBILITY -> target.isVisible = newValues[0] > 0
            else -> assert(false)
        }
    }

    companion object {
        const val POSITION_XY = 1
        const val SCALE_XY = 2
        const val VISIBILITY = 3
    }
}
