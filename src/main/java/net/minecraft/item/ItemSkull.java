package net.minecraft.item;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;

public class ItemSkull extends Item
{
    private static final String[] SKULL_TYPES = new String[] {"skeleton", "wither", "zombie", "char", "creeper", "dragon"};

    public ItemSkull()
    {
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        if (facing == EnumFacing.DOWN)
        {
            return EnumActionResult.FAIL;
        }
        else
        {
            if (worldIn.getBlockState(pos).getBlock().isReplaceable(worldIn, pos))
            {
                facing = EnumFacing.UP;
                pos = pos.down();
            }
            IBlockState iblockstate = worldIn.getBlockState(pos);
            Block block = iblockstate.getBlock();
            boolean flag = block.isReplaceable(worldIn, pos);

            if (!flag)
            {
                if (!worldIn.getBlockState(pos).getMaterial().isSolid() && !worldIn.isSideSolid(pos, facing, true))
                {
                    return EnumActionResult.FAIL;
                }

                pos = pos.offset(facing);
            }

            ItemStack itemstack = player.getHeldItem(hand);

            if (player.canPlayerEdit(pos, facing, itemstack) && Blocks.SKULL.canPlaceBlockAt(worldIn, pos))
            {
                if (worldIn.isRemote)
                {
                    return EnumActionResult.SUCCESS;
                }
                else
                {
                    worldIn.setBlockState(pos, Blocks.SKULL.getDefaultState().withProperty(BlockSkull.FACING, facing), 11);
                    int i = 0;

                    if (facing == EnumFacing.UP)
                    {
                        i = MathHelper.floor((double)(player.rotationYaw * 16.0F / 360.0F) + 0.5D) & 15;
                    }

                    TileEntity tileentity = worldIn.getTileEntity(pos);

                    if (tileentity instanceof TileEntitySkull)
                    {
                        TileEntitySkull tileentityskull = (TileEntitySkull)tileentity;

                        if (itemstack.getMetadata() == 3)
                        {
                            GameProfile gameprofile = null;

                            if (itemstack.hasTagCompound())
                            {
                                NBTTagCompound nbttagcompound = itemstack.getTagCompound();

                                if (nbttagcompound.hasKey("SkullOwner", 10))
                                {
                                    gameprofile = NBTUtil.readGameProfileFromNBT(nbttagcompound.getCompoundTag("SkullOwner"));
                                }
                                else if (nbttagcompound.hasKey("SkullOwner", 8) && !StringUtils.isBlank(nbttagcompound.getString("SkullOwner")))
                                {
                                    gameprofile = new GameProfile((UUID)null, nbttagcompound.getString("SkullOwner"));
                                }
                            }

                            tileentityskull.setPlayerProfile(gameprofile);
                        }
                        else
                        {
                            tileentityskull.setType(itemstack.getMetadata());
                        }

                        tileentityskull.setSkullRotation(i);
                        Blocks.SKULL.checkWitherSpawn(worldIn, pos, tileentityskull);
                    }

                    if (player instanceof EntityPlayerMP)
                    {
                        CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP)player, pos, itemstack);
                    }

                    itemstack.shrink(1);
                    return EnumActionResult.SUCCESS;
                }
            }
            else
            {
                return EnumActionResult.FAIL;
            }
        }
    }

    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
    {
        if (this.isInCreativeTab(tab))
        {
            for (int i = 0; i < SKULL_TYPES.length; ++i)
            {
                items.add(new ItemStack(this, 1, i));
            }
        }
    }

    public int getMetadata(int damage)
    {
        return damage;
    }

    public String getUnlocalizedName(ItemStack stack)
    {
        int i = stack.getMetadata();

        if (i < 0 || i >= SKULL_TYPES.length)
        {
            i = 0;
        }

        return super.getUnlocalizedName() + "." + SKULL_TYPES[i];
    }

    public String getItemStackDisplayName(ItemStack stack)
    {
        if (stack.getMetadata() == 3 && stack.hasTagCompound())
        {
            if (stack.getTagCompound().hasKey("SkullOwner", 8))
            {
                return I18n.translateToLocalFormatted("item.skull.player.name", stack.getTagCompound().getString("SkullOwner"));
            }

            if (stack.getTagCompound().hasKey("SkullOwner", 10))
            {
                NBTTagCompound nbttagcompound = stack.getTagCompound().getCompoundTag("SkullOwner");

                if (nbttagcompound.hasKey("Name", 8))
                {
                    return I18n.translateToLocalFormatted("item.skull.player.name", nbttagcompound.getString("Name"));
                }
            }
        }

        return super.getItemStackDisplayName(stack);
    }

    public boolean updateItemStackNBT(final NBTTagCompound nbt) // Spigot - make final
    {
        super.updateItemStackNBT(nbt);

        if (nbt.hasKey("SkullOwner", 8) && !StringUtils.isBlank(nbt.getString("SkullOwner")))
        {
            GameProfile gameprofile = new GameProfile((UUID)null, nbt.getString("SkullOwner"));
            // Spigot start
            TileEntitySkull.updateGameprofile(gameprofile, new com.google.common.base.Predicate<GameProfile>() {

                @Override
                public boolean apply(GameProfile gameprofile) {
                    nbt.setTag("SkullOwner", NBTUtil.writeGameProfile(new NBTTagCompound(), gameprofile));
                    return false;
                }
            }, false);
            // Spigot end
            return true;
        }
        else
        {
            NBTTagList textures = nbt.getCompoundTag("SkullOwner").getCompoundTag("Properties").getTagList("textures", 10); // Safe due to method contracts
            for (int i = 0; i < textures.tagCount(); i++) {
                if (textures.get(i) instanceof NBTTagCompound && !((NBTTagCompound) textures.get(i)).hasKey("Signature", 8) && ((NBTTagCompound) textures.get(i)).getString("Value").trim().isEmpty()) {
                    nbt.removeTag("SkullOwner");
                    break;
                }
            }
            return false;
        }
    }
}