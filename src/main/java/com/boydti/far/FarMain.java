package com.boydti.far;

import java.io.File;

import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.Blocks;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.MinecraftKey;

import org.bukkit.plugin.java.JavaPlugin;

import com.boydti.far.blocks.Lamp;
import com.boydti.far.blocks.Piston;
import com.boydti.far.blocks.Wire;
import com.google.common.collect.UnmodifiableIterator;

public class FarMain extends JavaPlugin {
    
    private QueueManager provider;

    @Override
    public void onEnable() {
        RedstoneSettings.load(new File(getDataFolder(), "config.yml"));
        provider = new QueueManager();
        add(55, "redstone_wire", new Wire(provider));
        add(123, "redstone_lamp", new Lamp(false, provider));
        add(124, "lit_redstone_lamp", new Lamp(true, provider));
        add(29, "sticky_piston", new Piston(true, provider).c("pistonStickyBase"));
        add(33, "piston", new Piston(false, provider).c("pistonBase"));
        
        // sticky_piston
        // piston

        ReflectionUtil.setStatic("REDSTONE_WIRE", Blocks.class, get("redstone_wire"));
        ReflectionUtil.setStatic("REDSTONE_LAMP", Blocks.class, get("redstone_lamp"));
        ReflectionUtil.setStatic("LIT_REDSTONE_LAMP", Blocks.class, get("lit_redstone_lamp"));
        ReflectionUtil.setStatic("STICKY_PISTON", Blocks.class, get("sticky_piston"));
        ReflectionUtil.setStatic("PISTON", Blocks.class, get("piston"));
    }
    
    private static Block get(String s) {
        return Block.REGISTRY.get(new MinecraftKey(s));
    }
    
    private static void add(int index, String name, Block block) {
        Block.REGISTRY.a(index, new MinecraftKey(name), block);
        for (UnmodifiableIterator localUnmodifiableIterator = block.t().a().iterator(); localUnmodifiableIterator.hasNext();) {
            IBlockData iblockdata = (IBlockData) localUnmodifiableIterator.next();
            int k = Block.REGISTRY.a(block) << 4 | block.toLegacyData(iblockdata);
            Block.REGISTRY_ID.a(iblockdata, k);
        }
    }
}
