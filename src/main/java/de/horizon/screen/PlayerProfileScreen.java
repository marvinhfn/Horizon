package de.horizon.screen;
import com.google.common.collect.LinkedHashMultimap;
import de.horizon.api.profile.HorizonAccessory;
import de.horizon.api.profile.HorizonAccessoryStorage;
import de.horizon.api.profile.HorizonDungeonClass;
import de.horizon.api.profile.HorizonDungeonFloor;
import de.horizon.api.profile.HorizonInventoryItem;
import de.horizon.api.profile.HorizonInventorySlot;
import de.horizon.api.profile.HorizonPet;
import de.horizon.api.profile.HorizonProfileData;
import de.horizon.api.profile.HorizonProfileGateway;
import de.horizon.api.profile.HorizonSkill;
import de.horizon.api.profile.HorizonSlayerBoss;
import de.horizon.api.profile.HorizonStoragePage;
import de.horizon.hud.HudStyle;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerProfileScreen extends Screen {
    private static final double[] DUNGEON_LEVEL_XP = {
        50, 75, 110, 160, 230, 330, 470, 670, 950, 1340,
        1890, 2665, 3760, 5260, 7380, 10300, 14400, 20000, 27600, 38000,
        52500, 71500, 97000, 132000, 180000, 243000, 328000, 445000, 600000, 800000,
        1065000, 1410000, 1900000, 2500000, 3300000, 4300000, 5600000, 7200000, 9200000, 12000000,
        15000000, 19000000, 24000000, 30000000, 38000000, 48000000, 60000000, 75000000, 93000000, 116250000
    };
    private static final double DUNGEON_POST_50_XP = 200_000_000D;
    private static final int INVENTORY_CHIPS_PER_ROW = 8;
    private static final int INVENTORY_CHIP_SIZE = 28;
    private static final int INVENTORY_CHIP_GAP = 8;
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
    private String expandedDungeonFloorId;
    private ItemStack hoveredTooltipStack;

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
            int chipY = inventorySelectorTop(viewport);
            for (int index = 0; index < profile.storages().size(); index++) {
                if (inventoryChipRect(viewport, chipY, index).contains(click.x(), click.y())) {
                    selectedStoragePageIndex = index;
                    return true;
                }
            }
        }
        if (profile != null && activeTab == ViewerTab.DUNGEONS) {
            Rect viewport = contentViewportRect(frame);
            String clickedFloorId = clickedDungeonFloorId(viewport, click.x(), click.y());
            if (clickedFloorId != null) {
                expandedDungeonFloorId = clickedFloorId.equals(expandedDungeonFloorId) ? null : clickedFloorId;
                return true;
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
        hoveredTooltipStack = null;

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
                case DUNGEONS -> drawDungeons(context, viewport);
                case SKILLS -> drawSkills(context, viewport);
                case SLAYERS -> drawSlayers(context, viewport);
                case PETS -> drawPets(context, viewport, mouseX, mouseY);
                case ACCESSORIES -> drawAccessories(context, viewport, mouseX, mouseY);
            }
        }
        context.disableScissor();
        drawScrollBar(context, viewport);
        if (hoveredTooltipStack != null) {
            context.drawItemTooltip(textRenderer, hoveredTooltipStack, mouseX, mouseY);
        }

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
        y = drawSectionHeader(context, viewport.x, y, "Profile Viewer", "Lade SkyBlock-Daten fuer " + displayRequestedPlayer() + ".");
        y = drawInfoCard(context, viewport.x, y, "Backend", "Der Viewer wartet auf Horizon-Backend, Auth-Token und Profildaten.");
        drawInfoCard(context, viewport.x, y, "Hinweis", "Aktiviere das Horizon-Backend im Client und starte den lokalen Backend-Service.");
    }

    private void drawError(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionHeader(context, viewport.x, y, "Profile Viewer", "Der Abruf konnte nicht abgeschlossen werden.");
        y = drawInfoCard(context, viewport.x, y, "Fehler", error);
        drawInfoCard(context, viewport.x, y, "Hinweis", "Wenn der Hypixel-Key ungueltig ist oder das Backend nicht laeuft, bleiben Inventories leer.");
    }

    private void drawOverview(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawHeroCard(context, viewport.x, y, "SkyBlock Profile", "Klare Uebersicht ueber Progress, Dungeons, Slayer und wichtige Profilwerte.");
        y = drawStatsStrip(context, viewport.x, y);
        y = drawOverviewHighlights(context, viewport.x, y);
        y = drawProfileSummary(context, viewport.x, y);
        y = drawMetadataCard(context, viewport.x, y);
        drawProfileListCard(context, viewport.x, y);
    }

    private void drawInventories(DrawContext context, Rect viewport, int mouseX, int mouseY) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionHeader(context, viewport.x, y, "Inventories", "Container-Browser fuer Inventory, Ender Chest, Wardrobe, Backpack, Pets und Accessory-Bag.");
        y = drawInventorySelector(context, viewport.x, y, mouseX, mouseY);
        drawInventoryPage(context, viewport, y, mouseX, mouseY);
    }

    private void drawSkills(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionHeader(context, viewport.x, y, "Skills", "Skill-Level, Progress und Experience je Skill.");
        List<HorizonSkill> skills = sortedSkills();
        int index = 0;
        for (HorizonSkill skill : skills) {
            Rect card = statGridRect(viewport.x, y, index++);
            boolean maxed = isMaxedSkill(skill);
            context.fill(card.x, card.y, card.right(), card.bottom(), maxed ? BUTTON : index % 2 == 0 ? CARD : CARD_ALT);
            drawText(context, card.x + 10, card.y + 8, skill.displayName(), TEXT);
            drawText(context, card.x + 10, card.y + 24, "Level " + skill.level(), maxed ? rarityColor("LEGENDARY") : HudStyle.accent());
            drawProgressBar(context, card.x + 10, card.y + 42, card.width - 20, 8, (float) skill.progress(), HudStyle.selected(), "Progress");
            drawText(context, card.x + 10, card.y + 56, formatNumber((long) skill.experience()) + " XP", MUTED);
            if (maxed) {
                drawText(context, card.x + 138, card.y + 8, "MAX", rarityColor("LEGENDARY"));
            }
        }
    }

    private void drawSlayers(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionHeader(context, viewport.x, y, "Slayers", "Level, XP und Kills pro Boss.");
        int row = 0;
        for (HorizonSlayerBoss slayer : sortedSlayers()) {
            Rect card = new Rect(viewport.x - 12, y + row * 66, 622, 58);
            boolean maxed = isMaxedSlayer(slayer);
            context.fill(card.x, card.y, card.right(), card.bottom(), maxed ? BUTTON : row % 2 == 0 ? CARD : CARD_ALT);
            drawSlayerMob(context, new Rect(card.x + 8, card.y + 4, 44, 48), slayer);
            drawText(context, card.x + 60, card.y + 10, slayer.displayName(), TEXT);
            drawText(context, card.x + 60, card.y + 28, slayerMobName(slayer.id()), MUTED);
            drawText(context, card.x + 248, card.y + 10, "Level " + slayer.level(), maxed ? rarityColor("LEGENDARY") : HudStyle.accent());
            drawText(context, card.x + 378, card.y + 10, formatNumber(slayer.experience()) + " XP", TEXT);
            drawText(context, card.x + 538, card.y + 10, formatNumber(slayer.kills()) + " Kills", MUTED);
            if (maxed) {
                drawText(context, card.x + 560, card.y + 32, "MAX", rarityColor("LEGENDARY"));
            }
            row++;
        }
    }

    private void drawPets(DrawContext context, Rect viewport, int mouseX, int mouseY) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionHeader(context, viewport.x, y, "Pets", "Aktive und gespeicherte Pets mit Tooltip, Level und Pet-Item.");
        drawPetBrowser(context, viewport, y, mouseX, mouseY);
    }

    private void drawDungeons(DrawContext context, Rect viewport) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionHeader(context, viewport.x, y, "Dungeons", "Catacombs, Klassenfortschritt, Secrets und absolvierte Floors.");
        y = drawDungeonOverview(context, viewport.x, y);
        y = drawDungeonFloorRows(context, viewport.x, y);
        HorizonDungeonFloor expandedFloor = expandedDungeonFloor();
        if (expandedFloor != null) {
            drawDungeonFloorDetail(context, viewport.x, y, expandedFloor);
        }
    }

    private int drawDungeonOverview(DrawContext context, int x, int y) {
        int height = 152;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, "Catacombs Overview", TEXT);
        drawMetric(context, new Rect(x - 4, y + 28, 145, 54), "Catacombs", String.valueOf(profile.catacombsLevel()));
        drawMetric(context, new Rect(x + 145, y + 28, 145, 54), "Secrets", formatNumber(profile.dungeons().secrets()));
        drawMetric(context, new Rect(x + 294, y + 28, 145, 54), "Selected", profile.dungeons().selectedClass().isBlank() ? "--" : humanize(profile.dungeons().selectedClass()));
        drawMetric(context, new Rect(x + 443, y + 28, 145, 54), "Avg Class", formatDecimal(averageDungeonClassLevel()));

        int index = 0;
        for (HorizonDungeonClass dungeonClass : sortedDungeonClasses()) {
            Rect card = statGridRect(x, y + 88, index++);
            boolean maxed = isMaxedDungeonClass(dungeonClass);
            context.fill(card.x, card.y, card.right(), card.bottom(), maxed || dungeonClass.selected() ? BUTTON : CARD_ALT);
            drawText(context, card.x + 10, card.y + 8, dungeonClass.displayName(), TEXT);
            drawText(context, card.x + 10, card.y + 24, "Level " + dungeonClass.level(), maxed ? rarityColor("LEGENDARY") : HudStyle.accent());
            drawText(context, card.x + 10, card.y + 40, formatNumber((long) dungeonClass.experience()) + " XP", MUTED);
            if (dungeonClass.selected()) {
                drawText(context, card.x + 10, card.y + 56, "Ausgewaehlt", rarityColor("LEGENDARY"));
            } else if (maxed) {
                drawText(context, card.x + 138, card.y + 8, "MAX", rarityColor("LEGENDARY"));
            }
        }
        return y + 88 + Math.max(1, (sortedDungeonClasses().size() + 2) / 3) * 96;
    }

    private int drawDungeonFloorRows(DrawContext context, int x, int y) {
        int height = 114;
        drawSettingCard(context, x, y, height, HudStyle.border());
        drawText(context, x, y + 10, "Floors", TEXT);
        drawText(context, x, y + 28, "Catacombs", MUTED);
        drawText(context, x, y + 68, "Master Mode", MUTED);
        drawDungeonFloorRow(context, x, y + 36, false);
        drawDungeonFloorRow(context, x, y + 76, true);
        return y + height;
    }

    private void drawDungeonFloorRow(DrawContext context, int x, int y, boolean masterMode) {
        for (int floor = 1; floor <= 7; floor++) {
            HorizonDungeonFloor data = dungeonFloor(masterMode, floor);
            Rect rect = dungeonFloorChipRect(x, y, floor - 1);
            boolean expanded = data != null && data.id().equals(expandedDungeonFloorId);
            context.fill(rect.x, rect.y, rect.right(), rect.bottom(), expanded ? BUTTON : CARD_ALT);
            context.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, expanded ? 0xFF304356 : 0xCC16202A);
            String label = masterMode ? "M" + floor : "F" + floor;
            drawCenteredText(context, rect.centerX(), rect.y + 6, label, TEXT);
            drawCenteredText(context, rect.centerX(), rect.y + 18, data == null ? "--" : String.valueOf(data.completions()), data == null ? MUTED : HudStyle.accent());
        }
    }

    private void drawDungeonFloorDetail(DrawContext context, int x, int y, HorizonDungeonFloor floor) {
        int height = 104;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, floor.displayName(), TEXT);
        drawKeyValue(context, x, y + 34, "Completions", formatNumber(floor.completions()));
        drawKeyValue(context, x + 280, y + 34, "Best Score", floor.bestScore() > 0 ? String.valueOf(floor.bestScore()) : "--");
        drawKeyValue(context, x, y + 56, "Fastest Time", floor.fastestTimeMs() > 0 ? formatDuration(floor.fastestTimeMs()) : "--:--");
        drawKeyValue(context, x + 280, y + 56, "S+ Time", floor.fastestSPlusTimeMs() > 0 ? formatDuration(floor.fastestSPlusTimeMs()) : "--:--");
        drawKeyValue(context, x, y + 78, "Mode", floor.id().startsWith("m") ? "Master" : "Catacombs");
    }

    private void drawAccessories(DrawContext context, Rect viewport, int mouseX, int mouseY) {
        int y = viewport.y - contentScrollOffset;
        y = drawSectionHeader(context, viewport.x, y, "Accessories", "Accessoires aus der Accessory-Bag, inklusive Power, Tuning und Rarity.");
        y = drawAccessoryPages(context, viewport, y, mouseX, mouseY);
        int index = 0;
        for (HorizonAccessory accessory : sortedAccessories()) {
            Rect card = statGridRect(viewport.x, y, index++);
            context.fill(card.x, card.y, card.right(), card.bottom(), index % 2 == 0 ? CARD : CARD_ALT);
            drawText(context, card.x + 10, card.y + 8, plainText(accessory.displayName()), TEXT);
            drawText(context, card.x + 10, card.y + 24, accessory.rarity().isBlank() ? "Rarity unbekannt" : accessory.rarity(), rarityColor(accessory.rarity()));
            drawWrappedText(context, card.x + 10, card.y + 38, accessory.enrichment().isBlank() ? "Kein Enrichment gelesen." : accessory.enrichment(), card.width - 20, MUTED);
        }
    }

    private int drawHeroCard(DrawContext context, int x, int y, String title, String subtitle) {
        int height = 152;
        drawSettingCard(context, x, y, height, HudStyle.accent());
        Rect avatar = new Rect(x + 8, y + 10, 88, 88);
        context.fill(avatar.x, avatar.y, avatar.right(), avatar.bottom(), CARD_ALT);
        drawPlayerModel(context, avatar);
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

    private int drawSectionHeader(DrawContext context, int x, int y, String title, String subtitle) {
        int height = 92;
        drawSettingCard(context, x, y, height, HudStyle.accent());
        drawText(context, x, y + 12, title, TEXT);
        drawWrappedText(context, x, y + 30, subtitle, 584, MUTED);
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

    private int drawOverviewHighlights(DrawContext context, int x, int y) {
        int height = 104;
        drawSettingCard(context, x, y, height, HudStyle.border());
        drawText(context, x, y + 10, "Highlights", TEXT);
        drawHighlightPill(context, x, y + 36, 180, "Top Skill", topSkillLabel());
        drawHighlightPill(context, x + 196, y + 36, 180, "Top Slayer", topSlayerLabel());
        drawHighlightPill(context, x + 392, y + 36, 196, "Maxed", maxedSummaryLabel());
        return y + height;
    }

    private int drawProfileSummary(DrawContext context, int x, int y) {
        int height = 126;
        drawSettingCard(context, x, y, height, HudStyle.selected());
        drawText(context, x, y + 10, "Profile Summary", TEXT);
        drawKeyValue(context, x, y + 34, "Storage Pages", String.valueOf(profile.storages().size()));
        drawKeyValue(context, x, y + 54, "Accessories", String.valueOf(profile.accessories().size()));
        drawKeyValue(context, x, y + 74, "Purse", formatCoins(profile.purse()));
        drawKeyValue(context, x + 280, y + 34, "Pets", String.valueOf(profile.pets().size()));
        drawKeyValue(context, x + 280, y + 54, "Slayer XP", formatNumber(totalSlayerXp()));
        drawKeyValue(context, x + 280, y + 74, "Avg Skill", formatDecimal(averageSkillLevel()));
        drawKeyValue(context, x + 280, y + 94, "Bank", formatCoins(profile.bank()));
        return y + height;
    }

    private void drawHighlightPill(DrawContext context, int x, int y, int width, String label, String value) {
        Rect rect = new Rect(x - 4, y, width, 46);
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), CARD_ALT);
        context.fill(rect.x, rect.y, rect.x + 3, rect.bottom(), HudStyle.accent());
        drawText(context, rect.x + 10, rect.y + 8, label, MUTED);
        drawWrappedTextClamped(context, rect.x + 10, rect.y + 22, value, rect.width - 20, 1, TEXT);
    }

    private void drawPetBrowser(DrawContext context, Rect viewport, int y, int mouseX, int mouseY) {
        List<HorizonPet> pets = sortedPets();
        int rows = Math.max(1, (pets.size() + 7) / 8);
        int height = Math.max(276, rows * 40 + 56);
        drawSettingCard(context, viewport.x, y, height, HudStyle.selected());
        drawText(context, viewport.x, y + 10, "Pet Menu", TEXT);
        drawText(context, viewport.x + 180, y + 10, "Hover fuer Tooltip, Level, Rarity und Pet-Item", MUTED);

        Rect detail = new Rect(viewport.x + 404, y + 30, 194, height - 42);
        context.fill(detail.x, detail.y, detail.right(), detail.bottom(), CARD_ALT);

        HorizonPet hoveredPet = hoveredPet(pets, viewport.x, y + 34, mouseX, mouseY);
        if (hoveredPet != null) {
            hoveredTooltipStack = buildPetStack(hoveredPet);
        }

        for (int index = 0; index < pets.size(); index++) {
            HorizonPet pet = pets.get(index);
            Rect slot = petSlotRect(viewport.x, y + 34, index);
            boolean hovered = hoveredPet == pet;
            drawPetSlot(context, slot, pet, hovered);
        }

        drawPetDetail(context, detail, hoveredPet);
    }

    private void drawPetSlot(DrawContext context, Rect rect, HorizonPet pet, boolean hovered) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), hovered ? 0xFF3A4958 : pet.active() ? BUTTON : CARD_ALT);
        context.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, hovered ? 0xE2354657 : 0xCC16202A);
        context.fill(rect.x, rect.y, rect.x + 2, rect.bottom(), pet.active() ? rarityColor(pet.tier()) : 0x664F5A66);
        ItemStack stack = buildPetStack(pet);
        context.drawItem(stack, rect.x + 9, rect.y + 9);
        if (isMaxedPet(pet)) {
            drawText(context, rect.x + 4, rect.y + 3, "MAX", rarityColor("LEGENDARY"));
        }
    }

    private void drawPetDetail(DrawContext context, Rect rect, HorizonPet pet) {
        if (pet == null) {
            drawText(context, rect.x + 10, rect.y + 12, "Keine Pets", TEXT);
            return;
        }
        ItemStack stack = buildPetStack(pet);
        context.drawItem(stack, rect.x + 10, rect.y + 10);
        drawWrappedTextClamped(context, rect.x + 34, rect.y + 12, plainText(pet.displayName()), rect.width - 44, 2, TEXT);
        drawText(context, rect.x + 10, rect.y + 44, pet.tier().isBlank() ? "Tier unbekannt" : pet.tier(), rarityColor(pet.tier()));
        drawText(context, rect.x + 10, rect.y + 60, pet.level() > 0 ? "Level " + pet.level() : "Level --", isMaxedPet(pet) ? rarityColor("LEGENDARY") : TEXT);
        drawText(context, rect.x + 10, rect.y + 76, "XP " + formatNumber((long) pet.experience()), MUTED);
        drawText(context, rect.x + 10, rect.y + 94, pet.active() ? "Aktiv" : "Nicht aktiv", pet.active() ? HudStyle.accent() : MUTED);
        drawWrappedTextClamped(context, rect.x + 10, rect.y + 114, pet.heldItemDisplayName().isBlank() ? "Kein Pet Item" : pet.heldItemDisplayName(), rect.width - 20, 2, TEXT);
        drawText(context, rect.x + 10, rect.y + 144, "Candies " + pet.candyUsed(), MUTED);
        drawText(context, rect.x + 10, rect.y + 160, pet.soulbound() ? "Soulbound" : "Nicht soulbound", pet.soulbound() ? WARNING : MUTED);
        if (!pet.skin().isBlank()) {
            String skinDisplayName = pet.skinDisplayName().isBlank() ? humanize(pet.skin()) : plainText(pet.skinDisplayName());
            drawWrappedTextClamped(context, rect.x + 10, rect.y + 178, "Skin " + skinDisplayName, rect.width - 20, 2, MUTED);
        }
    }

    private int drawAccessoryStorageSummary(DrawContext context, int x, int y) {
        HorizonAccessoryStorage storage = profile.accessoryStorage();
        int height = 118;
        drawSettingCard(context, x, y, height, HudStyle.border());
        drawText(context, x, y + 10, "Accessory Storage", TEXT);
        drawKeyValue(context, x, y + 34, "Selected Power", storage.selectedPower().isBlank() ? "--" : humanize(storage.selectedPower()));
        drawKeyValue(context, x, y + 54, "Magical Power", storage.highestMagicalPower() <= 0 ? "--" : formatNumber(storage.highestMagicalPower()));
        drawKeyValue(context, x + 280, y + 34, "Bag Upgrades", String.valueOf(storage.bagUpgradesPurchased()));
        drawKeyValue(context, x + 280, y + 54, "Unlocked Powers", String.valueOf(storage.unlockedPowers().size()));
        String tuning = storage.tuning().isEmpty() ? "Kein Tuning gelesen." : storage.tuning().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> humanize(entry.getKey()) + ": " + entry.getValue())
            .reduce((left, right) -> left + " | " + right)
            .orElse("Kein Tuning gelesen.");
        drawWrappedTextClamped(context, x + 10, y + 78, tuning, 584, 2, MUTED);
        return y + height;
    }

    private int drawAccessoryPages(DrawContext context, Rect viewport, int y, int mouseX, int mouseY) {
        List<HorizonStoragePage> pages = accessoryPages();
        if (pages.isEmpty()) {
            return y;
        }
        for (HorizonStoragePage page : pages) {
            int height = Math.max(224, page.rows() * 40 + 56);
            drawSettingCard(context, viewport.x, y, height, HudStyle.selected());
            drawText(context, viewport.x, y + 10, page.title(), TEXT);
            drawText(context, viewport.x + 180, y + 10, page.columns() + "x" + page.rows(), MUTED);
            for (int row = 0; row < page.rows(); row++) {
                for (int column = 0; column < page.columns(); column++) {
                    int index = row * page.columns() + column;
                    Rect slotRect = inventorySlotRect(viewport.x, y + 34, column, row);
                    HorizonInventorySlot slot = slotByIndex(page, index);
                    drawInventorySlot(context, slotRect, slot, false);
                    if (slotRect.contains(mouseX, mouseY) && slot != null && slot.item() != null && !slot.item().isEmpty()) {
                        hoveredTooltipStack = buildItemStack(slot.item());
                    }
                }
            }
            y += height;
        }
        return y;
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

    private int drawInventorySelector(DrawContext context, int x, int y, int mouseX, int mouseY) {
        int rows = inventoryChipRows();
        int height = 24 + rows * 34;
        drawSettingCard(context, x, y, height, HudStyle.border());
        drawText(context, x, y + 10, "Pages", TEXT);
        HorizonStoragePage selected = selectedStoragePage();
        if (selected != null) {
            drawText(context, x + 70, y + 10, selected.title(), MUTED);
        }
        int chipY = y + 26;
        Rect viewport = new Rect(x, 0, 622, 0);
        for (int index = 0; index < profile.storages().size(); index++) {
            Rect chip = inventoryChipRect(viewport, chipY, index);
            HorizonStoragePage page = profile.storages().get(index);
            boolean selectedChip = index == selectedStoragePageIndex;
            renderStorageButton(context, chip, page, selectedChip);
            if (chip.contains(mouseX, mouseY)) {
                hoveredTooltipStack = buildItemStack(page.buttonItem());
            }
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

        HorizonInventorySlot hovered = hoveredSlot(page, viewport.x, y + 34, mouseX, mouseY);
        if (hovered != null && hovered.item() != null && !hovered.item().isEmpty()) {
            hoveredTooltipStack = buildItemStack(hovered.item());
        }

        for (int row = 0; row < page.rows(); row++) {
            for (int column = 0; column < page.columns(); column++) {
                int index = row * page.columns() + column;
                Rect slotRect = inventorySlotRect(viewport.x, y + 34, column, row);
                HorizonInventorySlot slot = slotByIndex(page, index);
                boolean isHovered = hovered != null && hovered.index() == index && !hovered.item().isEmpty();
                drawInventorySlot(context, slotRect, slot, isHovered);
            }
        }

        if (hovered != null && !hovered.item().isEmpty()) {
            Rect panel = new Rect(viewport.x + 380, y + 30, 218, height - 42);
            context.fill(panel.x, panel.y, panel.right(), panel.bottom(), CARD_ALT);
            drawInventoryDetail(context, panel, hovered);
        }
    }

    private void drawInventorySlot(DrawContext context, Rect rect, HorizonInventorySlot slot, boolean hovered) {
        HorizonInventoryItem item = slot == null ? HorizonInventoryItem.empty() : slot.item();
        int background = item.isEmpty() ? 0xFF1B2630 : 0xFF2A3744;
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), hovered ? 0xFF3A4958 : background);
        context.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, hovered ? 0xE2354657 : 0xCC1E2A35);
        context.fill(rect.x, rect.y, rect.x + 2, rect.bottom(), item.isEmpty() ? CARD_ALT : rarityColor(item.rarity()));
        if (item.isEmpty()) {
            return;
        }
        ItemStack stack = buildItemStack(item);
        context.drawItem(stack, rect.x + 8, rect.y + 8);
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
        ItemStack stack = buildItemStack(item);
        context.drawItem(stack, rect.x + 10, rect.y + 10);
        drawWrappedTextClamped(context, rect.x + 34, rect.y + 12, plainText(item.displayName()), rect.width - 44, 2, TEXT);
        drawText(context, rect.x + 34, rect.y + 38, item.rarity().isBlank() ? "Rarity unbekannt" : item.rarity(), rarityColor(item.rarity()));
        drawText(context, rect.x + 10, rect.y + 58, "Stack " + item.count(), MUTED);
        drawText(context, rect.x + 10, rect.y + 72, item.itemId().isBlank() ? "Item ID unbekannt" : item.itemId(), MUTED);
        int maxLoreLines = Math.max(4, (rect.height - 100) / 12);
        drawWrappedTextClamped(context, rect.x + 10, rect.y + 92, item.lore().isBlank() ? "Keine Lore verfuegbar." : plainText(item.lore()), rect.width - 20, maxLoreLines, TEXT);
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

    private void drawWrappedTextClamped(DrawContext context, int x, int y, String text, int maxWidth, int maxLines, int color) {
        List<String> lines = wrappedLines(text, maxWidth);
        int limit = Math.max(1, Math.min(maxLines, lines.size()));
        for (int index = 0; index < limit; index++) {
            String line = lines.get(index);
            if (index == limit - 1 && lines.size() > limit && line.length() > 1) {
                line = abbreviation(line, Math.max(2, Math.min(40, line.length() - 1)));
            }
            drawText(context, x, y + index * 12, line, color);
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
            case OVERVIEW -> 152 + 98 + 126 + (34 + Math.max(1, profile.metadata().size()) * 20) + (34 + Math.max(1, profile.profileNames().size()) * 22);
            case INVENTORIES -> tabHeaderHeight(ViewerTab.INVENTORIES) + (24 + inventoryChipRows() * 34)
                + Math.max(224, selectedStoragePage() == null ? 224 : selectedStoragePage().rows() * 40 + 56);
            case DUNGEONS -> tabHeaderHeight(ViewerTab.DUNGEONS)
                + 114
                + (expandedDungeonFloor() == null ? 0 : 104);
            case SKILLS -> tabHeaderHeight(ViewerTab.SKILLS)
                + Math.max(1, (sortedSkills().size() + 2) / 3) * 96;
            case SLAYERS -> tabHeaderHeight(ViewerTab.SLAYERS) + Math.max(1, sortedSlayers().size()) * 66;
            case PETS -> tabHeaderHeight(ViewerTab.PETS)
                + Math.max(276, Math.max(1, (sortedPets().size() + 7) / 8) * 40 + 56);
            case ACCESSORIES -> tabHeaderHeight(ViewerTab.ACCESSORIES)
                + accessoryPages().stream().mapToInt(page -> Math.max(224, page.rows() * 40 + 56)).sum()
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

    private List<HorizonDungeonClass> sortedDungeonClasses() {
        return profile.dungeons().classes().stream()
            .sorted(Comparator.comparing(HorizonDungeonClass::selected).reversed().thenComparingInt(HorizonDungeonClass::level).reversed())
            .toList();
    }

    private List<HorizonDungeonFloor> sortedDungeonFloors() {
        return profile.dungeons().floors().stream()
            .sorted(Comparator.comparing(HorizonDungeonFloor::id))
            .toList();
    }

    private HorizonDungeonFloor dungeonFloor(boolean masterMode, int floor) {
        String id = (masterMode ? "m" : "f") + floor;
        return profile.dungeons().floors().stream()
            .filter(entry -> id.equals(entry.id()))
            .findFirst()
            .orElse(null);
    }

    private HorizonDungeonFloor expandedDungeonFloor() {
        if (expandedDungeonFloorId == null || profile == null) {
            return null;
        }
        return profile.dungeons().floors().stream()
            .filter(floor -> expandedDungeonFloorId.equals(floor.id()))
            .findFirst()
            .orElse(null);
    }

    private String clickedDungeonFloorId(Rect viewport, double mouseX, double mouseY) {
        int y = viewport.y - contentScrollOffset;
        y += tabHeaderHeight(ViewerTab.DUNGEONS);
        for (int floor = 1; floor <= 7; floor++) {
            Rect normal = dungeonFloorChipRect(viewport.x, y + 36, floor - 1);
            if (normal.contains(mouseX, mouseY) && dungeonFloor(false, floor) != null) {
                return "f" + floor;
            }
            Rect master = dungeonFloorChipRect(viewport.x, y + 76, floor - 1);
            if (master.contains(mouseX, mouseY) && dungeonFloor(true, floor) != null) {
                return "m" + floor;
            }
        }
        return null;
    }

    private HorizonPet hoveredPet(List<HorizonPet> pets, int x, int y, int mouseX, int mouseY) {
        for (int index = 0; index < pets.size(); index++) {
            if (petSlotRect(x, y, index).contains(mouseX, mouseY)) {
                return pets.get(index);
            }
        }
        return null;
    }

    private List<HorizonSlayerBoss> sortedSlayers() {
        return profile.slayers().stream()
            .sorted(Comparator.comparingLong(HorizonSlayerBoss::experience).reversed())
            .toList();
    }

    private List<HorizonPet> sortedPets() {
        return profile.pets().stream()
            .sorted(Comparator.comparing(HorizonPet::active).reversed().thenComparing(pet -> plainText(pet.displayName())))
            .toList();
    }

    private List<HorizonAccessory> sortedAccessories() {
        return profile.accessories().stream()
            .sorted(Comparator.comparing(accessory -> plainText(accessory.displayName())))
            .toList();
    }

    private List<HorizonStoragePage> accessoryPages() {
        return profile.storages().stream()
            .filter(page -> {
                String id = page.id() == null ? "" : page.id().toLowerCase(Locale.ROOT);
                String title = page.title() == null ? "" : page.title().toLowerCase(Locale.ROOT);
                return id.contains("accessory") || id.contains("talisman") || title.contains("accessory");
            })
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

    private double averageDungeonClassLevel() {
        if (profile == null || profile.dungeons().classes().isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (HorizonDungeonClass dungeonClass : profile.dungeons().classes()) {
            total += dungeonLevelWithProgress(dungeonClass.experience());
        }
        return total / profile.dungeons().classes().size();
    }

    private double dungeonLevelWithProgress(double experience) {
        if (experience <= 0.0D) {
            return 0.0D;
        }
        double remaining = experience;
        int level = 0;
        for (double levelXp : DUNGEON_LEVEL_XP) {
            if (remaining < levelXp) {
                return level + Math.max(0.0D, Math.min(1.0D, remaining / levelXp));
            }
            remaining -= levelXp;
            level++;
        }
        if (remaining <= 0.0D) {
            return level;
        }
        return level + (remaining / DUNGEON_POST_50_XP);
    }

    private boolean isMaxedSkill(HorizonSkill skill) {
        return skill != null && skill.level() >= maxSkillLevel(skill.id());
    }

    private int maxSkillLevel(String skillId) {
        if (skillId == null) {
            return 50;
        }
        return switch (skillId.toLowerCase(Locale.ROOT)) {
            case "farming", "mining", "combat", "enchanting", "taming", "foraging" -> 60;
            case "hunting", "runecrafting", "social" -> 25;
            case "carpentry" -> 50;
            default -> 50;
        };
    }

    private boolean isMaxedDungeonClass(HorizonDungeonClass dungeonClass) {
        return dungeonClass != null && dungeonClass.level() >= 50;
    }

    private boolean isMaxedSlayer(HorizonSlayerBoss slayer) {
        return slayer != null && switch ((slayer.id() == null ? "" : slayer.id()).toLowerCase(Locale.ROOT)) {
            case "vampire" -> slayer.level() >= 5;
            default -> slayer.level() >= 9;
        };
    }

    private boolean isMaxedPet(HorizonPet pet) {
        return pet != null && pet.level() >= maxPetLevel(pet.type());
    }

    private int maxPetLevel(String petType) {
        if (petType == null) {
            return 100;
        }
        return "GOLDEN_DRAGON".equalsIgnoreCase(petType) ? 200 : 100;
    }

    private String topSkillLabel() {
        HorizonSkill skill = sortedSkills().stream().findFirst().orElse(null);
        return skill == null ? "--" : skill.displayName() + " " + skill.level();
    }

    private String topSlayerLabel() {
        HorizonSlayerBoss slayer = sortedSlayers().stream().findFirst().orElse(null);
        return slayer == null ? "--" : slayer.displayName() + " L" + slayer.level();
    }

    private String maxedSummaryLabel() {
        long maxedSkills = sortedSkills().stream().filter(this::isMaxedSkill).count();
        long maxedClasses = sortedDungeonClasses().stream().filter(dungeonClass -> dungeonClass.level() >= 50).count();
        long maxedSlayers = sortedSlayers().stream().filter(this::isMaxedSlayer).count();
        return maxedSkills + " Skills | " + maxedClasses + " 50+ Klassen | " + maxedSlayers + " Slayer";
    }

    private String activePetName() {
        if (profile == null) {
            return "--";
        }
        return profile.pets().stream().filter(HorizonPet::active).map(HorizonPet::displayName).map(this::plainText).findFirst().orElse("--");
    }

    private String slayerMobName(String slayerId) {
        if (slayerId == null) {
            return "";
        }
        return switch (slayerId.toLowerCase(Locale.ROOT)) {
            case "zombie" -> "Revenant Horror";
            case "spider" -> "Tarantula Broodfather";
            case "wolf" -> "Sven Packmaster";
            case "enderman" -> "Voidgloom Seraph";
            case "blaze" -> "Inferno Demonlord";
            case "vampire" -> "Riftstalker Bloodfiend";
            default -> humanize(slayerId);
        };
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

    private String formatDuration(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
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

    private Rect inventoryChipRect(Rect viewport, int y, int index) {
        return new Rect(
            viewport.x - 2 + (index % INVENTORY_CHIPS_PER_ROW) * (INVENTORY_CHIP_SIZE + INVENTORY_CHIP_GAP),
            y + (index / INVENTORY_CHIPS_PER_ROW) * 34,
            INVENTORY_CHIP_SIZE,
            INVENTORY_CHIP_SIZE
        );
    }

    private Rect inventorySlotRect(int x, int y, int column, int row) {
        return new Rect(x + column * 40 - 6, y + row * 40, 34, 34);
    }

    private Rect petSlotRect(int x, int y, int index) {
        return new Rect(x - 6 + (index % 8) * 40, y + (index / 8) * 40, 34, 34);
    }

    private Rect dungeonFloorChipRect(int x, int y, int index) {
        return new Rect(x - 2 + index * 86, y, 74, 28);
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

    private int inventoryChipRows() {
        return Math.max(1, (profile.storages().size() + INVENTORY_CHIPS_PER_ROW - 1) / INVENTORY_CHIPS_PER_ROW);
    }

    private int inventorySelectorTop(Rect viewport) {
        return viewport.y - contentScrollOffset + tabHeaderHeight(ViewerTab.INVENTORIES) + 26;
    }

    private int tabHeaderHeight(ViewerTab tab) {
        return tab == ViewerTab.OVERVIEW ? 152 : 92;
    }

    private String slotLabel(HorizonInventoryItem item) {
        if (item.displayName() != null && !item.displayName().isBlank()) {
            return item.displayName();
        }
        return humanize(item.itemId());
    }

    private void renderStorageButton(DrawContext context, Rect rect, HorizonStoragePage page, boolean selected) {
        context.fill(rect.x, rect.y, rect.right(), rect.bottom(), selected ? BUTTON : CARD_ALT);
        context.fill(rect.x + 1, rect.y + 1, rect.right() - 1, rect.bottom() - 1, selected ? 0xFF304356 : 0xCC16202A);
        context.fill(rect.x, rect.y, rect.right(), rect.y + 2, selected ? HudStyle.accent() : 0x664F5A66);
        context.drawItem(buildItemStack(page.buttonItem()), rect.x + 6, rect.y + 6);
    }

    private ItemStack buildItemStack(HorizonInventoryItem item) {
        if (item == null || item.isEmpty() && (item.minecraftItemId() == null || item.minecraftItemId().isBlank())) {
            return Items.BARRIER.getDefaultStack();
        }
        Item vanillaItem = resolveVanillaItem(item.minecraftItemId());
        ItemStack stack = new ItemStack(vanillaItem, Math.max(1, item.count()));
        if (item.displayName() != null && !item.displayName().isBlank()) {
            stack.set(DataComponentTypes.CUSTOM_NAME, parseLegacyText(item.displayName()));
        }
        if (item.lore() != null && !item.lore().isBlank()) {
            List<Text> loreLines = new ArrayList<>();
            for (String line : item.lore().split("\\R")) {
                loreLines.add(parseLegacyText(line));
            }
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreLines));
        }
        if (item.leatherColor() >= 0) {
            stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(item.leatherColor()));
        }
        if (!item.iconTexture().isBlank() && vanillaItem == Items.PLAYER_HEAD) {
            var textures = LinkedHashMultimap.<String, Property>create();
            textures.put(
                "textures",
                item.iconTextureSignature().isBlank()
                    ? new Property("textures", item.iconTexture())
                    : new Property("textures", item.iconTexture(), item.iconTextureSignature())
            );
            GameProfile profile = new GameProfile(
                UUID.nameUUIDFromBytes(item.iconTexture().getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "horizon_head",
                new PropertyMap(textures)
            );
            stack.set(DataComponentTypes.PROFILE, net.minecraft.component.type.ProfileComponent.ofStatic(profile));
        }
        return stack;
    }

    private Text parseLegacyText(String value) {
        if (value == null || value.isBlank()) {
            return Text.empty();
        }
        MutableText result = Text.empty();
        StringBuilder segment = new StringBuilder();
        Formatting color = null;
        List<Formatting> modifiers = new ArrayList<>();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\u00A7' && index + 1 < value.length()) {
                if (segment.length() > 0) {
                    result.append(applyFormatting(segment.toString(), color, modifiers));
                    segment.setLength(0);
                }
                Formatting formatting = Formatting.byCode(value.charAt(++index));
                if (formatting == null) {
                    continue;
                }
                if (formatting == Formatting.RESET) {
                    color = null;
                    modifiers = new ArrayList<>();
                } else if (formatting.isColor()) {
                    color = formatting;
                } else {
                    modifiers.add(formatting);
                }
                continue;
            }
            segment.append(character);
        }
        if (segment.length() > 0) {
            result.append(applyFormatting(segment.toString(), color, modifiers));
        }
        return result;
    }

    private Text applyFormatting(String text, Formatting color, List<Formatting> modifiers) {
        MutableText formatted = Text.literal(text).styled(style -> style.withItalic(false));
        if (color != null) {
            formatted.styled(style -> style.withFormatting(color));
        }
        for (Formatting formatting : modifiers) {
            formatted.styled(style -> style.withFormatting(formatting));
        }
        return formatted;
    }

    private String plainText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("§.", "").replace('\u00A0', ' ').trim();
    }

    private ItemStack buildPetStack(HorizonPet pet) {
        String minecraftItemId = pet.minecraftItemId();
        if (minecraftItemId == null || minecraftItemId.isBlank()) {
            minecraftItemId = fallbackPetItemId(pet.type());
        }
        StringBuilder lore = new StringBuilder();
        lore.append("§7Rarity: ").append(rarityColorCode(pet.tier())).append(pet.tier().isBlank() ? "--" : humanize(pet.tier()));
        lore.append('\n').append("§7Level: §e").append(pet.level() > 0 ? pet.level() : "--");
        lore.append('\n').append("§7XP: §e").append(formatNumber((long) pet.experience()));
        if (!pet.skinDisplayName().isBlank()) {
            lore.append('\n').append("§8").append(plainText(pet.skinDisplayName())).append(" Skin");
        }
        if (!pet.heldItemDisplayName().isBlank()) {
            lore.append('\n').append("§7Held Item: §f").append(pet.heldItemDisplayName());
        }
        lore.append('\n').append(pet.active() ? "§aActive" : "§cInactive");
        if (pet.candyUsed() > 0) {
            lore.append('\n').append("§7Candies Used: §e").append(pet.candyUsed());
        }
        if (pet.soulbound()) {
            lore.append('\n').append("§8Soulbound");
        }
        return buildItemStack(new HorizonInventoryItem(
            pet.type(),
            minecraftItemId,
            pet.displayName(),
            pet.tier(),
            1,
            lore.toString(),
            pet.iconTexture(),
            pet.iconTextureSignature(),
            -1,
            false
        ));
    }

    private String fallbackPetItemId(String type) {
        if (type == null || type.isBlank()) {
            return "minecraft:player_head";
        }
        return switch (type) {
            case "WITHER_SKELETON" -> "minecraft:wither_skeleton_skull";
            case "ZOMBIE" -> "minecraft:zombie_head";
            case "CREEPER" -> "minecraft:creeper_head";
            case "ELEPHANT" -> "minecraft:gray_wool";
            case "SQUID", "JELLYFISH", "FLYING_FISH", "AMMONITE" -> "minecraft:tropical_fish_bucket";
            case "BAT" -> "minecraft:bat_spawn_egg";
            case "ROCK" -> "minecraft:stone";
            case "WOLF", "GRANDMA_WOLF", "HOUND" -> "minecraft:bone";
            default -> "minecraft:player_head";
        };
    }

    private ItemStack buildPlayerHead() {
        if (profile == null) {
            return Items.PLAYER_HEAD.getDefaultStack();
        }
        try {
            ItemStack stack = Items.PLAYER_HEAD.getDefaultStack();
            GameProfile gameProfile = texturedGameProfile();
            if (gameProfile != null) {
                stack.set(DataComponentTypes.PROFILE, net.minecraft.component.type.ProfileComponent.ofStatic(gameProfile));
            } else if (profile.playerUuid() != null && !profile.playerUuid().isBlank()) {
                stack.set(DataComponentTypes.PROFILE, net.minecraft.component.type.ProfileComponent.ofDynamic(UUID.fromString(profile.playerUuid())));
            }
            if (profile.playerName() != null && !profile.playerName().isBlank()) {
                stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(profile.playerName()));
            }
            return stack;
        } catch (Exception ignored) {
            return Items.PLAYER_HEAD.getDefaultStack();
        }
    }

    private void drawPlayerModel(DrawContext context, Rect rect) {
        LivingEntity entity = buildPlayerModelEntity();
        if (entity == null) {
            drawLargeItem(context, buildPlayerHead(), rect.x + 12, rect.y + 12, 4.0F);
            return;
        }
        int centerX = rect.x + rect.width / 2;
        int centerY = rect.y + rect.height / 2;
        InventoryScreen.drawEntity(
            context,
            rect.x + 4,
            rect.y + 2,
            rect.right() - 4,
            rect.bottom() + 36,
            30,
            0.0625F,
            centerX,
            centerY,
            entity
        );
    }

    private LivingEntity buildPlayerModelEntity() {
        if (client == null || client.world == null) {
            return client == null ? null : client.player;
        }
        if (profile == null || profile.playerUuid() == null || profile.playerUuid().isBlank()) {
            return client.player;
        }
        try {
            return new OtherClientPlayerEntity(
                client.world,
                texturedGameProfile(UUID.fromString(profile.playerUuid()), profile.playerName().isBlank() ? displayRequestedPlayer() : profile.playerName())
            );
        } catch (Exception ignored) {
            return client.player;
        }
    }

    private void drawSlayerMob(DrawContext context, Rect rect, HorizonSlayerBoss slayer) {
        LivingEntity entity = buildSlayerEntity(slayer);
        if (entity == null) {
            drawLargeItem(context, fallbackSlayerIcon(slayer), rect.x + 8, rect.y + 8, 1.5F);
            return;
        }
        int centerX = rect.x + rect.width / 2;
        int centerY = rect.y + rect.height / 2;
        InventoryScreen.drawEntity(
            context,
            rect.x,
            rect.y,
            rect.right(),
            rect.bottom() + 18,
            18,
            0.0625F,
            centerX,
            centerY,
            entity
        );
    }

    private String rarityColorCode(String rarity) {
        if (rarity == null) {
            return "§f";
        }
        return switch (rarity.toUpperCase(Locale.ROOT)) {
            case "COMMON" -> "§f";
            case "UNCOMMON" -> "§a";
            case "RARE" -> "§9";
            case "EPIC" -> "§5";
            case "LEGENDARY" -> "§6";
            case "MYTHIC" -> "§d";
            case "DIVINE" -> "§b";
            case "SPECIAL", "VERY SPECIAL" -> "§c";
            default -> "§f";
        };
    }

    private LivingEntity buildSlayerEntity(HorizonSlayerBoss slayer) {
        if (client == null || client.world == null || slayer == null) {
            return null;
        }
        EntityType<?> type = switch ((slayer.id() == null ? "" : slayer.id()).toLowerCase(Locale.ROOT)) {
            case "zombie" -> EntityType.ZOMBIE;
            case "spider", "tarantula" -> EntityType.SPIDER;
            case "wolf", "sven" -> EntityType.WOLF;
            case "enderman" -> EntityType.ENDERMAN;
            case "blaze" -> EntityType.BLAZE;
            case "vampire" -> EntityType.ZOMBIE_VILLAGER;
            default -> null;
        };
        if (type == null) {
            return null;
        }
        try {
            Entity entity = type.create(client.world, SpawnReason.COMMAND);
            return entity instanceof LivingEntity livingEntity ? livingEntity : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private ItemStack fallbackSlayerIcon(HorizonSlayerBoss slayer) {
        String minecraftItemId = switch ((slayer == null || slayer.id() == null ? "" : slayer.id()).toLowerCase(Locale.ROOT)) {
            case "zombie" -> "minecraft:zombie_head";
            case "spider", "tarantula" -> "minecraft:spider_eye";
            case "wolf", "sven" -> "minecraft:bone";
            case "enderman" -> "minecraft:ender_pearl";
            case "blaze" -> "minecraft:blaze_rod";
            case "vampire" -> "minecraft:redstone";
            default -> "minecraft:paper";
        };
        return new ItemStack(resolveVanillaItem(minecraftItemId));
    }

    private GameProfile texturedGameProfile() {
        if (profile == null) {
            return null;
        }
        UUID uuid = null;
        try {
            if (profile.playerUuid() != null && !profile.playerUuid().isBlank()) {
                uuid = UUID.fromString(profile.playerUuid());
            }
        } catch (Exception ignored) {
            uuid = null;
        }
        return texturedGameProfile(uuid, profile.playerName().isBlank() ? displayRequestedPlayer() : profile.playerName());
    }

    private GameProfile texturedGameProfile(UUID uuid, String name) {
        if (profile == null) {
            return null;
        }
        GameProfile gameProfile = new GameProfile(uuid, name);
        if (!profile.playerSkinTexture().isBlank()) {
            var textures = LinkedHashMultimap.<String, Property>create();
            textures.put(
                "textures",
                profile.playerSkinTextureSignature().isBlank()
                    ? new Property("textures", profile.playerSkinTexture())
                    : new Property("textures", profile.playerSkinTexture(), profile.playerSkinTextureSignature())
            );
            gameProfile = new GameProfile(uuid, name, new PropertyMap(textures));
        }
        return gameProfile;
    }

    private void drawLargeItem(DrawContext context, ItemStack stack, int x, int y, float scale) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x, y);
        context.getMatrices().scale(scale, scale);
        context.drawItem(stack, 0, 0);
        context.getMatrices().popMatrix();
    }

    private Item resolveVanillaItem(String minecraftItemId) {
        if (minecraftItemId == null || minecraftItemId.isBlank()) {
            return Items.PAPER;
        }
        try {
            Item item = Registries.ITEM.get(Identifier.of(minecraftItemId));
            return item == Items.AIR ? Items.PAPER : item;
        } catch (Exception ignored) {
            return Items.PAPER;
        }
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
        DUNGEONS("Dungeons"),
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
