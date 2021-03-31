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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.border.Border;

import dorkbox.util.swing.GroupBorder;

public class Theme {
    public static final Color MAIN_BACKGROUND = new Color(0x444444);
    public static final Color MAIN_FOREGROUND = new Color(0xF0F0F0);
    public static final Color MAIN_ALT_BACKGROUND = new Color(0x707070);
    public static final Color MAIN_ALT_FOREGROUND = new Color(0xF0F0F0);

    public static final Color HEADER_BACKGROUND = new Color(0x707070);
    public static final Color HEADER_FOREGROUND = new Color(0xF0F0F0);

    public static final Color TEXTAREA_BACKGROUND = new Color(0x333333);
    public static final Color TEXTAREA_FOREGROUND = new Color(0xF0F0F0);
    public static final Color TEXTAREA_SELECTED_BACKGROUND = new Color(0x808080);
    public static final Color TEXTAREA_SELECTED_FOREGROUND = new Color(0xF0F0F0);

    public static final Color CONSOLE_BACKGROUND = new Color(0xA5A5A5);
    public static final Color CONSOLE_FOREGROUND = new Color(0x000000);

    public static final Color SEPARATOR = new Color(0xB5B5B5);

	public static void apply(Component cmp) {
		if (cmp instanceof JComponent) {
			JComponent c = (JComponent) cmp;
			Border border = c.getBorder();
			if (border != null && border instanceof GroupBorder) {
				Font font = c.getFont();
				c.setFont(new Font(font.getFamily(), Font.BOLD, font.getSize()));
				c.setBackground(MAIN_ALT_BACKGROUND);
				c.setForeground(MAIN_ALT_FOREGROUND);
				c.setOpaque(false);
			}
		}

		if (cmp instanceof JLabel) {
			JLabel c = (JLabel) cmp;
			c.setForeground(MAIN_FOREGROUND);
		}

		if (cmp instanceof JCheckBox) {
			JCheckBox c = (JCheckBox) cmp;
			c.setForeground(MAIN_FOREGROUND);
			c.setOpaque(false);
		}

		if (cmp instanceof Container) {
			Container c = (Container) cmp;
			for (Component child : c.getComponents())
				apply(child);
		}

		if (cmp instanceof JButton) {
			JButton c = (JButton) cmp;
			c.setOpaque(false);
		}

		if (cmp instanceof JSlider) {
			JSlider c = (JSlider) cmp;
			c.setOpaque(false);
			c.setForeground(MAIN_FOREGROUND);
		}
	}
}
