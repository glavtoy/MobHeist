package ru.glavtoy.mobheist.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.mob.OwnedMob;

@Getter
@RequiredArgsConstructor
public final class MobStolenEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player thief;
    private final Base victim;
    private final OwnedMob mob;

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
