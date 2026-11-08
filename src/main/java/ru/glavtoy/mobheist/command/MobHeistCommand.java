package ru.glavtoy.mobheist.command;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.glavtoy.mobheist.MobHeist;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.base.BaseManager;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.mob.MobManager;
import ru.glavtoy.mobheist.plot.Plot;
import ru.glavtoy.mobheist.plot.PlotManager;
import ru.glavtoy.mobheist.plot.PlotTemplate;
import ru.glavtoy.mobheist.upgrade.UpgradeManager;
import ru.glavtoy.mobheist.util.Numbers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class MobHeistCommand implements CommandExecutor, TabCompleter {

    private static final String RELOAD_PERMISSION = "mobheist.reload";
    private static final String PLOT_PERMISSION = "mobheist.plot";
    private static final String MONEY_PERMISSION = "mobheist.money";

    private final MobHeist mobHeist;
    private final PlotManager plotManager;
    private final BaseManager baseManager;
    private final MobManager mobManager;
    private final UpgradeManager upgradeManager;
    private final Messages messages;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if (subcommand.equals("reload")) {
            reload(sender);
            return true;
        }
        if (subcommand.equals("money")) {
            money(sender, args);
            return true;
        }
        if (!(sender instanceof Player)) {
            messages.send(sender, "players-only");
            return true;
        }

        Player player = (Player) sender;
        if (subcommand.equals("plot")) {
            plot(player, args);
        } else {
            sendUsage(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission(MONEY_PERMISSION)) {
                subcommands.add("money");
            }
            if (sender.hasPermission(PLOT_PERMISSION)) {
                subcommands.add("plot");
            }
            if (sender.hasPermission(RELOAD_PERMISSION)) {
                subcommands.add("reload");
            }
            return complete(args[0], subcommands);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("plot") && sender.hasPermission(PLOT_PERMISSION)) {
            return complete(args[1], Arrays.asList("add", "remove", "shop", "door", "spawn", "manager",
                    "managerpad", "incomepad", "luckpad", "storagepad", "rebirthpad", "pen", "penpad", "list"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("money") && sender.hasPermission(MONEY_PERMISSION)) {
            return complete(args[1], mobHeist.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
        }
        return Collections.emptyList();
    }

    private List<String> complete(String input, Collection<String> options) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void sendUsage(CommandSender sender) {
        boolean plot = sender.hasPermission(PLOT_PERMISSION);
        boolean reload = sender.hasPermission(RELOAD_PERMISSION);
        boolean money = sender.hasPermission(MONEY_PERMISSION);
        if (!plot && !reload && !money) {
            messages.send(sender, "no-permission");
            return;
        }
        if (money) {
            messages.send(sender, "usage.money");
        }
        if (plot) {
            sendPlotUsage(sender);
        }
        if (reload) {
            messages.send(sender, "usage.reload");
        }
    }

    private void sendPlotUsage(CommandSender sender) {
        messages.send(sender, "usage.plot-add");
        messages.send(sender, "usage.plot-remove");
        messages.send(sender, "usage.plot-point");
        messages.send(sender, "usage.plot-pen");
        messages.send(sender, "usage.plot-pen-pad");
        messages.send(sender, "usage.plot-list");
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    private int parseNumber(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void money(CommandSender sender, String[] args) {
        if (!checkPermission(sender, MONEY_PERMISSION)) {
            return;
        }
        if (args.length < 3) {
            messages.send(sender, "usage.money");
            return;
        }

        Player target = mobHeist.getServer().getPlayerExact(args[1]);
        Base base = target == null ? null : baseManager.getBase(target);
        if (base == null) {
            messages.send(sender, "money.no-base");
            return;
        }
        int amount = parseNumber(args[2]);
        if (amount < 0) {
            messages.send(sender, "money.wrong-amount");
            return;
        }

        base.getData().setMoney(base.getData().getMoney() + amount);
        sender.sendMessage(messages.get("money.given")
                .replace("{player}", target.getName())
                .replace("{money}", Numbers.format(amount)));
    }

    private void reload(CommandSender sender) {
        if (!checkPermission(sender, RELOAD_PERMISSION)) {
            return;
        }

        mobHeist.reloadConfig();
        mobManager.load();
        upgradeManager.load();
        messages.load();
        messages.send(sender, "reloaded");
    }

    private void plot(Player player, String[] args) {
        if (!checkPermission(player, PLOT_PERMISSION)) {
            return;
        }
        if (args.length < 2) {
            sendPlotUsage(player);
            return;
        }

        String action = args[1].toLowerCase();
        if (action.equals("add")) {
            Plot plot = plotManager.addPlot(player.getLocation());
            player.sendMessage(messages.get("plot.added").replace("{number}", String.valueOf(plot.getNumber())));
            return;
        }
        if (action.equals("list")) {
            sendPlotList(player);
            return;
        }

        Plot plot = plotManager.getNearest(player.getLocation());
        if (plot == null) {
            messages.send(player, "plot.no-plots");
            return;
        }
        if (action.equals("remove")) {
            removePlot(player, plot);
            return;
        }

        PlotTemplate template = plotManager.getTemplate();
        Vector blockOffset = plot.getOffset(player.getLocation().getBlock().getLocation());
        switch (action) {
            case "shop":
                template.setShop(blockOffset);
                break;
            case "door":
                template.setDoor(blockOffset);
                break;
            case "spawn":
                template.setSpawn(plot.getOffset(player.getLocation()));
                template.setSpawnYaw(player.getLocation().getYaw());
                break;
            case "manager":
                template.setManager(blockOffset);
                break;
            case "managerpad":
                template.setManagerPad(blockOffset);
                break;
            case "incomepad":
                template.setIncomePad(blockOffset);
                break;
            case "luckpad":
                template.setLuckPad(blockOffset);
                break;
            case "storagepad":
                template.setStoragePad(blockOffset);
                break;
            case "rebirthpad":
                template.setRebirthPad(blockOffset);
                break;
            case "pen":
                template.getPens().add(blockOffset);
                break;
            case "penpad":
                template.getPenPads().add(blockOffset);
                break;
            default:
                sendPlotUsage(player);
                return;
        }

        plotManager.save();
        if (action.equals("pen")) {
            player.sendMessage(messages.get("plot.pen-added").replace("{number}", String.valueOf(template.getPens().size())));
        } else if (action.equals("penpad")) {
            player.sendMessage(messages.get("plot.pen-pad-added").replace("{number}", String.valueOf(template.getPenPads().size())));
        } else {
            player.sendMessage(messages.get("plot.point-set")
                    .replace("{point}", action)
                    .replace("{number}", String.valueOf(plot.getNumber())));
        }
    }

    private void removePlot(Player player, Plot plot) {
        Player owner = plot.getOwner();
        if (owner != null) {
            mobHeist.getStealManager().cancel(owner);
            baseManager.remove(owner);
            plotManager.release(owner);
            messages.send(owner, "plot.lost");
        }

        int number = plot.getNumber();
        plotManager.removePlot(plot);
        player.sendMessage(messages.get("plot.removed").replace("{number}", String.valueOf(number)));
    }

    private void sendPlotList(Player player) {
        player.sendMessage(messages.get("plot.list")
                .replace("{count}", String.valueOf(plotManager.getPlots().size())));
        for (Plot plot : plotManager.getPlots()) {
            String owner = plot.getOwner() != null ? plot.getOwner().getName() : messages.get("plot.free");
            player.sendMessage(messages.get("plot.list-entry")
                    .replace("{number}", String.valueOf(plot.getNumber()))
                    .replace("{owner}", owner));
        }
    }
}
