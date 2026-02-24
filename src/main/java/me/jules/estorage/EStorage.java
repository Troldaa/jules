package me.jules.estorage;

import me.jules.estorage.commands.StorageCommand;
import me.jules.estorage.database.Database;
import me.jules.estorage.database.MySQL;
import me.jules.estorage.database.SQLite;
import me.jules.estorage.listeners.JoinListener;
import me.jules.estorage.listeners.StorageListener;
import me.jules.estorage.managers.InviteManager;
import me.jules.estorage.managers.StorageManager;
import me.jules.estorage.placeholder.EStorageExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class EStorage extends JavaPlugin {

    private static EStorage instance;
    private StorageManager storageManager;
    private InviteManager inviteManager;
    private Database database;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        setupDatabase();

        storageManager = new StorageManager(this);
        inviteManager = new InviteManager(this);

        getCommand("estorage").setExecutor(new StorageCommand(this));
        getServer().getPluginManager().registerEvents(new StorageListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new EStorageExpansion(this).register();
        }

        getLogger().info("EStorage has been enabled!");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.saveAll();
        }
        if (database != null) {
            database.close();
        }
        getLogger().info("EStorage has been disabled!");
    }

    private void setupDatabase() {
        String type = getConfig().getString("database.type", "sqlite");
        if (type.equalsIgnoreCase("mysql")) {
            database = new MySQL(this);
        } else {
            database = new SQLite(this);
        }

        try {
            database.connect();
            database.createTables();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            getLogger().severe("Could not connect to database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public static EStorage getInstance() {
        return instance;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public Database getDatabase() {
        return database;
    }
}
