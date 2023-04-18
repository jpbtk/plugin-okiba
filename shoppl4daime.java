public class ShopPlugin extends JavaPlugin implements Listener {
    
    @Override
    public void onEnable() {
        // プラグインの有効化処理
        getServer().getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        // プラグインの無効化処理
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            Player player = event.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            BlockState state = clickedBlock.getState();
            if (state instanceof Sign && itemInHand.getType() != Material.AIR) {
                Sign sign = (Sign) state;
                if (sign.getLine(0).equals("[販売ショップ]")) {
                    // 販売ショップの場合
                    String itemName = itemInHand.getItemMeta().getDisplayName();
                    double price = Double.parseDouble(sign.getLine(2));
                    ShopGUI gui = new ShopGUI(player, itemInHand, price, true);
                    gui.open();
                } else if (sign.getLine(0).equals("[買取ショップ]")) {
                    // 買取ショップの場合
                    String itemName = itemInHand.getItemMeta().getDisplayName();
                    double price = Double.parseDouble(sign.getLine(2));
                    ShopGUI gui = new ShopGUI(player, itemInHand, price, false);
                    gui.open();
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Inventory inventory = event.getClickedInventory();
            ItemStack clickedItem = event.getCurrentItem();
            if (inventory.getHolder() instanceof ShopGUI) {
                event.setCancelled(true);
                ShopGUI gui = (ShopGUI) inventory.getHolder();
                if (gui.isBuying()) {
                    // アイテムを買う処理を実装
                } else {
                    // アイテムを売る処理を実装
                }
                gui.close();
            }
        }
    }

    // コマンドの処理
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shop")
                && args.length == 3
                && (args[0].equalsIgnoreCase("create")
                    && (args[1].equalsIgnoreCase("buy") || args[1].equalsIgnoreCase("sell")))) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() == Material.AIR) {
                    player.sendMessage("手にアイテムを持っていません。");
                    return true;
                }
                double price = 0.0;
                try {
                    price = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage("値段が正しくありません。");
                    return true;
                }
                Block signBlock = player.getTargetBlockExact(5);
                if (signBlock == null) {
                    player.sendMessage("看板を設置するためのブロックがありません。");
                    return true;
                }
                BlockFace facing = BlockFace.UP;
                Block block = signBlock.getRelative(facing);
                if (block.getType() != Material.AIR) {
                    player.sendMessage("看板を設置するためのスペースがありません。");
                    return true;
                }
                block.setType(Material.OAK_WALL_SIGN);
                Sign sign = (Sign) block.getState();
                if (args[1].equalsIgnoreCase("buy")) {
                    sign.setLine(0, "[販売ショップ]");
                } else {
                    sign.setLine(0, "[買取ショップ]");
                }
                sign.setLine(1, itemInHand.getItemMeta().getDisplayName());
                sign.setLine(2, Double.toString(price));
                sign.update();
                player.sendMessage("看板を設置しました。");
            }
            return true;
        }
        return false;
    }
    
}
