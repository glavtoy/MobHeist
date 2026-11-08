package ru.glavtoy.mobheist.plot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.glavtoy.mobheist.MobHeist;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class PlotManager {

    private static final int DEFAULT_SIZE = 25;

    private final MobHeist mobHeist;
    @Getter
    private final List<Plot> plots = new ArrayList<>();
    @Getter
    private PlotTemplate template;
    private File file;
    private FileConfiguration config;

    public void load() {
        file = new File(mobHeist.getDataFolder(), "plots.yml");
        if (!file.exists()) {
            mobHeist.saveResource("plots.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        template = new PlotTemplate();
        template.setShop(config.getVector("template.shop"));
        template.setDoor(config.getVector("template.door"));
        template.setSpawn(config.getVector("template.spawn"));
        template.setSpawnYaw((float) config.getDouble("template.spawn-yaw"));
        template.setSize(config.getInt("template.size", DEFAULT_SIZE));
        template.setManager(config.getVector("template.manager"));
        template.setManagerPad(config.getVector("template.manager-pad"));
        template.setIncomePad(config.getVector("template.income-pad"));
        template.setLuckPad(config.getVector("template.luck-pad"));
        template.setStoragePad(config.getVector("template.storage-pad"));
        template.setRebirthPad(config.getVector("template.rebirth-pad"));
        config.getList("template.pens", new ArrayList<>()).stream()
                .map(Vector.class::cast)
                .forEach(template.getPens()::add);
        config.getList("template.pen-pads", new ArrayList<>()).stream()
                .map(Vector.class::cast)
                .forEach(template.getPenPads()::add);

        plots.clear();
        config.getList("plots", new ArrayList<>()).stream()
                .map(Location.class::cast)
                .forEach(corner -> plots.add(new Plot(template, plots.size() + 1, corner)));
    }

    public void save() {
        config.set("template.shop", template.getShop());
        config.set("template.door", template.getDoor());
        config.set("template.spawn", template.getSpawn());
        config.set("template.spawn-yaw", template.getSpawnYaw());
        config.set("template.size", template.getSize());
        config.set("template.manager", template.getManager());
        config.set("template.manager-pad", template.getManagerPad());
        config.set("template.income-pad", template.getIncomePad());
        config.set("template.luck-pad", template.getLuckPad());
        config.set("template.storage-pad", template.getStoragePad());
        config.set("template.rebirth-pad", template.getRebirthPad());
        config.set("template.pens", template.getPens());
        config.set("template.pen-pads", template.getPenPads());

        config.set("plots", plots.stream().map(Plot::getCorner).collect(Collectors.toList()));

        try {
            config.save(file);
        } catch (IOException e) {
            mobHeist.getLogger().warning("Не удалось сохранить plots.yml: " + e.getMessage());
        }
    }

    public Plot addPlot(Location corner) {
        Plot plot = new Plot(template, plots.size() + 1, corner.getBlock().getLocation());
        plots.add(plot);
        save();
        return plot;
    }

    public void removePlot(Plot plot) {
        plots.remove(plot);
        for (int i = 0; i < plots.size(); i++) {
            plots.get(i).setNumber(i + 1);
        }
        save();
    }

    public Plot getNearest(Location location) {
        return plots.stream()
                .filter(plot -> plot.getCorner().getWorld() == location.getWorld())
                .min(Comparator.comparingDouble(plot -> plot.getCorner().distanceSquared(location)))
                .orElse(null);
    }

    public Plot getPlot(Player player) {
        return plots.stream().filter(plot -> plot.getOwner() == player).findFirst().orElse(null);
    }

    public Plot claim(Player player) {
        Plot plot = plots.stream().filter(free -> free.getOwner() == null).findFirst().orElse(null);
        if (plot != null) {
            plot.setOwner(player);
        }
        return plot;
    }

    public void release(Player player) {
        Plot plot = getPlot(player);
        if (plot != null) {
            plot.setOwner(null);
        }
    }
}
