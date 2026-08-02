package de.horizon.feature.dungeon;

import de.horizon.config.HorizonConfig;
import de.horizon.feature.skyblock.SkyblockPriceService;
import de.horizon.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Croesus / dungeon chest profit helper using live prices ({@link SkyblockPriceService}). Mirrors the
 * three Croesus screens:
 * <ul>
 *   <li><b>Run list</b> {@code (X/X) Croesus}: highlights each run head green (has unopened chests)
 *       or gold (opened), and hides fully-claimed runs.</li>
 *   <li><b>Chest preview</b> {@code Catacombs - Floor X}: highlights the two most profitable chests
 *       and draws a per-chest breakdown HUD.</li>
 *   <li><b>Single chest</b> {@code Gold Chest}: draws the chest value and a per-item breakdown.</li>
 * </ul>
 */
public final class ChestProfitService {
    private static final Pattern CROESUS_SCREEN = Pattern.compile("^(?:\\(\\d+/\\d+\\) )?Croesus$");
    private static final Pattern CHEST_PREVIEW = Pattern.compile("^(?:Master )?Catacombs - [A-Za-z0-9 ]*$");
    private static final Pattern CHEST_NAME = Pattern.compile("^(Wood|Iron|Gold|Diamond|Emerald|Obsidian|Bedrock)(?: Chest)?$");
    private static final Pattern CHEST_STATUS = Pattern.compile("^Opened Chest: .+$|^No more chests to open!$");
    private static final Pattern CHEST_OPENED = Pattern.compile("^Opened Chest: .+$");
    private static final Pattern ENCH_BOOK = Pattern.compile("^Enchanted Book \\(?([\\w ]+) (\\w+)\\)$");
    private static final Pattern ESSENCE = Pattern.compile("^(\\w+) Essence x(\\d+)$");
    private static final Pattern SHARD = Pattern.compile("^([A-Za-z ]+) Shard(?: x1)?$");
    private static final Pattern COST = Pattern.compile("^([\\d,]+) Coins$");

    private static final Set<String> ULTIMATE = Set.of(
        "Soul Eater", "Combo", "Legion", "One For All", "Rend",
        "Bank", "Swarm", "Last Stand", "Wisdom", "No Pain No Gain");

    private static final Map<String, String> ITEM_REPLACEMENTS = Map.ofEntries(
        Map.entry("Shiny Wither Chestplate", "WITHER_CHESTPLATE"),
        Map.entry("Shiny Wither Leggings", "WITHER_LEGGINGS"),
        Map.entry("Shiny Necron's Handle", "NECRON_HANDLE"),
        Map.entry("Necron's Handle", "NECRON_HANDLE"),
        Map.entry("Shiny Wither Helmet", "WITHER_HELMET"),
        Map.entry("Shiny Wither Boots", "WITHER_BOOTS"),
        Map.entry("Wither Shield", "WITHER_SHIELD_SCROLL"),
        Map.entry("Implosion", "IMPLOSION_SCROLL"),
        Map.entry("Shadow Warp", "SHADOW_WARP_SCROLL"),
        Map.entry("Necron Dye", "DYE_NECRON"),
        Map.entry("Livid Dye", "DYE_LIVID"),
        Map.entry("Giant's Sword", "GIANTS_SWORD"));

    private static final int GREEN = 0x7044FF44;
    private static final int GOLD = 0x70FFAA00;
    private static final int HIDE = 0xE0181818;
    private static final int BEST = 0x8000C000;
    private static final int SECOND = 0x80FFFF00;
    private static final int PANEL_BG = 0xE0101820;
    private static final int PANEL_BORDER = 0xFF4A5568;

    private final SkyblockPriceService prices;

    public ChestProfitService(SkyblockPriceService prices) {
        this.prices = prices;
    }

    private record Item(String name, long value) {}
    private record Chest(String name, List<Item> items, long profit, int slotIndex) {}

