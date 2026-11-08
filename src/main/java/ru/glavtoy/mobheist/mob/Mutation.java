package ru.glavtoy.mobheist.mob;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

@Getter
public final class Mutation {

    private final String id;
    private final String name;
    private final double chance;
    private final double multiplier;

    public Mutation(ConfigurationSection section) {
        id = section.getName();
        name = ChatColor.translateAlternateColorCodes('&', section.getString("name"));
        chance = section.getDouble("chance");
        multiplier = section.getDouble("multiplier");
    }
}
