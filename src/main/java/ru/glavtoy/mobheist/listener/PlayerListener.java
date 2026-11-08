package ru.glavtoy.mobheist.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.glavtoy.mobheist.base.BaseManager;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.plot.Plot;
import ru.glavtoy.mobheist.plot.PlotManager;
import ru.glavtoy.mobheist.steal.StealManager;

@RequiredArgsConstructor
public final class PlayerListener implements Listener {

    private final PlotManager plotManager;
    private final BaseManager baseManager;
    private final StealManager stealManager;
    private final Messages messages;

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            baseManager.preload(event.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        join(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stealManager.cancel(event.getPlayer());
        baseManager.remove(event.getPlayer());
        plotManager.release(event.getPlayer());
    }

    public void join(Player player) {
        Plot plot = plotManager.claim(player);
        if (plot == null) {
            messages.send(player, "plot.full");
            return;
        }

        baseManager.create(player, plot);
        player.teleport(plot.getSpawn());
        player.sendMessage(messages.get("plot.claimed").replace("{number}", String.valueOf(plot.getNumber())));
    }
}
