package com.example.shopplugin;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopPlugin extends JavaPlugin {
    
    private HashMap<UUID, ItemStack> playerHeldItems = new HashMap<UUID, ItemStack>();
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents((listener), this);
    }
    
    @Override
    public void onDisable() {
        playerHeldItems.clear();
    }
    
    private void createShop(Player player, Material material, double price, boolean isSell) {
        Block target = player.getTargetBlock(null, 5);
        if (target.getState() instanceof Sign) {
            Sign sign = (Sign)target.getState();
            if (isSell) {
                sign.setLine(0, "[買取ショップ]");
            }
            else {
                sign.setLine(0, "[販売ショップ]");
            }
            sign.setLine(1, material.name());
            sign.setLine(2, Double.toString(price));
            sign.update();
            player.sendMessage("[ShopPlugin] ショップが作成されました。");
        }
        else {
            player.sendMessage("[ShopPlugin] ショップを作成する場所が間違っています。");
        }
    }
    
    private ItemStack createShopItem(Material material, String name, double price, boolean isSell) {
        ItemStack item = new ItemStack(material, 1);
        
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(name);
        itemMeta.setLore(Arrays.asList("価格: " + price, isSell ? "買取" : "販売"));
        item.setItemMeta(itemMeta);
        
        return item;
    }
    
    private boolean isShopItem(ItemStack item, boolean isSell) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore.size() < 2) {
            return false;
        }
        
        String sellOrBuy = isSell ? "買取" : "販売";
        if (!sellOrBuy.equals(lore.get(1))) {
            return false;
        }
        
        return true;
    }
    
    private boolean isShopSign(BlockState blockState) {
        if (!(blockState instanceof Sign)) {
            return false;
        }
        
        Sign sign = (Sign)blockState;
        String title = sign.getLine(0);
        if (!"[販売ショップ]".equals(title) && !"[買取ショップ]".equals(title)) {
            return false;
        }
        
        return true;
    }

    private boolean handleShop(Player player, Sign sign) {
        String title = sign.getLine(0);
        boolean isSell = "[買取ショップ]".equals(title);
        
        try {
            double price = Double.parseDouble(sign.getLine(2));
            ItemStack itemStack = createShopItem(Material.getMaterial(sign.getLine(1)), sign.getLine(1), price, isSell);
            playerHeldItems.put(player.getUniqueId(), itemStack);
            player.sendMessage("[ShopPlugin] " + sign.getLine(1) + "を " + (isSell ? "買取" : "販売") + " ショップから選択しました。");
            return true;
        }
        catch (NumberFormatException e) {
            player.sendMessage("[ShopPlugin] ショップの値段が数値ではありません。");
            return false;
        }
    }
    
    private boolean handleShopTransaction(Player player, ItemStack shopItem, ItemStack playerItem, double price, boolean isSell) {
        boolean success = false;
        
        String transaction = isSell ? "買取" : "販売";
        if (isSell) {
            if (player.getInventory().containsAtLeast(shopItem, 1)) {
                player.getInventory().removeItem(shopItem);
                player.getInventory().addItem(new ItemStack(Material.getMaterial(playerItem.getType().name()), 1));
                player.sendMessage("[ShopPlugin] " + playerItem.getType().name() + "を " + transaction + " しました。");
                success = true;
            }
            else {
                player.sendMessage("[ShopPlugin] " + transaction + "に必要なアイテムを持っていません。");
            }
        }
        else {
            if (player.getInventory().containsAtLeast(playerItem, 1)) {
                player.getInventory().removeItem(playerItem);
                player.getInventory().addItem(shopItem);
                player.sendMessage("[ShopPlugin] " + shopItem.getItemMeta().getDisplayName() + "を " + transaction + " しました。");
                success = true;
            }
            else {
                player.sendMessage("[ShopPlugin] " + transaction + "に必要な金額がありません。");
            }
        }
        
        return success;
    }
    
    private final Listener listener = new Listener() {
        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            
            Block target = event.getClickedBlock();
            BlockState blockState = target.getState();
            
            if (isShopSign(blockState)) {
                Sign sign = (Sign)blockState;
                if (playerHeldItems.containsKey(player.getUniqueId())) {
                    ItemStack playerItem = playerHeldItems.get(player.getUniqueId());
                    ItemStack shopItem = createShopItem(Material.getMaterial(sign.getLine(1)), sign.getLine(1), Double.parseDouble(sign.getLine(2)), "[買取ショップ]".equals(sign.getLine(0)));
                    boolean isSell = "[買取ショップ]".equals(sign.getLine(0));
                    if (isShopItem(playerItem, !isSell)) {
                        handleShopTransaction(player, shopItem, playerItem, Double.parseDouble(sign.getLine(2)), isSell);
                    }
                    else {
                        player.sendMessage("[ShopPlugin] そのアイテムは " + (isSell ? "買取" : "販売") + " ショップの取り扱い品目ではありません。");
                    }
                    playerHeldItems.remove(player.getUniqueId());
                }
                else {
                    handleShop(player, sign);
                }
            }
        }
    };
}
