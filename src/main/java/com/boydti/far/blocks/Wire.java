package com.boydti.far.blocks;

import com.boydti.far.QueueManager;
import com.boydti.fawe.object.RunnableVal;
import java.util.*;
import net.minecraft.server.v1_10_R1.*;
import net.minecraft.server.v1_10_R1.BlockPosition.MutableBlockPosition;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.event.block.BlockRedstoneEvent;

public class Wire extends BlockRedstoneWire {
    private final LinkedHashSet<BlockPosition> turnOff;
    private final LinkedHashSet<BlockPosition> turnOn;
    private final LinkedHashSet<BlockPosition> updatedRedstone;
    private static final EnumDirection[] facingsHorizontal;
    private static final EnumDirection[] facingsVertical;
    private static final EnumDirection[] facings;
    private static final BaseBlockPosition[] surroundingBlocksOffset;
    private boolean g;
    private final QueueManager provider;
    
    public Wire(QueueManager provider) {
        this.provider = provider;
        this.turnOff = new LinkedHashSet<>();
        this.turnOn = new LinkedHashSet<>();
        this.updatedRedstone = new LinkedHashSet<>();
        this.g = true;
        this.c(0.0f);
        this.a(SoundEffectType.d);
        this.c("redstoneDust");
        this.q();
    }
    
    private EnumDirection getDirection(int dx, int dy, int dz) {
        switch (dx) {
            case -1:
                return EnumDirection.WEST;
            case 1:
                return EnumDirection.EAST;
        }
        switch (dz) {
            case -1:
                return EnumDirection.NORTH;
            case 1:
                return EnumDirection.SOUTH;
        }
        switch (dy) {
            case -1:
                return EnumDirection.DOWN;
            case 1:
                return EnumDirection.UP;
        }
        return null;
    }
    
    private EnumDirection getDirection(BlockPosition pos1, BlockPosition pos2) {
        int dx = pos2.getX() - pos1.getX();
        switch (dx) {
            case -1:
                return EnumDirection.WEST;
            case 1:
                return EnumDirection.EAST;
        }
        int dz = pos2.getZ() - pos1.getZ();
        switch (dz) {
            case -1:
                return EnumDirection.NORTH;
            case 1:
                return EnumDirection.SOUTH;
        }
        int dy = pos2.getY() - pos1.getY();
        switch (dy) {
            case -1:
                return EnumDirection.DOWN;
            case 1:
                return EnumDirection.UP;
        }
        
        return null;
    }
    
    private BlockPosition getImmutable(BlockPosition pos) {
        if (pos instanceof MutableBlockPosition) {
            return new BlockPosition(pos.getX(), pos.getY(), pos.getZ());
        }
        return pos;
    }

    private static int tick;

    private void handleUpdate(final World world, final BlockPosition blockposition, final IBlockData iblockdata) {
        tick++;
        LinkedHashSet<BlockPosition> toUpdate = new LinkedHashSet<BlockPosition>();
        this.calculateCurrentChanges(world, blockposition, new RunnableVal<BlockPosition>() {
            @Override
            public void run(BlockPosition pos) {
                if (!updatedRedstone.add(pos)) {
                    return;
                }
                addBlocksNeedingUpdate(world, pos, new RunnableVal<BlockPosition>() {
                    @Override
                    public void run(BlockPosition pos2) {
                        if (updatedRedstone.contains(pos2) || toUpdate.contains(pos2)) {
                            return;
                        }
                        pos2 = getImmutable(pos2);
                        toUpdate.add(pos2);
                    }
                });
            }
        });
        BlockPosition[] array = toUpdate.toArray(new BlockPosition[toUpdate.size()]);
        for (int i = array.length - 1; i >= 0; i--) {
            BlockPosition pos = array[i];
            if (updatedRedstone.add(pos)) {
                doPhysics(world, pos);
            }
        }
        if (--tick == 0) {
            updatedRedstone.clear();
        }
    }

    public void doPhysics(World world, BlockPosition pos) {
        world.e(pos, this);
    }
    
//    private void handleUpdate(World world, BlockPosition blockposition, IBlockData iblockdata) {
//        calculateCurrentChanges(world, blockposition, new RunnableVal<BlockPosition>() {
//            @Override
//            public void run(BlockPosition arg0) {
//                updatedRedstoneWire.add(arg0);
//            }
//        });
//
//        Set<BlockPosition> blocksNeedingUpdate = Sets.newLinkedHashSet();
//        for (Iterator localIterator = this.updatedRedstoneWire.iterator(); localIterator.hasNext();) {
//            BlockPosition posi = (BlockPosition) localIterator.next();
//            addBlocksNeedingUpdateOld(world, posi, blocksNeedingUpdate);
//        }
//        BlockPosition posi;
//        Object it = Lists.newLinkedList(this.updatedRedstoneWire).descendingIterator();
//        while (((Iterator) it).hasNext()) {
//            addAllSurroundingBlocksOld((BlockPosition) ((Iterator) it).next(), blocksNeedingUpdate);
//        }
//        blocksNeedingUpdate.removeAll(this.updatedRedstoneWire);
//
//        this.updatedRedstoneWire.clear();
//        for (BlockPosition pos2 : blocksNeedingUpdate) {
//            world.e(pos2, this);
//        }
//    }

