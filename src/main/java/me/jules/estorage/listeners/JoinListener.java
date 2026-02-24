package me.jules.estorage.listeners;

import me.jules.estorage.EStorage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class JoinListener implements Listener {

    private final EStorage plugin;

    public JoinListener(EStorage plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            List<String> messages = plugin.getDatabase().getAndClearPendingMessages(event.getPlayer().getUniqueId());
            for (String message : messages) {
                event.getPlayer().sendMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
