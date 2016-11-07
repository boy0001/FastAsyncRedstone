package com.boydti.far;

import com.boydti.far.blocks.Lamp;
import com.boydti.far.blocks.Piston;
import com.boydti.far.blocks.Torch;
import com.boydti.far.blocks.Wire;
import com.boydti.fawe.FaweVersion;
import com.google.common.collect.UnmodifiableIterator;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.Blocks;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.MinecraftKey;
import org.bukkit.plugin.java.JavaPlugin;

public class FarMain extends JavaPlugin {
    
    private QueueManager provider;

    @Override
    public void onEnable() {
        setupConfig();

        this.provider = new QueueManager();

        if (RedstoneSettings.OPTIMIZE_DEVICES.REDSTONE_WIRE) {
            add(55, "redstone_wire", new Wire(provider));
            ReflectionUtil.setStatic("REDSTONE_WIRE", Blocks.class, get("redstone_wire"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.REDSTONE_LAMP) {
            add(123, "redstone_lamp", new Lamp(false, provider));
            ReflectionUtil.setStatic("REDSTONE_LAMP", Blocks.class, get("redstone_lamp"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.LIT_REDSTONE_LAMP) {
            add(124, "lit_redstone_lamp", new Lamp(true, provider));
            ReflectionUtil.setStatic("LIT_REDSTONE_LAMP", Blocks.class, get("lit_redstone_lamp"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.STICKY_PISTON) {
            add(29, "sticky_piston", new Piston(true, provider));
            ReflectionUtil.setStatic("STICKY_PISTON", Blocks.class, get("sticky_piston"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.PISTON) {
            add(33, "piston", new Piston(false, provider));
            ReflectionUtil.setStatic("PISTON", Blocks.class, get("piston"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.UNLIT_REDSTONE_TORCH) {
            add(75, "unlit_redstone_torch", new Torch(provider, false));
            ReflectionUtil.setStatic("UNLIT_REDSTONE_TORCH", Blocks.class, get("unlit_redstone_torch"));
        }
        if (RedstoneSettings.OPTIMIZE_DEVICES.REDSTONE_TORCH) {
            add(76, "redstone_torch", new Torch(provider, true));
            ReflectionUtil.setStatic("REDSTONE_TORCH", Blocks.class, get("redstone_torch"));
        }

    }

    public void setupConfig() {
        File file = new File(getDataFolder(), "config.yml");
        RedstoneSettings.load(file);
        RedstoneSettings.save(file);
        RedstoneSettings.PLATFORM = "bukkit";
        try {
            InputStream stream = getClass().getResourceAsStream("/fawe.properties");
            java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
            String versionString = scanner.next().trim();
            scanner.close();
            FaweVersion version = new FaweVersion(versionString);
            RedstoneSettings.DATE = new Date(100 + version.year, version.month, version.day).toGMTString();
            RedstoneSettings.BUILD = "http://ci.athion.net/job/FastAsyncRedstone/" + version.build;
            RedstoneSettings.COMMIT = "https://github.com/boy0001/FastAsyncWorldedit/commit/" + Integer.toHexString(version.hash);
        } catch (Throwable ignore) {}
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
