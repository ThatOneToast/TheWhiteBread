package dev.theWhiteBread.storage_system.container

import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.storage_system.ControllerRegistry
import dev.theWhiteBread.storage_system.controller.StorageController
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.CompletableFuture
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

typealias InventoryMigrationFn = () -> Unit

object ItemTransport {

    private val inventoryOperations = mutableListOf<InventoryMigrationFn>()

    fun enqueueMigration(fn: InventoryMigrationFn) {
        synchronized(inventoryOperations) {
            inventoryOperations.add(fn)
        }
    }


    /**
     * Create an async plan and return a Future that completes with the "apply" function.
     *
     * IMPORTANT: The returned function must be executed on the main server thread
     * (e.g. Bukkit.getScheduler().runTask(plugin) { applyFn() }).
     *
     * ```kotlin
     * val future = BatchContainerModification.InventoryMigration(recvCont, expCont, controller)
     * future.thenAccept { applyFn ->
     *     // run on main thread with your plugin instance:
     *     Bukkit.getScheduler().runTask(plugin) {
     *         applyFn()
     *     }
     * }
     * ```
     */
    fun inventoryMigration(
        receivingContainer: StorageContainer,
        exportingContainer: StorageContainer,
        controller: StorageController
    ): CompletableFuture<InventoryMigrationFn> {

        fun getInventory(container: StorageContainer): Inventory {
            val block = container.getBlock()
            val state = block.state as Container
            return state.inventory
        }

        val receivingWhitelist = receivingContainer.customization.importer.whiteList
        val exportingWhitelist = exportingContainer.customization.exporter.whiteList

        data class SlotSnapshot(val material: Material?, val amount: Int)

        val receivingInvSnapshot: List<SlotSnapshot>
        val exportingInvSnapshot: List<SlotSnapshot>

        run {
            val receivingInv = getInventory(receivingContainer)
            val exportingInv = getInventory(exportingContainer)

            receivingInvSnapshot = receivingInv.contents.map { it?.let { s -> SlotSnapshot(s.type, s.amount) } ?: SlotSnapshot(null, 0) }
            exportingInvSnapshot = exportingInv.contents.map { it?.let { s -> SlotSnapshot(s.type, s.amount) } ?: SlotSnapshot(null, 0) }
        }

        val planFuture: CompletableFuture<Map<Material, Int>> = CompletableFuture.supplyAsync {
            fun countInSnapshot(snapshot: List<SlotSnapshot>, material: Material): Int = snapshot.fold(0) { acc, slot -> if (slot.material == material) acc + slot.amount else acc }


            val exportReserveByMat: Map<Material, Int> =
                exportingWhitelist.associate { (mat, amt) -> mat to amt }

            val transferByMaterial = mutableMapOf<Material, Int>()

            for ((material, receiveReserve) in receivingWhitelist) {
                if (receiveReserve <= 0) continue

                val curReceiving = countInSnapshot(receivingInvSnapshot, material)
                val need = (receiveReserve - curReceiving).coerceAtLeast(0)
                if (need <= 0) continue

                val exportReserve = exportReserveByMat[material] ?: continue
                val curExporting = countInSnapshot(exportingInvSnapshot, material)
                val canExport = (curExporting - exportReserve).coerceAtLeast(0)

                val toTransfer = minOf(need, canExport)
                if (toTransfer > 0) transferByMaterial[material] = toTransfer
            }

            transferByMaterial.toMap()
        }

        return planFuture.thenApply { plan ->
            val applyFn: InventoryMigrationFn = {
                val receivingInv = getInventory(receivingContainer)
                val exportingInv = getInventory(exportingContainer)

                fun dropAt(location: Location, stacks: Collection<ItemStack>) {
                    val world = location.world ?: return
                    for (s in stacks) world.dropItemNaturally(location, s)
                }

                for ((material, amount) in plan) {
                    if (amount <= 0) continue
                    var remaining = amount

                    for (slot in 0 until exportingInv.size) {
                        if (remaining <= 0) break
                        val slotItem = exportingInv.getItem(slot) ?: continue
                        if (slotItem.type != material) continue

                        val take = minOf(remaining, slotItem.amount)
                        val toMove = slotItem.clone().apply { this.amount = take }

                        val leftoverMap = receivingInv.addItem(toMove)
                        val leftover = leftoverMap.values.firstOrNull()
                        val moved = if (leftover == null) take else (take - leftover.amount)
                        if (moved <= 0) continue

                        if (moved >= slotItem.amount) {
                            exportingInv.clear(slot)
                        } else {
                            slotItem.amount -= moved
                            exportingInv.setItem(slot, slotItem)
                        }

                        remaining -= moved

                        if (leftover != null && leftover.amount > 0) {
                            val cur = exportingInv.getItem(slot)
                            if (cur == null) {
                                exportingInv.setItem(slot, leftover)
                            } else {
                                val ret = exportingInv.addItem(leftover)
                                if (ret.isNotEmpty()) {
                                    dropAt(exportingContainer.getBlock().location, ret.values)
                                }
                            }
                        }
                    }
                }

                receivingContainer.getBlock().state.update(true, false)
                exportingContainer.getBlock().state.update(true, false)
                controller.updateViewState()
            }

            applyFn
        }
    }