    private BlockPosition add(MutableBlockPosition pos, BaseBlockPosition pos1, BaseBlockPosition pos2) {
        return pos.c(pos1.getX() + pos2.getX(), pos1.getY() + pos2.getY(), pos1.getZ() + pos2.getZ());
    }

    private BlockPosition subtract(MutableBlockPosition pos, BaseBlockPosition pos1, BaseBlockPosition pos2) {
        return pos.c(pos1.getX() - pos2.getX(), pos1.getY() - pos2.getY(), pos1.getZ() - pos2.getZ());
    }
    
    private void calculateCurrentChanges(World world, BlockPosition blockposition, RunnableVal<BlockPosition> onEach) {
        if (world.getType(blockposition).getBlock() == this) {
            this.turnOff.add(blockposition);
        } else {
            checkSurroundingWires(world, blockposition);
        }
        while (!this.turnOff.isEmpty()) {
            Iterator<BlockPosition> iter = turnOff.iterator();
            final BlockPosition pos = iter.next();
            iter.remove();
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
                onEach.run(pos);
            } else if (newPower > oldPower) {
                state = setWireState(world, pos, state, newPower);
                onEach.run(pos);
            }
            checkSurroundingWires(world, pos);
        }
        while (!this.turnOn.isEmpty()) {
            Iterator<BlockPosition> iter = turnOn.iterator();
            final BlockPosition pos = iter.next();
            iter.remove();
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
                onEach.run(pos);
            }
            checkSurroundingWires(world, pos);
        }
        this.turnOff.clear();
        this.turnOn.clear();
    }
    
    private void addWireToList(final World worldIn, final BlockPosition pos, final int otherPower) {
        final IBlockData state = worldIn.getType(pos);
        if (state.getBlock() == this) {
            int power = state.get(POWER).intValue();
            if (power < otherPower - 1) {
                this.turnOn.add(pos);
            }
            if (power > otherPower) {
                this.turnOff.add(pos);
            }
        }
    }
    
    private void checkSurroundingWires(final World worldIn, final BlockPosition pos) {
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
    
    private int getSurroundingWirePower(final World worldIn, final BlockPosition pos) {
        int wirePower = 0;
        for (EnumDirection enumfacing : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition offsetPos = shift(mpos2, pos, enumfacing, 1);
            
            wirePower = getPower(worldIn, offsetPos, wirePower);
            if ((worldIn.getType(offsetPos).l()) && (!worldIn.getType(pos.up()).l())) {
                wirePower = getPower(worldIn, offsetPos.up(), wirePower);
            } else if (!worldIn.getType(offsetPos).l()) {
                wirePower = getPower(worldIn, offsetPos.down(), wirePower);
            }
        }
        return wirePower;
    }

//    private void addBlocksNeedingUpdate(final World worldIn, final BlockPosition pos, RunnableVal<BlockPosition> onEach) {
//        for (final EnumDirection facing : Wire.facings) {
//            final BlockPosition offsetPos = pos.shift(facing);
//            boolean shouldPower = facing == EnumDirection.DOWN || shouldPower(worldIn, pos, facing.opposite());
//            if (shouldPower) {
//                if ((facing.k().c() && this.canBlockBePoweredFromSide(worldIn.getType(offsetPos), facing, true))) {
//                    onEach.run(offsetPos);
//                }
//            }
//        }
//    }
    private void addBlocksNeedingUpdate(final World worldIn, final BlockPosition pos, RunnableVal<BlockPosition> onEach) {
        for (BaseBlockPosition facing : Wire.surroundingBlocksOffset) {
            BlockPosition offsetPos = add(mpos3, pos, facing);
            if (offsetPos.getY() >= 0 && offsetPos.getY() < 256) {
                if (canBlockBePowered(worldIn.getType(offsetPos))) {
                    onEach.run(offsetPos);
                }
            }
        }
    }

    public boolean canBlockBePowered(IBlockData state) {
        Block block = state.getBlock();
        switch (Block.getId(block)) {
            case 23: // dispensor
            case 158: // dropper
            case 25:
            // piston
            case 29:
            case 33:
//                return state.get(BlockPiston.FACING) != side.opposite();
            // tnt
            case 44:
            // door
            case 96: // trapdoor
            case 167:
            case 107: // fence
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 64: // door
            case 71:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            // diode
            case 93:
            case 94:
            // torch
            case 75:
            case 76:
            // comparator
            case 149:
            case 150:
            // lamp
            case 123:
            case 124:
            // rail
            case 27:
            // BUD
            case 73: // ore
            case 74:
            case 8: // water
            case 9:
            case 34: // piston
                return true;
            default:
                return false;
        }
    }

    private boolean canBlockBePoweredFromSide(IBlockData state, EnumDirection side, boolean isWire) {
        Block block = state.getBlock();
        switch (Block.getId(block)) {
            case 23: // dispensor
            case 158: // dropper
                return true;
                // note block
            case 25:
                return true;
                // piston
            case 29:
            case 33:
                return true;
//                return state.get(BlockPiston.FACING) != side.opposite();
                // tnt
            case 44:
                return true;
                // door
            case 96: // trapdoor
            case 167:
            case 107: // fence
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 64: // door
            case 71:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
                return true;
                // diode
            case 93:
            case 94:
//                if (state.get(BlockDiodeAbstract.FACING) != side.opposite()) {
//                    return true;
//                }
                return true;
                // torch
            case 75:
            case 76:
//                if (!isWire && state.get(BlockRedstoneTorch.FACING) == side) {
//                    return true;
//                }
//                break;
                return true;
                // comparator
            case 149:
            case 150:
//                if (state.get(BlockDiodeAbstract.FACING) != side.opposite() && isWire && (state.get(BlockRedstoneComparator.FACING).k() != side.k()) && (side.k().c())) {
//                    return true;
//                }
//                break;
                return true;
                // lamp
            case 123:
            case 124:
                return true;
                // rail
            case 27:
                return true;
                // BUD
            case 73: // ore
            case 74:
            case 8: // water
            case 9:
            case 34: // piston
                return true;
            default:
                return false;
        }
//        return false;
    }
    
    private IBlockData setWireState(final World worldIn, final BlockPosition pos, IBlockData state, final int power) {
        state = state.set((IBlockState) Wire.POWER, (Comparable) power);
        provider.update(worldIn, pos, state, Wire.this, false, 0);
        return state;
    }

    private static MutableBlockPosition mpos1 = new MutableBlockPosition();
    private static MutableBlockPosition mpos2 = new MutableBlockPosition();
    private static MutableBlockPosition mpos3 = new MutableBlockPosition();

    private static BlockPosition shift(BaseBlockPosition blockPosition, EnumDirection dir, int amount) {
        return shift(mpos1, blockPosition, dir, amount);
    }

    private static BlockPosition shift(MutableBlockPosition pos, BaseBlockPosition blockPosition, EnumDirection dir, int amount) {
        if (dir == null) {
            return pos.c(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
        }
        switch (dir) {
            case DOWN:
                pos.c(blockPosition.getX(), blockPosition.getY() - amount, blockPosition.getZ());
                return pos;
            case EAST:
                pos.c(blockPosition.getX() + amount, blockPosition.getY(), blockPosition.getZ());
                return pos;
            case NORTH:
                pos.c(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ() - amount);
                return pos;
            case SOUTH:
                pos.c(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ() + amount);
                return pos;
            case UP:
                pos.c(blockPosition.getX(), blockPosition.getY() + amount, blockPosition.getZ());
                return pos;
            case WEST:
                pos.c(blockPosition.getX() - amount, blockPosition.getY(), blockPosition.getZ());
                return pos;
        }
        return pos;
    }

    private boolean c(IBlockAccess iblockaccess, BlockPosition blockposition, EnumDirection enumdirection) {
        BlockPosition blockposition1 = shift(blockposition, enumdirection, 1);
        IBlockData iblockdata = iblockaccess.getType(blockposition1);
        boolean flag = iblockdata.l();
        boolean flag1 = iblockaccess.getType(blockposition.up()).l();
        return (!flag1) && (flag) && (c(iblockaccess, shift(blockposition1, EnumDirection.UP, 1)));
    }
    
    private boolean shouldPower(World worldIn, BlockPosition pos, EnumDirection enumdirection) {
        switch (enumdirection) {
            case DOWN:
            case UP:
                return false;
            case NORTH:
                return (c(worldIn, pos, EnumDirection.NORTH) && !c(worldIn, pos, EnumDirection.EAST) && !c(worldIn, pos, EnumDirection.WEST))
                || (!c(worldIn, pos, EnumDirection.EAST) && !c(worldIn, pos, EnumDirection.WEST) && !c(worldIn, pos, EnumDirection.SOUTH));
            case SOUTH:
                return (c(worldIn, pos, EnumDirection.SOUTH) && !c(worldIn, pos, EnumDirection.EAST) && !c(worldIn, pos, EnumDirection.WEST))
                || (!c(worldIn, pos, EnumDirection.EAST) && !c(worldIn, pos, EnumDirection.WEST) && !c(worldIn, pos, EnumDirection.NORTH));
            case EAST:
                return (c(worldIn, pos, EnumDirection.EAST) && !c(worldIn, pos, EnumDirection.NORTH) && !c(worldIn, pos, EnumDirection.SOUTH))
                || (!c(worldIn, pos, EnumDirection.NORTH) && !c(worldIn, pos, EnumDirection.SOUTH) && !c(worldIn, pos, EnumDirection.WEST));
            case WEST:
                return (c(worldIn, pos, EnumDirection.WEST) && !c(worldIn, pos, EnumDirection.NORTH) && !c(worldIn, pos, EnumDirection.SOUTH))
                || (!c(worldIn, pos, EnumDirection.NORTH) && !c(worldIn, pos, EnumDirection.SOUTH) && !c(worldIn, pos, EnumDirection.EAST));
        }
        return false;
    }

    @Override
    public void onPlace(final World world, final BlockPosition blockposition, final IBlockData iblockdata) {
        this.handleUpdate(world, blockposition, iblockdata);
        for (final EnumDirection enumdirection : EnumDirection.EnumDirectionLimit.VERTICAL) {
            world.applyPhysics(blockposition.shift(enumdirection), this);
        }
        for (final EnumDirection enumdirection : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            this.b(world, blockposition.shift(enumdirection));
        }
        for (final EnumDirection enumdirection : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            final BlockPosition blockposition2 = blockposition.shift(enumdirection);
            if (world.getType(blockposition2).l()) {
                this.b(world, blockposition2.up());
            } else {
                this.b(world, blockposition2.down());
            }
        }
    }
    
    @Override
    public void remove(final World world, final BlockPosition blockposition, final IBlockData iblockdata) {
        super.remove(world, blockposition, iblockdata);
        for (final EnumDirection enumdirection : EnumDirection.values()) {
            world.applyPhysics(blockposition.shift(enumdirection), this);
        }
        this.handleUpdate(world, blockposition, iblockdata);
        for (final EnumDirection enumdirection2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            this.b(world, blockposition.shift(enumdirection2));
        }
        for (final EnumDirection enumdirection2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            final BlockPosition blockposition2 = blockposition.shift(enumdirection2);
            if (world.getType(blockposition2).l()) {
                this.b(world, blockposition2.up());
            } else {
                this.b(world, blockposition2.down());
            }
        }
    }
    
    @Override
    public void a(final IBlockData iblockdata, final World world, final BlockPosition blockposition, final Block block) {
        if (this.canPlace(world, blockposition)) {
            this.handleUpdate(world, blockposition, iblockdata);
        } else {
            this.b(world, blockposition, iblockdata, 0);
            world.setAir(blockposition);
        }
    }
    
    @Override
    public int b(final IBlockData iblockdata, final IBlockAccess iblockaccess, final BlockPosition blockposition, final EnumDirection enumdirection) {
        if (!this.g) {
            return 0;
        }
        final int i = (int) iblockdata.get((IBlockState) BlockRedstoneWire.POWER);
        if (i == 0) {
            return 0;
        }
        if (enumdirection == EnumDirection.UP) {
            return i;
        }
        if (shouldPower((World) iblockaccess, blockposition, enumdirection)) {
            return i;
        }
        return 0;
    }
    
    static {
        facingsHorizontal = new EnumDirection[] { EnumDirection.WEST, EnumDirection.EAST, EnumDirection.NORTH, EnumDirection.SOUTH };
        facingsVertical = new EnumDirection[] { EnumDirection.DOWN, EnumDirection.UP };
        facings = (EnumDirection[]) ArrayUtils.addAll((Object[]) Wire.facingsVertical, (Object[]) Wire.facingsHorizontal);
        final Set<BaseBlockPosition> set = new LinkedHashSet<>();
        set.add(new BlockPosition(1, 0, 0));
        set.add(new BlockPosition(-1, 0, 0));
        set.add(new BlockPosition(0, 1, 0));
        set.add(new BlockPosition(0, -1, 0));
        set.add(new BlockPosition(1, 1, 0));
        set.add(new BlockPosition(-1, 1, 0));
        set.add(new BlockPosition(1, -1, 0));
        set.add(new BlockPosition(-1, -1, 0));
        set.add(new BlockPosition(0, 1, 1));
        set.add(new BlockPosition(0, 1, -1));
        set.add(new BlockPosition(0, -1, 1));
        set.add(new BlockPosition(0, -1, -1));
        set.add(new BlockPosition(0, 0, 1));
        set.add(new BlockPosition(0, 0, -1));
        surroundingBlocksOffset = set.<BaseBlockPosition> toArray(new BaseBlockPosition[set.size()]);
    }
}