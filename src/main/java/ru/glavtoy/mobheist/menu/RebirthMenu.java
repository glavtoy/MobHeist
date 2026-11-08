package ru.glavtoy.mobheist.menu;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.base.BaseManager;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.util.Numbers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class RebirthMenu implements Menu {

    private static final int SIZE = 27;
    private static final int CONFIRM_SLOT = 11;
    private static final int CANCEL_SLOT = 15;
    private static final short FILLER_COLOR = 15;
    private static final short CONFIRM_COLOR = 5;
    private static final short CANCEL_COLOR = 14;

    private final Base base;
    private final BaseManager baseManager;
    @Getter
    private final Inventory inventory;

    public RebirthMenu(Base base, BaseManager baseManager, Messages messages, double price, double multiplier) {
        this.base = base;
        this.baseManager = baseManager;
        inventory = Bukkit.createInventory(null, SIZE, messages.get("menu.rebirth.title"));

        ItemStack filler = createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, FILLER_COLOR), " ", new ArrayList<>());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        String key = base.getData().getMoney() >= price ? "menu.rebirth.price" : "menu.rebirth.price-locked";
        List<String> confirm = messages.getList("menu.rebirth.confirm-lore").stream()
                .map(line -> line.replace("{multiplier}", Numbers.format(multiplier)))
                .collect(Collectors.toList());
        confirm.add("");
        confirm.add(messages.get(key).replace("{price}", Numbers.format(price)));
        inventory.setItem(CONFIRM_SLOT, createItem(new ItemStack(Material.WOOL, 1, CONFIRM_COLOR),
                messages.get("menu.rebirth.confirm"), confirm));
        inventory.setItem(CANCEL_SLOT, createItem(new ItemStack(Material.WOOL, 1, CANCEL_COLOR),
                messages.get("menu.rebirth.cancel"), messages.getList("menu.rebirth.cancel-lore")));
    }

    public void open() {
        base.getOwner().openInventory(inventory);
    }

    @Override
    public void click(int slot) {
        if (slot == CONFIRM_SLOT) {
            baseManager.rebirth(base);
            baseManager.closeMenu(base);
        } else if (slot == CANCEL_SLOT) {
            baseManager.closeMenu(base);
        }
    }

    private ItemStack createItem(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
