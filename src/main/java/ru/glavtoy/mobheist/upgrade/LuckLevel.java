package ru.glavtoy.mobheist.upgrade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public final class LuckLevel {

    private final double price;
    private final double multiplier;

    public LuckLevel(ConfigurationSection section) {
        price = section.getDouble("price");
        multiplier = section.getDouble("multiplier");
    }
}
