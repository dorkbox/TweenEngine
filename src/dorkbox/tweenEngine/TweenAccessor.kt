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
package dorkbox.tweenEngine

/**
 * The TweenAccessor interface lets you interpolate any attribute from any
 * object. Just implement it as you want and register it to the engine by
 * calling [EngineBuilder.registerAccessor].
 *
 *
 * ##Example
 *
 * The following code snippet presents an example of implementation for tweening
 * a Particle class. This Particle class is supposed to only define a position
 * with an "x" and an "y" fields, and their associated getters and setters.
 *
 *
 *
 * ```
 * public class ParticleAccessor implements TweenAccessor<Particle> {
 *      public static final int X = 1;
 *      public static final int Y = 2;
 *      public static final int XY = 3;
 *
 *      public int getValues(Particle target, int tweenType, float[] returnValues) {
 *          switch (tweenType) {
 *              case X: returnValues[0] = target.getX(); return 1;
 *              case Y: returnValues[0] = target.getY(); return 1;
 *              case XY:
 *                  returnValues[0] = target.getX();
 *                  returnValues[1] = target.getY();
 *                  return 2;
 *              default: assert false; return 0;
 *          }
 *      }
 *
 *      public void setValues(Particle target, int tweenType, float[] newValues) {
 *          switch (tweenType) {
 *              case X: target.setX(newValues[0]); break;
 *              case Y: target.setY(newValues[1]); break;
 *              case XY:
 *                  target.setX(newValues[0]);
 *                  target.setY(newValues[1]);
 *                  break;
 *              default: assert false; break;
 *         }
 *      }
 * }
 * ```
 *
 * Once done, you only need to register this TweenAccessor once to be able to
 * use it for every Particle objects in your application:
 *
 *
 * ```
 * Tween.registerAccessor(Particle.class, new ParticleAccessor());
 * ```
 *
 * And that's all, the Tween Engine can now work with all your particles!
 *
 * @author Aurelien Ribon | http://www.aurelienribon.com
 * @author dorkbox, llc
 */
interface TweenAccessor<T> {
    /**
     * Gets one or many values from the target object associated to the
     * given tween type. It is used by the Tween Engine to determine starting
     * values.
     *
     * @param target The target object of the tween.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param returnValues An array which should be modified by this method.
     *
     * @return The count of modified slots from the returnValues array.
     */
    fun getValues(target: T, tweenType: Int, returnValues: FloatArray): Int

    /**
     * This method is called by the Tween Engine each time a running tween
     * associated with the current target object has been updated.
     *
     * @param target The target object of the tween.
     * @param tweenType An arbitrary number used to associate an interpolation type for a tween in the TweenAccessor get/setValues() methods
     * @param newValues The new values determined by the Tween Engine.
     */
    fun setValues(target: T, tweenType: Int, newValues: FloatArray)
}
