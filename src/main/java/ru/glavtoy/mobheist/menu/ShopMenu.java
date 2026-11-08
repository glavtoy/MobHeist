package ru.glavtoy.mobheist.menu;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.base.BaseManager;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.mob.MobManager;
import ru.glavtoy.mobheist.mob.MobType;
import ru.glavtoy.mobheist.util.Numbers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ShopMenu implements Menu {

    private static final int SIZE = 36;
    private static final int MOB_SLOTS = 27;
    private static final int MONEY_SLOT = 31;
    private static final short FILLER_COLOR = 15;

    private final Base base;
    private final BaseManager baseManager;
    private final MobManager mobManager;
    private final Messages messages;
    private final Map<Integer, MobType> mobs = new HashMap<>();
    @Getter
    private final Inventory inventory;

    public ShopMenu(Base base, BaseManager baseManager, MobManager mobManager, Messages messages) {
        this.base = base;
        this.baseManager = baseManager;
        this.mobManager = mobManager;
        this.messages = messages;
        inventory = Bukkit.createInventory(null, SIZE, messages.get("menu.shop.title"));
        mobManager.getTypes().stream()
                .limit(MOB_SLOTS)
                .forEach(type -> mobs.put(mobs.size(), type));
    }

    public void open() {
        update();
        base.getOwner().openInventory(inventory);
    }

    @Override
    public void click(int slot) {
        MobType type = mobs.get(slot);
        if (type == null) {
            return;
        }
        baseManager.buyMob(base, type);
        update();
    }

    private void update() {
        ItemStack filler = createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, FILLER_COLOR), " ", new ArrayList<>());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        mobs.forEach((slot, type) -> inventory.setItem(slot, createMobIcon(type)));

        String money = Numbers.format(base.getData().getMoney());
        inventory.setItem(MONEY_SLOT, createItem(new ItemStack(Material.EMERALD),
                messages.get("menu.shop.money").replace("{money}", money), messages.getList("menu.shop.money-lore")));
    }

    private ItemStack createMobIcon(MobType type) {
        ItemStack item = new ItemStack(Material.MONSTER_EGG);
        SpawnEggMeta meta = (SpawnEggMeta) item.getItemMeta();
        meta.setSpawnedType(type.getEntity());
        item.setItemMeta(meta);

        List<String> lore = new ArrayList<>();
        lore.add(mobManager.getRarity(type.getTier()));
        lore.add(messages.get("menu.shop.income").replace("{income}", Numbers.format(type.getIncome())));
        lore.add("");
        if (base.getData().getRebirths() < type.getRebirth()) {
            lore.add(messages.get("menu.shop.locked").replace("{rebirth}", String.valueOf(type.getRebirth())));
        } else {
            String key = base.getData().getMoney() >= type.getPrice() ? "menu.shop.price" : "menu.shop.price-locked";
            lore.add(messages.get(key).replace("{price}", Numbers.format(type.getPrice())));
        }
        return createItem(item, messages.get("menu.shop.mob").replace("{mob}", ChatColor.stripColor(type.getName())), lore);
    }

    private ItemStack createItem(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
