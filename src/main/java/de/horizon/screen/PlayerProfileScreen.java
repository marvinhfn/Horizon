package de.horizon.screen;

import de.horizon.api.profile.HorizonAccessory;
import de.horizon.api.profile.HorizonInventoryItem;
import de.horizon.api.profile.HorizonInventorySlot;
import de.horizon.api.profile.HorizonPet;
import de.horizon.api.profile.HorizonProfileData;
import de.horizon.api.profile.HorizonProfileGateway;
import de.horizon.api.profile.HorizonSkill;
import de.horizon.api.profile.HorizonSlayerBoss;
import de.horizon.api.profile.HorizonStoragePage;
import de.horizon.hud.HudStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class PlayerProfileScreen extends Screen {
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAFBAC7;
    private static final int WARNING = 0xFFF9C978;
    private static final int WINDOW = 0x7A10161D;
    private static final int WINDOW_HEADER = 0xC8161E28;
    private static final int CARD = 0xBE18212C;
    private static final int CARD_ALT = 0xBE111920;
    private static final int SLOT = 0xD1243443;
    private static final int SLOT_EMPTY = 0xAA17222D;
    private static final int BUTTON = 0xCC20303E;
    private static final int BUTTON_TEXT = 0xFFF6F7F8;

    private final Screen parent;
    private final String requestedPlayer;
    private final HorizonProfileGateway profileGateway;

    private CompletableFuture<Void> loadFuture;
    private HorizonProfileData profile;
    private String error;
    private ViewerTab activeTab = ViewerTab.OVERVIEW;
    private int contentScrollOffset;
    private int selectedStoragePageIndex;

    public PlayerProfileScreen(Screen parent, String requestedPlayer, HorizonProfileGateway profileGateway) {
        super(Text.literal("Horizon Viewer"));
        this.parent = parent;
        this.requestedPlayer = requestedPlayer;
        this.profileGateway = profileGateway;
    }

    @Override
    protected void init() {
        super.init();
        if (loadFuture != null) {
            return;
        }
        loadFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return profileGateway.loadProfile(requestedPlayer);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).handle((loaded, throwable) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    if (throwable != null) {
                        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
                        error = cause.getMessage() == null ? "Profil konnte nicht geladen werden." : cause.getMessage();
                    } else {
                        profile = loaded;
                        selectedStoragePageIndex = 0;
                    }
                });
            }
            return null;
        });
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Rect frame = frame();
        if (!frame.contains(click.x(), click.y())) {
            return super.mouseClicked(click, doubled);
        }
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }
        if (closeRect(frame).contains(click.x(), click.y())) {
            close();
            return true;
        }

        Rect sidebar = sidebarRect(frame);
        for (int index = 0; index < ViewerTab.values().length; index++) {
            if (sidebarTabRect(sidebar, index).contains(click.x(), click.y())) {
                activeTab = ViewerTab.values()[index];
                contentScrollOffset = 0;
                return true;
            }
        }

        if (profile != null && activeTab == ViewerTab.INVENTORIES) {
            Rect viewport = contentViewportRect(frame);
            int chipY = viewport.y - contentScrollOffset + 176;
            for (int index = 0; index < profile.storages().size(); index++) {
                if (inventoryChipRect(viewport.x, chipY, index).contains(click.x(), click.y())) {
                    selectedStoragePageIndex = index;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Rect viewport = contentViewportRect(frame());
        if (!viewport.contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int maxScroll = Math.max(0, contentHeight() - viewport.height);
        contentScrollOffset = Math.max(0, Math.min(maxScroll, contentScrollOffset - (int) Math.round(verticalAmount * 28.0D)));
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Rect frame = frame();
        Rect sidebar = sidebarRect(frame);
        Rect viewport = contentViewportRect(frame);
        Rect clip = contentClipRect(frame);

        drawWindowChrome(context, frame);
        drawSidebar(context, sidebar);

        context.enableScissor(clip.x, clip.y, clip.right(), clip.bottom());
        if (profile == null && error == null) {
            drawLoading(context, viewport);
        } else if (error != null) {
            drawError(context, viewport);
        } else {
            switch (activeTab) {
                case OVERVIEW -> drawOverview(context, viewport);
                case INVENTORIES -> drawInventories(context, viewport, mouseX, mouseY);
                case SKILLS -> drawSkills(context, viewport);
                case SLAYERS -> drawSlayers(context, viewport);
                case PETS -> drawPets(context, viewport);
                case ACCESSORIES -> drawAccessories(context, viewport);
            }
        }
        context.disableScissor();
        drawScrollBar(context, viewport);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSidebar(DrawContext context, Rect sidebar) {
        for (int index = 0; index < ViewerTab.values().length; index++) {
            ViewerTab tab = ViewerTab.values()[index];
            Rect rect = sidebarTabRect(sidebar, index);
            context.fill(rect.x, rect.y, rect.right(), rect.bottom(), tab == activeTab ? CARD : CARD_ALT);
            drawText(context, rect.x + 8, rect.y + 7, tab.label, tab == activeTab ? HudStyle.accent() : TEXT);
        }
    }

    private void drawLoading(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Profile Viewer", "Lade SkyBlock-Daten fuer " + displayRequestedPlayer() + ".");
        y = drawInfoCard(context, viewport.x, y, "Backend", "Der Viewer wartet auf Horizon-Backend, Auth-Token und Profildaten.");
        drawInfoCard(context, viewport.x, y, "Hinweis", "Aktiviere das Horizon-Backend im Client und starte den lokalen Backend-Service.");
    }

    private void drawError(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Profile Viewer", "Der Abruf konnte nicht abgeschlossen werden.");
        y = drawInfoCard(context, viewport.x, y, "Fehler", error);
        drawInfoCard(context, viewport.x, y, "Hinweis", "Wenn der Hypixel-Key ungueltig ist oder das Backend nicht laeuft, bleiben Inventories leer.");
    }

    private void drawOverview(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "SkyBlock Profile", "Uebersicht aus Backend, Hypixel-Profil und SkyCrypt-aehnlichen Summary-Daten.");
        y = drawStatsStrip(context, viewport.x, y);
        y = drawProfileSummary(context, viewport.x, y);
        y = drawMetadataCard(context, viewport.x, y);
        drawProfileListCard(context, viewport.x, y);
    }

    private void drawInventories(DrawContext context, Rect viewport, int mouseX, int mouseY) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Inventories", "Container-Browser fuer Inventory, Ender Chest, Wardrobe, Backpack, Pets und Accessory-Bag.");
        y = drawInventorySummary(context, viewport.x, y);
        y = drawInventorySelector(context, viewport.x, y);
        drawInventoryPage(context, viewport, y, mouseX, mouseY);
    }

    private void drawSkills(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Skills", "Skill-Level, Progress und Experience je Skill.");
        List<HorizonSkill> skills = sortedSkills();
        y = drawInfoCard(context, viewport.x, y, "Summary", "Durchschnitt: " + formatDecimal(averageSkillLevel()) + " | Skills: " + skills.size());
        int index = 0;
        for (HorizonSkill skill : skills) {
            Rect card = statGridRect(viewport.x, y, index++);
            context.fill(card.x, card.y, card.right(), card.bottom(), index % 2 == 0 ? CARD : CARD_ALT);
            drawText(context, card.x + 10, card.y + 8, skill.displayName(), TEXT);
            drawText(context, card.x + 10, card.y + 24, "Level " + skill.level(), HudStyle.accent());
            drawProgressBar(context, card.x + 10, card.y + 42, card.width - 20, 8, (float) skill.progress(), HudStyle.selected(), "Progress");
            drawText(context, card.x + 10, card.y + 56, formatNumber((long) skill.experience()) + " XP", MUTED);
        }
    }

    private void drawSlayers(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Slayers", "Level, XP und Kills pro Boss.");
        y = drawInfoCard(context, viewport.x, y, "Total Slayer XP", formatNumber(totalSlayerXp()));
        int row = 0;
        for (HorizonSlayerBoss slayer : sortedSlayers()) {
            Rect card = new Rect(viewport.x - 12, y + row * 58, 622, 50);
            context.fill(card.x, card.y, card.right(), card.bottom(), row % 2 == 0 ? CARD : CARD_ALT);
            drawText(context, card.x + 12, card.y + 10, slayer.displayName(), TEXT);
            drawText(context, card.x + 220, card.y + 10, "Level " + slayer.level(), HudStyle.accent());
            drawText(context, card.x + 350, card.y + 10, formatNumber(slayer.experience()) + " XP", TEXT);
            drawText(context, card.x + 510, card.y + 10, formatNumber(slayer.kills()) + " Kills", MUTED);
            row++;
        }
    }

    private void drawPets(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Pets", "Aktive und gespeicherte Pets aus dem Profil.");
        y = drawInfoCard(context, viewport.x, y, "Pet Summary", "Gesamt: " + profile.pets().size() + " | Aktiv: " + activePetName());
        int index = 0;
        for (HorizonPet pet : sortedPets()) {
            Rect card = statGridRect(viewport.x, y, index++);
            context.fill(card.x, card.y, card.right(), card.bottom(), pet.active() ? CARD : CARD_ALT);
            drawText(context, card.x + 10, card.y + 8, pet.displayName(), TEXT);
            drawText(context, card.x + 10, card.y + 24, pet.tier().isBlank() ? "Tier unbekannt" : pet.tier(), rarityColor(pet.tier()));
            drawText(context, card.x + 10, card.y + 38, pet.level() > 0 ? "Level " + pet.level() : "Level --", TEXT);
            drawText(context, card.x + 10, card.y + 52, pet.heldItem().isBlank() ? "Kein Pet Item" : humanize(pet.heldItem()), MUTED);
        }
    }

    private void drawAccessories(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "Accessories", "Accessoires aus der Accessory-Bag, inklusive Rarity und Enrichment-Hinweisen.");
        y = drawInfoCard(context, viewport.x, y, "Accessory Summary", "Gefunden: " + profile.accessories().size());
        int index = 0;
        for (HorizonAccessory accessory : sortedAccessories()) {
            Rect card = statGridRect(viewport.x, y, index++);
            context.fill(card.x, card.y, card.right(), card.bottom(), index % 2 == 0 ? CARD : CARD_ALT);
            drawText(context, card.x + 10, card.y + 8, accessory.displayName(), TEXT);
            drawText(context, card.x + 10, card.y + 24, accessory.rarity().isBlank() ? "Rarity unbekannt" : accessory.rarity(), rarityColor(accessory.rarity()));
            drawWrappedText(context, card.x + 10, card.y + 38, accessory.enrichment().isBlank() ? "Kein Enrichment gelesen." : accessory.enrichment(), card.width - 20, MUTED);
        }
    }

    private int drawHeroCard(DrawContext context, int x, int y, String title, String subtitle) {
        int height = 152;
        drawSettingCard(context, x, y, height, HudStyle.accent());
        Rect avatar = new Rect(x + 8, y + 10, 88, 88);
        context.fill(avatar.x, avatar.y, avatar.right(), avatar.bottom(), CARD_ALT);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(initials()), avatar.centerX(), avatar.y + 34, TEXT);
        drawText(context, x + 110, y + 12, title, TEXT);
        drawWrappedText(context, x + 110, y + 28, subtitle, 480, MUTED);
        drawText(context, x + 110, y + 66, profile == null ? displayRequestedPlayer() : profile.playerName(), HudStyle.accent());
        drawText(context, x + 110, y + 82, "Profil " + profileName(), TEXT);
        drawText(context, x + 110, y + 98, "UUID " + compactUuid(), MUTED);
        drawText(context, x + 110, y + 114, gameModeText(), MUTED);
        drawProgressBar(context, x + 110, y + 130, 220, 8, progress(profile == null ? 0 : profile.skyblockLevel(), 500), HudStyle.accent(), "SkyBlock");
        drawProgressBar(context, x + 352, y + 130, 220, 8, progress(profile == null ? 0 : profile.catacombsLevel(), 50), HudStyle.selected(), "Catacombs");
        return y + height;
    }

    private int drawStatsStrip(DrawContext context, int x, int y) {
        int height = 98;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawMetric(context, new Rect(x - 4, y + 28, 145, 54), "SkyBlock", String.valueOf(profile.skyblockLevel()));
        drawMetric(context, new Rect(x + 145, y + 28, 145, 54), "Catacombs", String.valueOf(profile.catacombsLevel()));
        drawMetric(context, new Rect(x + 294, y + 28, 145, 54), "Purse", formatCoins(profile.purse()));
        drawMetric(context, new Rect(x + 443, y + 28, 145, 54), "Networth", formatCoins(profile.networth()));
        return y + height;
    }

    private int drawProfileSummary(DrawContext context, int x, int y) {
        int height = 104;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, "Profile Summary", TEXT);
        drawKeyValue(context, x, y + 34, "Storage Pages", String.valueOf(profile.storages().size()));
        drawKeyValue(context, x, y + 54, "Accessories", String.valueOf(profile.accessories().size()));
        drawKeyValue(context, x + 280, y + 34, "Pets", String.valueOf(profile.pets().size()));
        drawKeyValue(context, x + 280, y + 54, "Slayer XP", formatNumber(totalSlayerXp()));
        drawKeyValue(context, x, y + 74, "Avg Skill", formatDecimal(averageSkillLevel()));
        drawKeyValue(context, x + 280, y + 74, "Bank", formatCoins(profile.bank()));
        return y + height;
    }

    private int drawMetadataCard(DrawContext context, int x, int y) {
        int height = 34 + Math.max(1, profile.metadata().size()) * 20;
        drawSettingCard(context, x, y, height, HudStyle.border());
        drawText(context, x, y + 10, "Metadata", TEXT);
        int rowY = y + 34;
        for (Map.Entry<String, String> entry : profile.metadata().entrySet()) {
            drawText(context, x + 10, rowY, humanize(entry.getKey()), MUTED);
            drawText(context, x + 170, rowY, entry.getValue(), TEXT);
            rowY += 20;
        }
        return y + height;
    }

    private void drawProfileListCard(DrawContext context, int x, int y) {
        int height = 34 + Math.max(1, profile.profileNames().size()) * 22;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, "Profiles", TEXT);
        int rowY = y + 34;
        for (String name : profile.profileNames()) {
            drawText(context, x + 12, rowY, name, name.startsWith(profile.profileName()) ? HudStyle.accent() : TEXT);
            rowY += 22;
        }
    }

    private int drawInventorySummary(DrawContext context, int x, int y) {
        int height = 94;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        HorizonStoragePage selected = selectedStoragePage();
        drawMetric(context, new Rect(x - 4, y + 26, 192, 50), "Pages", String.valueOf(profile.storages().size()));
        drawMetric(context, new Rect(x + 194, y + 26, 192, 50), "Selected", selected == null ? "--" : selected.title());
        drawMetric(context, new Rect(x + 392, y + 26, 196, 50), "Items", selected == null ? "0" : String.valueOf(selected.slots().size()));
        return y + height;
    }

    private int drawInventorySelector(DrawContext context, int x, int y) {
        int rows = Math.max(1, (profile.storages().size() + 2) / 3);
        int height = 24 + rows * 34;
        drawSettingCard(context, x, y, height, HudStyle.border());
        drawText(context, x, y + 10, "Pages", TEXT);
        int chipY = y + 26;
        for (int index = 0; index < profile.storages().size(); index++) {
            Rect chip = inventoryChipRect(x, chipY, index);
            boolean selected = index == selectedStoragePageIndex;
            context.fill(chip.x, chip.y, chip.right(), chip.bottom(), selected ? BUTTON : CARD_ALT);
            drawText(context, chip.x + 8, chip.y + 6, profile.storages().get(index).title(), selected ? HudStyle.accent() : TEXT);
        }
        return y + height;
    }

    private void drawInventoryPage(DrawContext context, Rect viewport, int y, int mouseX, int mouseY) {
        HorizonStoragePage page = selectedStoragePage();
        if (page == null) {
            drawInfoCard(context, viewport.x, y, "Inventory", "Keine Containerdaten verfuegbar.");
            return;
        }

        int height = Math.max(224, page.rows() * 40 + 56);
        drawSettingCard(context, viewport.x, y, height, HudStyle.selected());
        drawText(context, viewport.x, y + 10, page.title(), TEXT);
        drawText(context, viewport.x + 180, y + 10, page.columns() + "x" + page.rows(), MUTED);

        Rect panel = new Rect(viewport.x + 380, y + 30, 218, height - 42);
        context.fill(panel.x, panel.y, panel.right(), panel.bottom(), CARD_ALT);

        HorizonInventorySlot hovered = hoveredSlot(page, viewport.x, y + 34, mouseX, mouseY);
        HorizonInventorySlot displaySlot = hovered != null ? hovered : firstSlot(page);

        for (int row = 0; row < page.rows(); row++) {
            for (int column = 0; column < page.columns(); column++) {
                int index = row * page.columns() + column;
                Rect slotRect = inventorySlotRect(viewport.x, y + 34, column, row);
                HorizonInventorySlot slot = slotByIndex(page, index);
                drawInventorySlot(context, slotRect, slot, slot == hovered);
            }
        }

        drawInventoryDetail(context, panel, displaySlot);
    }

    private void drawInventorySlot(DrawContext context, Rect rect, HorizonInventorySlot slot, boolean hovered) {
        HorizonInventoryItem item = slot == null ? HorizonInventoryItem.empty() : slot.item();
        int background = item.isEmpty() ? SLOT_EMPTY : SLOT;
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), hovered ? background + 0x00111111 : background);
        context.fill(rect.x, rect.y, rect.x + 2, rect.bottom(), item.isEmpty() ? CARD_ALT : rarityColor(item.rarity()));
        if (item.isEmpty()) {
            return;
        }
        drawCenteredText(context, rect.centerX(), rect.y + 6, abbreviation(item.displayName(), 8), TEXT);
        if (item.count() > 1) {
            drawText(context, rect.x + 4, rect.bottom() - 12, String.valueOf(item.count()), HudStyle.accent());
        }
    }

    private void drawInventoryDetail(DrawContext context, Rect rect, HorizonInventorySlot slot) {
        if (slot == null || slot.item().isEmpty()) {
            drawText(context, rect.x + 10, rect.y + 12, "Item Details", TEXT);
            drawWrappedText(context, rect.x + 10, rect.y + 32, "Fahre mit der Maus ueber einen Slot, um Name, Rarity und Lore anzuzeigen.", rect.width - 20, MUTED);
            return;
        }

        HorizonInventoryItem item = slot.item();
        drawText(context, rect.x + 10, rect.y + 12, item.displayName(), TEXT);
        drawText(context, rect.x + 10, rect.y + 28, item.rarity().isBlank() ? "Rarity unbekannt" : item.rarity(), rarityColor(item.rarity()));
        drawText(context, rect.x + 10, rect.y + 44, "Stack " + item.count(), MUTED);
        drawText(context, rect.x + 10, rect.y + 60, item.itemId().isBlank() ? "Item ID unbekannt" : item.itemId(), MUTED);
        drawWrappedText(context, rect.x + 10, rect.y + 84, item.lore().isBlank() ? "Keine Lore verfuegbar." : item.lore(), rect.width - 20, TEXT);
    }

    private int drawInfoCard(DrawContext context, int x, int y, String title, String value) {
        int height = 48 + wrappedLines(value, 590).size() * 12;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, title, TEXT);
        drawWrappedText(context, x + 10, y + 28, value, 590, MUTED);
        return y + height;
    }

    private void drawMetric(DrawContext context, Rect rect, String label, String value) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), CARD_ALT);
        drawText(context, rect.x + 10, rect.y + 8, label, MUTED);
        drawText(context, rect.x + 10, rect.y + 24, value, TEXT);
    }

    private void drawSettingCard(DrawContext context, int x, int y, int height, int markerColor) {
        int left = x - 12;
        int right = x + 622;
        context.fill(left, y, right, y + height - 10, CARD);
        context.fill(left, y, left + 3, y + height - 10, markerColor);
    }

    private void drawKeyValue(DrawContext context, int x, int y, String key, String value) {
        drawText(context, x, y, key, MUTED);
        drawText(context, x + 120, y, value, TEXT);
    }

    private void drawProgressBar(DrawContext context, int x, int y, int width, int height, float progress, int color, String label) {
        context.fill(x, y, x + width, y + height, BUTTON);
        context.fill(x, y, x + Math.max(0, Math.min(width, Math.round(width * progress))), y + height, color);
        drawText(context, x, y - 10, label, MUTED);
    }

    private void drawWrappedText(DrawContext context, int x, int y, String text, int maxWidth, int color) {
        int lineY = y;
        for (String line : wrappedLines(text, maxWidth)) {
            drawText(context, x, lineY, line, color);
            lineY += 12;
        }
    }

    private List<String> wrappedLines(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        for (String paragraph : text.split("\\R")) {
            String[] words = paragraph.split(" ");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (textRenderer.getWidth(candidate) > maxWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            if (paragraph.isBlank()) {
                lines.add("");
            }
        }
        return lines;
    }

    private void drawText(DrawContext context, int x, int y, String text, int color) {
        context.drawTextWithShadow(textRenderer, Text.literal(text), x, y, color);
    }

    private void drawCenteredText(DrawContext context, int centerX, int y, String text, int color) {
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(text), centerX, y, color);
    }

    private void drawWindowChrome(DrawContext context, Rect frame) {
        context.fill(frame.x, frame.y, frame.right(), frame.bottom(), WINDOW);
        context.fill(frame.x, frame.y, frame.right(), frame.y + 34, WINDOW_HEADER);
        drawText(context, frame.x + 12, frame.y + 12, "HORIZON", HudStyle.accent());
        drawText(context, frame.x + 100, frame.y + 12, "Viewer: " + displayRequestedPlayer(), TEXT);
        drawText(context, closeRect(frame).x, closeRect(frame).y + 2, "[X]", WARNING);
    }

    private void drawScrollBar(DrawContext context, Rect viewport) {
        int totalHeight = contentHeight();
        if (totalHeight <= viewport.height) {
            return;
        }
        int barX = viewport.right() - 4;
        context.fill(barX, viewport.y, barX + 2, viewport.bottom(), BUTTON);
        int thumbHeight = Math.max(24, Math.round((viewport.height / (float) totalHeight) * viewport.height));
        int maxThumbTravel = viewport.height - thumbHeight;
        int thumbY = viewport.y + Math.round((contentScrollOffset / (float) (totalHeight - viewport.height)) * maxThumbTravel);
        context.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, HudStyle.accent());
    }

    private int contentHeight() {
        if (profile == null || error != null) {
            return 340;
        }
        return switch (activeTab) {
            case OVERVIEW -> 152 + 98 + 104 + (34 + Math.max(1, profile.metadata().size()) * 20) + (34 + Math.max(1, profile.profileNames().size()) * 22);
            case INVENTORIES -> 152 + 94 + (24 + Math.max(1, (profile.storages().size() + 2) / 3) * 34)
                + Math.max(224, selectedStoragePage() == null ? 224 : selectedStoragePage().rows() * 40 + 56);
            case SKILLS -> 152 + infoCardHeight("Durchschnitt: " + formatDecimal(averageSkillLevel()) + " | Skills: " + sortedSkills().size())
                + Math.max(1, (sortedSkills().size() + 2) / 3) * 96;
            case SLAYERS -> 152 + infoCardHeight(formatNumber(totalSlayerXp())) + Math.max(1, sortedSlayers().size()) * 58;
            case PETS -> 152 + infoCardHeight("Gesamt: " + profile.pets().size() + " | Aktiv: " + activePetName())
                + Math.max(1, (sortedPets().size() + 2) / 3) * 96;
            case ACCESSORIES -> 152 + infoCardHeight("Gefunden: " + profile.accessories().size())
                + Math.max(1, (sortedAccessories().size() + 2) / 3) * 96;
        };
    }

    private int infoCardHeight(String value) {
        return 48 + wrappedLines(value, 590).size() * 12;
    }

    private HorizonStoragePage selectedStoragePage() {
        if (profile == null || profile.storages().isEmpty()) {
            return null;
        }
        selectedStoragePageIndex = Math.max(0, Math.min(selectedStoragePageIndex, profile.storages().size() - 1));
        return profile.storages().get(selectedStoragePageIndex);
    }

    private HorizonInventorySlot hoveredSlot(HorizonStoragePage page, int x, int y, int mouseX, int mouseY) {
        for (int row = 0; row < page.rows(); row++) {
            for (int column = 0; column < page.columns(); column++) {
                Rect rect = inventorySlotRect(x, y, column, row);
                if (rect.contains(mouseX, mouseY)) {
                    return slotByIndex(page, row * page.columns() + column);
                }
            }
        }
        return null;
    }

    private HorizonInventorySlot firstSlot(HorizonStoragePage page) {
        return page.slots().stream().findFirst().orElse(null);
    }

    private HorizonInventorySlot slotByIndex(HorizonStoragePage page, int index) {
        for (HorizonInventorySlot slot : page.slots()) {
            if (slot.index() == index) {
                return slot;
            }
        }
        return null;
    }

    private List<HorizonSkill> sortedSkills() {
        return profile.skills().stream()
            .sorted(Comparator.comparingInt(HorizonSkill::level).reversed().thenComparing(HorizonSkill::displayName))
            .toList();
    }

    private List<HorizonSlayerBoss> sortedSlayers() {
        return profile.slayers().stream()
            .sorted(Comparator.comparingLong(HorizonSlayerBoss::experience).reversed())
            .toList();
    }

    private List<HorizonPet> sortedPets() {
        return profile.pets().stream()
            .sorted(Comparator.comparing(HorizonPet::active).reversed().thenComparing(HorizonPet::displayName))
            .toList();
    }

    private List<HorizonAccessory> sortedAccessories() {
        return profile.accessories().stream()
            .sorted(Comparator.comparing(HorizonAccessory::displayName))
            .toList();
    }

    private long totalSlayerXp() {
        long total = 0L;
        if (profile == null) {
            return total;
        }
        for (HorizonSlayerBoss slayer : profile.slayers()) {
            total += slayer.experience();
        }
        return total;
    }

    private double averageSkillLevel() {
        if (profile == null || profile.skills().isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (HorizonSkill skill : profile.skills()) {
            total += skill.level() + skill.progress();
        }
        return total / profile.skills().size();
    }

    private String activePetName() {
        if (profile == null) {
            return "--";
        }
        return profile.pets().stream().filter(HorizonPet::active).map(HorizonPet::displayName).findFirst().orElse("--");
    }

    private String displayRequestedPlayer() {
        return requestedPlayer == null || requestedPlayer.isBlank() ? "Spieler" : requestedPlayer;
    }

    private String profileName() {
        if (profile == null || profile.profileName() == null || profile.profileName().isBlank()) {
            return "Unbekannt";
        }
        return profile.profileName();
    }

    private String compactUuid() {
        if (profile == null || profile.playerUuid() == null || profile.playerUuid().length() < 13) {
            return profile == null ? "" : profile.playerUuid();
        }
        return profile.playerUuid().substring(0, 8) + "..." + profile.playerUuid().substring(profile.playerUuid().length() - 4);
    }

    private String gameModeText() {
        if (profile == null || profile.gameMode().isBlank()) {
            return "Normal Profile";
        }
        return humanize(profile.gameMode());
    }

    private String initials() {
        String name = profile == null ? displayRequestedPlayer() : profile.playerName();
        if (name == null || name.isBlank()) {
            return "HV";
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase(Locale.ROOT);
    }

    private Rect frame() {
        int frameWidth = Math.min(960, width - 28);
        int frameHeight = Math.min(680, height - 28);
        return new Rect((width - frameWidth) / 2, (height - frameHeight) / 2, frameWidth, frameHeight);
    }

    private Rect sidebarRect(Rect frame) {
        return new Rect(frame.x + 12, frame.y + 44, 144, frame.height - 56);
    }

    private Rect contentViewportRect(Rect frame) {
        int left = sidebarRect(frame).right() + 18;
        int top = frame.y + 44;
        return new Rect(left, top, frame.right() - left - 12, frame.bottom() - top - 12);
    }

    private Rect contentClipRect(Rect frame) {
        Rect viewport = contentViewportRect(frame);
        return new Rect(viewport.x - 12, viewport.y, frame.right() - viewport.x, viewport.height);
    }

    private Rect closeRect(Rect frame) {
        return new Rect(frame.right() - 24, frame.y + 12, 18, 18);
    }

    private Rect sidebarTabRect(Rect sidebar, int index) {
        return new Rect(sidebar.x, sidebar.y + index * 36, sidebar.width, 28);
    }

    private Rect inventoryChipRect(int x, int y, int index) {
        return new Rect(x - 2 + (index % 3) * 198, y + (index / 3) * 34, 188, 24);
    }

    private Rect inventorySlotRect(int x, int y, int column, int row) {
        return new Rect(x + column * 40 - 6, y + row * 40, 34, 34);
    }

    private Rect statGridRect(int x, int y, int index) {
        return new Rect(x - 12 + (index % 3) * 206, y + (index / 3) * 96, 194, 84);
    }

    private float progress(int value, int max) {
        if (max <= 0) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value / (float) max));
    }

    private String abbreviation(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + ".";
        return compact;
    }

    private String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(normalized.length());
        boolean capitalize = true;
        for (char character : normalized.toCharArray()) {
            if (Character.isWhitespace(character)) {
                builder.append(character);
                capitalize = true;
            } else if (capitalize) {
                builder.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private String formatCoins(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000D) {
            return String.format(Locale.ROOT, "%.2fB", value / 1_000_000_000D);
        }
        if (abs >= 1_000_000D) {
            return String.format(Locale.ROOT, "%.2fM", value / 1_000_000D);
        }
        if (abs >= 1_000D) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000D);
        }
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private String formatNumber(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private int rarityColor(String rarity) {
        if (rarity == null) {
            return TEXT;
        }
        return switch (rarity.toUpperCase(Locale.ROOT)) {
            case "COMMON" -> 0xFFD7D9DE;
            case "UNCOMMON" -> 0xFF76E08A;
            case "RARE" -> 0xFF5CA8FF;
            case "EPIC" -> 0xFFD783FF;
            case "LEGENDARY" -> 0xFFFFB44F;
            case "MYTHIC" -> 0xFFFF6BB0;
            case "DIVINE" -> 0xFF74E6E8;
            case "SPECIAL", "VERY SPECIAL" -> 0xFFF26CF9;
            default -> HudStyle.accent();
        };
    }

    private enum ViewerTab {
        OVERVIEW("Overview"),
        INVENTORIES("Inventories"),
        SKILLS("Skills"),
        SLAYERS("Slayers"),
        PETS("Pets"),
        ACCESSORIES("Accessories");

        private final String label;

        ViewerTab(String label) {
            this.label = label;
        }
    }

    private record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        int centerX() {
            return x + (width / 2);
        }

        boolean contains(double px, double py) {
            return px >= x && px <= right() && py >= y && py <= bottom();
        }
    }
}
