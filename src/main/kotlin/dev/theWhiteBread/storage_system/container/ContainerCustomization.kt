package dev.theWhiteBread.storage_system.container

import kotlinx.serialization.Serializable
import org.bukkit.Material

typealias ReserveAmount = Int

@Serializable
data class ContainerCustomization(
    var displayName: String? = null,
    var description: List<String>? = null,
    var displayItem: Material? = null,
    var exporter: ChunkStorageExporter = ChunkStorageExporter(exporting = false, whiteList = emptyList()),
    var importer: ChunkStorageImporter = ChunkStorageImporter(importFrom = emptyList(), whiteList = emptyList())
)

@Serializable
data class ChunkStorageExporter(
    var exporting: Boolean,
    var whiteList: List<Pair<Material, ReserveAmount>>
)

@Serializable
data class ChunkStorageImporter(
    var importFrom: List<StorageContainer>,
    var whiteList: List<Pair<Material, ReserveAmount>>,
)