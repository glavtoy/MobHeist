package ru.glavtoy.mobheist.mob;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import ru.glavtoy.mobheist.MobHeist;
import ru.glavtoy.mobheist.config.ConfigLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public final class MobManager {

    private final MobHeist mobHeist;
    private final ConfigLoader configLoader;
    private final Map<String, MobType> types = new LinkedHashMap<>();
    private final Map<String, Mutation> mutations = new LinkedHashMap<>();
    private final List<String> rarities = new ArrayList<>();
    @Getter
    private int maxLevel;
    private double incomeGrowth;
    private double upgradePricePercent;
    private double upgradePriceMultiplier;
    private double sellPercent;

    public void load() {
        FileConfiguration config = mobHeist.getConfig();

        types.clear();
        types.putAll(configLoader.loadMobs());

        mutations.clear();
        for (String id : config.getConfigurationSection("mutations").getKeys(false)) {
            mutations.put(id, new Mutation(config.getConfigurationSection("mutations." + id)));
        }

        rarities.clear();
        for (String rarity : config.getStringList("rarities")) {
            rarities.add(ChatColor.translateAlternateColorCodes('&', rarity));
        }

        maxLevel = config.getInt("mob-upgrade.max-level");
        incomeGrowth = config.getDouble("mob-upgrade.income-growth");
        upgradePricePercent = config.getDouble("mob-upgrade.price-percent");
        upgradePriceMultiplier = config.getDouble("mob-upgrade.price-multiplier");
        sellPercent = config.getDouble("sell-percent");
    }

    public Collection<MobType> getTypes() {
        return types.values();
    }

    public MobType getType(String id) {
        return types.get(id);
    }

    public Mutation getMutation(String id) {
        return mutations.get(id);
    }

    public String getRarity(int tier) {
        return tier < rarities.size() ? rarities.get(tier) : String.valueOf(tier);
    }

    public Mutation rollMutation(double luck) {
        Mutation result = null;
        for (Mutation mutation : mutations.values()) {
            if (ThreadLocalRandom.current().nextDouble() < mutation.getChance() * luck) {
                result = mutation;
            }
        }
        return result;
    }

    public double getIncome(OwnedMob mob, int level) {
        double income = mob.getType().getIncome() * Math.pow(incomeGrowth, level - 1);
        return mob.getMutation() == null ? income : income * mob.getMutation().getMultiplier();
    }

    public double getUpgradePrice(OwnedMob mob) {
        return mob.getType().getPrice() * upgradePricePercent / 100 * Math.pow(upgradePriceMultiplier, mob.getLevel() - 1);
    }

    public double getSellPrice(OwnedMob mob) {
        return mob.getType().getPrice() * sellPercent / 100;
    }
}
