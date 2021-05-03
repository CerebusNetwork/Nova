package xyz.xenondevs.nova.tileentity.impl

import de.studiocode.invui.gui.builder.GUIBuilder
import de.studiocode.invui.gui.builder.GUIType
import de.studiocode.invui.item.Item
import de.studiocode.invui.window.impl.single.SimpleWindow
import org.bukkit.Chunk
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.ChunkLoadManager
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.SideConfigGUI
import xyz.xenondevs.nova.ui.item.AddNumberItem
import xyz.xenondevs.nova.ui.item.DisplayNumberItem
import xyz.xenondevs.nova.ui.item.RemoveNumberItem
import xyz.xenondevs.nova.util.getSurroundingChunks
import java.util.*

private val MAX_ENERGY = NovaConfig.getInt("chunk_loader.capacity")!!
private val ENERGY_PER_CHUNK = NovaConfig.getInt("chunk_loader.energy_per_chunk")!!
private val MAX_RANGE = NovaConfig.getInt("chunk_loader.max_range")!!

class ChunkLoader(
    ownerUUID: UUID?,
    material: NovaMaterial,
    armorStand: ArmorStand
) : EnergyTileEntity(ownerUUID, material, armorStand) {
    
    override val defaultEnergyConfig by lazy { createEnergySideConfig(EnergyConnectionType.CONSUME) }
    override val requestedEnergy: Int
        get() = MAX_ENERGY - energy
    
    private var range = retrieveData("range") { 0 }
    private var chunks: List<Chunk> = retrieveData("chunks") { chunk.getSurroundingChunks(range, true) }
    private var active = false
    
    private val gui by lazy { ChunkLoaderGUI() }
    
    override fun saveData() {
        super.saveData()
        storeData("chunks", chunks)
        storeData("range", range)
    }
    
    override fun handleTick() {
        if (energy >= (ENERGY_PER_CHUNK * chunks.size)) {
            energy -= (ENERGY_PER_CHUNK * chunks.size)
            if (!active) {
                setChunksForceLoaded(true)
                active = true
            }
        } else if (active) {
            setChunksForceLoaded(false)
            active = false
        }
        
        if (hasEnergyChanged) {
            gui.energyBar.update()
            hasEnergyChanged = false
        }
    }
    
    private fun setChunksForceLoaded(state: Boolean) {
        chunks.forEach {
            if (state) ChunkLoadManager.submitChunkLoadRequest(it, uuid)
            else ChunkLoadManager.revokeChunkLoadRequest(it, uuid)
        }
    }
    
    private fun setRange(range: Int) {
        this.range = range
        setChunksForceLoaded(false)
        chunks = chunk.getSurroundingChunks(range, true)
        active = false
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        if (!unload) setChunksForceLoaded(false)
    }
    
    override fun handleRightClick(event: PlayerInteractEvent) {
        event.isCancelled = true
        gui.openWindow(event.player)
    }
    
    private inner class ChunkLoaderGUI {
        
        private val sideConfigGUI = SideConfigGUI(
            this@ChunkLoader,
            listOf(EnergyConnectionType.NONE, EnergyConnectionType.CONSUME),
            null
        ) { openWindow(it) }
        
        private val rangeItems = ArrayList<Item>()
        
        private val gui = GUIBuilder(GUIType.NORMAL, 9, 3)
            .setStructure("" +
                "# 1 - - - - - 2 ." +
                "# | # m n p # | ." +
                "# 3 - - - - - 4 ."
            )
            .addIngredient('s', OpenSideConfigItem(sideConfigGUI))
            .addIngredient('p', AddNumberItem(0..MAX_RANGE, { range }, ::setRange).also(rangeItems::add))
            .addIngredient('m', RemoveNumberItem(0..MAX_RANGE, { range }, ::setRange).also(rangeItems::add))
            .addIngredient('n', DisplayNumberItem { range + 1 }.also(rangeItems::add))
            .build()
        
        val energyBar = EnergyBar(gui, 8, 0, 3) { Triple(energy, MAX_ENERGY, ENERGY_PER_CHUNK * chunks.size) }
        
        private fun setRange(range: Int) {
            this@ChunkLoader.setRange(range)
            rangeItems.forEach(Item::notifyWindows)
            energyBar.update()
        }
        
        fun openWindow(player: Player) {
            SimpleWindow(player, "Chunk Loader", gui).show()
        }
        
    }
    
}