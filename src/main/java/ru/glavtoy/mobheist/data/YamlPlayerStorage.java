package ru.glavtoy.mobheist.data;

import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.glavtoy.mobheist.MobHeist;
import ru.glavtoy.mobheist.mob.MobManager;
import ru.glavtoy.mobheist.mob.MobType;
import ru.glavtoy.mobheist.mob.OwnedMob;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public final class YamlPlayerStorage implements PlayerStorage {

    private final MobHeist mobHeist;
    private final MobManager mobManager;

    @Override
    public PlayerData load(UUID uuid) {
        PlayerData data = new PlayerData();
        File file = getFile(uuid);
        if (!file.exists()) {
            data.setMoney(mobHeist.getConfig().getDouble("start-money"));
            return data;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.setMoney(config.getDouble("money"));
        data.setIncomeLevel(config.getInt("income-level", 1));
        data.setAutomationLevel(config.getInt("automation-level", 1));
        data.setLuckLevel(config.getInt("luck-level", 1));
        data.setStorageLevel(config.getInt("storage-level", 1));
        data.setPens(config.getInt("pens"));
        data.setRebirths(config.getInt("rebirths"));

        ConfigurationSection mobs = config.getConfigurationSection("mobs");
        if (mobs != null) {
            for (String pen : mobs.getKeys(false)) {
                ConfigurationSection section = mobs.getConfigurationSection(pen);
                MobType type = mobManager.getType(section.getString("type"));
                if (type != null) {
                    data.getMobs().put(Integer.parseInt(pen), new OwnedMob(type,
                            mobManager.getMutation(section.getString("mutation")),
                            section.getInt("level", 1),
                            section.getDouble("stored")));
                }
            }
        }
        return data;
    }

    @Override
    public void save(UUID uuid, PlayerData data) {
        FileConfiguration config = new YamlConfiguration();
        config.set("money", data.getMoney());
        config.set("income-level", data.getIncomeLevel());
        config.set("automation-level", data.getAutomationLevel());
        config.set("luck-level", data.getLuckLevel());
        config.set("storage-level", data.getStorageLevel());
        config.set("pens", data.getPens());
        config.set("rebirths", data.getRebirths());
        for (Map.Entry<Integer, OwnedMob> entry : data.getMobs().entrySet()) {
            OwnedMob mob = entry.getValue();
            String path = "mobs." + entry.getKey();
            config.set(path + ".type", mob.getType().getId());
            config.set(path + ".level", mob.getLevel());
            config.set(path + ".stored", mob.getStored());
            if (mob.getMutation() != null) {
                config.set(path + ".mutation", mob.getMutation().getId());
            }
        }

        try {
            config.save(getFile(uuid));
        } catch (IOException e) {
            mobHeist.getLogger().warning("Не удалось сохранить игрока " + uuid + ": " + e.getMessage());
        }
    }

    private File getFile(UUID uuid) {
        return new File(mobHeist.getDataFolder(), "players/" + uuid + ".yml");
    }
}
