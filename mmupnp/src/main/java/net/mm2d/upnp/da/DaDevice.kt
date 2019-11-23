/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da

interface DaDevice {
    val description: String
    val baseUrl: String
    val udn: String
    val upc: String?
    val deviceType: String
    val friendlyName: String
    val manufacture: String?
    val manufactureUrl: String?
    val modelName: String
    val modelUrl: String?
    val modelDescription: String?
    val modelNumber: String?
    val serialNumber: String?
    val presentationUrl: String?
    val iconList: List<DaIcon>
    val serviceList: List<DaService>
    val isEmbeddedDevice: Boolean
    val parent: DaDevice?
    val deviceList: List<DaDevice>

    fun getValue(name: String): String?
    fun getValueWithNamespace(
        namespace: String,
        name: String
    ): String?

    fun findServiceById(id: String): DaService?
    fun findServiceByType(type: String): DaService?
    fun findAction(name: String): DaAction?
    fun findDeviceByType(deviceType: String): DaDevice?
    fun findDeviceByTypeRecursively(deviceType: String): DaDevice?
}
