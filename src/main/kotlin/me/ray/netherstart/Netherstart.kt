package me.ray.netherstart

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.text.Component
import io.papermc.paper.ban.BanListType
import org.bukkit.Bukkit
import java.time.Instant

class Netherstart : JavaPlugin(), Listener {

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)
        logger.info("Netherstart has been enabled!")
    }

    override fun onDisable() {
        logger.info("Netherstart has been disabled!")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        // Always teleport to Nether on join (for first-timers and unbanned players)
        if (!player.hasPlayedBefore()) {
            val netherWorld = server.worlds.firstOrNull { it.environment == World.Environment.NETHER }

            if (netherWorld != null) {
                // Get a safe spawn location in the nether
                val spawnLocation = getSafeNetherSpawn(netherWorld)
                player.teleport(spawnLocation)
                player.sendMessage("§cWelcome to the Nether!")
                logger.info("${player.name} has been teleported to the Nether")
            } else {
                logger.warning("Nether world not found! Make sure allow-nether is true in server.properties")
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player

        // Get the correct ban‐list for profiles
        val banList = Bukkit.getBanList(BanListType.PROFILE)

        // Add a ban: target = profile, reason, expires = null (permanent), source
        banList.addBan(
            player.playerProfile,       // T target
            "You died!",                // reason
            null as Instant?,           // expires (null = permanent) SPECIFY as INSTANT PLEASE GOD
            "Server"                    // source
        )

        player.kick(Component.text("You died and were banned!"))

        logger.info("${player.name} has been banned for dying")
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val netherWorld = server.worlds.firstOrNull { it.environment == World.Environment.NETHER }

        if (netherWorld != null) {
            val spawnLocation = getSafeNetherSpawn(netherWorld)
            event.respawnLocation = spawnLocation
            logger.info("${player.name} respawned in the Nether")
        } else {
            logger.warning("Nether world not found for respawn!")
        }
    }

    private fun getSafeNetherSpawn(world: World): Location {
        // Use the world's spawn location as a starting point
        val spawnPoint = world.spawnLocation

        // Find a safe location (not in lava, has floor and air above)
        var safeLocation = spawnPoint.clone()
        var attempts = 0
        val maxAttempts = 10

        while (attempts < maxAttempts) {
            val checkLoc = safeLocation.clone()
            val blockBelow = checkLoc.subtract(0.0, 1.0, 0.0).block
            val blockAt = safeLocation.block
            val blockAbove = safeLocation.clone().add(0.0, 1.0, 0.0).block

            // Check if location is safe (solid block below, air at player level and above)
            if (blockBelow.type.isSolid &&
                !blockBelow.isLiquid &&
                blockAt.type.isAir &&
                blockAbove.type.isAir) {
                break
            }

            // Try moving up if not safe
            safeLocation.add(0.0, 1.0, 0.0)
            attempts++
        }

        return safeLocation
    }
}