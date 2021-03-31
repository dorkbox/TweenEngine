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

import java.util.Locale;

import dorkbox.tweenEngine.BaseTween;
import dorkbox.tweenEngine.Timeline;
import dorkbox.tweenEngine.Tween;
import dorkbox.tweenEngine.TweenAccessor;
import dorkbox.tweenEngine.TweenCallback;
import dorkbox.tweenEngine.TweenEngine;

public
class ConsoleDemo {

    private static final TweenEngine tweenEngine = TweenEngine.create()
                                                              .unsafe()
                                                              .setWaypointsLimit(10)
                                                              .setCombinedAttributesLimit(3)
                                                              .registerAccessor(Bugtest.class, new A())
                                                              .build();

    public static
    void main(String[] args) {
        // Tests
        RunConsole();
    }

    private static
    TweenCallback buildCallback(final String name, final int triggers) {
        return new TweenCallback(triggers) {
            @Override
            public
            void onEvent(int type, BaseTween<?> source) {
                String t;
                if (type == Events.BEGIN) {
                    t = "BEGIN        ";
                } else if (type == Events.START) {
                    t = "START        ";
                } else if (type == Events.END) {
                    t = "END          ";
                } else if (type == Events.COMPLETE) {
                    t = "COMPLETE     ";
                } else if (type == Events.BACK_BEGIN) {
                    t = "BACK_BEGIN   ";
                } else if (type == Events.BACK_START) {
                    t = "BACK_START   ";
                } else if (type == Events.BACK_END) {
                    t = "BACK_END     ";
                } else if (type == Events.BACK_COMPLETE) {
                    t = "BACK_COMPLETE";
                } else {
                    t = "???";
                }

                String str = String.format(Locale.US, "%s %s   lt %3f", name, t, source.getCurrentTime());
                System.out.println(str);
            }
        };
    }

    private static
    void RunConsole() {
        int delta = 50;
//        int delta = 51;

//        ConsoleDemo(delta, 250, false, 0);
//        ConsoleDemo(delta, 250, false, 1);
        ConsoleDemo(delta, 250, false, 2);
//        ConsoleDemo(delta, 250, false, Tween.INFINITY);
//
//        ConsoleDemo(delta, 250, true, 1);
//        ConsoleDemo(delta, 250, true, 2);
//        ConsoleDemo(delta, 250, true, 4);
//        ConsoleDemo(delta, 250, true, Tween.INFINITY);
    }

    private static
    void ConsoleDemo(int delta, final int delay, final boolean isAutoReverse, final int reverseCount) {
        final int terminalwidth = 50;

        Bugtest[] bugs;

        bugs = new Bugtest[]{
                        new Bugtest('a'),
                        new Bugtest('b'),
//                        new Bugtest('c')
        };

        Timeline timeline = tweenEngine.createSequential()
                                       // callback text is ABOVE the line that it applies to
                                       .addCallback(buildCallback("TL", TweenCallback.Events.ANY))
                                       .delay(delay)
                                       .push(bugs[0].t)
                                       .beginParallel()
                                       .push(bugs[1].t)
////                                    .beginSequence()
////                                        .push(bugs[2].t) // third tween not even needed
////                                        .end()
                                       .end()
//                                    .push(bugs[2].t)
                                    ;

        if (isAutoReverse) {
            timeline.repeatAutoReverse(reverseCount, 500);
        } else {
            timeline.repeat(reverseCount, 500);
        }

        timeline.start();


        boolean permitFlip = false;
        boolean flipped = false;
        do {
            if (permitFlip && !flipped && timeline.getCurrentTime() > 0.5F) {
                flipped = true;
                delta = -delta;
            }

            drawConsole(timeline, terminalwidth, bugs);
            timeline.update(delta);

            try {
                Thread.sleep(30);
            } catch (Throwable ignored) {
            }
        } while (!timeline.isFinished());
    }

    private static
    void drawConsole(final Timeline timeline, final int terminalWidth, final Bugtest[] bugs) {
        char[] prog = new char[terminalWidth + 1];

        //just for drawing
        for (int i = 0; i <= terminalWidth; i++) {
            prog[i] = '-';
        }
        for (int i = 0; i < bugs.length; i++) {
            Bugtest bug = bugs[i];
            int i1 = (int) (bug.val * terminalWidth);
            prog[i1] = bug.name;
        }

        System.out.print(prog);
        System.out.print(String.format(Locale.US, "\t%s:%.1f %s",
                                       timeline.getDirection() ? "F" : "R",
                                       timeline.getCurrentTime(),
                                       timeline.isFinished() ? "don" : "run"));

        for (int i = 0; i < bugs.length; i++) {
            Bugtest bug = bugs[i];
            System.out.print("\t\t" + String.format(Locale.US, "%s: %.1f %s",
                                                    bug.t.getDirection() ? "F" : "R",
                                                    bug.t.getCurrentTime(),
                                                    timeline.isFinished() ? "don" : "run"));
        }
        System.out.println();
    }

    static
    class A implements TweenAccessor<Bugtest> {
        @Override
        public
        int getValues(Bugtest b, int m, float[] val) {
            val[0] = b.val;
            return 1;
        }

        @Override
        public
        void setValues(Bugtest b, int m, float[] val) {
            b.val = val[0];
        }
    }


    static
    class Bugtest {
        public float val = 0; // tweened
        public char name;
        public Tween t;


        Bugtest(char name) {
            this.name = name;
            t = tweenEngine.to(this, 0, 1000)
                           .target(1)
                           .addCallback(buildCallback("" + name, TweenCallback.Events.ANY));
        }
    }
}
