package ru.glavtoy.mobheist.mob;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

@Getter
public final class MobType {

    private final String id;
    private final String name;
    private final EntityType entity;
    private final int tier;
    private final double income;
    private final double price;
    private final int rebirth;

    public MobType(ConfigurationSection section) {
        id = section.getName();
        name = ChatColor.translateAlternateColorCodes('&', section.getString("name"));
        entity = EntityType.valueOf(section.getString("entity"));
        tier = section.getInt("tier");
        income = section.getDouble("income");
        price = section.getDouble("price");
        rebirth = section.getInt("rebirth");
    }
}
