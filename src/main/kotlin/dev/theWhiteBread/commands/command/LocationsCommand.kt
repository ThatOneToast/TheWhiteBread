package dev.theWhiteBread.commands.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.zaxxer.hikari.HikariDataSource
import dev.theWhiteBread.TheWhiteBread
import dev.theWhiteBread.commands.CommandBuilder
import dev.theWhiteBread.message
import dev.theWhiteBread.miniMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.minecraft.commands.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

object LocationsCommand {
    private val suggestionExecutor = Executors.newCachedThreadPool()

    val homeSuggestionProvider = SuggestionProvider<CommandSourceStack> { ctx, builder ->
        // if the source isn't a player, return no suggestions
        val entity = ctx.source.entity
        val bukkitPlayer = (entity?.bukkitEntity as? Player) ?: return@SuggestionProvider builder.buildFuture()

        CompletableFuture.supplyAsync({
            // Call your suspend DAO from this background thread
            val locations: Set<Pair<String, Location>> = try {
                runBlocking { HomesSql.getHomes(TheWhiteBread.database, bukkitPlayer.uniqueId) }
            } catch (e: Exception) {
                emptyList()
            } as Set<Pair<String, Location>>

            // Use a fresh SuggestionsBuilder using the same input/start to be thread-safe
            val sb = SuggestionsBuilder(builder.input, builder.start)
            // Optionally filter by prefix (builder.input.substring(builder.start) gives partial token)
            val partial = builder.input.substring(builder.start).lowercase()

            // Add suggestions (limit if needed)
            var added = 0
            for ((name, _) in locations) {
                if (partial.isEmpty() || name.lowercase().startsWith(partial)) {
                    sb.suggest(name)
                    added++
                    if (added >= 50) break // limit to 50 suggestions
                }
            }

            sb.build()
        }, suggestionExecutor)
    }

    fun storeLocation(ctx: CommandContext<CommandSourceStack>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        val player = ctx.source.player ?: return 0
        val bukkitPlayer = Bukkit.getPlayer(player.uuid) ?: return 0
        TheWhiteBread.threadingScopes.pluginScope.launch {
            HomesSql.saveHomeMysql(TheWhiteBread.database, HomesSql.Home(player.uuid, name, bukkitPlayer.location))
        }
        bukkitPlayer.sendMessage(miniMessage.deserialize("<green>Stored Location: <bold><white>$name</white></bold> Saved!"))
        return 1
    }

    fun deleteLocation(ctx: CommandContext<CommandSourceStack>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        val player = ctx.source.player ?: return 0
        val bukkitPlayer = Bukkit.getPlayer(player.uuid) ?: return 0
        TheWhiteBread.threadingScopes.pluginScope.launch {
            HomesSql.deleteHome(TheWhiteBread.database, player.uuid, name)
        }
        bukkitPlayer.sendMessage(miniMessage.deserialize("<red>Stored Location: <bold><white>$name</white></bold> Deleted!"))

        return 1
    }

    fun teleportLocation(ctx: CommandContext<CommandSourceStack>): Int {
        val name = StringArgumentType.getString(ctx, "name")
        val player = ctx.source.player ?: return 0
        val bukkitPlayer = Bukkit.getPlayer(player.uuid) ?: return 0
        runBlocking {
            val local: Location =
                HomesSql.loadHome(TheWhiteBread.database, bukkitPlayer.uniqueId, name) ?: return@runBlocking 0
            bukkitPlayer.teleport(local)
        }


        return 1
    }

    fun register() {
        CommandBuilder()
            .setEntry("location") { ctx ->
                run {
                    val msg = """
                        <gray>Sub commands:
                           <green>store</green>  - Store a location with a given name to teleport to later.
                           <green>delete</green>  - Delete a stored location.
                           <green>teleport</green>  - Teleport to a saved location.
                        """".trimIndent()
                    val player = CommandBuilder.bukkitPlayer(ctx) ?: return@setEntry 0
                    player.message(msg)
                    1
                }
            }
            .addChildWiRequiredArgument<String>(
                "store",
                StringArgumentType.word(),
                "name",
                action = ::storeLocation
            )
            .addChildWiRequiredArgument<String>(
                "delete",
                StringArgumentType.word(),
                "name",
                suggests = homeSuggestionProvider,
                action = ::deleteLocation
            )
            .addChildWiRequiredArgument<String>(
                "teleport",
                StringArgumentType.word(),
                "name",
                suggests = homeSuggestionProvider,
                action = ::teleportLocation
            )
            .registerPluginGroup()
    }

    object HomesSql {
        data class Home(val owner: UUID, val name: String, val location: Location)

