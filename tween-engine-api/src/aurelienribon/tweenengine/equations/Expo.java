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
package aurelienribon.tweenengine.equations;

import aurelienribon.tweenengine.TweenEquation;

/**
 * Easing equation based on Robert Penner's work: http://robertpenner.com/easing/
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public abstract
class Expo extends TweenEquation {
    public static final Expo IN = new Expo() {
        @Override
        public
        float compute(float t) {
            return (t == 0) ? 0 : (float) Math.pow(2, 10 * (t - 1));
        }

        @Override
        public
        String toString() {
            return "Expo.IN";
        }
    };

    public static final Expo OUT = new Expo() {
        @Override
        public
        float compute(float t) {
            return (t == 1) ? 1 : -(float) Math.pow(2, -10 * t) + 1;
        }

        @Override
        public
        String toString() {
            return "Expo.OUT";
        }
    };

    public static final Expo INOUT = new Expo() {
        @Override
        public
        float compute(float t) {
            if (t == 0) {
                return 0;
            }
            if (t == 1) {
                return 1;
            }
            if ((t *= 2) < 1) {
                return 0.5f * (float) Math.pow(2, 10 * (t - 1));
            }
            return 0.5f * (-(float) Math.pow(2, -10 * --t) + 2);
        }

        @Override
        public
        String toString() {
            return "Expo.INOUT";
        }
    };
}
