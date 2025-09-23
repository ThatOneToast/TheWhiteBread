package dev.theWhiteBread.enchantments.enchantment

import com.destroystokyo.paper.MaterialTags
import dev.theWhiteBread.Keys
import dev.theWhiteBread.enchantments.Enchantment
import dev.theWhiteBread.enchantments.hasEnchantment
import dev.theWhiteBread.miniMessage
import io.papermc.paper.registry.data.EnchantmentRegistryEntry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import kotlin.collections.forEach
import kotlin.math.abs
import kotlin.text.split


@Suppress("UnstableApiUsage")
object Veinminer {
    val enchantment = Enchantment(
        "whitebread:veinminer",
        Component.text("Veinminer"),
        anvilCost = 5,
        minimumCost = EnchantmentRegistryEntry.EnchantmentCost.of(2, 2),
        maxiumCost = EnchantmentRegistryEntry.EnchantmentCost.of(5, 3),
        maxLevel = 5,
        activeSlots = EquipmentSlotGroup.MAINHAND,
        weight = 4,
        inEnchantmentTable = true
    )

    enum class MinerStyle(val description: String) {
        Disk(
            "Area mining on the face plane: collects blocks in a disk/sphere pattern " +
                    "around the origin. Good for broad, localized clearing."
        ),
        Vein(
            "Connected-component mining: breaks contiguous same-material blocks " +
                    "(vein-like). Good for following ore veins or tree clusters."
        );

        companion object {
            fun toStringSet(): Set<String> = entries.map { it.name }.toSet()
        }

        fun toComponent(): Component =
            Component.text("${name}: ", NamedTextColor.AQUA)
                .append(Component.text(description, NamedTextColor.GRAY))
    }

    enum class MinerFilter(val description: String) {
        Ores(
            "Include ore-type blocks (ores and ore-like blocks). " +
                    "Uses MaterialTags.ORES where available, with fallbacks."
        ),
        Logs(
            "Include log and stem blocks (wood). Useful for felling trees or " +
                    "harvesting trunks."
        ),
        Blocks(
            "Include only the exact same block type as the origin. " +
                    "Use when you want strict same-type harvesting."
        );

        companion object {
            fun toStringSet(): Set<String> = entries.map { it.name }.toSet()
        }

        fun toComponent(): Component =
            Component.text("${name}: ", NamedTextColor.GOLD)
                .append(Component.text(description, NamedTextColor.GRAY))
    }

    enum class DiskSweep(val description: String) {
        CenterOut(
            "Expand outward from the origin in all directions (3D sphere-style). " +
                    "Useful for symmetric area mining around the block."
        ),
        UpOnly(
            "Only include blocks above the origin (positive Y) up to the radius. " +
                    "Useful for digging upward / clearing ceilings."
        ),
        DownOnly(
            "Only include blocks below the origin (negative Y) up to the radius. " +
                    "Useful for digging down / stripping out below the block."
        ),
        UpDown(
            "Include blocks both above and below the origin alternately out to the radius. " +
                    "Combines UpOnly and DownOnly to scan vertically both directions."
        ),
        ForwardOnly(
            "Step only in the cardinal forward direction relative to the player (XZ plane). " +
                    "Only meaningful for horizontal disks — moves in the player's forward cardinal axis."
        ),
        LeftRight(
            "Step only left and right relative to the player's forward direction (XZ plane). " +
                    "Produces a line across the face plane perpendicular to forward."
        );

        ;

        fun toComponent(): Component = miniMessage.deserialize("<gold>$name:</gold> <gray>$description")

        companion object {
            fun toStringSet(): Set<String> = entries.toSet().map { it.toString() }.toSet()
        }

    }


    private data class Off(val dx: Int, val dy: Int, val dz: Int)

    private fun sgn(d: Double): Int = if (d >= 0.0) 1 else -1

    private fun lineFromStep(length: Int, step: Off): List<Off> {
        val n = length.coerceAtLeast(0)
        val out = ArrayList<Off>(n)
        var x = 0
        var y = 0
        var z = 0
        repeat(n) {
            x += step.dx
            y += step.dy
            z += step.dz
            out += Off(x, y, z)
        }
        return out
    }

    private fun lineBothSidesFromStep(radius: Int, step: Off): List<Off> {
        val out = ArrayList<Off>(radius * 2)
        for (d in 1..radius) {
            out += Off(step.dx * d, step.dy * d, step.dz * d)
            out += Off(-step.dx * d, -step.dy * d, -step.dz * d)
        }
        return out
    }

