package dev.theWhiteBread.items.item

import dev.theWhiteBread.*
import dev.theWhiteBread.enchantments.enchantment.Veinminer
import dev.theWhiteBread.items.BreadItem
import dev.theWhiteBread.serializables.SerializableLocation
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.craftbukkit.metadata.BlockMetadataStore
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.metadata.MetadataValue

enum class BuilderWandMode {
    PLACE,
    BREAK
}

object BuilderWand : BreadItem(
    ItemStack(Material.STICK).apply {
        val meta = itemMeta
        meta.displayName("<gold>Builders Stick".toComponent())
        meta.setEnchantmentGlintOverride(true)
        meta.lore(listOf(
            "<gray>/<gold>whitebread</gold> <white>builderswand</white> - to customize".toComponent(),
            "<gray>Breaking mode can only destroy blocks placed by a <gold>builders tool</gold>".toComponent(),
            "<gray>Breaking mode <red>does <bold>NOT</bold> return blocks back to you</red>.".toComponent()
        ))
        itemMeta = meta
    },
    "builders_wand"
) {
    init {
        computeData("mode", BuilderWandMode.PLACE)
        computeData<Int>("size", 8)
    }

    override fun equip(player: Player) {
        val mode = getData<BuilderWandMode>("mode")!!
        val size = getData<Int>("size")!!

        player.message("<gray> --- Builders Wand ---")
        player.message("<gray><gold>Operating Mode</gold>: $mode")
        player.message("<gray><gold>Operating Size</gold>: ${size + 1}")

    }

    override fun deEquip(player: Player) {
    }

    override fun onMovement(event: PlayerMoveEvent) {
        fun firstNonAirTarget(range: Int = 6): Block? {
            val b = event.player.getTargetBlockExact(range, FluidCollisionMode.NEVER) ?: return null
            return if (!b.type.isAir) b else null
        }

        when (getData<BuilderWandMode>("mode")!!) {
            BuilderWandMode.PLACE -> {
                deleteData("Ynextblock")
                val blockLookingAt = firstNonAirTarget(5) ?: return
                var size = getData<Int>("size")!!

                var block = nextAirBlockOnSameY(event.player, blockLookingAt.location, 30, false)?: return

                val placeableLocations = mutableSetOf<Block>()
                placeableLocations.add(block)

                for (i in 0 until size) {
                    val adjcBlock = nextAirBlockOnSameY(
                        event.player,
                        block.location,
                        allowDiagonals = false,
                    )

                    if (adjcBlock == null) {
                        size++
                        continue
                    }

                    block = adjcBlock
                    placeableLocations.add(adjcBlock)
                }

                placeableLocations.forEach { block -> run {
                    outlineBlock(block, event.player)
                }}

                setData("Ynextblock", Pair(blockLookingAt.type, block.location.toSerLocation()))
                setData("Ynextblocks", placeableLocations.map { it.location.toSerLocation() }.toList())
            }
            BuilderWandMode.BREAK -> {
                deleteData("Ynextblock")
                val blockLookingAt = firstNonAirTarget(5) ?: return
                val blocks = mutableSetOf<Block>()
                fun addBlockToSet(block: Block): Block {
                    blocks.add(block)
                    return block
                }
                var block: Block = blockLookingAt
                var size = getData<Int>("size")!!
                for (i in 0 until size) {
                    val adjcBlock = adjacentBlockSameY(event.player, block.location,
                        allowDiagonals = false,
                        returnOnAir = true
                    )
                    if (adjcBlock == null) {
                        size++
                        continue
                    } else if (Veinminer.isOre(adjcBlock.type)
                        || adjcBlock.type == Material.BEDROCK
                        || adjcBlock.type == Material.OBSIDIAN
                        || adjcBlock.type == Material.END_PORTAL_FRAME
                        || adjcBlock.type == Material.END_PORTAL
                        || adjcBlock.type == Material.NETHER_PORTAL
                    ) {
                        size++
                        continue
                    }
                    block = addBlockToSet(adjcBlock)
                }
                outlineBlock(blockLookingAt, event.player)
                blocks.forEach {
                    outlineBlock(it, event.player)
                }
                setData("Ynextblock", Pair(blockLookingAt.type, blockLookingAt.location.toSerLocation()))
                setData("Ynextblocks", blocks.map { it.location.toSerLocation() }.toList())

            }
        }

    }

    override fun onRightClickBlock(event: PlayerInteractEvent) {
        if (!isMainHand(event.player)) return

        when (getData<BuilderWandMode>("mode")!!) {
            BuilderWandMode.PLACE -> {
                val playerInv = event.player.inventory

                val material = getData<Pair<Material, SerializableLocation>>("Ynextblock")!!.first
                val placeableLocations = getData<List<SerializableLocation>>("Ynextblocks")!!


                val canProceed = playerInv.removeAmount(material, amount = placeableLocations.size)
                if (!canProceed) {
                    event.player.message("<gray>You do not have enough <gold>${material.toString().lowercase()}</gold> to place.")
                    return
                }

                placeableLocations.forEach { block -> run {
                    val block = block.toBlock()
                    block.setType(material, true)
                    block.state.setMetadata("builderwand_placed", FixedMetadataValue(TheWhiteBread.instance, true))
                    block.state.update(true)
                }}

                deleteData("Ynextblock")
                deleteData("Ynextblocks")

            }
            BuilderWandMode.BREAK -> {
                val location = getData<Pair<Material, SerializableLocation>>("Ynextblock")?: return
                val sequenceLocations = getData<List<SerializableLocation>>("Ynextblocks")?.toMutableList() ?: return
                sequenceLocations.add(location.second)
                sequenceLocations.map { it.toBlock() }.toList().forEach {
                    if (it.state.hasMetadata("builderwand_placed")) {
                        it.type = Material.AIR
                        it.state.update(true, true)
                    } else {
                        event.player.message("<gray>This block cannot be broken, because it wasn't placed by a <gold>builder's tool</gold>.")
                    }

                }
                deleteData("Ynextblocks", "Ynextblock")

            }
        }

    }

}