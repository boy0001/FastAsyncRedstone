package com.boydti.far.v112.blocks;

import com.boydti.far.MutableBlockRedstoneEvent;
import com.boydti.far.v112.QueueManager112;

import java.util.Random;

import javax.annotation.Nullable;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.BlockRedstoneTorch;
import net.minecraft.server.v1_12_R1.Blocks;
import net.minecraft.server.v1_12_R1.CreativeModeTab;
import net.minecraft.server.v1_12_R1.EnumDirection;
import net.minecraft.server.v1_12_R1.IBlockAccess;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.Item;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.World;

public class TorchOn extends BlockRedstoneTorch {
    private final QueueManager112 provider;


    public TorchOn(QueueManager112 provider, boolean flag) {
        super(flag);
        this.provider = provider;
        a(true);
        a((CreativeModeTab) null);
    }

    @Override
    public int a(World world) {
        return 2;
    }

    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection[] aenumdirection = EnumDirection.values();
        int i = aenumdirection.length;
        for (int j = 0; j < i; j++) {
            EnumDirection enumdirection = aenumdirection[j];

            world.applyPhysics(blockposition.shift(enumdirection), this, false);
        }
    }

    @Override
    public void remove(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection[] aenumdirection = EnumDirection.values();
        int i = aenumdirection.length;
        for (int j = 0; j < i; j++) {
            EnumDirection enumdirection = aenumdirection[j];
            world.applyPhysics(blockposition.shift(enumdirection), this, false);
        }
    }

    @Override
    public int b(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return (iblockdata.get(FACING) != enumdirection) ? 15 : 0;
    }

    private boolean isFacePowered(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = iblockdata.get(FACING).opposite();
        return world.isBlockFacePowered(blockposition.shift(enumdirection), enumdirection);
    }

    @Override
    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {}

    @Override
    public void b(World world, BlockPosition pos, IBlockData iblockdata, Random random) {
        boolean isFacePowered = isFacePowered(world, pos, iblockdata);
        if (isFacePowered) {
            MutableBlockRedstoneEvent.INSTANCE.call(world.getWorld(), pos.getX(), pos.getY(), pos.getZ(), 15, 0);
            MutableBlockRedstoneEvent.INSTANCE.setNewCurrent(0);
            world.getServer().getPluginManager().callEvent(MutableBlockRedstoneEvent.INSTANCE);
            if (MutableBlockRedstoneEvent.INSTANCE.getNewCurrent() != 0) {
                return;
            }
            provider.updateBlockFast(world, pos, Blocks.UNLIT_REDSTONE_TORCH.getBlockData().set(FACING, iblockdata.get(FACING)), TorchOn.this, true, 3);
        }
    }

    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Block block, BlockPosition ignore) {
        if ((!e(world, blockposition, iblockdata)) && (isFacePowered(world, blockposition, iblockdata)) == true) {
            world.a(blockposition, this, a(world));
        }
    }

    @Override
    public int c(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return enumdirection == EnumDirection.DOWN ? iblockdata.a(iblockaccess, blockposition, enumdirection) : 0;
    }

    @Override
    @Nullable
    public Item getDropType(IBlockData iblockdata, Random random, int i) {
        return Item.getItemOf(Blocks.REDSTONE_TORCH);
    }

    @Override
    public boolean isPowerSource(IBlockData iblockdata) {
        return true;
    }

    @Override
    public ItemStack a(World world, BlockPosition blockposition, IBlockData iblockdata) {
        return new ItemStack(Blocks.REDSTONE_TORCH);
    }

    @Override
    public boolean d(Block block) {
        return (block == Blocks.UNLIT_REDSTONE_TORCH) || (block == Blocks.REDSTONE_TORCH);
    }

    static class RedstoneUpdateInfo {
        BlockPosition pos;
        long time;

        public RedstoneUpdateInfo(BlockPosition blockposition, long i) {
            this.pos = blockposition;
            this.time = i;
        }
    }
}
