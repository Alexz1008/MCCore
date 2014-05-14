package com.rit.sucy.config;

import com.rit.sucy.MCCore;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handles configs for files other than the default config.yml
 *
 * Slightly modified version of the one from the bukkit tutorial
 * Source: http://wiki.bukkit.org/Configuration_API_Reference
 */
public class Config {

    private final HashMap<ISavable, String> savables = new HashMap<ISavable, String>();
    private final String fileName;
    private final JavaPlugin plugin;

    private File configFile;
    private FileConfiguration fileConfiguration;

    /**
     * Constructor
     *
     * @param plugin plugin reference
     * @param name   file name
     */
    public Config(JavaPlugin plugin, String name) {
        this.plugin = plugin;
        this.fileName = name + ".yml";

        // Setup the path
        this.configFile = new File(plugin.getDataFolder().getAbsolutePath() + "/" + fileName);
        try {
            String path = configFile.getAbsolutePath();
            if (new File(path.substring(0, path.lastIndexOf(File.separator))).mkdirs())
                plugin.getLogger().info("Created a new folder for config files");
        }
        catch (Exception e) { /* */ }

        // Register for auto-saving
        ((MCCore) plugin.getServer().getPluginManager().getPlugin("MCCore")).registerConfig(this);
    }

    /**
     * @return plugin owning this config file
     */
    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * @return name of the file this config saves to
     */
    public String getFile() {
        return fileName.replace(".yml", "");
    }

    /**
     * Saves if there are savables added
     */
    public void save() {
        if (savables.size() > 0) {
            for (Map.Entry<ISavable, String> entry : savables.entrySet()) {
                entry.getKey().save(getConfig(), entry.getValue());
            }
        }
        saveConfig();
    }

    /**
     * Adds a savable object to the config for automatic saving
     *
     * @param savable  savable object
     * @param basePath base path for it
     */
    public void addSavable(ISavable savable, String basePath) {
        this.savables.put(savable, basePath);
    }

    /**
     * Deletes the savable from the config
     *
     * @param savable savable to delete
     */
    public void deleteSavable(ISavable savable) {
        if (savables.containsKey(savable)) {
            String base = savables.get(savable);
            if (base.length() > 0 && base.charAt(base.length() - 1) == '.')
                base = base.substring(0, base.length() - 1);
            getConfig().set(base, null);
            savables.remove(savable);
        }
    }

    /**
     * Reloads the config
     */
    public void reloadConfig() {
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            fileConfiguration.setDefaults(defConfig);
        }
    }

    /**
     * @return config file
     */
    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            this.reloadConfig();
        }
        return fileConfiguration;
    }

    /**
     * <p>Retrieves the file of the configuration</p>
     *
     * @return the file of the configuration
     */
    public File getConfigFile() {
        return configFile;
    }

    /**
     * Saves the config
     */
    public void saveConfig() {
        if (fileConfiguration != null || configFile != null) {
            try {
                getConfig().save(configFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
            }
        }
    }

    /**
     * Saves the default config if no file exists yet
     */
    public void saveDefaultConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder().getAbsolutePath() + "/" + fileName);
        }
        if (!configFile.exists()) {
            this.plugin.saveResource(fileName, false);
        }
    }

    /**
     * <p>Checks the configuration for default values, copying
     * default values over if they are not set. Once finished,
     * the config is saved so the user can see the changes.</p>
     * <p>This acts differently than saveDefaultConfig() as
     * the config can already exist for this method. This is
     * more for making sure users do not erase needed values
     * from settings configs.</p>
     */
    public void checkDefaults() {
        ConfigurationSection config = getConfig();
        setDefaults(config);
        if (config.getDefaultSection() != null) saveConfig();
    }

    /**
     * <p>Trims excess (non-default) values from the configuration</p>
     * <p>Any values that weren't in the default configuration are removed</p>
     * <p>This is primarily used for settings configs </p>
     */
    public void trim() {
        ConfigurationSection config = getConfig();
        trim(config);
        saveConfig();
    }

    /**
     * <p>A recursive method to set the defaults of the config</p>
     * <p>Only defaults that aren't set in the current config are
     * copied over so that user changes aren't overwritten.</p>
     *
     * @param config config section to set the defaults for
     */
    public static void setDefaults(ConfigurationSection config) {
        if (config.getDefaultSection() != null) {
            for (String key : config.getDefaultSection().getKeys(false)) {

                // Recursively set the defaults for the inner sections
                if (config.isConfigurationSection(key)) {
                    setDefaults(config.getConfigurationSection(key));
                }

                // Set each default value if not set already
                else if (!config.isSet(key)) {
                    config.set(key, config.get(key));
                }
            }
        }
    }

    /**
     * <p>A recursive method to trim the non-default values from a config</p>
     * <p>This is for clearing unnecessary values from settings configs</p>
     *
     * @param config configuration section to trim
     */
    public static void trim(ConfigurationSection config) {
        if (config.getDefaultSection() != null) {
            ConfigurationSection d = config.getDefaultSection();
            for (String key : config.getKeys(false)) {

                // If the default section doesn't contain the key, remove it
                if (!d.contains(key)) {
                    config.set(key, null);
                }

                // Recursively set the defaults for the inner sections
                else if (config.isConfigurationSection(key)) {
                    trim(config.getConfigurationSection(key));
                }
            }
        }
        else {
            for (String key : config.getKeys(false)) {
                config.set(key, null);
            }
        }
    }
}
