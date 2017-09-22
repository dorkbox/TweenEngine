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
package dorkbox.tweenEngine;

/**
 * Base class for every paths. You can create your own paths and directly use
 * them in the Tween engine by inheriting from this class.
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public
interface TweenPath {

    /**
     * Computes the next value of the interpolation, based on its waypoints and
     * the current progress.
     *
     * @param tweenValue The progress of the interpolation, between 0 and 1. May be out
     *                   of these bounds if the easing equation involves some kind of rebounds.
     * @param points The waypoints of the tween, from start to target values.
     * @param pointsCount The number of valid points in the array.
     *
     * @return The next value of the interpolation.
     */
    float compute(final float tweenValue, final float[] points, final int pointsCount);
}
