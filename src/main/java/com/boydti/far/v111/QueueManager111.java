package com.boydti.far.v111;

import com.boydti.far.QueueManager;
import com.boydti.far.RedstoneSettings;
import com.boydti.far.ReflectionUtil;
import com.boydti.far.v111.blocks.Lamp;
import com.boydti.far.v111.blocks.Piston;
import com.boydti.far.v111.blocks.TorchOff;
import com.boydti.far.v111.blocks.TorchOn;
import com.boydti.far.v111.blocks.Wire;
import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.Blocks;
import net.minecraft.server.v1_11_R1.ChunkSection;
import net.minecraft.server.v1_11_R1.IBlockData;
import net.minecraft.server.v1_11_R1.MinecraftKey;
import net.minecraft.server.v1_11_R1.World;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;

public class QueueManager111 extends QueueManager {
    private net.minecraft.server.v1_11_R1.Chunk cachedChunk;
    public final BlockPosition mutable = new BlockPosition.MutableBlockPosition();
    private ChunkSection cachedSection;
    private IBlockData air = Block.getByCombinedId(0);

    public QueueManager111() {
        if (RedstoneSettings.OPTIMIZE_DEVICES.REDSTONE_WIRE) {
            try {
                add(55, "redstone_wire", new Wire(this));
                ReflectionUtil.setStatic("REDSTONE_WIRE", Blocks.class, get("redstone_wire"));
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.REDSTONE_LAMP) {
            add(123, "redstone_lamp", new Lamp(false, this));
            ReflectionUtil.setStatic("REDSTONE_LAMP", Blocks.class, get("redstone_lamp"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.LIT_REDSTONE_LAMP) {
            add(124, "lit_redstone_lamp", new Lamp(true, this));
            ReflectionUtil.setStatic("LIT_REDSTONE_LAMP", Blocks.class, get("lit_redstone_lamp"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.STICKY_PISTON) {
            add(29, "sticky_piston", new Piston(true, this));
            ReflectionUtil.setStatic("STICKY_PISTON", Blocks.class, get("sticky_piston"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.PISTON) {
            add(33, "piston", new Piston(false, this));
            ReflectionUtil.setStatic("PISTON", Blocks.class, get("piston"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.UNLIT_REDSTONE_TORCH) {
            add(75, "unlit_redstone_torch", new TorchOff(this, false));
            ReflectionUtil.setStatic("UNLIT_REDSTONE_TORCH", Blocks.class, get("unlit_redstone_torch"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.REDSTONE_TORCH) {
            add(76, "redstone_torch", new TorchOn(this, true));
            ReflectionUtil.setStatic("REDSTONE_TORCH", Blocks.class, get("redstone_torch"));
        }
    }

    private static Block get(String s) {
        return Block.REGISTRY.get(new MinecraftKey(s));
    }

    private static void add(int index, String name, Block block) {
        Block.REGISTRY.a(index, new MinecraftKey(name), block);
        for (UnmodifiableIterator localUnmodifiableIterator = block.s().a().iterator(); localUnmodifiableIterator.hasNext();) {
            IBlockData iblockdata = (IBlockData) localUnmodifiableIterator.next();
            int k = Block.getId(block) << 4 | block.toLegacyData(iblockdata);
            Block.REGISTRY_ID.a(iblockdata, k);
        }
    }

    public final IBlockData getTypeFast(net.minecraft.server.v1_11_R1.World world, BlockPosition pos) {
        return getTypeFast(world, pos.getX(), pos.getY(), pos.getZ());
    }

    public final IBlockData getTypeFast(net.minecraft.server.v1_11_R1.World world, int x, int y, int z) {
        int x4 = x >> 4;
        int z4 = z >> 4;
        if (cachedChunk == null || cachedChunk.locX != x4 || cachedChunk.locZ != z4) {
            cachedChunk = world.getChunkAt(x4, z4);
            cachedSection = cachedChunk.getSections()[y >> 4];
        } else {
            int y4 = y >> 4;
            if (cachedSection == null || cachedSection.getYPosition() != y4 << 4) {
                cachedSection = cachedChunk.getSections()[y4];
            }
        }
        if (cachedSection == net.minecraft.server.v1_11_R1.Chunk.a) {
            return air;
        }
        return cachedSection.getType(x & 15, y & 15, z & 15);
    }

    public void updateBlockFast(World world, BlockPosition pos, IBlockData state, Block block, boolean light, int physics) {
        CraftWorld bukkitWorld = world.getWorld();
        int x = pos.getX();
        int z = pos.getZ();
        int y = pos.getY();
        net.minecraft.server.v1_11_R1.Chunk chunk = world.getChunkAtWorldCoords(pos);
        IBlockData previous = chunk.a(pos, state);
        int lightDistance = light ? Math.max(state.d(), previous == null ? 0 : previous.d()) : 0;
        queueUpdate(bukkitWorld, x, y, z, lightDistance);
        if (previous != null && physics != 0) {
            for (int yy = Math.max(0, y - 1); yy <= y + 1 && yy < 256; yy++) {
                for (int xx = x - 1; xx <= x + 1; xx++) {
                    for (int zz = z - 1; zz <= z + 1; zz++) {
                        world.a(mutable.a(xx, yy, zz), block, pos);
                    }
                }
            }
        }
    }
}
