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
package dorkbox.demo;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dorkbox.demo.applets.Sprite;
import dorkbox.demo.applets.SpriteAccessor;
import dorkbox.demo.applets.Theme;
import dorkbox.swingActiveRender.ActionHandlerLong;
import dorkbox.swingActiveRender.SwingActiveRender;
import dorkbox.tweenEngine.Tween;
import dorkbox.tweenEngine.TweenEngine;
import dorkbox.tweenEngine.TweenEquation;
import dorkbox.tweenEngine.TweenEquations;
import dorkbox.util.SwingUtil;
import dorkbox.util.swing.GroupBorder;
import dorkbox.util.swing.SwingHelper;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 */
@SuppressWarnings("FieldCanBeLocal")
public class TweenApplet extends javax.swing.JApplet {
    public static void main(String[] args) {
        SwingUtil.invokeLater(new Runnable() {
            @Override
            public
            void run() {
                // NullRepaintManager.install();

                TweenApplet applet = new TweenApplet();
                applet.init();
                applet.start();

                javax.swing.JFrame wnd = new javax.swing.JFrame();
                wnd.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                wnd.add(applet);
                wnd.setSize(600, 550);

                SwingHelper.showOnSameScreenAsMouseCenter(wnd);
                wnd.setVisible(true);

                SwingActiveRender.addActiveRender(applet.canvas);
            }
        });
    }

	// -------------------------------------------------------------------------
	// Applet
	// -------------------------------------------------------------------------

    private ActionHandlerLong frameStartHandler;
    public MyCanvas canvas;

    @Override
    public
    void init() {
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

	private void load() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
            ex.printStackTrace();
		}

		initComponents();

		getContentPane().setBackground(Theme.MAIN_BACKGROUND);
		Theme.apply(getContentPane());

		OptionsListener listener = new OptionsListener();
		easingCbox.addActionListener(listener);
		delaySpinner.addChangeListener(listener);
		durationSpinner.addChangeListener(listener);
		repeatSpinner.addChangeListener(listener);
		repeatDelaySpinner.addChangeListener(listener);
		autoReverseCheckbox.addActionListener(listener);

		generateCode();

        canvas = new MyCanvas();
        canvasWrapper.add(canvas, BorderLayout.CENTER);

        frameStartHandler = new ActionHandlerLong() {
            @Override
            public
            void handle(final long deltaInNanos) {
                canvas.tweenEngine.update(deltaInNanos);
                // canvas.repaint();
            }
        };

