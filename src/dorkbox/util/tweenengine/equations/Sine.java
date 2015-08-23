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
class Sine extends TweenEquation {
    private static final float PI = 3.14159265f;

    public static final Sine IN = new Sine() {
        @Override
        public
        float compute(float t) {
            return (float) -Math.cos(t * (PI / 2)) + 1;
        }

        @Override
        public
        String toString() {
            return "Sine.IN";
        }
    };

    public static final Sine OUT = new Sine() {
        @Override
        public
        float compute(float t) {
            return (float) Math.sin(t * (PI / 2));
        }

        @Override
        public
        String toString() {
            return "Sine.OUT";
        }
    };

    public static final Sine INOUT = new Sine() {
        @Override
        public
        float compute(float t) {
            return -0.5f * ((float) Math.cos(PI * t) - 1);
        }

        @Override
        public
        String toString() {
            return "Sine.INOUT";
        }
    };
}
