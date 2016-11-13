package com.boydti.far.v110.blocks;

import com.boydti.far.v110.QueueManager110;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.v1_10_R1.*;
import net.minecraft.server.v1_10_R1.BlockPistonExtension.EnumPistonType;
import org.bukkit.craftbukkit.v1_10_R1.block.CraftBlock;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public class Piston extends BlockPiston {
    
    public static final BlockStateBoolean EXTENDED = BlockStateBoolean.of("extended");
    protected static final AxisAlignedBB b = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.75D, 1.0D, 1.0D);
    protected static final AxisAlignedBB c = new AxisAlignedBB(0.25D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB d = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.75D);
    protected static final AxisAlignedBB e = new AxisAlignedBB(0.0D, 0.0D, 0.25D, 1.0D, 1.0D, 1.0D);
    protected static final AxisAlignedBB f = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D);
    protected static final AxisAlignedBB g = new AxisAlignedBB(0.0D, 0.25D, 0.0D, 1.0D, 1.0D, 1.0D);
    private final QueueManager110 provider;
    private final boolean sticky;
    
    public Piston(boolean flag, QueueManager110 provider) {
        super(flag);
        w(this.blockStateList.getBlockData().set(FACING, EnumDirection.NORTH).set(EXTENDED, Boolean.valueOf(false)));
        a(SoundEffectType.d);
        c(0.5F);
        a(CreativeModeTab.d);
        this.sticky = flag;
        this.provider = provider;
    }
    
    @Override
    public void postPlace(World world, BlockPosition blockposition, IBlockData iblockdata, EntityLiving entityliving, ItemStack itemstack) {
        provider.updateBlockFast(world, blockposition, iblockdata.set(FACING, a(blockposition, entityliving)), Piston.this, true, 2);
        e(world, blockposition, iblockdata);
    }
    
    private void e(World world, BlockPosition blockposition, IBlockData iblockdata) {
        EnumDirection enumdirection = iblockdata.get(FACING);
        boolean flag = a(world, blockposition, enumdirection);
        if ((flag) && (!iblockdata.get(EXTENDED).booleanValue())) {
            if (new PistonExtendsChecker(world, blockposition, enumdirection, true).a()) {
                world.playBlockAction(blockposition, this, 0, enumdirection.a());
            }
        } else if ((!flag) && (iblockdata.get(EXTENDED).booleanValue())) {
            if (!this.sticky) {
                org.bukkit.block.Block block = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
                BlockPistonRetractEvent event = new BlockPistonRetractEvent(block, ImmutableList.of(), CraftBlock.notchToBlockFace(enumdirection));
                world.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
            }
            world.playBlockAction(blockposition, this, 1, enumdirection.a());
        }
    }
    
    @Override
    public boolean a(IBlockData iblockdata, World world, BlockPosition blockposition, int i, int j) {
        EnumDirection enumdirection = iblockdata.get(FACING);
        boolean flag = a(world, blockposition, enumdirection);
        if ((flag) && (i == 1)) {
            provider.updateBlockFast(world, blockposition, iblockdata.set(EXTENDED, Boolean.valueOf(true)), Piston.this, true, 2);
            return false;
        }
        if ((!flag) && (i == 0)) {
            return false;
        }
        if (i == 0) {
            if (!a(world, blockposition, enumdirection, true)) {
                return false;
            }
            provider.updateBlockFast(world, blockposition, iblockdata.set(EXTENDED, Boolean.valueOf(true)), Piston.this, true, 2);
            world.a(null, blockposition, SoundEffects.eb, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.25F + 0.6F);
        } else if (i == 1) {
            TileEntity tileentity = world.getTileEntity(blockposition.shift(enumdirection));
            if ((tileentity instanceof TileEntityPiston)) {
                ((TileEntityPiston) tileentity).i();
            }
            
            //            provider.updateBlockFast(
            //            world,
            //            blockposition,
            //            Blocks.PISTON_EXTENSION.getBlockData().set(BlockPistonMoving.FACING, enumdirection)
            //            .set(BlockPistonMoving.TYPE, this.sticky ? BlockPistonExtension.EnumPistonType.STICKY : BlockPistonExtension.EnumPistonType.DEFAULT), true, 3);
            
            provider.updateBlockFast(
            world,
            blockposition,
            Blocks.PISTON_EXTENSION.getBlockData().set(BlockPistonMoving.FACING, enumdirection)
            .set(BlockPistonMoving.TYPE, this.sticky ? BlockPistonExtension.EnumPistonType.STICKY : BlockPistonExtension.EnumPistonType.DEFAULT), Piston.this, true, 3);
            world.setTileEntity(blockposition, BlockPistonMoving.a(fromLegacyData(j), enumdirection, false, true));
            if (this.sticky) {
                BlockPosition blockposition1 = blockposition.a(enumdirection.getAdjacentX() * 2, enumdirection.getAdjacentY() * 2, enumdirection.getAdjacentZ() * 2);
                IBlockData iblockdata1 = world.getType(blockposition1);
                Block block = iblockdata1.getBlock();
                boolean flag1 = false;
                if (block == Blocks.PISTON_EXTENSION) {
                    TileEntity tileentity1 = world.getTileEntity(blockposition1);
                    if ((tileentity1 instanceof TileEntityPiston)) {
                        TileEntityPiston tileentitypiston = (TileEntityPiston) tileentity1;
                        if ((tileentitypiston.g() == enumdirection) && (tileentitypiston.e())) {
                            tileentitypiston.i();
                            flag1 = true;
                        }
                    }
                }
                if ((!flag1)
                && (a(iblockdata1, world, blockposition1, enumdirection.opposite(), false))
                && ((iblockdata1.o() == EnumPistonReaction.NORMAL) || (block == Blocks.PISTON) || (block == Blocks.STICKY_PISTON))) {
                    a(world, blockposition, enumdirection, false);
                }
            } else {
                provider.updateBlockFast(world, blockposition.shift(enumdirection), Blocks.AIR.getBlockData(), Piston.this, true, 3);
            }
            world.a(null, blockposition, SoundEffects.ea, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.15F + 0.6F);
        }
        return true;
    }
    
    private boolean a(World world, BlockPosition blockposition, EnumDirection enumdirection, boolean flag) {
        if (!flag) {
            provider.updateBlockFast(world, blockposition.shift(enumdirection), Blocks.AIR.getBlockData(), Piston.this, true, 3);
        }

        PistonExtendsChecker pistonextendschecker = new PistonExtendsChecker(world, blockposition, enumdirection, flag);
        if (!pistonextendschecker.a()) {
            return false;
        } else {
            List<BlockPosition> list = pistonextendschecker.getMovedBlocks();
            ArrayList<IBlockData> arraylist = Lists.newArrayList();
            
            for (int list1 = 0; list1 < list.size(); ++list1) {
                BlockPosition j = list.get(list1);
                arraylist.add(world.getType(j).b((IBlockAccess) world, j));
            }
            
            List var23 = pistonextendschecker.getBrokenBlocks();
            int var24 = list.size() + var23.size();
            IBlockData[] aiblockdata = new IBlockData[var24];
            EnumDirection enumdirection1 = flag ? enumdirection : enumdirection.opposite();
            final org.bukkit.block.Block bblock = world.getWorld().getBlockAt(blockposition.getX(), blockposition.getY(), blockposition.getZ());
            final List moved = pistonextendschecker.getMovedBlocks();
            final List broken = pistonextendschecker.getBrokenBlocks();
            AbstractList blocks = new AbstractList() {
                @Override
                public int size() {
                    return moved.size() + broken.size();
                }
                
                @Override
                public org.bukkit.block.Block get(int index) {
                    if (index < this.size() && index >= 0) {
                        BlockPosition pos = index < moved.size() ? (BlockPosition) moved.get(index) : (BlockPosition) broken.get(index - moved.size());
                        return bblock.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                    } else {
                        throw new ArrayIndexOutOfBoundsException(index);
                    }
                }
            };
            BlockPistonEvent event;
            if (flag) {
                event = new BlockPistonExtendEvent(bblock, blocks, CraftBlock.notchToBlockFace(enumdirection1));
            } else {
                event = new BlockPistonRetractEvent(bblock, blocks, CraftBlock.notchToBlockFace(enumdirection1));
            }
            
            world.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                Iterator var25 = broken.iterator();
                BlockPosition var26;
                while (var25.hasNext()) {
                    var26 = (BlockPosition) var25.next();
                    world.notify(var26, Blocks.AIR.getBlockData(), world.getType(var26), 3);
                }
                var25 = moved.iterator();
                while (var25.hasNext()) {
                    var26 = (BlockPosition) var25.next();
                    world.notify(var26, Blocks.AIR.getBlockData(), world.getType(var26), 3);
                    var26 = var26.shift(enumdirection1);
                    world.notify(var26, Blocks.AIR.getBlockData(), world.getType(var26), 3);
                }
                
                return false;
            } else {
                BlockPosition blockposition2;
                int k;
                IBlockData iblockdata;
                for (k = var23.size() - 1; k >= 0; --k) {
                    blockposition2 = (BlockPosition) var23.get(k);
                    iblockdata = world.getType(blockposition2);
                    iblockdata.getBlock().b(world, blockposition2, iblockdata, 0);
                    provider.updateBlockFast(world, blockposition2, Blocks.AIR.getBlockData(), Piston.this, true, 3);
                    --var24;
                    aiblockdata[var24] = iblockdata;
                }
                
                for (k = list.size() - 1; k >= 0; --k) {
                    blockposition2 = list.get(k);
                    iblockdata = world.getType(blockposition2);
                    provider.updateBlockFast(world, blockposition2, Blocks.AIR.getBlockData(), Piston.this, true, 2);
                    blockposition2 = blockposition2.shift(enumdirection1);
                    provider.updateBlockFast(world, blockposition2, Blocks.PISTON_EXTENSION.getBlockData().set(FACING, enumdirection), Piston.this, true, 4);
                    world.setTileEntity(blockposition2, BlockPistonMoving.a(arraylist.get(k), enumdirection, flag, false));
                    --var24;
                    aiblockdata[var24] = iblockdata;
                }
                
                BlockPosition blockposition3 = blockposition.shift(enumdirection);
                if (flag) {
                    EnumPistonType l = this.sticky ? EnumPistonType.STICKY : EnumPistonType.DEFAULT;
                    iblockdata = Blocks.PISTON_HEAD.getBlockData().set(BlockPistonExtension.FACING, enumdirection).set(BlockPistonExtension.TYPE, l);
                    IBlockData iblockdata1 = Blocks.PISTON_EXTENSION.getBlockData().set(BlockPistonMoving.FACING, enumdirection)
                    .set(BlockPistonMoving.TYPE, this.sticky ? EnumPistonType.STICKY : EnumPistonType.DEFAULT);
                    provider.updateBlockFast(world, blockposition3, iblockdata1, Piston.this, true, 4);
                    world.setTileEntity(blockposition3, BlockPistonMoving.a(iblockdata, enumdirection, true, false));
                }
                
                int var27;
                for (var27 = var23.size() - 1; var27 >= 0; --var27) {
                    world.applyPhysics((BlockPosition) var23.get(var27), aiblockdata[var24++].getBlock());
                }
                
                for (var27 = list.size() - 1; var27 >= 0; --var27) {
                    world.applyPhysics(list.get(var27), aiblockdata[var24++].getBlock());
                }
                
                if (flag) {
                    world.applyPhysics(blockposition3, Blocks.PISTON_HEAD);
                    world.applyPhysics(blockposition, this);
                }
                
                return true;
            }
        }
    }
    
    @Override
    public AxisAlignedBB a(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        if (iblockdata.get(EXTENDED).booleanValue()) {
            switch (SyntheticClass_1.a[iblockdata.get(FACING).ordinal()]) {
                case 1:
                    return g;
                case 2:
                default:
                    return f;
                case 3:
                    return e;
                case 4:
                    return d;
                case 5:
                    return c;
                case 6:
                    return b;
            }
        }
        return j;
    }
    
    @Override
    public boolean k(IBlockData iblockdata) {
        return (!iblockdata.get(EXTENDED).booleanValue()) || (iblockdata.get(FACING) == EnumDirection.DOWN);
    }
    
    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, AxisAlignedBB axisalignedbb, List<AxisAlignedBB> list, @Nullable Entity entity) {
        a(blockposition, axisalignedbb, list, iblockdata.c(world, blockposition));
    }
    
    @Override
    public boolean b(IBlockData iblockdata) {
        return false;
    }
    
    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Block block) {
        if (!world.isClientSide) {
            e(world, blockposition, iblockdata);
        }
    }
    
    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if ((!world.isClientSide) && (world.getTileEntity(blockposition) == null)) {
            e(world, blockposition, iblockdata);
        }
    }
    
    @Override
    public IBlockData getPlacedState(World world, BlockPosition blockposition, EnumDirection enumdirection, float f, float f1, float f2, int i, EntityLiving entityliving) {
        return getBlockData().set(FACING, a(blockposition, entityliving)).set(EXTENDED, Boolean.valueOf(false));
    }
    
    private boolean a(World world, BlockPosition blockposition, EnumDirection enumdirection) {
        EnumDirection[] aenumdirection = EnumDirection.values();
        int i = aenumdirection.length;
        for (int j = 0; j < i; j++) {
            EnumDirection enumdirection1 = aenumdirection[j];
            if ((enumdirection1 != enumdirection) && (world.isBlockFacePowered(blockposition.shift(enumdirection1), enumdirection1))) {
                return true;
            }
        }
        if (world.isBlockFacePowered(blockposition, EnumDirection.DOWN)) {
            return true;
        }
        BlockPosition blockposition1 = blockposition.up();
        EnumDirection[] aenumdirection1 = EnumDirection.values();
        
        int j = aenumdirection1.length;
        for (int k = 0; k < j; k++) {
            EnumDirection enumdirection2 = aenumdirection1[k];
            if ((enumdirection2 != EnumDirection.DOWN) && (world.isBlockFacePowered(blockposition1.shift(enumdirection2), enumdirection2))) {
                return true;
            }
        }
        return false;
    }
    
    
    @Override
    public boolean c(IBlockData iblockdata) {
        return false;
    }
    
    @Nullable
    public static EnumDirection e(int i) {
        int j = i & 0x7;
        return j > 5 ? null : EnumDirection.fromType1(j);
    }
    
    public static EnumDirection a(BlockPosition blockposition, EntityLiving entityliving) {
        if ((MathHelper.e((float) entityliving.locX - blockposition.getX()) < 2.0F) && (MathHelper.e((float) entityliving.locZ - blockposition.getZ()) < 2.0F)) {
            double d0 = entityliving.locY + entityliving.getHeadHeight();
            if (d0 - blockposition.getY() > 2.0D) {
                return EnumDirection.UP;
            }
            if (blockposition.getY() - d0 > 0.0D) {
                return EnumDirection.DOWN;
            }
        }
        return entityliving.getDirection().opposite();
    }
    
    public static boolean a(IBlockData iblockdata, World world, BlockPosition blockposition, EnumDirection enumdirection, boolean flag) {
        Block block = iblockdata.getBlock();
        if (block == Blocks.OBSIDIAN) {
            return false;
        }
        if (!world.getWorldBorder().a(blockposition)) {
            return false;
        }
        if ((blockposition.getY() >= 0) && ((enumdirection != EnumDirection.DOWN) || (blockposition.getY() != 0))) {
            if ((blockposition.getY() <= world.getHeight() - 1) && ((enumdirection != EnumDirection.UP) || (blockposition.getY() != world.getHeight() - 1))) {
                if ((block != Blocks.PISTON) && (block != Blocks.STICKY_PISTON)) {
                    if (iblockdata.b(world, blockposition) == -1.0F) {
                        return false;
                    }
                    if (iblockdata.o() == EnumPistonReaction.BLOCK) {
                        return false;
                    }
                    if (iblockdata.o() == EnumPistonReaction.DESTROY) {
                        return flag;
                    }
                } else if (iblockdata.get(EXTENDED).booleanValue()) {
                    return false;
                }
                return !block.isTileEntity();
            }
            return false;
        }
        return false;
    }
    
    
    @Override
    public IBlockData fromLegacyData(int i) {
        return getBlockData().set(FACING, e(i)).set(EXTENDED, Boolean.valueOf((i & 0x8) > 0));
    }
    
    @Override
    public int toLegacyData(IBlockData iblockdata) {
        byte b0 = 0;
        int i = b0 | iblockdata.get(FACING).a();
        if (iblockdata.get(EXTENDED).booleanValue()) {
            i |= 0x8;
        }
        return i;
    }
    
    @Override
    public IBlockData a(IBlockData iblockdata, EnumBlockRotation enumblockrotation) {
        return iblockdata.set(FACING, enumblockrotation.a(iblockdata.get(FACING)));
    }
    
    @Override
    public IBlockData a(IBlockData iblockdata, EnumBlockMirror enumblockmirror) {
        return iblockdata.a(enumblockmirror.a(iblockdata.get(FACING)));
    }
    
    @Override
    protected BlockStateList getStateList() {
        return new BlockStateList(this, new IBlockState[] { FACING, EXTENDED });
    }
    
    static class SyntheticClass_1 {
        static final int[] a = new int[EnumDirection.values().length];
        
        static {
            try {
                a[EnumDirection.DOWN.ordinal()] = 1;
            } catch (NoSuchFieldError localNoSuchFieldError1) {}
            try {
                a[EnumDirection.UP.ordinal()] = 2;
            } catch (NoSuchFieldError localNoSuchFieldError2) {}
            try {
                a[EnumDirection.NORTH.ordinal()] = 3;
            } catch (NoSuchFieldError localNoSuchFieldError3) {}
            try {
                a[EnumDirection.SOUTH.ordinal()] = 4;
            } catch (NoSuchFieldError localNoSuchFieldError4) {}
            try {
                a[EnumDirection.WEST.ordinal()] = 5;
            } catch (NoSuchFieldError localNoSuchFieldError5) {}
            try {
                a[EnumDirection.EAST.ordinal()] = 6;
            } catch (NoSuchFieldError localNoSuchFieldError6) {}
        }
    }

}
