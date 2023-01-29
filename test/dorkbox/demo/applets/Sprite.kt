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

import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 * @author dorkbox, llc
 */
class Sprite(gfxName: String) {
    private var image: BufferedImage? = null
    var x = 0f
        private set
    var y = 0f
        private set
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set
    private var isCentered = true
    @JvmField
	var isVisible = true

    init {
        try {
            image = ImageIO.read(Sprite::class.java.getResource("gfx/$gfxName"))
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    fun draw(gg: Graphics2D) {
        var gg = gg
        if (!isVisible) return
        gg = gg.create() as Graphics2D
        gg.translate(x.toDouble(), y.toDouble())
        gg.scale(scaleX.toDouble(), scaleY.toDouble())
        gg.drawImage(image, null, if (isCentered) -image!!.width / 2 else 0, if (isCentered) -image!!.height / 2 else 0)
        gg.dispose()
    }

    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun setScale(scaleX: Float, scaleY: Float) {
        this.scaleX = scaleX
        this.scaleY = scaleY
    }

    fun setCentered(isCentered: Boolean): Sprite {
        this.isCentered = isCentered
        return this
    }
}
