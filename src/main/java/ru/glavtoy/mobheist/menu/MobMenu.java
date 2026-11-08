package ru.glavtoy.mobheist.menu;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.base.BaseManager;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.mob.MobManager;
import ru.glavtoy.mobheist.mob.OwnedMob;
import ru.glavtoy.mobheist.util.Numbers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class MobMenu implements Menu {

    private static final int SIZE = 27;
    private static final int INFO_SLOT = 10;
    private static final int COLLECT_SLOT = 12;
    private static final int UPGRADE_SLOT = 14;
    private static final int SELL_SLOT = 16;
    private static final short FILLER_COLOR = 15;

    private final Base base;
    private final int pen;
    private final BaseManager baseManager;
    private final MobManager mobManager;
    private final Messages messages;
    @Getter
    private final Inventory inventory;

    public MobMenu(Base base, int pen, BaseManager baseManager, MobManager mobManager, Messages messages) {
        this.base = base;
        this.pen = pen;
        this.baseManager = baseManager;
        this.mobManager = mobManager;
        this.messages = messages;
        inventory = Bukkit.createInventory(null, SIZE, messages.get("menu.mob.title"));
    }

    public void open() {
        update();
        base.getOwner().openInventory(inventory);
    }

    @Override
    public void click(int slot) {
        if (!base.getData().getMobs().containsKey(pen)) {
            return;
        }
        if (slot == COLLECT_SLOT) {
            baseManager.collect(base, pen);
            update();
        } else if (slot == UPGRADE_SLOT) {
            baseManager.upgradeMob(base, pen);
            update();
        } else if (slot == SELL_SLOT) {
            baseManager.sellMob(base, pen);
            baseManager.closeMenu(base);
        }
    }

    private void update() {
        ItemStack filler = createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, FILLER_COLOR), " ", new ArrayList<>());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        OwnedMob mob = base.getData().getMobs().get(pen);
        ItemStack egg = new ItemStack(Material.MONSTER_EGG);
        SpawnEggMeta eggMeta = (SpawnEggMeta) egg.getItemMeta();
        eggMeta.setSpawnedType(mob.getType().getEntity());
        egg.setItemMeta(eggMeta);
        List<String> info = new ArrayList<>();
        info.add(mobManager.getRarity(mob.getType().getTier()));
        info.add(messages.get("menu.mob.level")
                .replace("{level}", String.valueOf(mob.getLevel()))
                .replace("{max}", String.valueOf(mobManager.getMaxLevel())));
        info.add(messages.get("menu.mob.income").replace("{income}", Numbers.format(baseManager.getIncome(base, mob))));
        inventory.setItem(INFO_SLOT, createItem(egg, mob.getName(), info));

        String stored = Numbers.format(mob.getStored());
        inventory.setItem(COLLECT_SLOT, createItem(new ItemStack(Material.EMERALD), messages.get("menu.mob.collect"),
                getLore("menu.mob.collect-lore", "{stored}", stored)));

        List<String> upgrade = new ArrayList<>();
        if (mob.getLevel() >= mobManager.getMaxLevel()) {
            upgrade.add(messages.get("menu.mob.upgrade-max"));
        } else {
            double nextIncome = baseManager.getIncome(base, mob, mob.getLevel() + 1);
            double price = mobManager.getUpgradePrice(mob);
            String key = base.getData().getMoney() >= price ? "menu.mob.upgrade-price" : "menu.mob.upgrade-price-locked";
            upgrade.add(messages.get("menu.mob.upgrade-next").replace("{income}", Numbers.format(nextIncome)));
            upgrade.add("");
            upgrade.add(messages.get(key).replace("{price}", Numbers.format(price)));
        }
        inventory.setItem(UPGRADE_SLOT, createItem(new ItemStack(Material.EXP_BOTTLE), messages.get("menu.mob.upgrade"), upgrade));

        String money = Numbers.format(mobManager.getSellPrice(mob) + mob.getStored());
        inventory.setItem(SELL_SLOT, createItem(new ItemStack(Material.GOLD_NUGGET), messages.get("menu.mob.sell"),
                getLore("menu.mob.sell-lore", "{money}", money)));
    }

    private List<String> getLore(String key, String placeholder, String value) {
        return messages.getList(key).stream()
                .map(line -> line.replace(placeholder, value))
                .collect(Collectors.toList());
    }

    private ItemStack createItem(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
