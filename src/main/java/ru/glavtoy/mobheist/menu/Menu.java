package ru.glavtoy.mobheist.menu;

import org.bukkit.inventory.Inventory;

public interface Menu {

    Inventory getInventory();

    void click(int slot);
}
