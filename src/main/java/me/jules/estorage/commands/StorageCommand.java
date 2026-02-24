package me.jules.estorage.commands;

import me.jules.estorage.EStorage;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StorageCommand implements CommandExecutor, TabCompleter {

    private final EStorage plugin;

    public StorageCommand(EStorage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
                Player targetBypass = player;
                if (args.length >= 2) {
                    targetBypass = Bukkit.getPlayer(args[1]);
                    if (targetBypass == null) {
                        player.sendMessage(getMessage("player_not_found"));
                        return true;
                    }
                }
                plugin.getInviteManager().toggleIpBypass(targetBypass.getUniqueId());
                if (plugin.getInviteManager().hasIpBypass(targetBypass.getUniqueId())) {
                    player.sendMessage(getMessage("ip_bypass_enabled").replace("%player%", targetBypass.getName()));
                } else {
                    player.sendMessage(getMessage("ip_bypass_disabled").replace("%player%", targetBypass.getName()));
                }
                break;
            case "accept":
                handleAccept(player);
                break;
            case "deny":
                handleDeny(player);
                break;
            case "reload":
                if (!player.hasPermission("estorage.admin")) {
                    player.sendMessage(getMessage("no_permission"));
                    return true;
                }
                plugin.reloadPluginConfig();
                player.sendMessage(getMessage("reload_success"));
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use: /estorage [party|invite|kick|bypassip|reload]");
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

        if (host.getUniqueId().equals(target.getUniqueId())) {
            host.sendMessage(getMessage("cannot_invite_self"));
            return;
        }

        if (plugin.getInviteManager().isInvited(host.getUniqueId(), target.getUniqueId())) {
            host.sendMessage(getMessage("already_in_party"));
            return;
        }

        if (plugin.getInviteManager().hasPendingInvitation(host.getUniqueId(), target.getUniqueId())) {
            host.sendMessage(getMessage("already_invited"));
            return;
        }

        if (plugin.getInviteManager().canInvite(host, target)) {
            plugin.getInviteManager().startHostConfirmation(host.getUniqueId(), target.getUniqueId());

            TextComponent message = new TextComponent(getMessage("host_confirm_prompt").replace("%player%", target.getName()));
            TextComponent accept = new TextComponent(" " + getMessage("accept_label"));
            accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, getMessageRaw("accept_command")));
            accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + "Click to accept")));

            TextComponent deny = new TextComponent(" " + getMessage("deny_label"));
            deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, getMessageRaw("deny_command")));
            deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.RED + "Click to deny")));

            host.spigot().sendMessage(message, accept, deny);
        } else {
            if (host.getAddress().getAddress().getHostAddress().equals(target.getAddress().getAddress().getHostAddress()) && !plugin.getInviteManager().hasIpBypass(host.getUniqueId())) {
                host.sendMessage(getMessage("same_ip"));
            } else {
                host.sendMessage(getMessage("limit_reached"));
            }
        }
    }

    private void handleAccept(Player player) {
        // Check if host confirmation
        UUID hostPendingTarget = plugin.getInviteManager().getPendingHostConfirmation(player.getUniqueId());
        if (hostPendingTarget != null) {
            Player target = Bukkit.getPlayer(hostPendingTarget);
            if (target != null) {
                plugin.getInviteManager().removeHostConfirmation(player.getUniqueId());
                plugin.getInviteManager().startGuestAcceptance(player.getUniqueId(), target.getUniqueId());

                player.sendMessage(getMessage("host_accepted").replace("%player%", target.getName()));

                TextComponent message = new TextComponent(getMessage("guest_confirm_prompt").replace("%player%", player.getName()));
                TextComponent accept = new TextComponent(" " + getMessage("accept_label"));
                accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, getMessageRaw("accept_command")));
                accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + "Click to accept")));

                TextComponent deny = new TextComponent(" " + getMessage("deny_label"));
                deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, getMessageRaw("deny_command")));
                deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.RED + "Click to deny")));

                target.spigot().sendMessage(message, accept, deny);
            }
            return;
        }

        // Check if guest acceptance
        UUID guestPendingHost = plugin.getInviteManager().getPendingGuestAcceptance(player.getUniqueId());
        if (guestPendingHost != null) {
            Player host = Bukkit.getPlayer(guestPendingHost);
            if (host != null) {
                plugin.getInviteManager().removeGuestAcceptance(player.getUniqueId());
                plugin.getInviteManager().finalizeInvite(host.getUniqueId(), player.getUniqueId());

                player.sendMessage(getMessage("guest_accepted").replace("%player%", host.getName()));
                host.sendMessage(getMessage("invite_success").replace("%player%", player.getName()));
            }
            return;
        }

        player.sendMessage(getMessage("no_pending_actions"));
    }

    private void handleDeny(Player player) {
        if (plugin.getInviteManager().getPendingHostConfirmation(player.getUniqueId()) != null) {
            plugin.getInviteManager().removeHostConfirmation(player.getUniqueId());
            player.sendMessage(getMessage("action_denied"));
            return;
        }
        if (plugin.getInviteManager().getPendingGuestAcceptance(player.getUniqueId()) != null) {
            plugin.getInviteManager().removeGuestAcceptance(player.getUniqueId());
            player.sendMessage(getMessage("action_denied"));
            return;
        }
        player.sendMessage(getMessage("no_pending_actions"));
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

        String kickedMessage = getMessage("kicked").replace("%player%", host.getName());
        if (target != null && target.isOnline()) {
            target.sendMessage(kickedMessage);
        } else {
            try {
                plugin.getDatabase().addPendingMessage(targetUuid, kickedMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("party");
            completions.add("invite");
            completions.add("kick");
            if (sender.hasPermission("estorage.admin")) {
                completions.add("bypassip");
                completions.add("reload");
            }
            completions.add("accept");
            completions.add("deny");
            return completions.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick") || (args[0].equalsIgnoreCase("bypassip") && sender.hasPermission("estorage.admin"))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return completions;
    }

    private String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages." + key, "Message not found: " + key));
    }

    private String getMessageRaw(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }
}
