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
package dorkbox.demo.applets;

import dorkbox.tweenEngine.TweenAccessor;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 */
public class SpriteAccessor implements TweenAccessor<Sprite> {
	public static final int POSITION_XY = 1;
	public static final int SCALE_XY = 2;
	public static final int VISIBILITY = 3;

	@Override
	public int getValues(Sprite target, int tweenType, float[] returnValues) {
		switch (tweenType) {
			case POSITION_XY:
				returnValues[0] = target.getX();
				returnValues[1] = target.getY();
				return 2;

			case SCALE_XY:
				returnValues[0] = target.getScaleX();
				returnValues[1] = target.getScaleY();
				return 2;

			case VISIBILITY:
				returnValues[0] = target.isVisible() ? 1 : 0;
				return 1;

			default: assert false; return -1;
		}
	}

	@Override
	public void setValues(Sprite target, int tweenType, float[] newValues) {
		switch (tweenType) {
			case POSITION_XY: target.setPosition(newValues[0], newValues[1]); break;
			case SCALE_XY: target.setScale(newValues[0], newValues[1]); break;
			case VISIBILITY: target.setVisible(newValues[0] > 0); break;
			default: assert false;
		}
	}
}
