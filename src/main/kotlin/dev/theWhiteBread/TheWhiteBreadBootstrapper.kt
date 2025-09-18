package dev.theWhiteBread

import dev.theWhiteBread.enchantments.enchantment.AutoSmelt
import dev.theWhiteBread.enchantments.enchantment.EXPBoost
import dev.theWhiteBread.enchantments.enchantment.Veinminer
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.event.RegistryEvents
import io.papermc.paper.registry.keys.EnchantmentKeys
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys

import net.kyori.adventure.key.Key
import dev.theWhiteBread.enchantments.Enchantment



@Suppress("UnstableApiUsage")
class TheWhiteBreadBootstrapper : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        registerEnchantment(context, Veinminer.enchantment)
        registerEnchantment(context, AutoSmelt.enchantment)
        registerEnchantment(context, EXPBoost.enchantment)
    }

    private fun registerEnchantment(ctx: BootstrapContext, enchant: Enchantment) {
        ctx.lifecycleManager.registerEventHandler(
            RegistryEvents.ENCHANTMENT.compose().newHandler(LifecycleEventHandler { event ->
                event.registry().register(EnchantmentKeys.create(Key.key(enchant.key))) { b ->
                    b.description(enchant.name)
                        .supportedItems(event.getOrCreateTag(enchant.supportedItems))
                        .anvilCost(enchant.anvilCost)
                        .maxLevel(enchant.maxLevel)
                        .weight(enchant.weight)
                        .minimumCost(enchant.minimumCost)
                        .maximumCost(enchant.maxiumCost)
                        .activeSlots(enchant.activeSlots)
                }
            })
        )

        ctx.lifecycleManager.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT)) { event ->
            val register = event.registrar()
            register.addToTag(
                EnchantmentTagKeys.IN_ENCHANTING_TABLE,
                sortedSetOf(EnchantmentKeys.create(Key.key(enchant.key)))
            )
        }


    }


}