package de.sean.splugin.util;

/* Spigot */
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/* Java */
import java.util.*;

public class PlayerType {
    private static List<PlayerType> playerTypes = new ArrayList<>();
    private static HashMap<UUID, PlayerType> players = new HashMap<>();

    public static final PlayerType DEFAULT = registerPlayerType("DEFAULT", "Default", ChatColor.GREEN);

    public final String id;
    public final String name;
    public final ChatColor color;

    private PlayerType(String id, String name, ChatColor color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public static void loadFromConfig(FileConfiguration config) {
        try {
            // We load the roles at startup
            // The PlayerType of a player gets loaded up when they join the server.
            Map<String, Object> roles = config.getConfigurationSection("Roles").getValues(false);
            for (Map.Entry<String, Object> pair : roles.entrySet()) {
                String id = pair.getKey(), name = null;
                ChatColor color = null;
                if (pair.getValue() instanceof MemorySection) {
                    Map<String, Object> role = ((MemorySection) pair.getValue()).getValues(false);
                    for (Map.Entry<String, Object> rolePair : role.entrySet()) {
                        if (rolePair.getKey().toLowerCase().equals("name")) name = rolePair.getValue().toString();
                        else if (rolePair.getKey().toLowerCase().equals("color")) color = (ChatColor) ChatColor.class.getField(rolePair.getValue().toString().toUpperCase()).get(null);
                    }
                    if (id != null && name != null) registerPlayerType(id, name, color == null ? ChatColor.WHITE : color);
                }
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public static PlayerType registerPlayerType(@NotNull String id, @NotNull String name, @NotNull ChatColor color) {
        PlayerType playerType = new PlayerType(id, name, color);
        if (!playerTypes.contains(playerType)) {
            playerTypes.add(playerType);
            return playerType;
        } else {
            System.out.println("WARN: Duplicate player role: " + id);
        }
        return null;
    }

    public static PlayerType setPlayerTypeForPlayer(@NotNull UUID uuid, @NotNull PlayerType playerType) {
        if (players == null) players = new HashMap<>();
        players.put(uuid, playerType);
        return playerType;
    }

    public static PlayerType getPlayerTypeForPlayer(UUID uuid) {
        if (players == null) players = new HashMap<>();
        return players.get(uuid);
    }

    public static PlayerType getForId(String id) {
        for (PlayerType playerType : playerTypes) {
            if (playerType.id.equals(id)) {
                return playerType;
            }
        }
        return PlayerType.DEFAULT;
    }
}
