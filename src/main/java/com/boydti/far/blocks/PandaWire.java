package com.boydti.far.blocks;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.server.v1_10_R1.BaseBlockPosition;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.BlockDiodeAbstract;
import net.minecraft.server.v1_10_R1.BlockPiston;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.BlockRedstoneComparator;
import net.minecraft.server.v1_10_R1.BlockRedstoneTorch;
import net.minecraft.server.v1_10_R1.BlockRedstoneWire;
import net.minecraft.server.v1_10_R1.EnumDirection;
import net.minecraft.server.v1_10_R1.IBlockAccess;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.SoundEffectType;
import net.minecraft.server.v1_10_R1.World;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.event.block.BlockRedstoneEvent;

import com.boydti.far.ReflectionUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class PandaWire extends BlockRedstoneWire {
    private final List<BlockPosition> turnOff = Lists.newArrayList();
    private final List<BlockPosition> turnOn = Lists.newArrayList();
    private final Set<BlockPosition> updatedRedstoneWire = Sets.newLinkedHashSet();
    private static final EnumDirection[] facingsHorizontal = { EnumDirection.WEST, EnumDirection.EAST, EnumDirection.NORTH, EnumDirection.SOUTH };
    private static final EnumDirection[] facingsVertical = { EnumDirection.DOWN, EnumDirection.UP };
    private static final EnumDirection[] facings = ArrayUtils.addAll(facingsVertical, facingsHorizontal);
    private static final BaseBlockPosition[] surroundingBlocksOffset;

    static {
        Set<BaseBlockPosition> set = Sets.newLinkedHashSet();
        for (EnumDirection facing : facings) {
            set.add(ReflectionUtil.getOfT(facing, BaseBlockPosition.class));
        }
        for (EnumDirection facing1 : facings) {
            BaseBlockPosition v1 = ReflectionUtil.getOfT(facing1, BaseBlockPosition.class);
            for (EnumDirection facing2 : facings) {
                BaseBlockPosition v2 = ReflectionUtil.getOfT(facing2, BaseBlockPosition.class);

                set.add(new BlockPosition(v1.getX() + v2.getX(), v1.getY() + v2.getY(), v1.getZ() + v2.getZ()));
            }
        }
        set.remove(BlockPosition.ZERO);
        surroundingBlocksOffset = set.toArray(new BaseBlockPosition[set.size()]);
    }

    private boolean g = true;

    public PandaWire() {
        c(0.0F);
        a(SoundEffectType.d);
        c("redstoneDust");
        q();
    }

    private void e(World world, BlockPosition blockposition, IBlockData iblockdata) {
        calculateCurrentChanges(world, blockposition);

        Set<BlockPosition> blocksNeedingUpdate = Sets.newLinkedHashSet();
        for (Iterator localIterator = this.updatedRedstoneWire.iterator(); localIterator.hasNext();) {
            BlockPosition posi = (BlockPosition) localIterator.next();
            addBlocksNeedingUpdate(world, posi, blocksNeedingUpdate);
        }
        BlockPosition posi;
        Object it = Lists.newLinkedList(this.updatedRedstoneWire).descendingIterator();
        while (((Iterator) it).hasNext()) {
            addAllSurroundingBlocks((BlockPosition) ((Iterator) it).next(), blocksNeedingUpdate);
        }
        blocksNeedingUpdate.removeAll(this.updatedRedstoneWire);

        this.updatedRedstoneWire.clear();
        for (BlockPosition pos2 : blocksNeedingUpdate) {
            world.e(pos2, this);
        }
    }

    private void calculateCurrentChanges(World world, BlockPosition blockposition) {
        if (world.getType(blockposition).getBlock() == this) {
            this.turnOff.add(blockposition);
        } else {
            checkSurroundingWires(world, blockposition);
        }
        while (!this.turnOff.isEmpty()) {
            BlockPosition pos = this.turnOff.remove(0);
            IBlockData state = world.getType(pos);
            int oldPower = state.get(POWER).intValue();
            this.g = false;
            int blockPower = world.z(pos);
            this.g = true;
            int wirePower = getSurroundingWirePower(world, pos);

            wirePower--;
            int newPower = Math.max(blockPower, wirePower);
            if (oldPower != newPower) {
                BlockRedstoneEvent event = new BlockRedstoneEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()), oldPower, newPower);
                world.getServer().getPluginManager().callEvent(event);

                newPower = event.getNewCurrent();
            }
            if (newPower < oldPower) {
                if ((blockPower > 0) && (!this.turnOn.contains(pos))) {
                    this.turnOn.add(pos);
                }
                state = setWireState(world, pos, state, 0);
            } else if (newPower > oldPower) {
                state = setWireState(world, pos, state, newPower);
            }
            checkSurroundingWires(world, pos);
        }
        while (!this.turnOn.isEmpty()) {
            BlockPosition pos = this.turnOn.remove(0);
            IBlockData state = world.getType(pos);
            int oldPower = state.get(POWER).intValue();
            this.g = false;
            int blockPower = world.z(pos);
            this.g = true;
            int wirePower = getSurroundingWirePower(world, pos);

            wirePower--;
            int newPower = Math.max(blockPower, wirePower);
            if (oldPower != newPower) {
                BlockRedstoneEvent event = new BlockRedstoneEvent(world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()), oldPower, newPower);
                world.getServer().getPluginManager().callEvent(event);

                newPower = event.getNewCurrent();
            }
            if (newPower > oldPower) {
                state = setWireState(world, pos, state, newPower);
            } else if (newPower >= oldPower) {}
            checkSurroundingWires(world, pos);
        }
        this.turnOff.clear();
        this.turnOn.clear();
    }

    private void addWireToList(World worldIn, BlockPosition pos, int otherPower) {
        IBlockData state = worldIn.getType(pos);
        if (state.getBlock() == this) {
            int power = state.get(POWER).intValue();
            if ((power < otherPower - 1) && (!this.turnOn.contains(pos))) {
                this.turnOn.add(pos);
            }
            if ((power > otherPower) && (!this.turnOff.contains(pos))) {
                this.turnOff.add(pos);
            }
        }
    }

    private void checkSurroundingWires(World worldIn, BlockPosition pos) {
        IBlockData state = worldIn.getType(pos);
        int ownPower = 0;
        if (state.getBlock() == this) {
            ownPower = state.get(POWER).intValue();
        }
        for (EnumDirection facing : facingsHorizontal) {
            BlockPosition offsetPos = pos.shift(facing);
            if (facing.k().c()) {
                addWireToList(worldIn, offsetPos, ownPower);
            }
        }
        for (EnumDirection facingVertical : facingsVertical) {
            BlockPosition offsetPos = pos.shift(facingVertical);
            boolean solidBlock = worldIn.getType(offsetPos).k();
            for (EnumDirection facingHorizontal : facingsHorizontal) {
                if (((facingVertical == EnumDirection.UP) && (!solidBlock)) || ((facingVertical == EnumDirection.DOWN) && (solidBlock) && (!worldIn.getType(offsetPos.shift(facingHorizontal)).l()))) {
                    addWireToList(worldIn, offsetPos.shift(facingHorizontal), ownPower);
                }
            }
        }
    }

    private int getSurroundingWirePower(World worldIn, BlockPosition pos) {
        int wirePower = 0;
        for (EnumDirection enumfacing : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition offsetPos = pos.shift(enumfacing);

            wirePower = getPower(worldIn, offsetPos, wirePower);
            if ((worldIn.getType(offsetPos).l()) && (!worldIn.getType(pos.up()).l())) {
                wirePower = getPower(worldIn, offsetPos.up(), wirePower);
            } else if (!worldIn.getType(offsetPos).l()) {
                wirePower = getPower(worldIn, offsetPos.down(), wirePower);
            }
        }
        return wirePower;
    }

    private void addBlocksNeedingUpdate(World worldIn, BlockPosition pos, Set<BlockPosition> set) {
        List<EnumDirection> connectedSides = getSidesToPower(worldIn, pos);
        for (EnumDirection facing : facings) {
            BlockPosition offsetPos = pos.shift(facing);
            if (((connectedSides.contains(facing.opposite())) || (facing == EnumDirection.DOWN) || ((facing.k().c()) && (a(worldIn.getType(offsetPos), facing))))
                    && (canBlockBePoweredFromSide(worldIn.getType(offsetPos), facing, true))) {
                set.add(offsetPos);
            }
        }
        for (EnumDirection facing : facings) {
            BlockPosition offsetPos = pos.shift(facing);
            if (((connectedSides.contains(facing.opposite())) || (facing == EnumDirection.DOWN)) && (worldIn.getType(offsetPos).l())) {
                for (EnumDirection facing1 : facings) {
                    if (canBlockBePoweredFromSide(worldIn.getType(offsetPos.shift(facing1)), facing1, false)) {
                        set.add(offsetPos.shift(facing1));
                    }
                }
            }
        }
    }

    private boolean canBlockBePoweredFromSide(IBlockData state, EnumDirection side, boolean isWire) {
        if (((state.getBlock() instanceof BlockPiston)) && (state.get(BlockPiston.FACING) == side.opposite())) {
            return false;
        }
        if (((state.getBlock() instanceof BlockDiodeAbstract)) && (state.get(BlockDiodeAbstract.FACING) != side.opposite())) {
            if ((isWire) && ((state.getBlock() instanceof BlockRedstoneComparator)) && (state.get(BlockRedstoneComparator.FACING).k() != side.k()) && (side.k().c())) {
                return true;
            }
            return false;
        }
        if (((state.getBlock() instanceof BlockRedstoneTorch)) && ((isWire) || (state.get(BlockRedstoneTorch.FACING) != side))) {
            return false;
        }
        return true;
    }

    private List<EnumDirection> getSidesToPower(World worldIn, BlockPosition pos) {
        List retval = Lists.newArrayList();
        for (EnumDirection facing : facingsHorizontal) {
            if (c(worldIn, pos, facing)) {
                retval.add(facing);
            }
        }
        if (retval.isEmpty()) {
            return Lists.newArrayList(facingsHorizontal);
        }
        boolean northsouth = (retval.contains(EnumDirection.NORTH)) || (retval.contains(EnumDirection.SOUTH));
        boolean eastwest = (retval.contains(EnumDirection.EAST)) || (retval.contains(EnumDirection.WEST));
        if (northsouth) {
            retval.remove(EnumDirection.EAST);
            retval.remove(EnumDirection.WEST);
        }
        if (eastwest) {
            retval.remove(EnumDirection.NORTH);
            retval.remove(EnumDirection.SOUTH);
        }
        return retval;
    }

    private void addAllSurroundingBlocks(BlockPosition pos, Set<BlockPosition> set) {
        for (BaseBlockPosition vect : surroundingBlocksOffset) {
            set.add(pos.a(vect));
        }
    }

    private IBlockData setWireState(World worldIn, BlockPosition pos, IBlockData state, int power) {
        state = state.set(POWER, Integer.valueOf(power));
        worldIn.setTypeAndData(pos, state, 2);
        this.updatedRedstoneWire.add(pos);
        return state;
    }

    @Override
    public void onPlace(World world, BlockPosition blockposition, IBlockData iblockdata) {
        if (!world.isClientSide) {
            e(world, blockposition, iblockdata);
            Iterator iterator = EnumDirection.EnumDirectionLimit.VERTICAL.iterator();
            while (iterator.hasNext()) {
                EnumDirection enumdirection = (EnumDirection) iterator.next();
                world.applyPhysics(blockposition.shift(enumdirection), this);
            }
            iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();
            while (iterator.hasNext()) {
                EnumDirection enumdirection = (EnumDirection) iterator.next();
                b(world, blockposition.shift(enumdirection));
            }
            iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();
            while (iterator.hasNext()) {
                EnumDirection enumdirection = (EnumDirection) iterator.next();
                BlockPosition blockposition1 = blockposition.shift(enumdirection);
                if (world.getType(blockposition1).l()) {
                    b(world, blockposition1.up());
                } else {
                    b(world, blockposition1.down());
                }
            }
        }
    }

    @Override
    public void remove(World world, BlockPosition blockposition, IBlockData iblockdata) {
        super.remove(world, blockposition, iblockdata);
        if (!world.isClientSide) {
            EnumDirection[] aenumdirection = EnumDirection.values();
            int i = aenumdirection.length;
            for (int j = 0; j < i; j++) {
                EnumDirection enumdirection = aenumdirection[j];

                world.applyPhysics(blockposition.shift(enumdirection), this);
            }
            e(world, blockposition, iblockdata);
            Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();
            while (iterator.hasNext()) {
                EnumDirection enumdirection1 = (EnumDirection) iterator.next();
                b(world, blockposition.shift(enumdirection1));
            }
            iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();
            while (iterator.hasNext()) {
                EnumDirection enumdirection1 = (EnumDirection) iterator.next();
                BlockPosition blockposition1 = blockposition.shift(enumdirection1);
                if (world.getType(blockposition1).l()) {
                    b(world, blockposition1.up());
                } else {
                    b(world, blockposition1.down());
                }
            }
        }
    }

    @Override
    public void a(IBlockData iblockdata, World world, BlockPosition blockposition, Block block) {
        if (!world.isClientSide) {
            if (canPlace(world, blockposition)) {
                e(world, blockposition, iblockdata);
            } else {
                b(world, blockposition, iblockdata, 0);
                world.setAir(blockposition);
            }
        }
    }

    @Override
    public int b(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        if (!this.g) {
            return 0;
        }
        int i = iblockdata.get(BlockRedstoneWire.POWER).intValue();
        if (i == 0) {
            return 0;
        }
        if (enumdirection == EnumDirection.UP) {
            return i;
        }
        if (getSidesToPower((World) iblockaccess, blockposition).contains(enumdirection)) {
            return i;
        }
        return 0;
    }

    private boolean c(IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        BlockPosition blockposition1 = blockposition.shift(enumdirection);
        IBlockData iblockdata = iblockaccess.getType(blockposition1);
        boolean flag = iblockdata.l();
        boolean flag1 = iblockaccess.getType(blockposition.up()).l();

        return (!flag1) && (flag) && (c(iblockaccess, blockposition1.up()));
    }
}