package com.flipmobs.listener;
import com.flipmobs.FlipMobs;
import com.flipmobs.economy.EconomyHook;
import com.flipmobs.util.MoneyFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MobBountyListener implements Listener {
 private final FlipMobs plugin;
 private final EconomyHook economy;
 private final NamespacedKey key;
 private final Map<UUID, Hit> hits = new ConcurrentHashMap<>();
 public MobBountyListener(FlipMobs p, EconomyHook e) { plugin=p; economy=e; key=new NamespacedKey(p,"mob_bounty"); }
 public void initializeLoadedMobs(){ Bukkit.getWorlds().forEach(w->w.getLivingEntities().forEach(this::ensure)); }
 public void refreshLoadedMobLabels(){ Bukkit.getWorlds().forEach(w->w.getLivingEntities().forEach(m->{if(eligible(m)&&value(m)>0) label(m);})); }
 public void shutdown(){ hits.clear(); }

 @EventHandler(priority=EventPriority.MONITOR)
 public void spawn(CreatureSpawnEvent e){ if(plugin.getConfig().getBoolean("bounty.show-on-all-mobs",true)) ensure(e.getEntity()); }
 @EventHandler(priority=EventPriority.MONITOR)
 public void chunk(ChunkLoadEvent e){ if(plugin.getConfig().getBoolean("bounty.show-on-all-mobs",true)) for(Entity x:e.getChunk().getEntities()) if(x instanceof LivingEntity m) ensure(m); }

 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 public void hit(EntityDamageByEntityEvent e){
  if(!(e.getEntity() instanceof LivingEntity m)||m instanceof Player)return;
  Player p=playerSource(e.getDamager()); if(p!=null) remember(m,p);
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 public void explosionDamage(EntityDamageEvent e){
  if(!(e.getEntity() instanceof LivingEntity m)||m instanceof Player)return;
  if(e.getCause()!=EntityDamageEvent.DamageCause.ENTITY_EXPLOSION&&e.getCause()!=EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)return;
  Player p=recent(m); if(p==null)p=nearest(m.getLocation(),radius());
  if(p!=null)remember(m,p);
 }
 @EventHandler(priority=EventPriority.MONITOR)
 public void entityExplosion(EntityExplodeEvent e){
  Player p=playerSource(e.getEntity()); if(p==null&&e.getEntity() instanceof TNTPrimed t)p=playerSource(t.getSource());
  markExplosion(e.getLocation(),p);
 }
 @EventHandler(priority=EventPriority.MONITOR)
 public void blockExplosion(BlockExplodeEvent e){ markExplosion(e.getBlock().getLocation(),null); }

 @EventHandler(priority=EventPriority.MONITOR)
 public void death(EntityDeathEvent e){
  LivingEntity m=e.getEntity(); if(!eligible(m))return;
  double amount=value(m); if(amount<=0)return;
  Player p=m.getKiller(); if(p==null)p=recent(m);
  if(p==null&&!plugin.getConfig().getBoolean("reward.require-player-attribution-for-explosions",true))p=nearest(m.getLocation(),radius());
  if(p==null)return;
  if(!economy.deposit(p,amount)){plugin.getLogger().warning("Failed to pay mob bounty to "+p.getName()+". Check economy/Vault hook.");return;}
  String msg=plugin.getConfig().getString("reward.actionbar","&a+%symbol%%amount% &7Mob bounty claimed!");
  msg=msg.replace("%symbol%",plugin.getConfig().getString("economy.symbol","$")).replace("%amount%",MoneyFormatter.format(plugin,amount));
  p.sendActionBar(Component.text(FlipMobs.color(msg))); hits.remove(m.getUniqueId());
 }

 private void ensure(LivingEntity m){
  if(!eligible(m))return;
  double v=value(m);
  if(v<=0){double min=Math.max(0,plugin.getConfig().getDouble("bounty.minimum",100000));double max=Math.max(min,plugin.getConfig().getDouble("bounty.maximum",30000000));v=Math.floor((min+Math.random()*(max-min))*100)/100; m.getPersistentDataContainer().set(key,PersistentDataType.DOUBLE,v);}
  label(m);
 }
 private void label(LivingEntity m){
  String f=plugin.getConfig().getString("bounty.name-format","&6$%amount%");
  m.setCustomName(FlipMobs.color(f.replace("%amount%",MoneyFormatter.format(plugin,value(m)))));m.setCustomNameVisible(true);
 }
 private double value(LivingEntity m){Double v=m.getPersistentDataContainer().get(key,PersistentDataType.DOUBLE);return v==null?0:v;}
 private boolean eligible(LivingEntity m){return !(m instanceof Player)&&!(m instanceof ArmorStand)&&!plugin.getConfig().getStringList("bounty.exclude").contains(m.getType().name());}
 private double radius(){return plugin.getConfig().getDouble("reward.explosion-attribution-radius",12);}
 private void remember(LivingEntity m,Player p){hits.put(m.getUniqueId(),new Hit(p.getUniqueId(),System.currentTimeMillis()+plugin.getConfig().getLong("reward.last-attacker-seconds",30)*1000));}
 private Player recent(LivingEntity m){Hit h=hits.get(m.getUniqueId());if(h==null)return null;if(h.until<System.currentTimeMillis()){hits.remove(m.getUniqueId());return null;}return Bukkit.getPlayer(h.player);}
 private void markExplosion(Location l,Player source){
  for(Entity x:l.getWorld().getNearbyEntities(l,radius(),radius(),radius()))if(x instanceof LivingEntity m&&eligible(m)){Player p=source;if(p==null)p=recent(m);if(p==null)p=nearest(l,radius());if(p!=null)remember(m,p);}
 }
 private Player nearest(Location l,double r){return l.getWorld().getNearbyPlayers(l,r).stream().min(Comparator.comparingDouble(p->p.getLocation().distanceSquared(l))).orElse(null);}
 private Player playerSource(Entity x){
  if(x instanceof Player p)return p;
  if(x instanceof Projectile q&&q.getShooter() instanceof Player p)return p;
  if(x instanceof TNTPrimed t&&t.getSource() instanceof Player p)return p;
  if(x instanceof Tameable t&&t.getOwner() instanceof Player p)return p;
  return null;
 }
 private record Hit(UUID player,long until){}
}