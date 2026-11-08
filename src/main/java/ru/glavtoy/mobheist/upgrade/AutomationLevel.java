package ru.glavtoy.mobheist.upgrade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public final class AutomationLevel {

    private final double price;
    private final int interval;

    public AutomationLevel(ConfigurationSection section) {
        price = section.getDouble("price");
        interval = section.getInt("interval");
    }
}
