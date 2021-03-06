package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import io.github.thebusybiscuit.slimefun4.core.handlers.EntityKillHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.BasicCircuitBoard;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public class MobDropListener implements Listener {

    private final BasicCircuitBoard circuitBoard;

    public MobDropListener(SlimefunPlugin plugin, BasicCircuitBoard circuitBoard) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        this.circuitBoard = circuitBoard;
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() != null) {
            Player p = e.getEntity().getKiller();
            ItemStack item = p.getInventory().getItemInMainHand();

            Set<ItemStack> customDrops = SlimefunPlugin.getRegistry().getMobDrops(e.getEntityType());
            if (customDrops != null && !customDrops.isEmpty()) {
                addDrops(p, customDrops, e.getDrops());
            }

            if (item.getType() != Material.AIR) {
                SlimefunItem sfItem = SlimefunItem.getByItem(item);

                if (sfItem != null && Slimefun.hasUnlocked(p, sfItem, true)) {
                    sfItem.callItemHandler(EntityKillHandler.class, handler -> handler.onKill(e, e.getEntity(), p, item));
                }
            }
        }
    }

    private void addDrops(Player p, Set<ItemStack> customDrops, List<ItemStack> drops) {
        for (ItemStack drop : customDrops) {
            if (Slimefun.hasUnlocked(p, drop, true)) {
                if (circuitBoard != null && circuitBoard.isItem(drop) && !circuitBoard.isDroppedFromGolems()) {
                    continue;
                }

                drops.add(drop.clone());
            }
        }
    }
}