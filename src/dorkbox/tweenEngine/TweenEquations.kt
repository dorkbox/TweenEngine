/*
 * Copyright © 2001 Robert Penner
 * All rights reserved.
 *                              BSD License
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *
 *      * Neither the name of the <ORGANIZATION> nor the names of its contributors
 *        may be used to endorse or promote products derived from this software
 *        without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL <ORGANIZATION> BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Copyright 2012 Aurelien Ribon
 * Copyright 2017 Michael Pohoresk
 * Copyright 2021 dorkbox, llc
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
package dorkbox.tweenEngine

import kotlin.math.*

// float values of used constants
private const val E = 2.7182817f
private const val PI = 3.1415927f

private const val PI_DIV_2 = PI / 2f
private const val PI_DIV_6 = PI / 6f
private const val PI_DIV_18 = PI / 18f

/**
 * Easing equation based on Michael Pohoreski's work: https://github.com/Michaelangel007/easing/blob/master/js/core/easing.js
 *
 * @author dorkbox, llc
 */
@Suppress("unused", "EnumEntryName")
enum class TweenEquations(typeName: String, computeFunction: (Float) -> Float) {
    // Power -- grouped by In,Out,InOut
    None("None",           { 1f                   }), // t^0 Placeholder for no active animation
    Linear("Linear",       { t -> t               }), // t^1 In = Out = InOut

    Quad_In("Quad.IN",     { t -> t*t             }), // t^2 = Math.pow(p,2)
    Cubic_In("Cubic.IN",   { t -> t*t*t           }), // t^3 = Math.pow(p,3)
    Quart_In("Quart.IN",   { t -> t*t*t*t         }), // t^4 = Math.pow(p,4)
    Quint_In("Quint.IN",   { t -> t*t*t*t*t       }), // t^5 = Math.pow(p,5)
    Sextic_In("Sextic.IN", { t -> t*t*t*t*t*t     }), // t^6 = Math.pow(p,6)
    Septic_In("Septic.IN", { t -> t*t*t*t*t*t*t   }), // t^7 = Math.pow(p,7)
    Octic_In("Octic.IN",   { t -> t*t*t*t*t*t*t*t }), // t^8 = Math.pow(p,8)


    Quad_Out("Quad.OUT",     { t -> val m=t-1f; 1f-m*m              }),
    Cubic_Out("Cubic.OUT",   { t -> val m=t-1f; 1f+m*m*m            }),
    Quart_Out("Quart.OUT",   { t -> val m=t-1f; 1f-m*m*m*m          }),
    Quint_Out("Quint.OUT",   { t -> val m=t-1f; 1f+m*m*m*m*m        }),
    Sextic_Out("Sextic.OUT", { t -> val m=t-1f; 1f-m*m*m*m*m*m      }),
    Septic_Out("Septic.OUT", { t -> val m=t-1f; 1f+m*m*m*m*m*m*m    }),
    Octic_Out("Octic.OUT",   { t -> val m=t-1f; 1f-m*m*m*m*m*m*m*m  }),


    Quad_InOut("Quad.INOUT",     { t -> val m=t-1f; if (t<0.5f) t*t;             1f-m*m             }),
    Cubic_InOut("Cubic.INOUT",   { t -> val m=t-1f; if (t<0.5f) t*t*t;           1f+m*m*m           }),
    Quart_InOut("Quart.INOUT",   { t -> val m=t-1f; if (t<0.5f) t*t*t*t;         1f-m*m*m*m         }),
    Quint_InOut("Quint.INOUT",   { t -> val m=t-1f; if (t<0.5f) t*t*t*t*t;       1f+m*m*m*m*m       }),
    Sextic_InOut("Sextic.INOUT", { t -> val m=t-1f; if (t<0.5f) t*t*t*t*t*t;     1f-m*m*m*m*m*m     }),
    Septic_InOut("Septic.INOUT", { t -> val m=t-1f; if (t<0.5f) t*t*t*t*t*t*t;   1f+m*m*m*m*m*m*m   }),
    Octic_InOut("Octic.INOUT",   { t -> val m=t-1f; if (t<0.5f) t*t*t*t*t*t*t*t; 1f-m*m*m*m*m*m*m*m }),


    // Standard -- grouped by Type
    Back_In("Back.IN",       { t ->                         val k=1.70158f;                    t*t*(t*(k+1f) - k)                                   }),
    Back_InOut("Back.INOUT", { t -> val m=t-1f; val n=t*2f; val k=1.70158f*1.525f; if (t<0.5f) t*n*(n*(k+1f) - k); else 1f+2f*m*m*(2f*m*(k+1f) + k) }), // This can go negative! (i.e. t = 0.008)
    Back_Out("Back.OUT",     { t -> val m=t-1f;             val k=1.70158f;                                             1f+   m*m*(   m*(k+1f) + k) }),


    Bounce_In("Bounce.IN", { t -> 1f - Bounce_Out.compute(1f-t) }),
    Bounce_InOut("Bounce.INOUT", { time ->
        val n = time * 2f
        if (n < 1f) 0.5f - 0.5f * Bounce_Out.compute(1f-n)
        else        0.5f + 0.5f * Bounce_Out.compute(n-1f)      }),
    Bounce_Out("Bounce.OUT", { t ->
        val r = 1f / 2.75f  // reciprocal
        val k0 = 7.5625f
        val k1 = 1f     * r // 36.36%
        val k2 = 2f     * r // 72.72%
        val k3 = 1.5f   * r // 54.54%
        val k4 = 2.5f   * r // 90.90%
        val k5 = 2.25f  * r // 81.81%
        val k6 = 2.625f * r // 95.45%

        when {
            t < k1 -> {             k0 * t*t              }
            t < k2 -> { val n=t-k3; k0 * n*n + 0.75f      } // 48/64
            t < k4 -> { val n=t-k5; k0 * n*n + 0.9375f    } // 60/64
            else ->   { val n=t-k6; k0 * n*n + 0.984375f; } // 63/64
        } }),


