package ru.glavtoy.mobheist.data;

import lombok.Getter;
import lombok.Setter;
import ru.glavtoy.mobheist.mob.OwnedMob;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public final class PlayerData {

    private double money;
    private int incomeLevel = 1;
    private int automationLevel = 1;
    private int luckLevel = 1;
    private int storageLevel = 1;
    private int pens;
    private int rebirths;
    private final Map<Integer, OwnedMob> mobs = new ConcurrentHashMap<>();
}
