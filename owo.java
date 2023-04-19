public class Shop {

    private final Block block;
    private final ItemStack item;
    private final double price;
    private final boolean isBuying;

    public Shop(Block block, ItemStack item, double price, boolean isBuying) {
        this.block = block;
        this.item = item;
        this.price = price;
        this.isBuying = isBuying;
    }

    public Block getBlock() {
        return block;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public boolean isBuying() {
        return isBuying;
    }
}