        SwingActiveRender.addActiveRenderFrameStart(frameStartHandler);
	}

	private void generateCode() {
		String easing = (String) easingCbox.getSelectedItem();
		int delay = (Integer) delaySpinner.getValue();
		float duration = convertToSeconds((Integer) durationSpinner.getValue());
		int repeatCount = (Integer) repeatSpinner.getValue();
		float repeatDelay = convertToSeconds((Integer) repeatDelaySpinner.getValue());
		boolean isAutoReverse = autoReverseCheckbox.isSelected();

        String code = "Tween.to(mySprite, POSITION_XY, " + duration + ")";
        code += "\n     .target()";

        if (!easing.equals("Linear") && !easing.equals("----------")) {
            code += "\n     .ease(" + easing + ")";
        }
        if (delay > 0) {
            code += "\n     .delay(" + delay + ")";
        }
        if (repeatCount > 0) {
            code += "\n     .repeat" + (isAutoReverse ? "autoReverse" : "") + "(" + repeatCount + ", " + repeatDelay + ")";
        }

        code += "\n     .start(myManager);";

        resultArea.setText(code);
	}

	private class OptionsListener implements ChangeListener, ActionListener {
		@Override public void stateChanged(ChangeEvent e) {onEvent();}
		@Override public void actionPerformed(ActionEvent e) {onEvent();}
		private void onEvent() {
			generateCode();
		}
	}

	// -------------------------------------------------------------------------
	// Canvas
	// -------------------------------------------------------------------------

	private class MyCanvas extends Canvas {
        private final TweenEngine tweenEngine = TweenEngine.create()
                                                           .unsafe()
                                                           .setWaypointsLimit(10)
                                                           .setCombinedAttributesLimit(3)
                                                           .registerAccessor(Sprite.class, new SpriteAccessor())
                                                           .build();
		private transient final Sprite vialSprite;
		private transient TexturePaint bgPaint;

		public MyCanvas() {
			addMouseListener(mouseAdapter);

			vialSprite = new Sprite("vial.png");
			vialSprite.setPosition(100, 100);

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

			vialSprite.draw(gg);
		}

		@SuppressWarnings("GwtInconsistentSerializableClass")
        private final MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
                TweenEquation easing = TweenEquations.Companion.parse((String) easingCbox.getSelectedItem());
                int delay = (Integer) delaySpinner.getValue();
                float duration = convertToSeconds((Integer) durationSpinner.getValue());
                int repeatCount = (Integer) repeatSpinner.getValue();
                float repeatDelay = convertToSeconds((Integer) repeatDelaySpinner.getValue());
                boolean isAutoReverse = autoReverseCheckbox.isSelected();

                tweenEngine.cancelAll();

                Tween<Sprite> tween = tweenEngine.to(vialSprite, SpriteAccessor.POSITION_XY, duration)
                                         .target(e.getX(), e.getY())
                                         .delay(delay);

                if (easing != null) {
                    tween.ease(easing);
                }
                if (isAutoReverse) {
                    tween.repeatAutoReverse(repeatCount, repeatDelay);
                }
                else {
                    tween.repeat(repeatCount, repeatDelay);
                }

                tween.start();
            }
		};
	}

    private static
    float convertToSeconds(int milliSeconds) {
        return milliSeconds / 1000.0F;
    }

	// -------------------------------------------------------------------------
	// Generated stuff
	// -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
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
        jLabel3 = new javax.swing.JLabel();
        delaySpinner = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        repeatSpinner = new javax.swing.JSpinner();
        autoReverseCheckbox = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        durationSpinner = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        easingCbox = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        repeatDelaySpinner = new javax.swing.JSpinner();
        jPanel4 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();

        jPanel1.setBorder(new GroupBorder());

        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        resultArea.setColumns(20);
        resultArea.setRows(5);
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
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 562, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 237, Short.MAX_VALUE)
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                .addContainerGap())
        );

        canvasWrapper.setLayout(new java.awt.BorderLayout());

        jPanel3.setOpaque(false);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setIcon(new javax.swing.ImageIcon(Sprite.class.getResource("gfx/logo-tween.png")));
        //
        // NOI18N

        GroupBorder groupBorder1 = new GroupBorder();
        groupBorder1.setTitle("Options");
        jPanel2.setBorder(groupBorder1);

        jLabel3.setText("Delay:");

        delaySpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 100));

        jLabel4.setText("Repetitions:");

        repeatSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 1));

        autoReverseCheckbox.setText("Auto reverse");

        jLabel5.setText("Duration (in ms):");

        durationSpinner.setModel(new javax.swing.SpinnerNumberModel(500, 0, null, 100));

        jLabel7.setText("Easing:");

        easingCbox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Linear.INOUT", "----------", "Quad.IN", "Quad.OUT", "Quad.INOUT", "----------", "Cubic.IN", "Cubic.OUT", "Cubic.INOUT", "----------", "Quart.IN", "Quart.OUT", "Quart.INOUT", "----------", "Quint.IN", "Quint.OUT", "Quint.INOUT", "----------", "Circ.IN", "Circ.OUT", "Circ.INOUT", "----------", "Sine.IN", "Sine.OUT", "Sine.INOUT", "----------", "Expo.IN", "Expo.OUT", "Expo.INOUT", "----------", "Back.IN", "Back.OUT", "Back.INOUT", "----------", "Bounce.IN", "Bounce.OUT", "Bounce.INOUT", "----------", "Elastic.IN", "Elastic.OUT", "Elastic.INOUT" }));
        easingCbox.setSelectedIndex(31);

        jLabel6.setText("Repeat delay (in ms):");

        repeatDelaySpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 100));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(autoReverseCheckbox, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(easingCbox, 0, 105, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(13, 13, 13)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel5)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(durationSpinner, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(delaySpinner, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(repeatSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(repeatDelaySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(easingCbox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(delaySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(durationSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(repeatSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(repeatDelaySpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(18, 18, 18)
                .addComponent(autoReverseCheckbox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(new GroupBorder());

        jLabel8.setText("<html>\nClick anywhere on the canvas to fire your custom tween.");
        jLabel8.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 42, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(canvasWrapper, javax.swing.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(canvasWrapper, javax.swing.GroupLayout.DEFAULT_SIZE, 365, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel canvasWrapper;
    private javax.swing.JSpinner delaySpinner;
    private javax.swing.JSpinner durationSpinner;
    private javax.swing.JComboBox easingCbox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea resultArea;
    private javax.swing.JSpinner repeatDelaySpinner;
    private javax.swing.JSpinner repeatSpinner;
    private javax.swing.JCheckBox autoReverseCheckbox;
    // End of variables declaration//GEN-END:variables

}
