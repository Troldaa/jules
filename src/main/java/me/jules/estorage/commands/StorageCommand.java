package me.jules.estorage.commands;

import me.jules.estorage.EStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StorageCommand implements CommandExecutor {

    private final EStorage plugin;

    public StorageCommand(EStorage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only_players"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Open own storage
            plugin.getStorageManager().openStorage(player, player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "party":
                openPartyStorage(player);
                break;
            case "invite":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /estorage invite <player>");
                    return true;
                }
                invitePlayer(player, args[1]);
                break;
            case "kick":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /estorage kick <player>");
                    return true;
                }
                kickPlayer(player, args[1]);
                break;
            case "bypassip":
                if (!player.hasPermission("estorage.admin")) {
                    player.sendMessage(getMessage("no_permission"));
                    return true;
                }
                plugin.getInviteManager().toggleIpBypass(player.getUniqueId());
                if (plugin.getInviteManager().hasIpBypass(player.getUniqueId())) {
                    player.sendMessage(getMessage("ip_bypass_enabled"));
                } else {
                    player.sendMessage(getMessage("ip_bypass_disabled"));
                }
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use: /estorage [party|invite|kick|bypassip]");
                break;
        }

        return true;
    }

    private void openPartyStorage(Player player) {
        UUID hostUuid = plugin.getInviteManager().getHostOf(player.getUniqueId());
        if (hostUuid == null) {
            player.sendMessage(getMessage("not_invited"));
            return;
        }

        Player host = Bukkit.getPlayer(hostUuid);
        if (host == null || !host.isOnline()) {
            player.sendMessage(getMessage("storage_not_found"));
            return;
        }

        plugin.getStorageManager().openStorage(player, host);
    }

    private void invitePlayer(Player host, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            host.sendMessage(getMessage("player_not_found"));
            return;
        }

        if (plugin.getInviteManager().isInvited(host.getUniqueId(), target.getUniqueId())) {
            host.sendMessage(getMessage("already_invited"));
            return;
        }

        if (plugin.getInviteManager().invite(host, target)) {
            host.sendMessage(getMessage("invite_sent").replace("%player%", target.getName()));
            target.sendMessage(getMessage("invited").replace("%player%", host.getName()));
        } else {
            if (host.getAddress().getAddress().getHostAddress().equals(target.getAddress().getAddress().getHostAddress()) && !plugin.getInviteManager().hasIpBypass(host.getUniqueId())) {
                host.sendMessage(getMessage("same_ip"));
            } else {
                host.sendMessage(getMessage("limit_reached"));
            }
        }
    }

    private void kickPlayer(Player host, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        UUID targetUuid = null;
        String name = targetName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            name = target.getName();
        } else {
            // Try to find in invited players list if offline?
            // For simplicity, we only kick online players or we'd need to store names.
            // But let's check the invited list.
            for (UUID uuid : plugin.getInviteManager().getInvitedPlayers(host.getUniqueId())) {
                if (Bukkit.getOfflinePlayer(uuid).getName().equalsIgnoreCase(targetName)) {
                    targetUuid = uuid;
                    name = Bukkit.getOfflinePlayer(uuid).getName();
                    break;
                }
            }
        }

        if (targetUuid == null || !plugin.getInviteManager().isInvited(host.getUniqueId(), targetUuid)) {
            host.sendMessage(getMessage("not_invited_to_host"));
            return;
        }

        plugin.getInviteManager().kick(host.getUniqueId(), targetUuid);
        host.sendMessage(getMessage("kick_success").replace("%player%", name));
        if (target != null) {
            target.sendMessage(getMessage("kicked").replace("%player%", host.getName()));
        }
    }

    private String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages." + key, "Message not found: " + key));
    }
}