    private fun cardinalForwardStepXZ(look: Vector?): Off {
        val l = look ?: Vector(0, 0, 1)
        val x = l.x
        val z = l.z
        if (x * x + z * z < 1e-9) return Off(0, 0, 1)
        return if (abs(x) >= abs(z)) Off(sgn(x), 0, 0)
        else Off(0, 0, sgn(z))
    }


    // Rotate a horizontal step 90° (Left/Right).
    private fun perpendicularHorizontal(step: Off): Off = Off(step.dz, 0, -step.dx)

    private fun sphereOffsets(radius: Int): List<Off> {
        val r = radius.coerceAtLeast(1)
        val r2 = r * r
        val out = ArrayList<Off>((2 * r + 1) * (2 * r + 1) * (2 * r + 1))

        for (dx in -r..r) {
            for (dy in -r..r) {
                for (dz in -r..r) {
                    if (dx == 0 && dy == 0 && dz == 0) continue
                    val d2 = dx * dx + dy * dy + dz * dz
                    if (d2 <= r2) out += Off(dx, dy, dz)
                }
            }
        }

        out.sortBy { it.dx * it.dx + it.dy * it.dy + it.dz * it.dz }
        return out
    }

    private fun diskOffsets(
        radius: Int,
        sweep: DiskSweep,
        playerDir: Vector?
    ): List<Off> {
        val r = radius.coerceAtLeast(1)

        // Vertical column modes (unchanged)
        when (sweep) {
            DiskSweep.UpOnly -> return (1..r).map { Off(0, it, 0) }
            DiskSweep.DownOnly -> return (1..r).map { Off(0, -it, 0) }
            DiskSweep.UpDown -> {
                val res = ArrayList<Off>(r * 2)
                for (d in 1..r) {
                    res += Off(0, d, 0)
                    res += Off(0, -d, 0)
                }
                return res
            }

            DiskSweep.ForwardOnly -> {
                val stepF = cardinalForwardStepXZ(playerDir)
                return lineFromStep(r, stepF)
            }

            DiskSweep.LeftRight -> {
                val stepF = cardinalForwardStepXZ(playerDir)
                val stepLR = perpendicularHorizontal(stepF)
                return lineBothSidesFromStep(r, stepLR)
            }

            DiskSweep.CenterOut -> return sphereOffsets(r)
        }

    }


    private val FACES_6 =
        arrayOf(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST
        )

    private fun neighbors6(b: Block): Sequence<Block> = sequence {
        for (f in FACES_6) yield(b.getRelative(f))
    }

