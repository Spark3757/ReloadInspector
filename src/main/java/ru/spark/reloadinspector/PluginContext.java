package ru.spark.reloadinspector;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Utility class for getting file of plugins.
 */
@UtilityClass
class PluginContext {

    private static final Field FILE_FIELD = lookupFileField();

    static Plugin fromFile(@NonNull File file) {
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (Objects.equal(getFile(plugin).getName(), file.getName())) {
                return plugin;
            }
        }
        return null;
    }

    private static File getFile(@NonNull Plugin plugin) {
        Preconditions.checkState(JavaPlugin.class.isInstance(plugin), "Plugin must be implemented as JavaPlugin.");

        try {
            return (File) FILE_FIELD.get(plugin);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot get file of " + plugin.getName(), e);
        }
    }

    private static Field lookupFileField() {
        for (Field field : JavaPlugin.class.getDeclaredFields()) {
            if (field.getName().equals("file")) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalStateException("Cannot find `file` field in JavaPlugin.");
    }
}
