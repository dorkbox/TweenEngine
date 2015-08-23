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
package dorkbox.util.tweenengine.equations;

import dorkbox.util.tweenengine.TweenEquation;

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
        float compute(float time) {
            if (time < (1 / 2.75)) {
                return 7.5625f * time * time;
            }
            else if (time < (2 / 2.75)) {
                return 7.5625f * (time -= (1.5f / 2.75f)) * time + .75f;
            }
            else if (time < (2.5 / 2.75)) {
                return 7.5625f * (time -= (2.25f / 2.75f)) * time + .9375f;
            }
            else {
                return 7.5625f * (time -= (2.625f / 2.75f)) * time + .984375f;
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
        float compute(float time) {
            return 1 - OUT.compute(1 - time);
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
        float compute(float time) {
            if (time < 0.5f) {
                return IN.compute(time * 2) * .5f;
            }
            else {
                return OUT.compute(time * 2 - 1) * .5f + 0.5f;
            }
        }

        @Override
        public
        String toString() {
            return "Bounce.INOUT";
        }
    };
}
