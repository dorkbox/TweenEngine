/*
 * Copyright 2012 Aurelien Ribon
 * Copyright 2015 dorkbox, llc
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
import dorkbox.demo.applets.Theme.MAIN_FOREGROUND
import dorkbox.demo.applets.Theme.apply
import dorkbox.swingActiveRender.ActionHandlerLong
import dorkbox.swingActiveRender.SwingActiveRender
import dorkbox.tweenEngine.Timeline
import dorkbox.tweenEngine.TweenEngine
import dorkbox.tweenEngine.TweenEngine.Companion.create
import dorkbox.tweenEngine.TweenEquations
import dorkbox.tweenEngine.TweenEvents
import dorkbox.util.LocationResolver
import dorkbox.util.SwingUtil
import dorkbox.util.swing.GroupBorder
import dorkbox.util.swing.SwingHelper.showOnSameScreenAsMouseCenter
import java.awt.BorderLayout
import java.awt.Canvas
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.Rectangle
import java.awt.TexturePaint
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.IOException
import java.util.*
import java.util.concurrent.*
import javax.imageio.ImageIO
import javax.swing.GroupLayout
import javax.swing.ImageIcon
import javax.swing.JApplet
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSlider
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.LayoutStyle
import javax.swing.ScrollPaneConstants
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.WindowConstants
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 * @author dorkbox, llc
 */
class TimelineApplet : JApplet() {
    private val canvas = MyCanvas()
    private var isPaused = false
    private var frameStartHandler: ActionHandlerLong? = null

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
        rptSpinner.addChangeListener(listener)
        rptDelaySpinner.addChangeListener(listener)
        autoReverseChk.addActionListener(listener)
        generateCode()

        val labels = Hashtable<Int, JLabel>()
        labels[-300] = JLabel("-3")
        labels[-200] = JLabel("-2")
        labels[-100] = JLabel("-1")
        labels[0] = JLabel("0")
        labels[100] = JLabel("1")
        labels[200] = JLabel("2")
        labels[300] = JLabel("3")

        for (lbl in labels.values) lbl.foreground = MAIN_FOREGROUND
        speedSlider.labelTable = labels

