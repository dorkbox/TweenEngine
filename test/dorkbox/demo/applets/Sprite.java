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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 */
public class Sprite {
	private BufferedImage image;
	private float x = 0;
	private float y = 0;
	private float scaleX = 1;
	private float scaleY = 1;
	private boolean isCentered = true;
	private boolean isVisible = true;

	public Sprite(String gfxName) {
		try {
			image = ImageIO.read(Sprite.class.getResource("gfx/" + gfxName));
		} catch (IOException ex) {
            ex.printStackTrace();
		}
	}

	public void draw(Graphics2D gg) {
		if (!isVisible) return;
		gg = (Graphics2D) gg.create();
		gg.translate(x, y);
		gg.scale(scaleX, scaleY);
		gg.drawImage(image, null, isCentered ? -image.getWidth()/2 : 0, isCentered ? -image.getHeight()/2 : 0);
		gg.dispose();
	}

	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void setScale(float scaleX, float scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
	}

	public Sprite setCentered(boolean isCentered) {
		this.isCentered = isCentered;
		return this;
	}

	public void setVisible(boolean isVisible) {
		this.isVisible = isVisible;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getScaleX() {
		return scaleX;
	}

	public float getScaleY() {
		return scaleY;
	}

	public boolean isVisible() {
		return isVisible;
	}
}
