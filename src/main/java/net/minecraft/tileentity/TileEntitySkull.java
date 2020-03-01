package net.minecraft.tileentity;

import CumServer.server.CumServer;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import net.minecraft.block.BlockSkull;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntitySkull extends TileEntity implements ITickable
{
    private int skullType;
    public int skullRotation;
    private GameProfile playerProfile;
    private int dragonAnimatedTicks;
    private boolean dragonAnimated;
    private static PlayerProfileCache profileCache;
    private static MinecraftSessionService sessionService;
    // Spigot start
    public static final ExecutorService executor = Executors.newFixedThreadPool(3,
            new ThreadFactoryBuilder()
                    .setNameFormat("Head Conversion Thread - %1$d")
                    .build()
    );
    public static final LoadingCache<String, GameProfile> skinCache = CacheBuilder.newBuilder()
            .maximumSize( 5000 )
            .expireAfterAccess( 60, TimeUnit.MINUTES )
            .build( new CacheLoader<String, GameProfile>()
            {
                @Override
                public GameProfile load(String key) throws Exception
                {
                    final GameProfile[] profiles = new GameProfile[1];
                    ProfileLookupCallback gameProfileLookup = new ProfileLookupCallback() {

                        @Override
                        public void onProfileLookupSucceeded(GameProfile gp) {
                            profiles[0] = gp;
                        }

                        @Override
                        public void onProfileLookupFailed(GameProfile gp, Exception excptn) {
                            profiles[0] = gp;
                        }
                    };

                    if (! CumServer.disableUpdateGameProfile)
                        MinecraftServer.getServerInst().getGameProfileRepository().findProfilesByNames(new String[] { key }, Agent.MINECRAFT, gameProfileLookup);

                    GameProfile profile = profiles[ 0 ];
                    if (profile == null) {
                        UUID uuid = EntityPlayer.getUUID(new GameProfile(null, key));
                        profile = new GameProfile(uuid, key);

                        gameProfileLookup.onProfileLookupSucceeded(profile);
                    } else
                    {

                        Property property = Iterables.getFirst( profile.getProperties().get( "textures" ), null );

                        if ( property == null )
                        {
                            profile = MinecraftServer.getServerInst().getMinecraftSessionService().fillProfileProperties( profile, true );
                        }
                    }


                    return profile;
                }
            } );
    // Spigot end

    public static void setProfileCache(PlayerProfileCache profileCacheIn)
    {
        profileCache = profileCacheIn;
    }

    public static void setSessionService(MinecraftSessionService sessionServiceIn)
    {
        sessionService = sessionServiceIn;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setByte("SkullType", (byte)(this.skullType & 255));
        compound.setByte("Rot", (byte)(this.skullRotation & 255));

        if (this.playerProfile != null)
        {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            NBTUtil.writeGameProfile(nbttagcompound, this.playerProfile);
            compound.setTag("Owner", nbttagcompound);
        }

        return compound;
    }

    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);
        this.skullType = compound.getByte("SkullType");
        this.skullRotation = compound.getByte("Rot");

        if (this.skullType == 3)
        {
            if (compound.hasKey("Owner", 10))
            {
                this.playerProfile = NBTUtil.readGameProfileFromNBT(compound.getCompoundTag("Owner"));
            }
            else if (compound.hasKey("ExtraType", 8))
            {
                String s = compound.getString("ExtraType");

                if (!StringUtils.isNullOrEmpty(s))
                {
                    this.playerProfile = new GameProfile((UUID)null, s);
                    this.updatePlayerProfile();
                }
            }
        }
    }

    public void update()
    {
        if (this.skullType == 5)
        {
            if (this.world.isBlockPowered(this.pos))
            {
                this.dragonAnimated = true;
                ++this.dragonAnimatedTicks;
            }
            else
            {
                this.dragonAnimated = false;
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public float getAnimationProgress(float p_184295_1_)
    {
        return this.dragonAnimated ? (float)this.dragonAnimatedTicks + p_184295_1_ : (float)this.dragonAnimatedTicks;
    }

    @Nullable
    public GameProfile getPlayerProfile()
    {
        return this.playerProfile;
    }

    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket()
    {
        return new SPacketUpdateTileEntity(this.pos, 4, this.getUpdateTag());
    }

    public NBTTagCompound getUpdateTag()
    {
        return this.writeToNBT(new NBTTagCompound());
    }

    public void setType(int type)
    {
        this.skullType = type;
        this.playerProfile = null;
    }

    public void setPlayerProfile(@Nullable GameProfile playerProfile)
    {
        this.skullType = 3;
        this.playerProfile = playerProfile;
        this.updatePlayerProfile();
    }

    private void updatePlayerProfile()
    {
        // Spigot start
        GameProfile profile = this.playerProfile;
        setType( 0 ); // Work around client bug
        updateGameprofile(profile, new Predicate<GameProfile>() {

            @Override
            public boolean apply(GameProfile input) {
                setType(3); // Work around client bug
                playerProfile = input;
                markDirty();
                if (world != null) {
                    world.notifyLightSet(pos); // PAIL: notify
                }
                return false;
            }
        }, false); 
        // Spigot end

    }

    // Spigot start - Support async lookups
    public static Future<GameProfile> updateGameprofile(final GameProfile input, final Predicate<GameProfile> callback, boolean sync)
    {
        if (input != null && !StringUtils.isNullOrEmpty(input.getName()))
        {
            if (input.isComplete() && input.getProperties().containsKey("textures"))
            {
                callback.apply(input);
            } else if (MinecraftServer.getServerInst() == null) {
                callback.apply(input);
            } else {
                GameProfile profile = skinCache.getIfPresent(input.getName().toLowerCase(java.util.Locale.ROOT));
                if (profile != null && Iterables.getFirst(profile.getProperties().get("textures"), (Object) null) != null) {
                    callback.apply(profile);

                    return Futures.immediateFuture(profile);
                }
                else
                {
                    Callable<GameProfile> callable = new Callable<GameProfile>() {
                        @Override
                        public GameProfile call() {
                            final GameProfile profile = skinCache.getUnchecked(input.getName().toLowerCase(java.util.Locale.ROOT));
                            MinecraftServer.getServerInst().processQueue.add(new Runnable() {
                                @Override
                                public void run() {
                                    if (profile == null) {
                                        callback.apply(input);
                                    } else {
                                        callback.apply(profile);
                                    }
                                }
                            });
                            return profile;
                        }
                    };
                    if (sync) {
                        try {
                            return Futures.immediateFuture(callable.call());
                        } catch (Exception ex) {
                            com.google.common.base.Throwables.throwIfUnchecked(ex);
                            throw new RuntimeException(ex); // Not possible
                        }
                    } else {
                        return executor.submit(callable);
                    }

                }
            }
        }
        else
        {
            callback.apply(input);
        }

        return Futures.immediateFuture(input);
    }
    // Spigot end

    public static GameProfile updateGameprofile(GameProfile input)
    {
        return com.google.common.util.concurrent.Futures.getUnchecked(updateGameprofile(input, com.google.common.base.Predicates.alwaysTrue(), true)); // CumServer
    }

    public int getSkullType()
    {
        return this.skullType;
    }

    @SideOnly(Side.CLIENT)
    public int getSkullRotation()
    {
        return this.skullRotation;
    }

    public void setSkullRotation(int rotation)
    {
        this.skullRotation = rotation;
    }

    public void mirror(Mirror mirrorIn)
    {
        if (this.world != null && this.world.getBlockState(this.getPos()).getValue(BlockSkull.FACING) == EnumFacing.UP)
        {
            this.skullRotation = mirrorIn.mirrorRotation(this.skullRotation, 16);
        }
    }

    public void rotate(Rotation rotationIn)
    {
        if (this.world != null && this.world.getBlockState(this.getPos()).getValue(BlockSkull.FACING) == EnumFacing.UP)
        {
            this.skullRotation = rotationIn.rotate(this.skullRotation, 16);
        }
    }
}