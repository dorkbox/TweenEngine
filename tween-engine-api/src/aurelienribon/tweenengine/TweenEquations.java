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
package aurelienribon.tweenengine;

import aurelienribon.tweenengine.equations.*;

/**
 * Collection of built-in easing equations
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
@SuppressWarnings("unused")
public
interface TweenEquations {
    Linear easeNone = Linear.INOUT;
    Quad easeInQuad = Quad.IN;
    Quad easeOutQuad = Quad.OUT;
    Quad easeInOutQuad = Quad.INOUT;
    Cubic easeInCubic = Cubic.IN;
    Cubic easeOutCubic = Cubic.OUT;
    Cubic easeInOutCubic = Cubic.INOUT;
    Quart easeInQuart = Quart.IN;
    Quart easeOutQuart = Quart.OUT;
    Quart easeInOutQuart = Quart.INOUT;
    Quint easeInQuint = Quint.IN;
    Quint easeOutQuint = Quint.OUT;
    Quint easeInOutQuint = Quint.INOUT;
    Circ easeInCirc = Circ.IN;
    Circ easeOutCirc = Circ.OUT;
    Circ easeInOutCirc = Circ.INOUT;
    Sine easeInSine = Sine.IN;
    Sine easeOutSine = Sine.OUT;
    Sine easeInOutSine = Sine.INOUT;
    Expo easeInExpo = Expo.IN;
    Expo easeOutExpo = Expo.OUT;
    Expo easeInOutExpo = Expo.INOUT;
    Back easeInBack = Back.IN;
    Back easeOutBack = Back.OUT;
    Back easeInOutBack = Back.INOUT;
    Bounce easeInBounce = Bounce.IN;
    Bounce easeOutBounce = Bounce.OUT;
    Bounce easeInOutBounce = Bounce.INOUT;
    Elastic easeInElastic = Elastic.IN;
    Elastic easeOutElastic = Elastic.OUT;
    Elastic easeInOutElastic = Elastic.INOUT;
}
