package com.boydti.far.v183.blocks;

import com.boydti.far.v183.QueueManager183;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockRedstoneTorch;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.CreativeModeTab;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.IBlockAccess;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.PluginManager;

public class Torch extends BlockRedstoneTorch {
    private static Map<World, List<RedstoneUpdateInfo>> b = new WeakHashMap();
    private final boolean isOn;
    private final QueueManager183 provider;

    private boolean a(World world, BlockPosition blockposition, boolean flag) {
        if(!b.containsKey(world)) {
            b.put(world, Lists.newArrayList());
        }
        List list = (List)b.get(world);
        if(flag) {
            list.add(new Torch.RedstoneUpdateInfo(blockposition, world.getTime()));
        }
        int i = 0;
        for(int j = 0; j < list.size(); ++j) {
            Torch.RedstoneUpdateInfo blockredstonetorch_redstoneupdateinfo = (RedstoneUpdateInfo)list.get(j);
            if(blockredstonetorch_redstoneupdateinfo.a.equals(blockposition)) {
                ++i;
                if(i >= 8) {
                    return true;
                }
            }
        }
        return false;
    }

    public Torch(QueueManager183 provider, boolean flag) {
        super(flag);
        this.isOn = flag;
        this.provider = provider;
        a(true);
        a((CreativeModeTab) null);
    }

    public int a(World world) {
        return 2;
    }

    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if(this.isOn) {
            EnumDirection[] aenumdirection = EnumDirection.values();
            int i = aenumdirection.length;
            for(int j = 0; j < i; ++j) {
                EnumDirection enumdirection = aenumdirection[j];

                world.applyPhysics(blockposition.shift(enumdirection), this);
            }
        }

    }

    @Override
    public void remove(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if(this.isOn) {
            world.applyPhysics(blockposition.shift(EnumDirection.DOWN), this);
            world.applyPhysics(blockposition.shift(EnumDirection.UP), this);
            world.applyPhysics(blockposition.shift(EnumDirection.WEST), this);
            world.applyPhysics(blockposition.shift(EnumDirection.EAST), this);
            world.applyPhysics(blockposition.shift(EnumDirection.SOUTH), this);
            world.applyPhysics(blockposition.shift(EnumDirection.NORTH), this);
        }

    }

    @Override
    public int a(IBlockAccess iblockaccess, BlockPosition blockposition, IBlockData iblockdata, EnumDirection enumdirection) {
        return this.isOn && iblockdata.get(FACING) != enumdirection?15:0;
    }

    private boolean g(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = ((EnumDirection)iblockdata.get(FACING)).opposite();
        return world.isBlockFacePowered(blockposition.shift(enumdirection), enumdirection);
    }

    @Override
    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
    }

    @Override
    public void b(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        boolean flag = this.g(world, blockposition, iblockdata);
        List list = (List)b.get(world);
        while(list != null && !list.isEmpty() && world.getTime() - ((RedstoneUpdateInfo)list.get(0)).b > 60L) {
            list.remove(0);
        }
        PluginManager manager = world.getServer().getPluginManager();
        Block block = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
        int oldCurrent = this.isOn?15:0;

        BlockRedstoneEvent event = new BlockRedstoneEvent(block, oldCurrent, oldCurrent);
        if(this.isOn) {
            if(flag) {
                if(oldCurrent != 0) {
                    event.setNewCurrent(0);
                    manager.callEvent(event);
                    if(event.getNewCurrent() != 0) {
                        return;
                    }
                }
                provider.updateBlockFast(world, blockposition, Blocks.UNLIT_REDSTONE_TORCH.getBlockData().set(FACING, iblockdata.get(FACING)), Torch.this, true, 3);
                if(this.a(world, blockposition, true)) {
                    world.makeSound((double)((float)blockposition.getX() + 0.5F), (double)((float)blockposition.getY() + 0.5F), (double)((float)blockposition.getZ() + 0.5F), "random.fizz", 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

                    for(int i = 0; i < 5; ++i) {
                        double d0 = (double)blockposition.getX() + random.nextDouble() * 0.6D + 0.2D;
                        double d1 = (double)blockposition.getY() + random.nextDouble() * 0.6D + 0.2D;
                        double d2 = (double)blockposition.getZ() + random.nextDouble() * 0.6D + 0.2D;
                        world.addParticle(EnumParticle.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D, new int[0]);
                    }

                    world.a(blockposition, world.getType(blockposition).getBlock(), 160);
                }
            }
        } else if(!flag && !this.a(world, blockposition, false)) {
            if(oldCurrent != 15) {
                event.setNewCurrent(15);
                manager.callEvent(event);
                if(event.getNewCurrent() != 15) {
                    return;
                }
            }
            provider.updateBlockFast(world, blockposition, Blocks.REDSTONE_TORCH.getBlockData().set(FACING, iblockdata.get(FACING)), Torch.this, true, 3);
        }

    }

    @Override
    public void doPhysics(World world, BlockPosition blockposition, IBlockData iblockdata, net.minecraft.server.v1_8_R3.Block block) {
        if(!this.e(world, blockposition, iblockdata) && this.isOn == this.g(world, blockposition, iblockdata)) {
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
