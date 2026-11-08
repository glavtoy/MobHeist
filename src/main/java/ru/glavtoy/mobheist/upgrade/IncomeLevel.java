package ru.glavtoy.mobheist.upgrade;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public final class IncomeLevel {

    private final double price;
    private final double multiplier;

    public IncomeLevel(ConfigurationSection section) {
        price = section.getDouble("price");
        multiplier = section.getDouble("multiplier");
    }
}
