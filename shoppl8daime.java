import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class ShopPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // プラグインが有効化されたときに呼び出される処理
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // プラグインが無効化されたときに呼び出される処理
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (action == Action.RIGHT_CLICK_BLOCK && block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            String[] lines = sign.getLines();

            if (lines[0].equalsIgnoreCase("[販売ショップ]") || lines[0].equalsIgnoreCase("[買取ショップ]")) {
                String shopType = lines[0].substring(1, 3);
                String itemName = lines[1];
                int price = Integer.parseInt(lines[2]);

                if (shopType.equals("販売")) {
                    // 商品を買う場合
                    ItemStack itemStack = new ItemStack(Material.getMaterial(itemName));
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.setDisplayName(itemName);
                    itemStack.setItemMeta(itemMeta);

                    if (player.getInventory().contains(itemStack)) {
                        // プレイヤーが商品を持っている場合
                        player.getInventory().removeItem(itemStack);
                        player.getInventory().addItem(new ItemStack(Material.getMaterial("金塊"), price));
                        player.sendMessage("§a§l[Shop]§r§a商品を" + price + "金塊で売りました。");
                    } else {
                        player.sendMessage("§c§l[Shop]§r§c商品がありません。");
                    }
                } else if (shopType.equals("買取")) {
                    // 商品を売る場合
                    ItemStack itemStack = new ItemStack(Material.getMaterial(itemName));
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    itemMeta.setDisplayName(itemName);
                    itemStack.setItemMeta(itemMeta);

                    if (player.getInventory().contains(new ItemStack(Material.getMaterial("金塊"), price))) {
                        // プレイヤーが金塊を持っている場合
                        player.getInventory().removeItem(new ItemStack(Material.getMaterial("金塊"), price));
                        player.getInventory().addItem(itemStack);
                        player.sendMessage("§a§l[Shop]§r§a商品を" + price + "金塊で買いました。");
                    } else {
                        player.sendMessage("§c§l[Shop]§r§c金塊がありません。");
}
}
}
}
}
public void createShop(Player player, String type, String itemName, int price) {
    // プレイヤーが持っているアイテムを取得
    ItemStack itemStack = player.getInventory().getItemInMainHand();

    if (itemStack.getType() != Material.AIR) {
        // 空気でないアイテムを持っている場合
        Block block = player.getTargetBlockExact(5);
        BlockState blockState = block.getState();

        if (blockState instanceof Sign) {
            // 看板を設置するブロックを取得
            Sign sign = (Sign) blockState;
            sign.setLine(0, "[" + type + "ショップ]");
            sign.setLine(1, itemName);
            sign.setLine(2, Integer.toString(price));
            sign.update();

            player.sendMessage("§a§l[Shop]§r§a看板を設置しました。");
        } else {
            player.sendMessage("§c§l[Shop]§r§c看板を設置するブロックがありません。");
        }
    } else {
        player.sendMessage("§c§l[Shop]§r§cアイテムを持っていません。");
    }
}

public void openShopInventory(Player player, String type, String itemName, int price) {
    Inventory inventory = Bukkit.createInventory(null, 27, "[" + type + "ショップ] " + itemName);

    ItemStack itemStack = new ItemStack(Material.getMaterial(itemName));
    ItemMeta itemMeta = itemStack.getItemMeta();
    itemMeta.setDisplayName(itemName);
    itemStack.setItemMeta(itemMeta);

    inventory.setItem(13, itemStack);

    ItemStack priceItemStack = new ItemStack(Material.getMaterial("金塊"), price);
    ItemMeta priceItemMeta = priceItemStack.getItemMeta();
    ArrayList<String> priceLore = new ArrayList<>();
    priceLore.add("§a§l価格:§r§a " + price + "金塊");
    priceItemMeta.setLore(priceLore);
    priceItemStack.setItemMeta(priceItemMeta);

    if (type.equals("販売")) {
        // 商品を買う場合
        ItemStack buyItemStack = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta buyItemMeta = buyItemStack.getItemMeta();
        buyItemMeta.setDisplayName("§a§l購入");
        buyItemStack.setItemMeta(buyItemMeta);

        inventory.setItem(11, buyItemStack);
        inventory.setItem(15, buyItemStack);
    } else if (type.equals("買取")) {
        // 商品を売る場合
        ItemStack sellItemStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta sellItemMeta = sellItemStack.getItemMeta();
        sellItemMeta.setDisplayName("§c§l売却");
        sellItemStack.setItemMeta(sellItemMeta);

        inventory.setItem(11, sellItemStack);
        inventory.setItem(15, sellItemStack);
    }

    inventory.setItem(18, priceItemStack);
    inventory.setItem(19, priceItemStack);
    inventory.setItem(20, priceItemStack);

    player.openInventory(inventory);
}
}
