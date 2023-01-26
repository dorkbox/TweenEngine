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
package dorkbox.util.swing

import java.awt.Container
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com
 * @author dorkbox, llc
 */
object SwingHelper {
    @JvmStatic
    fun showOnSameScreenAsMouseCenter(frame: Container) {
        val mouseLocation = MouseInfo.getPointerInfo()
                .location
        val device: GraphicsDevice
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val lstGDs = ge.screenDevices
        val lstDevices = ArrayList<GraphicsDevice>(lstGDs.size)
        for (gd in lstGDs) {
            val gc = gd.defaultConfiguration
            val screenBounds = gc.bounds
            if (screenBounds.contains(mouseLocation)) {
                lstDevices.add(gd)
            }
        }
        device = if (lstDevices.size > 0) {
            lstDevices[0]
        } else {
            ge.defaultScreenDevice
        }
        val bounds = device.defaultConfiguration
                .bounds
        frame.setLocation(bounds.x + bounds.width / 2 - frame.width / 2, bounds.y + bounds.height / 2 - frame.height / 2)
    }
}
