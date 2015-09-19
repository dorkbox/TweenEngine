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
package dorkbox.util;

import dorkbox.util.tweenengine.BaseTween;
import dorkbox.util.tweenengine.Timeline;
import dorkbox.util.tweenengine.Tween;
import dorkbox.util.tweenengine.TweenAccessor;
import dorkbox.util.tweenengine.TweenCallback;
import dorkbox.util.tweenengine.primitives.MutableFloat;

import java.util.Locale;

public
class ConsoleTests {


    public static
    void main(String[] args) {
        // Tests

//        float step = 0.0001f;
//        System.out.println("-----------------------------------------------");
//        System.out.println("Tween (v:value, lt:localTime, gt:globalTime)");
//        System.out.println("-----------------------------------------------");
//        testTween(step);
//
//        System.out.println("-----------------------------------------------");
//        System.out.println("Timeline (v:value, lt:localTime, gt:globalTime)");
//        System.out.println("-----------------------------------------------");
//        testTimeline(step);

        Bugtest21();
    }

    private static
    void testTween(float step) {
        MutableFloat target = new MutableFloat(0);
        TweenCallback t1 = buildCallback("t", target);
        t1.setTriggers(TweenCallback.Events.ANY);

        Tween t = Tween.to(target, 0, 1000)
                       .target(1)
                       .repeat(2, 1)
                       .delay(1)
                       .addCallback(t1)
                       .start();

        float acc = 0;
        while (acc < t.getFullDuration() + 1) {
            t.update(step);
            acc += step;
        }
        System.out.println("-----------------------------------------------");
        while (acc > -1) {
            t.update(-step);
            acc -= step;
        }
        System.out.println("-----------------------------------------------");
        while (acc < t.getFullDuration() + 1) {
            t.update(step);
            acc += step;
        }
    }

    private static
    void testTimeline(float step) {
        MutableFloat target1 = new MutableFloat(0);
        MutableFloat target2 = new MutableFloat(0);
        MutableFloat target3 = new MutableFloat(0);
        Tween t1 = Tween.call(buildCallback("t1", TweenCallback.Events.ANY));
        Tween t2 = Tween.call(buildCallback("t2", TweenCallback.Events.ANY));
        Tween t3 = Tween.call(buildCallback("t3", TweenCallback.Events.ANY));

        TweenCallback tl1 = buildCallback("TL", TweenCallback.Events.ANY);
        Timeline tl = Timeline.createSequence()
                              .push(t1)
                              .pushPause(1)
                              .push(t2)
                              .pushPause(1)
                              .push(t3)
                              .repeat(2, 1)
                              .addCallback(tl1)
                              .start();

        float acc = 0;
        while (acc < tl.getFullDuration() + 1) {
            tl.update(step);
            acc += step;
        }
        System.out.println("-----------------------------------------------");
        while (acc > -1) {
            tl.update(-step);
            acc -= step;
        }
        System.out.println("-----------------------------------------------");
        while (acc < tl.getFullDuration() + 1) {
            tl.update(step);
            acc += step;
        }
    }

    private static
    TweenCallback buildCallback(final String name, final MutableFloat target) {
        return new TweenCallback() {
            @Override
            public
            void onEvent(int type, BaseTween<?> source) {
                String t = type == TweenCallback.Events.BEGIN
                           ? "BEGIN        "
                           : type == TweenCallback.Events.START
                             ? "START        "
                             : type == TweenCallback.Events.END
                               ? "END          "
                               : type == TweenCallback.Events.COMPLETE
                                 ? "COMPLETE     "
                                 : type == TweenCallback.Events.BACK_BEGIN
                                   ? "BACK_BEGIN   "
                                   : type == TweenCallback.Events.BACK_START
                                     ? "BACK_START   "
                                     : type == TweenCallback.Events.BACK_END
                                       ? "BACK_END     "
                                       : type == TweenCallback.Events.BACK_COMPLETE ? "BACK_COMPLETE" : "???";

                String str = String.format(Locale.US, "%s %s   lt %.2f   v %.2", name, t, source.getCurrentTime(), target.floatValue());
                System.out.println(str);
            }
        };
    }

