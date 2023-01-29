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

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.TexturePaint
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.logging.*
import javax.imageio.ImageIO
import javax.swing.JPanel

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 * @author dorkbox, llc
 */
class ImagePanel : JPanel() {
    private var background: BufferedImage? = null
    private var image: BufferedImage? = null
    private var useRegion = false
    private var x = 0
    private var y = 0
    private var width = 0
    private var height = 0
    fun setBackground(bgURL: URL?) {
        try {
            background = ImageIO.read(bgURL)
        } catch (ex: IOException) {
        }
    }

    fun clearImage() {
        image = null
        repaint()
    }

    fun setImage(img: BufferedImage?) {
        image = img
        repaint()
    }

    fun setImage(img: File?) {
        setImage(img, 0, 0, 0, 0)
        useRegion = false
    }

    fun setImage(imgUrl: URL?) {
        setImage(imgUrl, 0, 0, 0, 0)
        useRegion = false
    }

    fun setImage(img: File?, x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        useRegion = true
        try {
            image = if (img != null) ImageIO.read(img) else null
            repaint()
        } catch (ex: IOException) {
            Logger.getLogger(ImagePanel::class.java.name).log(Level.SEVERE, null, ex)
        }
    }

    fun setImage(imgUrl: URL?, x: Int, y: Int, width: Int, height: Int) {
        this.x = x
        this.y = y
        this.width = width
        this.height = height
        useRegion = true
        try {
            image = if (imgUrl != null) ImageIO.read(imgUrl) else null
            repaint()
        } catch (ex: IOException) {
            Logger.getLogger(ImagePanel::class.java.name).log(Level.SEVERE, null, ex)
        }
    }

    override fun paintComponent(g: Graphics) {
        val gg = g as Graphics2D
        gg.color = Color.LIGHT_GRAY
        gg.fillRect(0, 0, getWidth(), getHeight())
        if (background != null) {
            val paint = TexturePaint(background, Rectangle(0, 0, background!!.width, background!!.height))
            gg.paint = paint
            gg.fillRect(0, 0, getWidth(), getHeight())
            gg.paint = null
        }
        if (image != null && !useRegion) {
            val panelRatio = getWidth().toFloat() / getHeight().toFloat()
            val imgRatio = image!!.width.toFloat() / image!!.height.toFloat()
            if (imgRatio > panelRatio) {
                val tw = getWidth().toFloat()
                val th = getWidth().toFloat() / imgRatio
                gg.drawImage(image, 0, (getHeight() / 2 - th / 2).toInt(), tw.toInt(), th.toInt(), null)
            } else {
                val tw = getHeight().toFloat() * imgRatio
                val th = getHeight().toFloat()
                gg.drawImage(image, (getWidth().toFloat() / 2 - tw / 2).toInt(), 0, tw.toInt(), th.toInt(), null)
            }
        } else if (image != null && useRegion) {
            val panelRatio = getWidth().toFloat() / getHeight().toFloat()
            val imgRatio = width.toFloat() / height.toFloat()
            if (imgRatio > panelRatio) {
                val tw = getWidth()
                val th = (getWidth() / imgRatio).toInt()
                val tx = 0
                val ty = getHeight() / 2 - th / 2
                gg.drawImage(image, tx, ty, tx + tw, ty + th, x, y, x + width, y + width, null)
            } else {
                val tw = (getHeight() * imgRatio).toInt()
                val th = getHeight()
                val tx = getWidth() / 2 - tw / 2
                val ty = 0
                gg.drawImage(image, tx, ty, tx + tw, ty + th, x, y, x + width, y + width, null)
            }
        }
    }

    companion object {
        private const val serialVersionUID = 3406919747183221557L
    }
}
