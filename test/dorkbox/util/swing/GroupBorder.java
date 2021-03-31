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
package dorkbox.util.swing;

import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

public class GroupBorder implements Border {
	private final int titleHeight = 20;
	private final int borderPadding = 0;
	private String title = "";

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
		Graphics2D gg = (Graphics2D) g.create();
		
		int titleW = gg.getFontMetrics().stringWidth(title) + 20;
		int titleDescent = gg.getFontMetrics().getDescent();
		
		gg.setColor(c.getBackground());

		if (!title.equals("")) {
			int[] xs = {0, titleW, titleW + titleHeight, 0};
			int[] ys = {0, 0, titleHeight, titleHeight};
			gg.fillPolygon(xs, ys, 4);
			gg.fillRect(0, titleHeight, width, height);
			gg.setColor(c.getForeground());
			gg.drawString(title, 10, titleHeight - titleDescent);
		} else {
			gg.fillRect(0, 0, width, height);
		}
		
		gg.dispose();
	}

	@Override
	public Insets getBorderInsets(Component c) {
		return new Insets(!title.equals("") ? borderPadding + titleHeight : borderPadding, borderPadding, borderPadding, borderPadding);
	}

	@Override
	public boolean isBorderOpaque() {
		return false;
	}
}
