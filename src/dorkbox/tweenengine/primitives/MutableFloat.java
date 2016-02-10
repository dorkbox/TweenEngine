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
package dorkbox.tweenengine.primitives;

import dorkbox.tweenengine.TweenAccessor;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public
class MutableFloat extends Number implements TweenAccessor<MutableFloat> {
    private float value;

    public
    MutableFloat(float value) {
        this.value = value;
    }

    public
    MutableFloat() {
        super();
    }

    public
    void setValue(float value) {
        this.value = value;
    }

    @Override
    public
    int intValue() {
        return (int) value;
    }

    @Override
    public
    long longValue() {
        return (long) value;
    }

    @Override
    public
    float floatValue() {
        return value;
    }

    @Override
    public
    double doubleValue() {
        return (double) value;
    }

    @Override
    public
    int getValues(MutableFloat target, int tweenType, float[] returnValues) {
        returnValues[0] = target.value;
        return 1;
    }

    @Override
    public
    void setValues(MutableFloat target, int tweenType, float[] newValues) {
        target.value = newValues[0];
    }
}
