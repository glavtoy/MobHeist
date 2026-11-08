package ru.glavtoy.mobheist.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import ru.glavtoy.mobheist.base.Base;
import ru.glavtoy.mobheist.base.BaseManager;
import ru.glavtoy.mobheist.config.Messages;
import ru.glavtoy.mobheist.menu.Menu;
import ru.glavtoy.mobheist.menu.MobMenu;
import ru.glavtoy.mobheist.menu.ShopMenu;
import ru.glavtoy.mobheist.mob.MobManager;
import ru.glavtoy.mobheist.steal.StealManager;

@RequiredArgsConstructor
public final class BaseListener implements Listener {

    private static final double SHOP_DISTANCE = 2.5;

    private final BaseManager baseManager;
    private final MobManager mobManager;
    private final StealManager stealManager;
    private final Messages messages;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Base base = baseManager.getBase(event.getPlayer());
        if (base == null || base.getPlot().getShop() == null) {
            return;
        }
        Location shop = base.getPlot().getShop().clone().add(0.5, 0.5, 0.5);
        Location clicked = event.getClickedBlock().getLocation().add(0.5, 0.5, 0.5);
        if (clicked.getWorld() == shop.getWorld() && clicked.distance(shop) <= SHOP_DISTANCE) {
            event.setCancelled(true);
            ShopMenu menu = new ShopMenu(base, baseManager, mobManager, messages);
            base.setMenu(menu);
            menu.open();
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        Player thief = stealManager.getThief(clicked);
        if (thief == null && !clicked.getScoreboardTags().contains(BaseManager.ENTITY_TAG)) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (thief != null) {
            stealManager.rescue(thief, player);
            return;
        }
        Base owner = baseManager.getOwnerBase(clicked);
        if (owner == null) {
            return;
        }

        int pen = owner.getPen(clicked);
        if (!owner.getOwner().equals(player)) {
            stealManager.steal(player, owner, pen);
            return;
        }
        MobMenu menu = new MobMenu(owner, pen, baseManager, mobManager, messages);
        owner.setMenu(menu);
        menu.open();
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        Player thief = stealManager.getThief(event.getEntity());
        if (thief != null) {
            stealManager.rescue(thief, player);
            return;
        }
        Base base = baseManager.getBase(player);
        Integer pen = base == null ? null : base.getPen(event.getEntity());
        if (pen != null) {
            baseManager.collect(base, pen);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        boolean locked = baseManager.getBases().stream()
                .anyMatch(base -> base.isLocked() && !base.getOwner().equals(player)
                        && base.getPlot().contains(to) && !base.getPlot().contains(from));
        if (locked) {
            event.setCancelled(true);
            baseManager.actionBar(player, messages.get("lock.wall"));
            return;
        }

        Base base = baseManager.getBase(player);
        if (base == null) {
            return;
        }
        if (base.isPad(to)) {
            baseManager.usePad(base);
        }
        if (base.isLock(to)) {
            baseManager.useLock(base);
        }
        stealManager.deliver(player, to);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getScoreboardTags().contains(BaseManager.ENTITY_TAG)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Menu menu = getOpenMenu(event.getWhoClicked(), event.getView().getTopInventory());
        if (menu == null) {
            return;
        }

        event.setCancelled(true);
        if (menu.getInventory().equals(event.getClickedInventory())) {
            menu.click(event.getSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (getOpenMenu(event.getWhoClicked(), event.getView().getTopInventory()) != null) {
            event.setCancelled(true);
        }
    }

    private Menu getOpenMenu(HumanEntity entity, Inventory top) {
        if (!(entity instanceof Player)) {
            return null;
        }
        Player player = (Player) entity;
        Base base = baseManager.getBase(player);
        if (base == null || base.getMenu() == null) {
            return null;
        }
        return base.getMenu().getInventory().equals(top) ? base.getMenu() : null;
    }
}
