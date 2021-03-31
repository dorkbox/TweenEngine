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
package dorkbox.demo;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dorkbox.demo.applets.Sprite;
import dorkbox.demo.applets.SpriteAccessor;
import dorkbox.demo.applets.Theme;
import dorkbox.swingActiveRender.ActionHandlerLong;
import dorkbox.swingActiveRender.SwingActiveRender;
import dorkbox.tweenEngine.BaseTween;
import dorkbox.tweenEngine.Timeline;
import dorkbox.tweenEngine.TweenCallback;
import dorkbox.tweenEngine.TweenEngine;
import dorkbox.tweenEngine.TweenEquations;
import dorkbox.util.LocationResolver;
import dorkbox.util.SwingUtil;
import dorkbox.util.swing.GroupBorder;
import dorkbox.util.swing.SwingHelper;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 * @author dorkbox, llc
 */
@SuppressWarnings({"FieldCanBeLocal", "UnusedParameters"})
public class TimelineApplet extends javax.swing.JApplet {
    private static final long serialVersionUID = 3035833477036641021L;

    public static void main(String[] args) {
        SwingUtil.invokeLater(
                new Runnable() {
                    @Override
                    public
                    void run() {
                        final TimelineApplet applet = new TimelineApplet();
                        applet.init();
                        applet.start();
                        SwingActiveRender.addActiveRender(applet.canvas);

                        javax.swing.JFrame wnd = new javax.swing.JFrame();
                        wnd.add(applet);
                        wnd.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                        wnd.setSize(700, 700);

                        SwingHelper.showOnSameScreenAsMouseCenter(wnd);
                        wnd.setVisible(true);
                    }
                });
    }

    private static int DEFAULT_SPEED = 100;

	// -------------------------------------------------------------------------
	// Applet
	// -------------------------------------------------------------------------

	MyCanvas canvas;
	boolean isPaused = false;
    private ActionHandlerLong frameStartHandler;

    @Override
	public void init() {
	    SwingUtil.invokeAndWaitQuietly(new Runnable() {
            @Override
            public
            void run() {
                load();
            }
        });
	}

	@Override
	public void destroy() {
        SwingActiveRender.removeActiveRender(canvas);
        SwingActiveRender.removeActiveRenderFrameStart(frameStartHandler);
	}

