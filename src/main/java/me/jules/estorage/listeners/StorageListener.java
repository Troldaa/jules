package me.jules.estorage.listeners;

import me.jules.estorage.EStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.UUID;

public class StorageListener implements Listener {

    private final EStorage plugin;

    public StorageListener(EStorage plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getWhoClicked();

        UUID ownerUuid = null;
        for (Map.Entry<UUID, Inventory> entry : plugin.getStorageManager().getActiveStorages().entrySet()) {
            if (entry.getValue().equals(inv)) {
                ownerUuid = entry.getKey();
                break;
            }
        }

        if (ownerUuid != null) {
            int slot = event.getRawSlot();
            String command = plugin.getStorageManager().getClickCommand(ownerUuid, slot);
            if (command != null) {
                event.setCancelled(true);
                executeCommand(player, command);
            }
        }
    }

    private void executeCommand(Player player, String command) {
        if (command.equalsIgnoreCase("[close]")) {
            player.closeInventory();
        } else {
            // Support other commands?
            if (command.startsWith("/")) {
                player.performCommand(command.substring(1));
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();

        // Check if this inventory is one of our managed storages
        UUID ownerUuid = null;
        for (Map.Entry<UUID, Inventory> entry : plugin.getStorageManager().getActiveStorages().entrySet()) {
            if (entry.getValue().equals(inv)) {
                ownerUuid = entry.getKey();
                break;
            }
        }

        if (ownerUuid != null) {
            // If this was the last viewer, save and remove from cache
            // Viewers list includes the person currently closing the inventory
            if (inv.getViewers().size() <= 1) {
                plugin.getStorageManager().handleClose(ownerUuid);
            } else {
                plugin.getStorageManager().saveStorage(ownerUuid);
            }
        }
    }
}
