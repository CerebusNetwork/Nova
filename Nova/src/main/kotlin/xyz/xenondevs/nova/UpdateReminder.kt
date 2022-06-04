package xyz.xenondevs.nova

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitTask
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.data.ComponentUtils
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.runAsyncTaskTimer
import java.io.IOException
import java.net.URL

internal object UpdateReminder : Initializable(), Listener {
    
    private const val NOVA_RESOURCE_ID = 93648
    
    override val inMainThread = false
    override val dependsOn = setOf(NovaConfig)
    
    private var task: BukkitTask? = null
    private val needsUpdate = ArrayList<Addon?>()
    
    override fun init() {
        reload()
    }
    
    fun reload() {
        val enabled = DEFAULT_CONFIG.getBoolean("update_reminder.enabled")
        if (task == null && enabled) {
            enableReminder()
        } else if (task != null && !enabled) {
            disableReminder()
        }
    }
    
    private fun enableReminder() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        
        task = runAsyncTaskTimer(0, DEFAULT_CONFIG.getLong("update_reminder.interval")) {
            checkVersions()
            if (needsUpdate.isNotEmpty()) {
                needsUpdate.forEach { 
                    val name = it?.description?.name ?: "Nova"
                    val id = it?.description?.spigotResourceId ?: NOVA_RESOURCE_ID
                    LOGGER.warning("You're running an outdated version of $name. " +
                        "Please download the latest version at https://api.spigotmc.org/legacy/update.php?resource=$id")
                }
            }
        }
    }
    
    private fun disableReminder() {
        HandlerList.unregisterAll(this)
        
        task?.cancel()
        task = null
    }
    
    private fun checkVersions() {
        checkVersion(null)
        AddonManager.addons.values
            .filter { it.description.spigotResourceId != -1 }
            .forEach(::checkVersion)
    }
    
    private fun checkVersion(addon: Addon?) {
        if (addon in needsUpdate)
            return
        
        val id: Int
        val currentVersion: Version
        
        if (addon != null) {
            id = addon.description.spigotResourceId
            currentVersion = Version(addon.description.version)
        } else {
            id = NOVA_RESOURCE_ID
            currentVersion = NOVA.version
        }
        
        if (id == -1)
            return
        
        try {
            val newVersion = Version(URL("https://api.spigotmc.org/legacy/update.php?resource=$id").readText())
            if (newVersion > currentVersion)
                needsUpdate += addon
        } catch (e: IOException) {
            LOGGER.warning("Failed to connect to SpigotMC while trying to check for updates")
        }
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (player.hasPermission("nova.misc.updateReminder") && needsUpdate.isNotEmpty()) {
            needsUpdate.forEach { player.spigot().sendMessage(getOutdatedMessage(it)) }
        }
    }
    
    private fun getOutdatedMessage(addon: Addon?): BaseComponent {
        val addonName = coloredText(ChatColor.AQUA, addon?.description?.name ?: "Nova")
        val url = "https://spigotmc.org/resources/" + (addon?.description?.spigotResourceId ?: NOVA_RESOURCE_ID)
        return localized(ChatColor.RED,"nova.outdated_version", addonName, ComponentUtils.createLinkComponent(url))
    }
    
}