	void load() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
            ex.printStackTrace();
		}

		initComponents();

		getContentPane().setBackground(Theme.MAIN_BACKGROUND);
		Theme.apply(getContentPane());

		OptionsListener listener = new OptionsListener();
		rptSpinner.addChangeListener(listener);
		rptDelaySpinner.addChangeListener(listener);
		autoReverseChk.addActionListener(listener);

		generateCode();

		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		labels.put(-300, new JLabel("-3"));
		labels.put(-200, new JLabel("-2"));
		labels.put(-100, new JLabel("-1"));
		labels.put(0, new JLabel("0"));
		labels.put(100, new JLabel("1"));
		labels.put(200, new JLabel("2"));
		labels.put(300, new JLabel("3"));

		for (JLabel lbl : labels.values()) lbl.setForeground(Theme.MAIN_FOREGROUND);
		speedSlider.setLabelTable(labels);

        canvas = new MyCanvas();
        canvasWrapper.add(canvas, BorderLayout.CENTER);

        frameStartHandler = new ActionHandlerLong() {
            @Override
            public
            void handle(final long deltaInNanos) {
                Timeline timeline = canvas.getTimeline();
                if (timeline == null || isPaused) {
                    return;
                }

                long adjustedDeltaInNanos = deltaInNanos * speedSlider.getValue() / 100;

                // everything here MUST be in milliseconds (because the GUI is in MS, and the tween is in NANO-SECONDS)
                int deltaInMillis = (int) TimeUnit.NANOSECONDS.toMillis(adjustedDeltaInNanos);

                if (!timeline.isFinished() && !timeline.isInDelay()) {
                    final int value = iterationTimeSlider.getValue();

                    if (!timeline.isInAutoReverse()) {
                        // normal, forwards running
                        iterationTimeSlider.setValue(value + deltaInMillis);
                    }
                    else {
                        // when the animation is set to reverse
                        iterationTimeSlider.setValue(value - deltaInMillis);
                    }
                }

                int value = totalTimeSlider.getValue();
                final int duration = (int) (timeline.getFullDuration() * 1000); // must be in MS

                if (value >= 0 && value <= duration) {
                    totalTimeSlider.setValue(value + deltaInMillis);
                }

                canvas.tweenEngine.update(adjustedDeltaInNanos);
                // canvas.repaint();
            }
        };
        SwingActiveRender.addActiveRenderFrameStart(frameStartHandler);

        canvas.createTimeline();
        initTimeline();
	}

    private static
    float convertToSeconds(int milliSeconds) {
        return milliSeconds / 1000.0F;
    }

	private void initTimeline() {
        final Timeline timeline = canvas.getTimeline();
        iterationTimeSlider.setMaximum((int) timeline.getDuration() * 1000); // this is in milliseconds
		totalTimeSlider.setMaximum((int) timeline.getFullDuration() * 1000); // this is in milliseconds

        iterationTimeSlider.setValue(0);
        totalTimeSlider.setValue(0);
	}

	void generateCode() {
		int rptCnt = (Integer) rptSpinner.getValue();
		float rptDelay = convertToSeconds((Integer) rptDelaySpinner.getValue());
		boolean isAutoReverse = autoReverseChk.isSelected();

		String code = "Timeline.createSequential()" +
				"\n    .push(Tween.to(imgTweenSprite, POSITION_XY, 0.5F).target(60, 90).ease(Quart.OUT))" +
				"\n    .push(Tween.to(imgEngineSprite, POSITION_XY, 0.5F).target(200, 90).ease(Quart.OUT))" +
				"\n    .push(Tween.to(imgUniversalSprite, POSITION_XY, 1.0F).target(60, 55).ease(Bounce.OUT))" +
				"\n    .pushPause(0.5F)" +
				"\n    .beginParallel()" +
				"\n        .push(Tween.set(imgLogoSprite, VISIBILITY).target(1))" +
				"\n        .push(Tween.to(imgLogoSprite, SCALE_XY, 0.8F).target(1, 1).ease(Back.OUT))" +
				"\n        .push(Tween.to(blankStripSprite, SCALE_XY, 0.5F).target(1, 1).ease(Back.OUT))" +
				"\n    .end()";

		if (rptCnt > 0) code += "\n    .repeat" + (isAutoReverse ? "AutoReverse" : "") + "(" + rptCnt + ", " + rptDelay + "F)";

		code += "\n    .start(myManager);";

		resultArea.setText(code);
	}

	void
    restart() {
        speedSlider.setValue(DEFAULT_SPEED);
        canvas.createTimeline();
        initTimeline();
    }

    private
    class OptionsListener implements ChangeListener, ActionListener {
        OptionsListener() {
        }

        @Override
        public
        void stateChanged(ChangeEvent e) {
            onEvent();
        }

        @Override
        public
        void actionPerformed(ActionEvent e) {
            onEvent();
        }

        private
        void onEvent() {
            generateCode();
            restart();
        }
    }

    // -------------------------------------------------------------------------
	// Canvas
	// -------------------------------------------------------------------------

	@SuppressWarnings("GwtInconsistentSerializableClass")
    private class MyCanvas extends Canvas {
        private static final long serialVersionUID = 1L;

        final TweenEngine tweenEngine = TweenEngine.create()
                                                   .unsafe()
                                                   .setWaypointsLimit(10)
                                                   .setCombinedAttributesLimit(3)
                                                   .registerAccessor(Sprite.class, new SpriteAccessor())
                                                   .build();

		private final Sprite imgUniversalSprite;
		private final Sprite imgTweenSprite;
		private final Sprite imgEngineSprite;
		private final Sprite imgLogoSprite;
		private final Sprite blankStripSprite;
		private TexturePaint bgPaint;
        private Timeline timeline;

		public MyCanvas() {
            imgUniversalSprite = new Sprite("img-universal.png").setCentered(false);
            imgTweenSprite = new Sprite("img-tween.png").setCentered(false);
            imgEngineSprite = new Sprite("img-engine.png").setCentered(false);
            imgLogoSprite = new Sprite("img-logo.png");
            blankStripSprite = new Sprite("blankStrip.png");

            try {
                BufferedImage bgImage = ImageIO.read(Sprite.class.getResource("gfx/transparent-dark.png"));
                bgPaint = new TexturePaint(bgImage, new Rectangle(0, 0, bgImage.getWidth(), bgImage.getHeight()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

		@Override
		public void paint(Graphics g) {
			Graphics2D gg = (Graphics2D) g;

			if (bgPaint != null) {
				gg.setPaint(bgPaint);
				gg.fillRect(0, 0, getWidth(), getHeight());
				gg.setPaint(null);
			}

			blankStripSprite.draw(gg);
			imgUniversalSprite.draw(gg);
			imgTweenSprite.draw(gg);
			imgEngineSprite.draw(gg);
			imgLogoSprite.draw(gg);
		}

		@SuppressWarnings("FieldRepeatedlyAccessedInMethod")
        public void createTimeline() {
            tweenEngine.cancelAll();

			imgUniversalSprite.setPosition(60, 105 - 200);
			imgTweenSprite.setPosition(60 - 300, 140);
			imgEngineSprite.setPosition(200 + 300, 140);

			imgLogoSprite.setPosition(310, 120);
			imgLogoSprite.setScale(7.0F, 7.0F);
			imgLogoSprite.setVisible(false);

			blankStripSprite.setPosition(250, 140);
			blankStripSprite.setScale(1, 0);

            // scale is NOT "1 tick = 1 second". It is adjusted for use with "animation speed" slider (slider is in MILLISECONDS)

			timeline = tweenEngine.createSequential()
                                  .addCallback(new TweenCallback(TweenCallback.Events.START) {
                                   @Override
                                   public
                                   void onEvent(final int type, final BaseTween<?> source) {
                                       iterationTimeSlider.setValue(0);
                                   }
                               })
                                  .addCallback(new TweenCallback(TweenCallback.Events.END) {
                                   @Override
                                   public
                                   void onEvent(final int type, final BaseTween<?> source) {
                                       // has to be in milliseconds
                                       iterationTimeSlider.setValue((int)source.getDuration() * 1000);
                                   }
                               })
                                  .push(tweenEngine.to(imgTweenSprite, SpriteAccessor.POSITION_XY, 0.5F)
                                                   .target(60, 140)
                                                   .ease(TweenEquations.Quart_Out))

                                  .push(tweenEngine.to(imgEngineSprite, SpriteAccessor.POSITION_XY, 0.5F)
                                                   .target(200, 140)
                                                   .ease(TweenEquations.Quart_Out))
                                  .push(tweenEngine.to(imgUniversalSprite, SpriteAccessor.POSITION_XY, 1.0F)
                                                   .target(60, 105)
                                                   .ease(TweenEquations.Bounce_Out))

                                  .pushPause(0.5F)

                                  .beginParallel()
                                  .push(tweenEngine.set(imgLogoSprite, SpriteAccessor.VISIBILITY)
                                                   .target(1))
                                  .push(tweenEngine.to(imgLogoSprite, SpriteAccessor.SCALE_XY, 0.8F)
                                                   .target(1, 1)
                                                   .ease(TweenEquations.Back_Out))
                                  .push(tweenEngine.to(blankStripSprite, SpriteAccessor.SCALE_XY, 0.5F)
                                                   .target(1, 1)
                                                   .ease(TweenEquations.Back_Out))
                                  .end();

			int rptCnt = (Integer) rptSpinner.getValue();
			float rpDelay = convertToSeconds((Integer) rptDelaySpinner.getValue());
			boolean autoReverse = autoReverseChk.isSelected();

            if (rptCnt > 0 && autoReverse) {
                timeline.repeatAutoReverse(rptCnt, rpDelay);
            }
            else if (rptCnt > 0) {
                timeline.repeat(rptCnt, rpDelay);
            }

			timeline.start();
		}

		public Timeline getTimeline() {
			return timeline;
		}
	}

	// -------------------------------------------------------------------------
	// Generated stuff
	// -------------------------------------------------------------------------

    @SuppressWarnings({"FieldRepeatedlyAccessedInMethod"})
    //GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultArea = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        canvasWrapper = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        setPositionLabel = new javax.swing.JLabel();
        repetitionsLabel = new javax.swing.JLabel();
        posSpinner = new javax.swing.JSpinner();
        rptSpinner = new javax.swing.JSpinner();
        setPositionButton = new javax.swing.JButton();
        autoReverseChk = new javax.swing.JCheckBox();
        repeatDelayLabel = new javax.swing.JLabel();
        rptDelaySpinner = new javax.swing.JSpinner();
        jPanel5 = new javax.swing.JPanel();
        speedSlider = new javax.swing.JSlider();
        jPanel4 = new javax.swing.JPanel();
        restartBtn = new javax.swing.JButton();
        pauseBtn = new javax.swing.JButton();
        resumeBtn = new javax.swing.JButton();
        reverseBtn = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        iterationTimeSlider = new javax.swing.JSlider();
        totalTimeSlider = new javax.swing.JSlider();

        jPanel1.setBorder(new GroupBorder());

        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        resultArea.setColumns(20);
        jScrollPane1.setViewportView(resultArea);

        jLabel1.setText("Java code:");

        jLabel9.setText("<html>\nTween Engine v" + TweenEngine.getVersion());

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 273, Short.MAX_VALUE)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
                .addContainerGap())
        );

        canvasWrapper.setLayout(new java.awt.BorderLayout());

        jPanel3.setOpaque(false);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        URL resource = LocationResolver.getResource("dorkbox/demo/applets/gfx/logo-timeline.png");
        jLabel2.setIcon(new javax.swing.ImageIcon(resource));

        GroupBorder groupBorder1 = new GroupBorder();
        groupBorder1.setTitle("Timeline options");
        jPanel2.setBorder(groupBorder1);

        repetitionsLabel.setText("Repetitions:");
        setPositionLabel.setText("Set Position:");

        posSpinner.setModel(new javax.swing.SpinnerNumberModel(.2, 0, 1, .1));
        rptSpinner.setModel(new javax.swing.SpinnerNumberModel(20, 0, null, 1));

        autoReverseChk.setSelected(true);
        autoReverseChk.setText("Auto reverse");

        setPositionButton.setText("X");
        setPositionButton.addActionListener(new ActionListener() {
            @Override
            public
            void actionPerformed(final ActionEvent e) {
                generateCode();

                Timeline timeline = canvas.getTimeline();

                // setPosition is a percentage of DURATION (not including any delays/repeat delay's/etc)
                float value = (float) (double) (Double) posSpinner.getValue();
                timeline.setProgress(value, timeline.getDirection());

                // also have to update the GUI status bar. Has to be in millis.
                iterationTimeSlider.setValue((int) ((value * timeline.getDuration()) * 1000));
            }
        });

        repeatDelayLabel.setText("Repeat delay (in ms):");

        rptDelaySpinner.setModel(new javax.swing.SpinnerNumberModel(500, 0, null, 100));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(autoReverseChk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(setPositionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(setPositionLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(posSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(repetitionsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rptSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(repeatDelayLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(rptDelaySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(posSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(setPositionLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rptSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(repetitionsLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rptDelaySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(repeatDelayLabel))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(autoReverseChk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(setPositionButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupBorder groupBorder2 = new GroupBorder();
        groupBorder2.setTitle("Animation speed");
        jPanel5.setBorder(groupBorder2);

        speedSlider.setMajorTickSpacing(100);
        speedSlider.setMaximum(300);
        speedSlider.setMinimum(-300);
        speedSlider.setPaintLabels(true);
        speedSlider.setPaintTicks(true);
        speedSlider.setValue(DEFAULT_SPEED);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(speedSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(speedSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel4.setBorder(new GroupBorder());

        restartBtn.setFont(new java.awt.Font("Tahoma", Font.BOLD, 11)); // NOI18N
        restartBtn.setText("Restart");
        restartBtn.setMargin(new java.awt.Insets(2, 3, 2, 3));
        restartBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restartBtnActionPerformed(evt);
            }
        });

        pauseBtn.setText("Pause");
        pauseBtn.setMargin(new java.awt.Insets(2, 3, 2, 3));
        pauseBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseBtnActionPerformed(evt);
            }
        });

        resumeBtn.setText("Resume");
        resumeBtn.setMargin(new java.awt.Insets(2, 3, 2, 3));
        resumeBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resumeBtnActionPerformed(evt);
            }
        });

        reverseBtn.setText("Reverse");
        reverseBtn.setMargin(new java.awt.Insets(2, 3, 2, 3));
        reverseBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reverseBtnActionPerformed(evt);
            }
        });

        jLabel3.setText("Total time:");

        jLabel5.setText("Iteration time:");

        iterationTimeSlider.setEnabled(false);

        totalTimeSlider.setEnabled(false);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(restartBtn)
                    .addComponent(reverseBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(resumeBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel3))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(pauseBtn)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel5)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(totalTimeSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE)
                    .addComponent(iterationTimeSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 395, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, pauseBtn, restartBtn, resumeBtn, reverseBtn);

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(restartBtn)
                        .addComponent(pauseBtn)
                        .addComponent(jLabel5))
                    .addComponent(iterationTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(resumeBtn)
                        .addComponent(reverseBtn)
                        .addComponent(jLabel3))
                    .addComponent(totalTimeSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(canvasWrapper, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(canvasWrapper, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

	void restartBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restartBtnActionPerformed
		restart();
	}//GEN-LAST:event_restartBtnActionPerformed

	void pauseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseBtnActionPerformed
		isPaused = true;
	}//GEN-LAST:event_pauseBtnActionPerformed

	void resumeBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resumeBtnActionPerformed
		isPaused = false;
	}//GEN-LAST:event_resumeBtnActionPerformed

	void reverseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reverseBtnActionPerformed
		speedSlider.setValue(-speedSlider.getValue());
	}//GEN-LAST:event_reverseBtnActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel canvasWrapper;
    javax.swing.JSlider iterationTimeSlider;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel setPositionLabel;
    private javax.swing.JLabel repetitionsLabel;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel repeatDelayLabel;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton pauseBtn;
    private javax.swing.JButton restartBtn;
    private javax.swing.JTextArea resultArea;
    private javax.swing.JButton resumeBtn;
    private javax.swing.JButton reverseBtn;
    javax.swing.JSpinner rptDelaySpinner;
    javax.swing.JSpinner posSpinner;
    javax.swing.JSpinner rptSpinner;
    javax.swing.JSlider speedSlider;
    javax.swing.JSlider totalTimeSlider;
    javax.swing.JCheckBox autoReverseChk;
    javax.swing.JButton setPositionButton;
    // End of variables declaration//GEN-END:variables

}
