package me.jules.estorage.managers;

import me.jules.estorage.EStorage;
import org.bukkit.entity.Player;

import java.util.*;

public class InviteManager {
    private final EStorage plugin;
    private final Map<UUID, Set<UUID>> invites = new HashMap<>(); // Host -> Invited
    private final Set<UUID> ipBypass = new HashSet<>();

    public InviteManager(EStorage plugin) {
        this.plugin = plugin;
    }

    public boolean invite(Player host, Player target) {
        if (host.getAddress().getAddress().getHostAddress().equals(target.getAddress().getAddress().getHostAddress()) && !ipBypass.contains(host.getUniqueId())) {
            return false; // Same IP and no bypass
        }

        int limit = plugin.getStorageManager().getMaxInvitesForPlayer(host);
        Set<UUID> hostInvites = invites.computeIfAbsent(host.getUniqueId(), k -> new HashSet<>());

        if (hostInvites.size() >= limit) {
            return false; // Limit reached
        }

        hostInvites.add(target.getUniqueId());
        return true;
    }

    public void kick(UUID hostUuid, UUID targetUuid) {
        Set<UUID> hostInvites = invites.get(hostUuid);
        if (hostInvites != null) {
            hostInvites.remove(targetUuid);
        }
    }

    public boolean isInvited(UUID host, UUID guest) {
        Set<UUID> hostInvites = invites.get(host);
        return hostInvites != null && hostInvites.contains(guest);
    }

    public UUID getHostOf(UUID guest) {
        for (Map.Entry<UUID, Set<UUID>> entry : invites.entrySet()) {
            if (entry.getValue().contains(guest)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Set<UUID> getInvitedPlayers(UUID host) {
        return invites.getOrDefault(host, Collections.emptySet());
    }

    public void toggleIpBypass(UUID uuid) {
        if (ipBypass.contains(uuid)) {
            ipBypass.remove(uuid);
        } else {
            ipBypass.add(uuid);
        }
    }

    public boolean hasIpBypass(UUID uuid) {
        return ipBypass.contains(uuid);
    }
}
