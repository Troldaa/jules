package me.jules.estorage.managers;

import me.jules.estorage.EStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class StorageManager {
    private final EStorage plugin;
    private final Map<UUID, Inventory> activeStorages = new HashMap<>();

    public StorageManager(EStorage plugin) {
        this.plugin = plugin;
    }

    public void openStorage(Player viewer, Player owner) {
        Inventory inv = activeStorages.get(owner.getUniqueId());
        if (inv == null) {
            inv = loadStorage(owner);
            activeStorages.put(owner.getUniqueId(), inv);
        }
        viewer.openInventory(inv);
    }

    private Inventory loadStorage(Player owner) {
        int rows = getRowsForPlayer(owner);
        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("menu_title", "&8Storage")
                .replace("%host%", owner.getName())
                .replace("%player_name%", owner.getName()));

        Inventory inv = Bukkit.createInventory(null, rows * 9, title);

        // Load items from database
        try {
            ItemStack[] items = plugin.getDatabase().loadStorage(owner.getUniqueId());
            if (items != null) {
                if (items.length == inv.getSize()) {
                    inv.setContents(items);
                } else {
                    // Resize: copy items one by one
                    for (int i = 0; i < Math.min(items.length, inv.getSize()); i++) {
                        inv.setItem(i, items[i]);
                    }
                }
            } else {
                // First time, apply default items from config
                applyDefaultItems(inv, owner);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            owner.sendMessage(ChatColor.RED + "Error loading your storage from database!");
        }

        return inv;
    }

    public void applyDefaultItems(Inventory inv, Player owner) {
        ConfigurationSection storagesSection = plugin.getConfig().getConfigurationSection("storages");
        if (storagesSection == null) return;

        String bestKey = "default";
        for (String key : storagesSection.getKeys(false)) {
            String permission = storagesSection.getString(key + ".permission", "");
            if (permission.isEmpty() || owner.hasPermission(permission)) {
                bestKey = key;
            }
        }

        ConfigurationSection itemsSection = storagesSection.getConfigurationSection(bestKey + ".items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                int slot = itemsSection.getInt(itemKey + ".slot");
                String materialName = itemsSection.getString(itemKey + ".material");
                String displayName = itemsSection.getString(itemKey + ".display_name");
                List<String> lore = itemsSection.getStringList(itemKey + ".lore");

                Material material = Material.matchMaterial(materialName);
                if (material != null && slot < inv.getSize()) {
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (displayName != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                        if (lore != null) meta.setLore(lore.stream().map(s -> ChatColor.translateAlternateColorCodes('&', s)).collect(Collectors.toList()));
                        item.setItemMeta(meta);
                    }
                    inv.setItem(slot, item);
                }
            }
        }
    }

    public int getRowsForPlayer(Player player) {
        ConfigurationSection storagesSection = plugin.getConfig().getConfigurationSection("storages");
        if (storagesSection == null) return 3;

        int maxRows = 1;
        for (String key : storagesSection.getKeys(false)) {
            String permission = storagesSection.getString(key + ".permission", "");
            if (permission.isEmpty() || player.hasPermission(permission)) {
                maxRows = Math.max(maxRows, storagesSection.getInt(key + ".size", 3));
            }
        }
        return maxRows;
    }

    public int getMaxInvitesForPlayer(Player player) {
        ConfigurationSection storagesSection = plugin.getConfig().getConfigurationSection("storages");
        if (storagesSection == null) return 1;

        int maxInvites = 1;
        for (String key : storagesSection.getKeys(false)) {
            String permission = storagesSection.getString(key + ".permission", "");
            if (permission.isEmpty() || player.hasPermission(permission)) {
                maxInvites = Math.max(maxInvites, storagesSection.getInt(key + ".invitation_limit", 1));
            }
        }
        return maxInvites;
    }

    public void saveStorage(UUID uuid) {
        Inventory inv = activeStorages.get(uuid);
        if (inv != null) {
            try {
                plugin.getDatabase().saveStorage(uuid, inv.getContents());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAll() {
        for (UUID uuid : activeStorages.keySet()) {
            saveStorage(uuid);
        }
    }

    public void handleClose(UUID uuid) {
        saveStorage(uuid);
        activeStorages.remove(uuid);
    }

    public Map<UUID, Inventory> getActiveStorages() {
        return activeStorages;
    }
}
