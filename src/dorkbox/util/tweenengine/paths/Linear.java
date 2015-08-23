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
package dorkbox.util.tweenengine.paths;

import dorkbox.util.tweenengine.TweenPath;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public
class Linear implements TweenPath {
    @Override
    public
    float compute(float tweenValue, float[] points, int pointsCount) {
        int segment = (int) Math.floor((pointsCount - 1) * tweenValue);
        segment = Math.max(segment, 0);
        segment = Math.min(segment, pointsCount - 2);

        tweenValue = tweenValue * (pointsCount - 1) - segment;

        return points[segment] + tweenValue * (points[segment + 1] - points[segment]);
    }
}
