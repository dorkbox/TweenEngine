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
package dorkbox.demo.applets

import dorkbox.util.swing.GroupBorder
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Font
import javax.swing.*

object Theme {
	val MAIN_BACKGROUND = Color(0x444444)
	val MAIN_FOREGROUND = Color(0xF0F0F0)
    val MAIN_ALT_BACKGROUND = Color(0x707070)
    val MAIN_ALT_FOREGROUND = Color(0xF0F0F0)
    val HEADER_BACKGROUND = Color(0x707070)
    val HEADER_FOREGROUND = Color(0xF0F0F0)
    val TEXTAREA_BACKGROUND = Color(0x333333)
    val TEXTAREA_FOREGROUND = Color(0xF0F0F0)
    val TEXTAREA_SELECTED_BACKGROUND = Color(0x808080)
    val TEXTAREA_SELECTED_FOREGROUND = Color(0xF0F0F0)
    val CONSOLE_BACKGROUND = Color(0xA5A5A5)
    val CONSOLE_FOREGROUND = Color(0x000000)
    val SEPARATOR = Color(0xB5B5B5)

	fun apply(cmp: Component?) {
        if (cmp is JComponent) {
            val c = cmp
            val border = c.border
            if (border != null && border is GroupBorder) {
                val font = c.font
                c.font = Font(font.family, Font.BOLD, font.size)
                c.background = MAIN_ALT_BACKGROUND
                c.foreground = MAIN_ALT_FOREGROUND
                c.isOpaque = false
            }
        }
        if (cmp is JLabel) {
            cmp.foreground = MAIN_FOREGROUND
        }
        if (cmp is JCheckBox) {
            val c = cmp
            c.foreground = MAIN_FOREGROUND
            c.isOpaque = false
        }
        if (cmp is Container) {
            for (child in cmp.components) apply(child)
        }
        if (cmp is JButton) {
            cmp.isOpaque = false
        }
        if (cmp is JSlider) {
            val c = cmp
            c.isOpaque = false
            c.foreground = MAIN_FOREGROUND
        }
    }
}
