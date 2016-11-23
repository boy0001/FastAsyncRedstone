package com.boydti.far.v111.blocks;

import com.boydti.far.FarMain;
import com.boydti.far.MutableBlockRedstoneEvent;
import com.boydti.far.ReflectionUtil;
import com.boydti.far.v111.QueueManager111;
import com.boydti.fawe.object.RunnableVal;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.server.v1_11_R1.BaseBlockPosition;
import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.BlockPosition.MutableBlockPosition;
import net.minecraft.server.v1_11_R1.BlockRedstoneWire;
import net.minecraft.server.v1_11_R1.Chunk;
import net.minecraft.server.v1_11_R1.EnumDirection;
import net.minecraft.server.v1_11_R1.IBlockAccess;
import net.minecraft.server.v1_11_R1.IBlockData;
import net.minecraft.server.v1_11_R1.IBlockState;
import net.minecraft.server.v1_11_R1.SoundEffectType;
import net.minecraft.server.v1_11_R1.World;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;

public class Wire extends BlockRedstoneWire {
    private final LinkedHashSet<BlockPosition> turnOff;
    private final LinkedHashSet<BlockPosition> turnOn;
    private final LinkedHashSet<BlockPosition> updatedRedstone;
    private static final EnumDirection[] facingsHorizontal;
    private static final EnumDirection[] facingsVertical;
    private static final EnumDirection[] facings;
    private static final BaseBlockPosition[] surroundingBlocksOffset;
    private boolean g;
    private final QueueManager111 provider;
    private final MutableBlockRedstoneEvent event;

    public Wire(QueueManager111 provider) throws NoSuchFieldException {
        this.provider = provider;
        this.turnOff = new LinkedHashSet<>();
        this.turnOn = new LinkedHashSet<>();
        this.updatedRedstone = new LinkedHashSet<>();
        this.g = true;
        this.c(0.0f);
        this.a(SoundEffectType.d);
        this.c("redstoneDust");
        this.p();
        this.event = new MutableBlockRedstoneEvent();
        Bukkit.getServer().getScheduler().runTask(FarMain.get(), new Runnable() {
            @Override
            public void run() {
                Wire.this.event.recalculateListeners();
            }
        });
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
        world.a(pos.west(), this, pos);
        world.a(pos.east(), this, pos);
        world.a(pos.down(), this, pos);
        world.a(pos.up(), this, pos);
        world.a(pos.north(), this, pos);
        world.a(pos.south(), this, pos);
    }
    
    private BlockPosition add(MutableBlockPosition pos, BaseBlockPosition pos1, BaseBlockPosition pos2) {
        return pos.c(pos1.getX() + pos2.getX(), pos1.getY() + pos2.getY(), pos1.getZ() + pos2.getZ());
    }

    private BlockPosition subtract(MutableBlockPosition pos, BaseBlockPosition pos1, BaseBlockPosition pos2) {
        return pos.c(pos1.getX() - pos2.getX(), pos1.getY() - pos2.getY(), pos1.getZ() - pos2.getZ());
    }

    private void calculateCurrentChanges(World world, BlockPosition blockposition, RunnableVal<BlockPosition> onEach) {
        if (provider.getTypeFast(world, blockposition).getBlock() == this) {
            this.turnOff.add(blockposition);
        } else {
            checkSurroundingWires(world, blockposition);
        }
        while (!this.turnOff.isEmpty()) {
            Iterator<BlockPosition> iter = turnOff.iterator();
            final BlockPosition pos = iter.next();
            iter.remove();
            IBlockData state = provider.getTypeFast(world, pos);
            int oldPower = state.get(POWER).intValue();
            this.g = false;
            int blockPower = world.z(pos);
            this.g = true;
            int wirePower = getSurroundingWirePower(world, pos);
            
            wirePower--;
            int newPower = Math.max(blockPower, wirePower);
            if (oldPower != newPower) {
                this.event.call(world.getWorld(), pos.getX(), pos.getY(), pos.getZ(), oldPower, newPower);
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
            IBlockData state = provider.getTypeFast(world, pos);
            int oldPower = state.get(POWER).intValue();
            this.g = false;
            int blockPower = world.z(pos);
            this.g = true;
            int wirePower = getSurroundingWirePower(world, pos);
            
            wirePower--;
            int newPower = Math.max(blockPower, wirePower);
            if (oldPower != newPower) {
                this.event.call(world.getWorld(), pos.getX(), pos.getY(), pos.getZ(), oldPower, newPower);
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
            boolean solidBlock = worldIn.getType(offsetPos).l();
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

    private Chunk cachedChunk;

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

    private IBlockData setWireState(final World worldIn, final BlockPosition pos, IBlockData state, final int power) {
        state = state.set((IBlockState) Wire.POWER, (Comparable) power);
        provider.updateBlockFast(worldIn, pos, state, Wire.this, false, 0);
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
            world.applyPhysics(blockposition.shift(enumdirection), this, false);
        }
        for (final EnumDirection enumdirection : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            this.b(world, blockposition.shift(enumdirection));
        }
        for (final EnumDirection enumdirection : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            final BlockPosition blockposition2 = blockposition.shift(enumdirection);
            if (provider.getTypeFast(world, blockposition2).l()) {
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
            world.applyPhysics(blockposition.shift(enumdirection), this, false);
        }
        this.handleUpdate(world, blockposition, iblockdata);
        for (final EnumDirection enumdirection2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            this.b(world, blockposition.shift(enumdirection2));
        }
        for (final EnumDirection enumdirection2 : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            final BlockPosition blockposition2 = blockposition.shift(enumdirection2);
            if (provider.getTypeFast(world, blockposition2).l()) {
                this.b(world, blockposition2.up());
            } else {
                this.b(world, blockposition2.down());
            }
        }
    }
    
    @Override
    public void a(final IBlockData iblockdata, final World world, final BlockPosition blockposition, final Block block, BlockPosition blockposition1) {
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
//        set.add(new BlockPosition(1, 0, 0));
//        set.add(new BlockPosition(-1, 0, 0));
//        set.add(new BlockPosition(0, 1, 0));
//        set.add(new BlockPosition(0, -1, 0));
//        set.add(new BlockPosition(1, 1, 0));
//        set.add(new BlockPosition(-1, 1, 0));
//        set.add(new BlockPosition(1, -1, 0));
//        set.add(new BlockPosition(-1, -1, 0));
//        set.add(new BlockPosition(0, 1, 1));
//        set.add(new BlockPosition(0, 1, -1));
//        set.add(new BlockPosition(0, -1, 1));
//        set.add(new BlockPosition(0, -1, -1));
//        set.add(new BlockPosition(0, 0, 1));
//        set.add(new BlockPosition(0, 0, -1));
        Set<BaseBlockPosition> set2 = new LinkedHashSet<>();
        for (final EnumDirection facing : Wire.facings) {
            set.add(ReflectionUtil.<BaseBlockPosition> getOfT((Object) facing, BaseBlockPosition.class));
        }
        for (final EnumDirection facing2 : Wire.facings) {
            final BaseBlockPosition v1 = ReflectionUtil.<BaseBlockPosition> getOfT(facing2, BaseBlockPosition.class);
            set.add(v1);
        }
        set.remove(BlockPosition.ZERO);
        surroundingBlocksOffset = set.<BaseBlockPosition> toArray(new BaseBlockPosition[set.size()]);
    }
}