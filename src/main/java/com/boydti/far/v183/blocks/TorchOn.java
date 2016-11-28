package com.boydti.far.v183.blocks;

import com.boydti.far.MutableBlockRedstoneEvent;
import com.boydti.far.v183.QueueManager183;
import java.util.Random;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockRedstoneTorch;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.CreativeModeTab;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.IBlockAccess;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.World;

public class TorchOn extends BlockRedstoneTorch {
    private final QueueManager183 provider;

    public TorchOn(QueueManager183 provider, boolean flag) {
        super(flag);
        this.provider = provider;
        a(true);
        a((CreativeModeTab) null);
    }

    public int a(World world) {
        return 2;
    }

    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection[] aenumdirection = EnumDirection.values();
        int i = aenumdirection.length;
        for(int j = 0; j < i; ++j) {
            EnumDirection enumdirection = aenumdirection[j];

            world.applyPhysics(blockposition.shift(enumdirection), this);
        }
    }

    @Override
    public void remove(World world, BlockPosition blockposition, IBlockData iblockdata) {
        world.applyPhysics(blockposition.shift(EnumDirection.DOWN), this);
        world.applyPhysics(blockposition.shift(EnumDirection.UP), this);
        world.applyPhysics(blockposition.shift(EnumDirection.WEST), this);
        world.applyPhysics(blockposition.shift(EnumDirection.EAST), this);
        world.applyPhysics(blockposition.shift(EnumDirection.SOUTH), this);
        world.applyPhysics(blockposition.shift(EnumDirection.NORTH), this);
    }

    @Override
    public int a(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, EnumDirection enumdirection) {
        return iblockdata.get(FACING) != enumdirection?15:0;
    }

    private boolean g(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = ((EnumDirection)iblockdata.get(FACING)).opposite();
        return world.isBlockFacePowered(blockposition.shift(enumdirection), enumdirection);
    }

    @Override
    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
    }

    @Override
    public void b(World world, BlockPosition pos, IBlockData iblockdata, Random random) {
        boolean flag = this.g(world, pos, iblockdata);
        if(flag) {
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
    public void doPhysics(World world, BlockPosition blockposition, IBlockData iblockdata, net.minecraft.server.v1_8_R3.Block block) {
        if(!this.e(world, blockposition, iblockdata) && this.g(world, blockposition, iblockdata)) {
            world.a(blockposition, this, this.a(world));
        }
    }

    @Override
    public int b(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, EnumDirection enumdirection) {
        return enumdirection == EnumDirection.DOWN?this.a(iblockaccess, blockposition, iblockdata, enumdirection):0;
    }

    @Override
    public Item getDropType(IBlockData iblockdata, Random random, int i) {
        return Item.getItemOf(Blocks.REDSTONE_TORCH);
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }

    @Override
    public boolean b(net.minecraft.server.v1_8_R3.Block block) {
        return block == Blocks.UNLIT_REDSTONE_TORCH || block == Blocks.REDSTONE_TORCH;
    }

    static class RedstoneUpdateInfo {
        BlockPosition a;
        long b;

        public RedstoneUpdateInfo(BlockPosition blockposition, long i) {
            this.a = blockposition;
            this.b = i;
        }
    }
}
