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
class Quint extends TweenEquation {
    public static final Quint IN = new Quint() {
        @Override
        public
        float compute(float time) {
            return time * time * time * time * time;
        }

        @Override
        public
        String toString() {
            return "Quint.IN";
        }
    };

    public static final Quint OUT = new Quint() {
        @Override
        public
        float compute(float time) {
            return (time -= 1) * time * time * time * time + 1;
        }

        @Override
        public
        String toString() {
            return "Quint.OUT";
        }
    };

    public static final Quint INOUT = new Quint() {
        @Override
        public
        float compute(float time) {
            if ((time *= 2) < 1) {
                return 0.5f * time * time * time * time * time;
            }
            return 0.5f * ((time -= 2) * time * time * time * time + 2);
        }

        @Override
        public
        String toString() {
            return "Quint.INOUT";
        }
    };
}
