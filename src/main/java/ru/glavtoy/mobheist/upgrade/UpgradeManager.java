package ru.glavtoy.mobheist.upgrade;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ru.glavtoy.mobheist.MobHeist;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.data.PlayerData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class UpgradeManager {

    private final MobHeist mobHeist;
    private final Messages messages;
    private final List<IncomeLevel> incomeLevels = new ArrayList<>();
    private final List<AutomationLevel> automationLevels = new ArrayList<>();
    private final List<LuckLevel> luckLevels = new ArrayList<>();
    private final List<StorageLevel> storageLevels = new ArrayList<>();
    private final List<Step> progression = new ArrayList<>();
    @Getter
    private int startPens;
    private double penPrice;
    private double penPriceMultiplier;
    private double rebirthPrice;
    private double rebirthPriceMultiplier;
    private double rebirthBonus;

    public void load() {
        FileConfiguration config = mobHeist.getConfig();

        incomeLevels.clear();
        ConfigurationSection income = config.getConfigurationSection("income");
        for (String level : income.getKeys(false)) {
            incomeLevels.add(new IncomeLevel(income.getConfigurationSection(level)));
        }

        automationLevels.clear();
        ConfigurationSection automation = config.getConfigurationSection("automation");
        for (String level : automation.getKeys(false)) {
            automationLevels.add(new AutomationLevel(automation.getConfigurationSection(level)));
        }

        luckLevels.clear();
        ConfigurationSection luck = config.getConfigurationSection("luck");
        for (String level : luck.getKeys(false)) {
            luckLevels.add(new LuckLevel(luck.getConfigurationSection(level)));
        }

        storageLevels.clear();
        ConfigurationSection storage = config.getConfigurationSection("storage");
        for (String level : storage.getKeys(false)) {
            storageLevels.add(new StorageLevel(storage.getConfigurationSection(level)));
        }

        progression.clear();
        for (String step : config.getStringList("progression")) {
            progression.add(Step.valueOf(step.toUpperCase()));
        }

        startPens = config.getInt("pens.start");
        penPrice = config.getDouble("pens.price");
        penPriceMultiplier = config.getDouble("pens.price-multiplier");
        rebirthPrice = config.getDouble("rebirth.price");
        rebirthPriceMultiplier = config.getDouble("rebirth.price-multiplier");
        rebirthBonus = config.getDouble("rebirth.income-bonus");
    }

    public IncomeLevel getIncome(int level) {
        return incomeLevels.get(level - 1);
    }

    public AutomationLevel getAutomation(int level) {
        return automationLevels.get(level - 1);
    }

    public LuckLevel getLuck(int level) {
        return luckLevels.get(level - 1);
    }

    public StorageLevel getStorage(int level) {
        return storageLevels.get(level - 1);
    }

    public double getRebirthMultiplier(int rebirths) {
        return 1 + rebirths * rebirthBonus;
    }

    public Step getNextStep(Base base) {
        Map<Step, Integer> reached = new EnumMap<>(Step.class);
        for (Step step : progression) {
            int level = reached.merge(step, 1, Integer::sum) + (step == Step.PEN ? startPens : 1);
            if (getLevel(base.getData(), step) < level && level <= getMaxLevel(base, step)) {
                return step;
            }
        }
        return null;
    }

    public double getPrice(Base base, Step step) {
        PlayerData data = base.getData();
        int next = getLevel(data, step) + 1;
        switch (step) {
            case PEN:
                return penPrice * Math.pow(penPriceMultiplier, data.getPens() - startPens);
            case MANAGER:
                return getAutomation(next).getPrice();
            case INCOME:
                return getIncome(next).getPrice();
            case LUCK:
                return getLuck(next).getPrice();
            case REBIRTH:
                return rebirthPrice * Math.pow(rebirthPriceMultiplier, data.getRebirths());
            default:
                return getStorage(next).getPrice();
        }
    }

    public boolean buy(Base base, Step step) {
        if (!pay(base, getPrice(base, step))) {
            return false;
        }

        PlayerData data = base.getData();
        int next = getLevel(data, step) + 1;
        switch (step) {
            case PEN:
                data.setPens(next);
                break;
            case MANAGER:
                data.setAutomationLevel(next);
                break;
            case INCOME:
                data.setIncomeLevel(next);
                break;
            case LUCK:
                data.setLuckLevel(next);
                break;
            case STORAGE:
                data.setStorageLevel(next);
                break;
        }
        messages.send(base.getOwner(), "upgrade.bought");
        return true;
    }

    public boolean pay(Base base, double price) {
        PlayerData data = base.getData();
        Player owner = base.getOwner();
        if (data.getMoney() < price) {
            messages.send(owner, "upgrade.no-money");
            owner.playSound(owner.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }

        data.setMoney(data.getMoney() - price);
        owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        return true;
    }

    private int getLevel(PlayerData data, Step step) {
        switch (step) {
            case PEN:
                return data.getPens();
            case MANAGER:
                return data.getAutomationLevel();
            case INCOME:
                return data.getIncomeLevel();
            case LUCK:
                return data.getLuckLevel();
            case REBIRTH:
                return data.getRebirths();
            default:
                return data.getStorageLevel();
        }
    }

    private int getMaxLevel(Base base, Step step) {
        switch (step) {
            case PEN:
                return base.getPlot().getPens().size();
            case MANAGER:
                return automationLevels.size();
            case INCOME:
                return incomeLevels.size();
            case LUCK:
                return luckLevels.size();
            case REBIRTH:
                return Integer.MAX_VALUE;
            default:
                return storageLevels.size();
        }
    }
}
