package think.rpgitems.config;

import java.util.Iterator;

import org.bukkit.configuration.ConfigurationSection;

import think.rpgitems.RPGItems;
import think.rpgitems.item.ItemManager;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.Power;
import think.rpgitems.power.PowerUnbreakable;
import think.rpgitems.power.PowerUnbreaking;

public class Update03To04 implements Updater {

    @Override
    public void update(ConfigurationSection section) {
        RPGItems rPGItems = RPGItems.p;

        ItemManager.load(rPGItems);

        for (RPGItem item : ItemManager.itemByName.values()) {
            Iterator<Power> it = item.powers.iterator();
            while (it.hasNext()) {
                Power power = it.next();
                if (power instanceof PowerUnbreakable) {
                    item.setMaxDurability(-1, false);
                    it.remove();
                }
                if (power instanceof PowerUnbreaking) {
                    PowerUnbreaking ub = (PowerUnbreaking) power;
                    item.setMaxDurability((int) (item.getMaxDurability() * (1d + (ub.level) / 2d)));
                    it.remove();
                }
            }
        }

        ItemManager.save(rPGItems);
        ItemManager.itemByName.clear();
        ItemManager.itemById.clear();
        section.set("version", "0.4");
    }

}
