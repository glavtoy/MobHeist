package ru.glavtoy.mobheist.config;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.glavtoy.mobheist.MobHeist;
import ru.glavtoy.mobheist.mob.MobType;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public final class ConfigLoader {

    private final MobHeist mobHeist;

    public Map<String, MobType> loadMobs() {
        FileConfiguration config = load("mobs.yml");
        Map<String, MobType> mobs = new LinkedHashMap<>();
        for (String id : config.getKeys(false)) {
            mobs.put(id, new MobType(config.getConfigurationSection(id)));
        }
        return mobs;
    }

    public FileConfiguration loadMessages() {
        FileConfiguration config = load("messages.yml");
        config.setDefaults(YamlConfiguration.loadConfiguration(
                new InputStreamReader(mobHeist.getResource("messages.yml"), StandardCharsets.UTF_8)));
        return config;
    }

    private FileConfiguration load(String name) {
        File file = new File(mobHeist.getDataFolder(), name);
        if (!file.exists()) {
            mobHeist.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
