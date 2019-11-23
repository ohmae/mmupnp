/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da

interface DaStateVariable {
    val isSendEvents: Boolean
    val isMulticast: Boolean
    val name: String
    val dataType: String
    val allowedValueList: List<String>
    val defaultValue: String?
    val minimum: String?
    val maximum: String?
    val step: String?
}
