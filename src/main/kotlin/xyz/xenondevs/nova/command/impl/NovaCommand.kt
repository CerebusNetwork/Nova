package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.context.CommandContext
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.command.PlayerCommand
import xyz.xenondevs.nova.command.executesCatching
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.debug.NetworkDebugger
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.ui.gui.CreativeGUI
import xyz.xenondevs.nova.util.capitalize
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.hasNovaData

class NovaCommand(name: String, permission: String) : PlayerCommand(name, permission) {
    
    init {
        builder = builder
            .then(literal("give")
                .apply {
                    NovaMaterial.values().forEach { material ->
                        then(literal(material.name)
                            .executesCatching { context -> handleGive(material, context) }
                        )
                    }
                })
            .then(literal("debug")
                .then(literal("listNearby")
                    .executesCatching { listNearby(it) })
                .then(literal("getNearestData")
                    .executesCatching { getNearestData(it) })
                .then(literal("energyNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ENERGY, it) })
                .then(literal("itemNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ITEMS, it) }))
            .then(literal("inventory")
                .executesCatching { openCreativeInventory(it) })
    }
    
    private fun handleGive(material: NovaMaterial, context: CommandContext<Any>) {
        val player = context.player
        player.inventory.addItem(material.createItemStack())
        val itemName = material.itemName.ifBlank { material.name }
        player.sendMessage("§7The item §b$itemName§7 has been added to your inventory.")
    }
    
    private fun listNearby(context: CommandContext<Any>) {
        val player = context.player
        val chunk = player.location.chunk
        val armorStands = chunk.entities.filterIsInstance<ArmorStand>()
        val tileEntityArmorStands = armorStands.filter { it.persistentDataContainer.hasNovaData() }
        
        player.sendMessage("§7Out of the §b${armorStands.count()}§7 ArmorStands in your chunk, §b${tileEntityArmorStands.count()}§7 are part of a TileEntity.")
    }
    
    private fun getNearestData(context: CommandContext<Any>) {
        val player = context.player
        val chunks = player.location.chunk.getSurroundingChunks(1, true)
        val armorStands = chunks
            .flatMap { it.entities.toList() }
            .filterIsInstance<ArmorStand>()
            .filter { it.persistentDataContainer.hasNovaData() }
        val armorStand = armorStands.minByOrNull { it.location.distance(player.location) }
        if (armorStand != null) player.chat("/data get entity ${armorStand.uniqueId}")
        else player.sendMessage("§cCould not find a TileEntity nearby.")
    }
    
    private fun toggleNetworkDebugging(type: NetworkType, context: CommandContext<Any>) {
        val player = context.player
        NetworkDebugger.toggleDebugger(type, player)
        player.sendMessage("§7Toggled debug-view for §b${type.name.lowercase().capitalize()}-Networks")
    }
    
    private fun openCreativeInventory(context: CommandContext<Any>) {
        CreativeGUI.getWindow(context.player).show()
    }
    
}

