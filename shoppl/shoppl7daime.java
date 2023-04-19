import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;

public class ShopPlugin extends JavaPlugin implements Listener {

    // HashMap to store shops created by players
    private HashMap<Block, Shop> shops = new HashMap<>();

    @Override
    public void onEnable() {
        // Register events for player interaction with signs
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Remove all shop signs when the plugin is disabled
        for (Block sign : shops.keySet()) {
            sign.setType(Material.AIR);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("shop")) {
            if (args.length == 3) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (args[0].equalsIgnoreCase("create")) {
                        if (args[1].equalsIgnoreCase("buy")) {
                            createBuyShop(player, Double.parseDouble(args[2]));
                        } else if (args[1].equalsIgnoreCase("sell")) {
                            createSellShop(player, Double.parseDouble(args[2]));
                        } else {
                            player.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /shop create [buy/sell] [price]");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /shop create [buy/sell] [price]");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /shop create [buy/sell] [price]");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                if (sign.getLine(0).equals(ChatColor.BLUE + "[販売ショップ]")) {
                    event.setCancelled(true);
                    openBuyShop(event.getPlayer(), sign);
                } else if (sign.getLine(0).equals(ChatColor.BLUE + "[買取ショップ]")) {
                    event.setCancelled(true);
                    openSellShop(event.getPlayer(), sign);
                }
            }
        }
    }

    private void createBuyShop(Player player, double price) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.AIR) {
            Block sign = player.getTargetBlock(null, 5);
            if (sign.getType() == Material.OAK_WALL_SIGN) {
                sign.setType(Material.OAK_SIGN);
                Sign signState = (Sign) sign.getState();
                  signState.setLine(0, ChatColor.BLUE + "[販売ショップ]");
                signState.setLine(1, item.getItemMeta().getDisplayName());
                signState.setLine(2, "$" + price);
                signState.update();
                Shop shop = new Shop(player.getUniqueId(), item, price, true);
                shops.put(sign, shop);
                player.sendMessage(ChatColor.GREEN + "ショップを作成しました。");
            } else {
                player.sendMessage(ChatColor.RED + "看板を置く場所が適切ではありません。");
            }
        } else {
            player.sendMessage(ChatColor.RED + "アイテムを手に持ってください。");
        }
    }

    private void createSellShop(Player player, double price) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.AIR) {
            Block sign = player.getTargetBlock(null, 5);
            if (sign.getType() == Material.OAK_WALL_SIGN) {
                sign.setType(Material.OAK_SIGN);
                Sign signState = (Sign) sign.getState();
                signState.setLine(0, ChatColor.BLUE + "[買取ショップ]");
                signState.setLine(1, item.getItemMeta().getDisplayName());
                signState.setLine(2, "$" + price);
                signState.update();
                Shop shop = new Shop(player.getUniqueId(), item, price, false);
                shops.put(sign, shop);
                player.sendMessage(ChatColor.GREEN + "ショップを作成しました。");
            } else {
                player.sendMessage(ChatColor.RED + "看板を置く場所が適切ではありません。");
            }
        } else {
            player.sendMessage(ChatColor.RED + "アイテムを手に持ってください。");
        }
    }

    private void openBuyShop(Player player, Sign sign) {
        Shop shop = shops.get(sign.getBlock());
        if (shop != null) {
            if (shop.isBuying()) {
                ItemStack item = shop.getItem().clone();
                ItemMeta meta = item.getItemMeta();
                ArrayList<String> lore = new ArrayList<String>();
                lore.add(ChatColor.GREEN + "価格: $" + shop.getPrice());
                meta.setLore(lore);
                item.setItemMeta(meta);
                player.getInventory().addItem(item);
                player.sendMessage(ChatColor.GREEN + "アイテムを購入しました。");
            } else {
                player.sendMessage(ChatColor.RED + "このショップは買取専門です。");
            }
        } else {
            player.sendMessage(ChatColor.RED + "この看板はショップではありません。");
        }
    }

    private void openSellShop(Player player, Sign sign) {
        Shop shop = shops.get(sign.getBlock());
        if (shop != null) {
            if (!shop.isBuying()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && item.isSimilar(shop.getItem())) {
                    int amount = item.getAmount();
                    if (amount >= 1) {
                        double price = shop.getPrice() * amount;
                        Bukkit.getOfflinePlayer(shop.getOwner()).getPlayer().getInventory().addItem(item);
                        player.getInventory().removeItem(item);
                        economy.depositPlayer(player, price);
                        player.sendMessage(ChatColor.GREEN + "アイテムを売却しました。$" + price + " を手に入れました。");
                    } else {
                        player.sendMessage(ChatColor.RED + "アイテムを手に持ってください。");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "このアイテムはこのショップでは買い取れません。");
                }
            } else {
                player.sendMessage(ChatColor.RED + "このショップは販売専門です。");
            }
        } else {
            player.sendMessage(ChatColor.RED + "この看板はショップではありません。");
        }
    }

    private void openShopGUI(Player player, Sign sign) {
        Shop shop = shops.get(sign.getBlock());
        if (shop != null) {
            Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.BLUE + "ショップ");
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            }
            ItemStack item = shop.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            ArrayList<String> lore = new ArrayList<String>();
            if (shop.isBuying()) {
                lore.add(ChatColor.GREEN + "価格: $" + shop.getPrice());
            } else {
                lore.add(ChatColor.RED + "買取価格: $" + shop.getPrice());
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(13, item);
            player.openInventory(inventory);
        } else {
            player.sendMessage(ChatColor.RED + "この看板はショップではありません。");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.OAK_SIGN || block.getType() == Material.OAK_WALL_SIGN) {
                Sign sign = (Sign) block.getState();
                if (sign.getLine(0).equals(ChatColor.BLUE + "[販売ショップ]") || sign.getLine(0).equals(ChatColor.BLUE + "[買取ショップ]")) {
                    openShopGUI(event.getPlayer(), sign);
                    event.setCancelled(true);
                }
            }
        }
    }
}

