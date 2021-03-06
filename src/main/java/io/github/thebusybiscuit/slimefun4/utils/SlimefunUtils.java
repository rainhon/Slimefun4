package io.github.thebusybiscuit.slimefun4.utils;

import io.github.thebusybiscuit.cscorelib2.item.ImmutableItemMeta;
import io.github.thebusybiscuit.cscorelib2.skull.SkullItem;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.exceptions.PrematureCodeException;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import me.mrCookieSlime.EmeraldEnchants.EmeraldEnchants;
import me.mrCookieSlime.EmeraldEnchants.ItemEnchantment;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public final class SlimefunUtils {

    private static final String EMERALDENCHANTS_LORE = ChatColor.YELLOW.toString() + ChatColor.YELLOW.toString() + ChatColor.GRAY.toString();
    private static final String SOULBOUND_LORE = ChatColor.GRAY + "灵魂绑定";
    private static final String NO_PICKUP_METADATA = "no_pickup";

    private static final NamespacedKey SOULBOUND_KEY = new NamespacedKey(SlimefunPlugin.instance, "soulbound");

    private SlimefunUtils() {
    }

    /**
     * This method quickly returns whether an {@link Item} was marked as "no_pickup" by
     * a Slimefun device.
     *
     * @param item The {@link Item} to query
     * @return Whether the {@link Item} is excluded from being picked up
     */
    public static boolean hasNoPickupFlag(Item item) {
        return item.hasMetadata(NO_PICKUP_METADATA);
    }

    /**
     * This will prevent the given {@link Item} from being picked up.
     * This is useful for display items which the {@link AncientPedestal} uses.
     *
     * @param item
     *            The {@link Item} to prevent from being picked up
     * @param context
     *            The context in which this {@link Item} was flagged
     */
    public static void markAsNoPickup(Item item, String context) {
        item.setMetadata(NO_PICKUP_METADATA, new FixedMetadataValue(SlimefunPlugin.instance, context));
    }

    /**
     * This method returns an {@link ItemStack} for the given texture.
     * The result will be a Player Head with this texture.
     *
     * @param texture The texture for this head (base64 or hash)
     * @return An {@link ItemStack} with this Head texture
     */
    public static ItemStack getCustomHead(String texture) {
        if (SlimefunPlugin.instance == null) {
            throw new PrematureCodeException("You cannot instantiate a custom head before Slimefun was loaded.");
        }

        if (SlimefunPlugin.getMinecraftVersion() == MinecraftVersion.UNIT_TEST) {
            // com.mojang.authlib.GameProfile does not exist in a Test Environment
            return new ItemStack(Material.PLAYER_HEAD);
        }

        String base64 = texture;

        if (PatternUtils.ALPHANUMERIC.matcher(texture).matches()) {
            base64 = Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + texture + "\"}}}").getBytes(StandardCharsets.UTF_8));
        }

        return SkullItem.fromBase64(base64);
    }

    public static boolean isSoulbound(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;

            if (meta != null && SlimefunPlugin.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_14)) {
                PersistentDataContainer container = meta.getPersistentDataContainer();

                if (container.has(SOULBOUND_KEY, PersistentDataType.BYTE)) {
                    return true;
                }
            }

            if (SlimefunPlugin.getThirdPartySupportService().isEmeraldEnchantsInstalled()) {
                // We wanna operate on a copy now
                item = item.clone();

                for (ItemEnchantment enchantment : EmeraldEnchants.getInstance().getRegistry().getEnchantments(item)) {
                    EmeraldEnchants.getInstance().getRegistry().applyEnchantment(item, enchantment.getEnchantment(), 0);
                }
            }

            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            if (sfItem instanceof Soulbound) {
                return !sfItem.isDisabled();
            } else if (meta != null) {
                return meta.hasLore() && meta.getLore().contains(SOULBOUND_LORE);
            }

        }
        return false;
    }

    /**
     * Toggles an {@link ItemStack} to be Soulbound.<br>
     * If true is passed, this will add the {@link #SOULBOUND_LORE} and
     * add a {@link NamespacedKey} to the item so it can be quickly identified
     * by {@link #isSoulbound(ItemStack)}.<br>
     * If false is passed, this property will be removed.
     *
     * @param item          The {@link ItemStack} you want to add/remove Soulbound from.
     * @param makeSoulbound If they item should be soulbound.
     * @see #isSoulbound(ItemStack)
     */
    public static void setSoulbound(ItemStack item, boolean makeSoulbound) {
        if (item == null || item.getType() == Material.AIR) {
            throw new IllegalArgumentException("A soulbound item cannot be null or air!");
        }

        boolean isSoulbound = isSoulbound(item);
        ItemMeta meta = item.getItemMeta();

        if (SlimefunPlugin.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_14)) {
            PersistentDataContainer container = meta.getPersistentDataContainer();

            if (makeSoulbound && !isSoulbound) {
                container.set(SOULBOUND_KEY, PersistentDataType.BYTE, (byte) 1);
            }

            if (!makeSoulbound && isSoulbound) {
                container.remove(SOULBOUND_KEY);
            }
        }

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        if (makeSoulbound && !isSoulbound) {
            lore.add(SOULBOUND_LORE);
        }

        if (!makeSoulbound && isSoulbound) {
            lore.remove(SOULBOUND_LORE);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * This method checks whether the given {@link ItemStack} is radioactive.
     *
     * @param item The {@link ItemStack} to check
     * @return Whether this {@link ItemStack} is radioactive or not
     */
    public static boolean isRadioactive(ItemStack item) {
        return SlimefunItem.getByItem(item) instanceof Radioactive;
    }

    public static boolean containsSimilarItem(Inventory inventory, ItemStack item, boolean checkLore) {
        if (inventory == null || item == null) {
            return false;
        }

        // Performance optimization
        ItemStackWrapper wrapper = new ItemStackWrapper(item);

        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            if (isItemSimilar(stack, wrapper, checkLore)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isItemSimilar(ItemStack item, ItemStack sfitem, boolean checkLore) {
        return isItemSimilar(item, sfitem, checkLore, true);
    }

    public static boolean isItemSimilar(ItemStack item, ItemStack sfitem, boolean checkLore, boolean checkAmount) {
        if (item == null) return sfitem == null;
        if (sfitem == null) return false;
        if (item.getType() != sfitem.getType()) return false;
        if (checkAmount && item.getAmount() < sfitem.getAmount()) return false;

        if (sfitem instanceof SlimefunItemStack && item instanceof SlimefunItemStack) {
            return ((SlimefunItemStack) item).getItemId().equals(((SlimefunItemStack) sfitem).getItemId());
        }

        boolean sfItemHasMeta = sfitem.hasItemMeta();
        if (item.hasItemMeta()) {
            ItemMeta itemMeta = item.getItemMeta();
            if (sfitem instanceof SlimefunItemStack) {
                Optional<String> id = SlimefunPlugin.getItemDataService().getItemData(itemMeta);
                if (id.isPresent()) {
                    return id.get().equals(((SlimefunItemStack) sfitem).getItemId());
                }

                ImmutableItemMeta meta = ((SlimefunItemStack) sfitem).getImmutableMeta();
                return equalsItemMeta(itemMeta, meta, checkLore);
            }

            if (sfItemHasMeta) {
                return equalsItemMeta(itemMeta, sfitem.getItemMeta(), checkLore);
            }
        } else {
            return !sfItemHasMeta;
        }

        return false;
    }

    private static boolean equalsItemMeta(ItemMeta itemMeta, ImmutableItemMeta meta, boolean checkLore) {
        Optional<String> displayName = meta.getDisplayName();

        if (itemMeta.hasDisplayName() && displayName.isPresent()) {
            if (itemMeta.getDisplayName().equals(displayName.get())) {
                Optional<List<String>> itemLore = meta.getLore();

                if (checkLore) {
                    if (itemMeta.hasLore() && itemLore.isPresent()) {
                        return equalsLore(itemMeta.getLore(), itemLore.get());
                    } else return !itemMeta.hasLore() && !itemLore.isPresent();
                } else return true;
            } else return false;
        } else if (!itemMeta.hasDisplayName() && !displayName.isPresent()) {
            Optional<List<String>> itemLore = meta.getLore();

            if (checkLore) {
                if (itemMeta.hasLore() && itemLore.isPresent()) {
                    return equalsLore(itemMeta.getLore(), itemLore.get());
                } else return !itemMeta.hasLore() && !itemLore.isPresent();
            } else return true;
        } else return false;
    }

    private static boolean equalsItemMeta(ItemMeta itemMeta, ItemMeta sfitemMeta, boolean checkLore) {
        if (itemMeta.hasDisplayName() && sfitemMeta.hasDisplayName()) {
            if (itemMeta.getDisplayName().equals(sfitemMeta.getDisplayName())) {
                if (checkLore) {
                    if (itemMeta.hasLore() && sfitemMeta.hasLore()) {
                        return equalsLore(itemMeta.getLore(), sfitemMeta.getLore());
                    } else return !itemMeta.hasLore() && !sfitemMeta.hasLore();
                } else return true;
            } else return false;
        } else if (!itemMeta.hasDisplayName() && !sfitemMeta.hasDisplayName()) {
            if (checkLore) {
                if (itemMeta.hasLore() && sfitemMeta.hasLore()) {
                    return equalsLore(itemMeta.getLore(), sfitemMeta.getLore());
                } else return !itemMeta.hasLore() && !sfitemMeta.hasLore();
            } else return true;
        } else return false;
    }

    private static boolean equalsLore(List<String> lore, List<String> lore2) {
        StringBuilder string1 = new StringBuilder();
        StringBuilder string2 = new StringBuilder();

        for (String string : lore) {
            if (!string.equals(SOULBOUND_LORE) && !string.startsWith(EMERALDENCHANTS_LORE)) {
                string1.append("-NEW LINE-").append(string);
            }
        }

        for (String string : lore2) {
            if (!string.equals(SOULBOUND_LORE) && !string.startsWith(EMERALDENCHANTS_LORE)) {
                string2.append("-NEW LINE-").append(string);
            }
        }

        return string1.toString().equals(string2.toString());
    }

}
