package ru.glavtoy.mobheist.hud;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.data.PlayerData;
import ru.glavtoy.mobheist.util.Numbers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class ScoreboardHud implements BaseHud {

    private static final int MAX_LINES = 15;
    private static final int PART_LENGTH = 16;

    private final Messages messages;
    private final Map<Player, Scoreboard> scoreboards = new HashMap<>();

    @Override
    public void update(Base base, double income) {
        Player player = base.getOwner();
        Scoreboard scoreboard = scoreboards.computeIfAbsent(player, key -> createScoreboard(player));
        PlayerData data = base.getData();

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add(messages.get("hud.money").replace("{money}", Numbers.format(data.getMoney())));
        lines.add(messages.get("hud.income").replace("{income}", Numbers.format(income)));
        lines.add("");
        lines.add(messages.get("hud.pens")
                .replace("{mobs}", String.valueOf(data.getMobs().size()))
                .replace("{pens}", String.valueOf(data.getPens())));
        lines.add(messages.get("hud.rebirths").replace("{rebirths}", String.valueOf(data.getRebirths())));
        lines.add(messages.get("hud.plot").replace("{number}", String.valueOf(base.getPlot().getNumber())));
        setLines(scoreboard, lines);
    }

    @Override
    public void remove(Player player) {
        scoreboards.remove(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private Scoreboard createScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("mobheist", "dummy");
        objective.setDisplayName(messages.get("hud.title"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        player.setScoreboard(scoreboard);
        return scoreboard;
    }

    private void setLines(Scoreboard scoreboard, List<String> lines) {
        Objective objective = scoreboard.getObjective("mobheist");
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = ChatColor.values()[i] + "" + ChatColor.RESET;
            Team team = scoreboard.getTeam("line" + i);

            if (i >= lines.size()) {
                if (team != null) {
                    scoreboard.resetScores(entry);
                    team.unregister();
                }
                continue;
            }

            if (team == null) {
                team = scoreboard.registerNewTeam("line" + i);
                team.addEntry(entry);
            }
            setText(team, lines.get(i));
            objective.getScore(entry).setScore(lines.size() - i);
        }
    }

    private void setText(Team team, String text) {
        if (text.length() <= PART_LENGTH) {
            team.setPrefix(text);
            team.setSuffix("");
            return;
        }

        int split = text.charAt(PART_LENGTH - 1) == ChatColor.COLOR_CHAR ? PART_LENGTH - 1 : PART_LENGTH;
        String prefix = text.substring(0, split);
        String suffix = ChatColor.getLastColors(prefix) + text.substring(split);
        team.setPrefix(prefix);
        team.setSuffix(suffix.length() > PART_LENGTH ? suffix.substring(0, PART_LENGTH) : suffix);
    }
}
