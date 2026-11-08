package ru.glavtoy.mobheist.base;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import ru.glavtoy.mobheist.data.PlayerData;
import ru.glavtoy.mobheist.menu.Menu;
import ru.glavtoy.mobheist.mob.OwnedMob;
import ru.glavtoy.mobheist.plot.Plot;
import ru.glavtoy.mobheist.upgrade.Step;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public final class Base {

    private static final double CENTER = 0.5;
    private static final double SHOP_TITLE_HEIGHT = 2.5;
    private static final double SHOP_HINT_HEIGHT = 2.2;
    private static final double TITLE_HEIGHT = 1.9;
    private static final double INFO_HEIGHT = 1.6;
    private static final double PRICE_HEIGHT = 1.3;
    private static final double MOB_INCOME_HEIGHT = 3.2;
    private static final double MOB_UPGRADE_HEIGHT = 3.5;
    private static final int PEN_RADIUS = 2;

    @Getter
    private final Player owner;
    @Getter
    private final Plot plot;
    @Getter
    private final PlayerData data;
    private final Map<Integer, LivingEntity> entities = new HashMap<>();
    private final Map<Integer, ArmorStand> holograms = new HashMap<>();
    private final Map<Integer, ArmorStand> upgrades = new HashMap<>();
    private final Set<Integer> stolen = new HashSet<>();
    private ArmorStand shopTitle;
    private ArmorStand shopHint;
    private Villager manager;
    @Getter
    private Location pad;
    private ArmorStand padTitle;
    private ArmorStand padInfo;
    private ArmorStand padPrice;
    @Getter
    private Step padStep;
    @Getter
    @Setter
    private int secondsToCollect;
    @Getter
    @Setter
    private Menu menu;
    private Location lock;
    private ArmorStand lockTitle;
    private ArmorStand lockStatus;
    @Getter
    @Setter
    private long lockedUntil;
    @Getter
    @Setter
    private long lockReadyAt;
    @Getter
    @Setter
    private long lastRobbed;

    public void spawnShopHologram(String title, String hint) {
        Location shop = plot.getShop();
        if (shop == null) {
            return;
        }
        shopTitle = spawnHologram(shop.clone().add(CENTER, SHOP_TITLE_HEIGHT, CENTER), title);
        shopHint = spawnHologram(shop.clone().add(CENTER, SHOP_HINT_HEIGHT, CENTER), hint);
    }

    public void buildPens(int built) {
        List<Location> pens = plot.getPens();
        for (int pen = 0; pen < pens.size(); pen++) {
            setFence(pens.get(pen), pen < built);
        }
    }

    public void setPad(Step step, Location location, String title, String info) {
        removePad();
        if (location == null) {
            return;
        }

        padStep = step;
        pad = location;
        if (location.getBlock().getType() == Material.AIR) {
            location.getBlock().setType(Material.GOLD_PLATE);
        }
        padTitle = spawnHologram(location.clone().add(CENTER, TITLE_HEIGHT, CENTER), title);
        padInfo = spawnHologram(location.clone().add(CENTER, INFO_HEIGHT, CENTER), info);
        padPrice = spawnHologram(location.clone().add(CENTER, PRICE_HEIGHT, CENTER), "");
    }

    public void setPadPrice(String text) {
        if (padPrice != null) {
            padPrice.setCustomName(text);
        }
    }

    public void clearPads() {
        plot.getPads().stream()
                .map(Location::getBlock)
                .filter(block -> block.getType() == Material.GOLD_PLATE || block.getType() == Material.STONE_PLATE)
                .forEach(block -> block.setType(Material.AIR));
    }

    public boolean isPad(Location location) {
        return isSameBlock(pad, location);
    }

    public void setLock(Location location, String title) {
        if (location == null) {
            return;
        }
        lock = location;
        if (location.getBlock().getType() == Material.AIR) {
            location.getBlock().setType(Material.STONE_PLATE);
        }
        lockTitle = spawnHologram(location.clone().add(CENTER, TITLE_HEIGHT, CENTER), title);
        lockStatus = spawnHologram(location.clone().add(CENTER, INFO_HEIGHT, CENTER), "");
    }

    public void setLockStatus(String text) {
        if (lockStatus != null) {
            lockStatus.setCustomName(text);
        }
    }

    public boolean isLock(Location location) {
        return isSameBlock(lock, location);
    }

    public boolean isLocked() {
        return lockedUntil > System.currentTimeMillis();
    }

    public void removePad() {
        if (pad == null) {
            return;
        }
        if (pad.getBlock().getType() == Material.GOLD_PLATE) {
            pad.getBlock().setType(Material.AIR);
        }
        padTitle.remove();
        padInfo.remove();
        padPrice.remove();
        pad = null;
        padStep = null;
    }

    public void spawnManager(Location location, String name) {
        if (manager != null || location == null) {
            return;
        }
        manager = location.getWorld().spawn(location.clone().add(CENTER, 0, CENTER), Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setCollidable(false);
            villager.setCustomName(name);
            villager.setCustomNameVisible(true);
            villager.addScoreboardTag(BaseManager.ENTITY_TAG);
        });
    }

    public void removeManager() {
        if (manager != null) {
            manager.remove();
            manager = null;
        }
    }

    public void spawnMob(int pen, OwnedMob mob) {
        Location location = plot.getPens().get(pen).clone().add(CENTER, 0, CENTER);
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, mob.getType().getEntity());
        entity.setAI(false);
        entity.setSilent(true);
        entity.setCollidable(false);
        entity.setRemoveWhenFarAway(false);
        entity.setCustomNameVisible(true);
        entity.addScoreboardTag(BaseManager.ENTITY_TAG);
        entities.put(pen, entity);
        holograms.put(pen, spawnHologram(location.clone().add(0, MOB_INCOME_HEIGHT, 0), ""));
        upgrades.put(pen, spawnHologram(location.clone().add(0, MOB_UPGRADE_HEIGHT, 0), ""));
    }

    public void updateMob(int pen, String name, String text, String upgrade) {
        entities.get(pen).setCustomName(name);
        holograms.get(pen).setCustomName(text);
        upgrades.get(pen).setCustomName(upgrade);
    }

    public void removeMob(int pen) {
        LivingEntity entity = entities.remove(pen);
        if (entity != null) {
            entity.remove();
            holograms.remove(pen).remove();
            upgrades.remove(pen).remove();
        }
    }

    public LivingEntity getEntity(int pen) {
        return entities.get(pen);
    }

    public boolean isStolen(int pen) {
        return stolen.contains(pen);
    }

    public void setStolen(int pen, boolean value) {
        if (value) {
            stolen.add(pen);
        } else {
            stolen.remove(pen);
        }
    }

    public void returnMob(int pen) {
        LivingEntity entity = entities.get(pen);
        if (entity != null && entity.isValid()) {
            entity.teleport(plot.getPens().get(pen).clone().add(CENTER, 0, CENTER));
        }
    }

    public Integer getPen(Entity entity) {
        return entities.entrySet().stream()
                .filter(entry -> entry.getValue().equals(entity))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public int getFreePen() {
        return IntStream.range(0, Math.min(data.getPens(), plot.getPens().size()))
                .filter(pen -> !data.getMobs().containsKey(pen))
                .findFirst()
                .orElse(-1);
    }

    public int getFreePen(Location location, double distance) {
        List<Location> pens = plot.getPens();
        return IntStream.range(0, Math.min(data.getPens(), pens.size()))
                .filter(pen -> !data.getMobs().containsKey(pen))
                .filter(pen -> isNear(pens.get(pen), location, distance))
                .findFirst()
                .orElse(-1);
    }

    public void remove() {
        entities.values().forEach(Entity::remove);
        entities.clear();
        holograms.values().forEach(Entity::remove);
        holograms.clear();
        upgrades.values().forEach(Entity::remove);
        upgrades.clear();
        if (shopTitle != null) {
            shopTitle.remove();
            shopHint.remove();
        }
        if (manager != null) {
            manager.remove();
        }
        removePad();
        if (lock != null) {
            if (lock.getBlock().getType() == Material.STONE_PLATE) {
                lock.getBlock().setType(Material.AIR);
            }
            lockTitle.remove();
            lockStatus.remove();
        }
    }

    private boolean isSameBlock(Location block, Location location) {
        return block != null && block.getBlockX() == location.getBlockX()
                && block.getBlockY() == location.getBlockY() && block.getBlockZ() == location.getBlockZ();
    }

    private boolean isNear(Location pen, Location location, double distance) {
        if (pen.getWorld() != location.getWorld()) {
            return false;
        }
        double dx = pen.getX() + CENTER - location.getX();
        double dz = pen.getZ() + CENTER - location.getZ();
        return dx * dx + dz * dz <= distance * distance;
    }

    private void setFence(Location center, boolean built) {
        for (int dx = -PEN_RADIUS; dx <= PEN_RADIUS; dx++) {
            for (int dz = -PEN_RADIUS; dz <= PEN_RADIUS; dz++) {
                if (Math.abs(dx) != PEN_RADIUS && Math.abs(dz) != PEN_RADIUS) {
                    continue;
                }
                Block block = center.clone().add(dx, 0, dz).getBlock();
                if (built && block.getType() == Material.AIR) {
                    block.setType(Material.FENCE);
                } else if (!built && block.getType() == Material.FENCE) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private ArmorStand spawnHologram(Location location, String text) {
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setCustomName(text);
            stand.setCustomNameVisible(true);
            stand.addScoreboardTag(BaseManager.ENTITY_TAG);
        });
    }
}
