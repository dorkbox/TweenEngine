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
package dorkbox.demo

import dorkbox.demo.applets.Sprite
import dorkbox.demo.applets.SpriteAccessor
import dorkbox.demo.applets.Theme.MAIN_BACKGROUND
import dorkbox.demo.applets.Theme.apply
import dorkbox.swingActiveRender.ActionHandlerLong
import dorkbox.swingActiveRender.SwingActiveRender
import dorkbox.tweenEngine.TweenEngine
import dorkbox.tweenEngine.TweenEngine.Companion.create
import dorkbox.tweenEngine.TweenEquations.Companion.parse
import dorkbox.util.SwingUtil
import dorkbox.util.swing.GroupBorder
import dorkbox.util.swing.SwingHelper.showOnSameScreenAsMouseCenter
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 */
class TweenApplet : JApplet() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtil.invokeLater {
                // NullRepaintManager.install();
                val applet = TweenApplet()
                applet.init()
                applet.start()
                val wnd = JFrame()
                wnd.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                wnd.add(applet)
                wnd.setSize(600, 550)
                showOnSameScreenAsMouseCenter(wnd)
                wnd.isVisible = true
                SwingActiveRender.addActiveRender(applet.canvas)
            }
        }

        private fun convertToSeconds(milliSeconds: Int): Float {
            return milliSeconds / 1000.0f
        }
    }

    private val canvasWrapper = JPanel()
    private val delaySpinner = JSpinner()
    private val durationSpinner = JSpinner()
    private val easingCbox = JComboBox<Any?>()
    private val jLabel1 = JLabel()
    private val jLabel2 = JLabel()
    private val jLabel3 = JLabel()
    private val jLabel4 = JLabel()
    private val jLabel5 = JLabel()
    private val jLabel6 = JLabel()
    private val jLabel7 = JLabel()
    private val jLabel8 = JLabel()
    private val jLabel9 = JLabel()
    private val jPanel1 = JPanel()
    private val jPanel2 = JPanel()
    private val jPanel3 = JPanel()
    private val jPanel4 = JPanel()
    private val jScrollPane1 = JScrollPane()
    private val resultArea = JTextArea()
    private val repeatDelaySpinner = JSpinner()
    private val repeatSpinner = JSpinner()
    private val autoReverseCheckbox = JCheckBox()


    private var frameStartHandler: ActionHandlerLong? = null
    private val canvas = MyCanvas()

    override fun init() {
        SwingUtil.invokeAndWaitQuietly { load() }
    }

    override fun destroy() {
        SwingActiveRender.removeActiveRender(canvas)
        SwingActiveRender.removeActiveRenderFrameStart(frameStartHandler)
    }

    private fun load() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        initComponents()

        contentPane.background = MAIN_BACKGROUND
        apply(contentPane)
        val listener = OptionsListener()
        easingCbox.addActionListener(listener)
        delaySpinner.addChangeListener(listener)
        durationSpinner.addChangeListener(listener)
        repeatSpinner.addChangeListener(listener)
        repeatDelaySpinner.addChangeListener(listener)
        autoReverseCheckbox.addActionListener(listener)

        generateCode()

        canvasWrapper.add(canvas, BorderLayout.CENTER)

        frameStartHandler = ActionHandlerLong { deltaInNanos ->
            canvas.tweenEngine.update(deltaInNanos)
            // canvas.repaint();
        }
        SwingActiveRender.addActiveRenderFrameStart(frameStartHandler)
    }

    // -------------------------------------------------------------------------
    // Generated stuff
    // -------------------------------------------------------------------------
    private fun initComponents() {
        jPanel1.border = GroupBorder()
        jScrollPane1.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        jScrollPane1.setViewportView(resultArea)
        resultArea.columns = 20
        resultArea.rows = 5
        jLabel1.text = "Java code:"
        jLabel9.text = """
             <html>
             Tween Engine v${TweenEngine.version}
             """.trimIndent()


        val jPanel1Layout = GroupLayout(jPanel1)
        jPanel1.layout = jPanel1Layout
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 562, Short.MAX_VALUE.toInt())
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 237, Short.MAX_VALUE.toInt())
                                                .addComponent(jLabel9, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        )
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel9, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE.toInt())
                                .addContainerGap())
        )


        canvasWrapper.layout = BorderLayout()
        jPanel3.isOpaque = false
        jLabel2.horizontalAlignment = SwingConstants.CENTER
        jLabel2.icon = ImageIcon(Sprite::class.java.getResource("gfx/logo-tween.png"))

        val groupBorder1 = GroupBorder()
        groupBorder1.title = "Options"
        jPanel2.border = groupBorder1
        jLabel3.text = "Delay:"
        delaySpinner.model = SpinnerNumberModel(0, 0, null, 100)
        jLabel4.text = "Repetitions:"
        repeatSpinner.model = SpinnerNumberModel(0, 0, null, 1)
        autoReverseCheckbox.text = "Auto reverse"
        jLabel5.text = "Duration (in ms):"
        durationSpinner.model = SpinnerNumberModel(500, 0, null, 100)
        jLabel7.text = "Easing:"
        easingCbox.model = DefaultComboBoxModel(arrayOf<String?>("Linear.INOUT", "----------", "Quad.IN", "Quad.OUT", "Quad.INOUT", "----------", "Cubic.IN", "Cubic.OUT", "Cubic.INOUT", "----------", "Quart.IN", "Quart.OUT", "Quart.INOUT", "----------", "Quint.IN", "Quint.OUT", "Quint.INOUT", "----------", "Circ.IN", "Circ.OUT", "Circ.INOUT", "----------", "Sine.IN", "Sine.OUT", "Sine.INOUT", "----------", "Expo.IN", "Expo.OUT", "Expo.INOUT", "----------", "Back.IN", "Back.OUT", "Back.INOUT", "----------", "Bounce.IN", "Bounce.OUT", "Bounce.INOUT", "----------", "Elastic.IN", "Elastic.OUT", "Elastic.INOUT"))
        easingCbox.selectedIndex = 31
        jLabel6.text = "Repeat delay (in ms):"
        repeatDelaySpinner.model = SpinnerNumberModel(0, 0, null, 100)

        val jPanel2Layout = GroupLayout(jPanel2)
        jPanel2.layout = jPanel2Layout
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(autoReverseCheckbox, GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel7)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(easingCbox, 0, 105, Short.MAX_VALUE.toInt()))
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addGap(13, 13, 13)
                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel5)
                                                        .addComponent(jLabel3))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(durationSpinner, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(delaySpinner, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel4)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(repeatSpinner, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel6)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(repeatDelaySpinner, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        )
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel7)
                                        .addComponent(easingCbox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(delaySpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel3))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(durationSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel5))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(repeatSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel4))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(repeatDelaySpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel6))
                                .addGap(18, 18, 18)
                                .addComponent(autoReverseCheckbox)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()))
        )
        jPanel4.border = GroupBorder()
        jLabel8.text = "<html>\nClick anywhere on the canvas to fire your custom tween."
        jLabel8.verticalAlignment = SwingConstants.TOP

        val jPanel4Layout = GroupLayout(jPanel4)
        jPanel4.layout = jPanel4Layout
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel8, GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE.toInt())
                                .addContainerGap())
        )
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel8, GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE.toInt())
                                .addContainerGap())
        )


        val jPanel3Layout = GroupLayout(jPanel3)
        jPanel3.layout = jPanel3Layout
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel2, GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE.toInt())
                        .addComponent(jPanel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE.toInt())
                                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        )


        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(canvasWrapper, GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE.toInt())
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        )
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(canvasWrapper, GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE.toInt())
                                        .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        )
    }


    private fun generateCode() {
        val easing = easingCbox.selectedItem as String
        val delay = delaySpinner.value as Int
        val duration = convertToSeconds(durationSpinner.value as Int)
        val repeatCount = repeatSpinner.value as Int
        val repeatDelay = convertToSeconds(repeatDelaySpinner.value as Int)
        val isAutoReverse = autoReverseCheckbox.isSelected
        var code = "Tween.to(mySprite, POSITION_XY, $duration)"
        code += "\n     .target()"
        if (easing != "Linear" && easing != "----------") {
            code += "\n     .ease($easing)"
        }
        if (delay > 0) {
            code += "\n     .delay($delay)"
        }
        if (repeatCount > 0) {
            code += """
     .repeat${if (isAutoReverse) "autoReverse" else ""}($repeatCount, $repeatDelay)"""
        }
        code += "\n     .start(myManager);"
        resultArea.text = code
    }

    private inner class OptionsListener : ChangeListener, ActionListener {
        override fun stateChanged(e: ChangeEvent) {
            onEvent()
        }

        override fun actionPerformed(e: ActionEvent) {
            onEvent()
        }

        private fun onEvent() {
            generateCode()
        }
    }

    // -------------------------------------------------------------------------
    // Canvas
    // -------------------------------------------------------------------------
    inner class MyCanvas : Canvas() {
        val tweenEngine = create()
                .unsafe()
                .setWaypointsLimit(10)
                .setCombinedAttributesLimit(3)
                .registerAccessor(Sprite::class.java, SpriteAccessor())
                .build()

        private val vialSprite = Sprite("vial.png")

        @Transient
        private var bgPaint: TexturePaint? = null

        private val mouseAdapter: MouseAdapter = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val easing = parse((easingCbox.selectedItem as String))
                val delay = delaySpinner.value as Int
                val duration = convertToSeconds(durationSpinner.value as Int)
                val repeatCount = repeatSpinner.value as Int
                val repeatDelay = convertToSeconds(repeatDelaySpinner.value as Int)
                val isAutoReverse = autoReverseCheckbox.isSelected
                tweenEngine.cancelAll()


                val tween = tweenEngine.to(vialSprite, SpriteAccessor.POSITION_XY, duration)
                        .target(e.x.toFloat(), e.y.toFloat())
                        .delay(delay.toFloat())
                if (easing != null) {
                    tween.ease(easing)
                }
                if (isAutoReverse) {
                    tween.repeatAutoReverse(repeatCount, repeatDelay)
                } else {
                    tween.repeat(repeatCount, repeatDelay)
                }
                tween.start()
            }
        }

        init {
            addMouseListener(mouseAdapter)

            vialSprite.setPosition(100f, 100f)
            try {
                val bgImage = ImageIO.read(Sprite::class.java.getResource("gfx/transparent-dark.png"))
                bgPaint = TexturePaint(bgImage, Rectangle(0, 0, bgImage.width, bgImage.height))
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
        }

        override fun paint(g: Graphics) {
            val gg = g as Graphics2D
            if (bgPaint != null) {
                gg.paint = bgPaint
                gg.fillRect(0, 0, width, height)
                gg.paint = null
            }
            vialSprite.draw(gg)
        }


    }
}
