package think.rpgitems.listeners;

import gnu.trove.map.hash.TIntByteHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import think.rpgitems.RPGItems;
import think.rpgitems.data.Locale;
import think.rpgitems.data.RPGMetadata;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.support.WorldGuard;

public class EntityListener implements Listener {

    public static TIntByteHashMap removeArrows = new TIntByteHashMap();
    public static TIntIntHashMap rpgProjectiles = new TIntIntHashMap();
    public static HashMap<String, Set<Integer>> drops = new HashMap<String, Set<Integer>>();

    private Random random = new Random();

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        String type = event.getEntity().getType().toString();
        Random random = new Random();
        if (drops.containsKey(type)) {
            Set<Integer> items = drops.get(type);
            Iterator<Integer> it = items.iterator();
            while (it.hasNext()) {
                int id = it.next();
                RPGItem item = ItemManager.getItemById(id);
                if (item == null) {
                    it.remove();
                    continue;
                }
                double chance = item.dropChances.get(type);
                if (random.nextDouble() < chance / 100d) {
                    event.getDrops().add(item.toItemStack("en_GB"));
                }
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        final Entity entity = event.getEntity();
        if (removeArrows.contains(entity.getEntityId())) {
            entity.remove();
            removeArrows.remove(entity.getEntityId());
        }
        else if (rpgProjectiles.contains(entity.getEntityId())) {
            RPGItem item = ItemManager.getItemById(rpgProjectiles.get(entity.getEntityId()));
            new BukkitRunnable() {

                @Override
                public void run() {
                    rpgProjectiles.remove(entity.getEntityId());

                }
            }.runTask(RPGItems.p);
            if (item == null)
                return;
            item.projectileHit((Player) ((Projectile) entity).getShooter(), (Projectile) entity);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onProjectileFire(ProjectileLaunchEvent event) {
        LivingEntity shooter = event.getEntity().getShooter();
        if (shooter instanceof Player) {
            Player player = (Player) shooter;
            ItemStack item = player.getItemInHand();
            RPGItem rItem = ItemManager.toRPGItem(item);
            if (rItem == null)
                return;
            if (!WorldGuard.canPvP(player.getLocation()) && !rItem.ignoreWorldGuard)
                return;
            RPGMetadata meta = RPGItem.getMetadata(item);
            if (rItem.getMaxDurability() != -1) {
                int durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : rItem.getMaxDurability();
                durability--;
                if (durability <= 0) {
                    player.setItemInHand(null);
                }
                meta.put(RPGMetadata.DURABILITY, Integer.valueOf(durability));
            }
            RPGItem.updateItem(item, Locale.getPlayerLocale(player), meta);
            player.updateInventory();
            rpgProjectiles.put(event.getEntity().getEntityId(), rItem.getID());
        }
    }

    @SuppressWarnings("deprecation")
    private int playerDamager(EntityDamageByEntityEvent event, int damage) {
        Player player = (Player) event.getDamager();
        ItemStack item = player.getItemInHand();
        if (item.getType() == Material.BOW || item.getType() == Material.SNOW_BALL || item.getType() == Material.EGG || item.getType() == Material.POTION)
            return damage;

        RPGItem rItem = ItemManager.toRPGItem(item);
        if (rItem == null)
            return damage;
        if (!WorldGuard.canPvP(player.getLocation()) && !rItem.ignoreWorldGuard)
            return damage;
        damage = rItem.getDamageMin() != rItem.getDamageMax() ? (rItem.getDamageMin() + random.nextInt(rItem.getDamageMax() - rItem.getDamageMin())) : rItem.getDamageMin();
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity le = (LivingEntity) event.getEntity();
            rItem.hit(player, le);
        }
        RPGMetadata meta = RPGItem.getMetadata(item);
        if (rItem.getMaxDurability() != -1) {
            int durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : rItem.getMaxDurability();
            durability--;
            if (durability <= 0) {
                player.setItemInHand(null);
            }
            meta.put(RPGMetadata.DURABILITY, Integer.valueOf(durability));
        }
        RPGItem.updateItem(item, Locale.getPlayerLocale(player), meta);
        player.updateInventory();
        return damage;
    }

    private int projectileDamager(EntityDamageByEntityEvent event, int damage) {
        Projectile entity = (Projectile) event.getDamager();
        if (rpgProjectiles.contains(entity.getEntityId())) {
            RPGItem rItem = ItemManager.getItemById(rpgProjectiles.get(entity.getEntityId()));
            if (rItem == null)
                return damage;
            damage = rItem.getDamageMin() != rItem.getDamageMax() ? (rItem.getDamageMin() + random.nextInt(rItem.getDamageMax() - rItem.getDamageMin())) : rItem.getDamageMin();
            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) event.getEntity();
                rItem.hit((Player) entity.getShooter(), le);
            }
        }
        return damage;
    }

    private int playerHit(EntityDamageByEntityEvent event, int damage) {
        Player p = (Player) event.getEntity();
        if (event.isCancelled() || !WorldGuard.canPvP(p.getLocation())) {
            return damage;
        }
        String locale = Locale.getPlayerLocale(p);
        ItemStack[] armour = p.getInventory().getArmorContents();
        for (int i = 0; i < armour.length; i++) {
            ItemStack pArmour = armour[i];
            RPGItem pRItem = ItemManager.toRPGItem(pArmour);
            if (pRItem == null) {
                continue;
            }
            if (!WorldGuard.canPvP(p.getLocation()) && !pRItem.ignoreWorldGuard) {
                return damage;
            }
            if (pRItem.getArmour() > 0) {
                damage -= Math.round((damage) * ((pRItem.getArmour()) / 100d));
            }
            RPGMetadata meta = RPGItem.getMetadata(pArmour);
            if (pRItem.getMaxDurability() != -1) {
                int durability = meta.containsKey(RPGMetadata.DURABILITY) ? ((Number) meta.get(RPGMetadata.DURABILITY)).intValue() : pRItem.getMaxDurability();
                durability--;
                if (durability <= 0) {
                    armour[i] = null;
                }
                meta.put(RPGMetadata.DURABILITY, Integer.valueOf(durability));
            }
            RPGItem.updateItem(pArmour, locale, meta);
        }
        p.getInventory().setArmorContents(armour);
        p.updateInventory();
        return damage;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        int damage = (int) event.getDamage();
        if (event.getDamager() instanceof Player) {
            damage = playerDamager(event, damage);
        }
        else if (event.getDamager() instanceof Projectile) {
            damage = projectileDamager(event, damage);
        }
        if (event.getEntity() instanceof Player) {
            damage = playerHit(event, damage);
        }
        event.setDamage(damage);
    }
}
