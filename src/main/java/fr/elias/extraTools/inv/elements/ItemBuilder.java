package fr.elias.extraTools.inv.elements;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
// removed Guava Lists
import fr.minuskube.inv.ClickableItem;
import fr.elias.extraTools.utils.config.Msgs;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public record ItemBuilder(ItemStack stack) {

    public static ItemBuilder skull() {
        return start(Material.PLAYER_HEAD);
    }

    public static ItemBuilder filler(DyeColor color) {
        String matName = color + "_STAINED_GLASS_PANE";
        Material material = Material.matchMaterial(matName);
        if (material == null) material = Material.GRAY_STAINED_GLASS_PANE;
        return start(material).name("");
    }

    public static ItemBuilder start(Material material) {
        return start(new ItemStack(material));
    }

    public static ItemBuilder start(ItemStack stack) {
        return new ItemBuilder(stack.clone());
    }

    public ItemBuilder amount(int amount) {
        stack.setAmount(amount);
        return this;
    }

    public ItemBuilder name(String name) {
        if (name == null) return name((Component) null);
        return name(Msgs.of(name).asComp());
    }

    public ItemBuilder name(Component name) {
        return meta(meta -> meta.setDisplayName(Msgs.of(name).build()));
    }

    public ItemBuilder value(String value) {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        if (value != null && !value.isEmpty()) {
            profile.setProperty(new ProfileProperty("textures", value));
        }
        return profile(profile);
    }

    public ItemBuilder profile(PlayerProfile profile) {
        return meta(SkullMeta.class, meta -> meta.setPlayerProfile(profile));
    }

    public ItemBuilder lore(String... lore) {
        return oldLore(Arrays.asList(lore));
    }

    public ItemBuilder oldLore(List<String> add) {
        return lore(add.stream().map(s -> Msgs.of(s).asComp()).collect(Collectors.toList()));
    }

    public ItemBuilder lore(Component... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder lore(List<Component> add) {
        // if no lore yet, start with an empty ArrayList (no Guava needed)
        List<String> lore = query(ItemMeta::hasLore) ? query(ItemMeta::getLore) : new ArrayList<>();
        List<String> extraLore = add.stream().map(Msgs::of).map(Msgs::build).collect(Collectors.toList());
        lore.addAll(extraLore);
        meta(meta -> meta.setLore(lore));
        return this;
    }

    public <T> ItemBuilder pdc(NamespacedKey key, PersistentDataType<T, T> type, T content) {
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key, type, content);
        stack.setItemMeta(meta);
        return this;
    }

    public <R> R query(Function<ItemMeta, R> function) {
        return query(ItemMeta.class, function);
    }

    public <T extends ItemMeta, R> R query(Class<T> metaType, Function<T, R> function) {
        T meta = metaType.cast(stack.getItemMeta());
        return function.apply(meta);
    }

    public <T extends ItemMeta> ItemBuilder meta(Class<T> metaType, Consumer<T> consumer) {
        T meta = metaType.cast(stack.getItemMeta());
        consumer.accept(meta);
        stack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder meta(Consumer<ItemMeta> consumer) {
        return meta(ItemMeta.class, consumer);
    }

    public ItemStack build() {
        return stack;
    }

    public ClickableItem empty() {
        return ClickableItem.empty(build());
    }

    public ClickableItem of(Consumer<InventoryClickEvent> consumer) {
        return ClickableItem.of(build(), consumer);
    }
}
