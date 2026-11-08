package ru.glavtoy.mobheist.data;

import java.util.UUID;

public interface PlayerStorage {

    PlayerData load(UUID uuid);

    void save(UUID uuid, PlayerData data);
}
