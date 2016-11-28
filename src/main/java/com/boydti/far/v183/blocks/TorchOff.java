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

public class TorchOff extends BlockRedstoneTorch {
    private final QueueManager183 provider;

    public TorchOff(QueueManager183 provider, boolean flag) {
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
    }

    @Override
    public void remove(World world, BlockPosition blockposition, IBlockData iblockdata) {
    }

    @Override
    public int a(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, EnumDirection enumdirection) {
        return 0;
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
        if(!flag) {
            MutableBlockRedstoneEvent.INSTANCE.call(world.getWorld(), pos.getX(), pos.getY(), pos.getZ(), 0, 15);
            if (MutableBlockRedstoneEvent.INSTANCE.getNewCurrent() != 15) {
                return;
            }
            provider.updateBlockFast(world, pos, Blocks.REDSTONE_TORCH.getBlockData().set(FACING, iblockdata.get(FACING)), TorchOff.this, true, 3);
        }

    }

    @Override
    public void doPhysics(World world, BlockPosition blockposition, IBlockData iblockdata, net.minecraft.server.v1_8_R3.Block block) {
        if(!this.e(world, blockposition, iblockdata) && !this.g(world, blockposition, iblockdata)) {
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
