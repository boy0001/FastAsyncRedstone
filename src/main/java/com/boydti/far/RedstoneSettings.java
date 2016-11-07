package com.boydti.far;

import java.io.File;

import com.boydti.fawe.config.Config;
import com.boydti.fawe.config.Settings.HISTORY;
import com.sk89q.worldedit.LocalSession;

public class RedstoneSettings extends Config {
    public static void save(File file) {
        save(file, RedstoneSettings.class);
    }
    
    public static void load(File file) {
        load(file, RedstoneSettings.class);
        if (HISTORY.USE_DISK) {
            LocalSession.MAX_HISTORY_SIZE = Integer.MAX_VALUE;
        }
    }
}
