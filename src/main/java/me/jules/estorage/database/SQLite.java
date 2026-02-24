package me.jules.estorage.database;

import me.jules.estorage.EStorage;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;

public class SQLite extends Database {

    public SQLite(EStorage plugin) {
        super(plugin);
    }

    @Override
    public void connect() throws SQLException, ClassNotFoundException {
        File dataFolder = new File(plugin.getDataFolder(), "storage.db");
        if (!dataFolder.exists()) {
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("File write error: storage.db");
            }
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
    }

    @Override
    public void createTables() throws SQLException {
        Statement s = connection.createStatement();
        s.executeUpdate("CREATE TABLE IF NOT EXISTS storage (uuid VARCHAR(36) PRIMARY KEY, items TEXT)");
        s.close();
    }

    @Override
    public void saveStorage(UUID uuid, ItemStack[] items) throws SQLException {
        String base64 = toBase64(items);
        PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO storage (uuid, items) VALUES (?, ?)");
        ps.setString(1, uuid.toString());
        ps.setString(2, base64);
        ps.executeUpdate();
        ps.close();
    }

    @Override
    public ItemStack[] loadStorage(UUID uuid) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT items FROM storage WHERE uuid = ?");
        ps.setString(1, uuid.toString());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String base64 = rs.getString("items");
            try {
                ItemStack[] items = fromBase64(base64);
                rs.close();
                ps.close();
                return items;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        rs.close();
        ps.close();
        return null;
    }
}
