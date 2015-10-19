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
package dorkbox.util.tweenengine;

/**
 * Collection of built-in paths.
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
@SuppressWarnings("NumericCastThatLosesPrecision")
public
enum TweenPaths {
	Linear(new TweenPath() {
        @Override
        public
        float compute(float tweenValue, final float[] points, final int pointsCount) {
            int segment = (int) Math.floor((pointsCount - 1) * tweenValue);
            segment = Math.max(segment, 0);
            segment = Math.min(segment, pointsCount - 2);

            tweenValue = tweenValue * (pointsCount - 1) - segment;

            return points[segment] + tweenValue * (points[segment + 1] - points[segment]);
        }
    }),
    CatmullRom(new TweenPath() {
        @Override
        public
        float compute(float tweenValue, final float[] points, final int pointsCount) {
            int segment = (int) Math.floor((pointsCount - 1) * tweenValue);
            segment = Math.max(segment, 0);
            segment = Math.min(segment, pointsCount - 2);

            tweenValue = tweenValue * (pointsCount - 1) - segment;

            if (segment == 0) {
                return catmullRomSpline(points[0], points[0], points[1], points[2], tweenValue);
            }

            if (segment == pointsCount - 2) {
                return catmullRomSpline(points[pointsCount - 3], points[pointsCount - 2], points[pointsCount - 1], points[pointsCount - 1],
                                        tweenValue);
            }

            return catmullRomSpline(points[segment - 1], points[segment], points[segment + 1], points[segment + 2], tweenValue);
        }

        private
        float catmullRomSpline(float a, float b, float c, float d, float t) {
            float t1 = (c - a) * 0.5f;
            float t2 = (d - b) * 0.5f;

            final float _t2 = t * t;
            final float _t3 = _t2 * t;

            final float _2t3 = 2 * _t3;
            final float _3t2 = 3 * _t2;

            float h1 = +_2t3 - _3t2 + 1;
            float h2 = -_2t3 + _3t2;
            float h3 = _t3 - 2 * _t2 + t;
            float h4 = _t3 - _t2;

            return b * h1 + c * h2 + t1 * h3 + t2 * h4;
        }
    });


    private transient final TweenPath path;

    TweenPaths(final TweenPath path) {
        this.path = path;
    }

    public
    TweenPath path() {
        return path;
    }
}