    private fun neighbors26(b: Block): Sequence<Block> = sequence {
        for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
            if (dx == 0 && dy == 0 && dz == 0) continue
            yield(b.getRelative(dx, dy, dz))
        }
    }

    // Squared distance in blocks (no sqrt) from origin to block.
    private fun dist2(origin: Block, b: Block): Int {
        val dx = b.x - origin.x
        val dy = b.y - origin.y
        val dz = b.z - origin.z
        return dx * dx + dy * dy + dz * dz
    }

    fun isOre(m: Material): Boolean {
        return try {
            MaterialTags.ORES.isTagged(m)
        } catch (_: Throwable) {
            m.name.endsWith("_ORE") ||
                    m == Material.NETHER_QUARTZ_ORE ||
                    m == Material.ANCIENT_DEBRIS
        }
    }

    fun isLog(m: Material): Boolean {
        return try {
            Tag.LOGS.isTagged(m)
        } catch (_: Throwable) {
            m.name.endsWith("_LOG") || m.name.endsWith("_STEM")
        }
    }

    private fun key(b: Block): Long {
        val x = b.x.toLong()
        val y = b.y.toLong()
        val z = b.z.toLong()
        return ((x and 0x3FFFFFF) shl 38) or
                ((z and 0x3FFFFFF) shl 12) or
                (y and 0xFFFL)
    }

    // Connected-component gather with radius and limit.
    private fun gatherConnected(
        origin: Block,
        limit: Int,
        radius: Int,
        allow: (Material) -> Boolean,
        sameAsOriginOnly: Boolean,
        diag: Boolean
    ): List<Block> {
        val maxR2 = radius * radius
        val q = ArrayDeque<Block>()
        val seen = HashSet<Long>()
        val out = ArrayList<Block>(limit)

        val originMat = origin.type
        if (!allow(originMat)) return emptyList()

        q.add(origin)
        seen.add(key(origin))

        while (q.isNotEmpty() && out.size < limit) {
            val cur = q.removeFirst()

            if (radius > 0 && dist2(origin, cur) > maxR2) continue
            if (sameAsOriginOnly && cur.type != originMat) continue
            if (!sameAsOriginOnly && !allow(cur.type)) continue

            out.add(cur)

            val nbrs = if (diag) neighbors26(cur) else neighbors6(cur)
            for (n in nbrs) {
                val k = key(n)
                if (k in seen) continue
                seen.add(k)
                if (
                    (radius == 0 || dist2(origin, n) <= maxR2) &&
                    (
                            (sameAsOriginOnly && n.type == originMat) ||
                                    (!sameAsOriginOnly && allow(n.type))
                            )
                ) {
                    q.add(n)
                }
            }
        }
        return out
    }


    fun gatherVeinBlocks(
        initialBlock: Block,
        limitOfBlocks: Int = 10,
        radiusOfBlocks: Double = 5.0,
        styleAndFilter: Pair<MinerStyle, MinerFilter>,
        sweep: DiskSweep = DiskSweep.CenterOut,
        playerDir: Vector? = null
    ): List<Block> {
        val (style, filter) = styleAndFilter
        val limit = limitOfBlocks.coerceAtLeast(1)
        val radius = radiusOfBlocks.toInt().coerceAtLeast(1)

        // IMPORTANT: snapshot the origin type; block.type may be AIR now
        val originTypeSnapshot: Material = initialBlock.state.type

        return when (style) {
            MinerStyle.Disk -> {
                val offs = diskOffsets(radius, sweep, playerDir)
                val out = ArrayList<Block>(limit)
                val seen = HashSet<Long>()

                for (o in offs) {
                    if (out.size >= limit) break
                    val b = initialBlock.getRelative(o.dx, o.dy, o.dz)

                    val ok =
                        when (filter) {
                            MinerFilter.Blocks -> b.type == originTypeSnapshot
                            MinerFilter.Logs -> isLog(b.type)
                            MinerFilter.Ores -> isOre(b.type)
                        }

                    if (!ok) continue
                    if (!seen.add(key(b))) continue
                    out += b
                }
                out
            }

            MinerStyle.Vein -> {
                when (filter) {
                    MinerFilter.Ores ->
                        gatherConnected(
                            origin = initialBlock,
                            limit = limit,
                            radius = radius,
                            allow = ::isOre,
                            sameAsOriginOnly = true,
                            diag = false
                        )

                    MinerFilter.Logs ->
                        gatherConnected(
                            origin = initialBlock,
                            limit = limit,
                            radius = radius,
                            allow = ::isLog,
                            sameAsOriginOnly = true,
                            diag = true
                        )

                    MinerFilter.Blocks ->
                        // sameAsOriginOnly=true uses origin.type internally — pass a snapshot
                        // by temp-swapping the block type if needed, or just reuse Disk path.
                        gatherConnected(
                            origin = initialBlock,
                            limit = limit,
                            radius = radius,
                            allow = { it == originTypeSnapshot },
                            sameAsOriginOnly = false,
                            diag = false
                        )
                }
            }
        }
    }

    fun veinMine(
        invoker: Player,
        initialBlockBroke: Block,
        levelOfPower: Int
    ): List<Block>? {
        if (invoker.foodLevel < 3) {
            invoker.sendMessage(miniMessage.deserialize("<gray>You are to hungry to use veinminer."))
            return null
        }

        val style = invoker.persistentDataContainer.get(Keys.veinMineStyle, PersistentDataType.STRING)
            ?: "Vein:Blocks:CenterOut"
        val styleFilter = style.split(":")

        val blocksBroken = gatherVeinBlocks(
            initialBlockBroke,
            10 * levelOfPower,
            10.0 + levelOfPower,
            Pair(
                MinerStyle.valueOf(styleFilter[0]),
                MinerFilter.valueOf(styleFilter[1])
            ),
            DiskSweep.valueOf(styleFilter[2]),
            invoker.location.direction
        )


        val tool = invoker.inventory.itemInMainHand
        if (hasEnchantment(tool, "whitebread:autosmelt").first) {
            blocksBroken.forEach { block ->
                run {
                    val autoSmeltDrops = AutoSmelt.getAutoSmeltedDrops(block, tool)
                    if (autoSmeltDrops.isEmpty()) {
                        block.breakNaturally(tool, true, true, false)
                    } else {
                        autoSmeltDrops.forEach {
                            block.world.dropItemNaturally(block.location, it)
                        }
                        AutoSmelt.spawnExperienceOrb(block.location, AutoSmelt.vanillaXpForBlock(block, tool))
                        block.type = Material.AIR
                    }

                }
            }
        } else {
            blocksBroken.forEach { block ->
                run {
                    block.breakNaturally(tool, true, true, false)
                }
            }
        }

        if (blocksBroken.size >= 15) {
            invoker.sendMessage(
                miniMessage.deserialize(
                    "Destroyed <red><bold>${blocksBroken.size}<bold/></red> blocks!"
                )
            )
        }

        return blocksBroken

    }
}