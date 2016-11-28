package com.boydti.far;

import java.lang.reflect.Field;
import org.bukkit.World;
import org.bukkit.event.EventException;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.RegisteredListener;

public class MutableBlockRedstoneEvent extends BlockRedstoneEvent {

    public static MutableBlockRedstoneEvent INSTANCE;

    static {
        try {
            INSTANCE = new MutableBlockRedstoneEvent();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private RegisteredListener[] listeners;
    private final Field fieldBlock;
    private int oldCurrent;
    private int newCurrent;

    public MutableBlockRedstoneEvent() throws NoSuchFieldException {
        super(null, 0, 0);
        recalculateListeners();
        this.fieldBlock = BlockEvent.class.getDeclaredField("block");
        ReflectionUtil.setAccessible(fieldBlock);
    }

    public void call(World world, int x, int y, int z, int oldCurrent, int newCurrent) {
        if (listeners.length == 0) {
            return;
        }
        try {
            this.oldCurrent = oldCurrent;
            this.newCurrent = newCurrent;
            this.fieldBlock.set(this, world.getBlockAt(x, y, z));
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
