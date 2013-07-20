package think.rpgitems.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import think.rpgitems.data.Locale;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.support.WorldGuard;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerAction(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || (e.getAction() == Action.RIGHT_CLICK_BLOCK) && !e.isCancelled())) {
            ItemStack item = player.getItemInHand();
            if (item.getType() == Material.BOW || item.getType() == Material.SNOW_BALL || item.getType() == Material.EGG || item.getType() == Material.POTION)
                return;

            RPGItem rItem = ItemManager.toRPGItem(item);
            if (rItem == null)
                return;
            if (!WorldGuard.canPvP(player.getLocation()) && !rItem.ignoreWorldGuard)
                return;
            rItem.rightClick(player);
            if (player.getItemInHand().getTypeId() != 0)
                RPGItem.updateItem(item, Locale.getPlayerLocale(player));
            else
                player.setItemInHand(null);
            player.updateInventory();
        }
        else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            ItemStack item = player.getItemInHand();
            if (item.getType() == Material.BOW || item.getType() == Material.SNOW_BALL || item.getType() == Material.EGG || item.getType() == Material.POTION)
                return;

            RPGItem rItem = ItemManager.toRPGItem(item);
            if (rItem == null)
                return;
            if (!WorldGuard.canPvP(player.getLocation()) && !rItem.ignoreWorldGuard)
                return;
            rItem.leftClick(player);
            RPGItem.updateItem(item, Locale.getPlayerLocale(player));
        }

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        PlayerInventory in = player.getInventory();
        String locale = Locale.getPlayerLocale(player);
        for (int i = 0; i < in.getSize(); i++) {
            ItemStack item = in.getItem(i);
            if (ItemManager.toRPGItem(item) != null)
                RPGItem.updateItem(item, locale);
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (ItemManager.toRPGItem(item) != null)
                RPGItem.updateItem(item, locale);
        }
    }

    @EventHandler
    public void onPlayerPickup(PlayerPickupItemEvent e) {
        ItemStack item = e.getItem().getItemStack();
        String locale = Locale.getPlayerLocale(e.getPlayer());
        if (ItemManager.toRPGItem(item) != null) {
            RPGItem.updateItem(item, locale);
            e.getItem().setItemStack(item);
        }
    }
}
