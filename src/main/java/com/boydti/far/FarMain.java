package com.boydti.far;

import com.boydti.far.v110.QueueManager110;
import com.boydti.far.v111.QueueManager111;
import com.boydti.far.v183.QueueManager183;
import com.boydti.fawe.FaweVersion;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import org.bukkit.plugin.java.JavaPlugin;

public class FarMain extends JavaPlugin {
    
    private static QueueManager provider;
    private static FarMain instance;

    public static FarMain get() {
        return instance;
    }

    public FarMain() {
        instance = this;
    }

    static {
        setupConfig();
        try {
            provider = new QueueManager111();
        } catch (Throwable ignore) {}
        try {
            provider = new QueueManager110();
        } catch (Throwable ignore) {}
        try {
            provider = new QueueManager183();
        } catch (Throwable ignore) {}
    }

    @Override
    public void onEnable() {

    }

    public static void setupConfig() {
        File file = new File("plugins/FastAsyncRedstone/config.yml");
        RedstoneSettings.load(file);
        RedstoneSettings.save(file);
        RedstoneSettings.PLATFORM = "bukkit";
        try {
            InputStream stream = FarMain.class.getResourceAsStream("/fawe.properties");
            java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
            String versionString = scanner.next().trim();
            scanner.close();
            FaweVersion version = new FaweVersion(versionString);
            RedstoneSettings.DATE = new Date(100 + version.year, version.month, version.day).toGMTString();
            RedstoneSettings.BUILD = "http://ci.athion.net/job/FastAsyncRedstone/" + version.build;
            RedstoneSettings.COMMIT = "https://github.com/boy0001/FastAsyncWorldedit/commit/" + Integer.toHexString(version.hash);
        } catch (Throwable ignore) {}
    }
}
