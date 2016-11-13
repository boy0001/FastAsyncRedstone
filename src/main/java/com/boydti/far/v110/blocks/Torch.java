package com.boydti.far.v110.blocks;

import com.boydti.far.v110.QueueManager110;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.BlockRedstoneTorch;
import net.minecraft.server.v1_10_R1.Blocks;
import net.minecraft.server.v1_10_R1.CreativeModeTab;
import net.minecraft.server.v1_10_R1.EnumDirection;
import net.minecraft.server.v1_10_R1.EnumParticle;
import net.minecraft.server.v1_10_R1.IBlockAccess;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.Item;
import net.minecraft.server.v1_10_R1.ItemStack;
import net.minecraft.server.v1_10_R1.SoundCategory;
import net.minecraft.server.v1_10_R1.SoundEffects;
import net.minecraft.server.v1_10_R1.World;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.PluginManager;

public class Torch extends BlockRedstoneTorch {
    private static final Map<World, List<RedstoneUpdateInfo>> g = new WeakHashMap();
    private final boolean isOn;
    private final QueueManager110 provider;
    
    private boolean a(World world, BlockPosition blockposition, boolean flag) {
        if (!g.containsKey(world)) {
            g.put(world, Lists.newArrayList());
        }
        List list = g.get(world);
        if (flag) {
            list.add(new RedstoneUpdateInfo(blockposition, world.getTime()));
        }
        int i = 0;
        for (int j = 0; j < list.size(); j++) {
            RedstoneUpdateInfo blockredstonetorch_redstoneupdateinfo = (RedstoneUpdateInfo) list.get(j);
            if (blockredstonetorch_redstoneupdateinfo.a.equals(blockposition)) {
                i++;
                if (i >= 8) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public Torch(QueueManager110 provider, boolean flag) {
        super(flag);
        this.isOn = flag;
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
        if (this.isOn) {
            EnumDirection[] aenumdirection = EnumDirection.values();
            int i = aenumdirection.length;
            for (int j = 0; j < i; j++) {
                EnumDirection enumdirection = aenumdirection[j];
                
                world.applyPhysics(blockposition.shift(enumdirection), this);
            }
        }
    }
    
    @Override
    public void remove(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if (this.isOn) {
            EnumDirection[] aenumdirection = EnumDirection.values();
            int i = aenumdirection.length;
            for (int j = 0; j < i; j++) {
                EnumDirection enumdirection = aenumdirection[j];
                
                world.applyPhysics(blockposition.shift(enumdirection), this);
            }
        }
    }
    
    @Override
    public int b(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        return (this.isOn) && (iblockdata.get(FACING) != enumdirection) ? 15 : 0;
    }

    private boolean g(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = iblockdata.get(FACING).opposite();
        
        return world.isBlockFacePowered(blockposition.shift(enumdirection), enumdirection);
    }
    
    @Override
    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {}
    
    @Override
    public void b(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        boolean flag = g(world, blockposition, iblockdata);
        List list = g.get(world);
        while ((list != null) && (!list.isEmpty()) && (world.getTime() - ((RedstoneUpdateInfo) list.get(0)).b > 60L)) {
            list.remove(0);
        }
        PluginManager manager = world.getServer().getPluginManager();
        org.bukkit.block.Block block = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
        int oldCurrent = this.isOn ? 15 : 0;
        
        BlockRedstoneEvent event = new BlockRedstoneEvent(block, oldCurrent, oldCurrent);
        if (this.isOn) {
            if (flag) {
                if (oldCurrent != 0) {
                    event.setNewCurrent(0);
                    manager.callEvent(event);
                    if (event.getNewCurrent() != 0) {
                        return;
                    }
                }

                provider.updateBlockFast(world, blockposition, Blocks.UNLIT_REDSTONE_TORCH.getBlockData().set(FACING, iblockdata.get(FACING)), Torch.this, true, 3);
                if (a(world, blockposition, true)) {
                    world.a(null, blockposition, SoundEffects.eR, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);

                    for (int i = 0; i < 5; i++) {
                        double d0 = blockposition.getX() + random.nextDouble() * 0.6D + 0.2D;
                        double d1 = blockposition.getY() + random.nextDouble() * 0.6D + 0.2D;
                        double d2 = blockposition.getZ() + random.nextDouble() * 0.6D + 0.2D;
                        world.addParticle(EnumParticle.SMOKE_NORMAL, d0, d1, d2, 0.0D, 0.0D, 0.0D, new int[0]);
                    }

                    world.a(blockposition, world.getType(blockposition).getBlock(), 160);
                }
            }
        } else if ((!flag) && (!a(world, blockposition, false))) {
            if (oldCurrent != 15) {
                event.setNewCurrent(15);
                manager.callEvent(event);
                if (event.getNewCurrent() != 15) {
                    return;
                }
            }
            provider.updateBlockFast(world, blockposition, Blocks.REDSTONE_TORCH.getBlockData().set(FACING, iblockdata.get(FACING)), Torch.this, true, 3);
        }
    }
    
    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Block block) {
        if ((!e(world, blockposition, iblockdata)) && (this.isOn == g(world, blockposition, iblockdata))) {
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
    public boolean b(Block block) {
        return (block == Blocks.UNLIT_REDSTONE_TORCH) || (block == Blocks.REDSTONE_TORCH);
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
