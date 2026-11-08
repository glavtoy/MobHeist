package ru.glavtoy.mobheist.base;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import ru.glavtoy.mobheist.MobHeist;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.data.PlayerData;
import ru.glavtoy.mobheist.data.PlayerStorage;
import ru.glavtoy.mobheist.event.BaseLockEvent;
import ru.glavtoy.mobheist.event.MobPurchaseEvent;
import ru.glavtoy.mobheist.event.MobUpgradeEvent;
import ru.glavtoy.mobheist.event.RebirthEvent;
import ru.glavtoy.mobheist.event.UpgradePurchaseEvent;
import ru.glavtoy.mobheist.hud.BaseHud;
import ru.glavtoy.mobheist.menu.RebirthMenu;
import ru.glavtoy.mobheist.mob.MobManager;
import ru.glavtoy.mobheist.mob.MobType;
import ru.glavtoy.mobheist.mob.OwnedMob;
import ru.glavtoy.mobheist.plot.Plot;
import ru.glavtoy.mobheist.upgrade.Step;
import ru.glavtoy.mobheist.upgrade.UpgradeManager;
import ru.glavtoy.mobheist.util.Numbers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public final class BaseManager {

    public static final String ENTITY_TAG = "mobheist";
    private static final int TICKS_PER_SECOND = 20;
    private static final int TITLE_FADE_IN = 10;
    private static final int TITLE_STAY = 60;
    private static final int TITLE_FADE_OUT = 20;

    private final MobHeist mobHeist;
    private final MobManager mobManager;
    private final UpgradeManager upgradeManager;
    private final PlayerStorage storage;
    private final BaseHud hud;
    private final Messages messages;
    private final Map<Player, Base> bases = new HashMap<>();
    private final Map<UUID, PlayerData> loaded = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerData> saving = new ConcurrentHashMap<>();

    public void start() {
        for (World world : mobHeist.getServer().getWorlds()) {
            world.getEntities().stream()
                    .filter(entity -> entity.getScoreboardTags().contains(ENTITY_TAG))
                    .forEach(Entity::remove);
        }
        mobHeist.getServer().getScheduler().runTaskTimer(mobHeist, this::tick, TICKS_PER_SECOND, TICKS_PER_SECOND);
        long autosave = mobHeist.getConfig().getLong("autosave-seconds") * TICKS_PER_SECOND;
        mobHeist.getServer().getScheduler().runTaskTimer(mobHeist, this::saveAll, autosave, autosave);
    }

    public void preload(UUID uuid) {
        PlayerData data = saving.get(uuid);
        loaded.put(uuid, data != null ? data : storage.load(uuid));
    }

    public void create(Player player, Plot plot) {
        PlayerData data = loaded.remove(player.getUniqueId());
        if (data == null) {
            data = storage.load(player.getUniqueId());
        }
        if (data.getPens() == 0) {
            data.setPens(upgradeManager.getStartPens());
        }

        Base base = new Base(player, plot, data);
        base.spawnShopHologram(messages.get("hologram.shop"), messages.get("hologram.shop-hint"));
        data.getMobs().keySet().removeIf(pen -> pen >= plot.getPens().size());
        data.getMobs().forEach((pen, mob) -> {
            base.spawnMob(pen, mob);
            updateMob(base, pen);
        });
        base.setSecondsToCollect(upgradeManager.getAutomation(data.getAutomationLevel()).getInterval());
        base.clearPads();
        base.setLock(plot.getDoor(), messages.get("lock.title"));
        updateLock(base);
        refresh(base);
        bases.put(player, base);
    }

    public void remove(Player player) {
        Base base = removeBase(player);
        if (base != null) {
            saveAsync(player.getUniqueId(), base.getData());
        }
    }

    public void removeAll() {
        for (Player player : new ArrayList<>(bases.keySet())) {
            storage.save(player.getUniqueId(), removeBase(player).getData());
        }
    }

    public void saveAll() {
        bases.forEach((player, base) -> saveAsync(player.getUniqueId(), base.getData()));
    }

    private Base removeBase(Player player) {
        Base base = bases.remove(player);
        if (base != null) {
            base.remove();
            hud.remove(player);
        }
        return base;
    }

    private void saveAsync(UUID uuid, PlayerData data) {
        saving.put(uuid, data);
        mobHeist.getServer().getScheduler().runTaskAsynchronously(mobHeist, () -> {
            storage.save(uuid, data);
            saving.remove(uuid, data);
        });
    }

    public Collection<Base> getBases() {
        return bases.values();
    }

    public Base getOwnerBase(Entity entity) {
        return bases.values().stream()
                .filter(base -> base.getPen(entity) != null)
                .findFirst()
                .orElse(null);
    }

    public Base getBase(Player player) {
        return bases.get(player);
    }

    public double getIncome(Base base, OwnedMob mob) {
        return getIncome(base, mob, mob.getLevel());
    }

    public double getIncome(Base base, OwnedMob mob, int level) {
        PlayerData data = base.getData();
        return mobManager.getIncome(mob, level) * upgradeManager.getIncome(data.getIncomeLevel()).getMultiplier()
                * upgradeManager.getRebirthMultiplier(data.getRebirths());
    }

    public double getIncome(Base base) {
        return base.getData().getMobs().values().stream().mapToDouble(mob -> getIncome(base, mob)).sum();
    }

    public void usePad(Base base) {
        Step step = base.getPadStep();
        if (step == Step.REBIRTH) {
            PlayerData data = base.getData();
            RebirthMenu menu = new RebirthMenu(base, this, messages, upgradeManager.getPrice(base, step),
                    upgradeManager.getRebirthMultiplier(data.getRebirths() + 1));
            base.setMenu(menu);
            menu.open();
            return;
        }
        if (step == null) {
            return;
        }
        UpgradePurchaseEvent event = new UpgradePurchaseEvent(base, step, upgradeManager.getPrice(base, step));
        mobHeist.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || !upgradeManager.buy(base, step)) {
            return;
        }

        Location location = base.getPad().clone().add(0.5, 0.5, 0.5);
        location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, location, 20, 0.5, 0.5, 0.5, 0);
        if (step == Step.MANAGER) {
            base.setSecondsToCollect(upgradeManager.getAutomation(base.getData().getAutomationLevel()).getInterval());
        }
        refresh(base);
    }

    public void rebirth(Base base) {
        double price = upgradeManager.getPrice(base, Step.REBIRTH);
        RebirthEvent event = new RebirthEvent(base, price);
        mobHeist.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || !upgradeManager.pay(base, price)) {
            return;
        }

        PlayerData data = base.getData();
        data.getMobs().keySet().forEach(base::removeMob);
        data.getMobs().clear();
        data.setMoney(mobHeist.getConfig().getDouble("start-money"));
        data.setPens(upgradeManager.getStartPens());
        data.setIncomeLevel(1);
        data.setAutomationLevel(1);
        data.setLuckLevel(1);
        data.setStorageLevel(1);
        data.setRebirths(data.getRebirths() + 1);
        base.removeManager();
        base.setSecondsToCollect(0);
        refresh(base);

        Player owner = base.getOwner();
        String multiplier = Numbers.format(upgradeManager.getRebirthMultiplier(data.getRebirths()));
        owner.sendMessage(messages.get("base.rebirth").replace("{multiplier}", multiplier));
        owner.sendTitle(messages.get("base.rebirth-title"), messages.get("base.rebirth-subtitle")
                .replace("{multiplier}", multiplier), TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT);
        owner.playSound(owner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        owner.getWorld().spawnParticle(Particle.TOTEM, owner.getLocation().add(0, 1, 0), 60, 0.5, 1, 0.5, 0.3);
    }

    public void buyMob(Base base, MobType type) {
        int pen = base.getFreePen();
        Player owner = base.getOwner();
        if (base.getData().getRebirths() < type.getRebirth()) {
            owner.sendMessage(messages.get("base.mob-locked").replace("{rebirth}", String.valueOf(type.getRebirth())));
            return;
        }
        if (pen < 0) {
            messages.send(owner, "base.no-pens");
            return;
        }
        MobPurchaseEvent event = new MobPurchaseEvent(base, type);
        mobHeist.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || !upgradeManager.pay(base, type.getPrice())) {
            return;
        }

        double luck = upgradeManager.getLuck(base.getData().getLuckLevel()).getMultiplier();
        OwnedMob mob = new OwnedMob(type, mobManager.rollMutation(luck), 1, 0);
        addMob(base, pen, mob);

        Location location = base.getPlot().getPens().get(pen).clone().add(0.5, 1, 0.5);
        location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, location, 15, 0.4, 0.5, 0.4, 0);
        owner.sendMessage(messages.get(mob.getMutation() == null ? "base.bought" : "base.bought-mutation")
                .replace("{mob}", mob.getName()));
    }

    public void addMob(Base base, int pen, OwnedMob mob) {
        base.getData().getMobs().put(pen, mob);
        base.spawnMob(pen, mob);
        updateMob(base, pen);
    }

    public void useLock(Base base) {
        long now = System.currentTimeMillis();
        Player owner = base.getOwner();
        if (now < base.getLockReadyAt()) {
            messages.send(owner, "lock.not-ready");
            return;
        }

        BaseLockEvent event = new BaseLockEvent(base);
        mobHeist.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        FileConfiguration config = mobHeist.getConfig();
        long seconds = config.getLong("steal.lock-seconds");
        base.setLockedUntil(now + TimeUnit.SECONDS.toMillis(seconds));
        base.setLockReadyAt(base.getLockedUntil() + TimeUnit.SECONDS.toMillis(config.getLong("steal.lock-cooldown-seconds")));
        owner.sendMessage(messages.get("lock.locked").replace("{seconds}", String.valueOf(seconds)));
        owner.playSound(owner.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1, 1);
        updateLock(base);
    }

    public void upgradeMob(Base base, int pen) {
        OwnedMob mob = base.getData().getMobs().get(pen);
        if (mob.getLevel() >= mobManager.getMaxLevel()) {
            messages.send(base.getOwner(), "upgrade.max");
            return;
        }
        double price = mobManager.getUpgradePrice(mob);
        MobUpgradeEvent event = new MobUpgradeEvent(base, mob, price);
        mobHeist.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled() && upgradeManager.pay(base, price)) {
            mob.setLevel(mob.getLevel() + 1);
            updateMob(base, pen);
            base.getOwner().sendMessage(messages.get("base.mob-upgraded")
                    .replace("{mob}", mob.getName())
                    .replace("{level}", String.valueOf(mob.getLevel())));
        }
    }

    public void sellMob(Base base, int pen) {
        OwnedMob mob = base.getData().getMobs().remove(pen);
        base.removeMob(pen);

        double money = mobManager.getSellPrice(mob) + mob.getStored();
        PlayerData data = base.getData();
        data.setMoney(data.getMoney() + money);

        Player owner = base.getOwner();
        owner.sendMessage(messages.get("base.sold")
                .replace("{mob}", mob.getName())
                .replace("{money}", Numbers.format(money)));
        owner.playSound(owner.getLocation(), Sound.ENTITY_VILLAGER_YES, 1, 1);
    }

    public void closeMenu(Base base) {
        mobHeist.getServer().getScheduler().runTask(mobHeist, () -> base.getOwner().closeInventory());
    }

    public void collect(Base base, int pen) {
        OwnedMob mob = base.getData().getMobs().get(pen);
        if (mob.getStored() < 1) {
            return;
        }

        PlayerData data = base.getData();
        data.setMoney(data.getMoney() + mob.getStored());
        actionBar(base.getOwner(), messages.get("base.collected").replace("{money}", Numbers.format(mob.getStored())));
        base.getOwner().playSound(base.getOwner().getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        mob.setStored(0);
        updateMob(base, pen);
    }

    private void collectAll(Base base) {
        double total = base.getData().getMobs().values().stream().mapToDouble(OwnedMob::getStored).sum();
        if (total < 1) {
            return;
        }

        PlayerData data = base.getData();
        data.setMoney(data.getMoney() + total);
        data.getMobs().values().forEach(mob -> mob.setStored(0));
        actionBar(base.getOwner(), messages.get("base.auto-collected").replace("{money}", Numbers.format(total)));
    }

    private void tick() {
        bases.values().forEach(this::tick);
    }

    private void tick(Base base) {
        int storageSeconds = upgradeManager.getStorage(base.getData().getStorageLevel()).getSeconds();
        for (OwnedMob mob : base.getData().getMobs().values()) {
            double income = getIncome(base, mob);
            mob.setStored(Math.min(mob.getStored() + income, income * storageSeconds));
        }

        int interval = upgradeManager.getAutomation(base.getData().getAutomationLevel()).getInterval();
        if (interval > 0) {
            base.setSecondsToCollect(base.getSecondsToCollect() - 1);
            if (base.getSecondsToCollect() <= 0) {
                collectAll(base);
                base.setSecondsToCollect(interval);
            }
        }

        base.getData().getMobs().keySet().forEach(pen -> updateMob(base, pen));
        updatePadPrice(base);
        updateLock(base);
        hud.update(base, getIncome(base));
    }

    private void refresh(Base base) {
        PlayerData data = base.getData();
        Plot plot = base.getPlot();
        base.buildPens(data.getPens());
        if (data.getAutomationLevel() > 1) {
            base.spawnManager(plot.getManager(), messages.get("hologram.manager"));
        }

        Step step = upgradeManager.getNextStep(base);
        if (step == null) {
            step = Step.REBIRTH;
        }
        switch (step) {
            case PEN:
                base.setPad(step, plot.getPenPad(data.getPens()), messages.get("pad.pen"), messages.get("pad.pen-info"));
                break;
            case MANAGER:
                String manager = data.getAutomationLevel() == 1 ? "pad.manager" : "pad.manager-upgrade";
                int interval = upgradeManager.getAutomation(data.getAutomationLevel() + 1).getInterval();
                base.setPad(step, plot.getManagerPad(), messages.get(manager),
                        messages.get("pad.manager-info").replace("{seconds}", String.valueOf(interval)));
                break;
            case INCOME:
                double income = upgradeManager.getIncome(data.getIncomeLevel() + 1).getMultiplier();
                base.setPad(step, plot.getIncomePad(), messages.get("pad.income"),
                        messages.get("pad.income-info").replace("{multiplier}", Numbers.format(income)));
                break;
            case LUCK:
                double luck = upgradeManager.getLuck(data.getLuckLevel() + 1).getMultiplier();
                base.setPad(step, plot.getLuckPad(), messages.get("pad.luck"),
                        messages.get("pad.luck-info").replace("{multiplier}", Numbers.format(luck)));
                break;
            case STORAGE:
                long minutes = TimeUnit.SECONDS.toMinutes(upgradeManager.getStorage(data.getStorageLevel() + 1).getSeconds());
                base.setPad(step, plot.getStoragePad(), messages.get("pad.storage"),
                        messages.get("pad.storage-info").replace("{minutes}", String.valueOf(minutes)));
                break;
            case REBIRTH:
                double rebirth = upgradeManager.getRebirthMultiplier(data.getRebirths() + 1);
                base.setPad(step, plot.getRebirthPad(), messages.get("pad.rebirth"),
                        messages.get("pad.rebirth-info").replace("{multiplier}", Numbers.format(rebirth)));
                break;
        }
        updatePadPrice(base);
    }

    private void updatePadPrice(Base base) {
        if (base.getPadStep() == null) {
            return;
        }
        double price = upgradeManager.getPrice(base, base.getPadStep());
        String key = base.getData().getMoney() >= price ? "pad.price" : "pad.price-locked";
        base.setPadPrice(messages.get(key).replace("{price}", Numbers.format(price)));
    }

    private void updateMob(Base base, int pen) {
        OwnedMob mob = base.getData().getMobs().get(pen);
        String name = messages.get("hologram.mob-name")
                .replace("{mob}", mob.getName())
                .replace("{level}", String.valueOf(mob.getLevel()));
        String text = messages.get("hologram.mob-income")
                .replace("{stored}", Numbers.format(mob.getStored()))
                .replace("{income}", Numbers.format(getIncome(base, mob)));
        base.updateMob(pen, name, base.isStolen(pen) ? messages.get("hologram.mob-stolen") : text, getUpgradeText(base, mob));
    }

    private String getUpgradeText(Base base, OwnedMob mob) {
        if (mob.getLevel() >= mobManager.getMaxLevel()) {
            return messages.get("hologram.mob-max");
        }
        double price = mobManager.getUpgradePrice(mob);
        String key = base.getData().getMoney() >= price ? "hologram.mob-upgrade" : "hologram.mob-upgrade-locked";
        return messages.get(key).replace("{price}", Numbers.format(price));
    }

    private void updateLock(Base base) {
        long now = System.currentTimeMillis();
        if (base.isLocked()) {
            base.setLockStatus(messages.get("lock.closed").replace("{seconds}", toSeconds(base.getLockedUntil() - now)));
        } else if (now < base.getLockReadyAt()) {
            base.setLockStatus(messages.get("lock.recharge").replace("{seconds}", toSeconds(base.getLockReadyAt() - now)));
        } else {
            base.setLockStatus(messages.get("lock.ready"));
        }
    }

    private String toSeconds(long millis) {
        return String.valueOf(TimeUnit.MILLISECONDS.toSeconds(millis) + 1);
    }

    public void actionBar(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
    }
}
