package ru.glavtoy.mobheist.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.mob.OwnedMob;

@Getter
@RequiredArgsConstructor
public final class MobUpgradeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Base base;
    private final OwnedMob mob;
    private final double price;
    @Setter
    private boolean cancelled;

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
