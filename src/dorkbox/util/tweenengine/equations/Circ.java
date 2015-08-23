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
class Circ extends TweenEquation {
    public static final Circ IN = new Circ() {
        @Override
        public final
        float compute(float t) {
            return (float) -Math.sqrt(1 - t * t) - 1;
        }

        @Override
        public
        String toString() {
            return "Circ.IN";
        }
    };

    public static final Circ OUT = new Circ() {
        @Override
        public final
        float compute(float t) {
            return (float) Math.sqrt(1 - (t -= 1) * t);
        }

        @Override
        public
        String toString() {
            return "Circ.OUT";
        }
    };

    public static final Circ INOUT = new Circ() {
        @Override
        public final
        float compute(float t) {
            if ((t *= 2) < 1) {
                return -0.5f * ((float) Math.sqrt(1 - t * t) - 1);
            }
            return 0.5f * ((float) Math.sqrt(1 - (t -= 2) * t) + 1);
        }

        @Override
        public
        String toString() {
            return "Circ.INOUT";
        }
    };
}
