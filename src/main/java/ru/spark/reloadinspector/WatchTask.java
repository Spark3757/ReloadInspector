package ru.spark.reloadinspector;

import lombok.NonNull;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Represents async runnable task that uses {@link WatchService}.
 */
final class WatchTask implements Runnable, AutoCloseable {


    private final WatchService watcher;
    private final ReloadInspector plugin;

    WatchTask(@NonNull Path dir, @NonNull ReloadInspector plugin) throws IOException {
        this.watcher = dir.getFileSystem().newWatchService();
        this.plugin = plugin;

        //Register directory
        dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY); //Subscribe to entry modifying.
    }

    @Override
    public void run() {
        try {
            processLoop();
        } catch (InterruptedException e) {
            //Ignore
        }
    }

    private void processLoop() throws InterruptedException {
        while (!Thread.interrupted()) {
            processKey(watcher.take());
        }
    }

    private void processKey(WatchKey key) {
        Path watchDir = (Path) key.watchable();

        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                Path path = watchDir.resolve((Path) event.context());
                File file = path.toFile();

                if (!file.getName().endsWith(".jar")) continue; //Ensure file has jar extension.

                Plugin modifiedPlugin = PluginContext.fromFile(file);

                if (modifiedPlugin == null || plugin.hasExcluded(modifiedPlugin)) continue;

                plugin.notifyChanges(modifiedPlugin);
            }
        }
        //Don't forget to reset the key.
        key.reset();
    }

    @Override
    public void close() throws IOException {
        this.watcher.close();
    }
}
