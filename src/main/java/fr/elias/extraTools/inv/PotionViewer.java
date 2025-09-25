package fr.elias.extraTools.inv;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import fr.elias.extraTools.utils.config.Msgs;
import fr.elias.extraTools.customs.CustomItem;
import fr.elias.extraTools.inv.elements.InvIcon;
import fr.elias.extraTools.inv.elements.ItemBuilder;
import fr.elias.extraTools.utils.text.Prompts;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public class PotionViewer implements InventoryProvider {

    public static SmartInventory INVENTORY(CustomItem item) {
        return SmartInventory.builder()
                .id("potionViewer")
                .title("Manage potion effects")
                .size(6, 9)
                .provider(new PotionViewer(item))
                .build();
    }

    private final CustomItem item;

    private PotionViewer(CustomItem item) {
        this.item = item;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        Set<PotionEffect> effects = item.effects();

        Pagination pagination = contents.pagination();
        ClickableItem[] items = new ClickableItem[effects.size()];

        int i = 0;
        for (PotionEffect effect : effects) {
            items[i++] = ItemBuilder.start(Material.POTION)
                    .name("<gray>Potion: " + effect.getType().getName())
                    .lore(
                            "<gray>Amplifier: " + (effect.getAmplifier() + 1),
                            effect.getDuration() == Integer.MAX_VALUE
                                    ? "<gray>Duration: Unlimited"
                                    : "<gray>Duration: " + effect.getDuration() + " ticks",
                            "<red>Right click to delete"
                    )
                    .meta(PotionMeta.class, pm -> pm.addCustomEffect(effect, true))
                    .of(e -> {
                        if (e.isRightClick()) {
                            item.removeEffect(effect.getType());
                            INVENTORY(item).open(player);
                        }
                    });
        }

        pagination.setItems(items);
        pagination.setItemsPerPage(45);
        pagination.addToIterator(contents.newIterator(SlotIterator.Type.HORIZONTAL, 0, 0));

        contents.set(5, 3, InvIcon.ADD.named("<gray>Click to add a potion effect")
                .of(e -> {
                    player.closeInventory();
                    buildPotion(player);
                }));

        contents.set(5, 5, InvIcon.CLOSE
                .of(e -> ManageItemInventory.INVENTORY(item).open(player)));

        if (!pagination.isFirst()) {
            contents.set(5, 1, InvIcon.PREV.of(e ->
                    INVENTORY(item).open(player, pagination.previous().getPage())));
        }

        if (!pagination.isLast()) {
            contents.set(5, 7, InvIcon.NEXT.of(e ->
                    INVENTORY(item).open(player, pagination.next().getPage())));
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {
        // no periodic updates needed
    }

    private void buildPotion(Player player) {
        // Make sure your prompt returns a PotionEffectType.
        // Rename your prompt to EFFECT (or adjust here to whatever you have).
        Prompts.EFFECT.start(player, "<blue>Write the potion effect (e.g., SPEED)", (PotionEffectType type) -> {
            if (type == null) {
                Msgs.of("<red>Invalid input, creation cancelled").send(player);
                INVENTORY(item).open(player);
                return;
            }

            Prompts.NUM.start(player, "<blue>Write the amplifier (1+)", amplifier -> {
                if (amplifier == null) {
                    Msgs.of("<red>Invalid input, creation cancelled").send(player);
                    INVENTORY(item).open(player);
                    return;
                }

                Prompts.NUM.start(player, "<blue>Write the duration in ticks (-1 for unlimited)", duration -> {
                    if (duration == null) {
                        Msgs.of("<red>Invalid input, creation cancelled").send(player);
                        INVENTORY(item).open(player);
                        return;
                    }

                    int ampl = amplifier.intValue();
                    if (ampl < 1 || ampl > 255) ampl = 1; // user enters 1.. -> we convert to 0-based
                    ampl -= 1;

                    int dur = duration.intValue();
                    if (dur == -1) {
                        dur = Integer.MAX_VALUE; // Unlimited duration
                    }

                    item.addEffect(new PotionEffect(type, dur, ampl));
                    Msgs.of("<green>Potion effect added!").send(player);
                    INVENTORY(item).open(player);
                });
            });
        });
    }
}
