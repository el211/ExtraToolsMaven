package fr.elias.extraTools.utils.config;


import com.google.common.collect.Lists;
import fr.elias.extraTools.ExtraTools;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class Cfgs {

    private static final JavaPlugin plugin = JavaPlugin.getPlugin(ExtraTools.class);
    private static final List<Cfgs> configs = Lists.newArrayList();
    private static final File dataFolder = plugin.getDataFolder();

    public static Cfgs of() {
        return of("config");
    }

    public static Cfgs of(String name) {
        String configName = name.endsWith(".yml") ? name : name + ".yml";
        return configs.stream().filter(c -> c.getConfigName().equals(configName)).findFirst().orElseGet(() -> {
            Cfgs newCfg = new Cfgs(configName);
            configs.add(newCfg);
            return newCfg;
        });
    }

    public static Cfgs of(File file) {
        return configs.stream().filter(c -> c.configFile.equals(file)).findFirst().orElseGet(() -> {
            Cfgs newCfg = new Cfgs(file);
            configs.add(newCfg);
            return newCfg;
        });
    }

    YamlConfiguration ymlConfig;
    final String configName;
    final File configFile;

    private Cfgs(File file) {
        configName = file.getName();
        configFile = file;
        ymlConfig = reload();
    }

    private Cfgs(String name) {
        configName = name;
        configFile = new File(dataFolder, configName);
        ymlConfig = reload();
    }

    public String getConfigName() {
        return configName;
    }

    public Cfgs save() {
        try {
            ymlConfig.save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public Configuration get() {
        if (ymlConfig == null)
            reload();

        return ymlConfig;
    }

    public YamlConfiguration reload() {

        if (!configFile.exists()) {

            configFile.getParentFile().mkdirs();

            try (InputStream is = plugin.getClass().getResourceAsStream("/" + configName)) {
                if (is == null) {
                    configFile.createNewFile();
                } else {
                    Files.copy(is, configFile.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return ymlConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    public Cfgs set(String key, Object value) {
        get().set(key, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T def) {
        if (key == null || key.isEmpty())
            return def;

        Object result = get().get(key);

        if (result == null)
            return def;

        return (T) result;
    }

}

