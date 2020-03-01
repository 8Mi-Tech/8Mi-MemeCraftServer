package net.minecraft.world;

import com.google.common.collect.Sets;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import org.bukkit.event.entity.CreatureSpawnEvent;

public final class WorldEntitySpawner
{
    private static final int MOB_COUNT_DIV = (int)Math.pow(17.0D, 2.0D);
    private final Set<ChunkPos> eligibleChunksForSpawning = Sets.<ChunkPos>newHashSet();

    // Spigot start - get entity count only from chunks being processed in b
    private int getEntityCount(WorldServer server, Class oClass)
    {
        int i = 0;
        Iterator<ChunkPos> it = this.eligibleChunksForSpawning.iterator();
        while ( it.hasNext() )
        {
            ChunkPos coord = it.next();
            int x = coord.x;
            int z = coord.z;
            if ( !((ChunkProviderServer)server.chunkProvider).droppedChunksSet.contains( coord ) && server.isChunkLoaded( x, z, true ) )
            {
                i += Objects.requireNonNull(server.getChunkProvider().getLoadedChunk(x, z)).entityCount.get( oClass );
            }
        }
        return i;
    }
    // Spigot end

    public int findChunksForSpawning(WorldServer worldServerIn, boolean spawnHostileMobs, boolean spawnPeacefulMobs, boolean spawnOnSetTickRate)
    {
        if (!spawnHostileMobs && !spawnPeacefulMobs)
        {
            return 0;
        }
        else
        {
            this.eligibleChunksForSpawning.clear();
            int i = 0;

            for (EntityPlayer entityplayer : worldServerIn.playerEntities)
            {
                if (!entityplayer.isSpectator())
                {
                    int j = MathHelper.floor(entityplayer.posX / 16.0D);
                    int k = MathHelper.floor(entityplayer.posZ / 16.0D);
                    int l = 8;

                    // Spigot Start
                    byte b0 = worldServerIn.spigotConfig.mobSpawnRange;
                    b0 = ( b0 > worldServerIn.spigotConfig.viewDistance ) ? (byte) worldServerIn.spigotConfig.viewDistance : b0;
                    b0 = ( b0 > 8 ) ? 8 : b0;

                    for (int i1 = -b0; i1 <= b0; ++i1) {
                        for (int j1 = -b0; j1 <= b0; ++j1) {
                            boolean flag = i1 == -b0 || i1 == b0 || j1 == -b0 || j1 == b0;
                            // Spigot End
                            ChunkPos chunkpos = new ChunkPos(i1 + j, j1 + k);

                            if (!this.eligibleChunksForSpawning.contains(chunkpos))
                            {
                                ++i;

                                if (!flag && worldServerIn.getWorldBorder().contains(chunkpos))
                                {
                                    PlayerChunkMapEntry playerchunkmapentry = worldServerIn.getPlayerChunkMap().getEntry(chunkpos.x, chunkpos.z);

                                    if (playerchunkmapentry != null && playerchunkmapentry.isSentToPlayers())
                                    {
                                        this.eligibleChunksForSpawning.add(chunkpos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            int j4 = 0;
            BlockPos blockpos1 = worldServerIn.getSpawnPoint();

            for (EnumCreatureType enumcreaturetype : EnumCreatureType.values())
            {
                // CraftBukkit start - Use per-world spawn limits
                int limit = enumcreaturetype.getMaxNumberOfCreature();
                switch (enumcreaturetype) {
                    case MONSTER:
                        limit = worldServerIn.getWorld().getMonsterSpawnLimit();
                        break;
                    case CREATURE:
                        limit = worldServerIn.getWorld().getAnimalSpawnLimit();
                        break;
                    case WATER_CREATURE:
                        limit = worldServerIn.getWorld().getWaterAnimalSpawnLimit();
                        break;
                    case AMBIENT:
                        limit = worldServerIn.getWorld().getAmbientSpawnLimit();
                        break;
                }

                if (limit == 0) {
                    continue;
                }
                int mobcnt = 0; // Spigot
                // CraftBukkit end
                if ((!enumcreaturetype.getPeacefulCreature() || spawnPeacefulMobs) && (enumcreaturetype.getPeacefulCreature() || spawnHostileMobs) && (!enumcreaturetype.getAnimal() || spawnOnSetTickRate))
                {
                    int k4 = worldServerIn.countEntities(enumcreaturetype.getCreatureClass());
                    int l4 = limit * i / MOB_COUNT_DIV; // CraftBukkit - use per-world limits

                    if ((mobcnt = getEntityCount(worldServerIn, enumcreaturetype.getCreatureClass())) <= limit * i / 256)
                    {
                        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
                        Iterator<ChunkPos> iterator = this.eligibleChunksForSpawning.iterator();
                        int moblimit = (limit * i / 256) - mobcnt + 1; // Spigot - up to 1 more than limit
                        label134:

                        while(iterator.hasNext() && (moblimit >0)) // Spigot - while more allowed
                        {
                            ChunkPos chunkpos1 = iterator.next();
                            BlockPos blockpos = getRandomChunkPosition(worldServerIn, chunkpos1.x, chunkpos1.z);
                            int k1 = blockpos.getX();
                            int l1 = blockpos.getY();
                            int i2 = blockpos.getZ();
                            IBlockState iblockstate = worldServerIn.getBlockState(blockpos);

                            if (!iblockstate.isNormalCube())
                            {
                                int j2 = 0;

                                for (int k2 = 0; k2 < 3; ++k2)
                                {
                                    int l2 = k1;
                                    int i3 = l1;
                                    int j3 = i2;
                                    int k3 = 6;
                                    Biome.SpawnListEntry biome$spawnlistentry = null;
                                    IEntityLivingData ientitylivingdata = null;
                                    int l3 = MathHelper.ceil(Math.random() * 4.0D);

                                    for (int i4 = 0; i4 < l3; ++i4)
                                    {
                                        l2 += worldServerIn.rand.nextInt(6) - worldServerIn.rand.nextInt(6);
                                        i3 += worldServerIn.rand.nextInt(1) - worldServerIn.rand.nextInt(1);
                                        j3 += worldServerIn.rand.nextInt(6) - worldServerIn.rand.nextInt(6);
                                        blockpos$mutableblockpos.setPos(l2, i3, j3);
                                        float f = (float)l2 + 0.5F;
                                        float f1 = (float)j3 + 0.5F;

                                        if (!worldServerIn.isAnyPlayerWithinRangeAt((double)f, (double)i3, (double)f1, 24.0D) && blockpos1.distanceSq((double)f, (double)i3, (double)f1) >= 576.0D)
                                        {
                                            if (biome$spawnlistentry == null)
                                            {
                                                biome$spawnlistentry = worldServerIn.getSpawnListEntryForTypeAt(enumcreaturetype, blockpos$mutableblockpos);

                                                if (biome$spawnlistentry == null)
                                                {
                                                    break;
                                                }
                                            }

                                            if (worldServerIn.canCreatureTypeSpawnHere(enumcreaturetype, biome$spawnlistentry, blockpos$mutableblockpos) && canCreatureTypeSpawnAtLocation(EntitySpawnPlacementRegistry.getPlacementForEntity(biome$spawnlistentry.entityClass), worldServerIn, blockpos$mutableblockpos))
                                            {
                                                EntityLiving entityliving;

                                                try
                                                {
                                                    entityliving = biome$spawnlistentry.newInstance(worldServerIn);
                                                }
                                                catch (Exception exception)
                                                {
                                                    exception.printStackTrace();
                                                    return j4;
                                                }

                                                entityliving.setLocationAndAngles((double)f, (double)i3, (double)f1, worldServerIn.rand.nextFloat() * 360.0F, 0.0F);

                                                net.minecraftforge.fml.common.eventhandler.Event.Result canSpawn = net.minecraftforge.event.ForgeEventFactory.canEntitySpawn(entityliving, worldServerIn, f, i3, f1, false);
                                                if (canSpawn == net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW || (canSpawn == net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT && (entityliving.getCanSpawnHere() && entityliving.isNotColliding())))
                                                {
                                                    if (!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entityliving, worldServerIn, f, i3, f1))
                                                    ientitylivingdata = entityliving.onInitialSpawn(worldServerIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);

                                                    if (entityliving.isNotColliding())
                                                    {
                                                        if (worldServerIn.addEntity(entityliving, CreatureSpawnEvent.SpawnReason.NATURAL)) {
                                                            ++j2;
                                                            moblimit--; // Spigot
                                                        }
                                                    }
                                                    else
                                                    {
                                                        entityliving.setDead();
                                                    }

                                                    if (moblimit <= 0 || j2 >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(entityliving)) // If we're past limit, stop spawn
                                                    {
                                                        continue label134;
                                                    }
                                                }

                                                j4 += j2;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return j4;
        }
    }

    private static BlockPos getRandomChunkPosition(World worldIn, int x, int z)
    {
        Chunk chunk = worldIn.getChunkFromChunkCoords(x, z);
        int i = x * 16 + worldIn.rand.nextInt(16);
        int j = z * 16 + worldIn.rand.nextInt(16);
        int k = MathHelper.roundUp(chunk.getHeight(new BlockPos(i, 0, j)) + 1, 16);
        int l = worldIn.rand.nextInt(k > 0 ? k : chunk.getTopFilledSegment() + 16 - 1);
        return new BlockPos(i, l, j);
    }

    public static boolean isValidEmptySpawnBlock(IBlockState state)
    {
        if (state.isBlockNormalCube())
        {
            return false;
        }
        else if (state.canProvidePower())
        {
            return false;
        }
        else if (state.getMaterial().isLiquid())
        {
            return false;
        }
        else
        {
            return !BlockRailBase.isRailBlock(state);
        }
    }

    public static boolean canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType spawnPlacementTypeIn, World worldIn, BlockPos pos)
    {
        if (!worldIn.getWorldBorder().contains(pos))
        {
            return false;
        }
        else
        {
            return spawnPlacementTypeIn.canSpawnAt(worldIn, pos);
        }
    }

    public static boolean canCreatureTypeSpawnBody(EntityLiving.SpawnPlacementType spawnPlacementTypeIn, World worldIn, BlockPos pos)
    {
        {

            IBlockState iblockstate = worldIn.getBlockState(pos);

            if (spawnPlacementTypeIn == EntityLiving.SpawnPlacementType.IN_WATER)
            {
                return iblockstate.getMaterial() == Material.WATER && worldIn.getBlockState(pos.down()).getMaterial() == Material.WATER && !worldIn.getBlockState(pos.up()).isNormalCube();
            }
            else
            {
                BlockPos blockpos = pos.down();
                IBlockState state = worldIn.getBlockState(blockpos);

                if (!state.getBlock().canCreatureSpawn(state, worldIn, blockpos, spawnPlacementTypeIn))
                {
                    return false;
                }
                else
                {
                    Block block = worldIn.getBlockState(blockpos).getBlock();
                    boolean flag = block != Blocks.BEDROCK && block != Blocks.BARRIER;
                    return flag && isValidEmptySpawnBlock(iblockstate) && isValidEmptySpawnBlock(worldIn.getBlockState(pos.up()));
                }
            }
        }
    }

    public static void performWorldGenSpawning(World worldIn, Biome biomeIn, int centerX, int centerZ, int diameterX, int diameterZ, Random randomIn)
    {
        List<Biome.SpawnListEntry> list = biomeIn.getSpawnableList(EnumCreatureType.CREATURE);

        if (!list.isEmpty())
        {
            while (randomIn.nextFloat() < biomeIn.getSpawningChance())
            {
                Biome.SpawnListEntry biome$spawnlistentry = (Biome.SpawnListEntry)WeightedRandom.getRandomItem(worldIn.rand, list);
                int i = biome$spawnlistentry.minGroupCount + randomIn.nextInt(1 + biome$spawnlistentry.maxGroupCount - biome$spawnlistentry.minGroupCount);
                IEntityLivingData ientitylivingdata = null;
                int j = centerX + randomIn.nextInt(diameterX);
                int k = centerZ + randomIn.nextInt(diameterZ);
                int l = j;
                int i1 = k;

                for (int j1 = 0; j1 < i; ++j1)
                {
                    boolean flag = false;

                    for (int k1 = 0; !flag && k1 < 4; ++k1)
                    {
                        BlockPos blockpos = worldIn.getTopSolidOrLiquidBlock(new BlockPos(j, 0, k));

                        if (canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, worldIn, blockpos))
                        {
                            EntityLiving entityliving;

                            try
                            {
                                entityliving = biome$spawnlistentry.newInstance(worldIn);
                            }
                            catch (Exception exception)
                            {
                                exception.printStackTrace();
                                continue;
                            }

                            if (net.minecraftforge.event.ForgeEventFactory.canEntitySpawn(entityliving, worldIn, j + 0.5f, (float) blockpos.getY(), k +0.5f, false) == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) continue;
                            entityliving.setLocationAndAngles((double)((float)j + 0.5F), (double)blockpos.getY(), (double)((float)k + 0.5F), randomIn.nextFloat() * 360.0F, 0.0F);
                            // CraftBukkit start - Added a reason for spawning this creature, moved entityliving.onInitialSpawn(ientitylivingdata) up
                            // worldIn.spawnEntity(entityliving);
                            ientitylivingdata = entityliving.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(entityliving)), ientitylivingdata);
                            worldIn.addEntity(entityliving, CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
                            // CraftBukkit end
                            flag = true;
                        }

                        j += randomIn.nextInt(5) - randomIn.nextInt(5);

                        for (k += randomIn.nextInt(5) - randomIn.nextInt(5); j < centerX || j >= centerX + diameterX || k < centerZ || k >= centerZ + diameterX; k = i1 + randomIn.nextInt(5) - randomIn.nextInt(5))
                        {
                            j = l + randomIn.nextInt(5) - randomIn.nextInt(5);
                        }
                    }
                }
            }
        }
    }
}