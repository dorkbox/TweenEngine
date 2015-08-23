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
@SuppressWarnings("unused")
public abstract
class Elastic extends TweenEquation {
    private static final float PI = 3.14159265f;

    public static final Elastic IN = new Elastic() {
        @Override
        public
        float compute(float t) {
            float a = param_a;
            float p = param_p;
            if (t == 0) {
                return 0;
            }
            if (t == 1) {
                return 1;
            }
            if (!setP) {
                p = .3f;
            }
            float s;
            if (!setA || a < 1) {
                a = 1;
                s = p / 4;
            }
            else {
                s = p / (2 * PI) * (float) Math.asin(1 / a);
            }
            return -(a * (float) Math.pow(2, 10 * (t -= 1)) * (float) Math.sin((t - s) * (2 * PI) / p));
        }

        @Override
        public
        String toString() {
            return "Elastic.IN";
        }
    };

    public static final Elastic OUT = new Elastic() {
        @Override
        public
        float compute(float t) {
            float a = param_a;
            float p = param_p;
            if (t == 0) {
                return 0;
            }
            if (t == 1) {
                return 1;
            }
            if (!setP) {
                p = .3f;
            }
            float s;
            if (!setA || a < 1) {
                a = 1;
                s = p / 4;
            }
            else {
                s = p / (2 * PI) * (float) Math.asin(1 / a);
            }
            return a * (float) Math.pow(2, -10 * t) * (float) Math.sin((t - s) * (2 * PI) / p) + 1;
        }

        @Override
        public
        String toString() {
            return "Elastic.OUT";
        }
    };

    public static final Elastic INOUT = new Elastic() {
        @Override
        public
        float compute(float t) {
            float a = param_a;
            float p = param_p;
            if (t == 0) {
                return 0;
            }
            if ((t *= 2) == 2) {
                return 1;
            }
            if (!setP) {
                p = .3f * 1.5f;
            }
            float s;
            if (!setA || a < 1) {
                a = 1;
                s = p / 4;
            }
            else {
                s = p / (2 * PI) * (float) Math.asin(1 / a);
            }
            if (t < 1) {
                return -.5f * (a * (float) Math.pow(2, 10 * (t -= 1)) * (float) Math.sin((t - s) * (2 * PI) / p));
            }
            return a * (float) Math.pow(2, -10 * (t -= 1)) * (float) Math.sin((t - s) * (2 * PI) / p) * .5f + 1;
        }

        @Override
        public
        String toString() {
            return "Elastic.INOUT";
        }
    };

    // -------------------------------------------------------------------------

    protected float param_a;
    protected float param_p;
    protected boolean setA = false;
    protected boolean setP = false;

    public
    Elastic a(float a) {
        param_a = a;
        this.setA = true;
        return this;
    }

    public
    Elastic p(float p) {
        param_p = p;
        this.setP = true;
        return this;
    }
}
