/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp

/**
 * Filter for specifying Icon to download Icon binary data when loading Device.
 *
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
interface IconFilter {
    /**
     * Select Icon to download binary data.
     *
     * Icon included in the List returned by return value is downloaded.
     * It is not called if there is no Icon information.
     *
     * @param list List of Icon described in Device. It can not be null or empty.
     * @return List of Icon to download
     */
    operator fun invoke(list: List<Icon>): List<Icon>
}
