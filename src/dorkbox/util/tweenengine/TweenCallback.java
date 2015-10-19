/*
 * Copyright 2012 Aurelien Ribon
 *  Copyright 2015 dorkbox, llc
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
package dorkbox.util.tweenengine;


/**
 * Specifies the triggers for a callback. The available triggers, listed as
 * members of the {@link TweenCallback.Events} class, are:
 * <p/>
 *
 * <b>BEGIN</b>: right after the delay (if any)<br/>
 * <b>START</b>: at each iteration beginning<br/>
 * <b>END</b>: at each iteration ending, before the repeat delay<br/>
 * <b>COMPLETE</b>: at last END event<br/>
 * <b>BACK_BEGIN</b>: at the beginning of the first backward iteration<br/>
 * <b>BACK_START</b>: at each backward iteration beginning, after the repeat delay<br/>
 * <b>BACK_END</b>: at each backward iteration ending<br/>
 * <b>BACK_COMPLETE</b>: at last BACK_END event
 * <p/>
 * <p/>
 * Timeline events are ALWAYS happen before children events (begin/start), or after (complete/end)
 * <pre> {@code
 *
 *
 * DELAY - (delay) initial start delay, only happens once, during init
 * R.DELAY - (repeatDelay) delay between repeat iterations, if there are more than one.
 *
 * there are two modes for repeat. LINEAR and AUTO_REVERSE
 *
 * LINEAR:
 *                BEGIN                                     COMPLETE
 *                START      END                 START      END
 *                v          v                   v          v
 * |---[DELAY]----[XXXXXXXXXX]->>-[R.DELAY]-->>--[XXXXXXXXXX]
 *
 *
 * AUTO_REVERSE
 *                BEGIN      COMPLETE
 *                START      END
 *                v          v
 * |---[DELAY]----[XXXXXXXXXX]──────────-─────╮
 *            ╭╴  [XXXXXXXXXX]-<<-[R.DELAY] <─╯
 *            │   ^          ^
 *            │   bEND       bSTART
 *            │   bCOMPLETE  bBEGIN
 *            │
 *            ╰╴> [R.DELAY]->>-[XXXXXXXXXX]  ╶╮
 *            ╭╴  [XXXXXXXXXX]-<<-[R.DELAY] <─╯
 *            ╰╴> [R.DELAY]->>-[XXXXXXXXXX]  ...
 *
 * }</pre>
 */
@SuppressWarnings("unused")
public abstract
class TweenCallback {
    int triggers;

    public
    TweenCallback() {
        this.triggers = Events.COMPLETE;
    }


    /**
     * Specifies the triggers for a callback. The available triggers, listed as
     * members of the {@link TweenCallback.Events} class, are:
     * <p/>
     *
     * <b>BEGIN</b>: right after the delay (if any)<br/>
     * <b>START</b>: at each iteration beginning<br/>
     * <b>END</b>: at each iteration ending, before the repeat delay<br/>
     * <b>COMPLETE</b>: at last END event<br/>
     * <b>BACK_BEGIN</b>: at the beginning of the first backward iteration<br/>
     * <b>BACK_START</b>: at each backward iteration beginning, after the repeat delay<br/>
     * <b>BACK_END</b>: at each backward iteration ending<br/>
     * <b>BACK_COMPLETE</b>: at last BACK_END event
     * <p/>
     * <p/>
     * Timeline events are ALWAYS happen before children events (begin/start), or after (complete/end)
     * <pre> {@code
     *
     *
     * DELAY - (delay) initial start delay, only happens once, during init
     * R.DELAY - (repeatDelay) delay between repeat iterations, if there are more than one.
     *
     * there are two modes for repeat. LINEAR and AUTO_REVERSE
     *
     * LINEAR:
     *                BEGIN                                     COMPLETE
     *                START      END                 START      END
     *                v          v                   v          v
     * |---[DELAY]----[XXXXXXXXXX]->>-[R.DELAY]-->>--[XXXXXXXXXX]
     *
     *
     * AUTO_REVERSE
     *                BEGIN      COMPLETE
     *                START      END
     *                v          v
     * |---[DELAY]----[XXXXXXXXXX]──────────-─────╮
     *            ╭╴  [XXXXXXXXXX]-<<-[R.DELAY] <─╯
     *            │   ^          ^
     *            │   bEND       bSTART
     *            │   bCOMPLETE  bBEGIN
     *            │
     *            ╰╴> [R.DELAY]->>-[XXXXXXXXXX]  ╶╮
     *            ╭╴  [XXXXXXXXXX]-<<-[R.DELAY] <─╯
     *            ╰╴> [R.DELAY]->>-[XXXXXXXXXX]  ...
     *
     * }</pre>
     *
     * @param triggers one or more triggers, separated by the '|' operator.
     */
    public
    TweenCallback(final int triggers) {
        this.triggers = triggers;
    }

    public abstract void onEvent(int type, BaseTween<?> source);

    public static final
    class Events {
        /** <b>BEGIN</b>: right after the delay (if any) */
        public static final int BEGIN = 1;              // 00000001
        /** <b>START</b>: at each iteration beginning */
        public static final int START = 1 << 1;         // 00000010
        /** <b>END</b>: at each iteration ending, before the repeat delay */
        public static final int END = 1 << 2;           // 00000100
        /** <b>COMPLETE</b>: at last END event */
        public static final int COMPLETE = 1 << 3;      // 00001000

        /** <b>BACK_BEGIN</b>: at the beginning of the first backward iteration */
        public static final int BACK_BEGIN = 1 << 4;    // 00010000
        /** <b>BACK_START</b>: at each backward iteration beginning, after the repeat delay */
        public static final int BACK_START = 1 << 5;    // 00100000
        /** <b>BACK_END</b>: at each backward iteration ending */
        public static final int BACK_END = 1 << 6;      // 01000000
        /** <b>BACK_COMPLETE</b>: at last BACK_END event */
        public static final int BACK_COMPLETE = 1 << 7; // 10000000

        public static final int ANY_FORWARD = 0x0F;     // 00001111
        public static final int ANY_BACKWARD = 0xF0;    // 11110000
        public static final int ANY = 0xFF;             // 11111111

        private
        Events() {
        }
    }
}