        canvasWrapper.add(canvas, BorderLayout.CENTER)
        frameStartHandler = ActionHandlerLong { deltaInNanos ->
            val timeline: Timeline = canvas.timeline
            if (isPaused) {
                return@ActionHandlerLong
            }
            val adjustedDeltaInNanos = deltaInNanos * speedSlider.value / 100

            // everything here MUST be in milliseconds (because the GUI is in MS, and the tween is in NANO-SECONDS)
            val deltaInMillis = TimeUnit.NANOSECONDS.toMillis(adjustedDeltaInNanos).toInt()
            if (!timeline.isFinished() && !timeline.isInDelay()) {
                val value = iterationTimeSlider.value
                if (!timeline.isInAutoReverse) {
                    // normal, forwards running
                    iterationTimeSlider.value = value + deltaInMillis
                } else {
                    // when the animation is set to reverse
                    iterationTimeSlider.value = value - deltaInMillis
                }
            }
            val value = totalTimeSlider.value
            val duration = (timeline.fullDuration() * 1000).toInt() // must be in MS
            if (value in 0..duration) {
                totalTimeSlider.value = value + deltaInMillis
            }
            canvas.tweenEngine.update(adjustedDeltaInNanos)
            // canvas.repaint();
        }
        SwingActiveRender.addActiveRenderFrameStart(frameStartHandler)
        canvas.createTimeline()
        initTimeline()
    }

    private fun initTimeline() {
        val timeline: Timeline = canvas.timeline
        iterationTimeSlider.maximum = timeline.duration.toInt() * 1000 // this is in milliseconds
        totalTimeSlider.maximum = timeline.fullDuration().toInt() * 1000 // this is in milliseconds
        iterationTimeSlider.value = 0
        totalTimeSlider.value = 0
    }

    fun generateCode() {
        val rptCnt = rptSpinner.value as Int
        val rptDelay = convertToSeconds(rptDelaySpinner.value as Int)
        val isAutoReverse = autoReverseChk.isSelected
        var code = """Timeline.createSequential()
    .push(Tween.to(imgTweenSprite, POSITION_XY, 0.5F).target(60, 90).ease(Quart.OUT))
    .push(Tween.to(imgEngineSprite, POSITION_XY, 0.5F).target(200, 90).ease(Quart.OUT))
    .push(Tween.to(imgUniversalSprite, POSITION_XY, 1.0F).target(60, 55).ease(Bounce.OUT))
    .pushPause(0.5F)
    .beginParallel()
        .push(Tween.set(imgLogoSprite, VISIBILITY).target(1))
        .push(Tween.to(imgLogoSprite, SCALE_XY, 0.8F).target(1, 1).ease(Back.OUT))
        .push(Tween.to(blankStripSprite, SCALE_XY, 0.5F).target(1, 1).ease(Back.OUT))
    .end()"""
        if (rptCnt > 0) code += """
    .repeat${if (isAutoReverse) "AutoReverse" else ""}($rptCnt, ${rptDelay}F)"""
        code += "\n    .start(myManager);"
        resultArea.text = code
    }

    fun restart() {
        speedSlider.value = DEFAULT_SPEED
        canvas.createTimeline()
        initTimeline()
    }

    private inner class OptionsListener internal constructor() : ChangeListener, ActionListener {
        override fun stateChanged(e: ChangeEvent) {
            onEvent()
        }

        override fun actionPerformed(e: ActionEvent) {
            onEvent()
        }

        private fun onEvent() {
            generateCode()
            restart()
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

        private val imgUniversalSprite = Sprite("img-universal.png").setCentered(false)
        private val imgTweenSprite = Sprite("img-tween.png").setCentered(false)
        private val imgEngineSprite = Sprite("img-engine.png").setCentered(false)
        private val imgLogoSprite = Sprite("img-logo.png")
        private val blankStripSprite = Sprite("blankStrip.png")
        private var bgPaint: TexturePaint? = null

        lateinit var timeline: Timeline

        init {
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
            blankStripSprite.draw(gg)
            imgUniversalSprite.draw(gg)
            imgTweenSprite.draw(gg)
            imgEngineSprite.draw(gg)
            imgLogoSprite.draw(gg)
        }

        fun createTimeline() {
            tweenEngine.cancelAll()
            imgUniversalSprite.setPosition(60f, (105 - 200).toFloat())
            imgTweenSprite.setPosition((60 - 300).toFloat(), 140f)
            imgEngineSprite.setPosition((200 + 300).toFloat(), 140f)
            imgLogoSprite.setPosition(310f, 120f)
            imgLogoSprite.setScale(7.0f, 7.0f)
            imgLogoSprite.isVisible = false
            blankStripSprite.setPosition(250f, 140f)
            blankStripSprite.setScale(1f, 0f)

            // scale is NOT "1 tick = 1 second". It is adjusted for use with "animation speed" slider (slider is in MILLISECONDS)
            timeline = tweenEngine.createSequential()
                    .addCallback(TweenEvents.START) {
                            iterationTimeSlider.value = 0
                    }
                    .addCallback(TweenEvents.END) {
                        // has to be in milliseconds
                        iterationTimeSlider.value = duration.toInt() * 1000
                    }
                    .push(tweenEngine.to<Sprite?>(imgTweenSprite, SpriteAccessor.POSITION_XY, 0.5f)
                            .target(60f, 140f)
                            .ease(TweenEquations.Quart_Out))
                    .push(tweenEngine.to<Sprite?>(imgEngineSprite, SpriteAccessor.POSITION_XY, 0.5f)
                            .target(200f, 140f)
                            .ease(TweenEquations.Quart_Out))
                    .push(tweenEngine.to<Sprite?>(imgUniversalSprite, SpriteAccessor.POSITION_XY, 1.0f)
                            .target(60f, 105f)
                            .ease(TweenEquations.Bounce_Out))
                    .pushPause(0.5f)
                    .beginParallel()
                    .push(tweenEngine.set<Sprite?>(imgLogoSprite, SpriteAccessor.VISIBILITY)
                            .target(1f))
                    .push(tweenEngine.to<Sprite?>(imgLogoSprite, SpriteAccessor.SCALE_XY, 0.8f)
                            .target(1f, 1f)
                            .ease(TweenEquations.Back_Out))
                    .push(tweenEngine.to<Sprite?>(blankStripSprite, SpriteAccessor.SCALE_XY, 0.5f)
                            .target(1f, 1f)
                            .ease(TweenEquations.Back_Out))
                    .end()
            val rptCnt = rptSpinner.value as Int
            val rpDelay = convertToSeconds(rptDelaySpinner.value as Int)
            val autoReverse = autoReverseChk.isSelected
            if (rptCnt > 0 && autoReverse) {
                timeline.repeatAutoReverse(rptCnt, rpDelay)
            } else if (rptCnt > 0) {
                timeline.repeat(rptCnt, rpDelay)
            }
            timeline.start()
        }
    }

    private fun initComponents() {
        jPanel1.border = GroupBorder()
        jScrollPane1.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        jScrollPane1.setViewportView(resultArea)
        resultArea.columns = 20
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
                                        .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE.toInt())
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 273, Short.MAX_VALUE.toInt())
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
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE.toInt())
                                .addContainerGap())
        )

        canvasWrapper.layout = BorderLayout()
        jPanel3.isOpaque = false
        jLabel2.horizontalAlignment = SwingConstants.CENTER

        val resource = LocationResolver.getResource("dorkbox/demo/applets/gfx/logo-timeline.png")
        jLabel2.icon = ImageIcon(resource)

        val groupBorder1 = GroupBorder()
        groupBorder1.title = "Timeline options"
        jPanel2.border = groupBorder1
        repetitionsLabel.text = "Repetitions:"
        setPositionLabel.text = "Set Position:"
        posSpinner.model = SpinnerNumberModel(.2, 0.0, 1.0, .1)
        rptSpinner.model = SpinnerNumberModel(20, 0, null, 1)
        autoReverseChk.isSelected = true
        autoReverseChk.text = "Auto reverse"
        setPositionButton.text = "X"
        setPositionButton.addActionListener {
            generateCode()
            val timeline: Timeline = canvas.timeline

            // setPosition is a percentage of DURATION (not including any delays/repeat delay's/etc)
            val value = (posSpinner.value as Double).toFloat()
            timeline.setProgress(value, timeline.getDirection())

            // also have to update the GUI status bar. Has to be in millis.
            iterationTimeSlider.value = (value * timeline.duration * 1000).toInt()
        }
        repeatDelayLabel.text = "Repeat delay (in ms):"
        rptDelaySpinner.model = SpinnerNumberModel(500, 0, null, 100)

        val jPanel2Layout = GroupLayout(jPanel2)
        jPanel2.layout = jPanel2Layout
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addComponent(autoReverseChk)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(setPositionButton, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addComponent(setPositionLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(posSpinner, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addComponent(repetitionsLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(rptSpinner, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                                .addComponent(repeatDelayLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(rptDelaySpinner, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        )
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(posSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(setPositionLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(rptSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(repetitionsLabel))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(rptDelaySpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(repeatDelayLabel))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(autoReverseChk, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(setPositionButton))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()))
        )

        val groupBorder2 = GroupBorder()
        groupBorder2.title = "Animation speed"
        jPanel5.border = groupBorder2
        speedSlider.majorTickSpacing = 100
        speedSlider.maximum = 300
        speedSlider.minimum = -300
        speedSlider.paintLabels = true
        speedSlider.paintTicks = true
        speedSlider.value = DEFAULT_SPEED

        val jPanel5Layout = GroupLayout(jPanel5)
        jPanel5.layout = jPanel5Layout
        jPanel5Layout.setHorizontalGroup(
                jPanel5Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(speedSlider, GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE.toInt())
                                .addContainerGap())
        )
        jPanel5Layout.setVerticalGroup(
                jPanel5Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(speedSlider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()))
        )

        val jPanel3Layout = GroupLayout(jPanel3)
        jPanel3.layout = jPanel3Layout
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel2, GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE.toInt())
                        .addComponent(jPanel5, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
        )
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
        )

        jPanel4.border = GroupBorder()
        restartBtn.font = Font("Tahoma", Font.BOLD, 11) // NOI18N
        restartBtn.text = "Restart"
        restartBtn.margin = Insets(2, 3, 2, 3)
        restartBtn.addActionListener { restart() }
        pauseBtn.text = "Pause"
        pauseBtn.margin = Insets(2, 3, 2, 3)
        pauseBtn.addActionListener { isPaused = true }
        resumeBtn.text = "Resume"
        resumeBtn.margin = Insets(2, 3, 2, 3)
        resumeBtn.addActionListener { isPaused = false }
        reverseBtn.text = "Reverse"
        reverseBtn.margin = Insets(2, 3, 2, 3)
        reverseBtn.addActionListener { speedSlider.value = -speedSlider.value }
        jLabel3.text = "Total time:"
        jLabel5.text = "Iteration time:"
        iterationTimeSlider.isEnabled = false
        totalTimeSlider.isEnabled = false

        val jPanel4Layout = GroupLayout(jPanel4)
        jPanel4.layout = jPanel4Layout
        jPanel4Layout.setHorizontalGroup(
                jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(restartBtn)
                                        .addComponent(reverseBtn))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addComponent(resumeBtn)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                                                .addComponent(jLabel3))
                                        .addGroup(jPanel4Layout.createSequentialGroup()
                                                .addComponent(pauseBtn)
                                                .addGap(18, 18, 18)
                                                .addComponent(jLabel5)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(totalTimeSlider, GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE.toInt())
                                        .addComponent(iterationTimeSlider, GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE.toInt()))
                                .addContainerGap())
        )
        jPanel4Layout.linkSize(SwingConstants.HORIZONTAL, pauseBtn, restartBtn, resumeBtn, reverseBtn)
        jPanel4Layout.setVerticalGroup(
                jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(restartBtn)
                                                .addComponent(pauseBtn)
                                                .addComponent(jLabel5))
                                        .addComponent(iterationTimeSlider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel4Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(resumeBtn)
                                                .addComponent(reverseBtn)
                                                .addComponent(jLabel3))
                                        .addComponent(totalTimeSlider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()))
        )

        val layout = GroupLayout(contentPane)
        contentPane.layout = layout
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                                        .addComponent(jPanel4, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt())
                                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(canvasWrapper, GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE.toInt())
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        )
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(canvasWrapper, GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE.toInt())
                                        .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE.toInt()))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        )
    }

    private val canvasWrapper = JPanel()
    private val iterationTimeSlider = JSlider()
    private val jLabel1 = JLabel()
    private val jLabel2 = JLabel()
    private val jLabel3 = JLabel()
    private val setPositionLabel = JLabel()
    private val repetitionsLabel = JLabel()
    private val jLabel5 = JLabel()
    private val repeatDelayLabel = JLabel()
    private val jLabel9 = JLabel()
    private val jPanel1 = JPanel()
    private val jPanel2 = JPanel()
    private val jPanel3 = JPanel()
    private val jPanel4 = JPanel()
    private val jPanel5 = JPanel()
    private val jScrollPane1 = JScrollPane()
    private val pauseBtn = JButton()
    private val restartBtn = JButton()
    private val resultArea = JTextArea()
    private val resumeBtn = JButton()
    private val reverseBtn = JButton()
    private val rptDelaySpinner = JSpinner()
    private val posSpinner = JSpinner()
    private val rptSpinner = JSpinner()
    private val speedSlider = JSlider()
    private val totalTimeSlider = JSlider()
    private val autoReverseChk = JCheckBox()
    private val setPositionButton = JButton()

    companion object {
        private const val serialVersionUID = 3035833477036641021L
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtil.invokeLater {
                val applet = TimelineApplet()
                applet.init()
                applet.start()
                SwingActiveRender.addActiveRender(applet.canvas)

                val wnd = JFrame()
                wnd.add(applet)
                wnd.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
                wnd.setSize(700, 700)
                showOnSameScreenAsMouseCenter(wnd)
                wnd.isVisible = true
            }
        }

        private const val DEFAULT_SPEED = 100
        private fun convertToSeconds(milliSeconds: Int): Float {
            return milliSeconds / 1000.0f
        }
    }
}
