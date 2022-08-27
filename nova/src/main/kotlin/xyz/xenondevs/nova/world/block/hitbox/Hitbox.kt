package xyz.xenondevs.nova.world.block.hitbox

import org.bukkit.Location
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.world.chunkPos

class Hitbox(
    val from: Location,
    val to: Location,
    val checkQualify: (PlayerInteractEvent) -> Boolean = { true },
    val handleHit: (PlayerInteractEvent) -> Unit
) {
    
    val chunk = from.chunkPos
    
    init {
        HitboxManager.addHitbox(this)
    }
    
    fun isInHitbox(location: Location): Boolean {
        return location.x in from.x..to.x
            && location.y in from.y..to.y
            && location.z in from.z..to.z
    }
    
    fun remove() {
        HitboxManager.removeHitbox(this)
    }
    
}