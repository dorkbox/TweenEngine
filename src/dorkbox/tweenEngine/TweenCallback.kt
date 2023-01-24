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
package dorkbox.tweenEngine

/**
 * Specifies the triggers for a callback. The available triggers, listed as
 * members of the [TweenCallback.Events] class, are:
 *
 *
 * - **BEGIN**: right after the delay (if any)
 *
 * - **START**: at each iteration beginning
 *
 * - **END**: at each iteration ending, before the repeat delay
 *
 * - **COMPLETE**: at last END event
 *
 * - **BACK_BEGIN**: at the beginning of the first backward iteration
 *
 * - **BACK_START**: at each backward iteration beginning, after the repeat delay
 *
 * - **BACK_END**: at each backward iteration ending
 *
 * - **BACK_COMPLETE**: at last BACK_END event
 *
 *
 * Timeline events are ALWAYS happen before children events (begin/start), or after (complete/end)
 * ```
 * DELAY - (delay) initial start delay, only happens once, during init
 * R.DELAY - (repeatDelay) delay between repeat iterations, if there are more than one.
 *
 * there are two modes for repeat. LINEAR and AUTO_REVERSE
 *
 * LINEAR:
 *
 * BEGIN                                     COMPLETE
 * START      END                 START      END
 * v          v                   v          v
 * |---[DELAY]----[XXXXXXXXXX]->>-[R.DELAY]-->>--[XXXXXXXXXX]
 *
 *
 * AUTO_REVERSE:
 *
 * BEGIN      COMPLETE
 * START      END
 * v          v
 * |---[DELAY]----[XXXXXXXXXX]──────────-─────╮
 *            ╭╴  [XXXXXXXXXX]-<<-[R.DELAY] <─╯
 *            │   ^          ^
 *            │   bEND       bSTART
 *            │   bCOMPLETE  bBEGIN
 *            │
 *            ╰╴> [R.DELAY]->>-[XXXXXXXXXX]  ╶╮
 *            ╭╴  [XXXXXXXXXX]-<<-[R.DELAY] <─╯
 *            ╰╴> [R.DELAY]->>-[XXXXXXXXXX]  ...
 * ```
 */
abstract class TweenCallback<T: BaseTween<T>>(var triggers: Int = Events.COMPLETE)  {
    object Events {
        /** **BEGIN**: right after the delay (if any)  */
        const val BEGIN = 1 // 00000001

        /** **START**: at each iteration beginning  */
        const val START = 1 shl 1 // 00000010

        /** **END**: at each iteration ending, before the repeat delay  */
        const val END = 1 shl 2 // 00000100

        /** **COMPLETE**: at last END event  */
        const val COMPLETE = 1 shl 3 // 00001000

        /** **BACK-BEGIN**: at the beginning of the first backward iteration  */
        const val BACK_BEGIN = 1 shl 4 // 00010000

        /** **BACK-START**: at each backward iteration beginning, after the repeat delay  */
        const val BACK_START = 1 shl 5 // 00100000

        /** **BACK-END**: at each backward iteration ending  */
        const val BACK_END = 1 shl 6 // 01000000

        /** **BACK-COMPLETE**: at last BACK_END event  */
        const val BACK_COMPLETE = 1 shl 7 // 10000000


        /** **ANY-FORWARD**: at each backward iteration ending  */
        const val ANY_FORWARD = 0x0F // 00001111
        const val ANY_BACKWARD = 0xF0 // 11110000
        const val ANY = 0xFF // 11111111
    }

    abstract fun onEvent(type: Int, source: T)
}