    public void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, HorizonConfig config) {
        if (!config.isCroesusProfitEnabled()) return;
        String title = strip(screen.getTitle().getString()).strip();

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) (Object) screen;
        int left = accessor.getLeftPos();
        int top = accessor.getTopPos();
        int imageWidth = accessor.getImageWidth();
        Font font = Minecraft.getInstance().font;

        if (CROESUS_SCREEN.matcher(title).matches()) {
            renderRunList(screen, ctx, left, top);              // no prices needed
        } else if (CHEST_PREVIEW.matcher(title).matches() && prices.isLoaded()) {
            renderChestPreview(screen, ctx, font, left, top);
        } else if (CHEST_NAME.matcher(title).matches() && prices.isLoaded()) {
            renderSingleChest(screen, ctx, font, left, top, imageWidth);
        }
    }

    // ── (X/X) Croesus — run list ─────────────────────────────────────────────────

    private void renderRunList(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, int left, int top) {
        for (Slot s : boardSlots(screen)) {
            ItemStack stack = s.getItem();
            if (stack.isEmpty()) continue;
            String name = strip(stack.getHoverName().getString());
            if (!name.equals("The Catacombs") && !name.equals("Master Mode The Catacombs")) continue;

            List<String> lore = loreLines(stack);
            // "No more chests to open!" run → hide completely (don't draw any tint), it's not a
            // real openable chest and highlighting it green (unopened) is confusing.
            if (lore.stream().anyMatch(l -> strip(l).contains("No more chests to open"))) {
                ctx.fill(left + s.x, top + s.y, left + s.x + 16, top + s.y + 16, HIDE);
                continue;
            }
            boolean hasStatus = lore.stream().anyMatch(l -> CHEST_STATUS.matcher(l).matches());
            // Fully claimed → hide (hide-claimed on, include-key off).
            if (hasStatus && !hasStrikethrough(stack, "Dungeon Chest Key")) {
                ctx.fill(left + s.x, top + s.y, left + s.x + 16, top + s.y + 16, HIDE);
                continue;
            }
            boolean opened = lore.stream().anyMatch(l -> CHEST_OPENED.matcher(l).matches());
            ctx.fill(left + s.x, top + s.y, left + s.x + 16, top + s.y + 16, opened ? GOLD : GREEN);
        }
    }

    // ── Catacombs - Floor X — chest preview ──────────────────────────────────────

    private void renderChestPreview(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, Font font,
                                    int left, int top) {
        List<Chest> chests = new ArrayList<>();
        for (Slot s : boardSlots(screen)) {
            if (s.index > 16) continue;
            ItemStack stack = s.getItem();
            if (stack.isEmpty() || stack.getItem() != Items.PLAYER_HEAD) continue;

            List<String> lore = loreLines(stack);
            int start = lore.indexOf("Contents") + 1;
            if (start == 0) continue;
            int end = start;
            while (end < lore.size() && !lore.get(end).isEmpty()) end++;

            long cost = 0L;
            if (end + 2 < lore.size()) {
                Matcher m = COST.matcher(lore.get(end + 2).strip());
                if (m.find()) cost = parseLong(m.group(1).replace(",", ""));
            }
            List<Item> items = new ArrayList<>();
            long total = 0L;
            for (int i = start; i < end; i++) {
                String line = lore.get(i).strip();
                long v = parseLoreItemValue(line);
                total += v;
                items.add(new Item(line, v));
            }
            chests.add(new Chest(strip(stack.getHoverName().getString()), items, total - cost, s.index));
        }
        if (chests.isEmpty()) return;
        chests.sort((a, b) -> Long.compare(b.profit, a.profit));

        // Highlight the two most profitable chests.
        List<Integer> topSlots = chests.stream().filter(c -> c.profit > 0).limit(2).map(Chest::slotIndex).toList();
        for (Slot s : boardSlots(screen)) {
            int rank = topSlots.indexOf(s.index);
            if (rank == 0) ctx.fill(left + s.x, top + s.y, left + s.x + 16, top + s.y + 16, BEST);
            else if (rank == 1) ctx.fill(left + s.x, top + s.y, left + s.x + 16, top + s.y + 16, SECOND);
        }
        drawBreakdownPanel(ctx, font, left, top, chests);
    }

    // ── Single reward chest ──────────────────────────────────────────────────────

    private void renderSingleChest(AbstractContainerScreen<?> screen, GuiGraphicsExtractor ctx, Font font,
                                   int left, int top, int imageWidth) {
        List<Item> items = new ArrayList<>();
        long cost = 0L;
        long total = 0L;
        for (Slot s : boardSlots(screen)) {
            if (s.index > 40) continue;
            ItemStack stack = s.getItem();
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.CHEST) {
                for (String l : loreLines(stack)) {
                    Matcher m = COST.matcher(l.strip());
                    if (m.find()) cost = parseLong(m.group(1).replace(",", ""));
                }
                continue;
            }
            long v = realItemValue(stack);
            if (v <= 0) continue;
            total += v;
            items.add(new Item(strip(stack.getHoverName().getString()), v));
        }
        if (items.isEmpty() && cost == 0L) return;
        items.sort((a, b) -> Long.compare(b.value, a.value));

        long profit = total - cost;
        String value = "§eProfit: " + (profit < 0 ? "§c" : "§a") + formatCoins(profit);
        ctx.text(font, value, left + imageWidth - font.width(strip(value)) - 6, top + 6, 0xFFFFFFFF, true);
        drawBreakdownPanel(ctx, font, left, top,
            List.of(new Chest("§eChest", items, profit, -1)));
    }

    // ── Value parsing ────────────────────────────────────────────────────────────

    /** Value of a preview-lore item line ("Enchanted Book (One For All V)", "Wither Essence x5", …). */
    private long parseLoreItemValue(String item) {
        Matcher eb = ENCH_BOOK.matcher(item);
        if (eb.find()) {
            String name = eb.group(1).trim();
            String ult = ULTIMATE.contains(name) ? "ULTIMATE_" : "";
            String key = "ENCHANTMENT_" + ult + name.toUpperCase(Locale.ROOT).replace(" ", "_")
                + "_" + romanToInt(eb.group(2));
            return prices.getPrice(key);
        }
        Matcher es = ESSENCE.matcher(item);
        if (es.find()) {
            return prices.getPrice("ESSENCE_" + es.group(1).toUpperCase(Locale.ROOT)) * parseLong(es.group(2));
        }
        Matcher sh = SHARD.matcher(item);
        if (sh.find()) {
            return prices.getPrice("SHARD_" + sh.group(1).trim().toUpperCase(Locale.ROOT).replace(" ", "_"));
        }
        String repl = ITEM_REPLACEMENTS.get(item);
        if (repl != null) return prices.getPrice(repl);
        String byName = prices.idFromName(item);
        if (byName != null) {
            long p = prices.getPrice(byName);
            if (p > 0) return p;
        }
        return prices.getPrice(item.toUpperCase(Locale.ROOT).replace("'", "").replace(" -", "").replace(" ", "_"));
    }

    /** Value of a real reward-chest ItemStack (enchanted book by NBT, essence/shard by name, else id). */
    private long realItemValue(ItemStack stack) {
        String name = strip(stack.getHoverName().getString());
        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            String key = enchantKey(stack);
            return key != null ? prices.getPrice(key) : 0L;
        }
        Matcher es = ESSENCE.matcher(name);
        if (es.find()) {
            return prices.getPrice("ESSENCE_" + es.group(1).toUpperCase(Locale.ROOT)) * parseLong(es.group(2));
        }
        Matcher sh = SHARD.matcher(name);
        if (sh.find()) {
            return prices.getPrice("SHARD_" + sh.group(1).trim().toUpperCase(Locale.ROOT).replace(" ", "_"));
        }
        String id = skyblockId(stack);
        if (id != null) {
            long p = prices.getPrice(id.replaceFirst("^STARRED_", ""));
            if (p > 0) return p * Math.max(1, stack.getCount());
        }
        return parseLoreItemValue(name);
    }

    private String enchantKey(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag ench = data.copyTag().getCompoundOrEmpty("ExtraAttributes").getCompoundOrEmpty("enchantments");
        for (String k : ench.keySet()) {
            int lvl = ench.getIntOr(k, 0);
            return "ENCHANTMENT_" + k.toUpperCase(Locale.ROOT) + "_" + lvl;
        }
        return null;
    }

    // ── Breakdown HUD panel (left of the container) ──────────────────────────────

    private void drawBreakdownPanel(GuiGraphicsExtractor ctx, Font font, int left, int top, List<Chest> chests) {
        int panelW = 150;
        int rows = 0;
        for (Chest c : chests) rows += 1 + Math.min(c.items.size(), 10);
        int panelH = 8 + rows * 10;
        int px = left - panelW - 6;
        if (px < 2) px = left;
        int py = top;

        ctx.fill(px, py, px + panelW, py + panelH, PANEL_BG);
        ctx.fill(px, py, px + panelW, py + 1, PANEL_BORDER);
        ctx.fill(px, py + panelH - 1, px + panelW, py + panelH, PANEL_BORDER);
        ctx.fill(px, py, px + 1, py + panelH, PANEL_BORDER);
        ctx.fill(px + panelW - 1, py, px + panelW, py + panelH, PANEL_BORDER);

        int y = py + 4;
        for (Chest c : chests) {
            String profitCol = c.profit >= 0 ? "§a" : "§c";
            ctx.text(font, c.name + " §8» §6Profit: " + profitCol + formatCoins(c.profit), px + 5, y, 0xFFFFFFFF, true);
            y += 10;
            int shown = Math.min(c.items.size(), 10);
            for (int i = 0; i < shown; i++) {
                Item it = c.items.get(i);
                String n = it.name.length() > 18 ? it.name.substring(0, 17) + "…" : it.name;
                ctx.text(font, "  §7" + n, px + 7, y, 0xFFFFFFFF, true);
                String v = "§a" + formatCoins(it.value);
                ctx.text(font, v, px + panelW - font.width(strip(v)) - 5, y, 0xFFFFFFFF, true);
                y += 10;
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private static boolean hasStrikethrough(ItemStack stack, String needle) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;
        for (Component line : lore.lines()) {
            if (strikeContains(line, needle)) return true;
        }
        return false;
    }

    private static boolean strikeContains(Component c, String needle) {
        if (c.getStyle().isStrikethrough() && c.getString().contains(needle)) return true;
        for (Component sib : c.getSiblings()) if (strikeContains(sib, needle)) return true;
        return false;
    }

    private static List<Slot> boardSlots(AbstractContainerScreen<?> screen) {
        List<Slot> out = new ArrayList<>();
        for (Slot s : screen.getMenu().slots) {
            if (!(s.container instanceof Inventory)) out.add(s);
        }
        return out;
    }

    private static List<String> loreLines(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return List.of();
        List<String> out = new ArrayList<>();
        for (Component c : lore.lines()) out.add(strip(c.getString()).strip());
        return out;
    }

    private static String skyblockId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag nbt = customData.copyTag();
        String id = nbt.getCompoundOrEmpty("ExtraAttributes").getStringOr("id", "");
        if (!id.isEmpty()) return id;
        id = nbt.getStringOr("id", "");
        return id.isEmpty() ? null : id;
    }

    private static int romanToInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) { }
        int result = 0, prev = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int v = switch (Character.toUpperCase(s.charAt(i))) {
                case 'I' -> 1; case 'V' -> 5; case 'X' -> 10;
                case 'L' -> 50; case 'C' -> 100; default -> 0;
            };
            result += v < prev ? -v : v;
            prev = v;
        }
        return result == 0 ? 1 : result;
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }

    private static String formatCoins(long v) {
        long a = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (a >= 1_000_000_000L) return sign + String.format(Locale.ROOT, "%.1fb", a / 1_000_000_000.0);
        if (a >= 1_000_000L) return sign + String.format(Locale.ROOT, "%.1fm", a / 1_000_000.0);
        if (a >= 1_000L) return sign + String.format(Locale.ROOT, "%.1fk", a / 1_000.0);
        return sign + a;
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("(?i)\\u00a7[0-9a-fk-or]", "");
    }
}