        fun ensureHomesTable(plugin: JavaPlugin, ds: HikariDataSource) {
            val sql = """
    CREATE TABLE IF NOT EXISTS storedlocations (
      id BIGINT AUTO_INCREMENT PRIMARY KEY,
      owner_uuid CHAR(36) NOT NULL,
      name VARCHAR(64) NOT NULL,
      world VARCHAR(255) NOT NULL,
      x DOUBLE NOT NULL,
      y DOUBLE NOT NULL,
      z DOUBLE NOT NULL,
      yaw FLOAT NOT NULL,
      pitch FLOAT NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      UNIQUE KEY ux_owner_name (owner_uuid, name),
      INDEX idx_owner (owner_uuid)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
  """.trimIndent()

            TheWhiteBread.threadingScopes.pluginScope.launch {
                ds.connection.use { conn ->
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.executeUpdate()
                    }
                    conn.commit()
                }

            }
        }

        @JvmStatic
        suspend fun saveHomeMysql(ds: HikariDataSource, home: Home) = withContext(Dispatchers.IO) {
            ds.connection.use { conn ->
                val sql = """
      INSERT INTO storedlocations(owner_uuid, name, world, x, y, z, yaw, pitch)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        world = VALUES(world),
        x = VALUES(x),
        y = VALUES(y),
        z = VALUES(z),
        yaw = VALUES(yaw),
        pitch = VALUES(pitch),
        updated_at = CURRENT_TIMESTAMP
    """.trimIndent()
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setString(1, home.owner.toString())
                    stmt.setString(2, home.name)
                    stmt.setString(3, home.location.world.name)
                    stmt.setDouble(4, home.location.x)
                    stmt.setDouble(5, home.location.y)
                    stmt.setDouble(6, home.location.z)
                    stmt.setFloat(7, home.location.yaw)
                    stmt.setFloat(8, home.location.pitch)
                    stmt.executeUpdate()
                    conn.commit()
                }
            }
        }

        @JvmStatic
        suspend fun loadHome(ds: HikariDataSource, owner: UUID, name: String) =
            withContext(Dispatchers.IO) {
                ds.connection.use { conn ->
                    val sql = "SELECT world,x,y,z,yaw,pitch FROM storedlocations WHERE owner_uuid = ? AND name = ?"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, owner.toString())
                        stmt.setString(2, name)
                        stmt.executeQuery().use { rs ->
                            if (!rs.next()) return@withContext null
                            val worldName = rs.getString("world") ?: return@withContext null
                            val world = Bukkit.getWorld(worldName) ?: return@withContext null
                            val x = rs.getDouble("x")
                            val y = rs.getDouble("y")
                            val z = rs.getDouble("z")
                            val yaw = rs.getFloat("yaw")
                            val pitch = rs.getFloat("pitch")
                            Location(world, x, y, z, yaw.toDouble().toFloat(), pitch.toDouble().toFloat())
                        }
                    }
                }
            }

        @JvmStatic
        suspend fun deleteHome(ds: HikariDataSource, owner: UUID, name: String) =
            withContext(Dispatchers.IO) {
                ds.connection.use { conn ->
                    val sql = "DELETE FROM storedlocations WHERE owner_uuid = ? AND name = ?"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, owner.toString())
                        stmt.setString(2, name)
                        stmt.executeUpdate()
                        conn.commit()
                    }
                }
            }

        @JvmStatic
        suspend fun getHomes(ds: HikariDataSource, owner: UUID): Set<Pair<String, Location>> =
            withContext(Dispatchers.IO) {
                val results = LinkedHashSet<Pair<String, Location>>()
                try {
                    ds.connection.use { conn ->
                        val sql = """
          SELECT name, world, x, y, z, yaw, pitch
          FROM storedlocations
          WHERE owner_uuid = ?
        """.trimIndent()
                        conn.prepareStatement(sql).use { stmt ->
                            stmt.setString(1, owner.toString())
                            stmt.executeQuery().use { rs ->
                                while (rs.next()) {
                                    val name = rs.getString("name") ?: continue
                                    val worldName = rs.getString("world") ?: continue
                                    val world = Bukkit.getWorld(worldName) ?: continue // skip if world not loaded
                                    val x = rs.getDouble("x")
                                    val y = rs.getDouble("y")
                                    val z = rs.getDouble("z")
                                    val yaw = rs.getFloat("yaw")
                                    val pitch = rs.getFloat("pitch")
                                    val loc =
                                        Location(world, x, y, z, yaw.toDouble().toFloat(), pitch.toDouble().toFloat())
                                    results.add(name to loc)
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    // Log or handle as needed; for now just return what we have (likely empty)
                    ex.printStackTrace()
                }
                results
            }

    }


}