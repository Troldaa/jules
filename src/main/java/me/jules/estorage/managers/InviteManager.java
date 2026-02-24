package me.jules.estorage.managers;

import me.jules.estorage.EStorage;
import org.bukkit.entity.Player;

import java.util.*;

public class InviteManager {
    private final EStorage plugin;
    private final Map<UUID, Set<UUID>> invites = new HashMap<>(); // Host -> Invited
    private final Set<UUID> ipBypass = new HashSet<>();

    // Pending confirmations: Host UUID -> Target UUID
    private final Map<UUID, UUID> pendingHostConfirmations = new HashMap<>();
    // Pending invitations: Target UUID -> Host UUID
    private final Map<UUID, UUID> pendingGuestAcceptances = new HashMap<>();

    public InviteManager(EStorage plugin) {
        this.plugin = plugin;
    }

    public boolean canInvite(Player host, Player target) {
        if (host.getAddress().getAddress().getHostAddress().equals(target.getAddress().getAddress().getHostAddress()) && !ipBypass.contains(host.getUniqueId())) {
            return false; // Same IP and no bypass
        }

        int limit = plugin.getStorageManager().getMaxInvitesForPlayer(host);
        Set<UUID> hostInvites = invites.computeIfAbsent(host.getUniqueId(), k -> new HashSet<>());

        if (hostInvites.size() >= limit) {
            return false; // Limit reached
        }
        return true;
    }

    public void startHostConfirmation(UUID host, UUID target) {
        pendingHostConfirmations.put(host, target);
        // We could add a task to remove it after 30 seconds
    }

    public UUID getPendingHostConfirmation(UUID host) {
        return pendingHostConfirmations.get(host);
    }

    public void removeHostConfirmation(UUID host) {
        pendingHostConfirmations.remove(host);
    }

    public void startGuestAcceptance(UUID host, UUID target) {
        pendingGuestAcceptances.put(target, host);
    }

    public UUID getPendingGuestAcceptance(UUID target) {
        return pendingGuestAcceptances.get(target);
    }

    public void removeGuestAcceptance(UUID target) {
        pendingGuestAcceptances.remove(target);
    }

    public void finalizeInvite(UUID host, UUID target) {
        invites.computeIfAbsent(host, k -> new HashSet<>()).add(target);
    }

    public boolean hasPendingInvitation(UUID host, UUID target) {
        // Check if host is waiting to confirm invitation to target
        if (target.equals(pendingHostConfirmations.get(host))) return true;
        // Check if target is waiting to accept invitation from host
        if (host.equals(pendingGuestAcceptances.get(target))) return true;
        return false;
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
