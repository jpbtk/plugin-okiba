package com.example.shop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopPlugin extends JavaPlugin implements Listener {

    private Map<Block, Shop> shops;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        shops = new HashMap<>();
    }

    @Override
    public void onDisable() {
        saveShops();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand == null || itemInHand.getType() != Material.AIR) {
            return;
        }

        Shop shop = shops.get(clickedBlock);
        if (shop == null) {
            return;
        }

        event.setCancelled(true);
        Inventory shopGUI = shop.getGUI(player);
        player.openInventory(shopGUI);
    }

    public void createShop(Player player, String shopType) {
        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "Cannot place sign here");
            return;
        }

        Block relativeBlock = targetBlock.getRelative(BlockFace.UP);
        if (relativeBlock == null || relativeBlock.getType() != Material.AIR) {
            player.sendMessage(ChatColor.RED + "Cannot place sign here");
            return;
        }

        ItemStack signItem = player.getInventory().getItemInMainHand();
        if (signItem == null || signItem.getType() != Material.OAK_SIGN) {
            player.sendMessage(ChatColor.RED + "You need an oak sign in your hand to create a shop");
            return;
        }

        signItem.setAmount(signItem.getAmount() - 1); // Decrease sign count

        Shop shop = new Shop(player.getUniqueId(), shopType);
        shops.put(relativeBlock, shop);

        relativeBlock.setType(Material.OAK_SIGN);
        relativeBlock.setData((byte) 0x0);

        org.bukkit.block.Sign signState = (org.bukkit.block.Sign) relativeBlock.getState();
        signState.setLine(0, ChatColor.BOLD + shopType.toUpperCase());
        signState.setLine(1, shop.getNumItems() + " items for sale");
        signState.update(true);
    }

    private void saveShops() {
        File shopsFile = new File(getDataFolder(), "shops.yml");
        if (!shopsFile.exists()) {
            try {
                shopsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(shopsFile);
        for (Map.Entry<Block, Shop> entry : shops.entrySet()) {
            Block block = entry.getKey();
            Shop shop = entry.getValue();

            String key = block.getWorld().getUID() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
            yaml.set(key, shop.serialize());
        }

        try {
            yaml.save(shopsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadShops() {
        File shopsFile = new File(getDataFolder(), "shops.yml");
        if (!shopsFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(shopsFile);
        ConfigurationSection shopsSection = yaml.getConfigurationSection("");
        if (shopsSection == null) {
            return;
        }

        for (String key : shopsSection.getKeys(false)) {
            String[] parts = key.split(",");
            if (parts.length != 4) {
                continue;
            }

            Block block = Bukkit.getWorld(UUID.fromString(parts[0])).getBlockAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            if (block.getType() != Material.OAK_SIGN) {
                continue;
            }

            org.bukkit.block.Sign signState = (org.bukkit.block.Sign) block.getState();

            Shop shop = Shop.deserialize(yaml.getConfigurationSection(key));
            shops.put(block, shop);
        }
    }

    private class Shop {

        private UUID owner;
        private String type;
        private Map<ItemStack, Double> prices;

        public Shop(UUID owner, String type) {
            this.owner = owner;
            this.type = type;
            prices = new HashMap<>();
        }

        public Inventory getGUI(Player player) {
            Inventory gui = Bukkit.createInventory(player, 27, ChatColor.BOLD + type.toUpperCase() + " SHOP");

            int index = 0;
            for (Map.Entry<ItemStack, Double> entry : prices.entrySet()) {
                ItemStack item = entry.getKey();
                ItemMeta meta = item.getItemMeta();

                double price = entry.getValue();

                List<String> lore = meta.getLore();
                if (lore == null) {
                    lore = new ArrayList<>();
                }
                lore.add(ChatColor.YELLOW + "Price: $" + price);

                meta.setLore(lore);
                item.setItemMeta(meta);

                gui.setItem(index++, item);
            }

            return gui;
        }

        public int getNumItems() {
            return prices.size();
        }

        public void addItem(ItemStack item, double price) {
            prices.put(item, price);
        }

        public double removeItem(ItemStack item) {
            Double price = prices.remove(item);
            if (price == null) {
                return 0;
            } else {
                return price;
            }
        }

        public Map<String, Object> serialize() {
            Map<String, Object> map = new HashMap<>();
            map.put("owner", owner.toString());
            map.put("type", type);
            map.put("prices", prices);
            return map;
        }

        public static Shop deserialize(ConfigurationSection section) {
            UUID owner = UUID.fromString(section.getString("owner"));
            String type = section.getString("type");
            Map<String, Object> priceMap = section.getConfigurationSection("prices").getValues(false);

            Shop shop = new Shop(owner, type);
            for (Map.Entry<String, Object> entry : priceMap.entrySet()) {
                ItemStack item = (ItemStack) entry.getKey();
                Double price = (Double) entry.getValue();
                shop.addItem(item, price);
            }

            return shop;
        }

    }

}
