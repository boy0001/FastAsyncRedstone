package com.boydti.far.v183.blocks;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.boydti.far.v183.QueueManager183;
import com.google.common.collect.ImmutableList;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftBlock;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public class Piston extends BlockPiston {
    public static final BlockStateDirection FACING = BlockStateDirection.of("facing");
    public static final BlockStateBoolean EXTENDED = BlockStateBoolean.of("extended");
    private final boolean sticky;
    private final QueueManager183 provider;

    public Piston(boolean flag, QueueManager183 provider) {
        super(flag);
        this.j(this.blockStateList.getBlockData().set(FACING, EnumDirection.NORTH).set(EXTENDED, Boolean.valueOf(false)));
        this.sticky = flag;
        this.a(i);
        this.c(0.5F);
        this.a(CreativeModeTab.d);
        this.provider = provider;
    }

    public boolean c() {
        return false;
    }

    public void postPlace(World world, BlockPosition blockposition, IBlockData iblockdata, EntityLiving entityliving, ItemStack itemstack) {
        provider.updateBlockFast(world, blockposition, iblockdata.set(FACING, a(world, blockposition, entityliving)), Piston.this, true, 2);
        if(!world.isClientSide) {
            this.e(world, blockposition, iblockdata);
        }

    }

    public void doPhysics(World world, BlockPosition blockposition, IBlockData iblockdata, Block block) {
        if(!world.isClientSide) {
            this.e(world, blockposition, iblockdata);
        }

    }

    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if(!world.isClientSide && world.getTileEntity(blockposition) == null) {
            this.e(world, blockposition, iblockdata);
        }

    }

    public IBlockData getPlacedState(World world, BlockPosition blockposition, EnumDirection enumdirection, float f, float f1, float f2, int i, EntityLiving entityliving) {
        return this.getBlockData().set(FACING, a(world, blockposition, entityliving)).set(EXTENDED, Boolean.valueOf(false));
    }

    private void e(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = (EnumDirection)iblockdata.get(FACING);
        boolean flag = this.a(world, blockposition, enumdirection);
        if(flag && !((Boolean)iblockdata.get(EXTENDED)).booleanValue()) {
            if((new PistonExtendsChecker(world, blockposition, enumdirection, true)).a()) {
                world.playBlockAction(blockposition, this, 0, enumdirection.a());
            }
        } else if(!flag && ((Boolean)iblockdata.get(EXTENDED)).booleanValue()) {
            if(!this.sticky) {
                org.bukkit.block.Block block = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
                BlockPistonRetractEvent event = new BlockPistonRetractEvent(block, ImmutableList.of(), CraftBlock.notchToBlockFace(enumdirection));
                world.getServer().getPluginManager().callEvent(event);
                if(event.isCancelled()) {
                    return;
                }
            }
            provider.updateBlockFast(world, blockposition, iblockdata.set(EXTENDED, Boolean.valueOf(false)), Piston.this, true, 2);
            world.playBlockAction(blockposition, this, 1, enumdirection.a());
        }

    }

    private boolean a(World world, BlockPosition blockposition, EnumDirection enumdirection) {
        EnumDirection[] aenumdirection = EnumDirection.values();
        int i = aenumdirection.length;

        int j;
        for(j = 0; j < i; ++j) {
            EnumDirection blockposition1 = aenumdirection[j];
            if(blockposition1 != enumdirection && world.isBlockFacePowered(blockposition.shift(blockposition1), blockposition1)) {
                return true;
            }
        }

        if(world.isBlockFacePowered(blockposition, EnumDirection.DOWN)) {
            return true;
        } else {
            BlockPosition var11 = blockposition.up();
            EnumDirection[] aenumdirection1 = EnumDirection.values();
            j = aenumdirection1.length;

            for(int k = 0; k < j; ++k) {
                EnumDirection enumdirection2 = aenumdirection1[k];
                if(enumdirection2 != EnumDirection.DOWN && world.isBlockFacePowered(var11.shift(enumdirection2), enumdirection2)) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean a(World world, BlockPosition blockposition, IBlockData iblockdata, int i, int j) {
        EnumDirection enumdirection = (EnumDirection)iblockdata.get(FACING);
        if(!world.isClientSide) {
            boolean tileentity = this.a(world, blockposition, enumdirection);
            if(tileentity && i == 1) {
                provider.updateBlockFast(world, blockposition, iblockdata.set(EXTENDED, Boolean.valueOf(true)), Piston.this, true, 2);
                return false;
            }

            if(!tileentity && i == 0) {
                return false;
            }
        }

        if(i == 0) {
            if(!this.a(world, blockposition, enumdirection, true)) {
                return false;
            }

            world.setTypeAndData(blockposition, iblockdata.set(EXTENDED, Boolean.valueOf(true)), 2);
            world.makeSound((double)blockposition.getX() + 0.5D, (double)blockposition.getY() + 0.5D, (double)blockposition.getZ() + 0.5D, "tile.piston.out", 0.5F, world.random.nextFloat() * 0.25F + 0.6F);
        } else if(i == 1) {
            TileEntity tileentity1 = world.getTileEntity(blockposition.shift(enumdirection));
            if(tileentity1 instanceof TileEntityPiston) {
                ((TileEntityPiston)tileentity1).h();
            }

//            world.setTypeAndData(blockposition, Blocks.PISTON_EXTENSION.getBlockData().set(BlockPistonMoving.FACING, enumdirection).set(BlockPistonMoving.TYPE, this.sticky? BlockPistonExtension.EnumPistonType.STICKY: BlockPistonExtension.EnumPistonType.DEFAULT), 3);

            provider.updateBlockFast(
            world,
            blockposition,
            Blocks.PISTON_EXTENSION.getBlockData().set(BlockPistonMoving.FACING, enumdirection)
            .set(BlockPistonMoving.TYPE, this.sticky ? BlockPistonExtension.EnumPistonType.STICKY : BlockPistonExtension.EnumPistonType.DEFAULT), Piston.this, true, 3);
            world.setTileEntity(blockposition, BlockPistonMoving.a(this.fromLegacyData(j), enumdirection, false, true));
            if(this.sticky) {
                BlockPosition blockposition1 = blockposition.a(enumdirection.getAdjacentX() * 2, enumdirection.getAdjacentY() * 2, enumdirection.getAdjacentZ() * 2);
                Block block = world.getType(blockposition1).getBlock();
                boolean flag1 = false;
                if(block == Blocks.PISTON_EXTENSION) {
                    tileentity1 = world.getTileEntity(blockposition1);
                    if(tileentity1 instanceof TileEntityPiston) {
                        TileEntityPiston tileentitypiston = (TileEntityPiston)tileentity1;
                        if(tileentitypiston.e() == enumdirection && tileentitypiston.d()) {
                            tileentitypiston.h();
                            flag1 = true;
                        }
                    }
                }

                if(!flag1 && a(block, world, blockposition1, enumdirection.opposite(), false) && (block.k() == 0 || block == Blocks.PISTON || block == Blocks.STICKY_PISTON)) {
                    this.a(world, blockposition, enumdirection, false);
                }
            } else {
                provider.updateBlockFast(world, blockposition.shift(enumdirection), Blocks.AIR.getBlockData(), Piston.this, true, 3);
            }

            world.makeSound((double)blockposition.getX() + 0.5D, (double)blockposition.getY() + 0.5D, (double)blockposition.getZ() + 0.5D, "tile.piston.in", 0.5F, world.random.nextFloat() * 0.15F + 0.6F);
        }

        return true;
    }

    public void updateShape(IBlockAccess iblockaccess, BlockPosition blockposition) {
        IBlockData iblockdata = iblockaccess.getType(blockposition);
        if(iblockdata.getBlock() == this && ((Boolean)iblockdata.get(EXTENDED)).booleanValue()) {
            EnumDirection enumdirection = (EnumDirection)iblockdata.get(FACING);
            if(enumdirection != null) {
                switch(SyntheticClass_1.a[enumdirection.ordinal()]) {
                    case 1:
                        this.a(0.0F, 0.25F, 0.0F, 1.0F, 1.0F, 1.0F);
                        break;
                    case 2:
                        this.a(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
                        break;
                    case 3:
                        this.a(0.0F, 0.0F, 0.25F, 1.0F, 1.0F, 1.0F);
                        break;
                    case 4:
                        this.a(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.75F);
                        break;
                    case 5:
                        this.a(0.25F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                        break;
                    case 6:
                        this.a(0.0F, 0.0F, 0.0F, 0.75F, 1.0F, 1.0F);
                }
            }
        } else {
            this.a(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        }

    }

    public void j() {
        this.a(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, AxisAlignedBB axisalignedbb, List<AxisAlignedBB> list, Entity entity) {
        this.a(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
        super.a(world, blockposition, iblockdata, axisalignedbb, list, entity);
    }

    public AxisAlignedBB a(World world, BlockPosition blockposition, IBlockData iblockdata) {
        this.updateShape(world, blockposition);
        return super.a(world, blockposition, iblockdata);
    }

    public boolean d() {
        return false;
    }

    public static EnumDirection b(int i) {
        int j = i & 7;
        return j > 5?null:EnumDirection.fromType1(j);
    }

    public static EnumDirection a(World world, BlockPosition blockposition, EntityLiving entityliving) {
        if(MathHelper.e((float)entityliving.locX - (float)blockposition.getX()) < 2.0F && MathHelper.e((float)entityliving.locZ - (float)blockposition.getZ()) < 2.0F) {
            double d0 = entityliving.locY + (double)entityliving.getHeadHeight();
            if(d0 - (double)blockposition.getY() > 2.0D) {
                return EnumDirection.UP;
            }

            if((double)blockposition.getY() - d0 > 0.0D) {
                return EnumDirection.DOWN;
            }
        }

        return entityliving.getDirection().opposite();
    }

    public static boolean a(Block block, World world, BlockPosition blockposition, EnumDirection enumdirection, boolean flag) {
        if(block == Blocks.OBSIDIAN) {
            return false;
        } else if(!world.getWorldBorder().a(blockposition)) {
            return false;
        } else if(blockposition.getY() < 0 || enumdirection == EnumDirection.DOWN && blockposition.getY() == 0) {
            return false;
        } else if(blockposition.getY() <= world.getHeight() - 1 && (enumdirection != EnumDirection.UP || blockposition.getY() != world.getHeight() - 1)) {
            if(block != Blocks.PISTON && block != Blocks.STICKY_PISTON) {
                if(block.g(world, blockposition) == -1.0F) {
                    return false;
                }

                if(block.k() == 2) {
                    return false;
                }

                if(block.k() == 1) {
                    if(!flag) {
                        return false;
                    }

                    return true;
                }
            } else if(((Boolean)world.getType(blockposition).get(EXTENDED)).booleanValue()) {
                return false;
            }

            return !(block instanceof IContainer);
        } else {
            return false;
        }
    }

    private boolean a(World world, BlockPosition blockposition, EnumDirection enumdirection, boolean flag) {
        if(!flag) {
            provider.updateBlockFast(world, blockposition.shift(enumdirection), Blocks.AIR.getBlockData(), Piston.this, true, 3);
            world.setAir(blockposition.shift(enumdirection));
        }

        PistonExtendsChecker pistonextendschecker = new PistonExtendsChecker(world, blockposition, enumdirection, flag);
        List list = pistonextendschecker.getMovedBlocks();
        List list1 = pistonextendschecker.getBrokenBlocks();
        if(!pistonextendschecker.a()) {
            return false;
        } else {
            final org.bukkit.block.Block bblock = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
            final List moved = pistonextendschecker.getMovedBlocks();
            final List broken = pistonextendschecker.getBrokenBlocks();
            AbstractList blocks = new AbstractList() {
                public int size() {
                    return moved.size() + broken.size();
                }

                public org.bukkit.block.Block get(int index) {
                    if(index < this.size() && index >= 0) {
                        BlockPosition pos = index < moved.size()?(BlockPosition)moved.get(index):(BlockPosition)broken.get(index - moved.size());
                        return bblock.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                    } else {
                        throw new ArrayIndexOutOfBoundsException(index);
                    }
                }
            };
            int i = list.size() + list1.size();
            Block[] ablock = new Block[i];
            EnumDirection enumdirection1 = flag?enumdirection:enumdirection.opposite();
            Object event;
            if(flag) {
                event = new BlockPistonExtendEvent(bblock, blocks, CraftBlock.notchToBlockFace(enumdirection1));
            } else {
                event = new BlockPistonRetractEvent(bblock, blocks, CraftBlock.notchToBlockFace(enumdirection1));
            }

            world.getServer().getPluginManager().callEvent((Event)event);
            if(((BlockPistonEvent)event).isCancelled()) {
                Iterator var22 = broken.iterator();

                BlockPosition var23;
                while(var22.hasNext()) {
                    var23 = (BlockPosition)var22.next();
                    world.notify(var23);
                }

                var22 = moved.iterator();

                while(var22.hasNext()) {
                    var23 = (BlockPosition)var22.next();
                    world.notify(var23);
                    world.notify(var23.shift(enumdirection1));
                }

                return false;
            } else {
                BlockPosition blockposition1;
                int j;
                for(j = list1.size() - 1; j >= 0; --j) {
                    blockposition1 = (BlockPosition)list1.get(j);
                    Block iblockdata = world.getType(blockposition1).getBlock();
                    iblockdata.b(world, blockposition1, world.getType(blockposition1), 0);
                    provider.updateBlockFast(world, blockposition1, Blocks.AIR.getBlockData(), Piston.this, true, 3);
                    --i;
                    ablock[i] = iblockdata;
                }

                IBlockData var24;
                for(j = list.size() - 1; j >= 0; --j) {
                    blockposition1 = (BlockPosition)list.get(j);
                    var24 = world.getType(blockposition1);
                    Block blockposition2 = var24.getBlock();
                    blockposition2.toLegacyData(var24);
                    provider.updateBlockFast(world, blockposition1, Blocks.AIR.getBlockData(), Piston.this, true, 2);
                    blockposition1 = blockposition1.shift(enumdirection1);
                    provider.updateBlockFast(world, blockposition1, Blocks.PISTON_EXTENSION.getBlockData().set(FACING, enumdirection), Piston.this, true, 4);
                    world.setTileEntity(blockposition1, BlockPistonMoving.a(var24, enumdirection, flag, false));
                    --i;
                    ablock[i] = blockposition2;
                }

                BlockPosition var25 = blockposition.shift(enumdirection);
                if(flag) {
                    BlockPistonExtension.EnumPistonType k = this.sticky? BlockPistonExtension.EnumPistonType.STICKY: BlockPistonExtension.EnumPistonType.DEFAULT;
                    var24 = Blocks.PISTON_HEAD.getBlockData().set(BlockPistonExtension.FACING, enumdirection).set(BlockPistonExtension.TYPE, k);
                    IBlockData iblockdata1 = Blocks.PISTON_EXTENSION.getBlockData().set(BlockPistonMoving.FACING, enumdirection).set(BlockPistonMoving.TYPE, this.sticky? BlockPistonExtension.EnumPistonType.STICKY: BlockPistonExtension.EnumPistonType.DEFAULT);
                    provider.updateBlockFast(world, var25, iblockdata1, Piston.this, true, 4);
                    world.setTileEntity(var25, BlockPistonMoving.a(var24, enumdirection, true, false));
                }

                int var26;
                for(var26 = list1.size() - 1; var26 >= 0; --var26) {
                    world.applyPhysics((BlockPosition)list1.get(var26), ablock[i++]);
                }

                for(var26 = list.size() - 1; var26 >= 0; --var26) {
                    world.applyPhysics((BlockPosition)list.get(var26), ablock[i++]);
                }

                if(flag) {
                    world.applyPhysics(var25, Blocks.PISTON_HEAD);
                    world.applyPhysics(blockposition, this);
                }

                return true;
            }
        }
    }

    public IBlockData fromLegacyData(int i) {
        return this.getBlockData().set(FACING, b(i)).set(EXTENDED, Boolean.valueOf((i & 8) > 0));
    }

    public int toLegacyData(IBlockData iblockdata) {
        byte b0 = 0;
        int i = b0 | ((EnumDirection)iblockdata.get(FACING)).a();
        if(((Boolean)iblockdata.get(EXTENDED)).booleanValue()) {
            i |= 8;
        }

        return i;
    }

    protected BlockStateList getStateList() {
        return new BlockStateList(this, new IBlockState[]{FACING, EXTENDED});
    }

    static class SyntheticClass_1 {
        static final int[] a = new int[EnumDirection.values().length];

        static {
            try {
                a[EnumDirection.DOWN.ordinal()] = 1;
            } catch (NoSuchFieldError var5) {
                ;
            }

            try {
                a[EnumDirection.UP.ordinal()] = 2;
            } catch (NoSuchFieldError var4) {
                ;
            }

            try {
                a[EnumDirection.NORTH.ordinal()] = 3;
            } catch (NoSuchFieldError var3) {
                ;
            }

            try {
                a[EnumDirection.SOUTH.ordinal()] = 4;
            } catch (NoSuchFieldError var2) {
                ;
            }

            try {
                a[EnumDirection.WEST.ordinal()] = 5;
            } catch (NoSuchFieldError var1) {
                ;
            }

            try {
                a[EnumDirection.EAST.ordinal()] = 6;
            } catch (NoSuchFieldError var0) {
                ;
            }

        }

        SyntheticClass_1() {
        }
    }
}