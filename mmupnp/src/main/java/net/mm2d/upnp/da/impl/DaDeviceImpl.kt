/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.upnp.da.impl

import net.mm2d.upnp.da.DaAction
import net.mm2d.upnp.da.DaDevice
import net.mm2d.upnp.da.DaIcon
import net.mm2d.upnp.da.DaService

class DaDeviceImpl(
    override val description: String,
    override val udn: String,
    override val upc: String?,
    override val deviceType: String,
    override val friendlyName: String,
    override val manufacture: String?,
    override val manufactureUrl: String?,
    override val modelName: String,
    override val modelUrl: String?,
    override val modelDescription: String?,
    override val modelNumber: String?,
    override val serialNumber: String?,
    override val presentationUrl: String?,
    override val iconList: List<DaIcon>,
    override val serviceList: List<DaService>,
    override val parent: DaDevice?,
    override val deviceList: List<DaDevice>
) : DaDevice {
    override val baseUrl: String
        get() = ""
    override val isEmbeddedDevice: Boolean
        get() = parent != null

    override fun getValue(name: String): String? = null
    override fun getValueWithNamespace(namespace: String, name: String): String? = null
    override fun findServiceById(id: String): DaService? = null
    override fun findServiceByType(type: String): DaService? = null
    override fun findAction(name: String): DaAction? = null
    override fun findDeviceByType(deviceType: String): DaDevice? = null
    override fun findDeviceByTypeRecursively(deviceType: String): DaDevice? = null

    class Builder {
        private var description: String? = null
        private var udn: String? = null
        private var upc: String? = null
        private var deviceType: String? = null
        private var friendlyName: String? = null
        private var manufacture: String? = null
        private var manufactureUrl: String? = null
        private var modelName: String? = null
        private var modelUrl: String? = null
        private var modelDescription: String? = null
        private var modelNumber: String? = null
        private var serialNumber: String? = null
        private var presentationUrl: String? = null
        private val iconList = mutableListOf<DaIcon>()
        private val serviceBuilderList = mutableListOf<DaServiceImpl.Builder>()
        private var deviceBuilderList: List<Builder> = emptyList()
        private val tagMap: MutableMap<String, MutableMap<String, String>>

        init {
            tagMap = mutableMapOf("" to mutableMapOf())
        }

        fun build(parent: DaDevice? = null): DaDeviceImpl {
            val description = description
                ?: throw IllegalStateException("description must be set.")
            val deviceType = deviceType
                ?: throw IllegalStateException("deviceType must be set.")
            val friendlyName = friendlyName
                ?: throw IllegalStateException("friendlyName must be set.")
            val manufacture = manufacture
                ?: throw IllegalStateException("manufacturer must be set.")
            val modelName = modelName
                ?: throw IllegalStateException("modelName must be set.")
            val udn = udn
                ?: throw IllegalStateException("UDN must be set.")
            val udnSet = collectUdn()

            return DaDeviceImpl(
                parent = parent,
                description = description,
                udn = udn,
                upc = upc,
                deviceType = deviceType,
                friendlyName = friendlyName,
                manufacture = manufacture,
                manufactureUrl = manufactureUrl,
                modelName = modelName,
                modelUrl = modelUrl,
                modelDescription = modelDescription,
                modelNumber = modelNumber,
                serialNumber = serialNumber,
                presentationUrl = presentationUrl,
                iconList = iconList,
                serviceList = serviceBuilderList.map { it.build() },
                deviceList = deviceBuilderList.map { it.build() }
            )
        }

        private fun collectUdn(): Set<String> = mutableSetOf<String>().also {
            collectUdn(this, it)
        }

        private fun collectUdn(builder: Builder, outSet: MutableSet<String>) {
            builder.udn?.let { outSet.add(it) }
            builder.deviceBuilderList.forEach { collectUdn(it, outSet) }
        }

        fun createEmbeddedDeviceBuilder(): Builder {
            val builder = Builder()
            builder.setDescription(description!!)
            return builder
        }

        fun setDescription(description: String): Builder = apply {
            this.description = description
        }

        fun setUdn(udn: String): Builder = apply {
            this.udn = udn
        }

        fun setUpc(upc: String): Builder = apply {
            this.upc = upc
        }

        fun setDeviceType(deviceType: String): Builder = apply {
            this.deviceType = deviceType
        }

        fun setFriendlyName(friendlyName: String): Builder = apply {
            this.friendlyName = friendlyName
        }

        fun setManufacture(manufacture: String): Builder = apply {
            this.manufacture = manufacture
        }

        fun setManufactureUrl(manufactureUrl: String): Builder = apply {
            this.manufactureUrl = manufactureUrl
        }

        fun setModelName(modelName: String): Builder = apply {
            this.modelName = modelName
        }

        fun setModelUrl(modelUrl: String): Builder = apply {
            this.modelUrl = modelUrl
        }

        fun setModelDescription(modelDescription: String): Builder = apply {
            this.modelDescription = modelDescription
        }

        fun setModelNumber(modelNumber: String): Builder = apply {
            this.modelNumber = modelNumber
        }

        fun setSerialNumber(serialNumber: String): Builder = apply {
            this.serialNumber = serialNumber
        }

        fun setPresentationUrl(presentationUrl: String): Builder = apply {
            this.presentationUrl = presentationUrl
        }

        fun addIcon(icon: DaIcon): Builder = apply {
            iconList.add(icon)
        }

        fun addServiceBuilder(builder: DaServiceImpl.Builder): Builder = apply {
            serviceBuilderList.add(builder)
        }

        fun setEmbeddedDeviceBuilderList(builderList: List<Builder>): Builder = apply {
            deviceBuilderList = builderList
        }

        fun getEmbeddedDeviceBuilderList(): List<Builder> = deviceBuilderList

        // DeviceDescriptionにはAttributeは使用されていないためAttributeには非対応
        fun putTag(namespace: String?, tag: String, value: String): Builder = apply {
            val namespaceUri = namespace ?: ""
            val map = tagMap[namespaceUri] ?: mutableMapOf<String, String>().also {
                tagMap[namespaceUri] = it
            }
            map[tag] = value
        }

        fun toDumpString(): String = buildString {
            append("DeviceBuilder")
            append("\nSSDP:")
            append("\nDESCRIPTION:")
            append(description)
            if (serviceBuilderList.size != 0) {
                append("\n")
                serviceBuilderList.forEach {
                    append(it.toDumpString())
                }
            }
        }
    }
}
