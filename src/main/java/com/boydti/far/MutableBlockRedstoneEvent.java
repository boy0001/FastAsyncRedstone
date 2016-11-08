package com.boydti.far;

import java.lang.reflect.Field;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.World;
import org.bukkit.craftbukkit.v1_10_R1.block.CraftBlock;
import org.bukkit.event.EventException;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.RegisteredListener;

public class MutableBlockRedstoneEvent extends BlockRedstoneEvent {

    private RegisteredListener[] listeners;
    private final CraftBlock mutableBlock;
    private final Field fieldBlock;
    private int oldCurrent;
    private int newCurrent;

    public MutableBlockRedstoneEvent() throws NoSuchFieldException {
        super(null, 0, 0);
        recalculateListeners();
        this.mutableBlock = new CraftBlock(null, 0, 0, 0);
        this.fieldBlock = BlockEvent.class.getDeclaredField("block");
        ReflectionUtil.setAccessible(fieldBlock);
    }

    public void call(World world, BlockPosition pos, int oldCurrent, int newCurrent) {
        if (listeners.length == 0) {
            return;
        }
        try {
            this.oldCurrent = oldCurrent;
            this.newCurrent = newCurrent;
            this.fieldBlock.set(this, world.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ()));
            for (RegisteredListener listener : listeners) {
                try {
                    listener.callEvent(this);
                } catch (EventException e) {
                    e.printStackTrace();
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public int getOldCurrent() {
        return this.oldCurrent;
    }

    public int getNewCurrent() {
        return this.newCurrent;
    }

    public void setNewCurrent(int newCurrent) {
        this.newCurrent = newCurrent;
    }

    public void recalculateListeners() {
        this.listeners = BlockRedstoneEvent.getHandlerList().getRegisteredListeners();
    }
}
