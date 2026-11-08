package ru.glavtoy.mobheist.hud;

import org.bukkit.entity.Player;
import ru.glavtoy.mobheist.base.Base;

public interface BaseHud {

    void update(Base base, double income);

    void remove(Player player);
}
