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

import dorkbox.tweenEngine.BaseTween
import dorkbox.tweenEngine.Timeline
import dorkbox.tweenEngine.TweenAccessor
import dorkbox.tweenEngine.TweenEngine
import dorkbox.tweenEngine.TweenEvents
import java.util.*

class ConsoleDemo(delta: Int, delay: Int, isAutoReverse: Boolean, reverseCount: Int) {
    init {
        var delta: Int = delta
        val terminalwidth = 50
        val bugs: Array<Bugtest>

        bugs = arrayOf(
                Bugtest('a'),
                Bugtest('b'))

        val timeline: Timeline = tweenEngine.createSequential() // callback text is ABOVE the line that it applies to
                .addCallback(TweenEvents.ANY) { "TL" }
                .delay(delay.toFloat())
                .push(bugs[0].t)
                .beginParallel()
                    .push(bugs[1].t)
                    //.beginSequence()
                    //.push(bugs[2].t) // third tween not even needed
                    //.end()

                .end()
                //.push(bugs[2].t)

        if (isAutoReverse) {
            timeline.repeatAutoReverse(reverseCount, 500f)
        } else {
            timeline.repeat(reverseCount, 500f)
        }
        timeline.start()


        val permitFlip = false
        var flipped = false
        do {
            if (permitFlip && !flipped && timeline.getCurrentTime() > 0.5f) {
                flipped = true
                delta = -delta
            }
            drawConsole(timeline, terminalwidth, bugs)
            timeline.update(delta.toFloat())
            try {
                Thread.sleep(30)
            } catch (ignored: Throwable) {
            }
        } while (!timeline.isFinished())
    }

    internal class A : TweenAccessor<Bugtest> {
        override fun getValues(target: Bugtest, tweenType: Int, returnValues: FloatArray): Int {
            returnValues[0] = target.tween
            return 1
        }

        override fun setValues(target: Bugtest, tweenType: Int, newValues: FloatArray) {
            target.tween = newValues[0]
        }
    }

    internal class Bugtest(var name: Char) {
        var tween = 0f // tweened
        var t = tweenEngine.to(this, 0, 1000f)
                .value(1f)
                .addCallback(TweenEvents.ANY) {"" + name }

    }

    companion object {

        private val tweenEngine = TweenEngine.create()
                .unsafe()
                .setWaypointsLimit(10)
                .setCombinedAttributesLimit(3)
                .registerAccessor(Bugtest::class.java, A())
                .build()

        @JvmStatic
        fun main(args: Array<String>) {
            // Tests
            RunConsole()
        }

        private fun <T : BaseTween<T>> buildCallback(name: String, triggers: Int): T.(triggers: Int)->Unit {
            return { type ->
                val t: String
                t = if (type == TweenEvents.BEGIN) {
                    "BEGIN        "
                } else if (type == TweenEvents.START) {
                    "START        "
                } else if (type == TweenEvents.END) {
                    "END          "
                } else if (type == TweenEvents.COMPLETE) {
                    "COMPLETE     "
                } else if (type == TweenEvents.BACK_BEGIN) {
                    "BACK_BEGIN   "
                } else if (type == TweenEvents.BACK_START) {
                    "BACK_START   "
                } else if (type == TweenEvents.BACK_END) {
                    "BACK_END     "
                } else if (type == TweenEvents.BACK_COMPLETE) {
                    "BACK_COMPLETE"
                } else {
                    "???"
                }
                val str = String.format(Locale.US, "%s %s   lt %3f", name, t, getCurrentTime())
                println(str)
            }
        }

        private fun RunConsole() {
            val delta = 50
            //        int delta = 51;

//        ConsoleDemo(delta, 250, false, 0);
//        ConsoleDemo(delta, 250, false, 1);
            ConsoleDemo(delta, 250, false, 2)
            //        ConsoleDemo(delta, 250, false, Tween.INFINITY);
//
//        ConsoleDemo(delta, 250, true, 1);
//        ConsoleDemo(delta, 250, true, 2);
//        ConsoleDemo(delta, 250, true, 4);
//        ConsoleDemo(delta, 250, true, Tween.INFINITY);
        }

        private fun drawConsole(timeline: Timeline, terminalWidth: Int, bugs: Array<Bugtest>) {
            val prog = CharArray(terminalWidth + 1)

            //just for drawing
            for (i in 0..terminalWidth) {
                prog[i] = '-'
            }
            for (i in bugs.indices) {
                val bug = bugs[i]
                val i1 = (bug.tween * terminalWidth).toInt()
                prog[i1] = bug.name
            }

            print(prog)

            print(String.format(Locale.US, "\t%s:%.1f %s",
                    if (timeline.getDirection()) "F" else "R",
                    timeline.getCurrentTime(),
                    if (timeline.isFinished()) "don" else "run"))
            for (i in bugs.indices) {
                val bug = bugs[i]

                print("\t\t" + String.format(Locale.US, "%s: %.1f %s",
                        if (bug.t.getDirection()) "F" else "R",
                        bug.t.getCurrentTime(),
                        if (timeline.isFinished()) "don" else "run"))
            }
            println()
        }
    }
}
