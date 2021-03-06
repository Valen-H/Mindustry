package io.anuke.mindustry.type;

public class ItemStack{
    public Item item;
    public int amount;

    public ItemStack(Item item, int amount){
        this.item = item;
        this.amount = amount;
    }

    public boolean equals(ItemStack other){
        return other != null && other.item == item && other.amount == amount;
    }

    public static ItemStack[] with(Object... items){
        ItemStack[] stacks = new ItemStack[items.length/2];
        for(int i = 0; i < items.length; i+= 2){
            stacks[i/2] = new ItemStack((Item)items[i], (Integer)items[i + 1]);
        }
        return stacks;
    }
}
