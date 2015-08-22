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
class Bounce extends TweenEquation {
    public static final Bounce OUT = new Bounce() {
        @Override
        public final
        float compute(float t) {
            if (t < (1 / 2.75)) {
                return 7.5625f * t * t;
            }
            else if (t < (2 / 2.75)) {
                return 7.5625f * (t -= (1.5f / 2.75f)) * t + .75f;
            }
            else if (t < (2.5 / 2.75)) {
                return 7.5625f * (t -= (2.25f / 2.75f)) * t + .9375f;
            }
            else {
                return 7.5625f * (t -= (2.625f / 2.75f)) * t + .984375f;
            }
        }

        @Override
        public
        String toString() {
            return "Bounce.OUT";
        }
    };
    public static final Bounce IN = new Bounce() {
        @Override
        public final
        float compute(float t) {
            return 1 - OUT.compute(1 - t);
        }

        @Override
        public
        String toString() {
            return "Bounce.IN";
        }
    };
    public static final Bounce INOUT = new Bounce() {
        @Override
        public final
        float compute(float t) {
            if (t < 0.5f) {
                return IN.compute(t * 2) * .5f;
            }
            else {
                return OUT.compute(t * 2 - 1) * .5f + 0.5f;
            }
        }

        @Override
        public
        String toString() {
            return "Bounce.INOUT";
        }
    };
}
