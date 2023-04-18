package com.example.shops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopsPlugin extends JavaPlugin implements Listener {
    
    private final Logger logger = getLogger();
    private final Map<String, Shop> shops = new HashMap<>();
    private final String BUY_PREFIX = "[販売ショップ]";
    private final String SELL_PREFIX = "[買取ショップ]";
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        logger.info("Shopsプラグインが有効になりました。");
    }
    
    @Override
    public void onDisable() {
        logger.info("Shopsプラグインが無効になりました。");
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (event.getLine(0).equals("/shop create buy") || event.getLine(0).equals("/shop create sell")) {
            if (!player.hasPermission("shops.create")) {
                player.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。");
                return;
            }
            ItemStack itemStack = player.getItemInHand();
            if (!itemStack.getType().equals(Material.AIR) && !event.getLine(2).isEmpty()) {
                String name = itemStack.getItemMeta().getDisplayName();
                int price = Integer.parseInt(event.getLine(2));
                if (event.getLine(0).equals("/shop create buy")) {
                    createShop(event.getBlock(), name, price, ShopType.BUY);
                } else {
                    createShop(event.getBlock(), name, price, ShopType.SELL);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        Player player = event.getPlayer();
        if (clickedBlock != null) {
            if (clickedBlock.getState() instanceof Sign) {
                Sign sign = (Sign) clickedBlock.getState();
                String line1 = sign.getLine(0);
                if (line1.equals(BUY_PREFIX) || line1.equals(SELL_PREFIX)) {
                    if (shops.containsKey(sign.getLine(1))) {
                        Shop shop = shops.get(sign.getLine(1));
                        event.setCancelled(true);
                        openShopGUI(player, shop);
                    }
                }
            }
        }
    }
    
    private void createShop(Block block, String name, int price, ShopType type) {
        Sign sign = (Sign) block.getState();
        sign.setLine(0, (type == ShopType.BUY) ? BUY_PREFIX : SELL_PREFIX);
        sign.setLine(1, name);
        sign.setLine(2, "価格: " + price);
        sign.update();
        Shop shop = new Shop(name, block.getLocation(), price, type);
        shops.put(name, shop);
    }
    
    private void openShopGUI(Player player, Shop shop) {
        Inventory shopInventory = Bukkit.createInventory(null, 27, shop.getName());
        List<ItemStack> items = (shop.getType() == ShopType.BUY) ? getBuyItems(shop) : getSellItems(shop);
        for (ItemStack item : items) {
            shopInventory.addItem(item);
        }
        player.openInventory(shopInventory);
    }
    
    private List<ItemStack> getBuyItems(Shop shop) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : shop.getItems()) {
            ItemStack displayItem = new ItemStack(item);
            ItemMeta itemMeta = displayItem.getItemMeta();
            itemMeta.setDisplayName(itemMeta.getDisplayName() + " - " + shop.getPrice());
            displayItem.setItemMeta(itemMeta);
            items.add(displayItem);
        }
        return items;
    }
    
    private List<ItemStack> getSellItems(Shop shop) {
        return shop.getItems();
    }
    
    private class Shop {
        private final String name;
        private final Location location;
        private final int price;
        private final ShopType type;
        private final List<ItemStack> items = new ArrayList<>();
        
        public Shop(String name, Location location, int price, ShopType type) {
            this.name = name;
            this.location = location;
            this.price = price;
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public Location getLocation() {
            return location;
        }
        
        public int getPrice() {
            return price;
        }
        
        public ShopType getType() {
            return type;
        }
        
        public List<ItemStack> getItems() {
            return items;
        }
        
        public void addItem(ItemStack item) {
            items.add(item);
        }
    }
    
    private enum ShopType {
        BUY, SELL;
    }
}
