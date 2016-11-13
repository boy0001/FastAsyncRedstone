package com.boydti.far.v183.blocks;

import com.boydti.far.v183.QueueManager183;
import java.util.Random;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.BlockRedstoneLamp;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.craftbukkit.v1_8_R3.event.CraftEventFactory;

public class Lamp extends BlockRedstoneLamp {
    private final boolean a;
    private final QueueManager183 provider;

    public Lamp(boolean flag, QueueManager183 provider) {
        super(flag);
        this.a = flag;
        this.provider = provider;

    }

    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if(!world.isClientSide) {
            if(this.a && !world.isBlockIndirectlyPowered(blockposition)) {
                provider.updateBlockFast(world, blockposition, Blocks.REDSTONE_LAMP.getBlockData(), Lamp.this, true, 2);
            } else if(!this.a && world.isBlockIndirectlyPowered(blockposition)) {
                if(CraftEventFactory.callRedstoneChange(world, blockposition.getX(), blockposition.getY(), blockposition.getZ(), 0, 15).getNewCurrent() != 15) {
                    return;
                }
                provider.updateBlockFast(world, blockposition, Blocks.LIT_REDSTONE_LAMP.getBlockData(), Lamp.this, true, 2);
                world.setTypeAndData(blockposition, Blocks.LIT_REDSTONE_LAMP.getBlockData(), 2);
            }
        }

    }

    public void doPhysics(World world, BlockPosition blockposition, IBlockData iblockdata, Block block) {
        if(!world.isClientSide) {
            if(this.a && !world.isBlockIndirectlyPowered(blockposition)) {
                world.a(blockposition, this, 4);
            } else if(!this.a && world.isBlockIndirectlyPowered(blockposition)) {
                if(CraftEventFactory.callRedstoneChange(world, blockposition.getX(), blockposition.getY(), blockposition.getZ(), 0, 15).getNewCurrent() != 15) {
                    return;
                }
                provider.updateBlockFast(world, blockposition, Blocks.LIT_REDSTONE_LAMP.getBlockData(), Lamp.this, true, 2);
            }
        }

    }

    public void b(World world, BlockPosition blockposition, IBlockData iblockdata, Random random) {
        if(!world.isClientSide && this.a && !world.isBlockIndirectlyPowered(blockposition)) {
            if(CraftEventFactory.callRedstoneChange(world, blockposition.getX(), blockposition.getY(), blockposition.getZ(), 15, 0).getNewCurrent() != 0) {
                return;
            }

            provider.updateBlockFast(world, blockposition, Blocks.REDSTONE_LAMP.getBlockData(), Lamp.this, true, 2);
        }

    }

    public Item getDropType(IBlockData iblockdata, Random random, int i) {
        return Item.getItemOf(Blocks.REDSTONE_LAMP);
    }

    protected ItemStack i(IBlockData iblockdata) {
        return new ItemStack(Blocks.REDSTONE_LAMP);
    }
}