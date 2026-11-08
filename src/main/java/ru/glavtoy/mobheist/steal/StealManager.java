package ru.glavtoy.mobheist.steal;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.glavtoy.mobheist.MobHeist;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.base.BaseManager;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.event.MobRescueEvent;
import ru.glavtoy.mobheist.event.MobStealEvent;
import ru.glavtoy.mobheist.event.MobStolenEvent;
import ru.glavtoy.mobheist.mob.OwnedMob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public final class StealManager {

    private static final double DELIVER_DISTANCE = 3.5;
    private static final int TICKS_PER_SECOND = 20;
    private static final int TITLE_FADE_IN = 5;
    private static final int TITLE_STAY = 40;
    private static final int TITLE_FADE_OUT = 10;

    private final MobHeist mobHeist;
    private final BaseManager baseManager;
    private final Messages messages;
    private final Map<Player, Theft> thefts = new HashMap<>();

    public void start() {
        mobHeist.getServer().getScheduler().runTaskTimer(mobHeist, this::tick, TICKS_PER_SECOND, TICKS_PER_SECOND);
    }

    public Player getThief(Entity entity) {
        Theft theft = thefts.get(entity);
        if (theft != null) {
            return theft.getThief();
        }
        return thefts.values().stream()
                .filter(value -> value.getEntity().equals(entity))
                .map(Theft::getThief)
                .findFirst()
                .orElse(null);
    }

    public void steal(Player thief, Base victim, int pen) {
        Base base = baseManager.getBase(thief);
        if (base == null || victim.isStolen(pen)) {
            return;
        }

        FileConfiguration config = mobHeist.getConfig();
        long now = System.currentTimeMillis();
        long wait = victim.getLastRobbed() + TimeUnit.SECONDS.toMillis(config.getLong("steal.cooldown-seconds")) - now;
        if (thefts.containsKey(thief)) {
            messages.send(thief, "steal.busy");
            return;
        }
        if (victim.isLocked()) {
            messages.send(thief, "steal.locked");
            return;
        }
        if (base.getFreePen() < 0) {
            messages.send(thief, "steal.no-pens");
            return;
        }
        if (victim.getData().getMobs().size() < config.getInt("steal.min-mobs")) {
            messages.send(thief, "steal.few-mobs");
            return;
        }
        if (wait > 0) {
            thief.sendMessage(messages.get("steal.cooldown").replace("{seconds}", String.valueOf(toSeconds(wait))));
            return;
        }

        OwnedMob mob = victim.getData().getMobs().get(pen);
        MobStealEvent event = new MobStealEvent(thief, victim, mob);
        mobHeist.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        LivingEntity entity = victim.getEntity(pen);
        long carry = config.getLong("steal.carry-seconds");
        thefts.put(thief, new Theft(thief, victim, pen, mob, entity, now + TimeUnit.SECONDS.toMillis(carry)));
        victim.setStolen(pen, true);
        victim.setLastRobbed(now);

        thief.addPassenger(entity);
        thief.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) carry * TICKS_PER_SECOND, 1));
        thief.setGlowing(true);
        thief.sendMessage(messages.get("steal.started").replace("{mob}", mob.getName()));

        Player owner = victim.getOwner();
        owner.sendMessage(messages.get("steal.robbed")
                .replace("{thief}", thief.getName())
                .replace("{mob}", mob.getName()));
        owner.sendTitle(messages.get("steal.robbed-title"), messages.get("steal.robbed-subtitle")
                .replace("{thief}", thief.getName())
                .replace("{mob}", mob.getName()), TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT);
        owner.playSound(owner.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
    }

    public void deliver(Player thief, Location location) {
        Theft theft = thefts.get(thief);
        Base base = baseManager.getBase(thief);
        if (theft == null || base == null) {
            return;
        }
        int pen = base.getFreePen(location, DELIVER_DISTANCE);
        if (pen < 0) {
            return;
        }

        end(theft);
        Base victim = theft.getVictim();
        OwnedMob mob = theft.getMob();
        if (victim.getData().getMobs().get(theft.getPen()) != mob) {
            return;
        }
        victim.getData().getMobs().remove(theft.getPen());
        victim.removeMob(theft.getPen());
        baseManager.addMob(base, pen, new OwnedMob(mob.getType(), mob.getMutation(), 1, 0));
        mobHeist.getServer().getPluginManager().callEvent(new MobStolenEvent(thief, victim, mob));

        thief.sendMessage(messages.get("steal.done").replace("{mob}", mob.getName()));
        thief.playSound(thief.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        victim.getOwner().sendMessage(messages.get("steal.lost")
                .replace("{thief}", thief.getName())
                .replace("{mob}", mob.getName()));
    }

    public void rescue(Player thief, Player rescuer) {
        Theft theft = thefts.get(thief);
        if (theft == null || thief.equals(rescuer)) {
            return;
        }

        giveBack(theft);
        mobHeist.getServer().getPluginManager().callEvent(new MobRescueEvent(thief, rescuer, theft.getVictim(), theft.getMob()));
        String mob = theft.getMob().getName();
        Player owner = theft.getVictim().getOwner();
        owner.sendMessage(messages.get("steal.rescued").replace("{mob}", mob));
        owner.playSound(owner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        thief.sendMessage(messages.get("steal.rescued-thief")
                .replace("{player}", rescuer.getName())
                .replace("{mob}", mob));
    }

    public void cancel(Player player) {
        new ArrayList<>(thefts.values()).stream()
                .filter(theft -> theft.getThief().equals(player) || theft.getVictim().getOwner().equals(player))
                .forEach(this::giveBack);
    }

    public void cancelAll() {
        new ArrayList<>(thefts.values()).forEach(this::giveBack);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Theft theft : new ArrayList<>(thefts.values())) {
            long left = theft.getUntil() - now;
            Player thief = theft.getThief();
            if (left <= 0) {
                giveBack(theft);
                thief.sendMessage(messages.get("steal.timeout").replace("{mob}", theft.getMob().getName()));
                continue;
            }
            baseManager.actionBar(thief, messages.get("steal.carry").replace("{seconds}", String.valueOf(toSeconds(left))));
        }
    }

    private void giveBack(Theft theft) {
        end(theft);
        theft.getVictim().returnMob(theft.getPen());
    }

    private void end(Theft theft) {
        Player thief = theft.getThief();
        thefts.remove(thief);
        thief.removePassenger(theft.getEntity());
        thief.removePotionEffect(PotionEffectType.SLOW);
        thief.setGlowing(false);
        theft.getVictim().setStolen(theft.getPen(), false);
    }

    private long toSeconds(long millis) {
        return TimeUnit.MILLISECONDS.toSeconds(millis) + 1;
    }
}
