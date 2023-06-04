import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BankPlugin extends JavaPlugin {

    private Economy economy;
    private Connection connection;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Vaultが見つかりません。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupDatabase()) {
            getLogger().severe("データベースの接続に失敗しました。プラグインを無効化します。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Bankプラグインが有効になりました。");
    }

    @Override
    public void onDisable() {
        closeDatabaseConnection();
        getLogger().info("Bankプラグインが無効になりました。");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    private boolean setupDatabase() {
        String host = "localhost";
        int port = 3306;
        String database = "bank_database";
        String username = "username";
        String password = "password";

        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password);
            createTables();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createTables() {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS bank_accounts (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "balance DOUBLE DEFAULT 0" +
                        ")"
        )) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void closeDatabaseConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private double getBalanceFromDatabase(String uuid) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance FROM bank_accounts WHERE uuid = ?"
        )) {
            statement.setString(1, uuid);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getDouble("balance");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void updateBalanceInDatabase(String uuid, double balance) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bank_accounts (uuid, balance) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE balance = ?"
        )) {
            statement.setString(1, uuid);
            statement.setDouble(2, balance);
            statement.setDouble(3, balance);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean deposit(CommandSender sender, double amount) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーからのみ実行できます。");
            return false;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        String uuid = player.getUniqueId().toString();
        double currentBalance = getBalanceFromDatabase(uuid);

        EconomyResponse response = economy.depositPlayer(player, amount);
        if (response.transactionSuccess()) {
            double newBalance = currentBalance + amount;
            updateBalanceInDatabase(uuid, newBalance);
            sender.sendMessage(ChatColor.GREEN + "口座に " + amount + " を預けました。");
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "預金に失敗しました。");
            return false;
        }
    }

    private boolean withdraw(CommandSender sender, double amount) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーからのみ実行できます。");
            return false;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        String uuid = player.getUniqueId().toString();
        double currentBalance = getBalanceFromDatabase(uuid);

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (response.transactionSuccess()) {
            double newBalance = currentBalance - amount;
            updateBalanceInDatabase(uuid, newBalance);
            sender.sendMessage(ChatColor.GREEN + "口座から " + amount + " を引き出しました。");
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "引き出しに失敗しました。");
            return false;
        }
    }

    private double getBalance(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーからのみ実行できます。");
            return 0;
        }

        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        String uuid = player.getUniqueId().toString();
        return getBalanceFromDatabase(uuid);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("deposit")) {
            if (args.length == 1) {
                try {
                    double amount = Double.parseDouble(args[0]);
                    return deposit(sender, amount);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "無効な金額です。");
                    return false;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "使用方法: /deposit <金額>");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("withdraw")) {
            if (args.length == 1) {
                try {
                    double amount = Double.parseDouble(args[0]);
                    return withdraw(sender, amount);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "無効な金額です。");
                    return false;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "使用方法: /withdraw <金額>");
                return false;
            }
        } else if (command.getName().equalsIgnoreCase("balance")) {
            double balance = getBalance(sender);
            sender.sendMessage(ChatColor.YELLOW + "残高: " + balance);
            return true;
        }
        return false;
    }
}
