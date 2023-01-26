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
package dorkbox.util.swing

import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import javax.swing.border.Border

class GroupBorder : Border {
    private val titleHeight = 20
    private val borderPadding = 0
    @JvmField
	var title = ""
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
        val gg = g.create() as Graphics2D
        val titleW = gg.fontMetrics.stringWidth(title) + 20
        val titleDescent = gg.fontMetrics.descent
        gg.color = c.background
        if (title != "") {
            val xs = intArrayOf(0, titleW, titleW + titleHeight, 0)
            val ys = intArrayOf(0, 0, titleHeight, titleHeight)
            gg.fillPolygon(xs, ys, 4)
            gg.fillRect(0, titleHeight, width, height)
            gg.color = c.foreground
            gg.drawString(title, 10, titleHeight - titleDescent)
        } else {
            gg.fillRect(0, 0, width, height)
        }
        gg.dispose()
    }

    override fun getBorderInsets(c: Component): Insets {
        return Insets(if (title != "") borderPadding + titleHeight else borderPadding, borderPadding, borderPadding, borderPadding)
    }

    override fun isBorderOpaque(): Boolean {
        return false
    }
}
