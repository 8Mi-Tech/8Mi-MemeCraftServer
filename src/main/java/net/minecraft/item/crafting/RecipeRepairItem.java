package net.minecraft.item.crafting;

import com.google.common.collect.Lists;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class RecipeRepairItem extends ShapelessRecipes implements IRecipe
{
    // CraftBukkit start - Delegate to new parent class
    public RecipeRepairItem() {
        super("", new ItemStack(Items.LEATHER_HELMET), NonNullList.from(Ingredient.EMPTY, Ingredient.fromItem(Items.LEATHER_HELMET)));
    }
    // CraftBukkit end

    public boolean matches(InventoryCrafting inv, World worldIn)
    {
        List<ItemStack> list = Lists.<ItemStack>newArrayList();

        for (int i = 0; i < inv.getSizeInventory(); ++i)
        {
            ItemStack itemstack = inv.getStackInSlot(i);

            if (!itemstack.isEmpty())
            {
                list.add(itemstack);

                if (list.size() > 1)
                {
                    ItemStack itemstack1 = list.get(0);

                    if (itemstack.getItem() != itemstack1.getItem() || itemstack1.getCount() != 1 || itemstack.getCount() != 1 || !itemstack1.getItem().isRepairable())
                    {
                        return false;
                    }
                }
            }
        }

        return list.size() == 2;
    }

    public ItemStack getCraftingResult(InventoryCrafting inv)
    {
        List<ItemStack> list = Lists.<ItemStack>newArrayList();

        for (int i = 0; i < inv.getSizeInventory(); ++i)
        {
            ItemStack itemstack = inv.getStackInSlot(i);

            if (!itemstack.isEmpty())
            {
                list.add(itemstack);

                if (list.size() > 1)
                {
                    ItemStack itemstack1 = list.get(0);

                    if (itemstack.getItem() != itemstack1.getItem() || itemstack1.getCount() != 1 || itemstack.getCount() != 1 || !itemstack1.getItem().isRepairable())
                    {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (list.size() == 2)
        {
            ItemStack itemstack2 = list.get(0);
            ItemStack itemstack3 = list.get(1);

            if (itemstack2.getItem() == itemstack3.getItem() && itemstack2.getCount() == 1 && itemstack3.getCount() == 1 && itemstack2.getItem().isRepairable())
            {
                // FORGE: Make itemstack sensitive // Item item = itemstack2.getItem();
                int j = itemstack2.getMaxDamage() - itemstack2.getItemDamage();
                int k = itemstack2.getMaxDamage() - itemstack3.getItemDamage();
                int l = j + k + itemstack2.getMaxDamage() * 5 / 100;
                int i1 = itemstack2.getMaxDamage() - l;

                if (i1 < 0)
                {
                    i1 = 0;
                }

                // CraftBukkit start - Construct a dummy repair recipe
                ItemStack result = new ItemStack(itemstack3.getItem(), 1, i1);
                NonNullList<Ingredient> ingredients = NonNullList.create();
                ingredients.add(Ingredient.fromStacks(new ItemStack[]{itemstack2.copy()}));
                ingredients.add(Ingredient.fromStacks(new ItemStack[]{itemstack3.copy()}));
                ShapelessRecipes recipe = new ShapelessRecipes("", result.copy(), ingredients);
                recipe.key = new ResourceLocation("repairitem");
                inv.currentRecipe = recipe;
                if (inv.resultInventory != null && inv.eventHandler.getBukkitView() != null) // CumServer - mods bypass
                    result = org.bukkit.craftbukkit.event.CraftEventFactory.callPreCraftEvent(inv, result, inv.eventHandler.getBukkitView(), true);
                return result;
                // return new ItemStack(itemstack2.getItem(), 1, i1);
                // CraftBukkit end
            }
        }

        return ItemStack.EMPTY;
    }

    public ItemStack getRecipeOutput()
    {
        return ItemStack.EMPTY;
    }

    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv)
    {
        NonNullList<ItemStack> nonnulllist = NonNullList.<ItemStack>withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for (int i = 0; i < nonnulllist.size(); ++i)
        {
            ItemStack itemstack = inv.getStackInSlot(i);
            nonnulllist.set(i, net.minecraftforge.common.ForgeHooks.getContainerItem(itemstack));
        }

        return nonnulllist;
    }

    public boolean isDynamic()
    {
        return true;
    }

    public boolean canFit(int width, int height)
    {
        return width * height >= 2;
    }
}