    Circle_In("Circle.IN",       { t ->                                        1f - sqrt(1f - t*t)                                                 }),
    Circle_InOut("Circle.INOUT", { t -> val m = t-1f; val n=t*2f; if (n < 1f) (1f - sqrt(1f - n*n)) * 0.5f; else (sqrt(1f - 4 * m*m) + 1f) * 0.5f  }),
    Circle_Out("Circle.OUT",     { t -> val m = t-1f;                                                             sqrt(1f -     m*m)               }),


    Elastic_In("Elastic.IN", { t -> val m=t-1f; -(2f.pow(10f*m) * sin(((m*40f-3f) * PI_DIV_6))) }), // 40/6 = 6.666... = 2/0.3 = PI/6;
    Elastic_InOut("Elastic.INOUT", { t ->
        val s = 2f*t-1f                // remap: [0,0.5] -> [-1,0]
        val k = (80f*s-9f) * PI_DIV_18 // and    [0.5,1] -> [0,+1]

        if (s < 0f)  -0.5f * 2f.pow( 10f*s) * sin(k)
        else       1 + .5f * 2f.pow(-10f*s) * sin(k)                                    }),
    Elastic_Out("Elastic.OUT", { t -> (1f+2f.pow(10f*-t) * sin((-t*40f-3f) * PI_DIV_6)) }),


    Expo_In("Exponent.IN",       { t -> 2f.pow(10f*(t-1f))    }),
    Expo_InOut("Exponent.INOUT", { t ->
        if (t < 0.5f)      2f.pow( 10f*(2f*t-1f) - 1f)
        else          1f - 2f.pow(-10f*(2f*t-1f) - 1f)        }),
    Expo_Out("Exponent.OUT", { t -> 1f - 2f.pow(-10f*t)       }),


    Sine_In("Sine.IN",       { t ->         1f - cos(t * PI_DIV_2) }),
    Sine_InOut("Sine.INOUT", { t -> 0.5f * (1f - cos(t * PI))      }),
    Sine_Out("Sine.OUT",     { t ->              sin(t * PI_DIV_2) }),


    // Non-Standard
    ExponentE_In("ExponentE.IN",       { t -> E.pow(-10f * (1 - t))             }), // Scale 0..1 -> t^-10 .. t^0
    ExponentE_InOut("ExponentE.INOUT", { t ->
        val n = t*2
        if (n < 1f) 0.5f - 0.5f*ExponentE_Out.compute(1f-n)
                    0.5f + 0.5f*ExponentE_Out.compute(n-1f)                     }),
    ExponentE_Out("ExponentE.OUT",     { t -> 1f - ExponentE_In.compute(1f-t)   }),


    Log_In("Log.IN",       { t -> 1f - Log_Out.compute(1f-t)    }),
    Log_InOut("Log.INOUT", { t ->
        val n = t*2
        if (n < 1) 0.5f - 0.5f*Log_Out.compute(1f-n)
                   0.5f + 0.5f*Log_Out.compute(n-1f)             }),
    Log_Out("Log.OUT",     { t -> log10((t * 9.0) + 1).toFloat() }), // Scale 0..1 -> Log10( 1 ) .. Log10( 10 )


    SquareRoot_In("SquareRoot.IN",       { t -> 1f - SquareRoot_Out.compute(1f-t) }),
    SquareRoot_InOut("SquareRoot.INOUT", { t ->
        val n = t*2f
        if (n < 1f) 0.5f - 0.5f*SquareRoot_Out.compute(1f-n)
                    0.5f + 0.5f*SquareRoot_Out.compute(n-1f)  }),
    SquareRoot_Out("SquareRoot.OUT",     { t -> sqrt(t)       }),


    Smoothstep_In("Smoothstep.IN",       { t -> if (t<0.5f)        t*t*(3f-2f*t) else t }),
    Smoothstep_InOut("Smoothstep.INOUT", { t ->                    t*t*(3f-2f*t)        }),
    Smoothstep_Out("Smoothstep.OUT",     { t -> if (t<0.5f) t else t*t*(3f-2f*t)        }),

    ;

    val equation: TweenEquation = InnerTweenEq(typeName, computeFunction)

    companion object {
        /**
         * Takes an easing name and gives you the corresponding TweenEquation. You probably won't need this, but tools will love that.
         *
         * @param name The name of an easing, like "Quad.INOUT".
         *
         * @return The parsed equation, or null if there is no match.
         */
        fun parse(name: String): TweenEquation? {
            val values = values()
            var i = 0
            val n = values.size

            while (i < n) {
                val equation = values[i].equation

                if (name == equation.name()) {
                    return equation
                }
                i++
            }

            return null
        }

        private class InnerTweenEq(val name: String, val computeFunction: (Float) -> Float) : TweenEquation {
            override fun compute(time: Float): Float {
                return computeFunction(time)
            }

            override fun name(): String {
                return name
            }
        }
    }

    /**
     * Computes the next value of the interpolation.
     *
     * @param time The current time, between 0 and 1.
     *
     * @return The corresponding value.
     */
    fun compute(time: Float): Float {
        return equation.compute(time)
    }

    override fun toString(): String {
        return equation.name()
    }
}