    fun runInventoryOperations() {
        val toRun: List<InventoryMigrationFn>
        synchronized(inventoryOperations) {
            toRun = inventoryOperations.toList()
            inventoryOperations.clear()
        }
        if (toRun.isEmpty()) return

        object : BukkitRunnable() {
            override fun run() {
                for (fn in toRun) {
                    try {
                        fn()
                    } catch (t: Throwable) {
                        TheWhiteBread.pluginLogger.error("CSM - Inventory migration operation failed", t)
                    }
                }
                TheWhiteBread.pluginLogger.info("CSM - Inventory migration finished.")
                synchronized(inventoryOperations){
                    inventoryOperations.clear()
                }
            }
        }.runTask(TheWhiteBread.instance)

    }

    @Volatile
    var isMigrationPrepped = false

    val storageManagerPreparation = object : BukkitRunnable() {
        override fun run() {
            TheWhiteBread.pluginLogger.warning("CSM - Inventory migration preparation in process")
            val storageControllers = ControllerRegistry.getControllers()

            for (controller in storageControllers) {
                val data = controller.getContainerData()
                val exporters = data.filter { it.customization.exporter.exporting }
                val importers = data.filter { it.customization.importer.importFrom.isNotEmpty() }
                if (exporters.isEmpty() || importers.isEmpty()) continue

                // 1) Snapshot on main thread
                data class Snapshot(
                    val container: StorageContainer,
                    val counts: Map<Material, Int>,
                    val expWhite: Map<Material, Int>,
                    val impWhite: Map<Material, Int>
                )

                val snapshots = data.map { c ->
                    val inv = (c.getBlock().state as Container).inventory
                    val counts = mutableMapOf<Material, Int>()
                    for (s in inv.contents) {
                        if (s == null) continue
                        counts[s.type] = counts.getOrDefault(s.type, 0) + s.amount
                    }
                    Snapshot(
                        c,
                        counts,
                        c.customization.exporter.whiteList.associate { (m, a) -> m to a },
                        c.customization.importer.whiteList.associate { (m, a) -> m to a }
                    )
                }

                // helper data classes for planning
                data class Provider(val container: StorageContainer, val available: Int)
                data class Consumer(val container: StorageContainer, val need: Int)
                data class Transfer(
                    val provider: StorageContainer,
                    val consumer: StorageContainer,
                    val material: Material,
                    val amount: Int
                )

                // 2) Heavy planning off-thread using coroutines
                TheWhiteBread.threadingScopes.pluginScope.launch {
                    val transfers = withContext(TheWhiteBread.threadingScopes.migrationDispatcher) {
                        val materials = mutableSetOf<Material>()
                        snapshots.forEach {
                            materials.addAll(it.expWhite.keys)
                            materials.addAll(it.impWhite.keys)
                        }

                        val out = mutableListOf<Transfer>()

                        for (mat in materials) {
                            val providers = mutableListOf<Provider>()
                            val consumers = mutableListOf<Consumer>()

                            for (s in snapshots) {
                                val reserve = s.expWhite[mat] ?: 0
                                val cur = s.counts[mat] ?: 0
                                val avail = (cur - reserve).coerceAtLeast(0)
                                if (avail > 0) providers.add(Provider(s.container, avail))
                            }
                            for (s in snapshots) {
                                val reserve = s.impWhite[mat] ?: 0
                                val cur = s.counts[mat] ?: 0
                                val need = (reserve - cur).coerceAtLeast(0)
                                if (need > 0) consumers.add(Consumer(s.container, need))
                            }

                            if (providers.isEmpty() || consumers.isEmpty()) continue

                            val totalAvail = providers.sumOf { it.available }
                            val totalNeed = consumers.sumOf { it.need }
                            val totalTransfer = minOf(totalAvail, totalNeed)
                            if (totalTransfer <= 0) continue

                            val consumerAlloc = mutableMapOf<StorageContainer, Int>()
                            var assigned = 0
                            for (consumer in consumers) {
                                val share = (consumer.need.toDouble() / totalNeed * totalTransfer).toInt()
                                val allocation = share.coerceAtMost(consumer.need)
                                consumerAlloc[consumer.container] = allocation
                                assigned += allocation
                            }

                            var remainder = totalTransfer - assigned
                            if (remainder > 0) {
                                for (consumer in consumers) {
                                    if (remainder <= 0) break
                                    val already = consumerAlloc[consumer.container] ?: 0
                                    if (already < consumer.need) {
                                        consumerAlloc[consumer.container] = already + 1
                                        remainder--
                                    }
                                }
                            }

                            val providerRemaining = providers.associate { it.container to it.available }.toMutableMap()

                            for ((consumerContainer, want) in consumerAlloc) {
                                var needLeft = want
                                if (needLeft <= 0) continue

                                for (provider in providers) {
                                    if (needLeft <= 0) break
                                    val cap = providerRemaining[provider.container] ?: 0
                                    if (cap <= 0) continue
                                    val take = minOf(cap, needLeft)
                                    providerRemaining[provider.container] = cap - take
                                    out.add(Transfer(provider.container, consumerContainer, mat, take))
                                    needLeft -= take
                                }
                            }
                        }

                        out
                    }

                    if (transfers.isEmpty()) return@launch

                    val grouped = transfers.groupBy { it.provider to it.consumer }.mapValues { (_, list) ->
                        list.groupBy { it.material }.mapValues { (_, l) -> l.sumOf { it.amount } }
                    }

                    withContext(TheWhiteBread.threadingScopes.bukkitDispatcher) {
                        for ((pair, plan) in grouped) {
                            val (provider, consumer) = pair
                            inventoryMigration(
                                receivingContainer = consumer,
                                exportingContainer = provider,
                                controller = controller
                            ).thenAccept { applyFn ->
                                enqueueMigration(applyFn)
                            }
                        }
                    }
                    isMigrationPrepped = true
                    TheWhiteBread.pluginLogger.warning("CSM - Inventory migration preparation ended.")
                }
            } // end controllers loop
        }
    }

    val storageManagerInvExecution = object : BukkitRunnable() {
        override fun run() {
            if (isMigrationPrepped) {
                TheWhiteBread.pluginLogger.info("CSM - Inventory migration in process")
                runInventoryOperations()
                isMigrationPrepped = false
            }
        }

    }

}