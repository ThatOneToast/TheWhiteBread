package dev.theWhiteBread.storage_system.controller

import dev.theWhiteBread.serializables.SerializableLocation
import kotlinx.serialization.Serializable
import org.bukkit.block.data.type.Chest


typealias StorageControllerData = StorageControllerDataVersions.Version3

object StorageControllerDataVersions {

    @Serializable
    data class Version1(
        val owner: String,
        val controllerBlock: SerializableLocation,
        val containers: Map<Chest.Type, List<SerializableLocation>>
    )

    @Serializable
    data class Version2(
        val owner: String,
        val controllerBlock: SerializableLocation,
        var containers: MutableMap<Chest.Type, MutableList<SerializableLocation>>,
        val members: StorageControllerMembers,
        var containersNormalAccessRevoked: Boolean = false,
        val linkedControllers: MutableSet<SerializableLocation> = mutableSetOf()
    ) {
        init {
            containers[Chest.Type.LEFT] = mutableListOf()
            containers[Chest.Type.RIGHT] = mutableListOf()
            containers[Chest.Type.SINGLE] = mutableListOf()

        }
    }

    @Serializable
    data class Version3(
        val owner: String,
        val controllerBlock: SerializableLocation,
        var containers: MutableMap<Chest.Type, MutableList<SerializableLocation>>,
        val members: StorageControllerMembers,
        var containersNormalAccessRevoked: Boolean = false,
        val linkedControllers: MutableSet<SerializableLocation> = mutableSetOf(),
        val upgrades: ControllerUpgrades = ControllerUpgrades()
    ) {
        init {
            containers[Chest.Type.LEFT] = mutableListOf()
            containers[Chest.Type.RIGHT] = mutableListOf()
            containers[Chest.Type.SINGLE] = mutableListOf()

        }
    }
}

@Serializable
data class ControllerUpgrades(
    val upgrades: MutableSet<ControllerUpgrade> = mutableSetOf()
) {
    fun getByName(theName: String): ControllerUpgrade? = upgrades.find { it.name == theName }

}

@Serializable
data class ControllerUpgrade(
    val name: String,
    val descriptionPerLevel: Map<Int, String>,
    val level: Int,
)



@Serializable
enum class StorageControllerMemberPermission(val weight: Int, val description: String) {
    VIEWER(0, "People can view your items, that is it."),
    USER(1, "People can view and interact with your items."),
    MANAGER(2,"People can view, interact, and change settings with your controller/containers.")

    ;

    fun isAtLeast(other: StorageControllerMemberPermission): Boolean = this.weight >= other.weight
}

@Serializable
data class StorageControllerMembers(
    var publicAccess: Pair<Boolean, StorageControllerMemberPermission?> = Pair(false, null),
    val members: MutableMap<String, StorageControllerMemberPermission> = mutableMapOf(),
    var maxMemberCount: Int = 5
)