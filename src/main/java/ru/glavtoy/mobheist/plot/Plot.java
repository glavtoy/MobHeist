package ru.glavtoy.mobheist.plot;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Plot {

    private final PlotTemplate template;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private int number;
    @Getter
    private final Location corner;
    @Getter
    @Setter
    private Player owner;

    public Plot(PlotTemplate template, int number, Location corner) {
        this.template = template;
        this.number = number;
        this.corner = corner;
    }

    public Location getShop() {
        return toLocation(template.getShop());
    }

    public Location getDoor() {
        return toLocation(template.getDoor());
    }

    public Location getManager() {
        return toLocation(template.getManager());
    }

    public Location getManagerPad() {
        return toLocation(template.getManagerPad());
    }

    public Location getIncomePad() {
        return toLocation(template.getIncomePad());
    }

    public Location getLuckPad() {
        return toLocation(template.getLuckPad());
    }

    public Location getStoragePad() {
        return toLocation(template.getStoragePad());
    }

    public Location getRebirthPad() {
        return toLocation(template.getRebirthPad());
    }

    public boolean contains(Location location) {
        if (location.getWorld() != corner.getWorld()) {
            return false;
        }
        int x = location.getBlockX() - corner.getBlockX();
        int z = location.getBlockZ() - corner.getBlockZ();
        return x >= 0 && x < template.getSize() && z >= 0 && z < template.getSize();
    }

    public List<Location> getPads() {
        List<Location> pads = template.getPenPads().stream().map(this::toLocation).collect(Collectors.toList());
        pads.addAll(Arrays.asList(getDoor(), getManagerPad(), getIncomePad(), getLuckPad(), getStoragePad(), getRebirthPad()));
        pads.removeIf(Objects::isNull);
        return pads;
    }

    public List<Location> getPens() {
        return template.getPens().stream().map(this::toLocation).collect(Collectors.toList());
    }

    public Location getPenPad(int pen) {
        return pen < template.getPenPads().size() ? toLocation(template.getPenPads().get(pen)) : null;
    }

    public Location getSpawn() {
        if (template.getSpawn() == null) {
            return corner.clone().add(0.5, 0, 0.5);
        }
        Location spawn = toLocation(template.getSpawn());
        spawn.setYaw(template.getSpawnYaw());
        return spawn;
    }

    public Vector getOffset(Location location) {
        return location.toVector().subtract(corner.toVector());
    }

    private Location toLocation(Vector offset) {
        return offset == null ? null : corner.clone().add(offset);
    }
}
