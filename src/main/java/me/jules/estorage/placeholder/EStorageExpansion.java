package me.jules.estorage.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.jules.estorage.EStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EStorageExpansion extends PlaceholderExpansion {

    private final EStorage plugin;

    public EStorageExpansion(EStorage plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Jules";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "estorage";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("host")) {
            UUID hostUuid = plugin.getInviteManager().getHostOf(player.getUniqueId());
            if (hostUuid == null) return "None";
            return Bukkit.getOfflinePlayer(hostUuid).getName();
        }

        if (params.equalsIgnoreCase("invited")) {
            Set<UUID> invited = plugin.getInviteManager().getInvitedPlayers(player.getUniqueId());
            if (invited.isEmpty()) return "None";
            return invited.stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .collect(Collectors.joining(", "));
        }

        if (params.equalsIgnoreCase("max_invites")) {
            if (player.isOnline()) {
                return String.valueOf(plugin.getStorageManager().getMaxInvitesForPlayer(player.getPlayer()));
            }
            return "0";
        }

        if (params.equalsIgnoreCase("party_count")) {
            return String.valueOf(plugin.getInviteManager().getInvitedPlayers(player.getUniqueId()).size());
        }

        return null;
    }
}
