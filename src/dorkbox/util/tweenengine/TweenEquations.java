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
 *
 *
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
 * Easing equation based on Robert Penner's work: http://robertpenner.com/easing/
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
@SuppressWarnings("unused")
public
enum TweenEquations {
    // Linear (just to prevent confusion, same as none)
    Linear(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return time;
        }

        @Override
        public
        String toString() {
            return "Linear.INOUT";
        }
    }),


    // Quad based
    Quad_In(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            return time * time;
        }

        @Override
        public
        String toString() {
            return "Quad.IN";
        }
    }),
    Quad_Out(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            return -time * (time - 2F);
        }

        @Override
        public
        String toString() {
            return "Quad.OUT";
        }
    }),
    Quad_InOut(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            if ((time *= 2F) < 1F) {
                return 0.5F * time * time;
            }
            return -0.5F * ((--time) * (time - 2F) - 1F);
        }

        @Override
        public
        String toString() {
            return "Quad.INOUT";
        }
    }),


    // Cubic
    Cubic_In(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return time * time * time;
        }

        @Override
        public
        String toString() {
            return "Cubic.IN";
        }
    }),
    Cubic_Out(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return (time -= 1F) * time * time + 1F;
        }

        @Override
        public
        String toString() {
            return "Cubic.OUT";
        }
    }),
    Cubic_InOut(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            if ((time *= 2F) < 1F) {
                return 0.5F * time * time * time;
            }
            return 0.5F * ((time -= 2F) * time * time + 2F);
        }

        @Override
        public
        String toString() {
            return "Cubic.INOUT";
        }
    }),


    // Quart
    Quart_In(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return time * time * time * time;
        }

        @Override
        public
        String toString() {
            return "Quart.IN";
        }
    }),
    Quart_Out(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return -((time -= 1F) * time * time * time - 1F);
        }

        @Override
        public
        String toString() {
            return "Quart.OUT";
        }
    }),
    Quart_InOut(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            if ((time *= 2F) < 1F) {
                return 0.5F * time * time * time * time;
            }
            return -0.5F * ((time -= 2F) * time * time * time - 2F);
        }

        @Override
        public
        String toString() {
            return "Quart.INOUT";
        }
    }),


    // Quint
    Quint_In(new TweenEquation() {
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
    }),
    Quint_Out(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return (time -= 1F) * time * time * time * time + 1F;
        }

        @Override
        public
        String toString() {
            return "Quint.OUT";
        }
    }),
    Quint_InOut(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            if ((time *= 2F) < 1F) {
                return 0.5F * time * time * time * time * time;
            }
            return 0.5F * ((time -= 2F) * time * time * time * time + 2F);
        }

        @Override
        public
        String toString() {
            return "Quint.INOUT";
        }
    }),


    // Circle
    Circle_In(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            return (float) -Math.sqrt(1F - time * time) - 1F;
        }

        @Override
        public
        String toString() {
            return "Circle.IN";
        }
    }),
    Circle_Out(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            return (float) Math.sqrt(1F - (time -= 1F) * time);
        }

        @Override
        public
        String toString() {
            return "Circle.OUT";
        }
    }),
    Circle_InOut(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            if ((time *= 2F) < 1F) {
                return -0.5F * ((float) Math.sqrt(1F - time * time) - 1F);
            }
            return 0.5F * ((float) Math.sqrt(1F - (time -= 2F) * time) + 1F);
        }

        @Override
        public
        String toString() {
            return "Circle.INOUT";
        }
    }),


    // Sine
    Sine_In(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return (float) -Math.cos(time * (1.57079633F)) + 1F; // PI/2
        }

        @Override
        public
        String toString() {
            return "Sine.IN";
        }
    }),
    Sine_Out(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return (float) Math.sin(time * (1.57079633F));
        } // PI/2

        @Override
        public
        String toString() {
            return "Sine.OUT";
        }
    }),
    Sine_InOut(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return -0.5F * ((float) Math.cos(3.14159265F * time) - 1F);
        }

        @Override
        public
        String toString() {
            return "Sine.INOUT";
        }
    }),


    // Exponential
    Expo_In(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return (time == 0F) ? 0F : (float) Math.pow(2, 10F * (time - 1F));
        }

        @Override
        public
        String toString() {
            return "Expo.IN";
        }
    }),
    Expo_Out(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            return (time == 1F) ? 1F : -(float) Math.pow(2, -10F * time) + 1F;
        }

        @Override
        public
        String toString() {
            return "Expo.OUT";
        }
    }),
    Expo_InOut(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            if (time == 0F) {
                return 0F;
            }
            if (time == 1F) {
                return 1F;
            }
            if ((time *= 2F) < 1F) {
                return 0.5F * (float) Math.pow(2, 10F * (time - 1F));
            }
            return 0.5F * (-(float) Math.pow(2, -10F * --time) + 2F);
        }

        @Override
        public
        String toString() {
            return "Expo.INOUT";
        }
    }),


    // Back
    Back_In(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            final float s = 1.70158F;
            return time * time * ((s + 1F) * time - s);
        }

        @Override
        public
        String toString() {
            return "Back.IN";
        }
    }),
    Back_Out(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            final float s = 1.70158F;
            return (time -= 1F) * time * ((s + 1F) * time + s) + 1F;
        }

        @Override
        public
        String toString() {
            return "Back.OUT";
        }
    }),
    Back_InOut(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            float s = 1.70158F;
            if ((time *= 2F) < 1F) {
                return 0.5F * (time * time * (((s *= (1.525F)) + 1F) * time - s));
            }
            return 0.5F * ((time -= 2F) * time * (((s *= (1.525F)) + 1F) * time + s) + 2F);
        }

        @Override
        public
        String toString() {
            return "Back.INOUT";
        }
    }),


    // Bounce
    Bounce_In(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            return 1F - Bounce_Out.equation.compute(1F - time);
        }

        @Override
        public
        String toString() {
            return "Bounce.IN";
        }
    }),
    Bounce_Out(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            if (time < (0.36363636F)) {   // 1 / 2.75
                return 7.5625F * time * time;
            }
            else if (time < (0.72727273F)) {   // 2 / 2.75
                return 7.5625F * (time -= (0.54545455F)) * time + .75F;  // 1.5 / 2.75
            }
            else if (time < (0.90909091F)) {  // 2.5 / 2.75
                return 7.5625F * (time -= (0.81818182F)) * time + .9375F;  // 2.25 / 2.75
            }
            else {
                return 7.5625F * (time -= (0.95454545F)) * time + .984375F;  // 2.625 / 2.75
            }
        }

        @Override
        public
        String toString() {
            return "Bounce.OUT";
        }
    }),
    Bounce_InOut(new TweenEquation() {
        @Override
        public final
        float compute(float time) {
            if (time < 0.5F) {
                return Bounce_In.equation.compute(time * 2F) * .5F;
            }
            else {
                return Bounce_Out.equation.compute(time * 2F - 1F) * .5F + 0.5F;
            }
        }

        @Override
        public
        String toString() {
            return "Bounce.INOUT";
        }
    }),


    // Elastic
    Elastic_In(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            float a;
            float p;
            if (time == 0F) {
                return 0F;
            }
            if (time == 1F) {
                return 1F;
            }
            //if (!setP) {                // Left the original algorithm in place, where 'a' & 'p' are parameters that can be set.
                p = 0.3F;
            //}
            float s;
            //if (!setA || a < 1) {
                a = 1F;
                //s = p / 4F;
                s = 0.075F;
            //}
            //else {
            //    s = p / (6.28318531F) * (float) Math.asin(1 / a);  // 2*PI
            //}

            return -(a * (float) Math.pow(2, 10F * (time -= 1F)) * (float) Math.sin((time - s) * (6.28318531F) / p)); // 2*PI
        }

        @Override
        public
        String toString() {
            return "Elastic.IN";
        }
    }),
    Elastic_Out(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            float a;
            float p;
            if (time == 0F) {
                return 0F;
            }
            if (time == 1F) {
                return 1F;
            }
            //if (!setP) {                  // Left the original algorithm in place, where 'a' & 'p' are parameters that can be set.
                p = 0.3F;
            //}
            float s;
            //if (!setA || a < 1) {
                a = 1F;
                //s = p / 4F;
                s = 0.075F;
            //}
            //else {
            //    s = p / (6.28318531F) * (float) Math.asin(1 / a); // 2*PI
            //}
            return a * (float) Math.pow(2, -10F * time) * (float) Math.sin((time - s) * (6.28318531F) / p) + 1F; // 2*PI
        }

        @Override
        public
        String toString() {
            return "Elastic.OUT";
        }
    }),
    Elastic_InOut(new TweenEquation() {
        @Override
        public
        float compute(float time) {
            float a;
            float p;
            if (time == 0F) {
                return 0F;
            }
            if ((time *= 2F) == 2F) {
                return 1F;
            }
            // if (!setP) {            // Left the original algorithm in place, where 'a' & 'p' are parameters that can be set.
                // p = .3F * 1.5F;
                p = 0.45F;
            //}
            float s;
            //if (!setA || a < 1) {
                a = 1F;
                //s = p / 4F;
                s = 0.1125F;
            //}
            //else {
            //    s = p / (6.28318531F) * (float) Math.asin(1 / a); // 2*PI
            //}
            if (time < 1F) {
                return -.5F * (a * (float) Math.pow(2, 10F * (time -= 1F)) * (float) Math.sin((time - s) * (6.28318531F) / p)); // 2*PI
            }
            return a * (float) Math.pow(2, -10F * (time -= 1F)) * (float) Math.sin((time - s) * (6.28318531F) / p) * .5F + 1F; // 2*PI
        }

        @Override
        public
        String toString() {
            return "Elastic.INOUT";
        }
    }),
    ;

    private transient final TweenEquation equation;

    TweenEquations(final TweenEquation equation) {
        this.equation = equation;
    }

    /**
     * Takes an easing name and gives you the corresponding TweenEquation.
     * You probably won't need this, but tools will love that.
     *
     * @param name The name of an easing, like "Quad.INOUT".
     * @return The parsed equation, or null if there is no match.
     */
    public static TweenEquation parse(String name) {
        TweenEquations[] values = TweenEquations.values();

        for (int i = 0, n = values.length; i < n; i++) {
            if (name.equals(values[i].toString())) {
                return values[i].equation;
            }
        }

        return null;
    }

    public
    TweenEquation getEquation() {
        return equation;
    }

    /**
     * Computes the next value of the interpolation.
     *
     * @param time The current time, between 0 and 1.
     * @return The corresponding value.
     */
    public
    float compute(float time) {
        return equation.compute(time);
    }

    @Override
    public
    String toString() {
        return equation.toString();
    }
}
