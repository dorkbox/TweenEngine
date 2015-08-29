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
 *
 *
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
package dorkbox.util.tweenengine;

/**
 * TweenCallbacks are used to trigger actions at some specific times. They are
 * used in both Tweens and Timelines. The moment when the callback is
 * triggered depends on its registered triggers:
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
 *
 * <pre> {@code
 * forward :      BEGIN                                   COMPLETE
 * forward :      START    END      START    END      START    END
 * |--------------[XXXXXXXXXX]------[XXXXXXXXXX]------[XXXXXXXXXX]
 * backward:      bEND  bSTART      bEND  bSTART      bEND  bSTART
 * backward:      bCOMPLETE                                 bBEGIN
 * }</pre>
 *
 * @see Tween
 * @see Timeline
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 * @author dorkbox, llc
 */
@SuppressWarnings("unused")
public abstract
class TweenCallback {
    volatile int triggers = Events.COMPLETE;

    public
    TweenCallback() {
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
     *
     * <pre> {@code
     * forward :      BEGIN                                   COMPLETE
     * forward :      START    END      START    END      START    END
     * |--------------[XXXXXXXXXX]------[XXXXXXXXXX]------[XXXXXXXXXX]
     * backward:      bEND  bSTART      bEND  bSTART      bEND  bSTART
     * backward:      bCOMPLETE                                 bBEGIN
     * }</pre>
     *
     * @param triggers one or more triggers, separated by the '|' operator.
     */
    public
    TweenCallback(final int triggers) {
        this.triggers = triggers;
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
     *
     * <pre> {@code
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
     *                BEGIN                   COMPLETE
     *                START      END
     *                v          v            v
     * |---[DELAY]----[XXXXXXXXXX]->>-[R.DELAY]  ╶╮
     *            ╭╴  [R.DELAY]-<<-[XXXXXXXXXX] <─╯
     *            │   ^            ^          ^
     *            │                bEND       bSTART
     *            │   bCOMPLETE               bBEGIN
     *            │
     *            ╰╴> [XXXXXXXXXX]->>-[R.DELAY]  ╶╮
     *            ╭╴  [R.DELAY]-<<-[XXXXXXXXXX] <─╯
     *            ╰╴> [XXXXXXXXXX]->>-[R.DELAY]  ...
     *
     * }</pre>
     *
     * @param triggers one or more triggers, separated by the '|' operator.
     */
    public
    void setTriggers(final int triggers) {
        this.triggers = triggers;
    }

    public abstract void onEvent(int type, BaseTween<?> source);

    public static final
    class Events {
        public static final int BEGIN = 1 << 0;         // 00000001
        public static final int START = 1 << 1;         // 00000010
        public static final int END = 1 << 2;           // 00000100
        public static final int COMPLETE = 1 << 3;      // 00001000

        public static final int BACK_BEGIN = 1 << 4;    // 00010000
        public static final int BACK_START = 1 << 5;    // 00100000
        public static final int BACK_END = 1 << 6;      // 01000000
        public static final int BACK_COMPLETE = 1 << 7; // 10000000

        public static final int ANY_FORWARD = 0x0F;     // 00001111
        public static final int ANY_BACKWARD = 0xF0;    // 11110000
        public static final int ANY = 0xFF;             // 11111111

        private
        Events() {
        }
    }
}
