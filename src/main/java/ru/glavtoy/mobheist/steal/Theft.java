package ru.glavtoy.mobheist.steal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.mob.OwnedMob;

@Getter
@RequiredArgsConstructor
public final class Theft {

    private final Player thief;
    private final Base victim;
    private final int pen;
    private final OwnedMob mob;
    private final LivingEntity entity;
    private final long until;
}
