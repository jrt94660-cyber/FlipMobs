package com.flipmobs.listener;

import com.flipmobs.FlipMobs;
import com.flipmobs.economy.EconomyHook;
import com.flipmobs.util.MoneyFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobBountyListener implements Listener {
    private final FlipMobs plugin;
    private final EconomyHook economy;
    private final NamespacedKey bountyKey;
    private final Map<UUID, Hit> recentHits = new ConcurrentHashMap<>();

    public MobBountyListener(FlipMobs plugin, EconomyHook economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.bountyKey = new NamespacedKey(plugin, "mob_bounty");
    }

    public void initializeLoadedMobs() {
        for (var world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                ensureBounty(entity);
            }
        }
    }

    public void refreshLoadedMobLabels() {
        for (var world : Bukkit.getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (isEligible(entity)) {
                    if (getBounty(entity) <= 0) assignBounty(entity);
                    updateLabel(entity);
                }
            }
        }
    }

    public void shutdown() {
        recentHits.clear();
    }

    /*
     * EntitySpawnEvent also catches plugin-spawned living entities that do not
     * necessarily go through CreatureSpawnEvent. Apply the label one tick later
     * so other spawn listeners have finished modifying the entity.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof LivingEntity living && isEligible(living)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (living.isValid() && !living.isDead()) ensureBounty(living);
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof LivingEntity living && isEligible(living)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (living.isValid() && !living.isDead()) ensureBounty(living);
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDirectDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living instanceof Player) return;
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker != null) remember(living, attacker);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living instanceof Player) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return;

        Player attacker = recentAttacker(living);
        if (attacker == null) attacker = nearestPlayer(living.getLocation(), explosionRadius());
        if (attacker != null) remember(living, attacker);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplosion(EntityExplodeEvent event) {
        Player source = resolvePlayer(event.getEntity());
        if (event.getEntity() instanceof TNTPrimed tnt && source == null) {
            source = resolvePlayer(tnt.getSource());
        }
        markExplosion(event.getLocation(), source);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplosion(BlockExplodeEvent event) {
        markExplosion(event.getBlock().getLocation(), null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (!isEligible(mob)) return;

        // Safety net: even if another plugin skipped the spawn event, a mob
        // that dies still receives a bounty before its payout is calculated.
        if (getBounty(mob) <= 0) assignBounty(mob);
        double amount = getBounty(mob);
        if (amount <= 0) return;

        Player killer = mob.getKiller();
        if (killer == null) killer = recentAttacker(mob);
        if (killer == null && !plugin.getConfig().getBoolean(
                "reward.require-player-attribution-for-explosions", true)) {
            killer = nearestPlayer(mob.getLocation(), explosionRadius());
        }
        if (killer == null) return;

        if (!economy.deposit(killer, amount)) {
            plugin.getLogger().warning("Could not pay " + MoneyFormatter.format(plugin, amount)
                    + " to " + killer.getName() + ". Vault economy is unavailable or rejected the deposit.");
            return;
        }

        String message = plugin.getConfig().getString(
                "reward.actionbar", "&a+%symbol%%amount% &7Mob bounty claimed!");
        message = message.replace("%symbol%", plugin.getConfig().getString("economy.symbol", "$"))
                .replace("%amount%", MoneyFormatter.format(plugin, amount));
        killer.sendActionBar(Component.text(FlipMobs.color(message)));
        recentHits.remove(mob.getUniqueId());
    }

    private void ensureBounty(LivingEntity entity) {
        if (!isEligible(entity)) return;
        if (getBounty(entity) <= 0) assignBounty(entity);
        updateLabel(entity);
    }

    private void assignBounty(LivingEntity entity) {
        double minimum = Math.max(0, plugin.getConfig().getDouble("bounty.minimum", 100000));
        double maximum = Math.max(minimum, plugin.getConfig().getDouble("bounty.maximum", 30000000));
        double amount = Math.floor((minimum + Math.random() * (maximum - minimum)) * 100.0) / 100.0;
        entity.getPersistentDataContainer().set(bountyKey, PersistentDataType.DOUBLE, amount);
    }

    private void updateLabel(LivingEntity entity) {
        String format = plugin.getConfig().getString("bounty.name-format", "&6$%amount%");
        String label = format.replace("%amount%", MoneyFormatter.format(plugin, getBounty(entity)));
        entity.setCustomName(FlipMobs.color(label));
        entity.setCustomNameVisible(true);
    }

    private double getBounty(LivingEntity entity) {
        Double amount = entity.getPersistentDataContainer().get(bountyKey, PersistentDataType.DOUBLE);
        return amount == null ? 0 : amount;
    }

    private boolean isEligible(LivingEntity entity) {
        if (entity instanceof Player || entity instanceof ArmorStand) return false;
        return !plugin.getConfig().getStringList("bounty.exclude").contains(entity.getType().name());
    }

    private double explosionRadius() {
        return Math.max(0, plugin.getConfig().getDouble("reward.explosion-attribution-radius", 12.0));
    }

    private void remember(LivingEntity mob, Player player) {
        long expiry = System.currentTimeMillis()
                + plugin.getConfig().getLong("reward.last-attacker-seconds", 30) * 1000L;
        recentHits.put(mob.getUniqueId(), new Hit(player.getUniqueId(), expiry));
    }

    private Player recentAttacker(LivingEntity mob) {
        Hit hit = recentHits.get(mob.getUniqueId());
        if (hit == null) return null;
        if (hit.expiresAt() < System.currentTimeMillis()) {
            recentHits.remove(mob.getUniqueId());
            return null;
        }
        return Bukkit.getPlayer(hit.playerId());
    }

    private void markExplosion(Location location, Player source) {
        double radius = explosionRadius();
        for (Entity entity : location.getWorld().getNearbyEntities(location, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || !isEligible(living)) continue;
            Player responsible = source;
            if (responsible == null) responsible = recentAttacker(living);
            if (responsible == null) responsible = nearestPlayer(location, radius);
            if (responsible != null) remember(living, responsible);
        }
    }

    private Player nearestPlayer(Location location, double radius) {
        return location.getWorld().getNearbyPlayers(location, radius).stream()
                .min(Comparator.comparingDouble(player ->
                        player.getLocation().distanceSquared(location)))
                .orElse(null);
    }

    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        if (entity instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            return player;
        }
        if (entity instanceof Tameable tameable && tameable.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }

    private record Hit(UUID playerId, long expiresAt) {}
}