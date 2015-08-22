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
package aurelienribon.tweenengine;

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
     * members of the {@link aurelienribon.tweenengine.TweenCallback.Events} class, are:
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
     * members of the {@link aurelienribon.tweenengine.TweenCallback.Events} class, are:
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
    void setTriggers(final int triggers) {
        this.triggers = triggers;
    }

    public abstract void onEvent(int type, BaseTween<?> source);

    public static final
    class Events {
        public static final int BEGIN = 0x01;
        public static final int START = 0x02;
        public static final int END = 0x04;
        public static final int COMPLETE = 0x08;
        public static final int BACK_BEGIN = 0x10;
        public static final int BACK_START = 0x20;
        public static final int BACK_END = 0x40;
        public static final int BACK_COMPLETE = 0x80;
        public static final int ANY_FORWARD = 0x0F;
        public static final int ANY_BACKWARD = 0xF0;
        public static final int ANY = 0xFF;

        private
        Events() {
        }
    }
}
