package com.boydti.far;

import com.boydti.fawe.config.Config;
import java.io.File;

public class RedstoneSettings extends Config {
    @Comment("These first 6 aren't configurable") // This is a comment
    @Final // Indicates that this value isn't configurable
    public static final String ISSUES = "https://github.com/boy0001/FastAsyncRedstone/issues";
    @Final
    public static final String WIKI = "https://github.com/boy0001/FastAsyncRedstone/wiki/";
    @Final
    public static String DATE = null; // These values are set from FAWE before loading
    @Final
    public static String BUILD = null; // These values are set from FAWE before loading
    @Final
    public static String COMMIT = null; // These values are set from FAWE before loading
    @Final
    public static String PLATFORM = null; // These values are set from FAWE before loading

    @Comment("Configure how the async redstone queue works")
    public static class QUEUE {
        @Comment({
        "Configure the interval for the async queue",
        " - Lighting changes are calculated",
        " - Bulk packets are sent",
        " - Increase to reduce CPU usage",
        " - Lower to see changes sooner",
        })
        public static int INTERVAL = 19;
    }

    public static class OPTIMIZE_DEVICES {
        public static boolean REDSTONE_WIRE = true;
        public static boolean REDSTONE_LAMP = true;
        public static boolean LIT_REDSTONE_LAMP = true;
        public static boolean STICKY_PISTON = true;
        public static boolean PISTON = true;
    }

    public static void save(File file) {
        save(file, RedstoneSettings.class);
    }
    
    public static void load(File file) {
        load(file, RedstoneSettings.class);
    }
}
