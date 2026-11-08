package ru.glavtoy.mobheist.upgrade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public final class StorageLevel {

    private final double price;
    private final int seconds;

    public StorageLevel(ConfigurationSection section) {
        price = section.getDouble("price");
        seconds = section.getInt("seconds");
    }
}