    private static
    TweenCallback buildCallback(final String name, final int flags) {
        return new TweenCallback(flags) {
            @Override
            public
            void onEvent(int type, BaseTween<?> source) {
                String t = type == TweenCallback.Events.BEGIN
                           ? "BEGIN        "
                           : type == TweenCallback.Events.START
                             ? "START        "
                             : type == TweenCallback.Events.END
                               ? "END          "
                               : type == TweenCallback.Events.COMPLETE
                                 ? "COMPLETE     "
                                 : type == TweenCallback.Events.BACK_BEGIN
                                   ? "BACK_BEGIN   "
                                   : type == TweenCallback.Events.BACK_START
                                     ? "BACK_START   "
                                     : type == TweenCallback.Events.BACK_END
                                       ? "BACK_END     "
                                       : type == TweenCallback.Events.BACK_COMPLETE ? "BACK_COMPLETE" : "???";

                String str = String.format(Locale.US, "%s %s   lt %d", name, t, source.getCurrentTime());
                System.out.println(str);
            }
        };
    }

    private static
    void Bugtest21() {
        final int terminalwidth = 50;

        // if the delta step doesn't line up with duration or delays, it won't line up. The event order/notifications will be
        // correct.
//        int dt = 25;
//        int dt = 23;
//        int dt = 50;
        int dt = 51;
//        int dt = 1003 + 250;
        Bugtest[] bugs;


        Tween.registerAccessor(Bugtest.class, new A());

        bugs = new Bugtest[]{
                        new Bugtest('a'),
                        new Bugtest('b'),
//                        new Bugtest('c')
        };

        Timeline timeline = Timeline.createSequence()
                                    .addCallback(buildCallback("TL", TweenCallback.Events.ANY))
                                    .delay(250)
                                    .push(bugs[0].t)
                                    .beginParallel()
                                        .push(bugs[1].t)
////                                        .beginSequence()
////                                            .push(bugs[2].t) // third tween not even needed
////                                        .end()
                                    .end()
//                                    .repeatAutoReverse(1, 500)
                                    .repeatAutoReverse(2, 500)
//                                    .repeatAutoReverse(4, 500)
//                                    .repeat(1, 500)
//                                    .repeat(4, 500)
//                                    .repeat(Tween.INFINITY, 500)
//                                    .onUpdateStart(new TweenAction<Timeline>() {
//                                        @Override
//                                        public
//                                        void action(final Timeline tween) {
//                                            System.out.println("start update");
//                                        }
//                                    })
//                                    .onUpdateEnd(new TweenAction<Timeline>() {
//                                        @Override
//                                        public
//                                        void action(final Timeline tween) {
//                                            System.out.println("end update");
//                                        }
//                                    })
//                                    .name('*')
                                    .start();


        boolean permitFlip = true;

        boolean flipped = false;
        do {
            if (permitFlip && !flipped && timeline.getCurrentTime() >= 500) {
                flipped = true;
                dt = -dt;
            }

            timeline.update(dt);

            drawConsole(timeline, terminalwidth, bugs);

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
        System.out.print(String.format(Locale.US, "\t%s:%4d%s", timeline.getDirection() ? "F" : "R", timeline.getCurrentTime(), timeline.isFinished() ? "*" : " ") +
                         (timeline.isInsideDelay() ? "D" : " "));

        for (int i = 0; i < bugs.length; i++) {
            Bugtest bug = bugs[i];
            System.out.print("\t\t" + String.format(Locale.US, "%s:%.2f", bug.t.getDirection() ? "F" : "R", bug.val) +
                             "," +
                             String.format(Locale.US, "%4d%s", bug.t.getCurrentTime(), bug.t.isFinished() ? "*" : " ") +
                            (bug.t.isInsideDelay() ? "D" : " "));
        }
        System.out.println();
    }

    static
    class A implements TweenAccessor<Bugtest> {
        public
        int getValues(Bugtest b, int m, float[] val) {
            val[0] = b.val;
            return 1;
        }

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
            t = Tween.to(this, 0, 1000)
                     .target(1).addCallback(buildCallback(""+name, TweenCallback.Events.ANY));
//            t.name(name);
        }
    }
}
