package ru.spark.reloadinspector;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;

public final class ReloadInspector extends JavaPlugin {

    private Thread watchThread;
    private WatchTask watchTask;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            getLogger().info("Starting watch thread...");
            watchTask = new WatchTask(Paths.get("plugins"), this);
            watchThread = new Thread(watchTask, "Plugin Watcher Thread");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            //Its bad
            getLogger().log(Level.SEVERE, "Cannot startup plugin. Notify developer.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    boolean hasExcluded(@NonNull Plugin plugin) {
        return getConfig().getStringList("exclude").contains(plugin.getName());
    }

    void notifyChanges(@NonNull Plugin plugin) {
        //Notify user
        getLogger().log(Level.INFO, "We detected changes in plugin {0}.", plugin.getName());

        //Schedule in main thread.
        getServer().getScheduler().runTask(this, () -> {
            if (getConfig().getBoolean("full_reload")) {
                Bukkit.reload(); //Reload server
            } else {
                //Disable and enable plugin
                PluginManager pm = getServer().getPluginManager();
                pm.disablePlugin(plugin);
                pm.enablePlugin(plugin);
            }
        });
    }

    @Override
    public void onDisable() {
        if (watchThread != null && watchTask != null) {
            getLogger().info("Stopping watch thread...");
            watchThread.interrupt();
            try {
                watchTask.close();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Cannot close WatchService", e);
            }
        }
    }
}
