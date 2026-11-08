package ru.glavtoy.mobheist;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.glavtoy.mobheist.base.BaseManager;
import ru.glavtoy.mobheist.command.MobHeistCommand;
import ru.glavtoy.mobheist.config.ConfigLoader;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.data.YamlPlayerStorage;
import ru.glavtoy.mobheist.hud.ScoreboardHud;
import ru.glavtoy.mobheist.listener.BaseListener;
import ru.glavtoy.mobheist.listener.PlayerListener;
import ru.glavtoy.mobheist.mob.MobManager;
import ru.glavtoy.mobheist.plot.PlotManager;
import ru.glavtoy.mobheist.steal.StealManager;
import ru.glavtoy.mobheist.upgrade.UpgradeManager;

public final class MobHeist extends JavaPlugin {

    private BaseManager baseManager;
    @Getter
    private StealManager stealManager;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        ConfigLoader configLoader = new ConfigLoader(this);
        Messages messages = new Messages(configLoader);
        messages.load();
        MobManager mobManager = new MobManager(this, configLoader);
        mobManager.load();
        UpgradeManager upgradeManager = new UpgradeManager(this, messages);
        upgradeManager.load();
        PlotManager plotManager = new PlotManager(this);
        plotManager.load();

        baseManager = new BaseManager(this, mobManager, upgradeManager,
                new YamlPlayerStorage(this, mobManager), new ScoreboardHud(messages), messages);
        baseManager.start();
        stealManager = new StealManager(this, baseManager, messages);
        stealManager.start();

        PlayerListener playerListener = new PlayerListener(plotManager, baseManager, stealManager, messages);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(new BaseListener(baseManager, mobManager, stealManager, messages), this);
        for (Player player : getServer().getOnlinePlayers()) {
            playerListener.join(player);
        }

        MobHeistCommand command = new MobHeistCommand(this, plotManager, baseManager, mobManager, upgradeManager, messages);
        getCommand("mobheist").setExecutor(command);
        getCommand("mobheist").setTabCompleter(command);
    }

    @Override
    public void onDisable() {
        stealManager.cancelAll();
        baseManager.removeAll();
    }
}
