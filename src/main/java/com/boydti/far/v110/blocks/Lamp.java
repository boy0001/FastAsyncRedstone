package com.boydti.far.v110.blocks;

import com.boydti.far.v110.QueueManager110;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.BlockRedstoneLamp;
import net.minecraft.server.v1_10_R1.Blocks;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.Item;
import net.minecraft.server.v1_10_R1.ItemStack;
import net.minecraft.server.v1_10_R1.World;
import org.bukkit.craftbukkit.v1_10_R1.event.CraftEventFactory;

public class Lamp extends BlockRedstoneLamp {
    
    private final boolean a;
    private final QueueManager110 provider;
    
    public Lamp(boolean flag, QueueManager110 provider) {
        super(flag);
        this.a = flag;
        this.provider = provider;
    }
    
    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if ((this.a) && (!world.isBlockIndirectlyPowered(blockposition))) {
            provider.updateBlockFast(world, blockposition, Blocks.REDSTONE_LAMP.getBlockData(), Lamp.this, true, 2);
        } else if ((!this.a) && (world.isBlockIndirectlyPowered(blockposition))) {
            if (CraftEventFactory.callRedstoneChange(world, blockposition.getX(), blockposition.getY(), blockposition.getZ(), 0, 15).getNewCurrent() != 15) {
                return;
            }
            provider.updateBlockFast(world, blockposition, Blocks.LIT_REDSTONE_LAMP.getBlockData(), Lamp.this, true, 2);
        }
    }
    
    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Block block) {
        if ((this.a) && (!world.isBlockIndirectlyPowered(blockposition))) {
            world.a(blockposition, this, 4);
        } else if ((!this.a) && (world.isBlockIndirectlyPowered(blockposition))) {
            if (CraftEventFactory.callRedstoneChange(world, blockposition.getX(), blockposition.getY(), blockposition.getZ(), 0, 15).getNewCurrent() != 15) {
                return;
            }
            provider.updateBlockFast(world, blockposition, Blocks.LIT_REDSTONE_LAMP.getBlockData(), Lamp.this, true, 2);
        }
    }
    
    @Override
    public void b(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        if ((this.a) && (!world.isBlockIndirectlyPowered(blockposition))) {
            if (CraftEventFactory.callRedstoneChange(world, blockposition.getX(), blockposition.getY(), blockposition.getZ(), 15, 0).getNewCurrent() != 0) {
                return;
            }
            provider.updateBlockFast(world, blockposition, Blocks.REDSTONE_LAMP.getBlockData(), Lamp.this, true, 2);
        }
    }
    
    @Override
    @Nullable
    public Item getDropType(IBlockData iblockdata, Random random, int i) {
        return Item.getItemOf(Blocks.REDSTONE_LAMP);
    }
    
    @Override
    public ItemStack a(World world, BlockPosition blockposition, IBlockData iblockdata) {
        return new ItemStack(Blocks.REDSTONE_LAMP);
    }
    
}
