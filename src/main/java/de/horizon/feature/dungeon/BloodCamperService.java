package de.horizon.feature.dungeon;

import de.horizon.feature.dungeon.puzzle.DungeonRenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.authlib.properties.Property;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Blood Camper helper — predicts blood mob spawn destinations.
 *
 * Uses velocity-based prediction: tracks actual movement direction of mobs
 * rather than watcher→mob direction. Only shows boxes for actively moving mobs.
 * Wall skulls (static ArmorStands) are filtered out automatically.
 */
public final class BloodCamperService {
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-or]");

    private static final int COLOR_SPAWN    = 0xAAFF0000;  // red — predicted spawn point
    private static final int COLOR_POSITION = 0xAA55FF55;  // green — current mob position
    private static final int COLOR_MERGED   = 0xAA00AAAA;  // dark aqua — when position reaches spawn
    private static final int COLOR_LINE     = 0xAAFF4444;

    private static final double VELOCITY_THRESHOLD = 0.05; // min blocks/tick to count as moving
    private static final int STATIC_TIMEOUT_TICKS = 30;    // remove if never moved after 30 ticks

    // Known blood mob skull textures — base64 texture values
    private static final Set<String> ALLOWED_MOB_SKULLS = Set.of(
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDEwNjQwNTAsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVhNzk4NjBhY2E3OTk0MDdjMGZhYTEwYjFiYmNmNDI5OThmYWQ0ZWJjZjMxZDdhMjE0MTgwODI2YjRhYzk0ZTEifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDExODY2MzYsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ3NzQ4NzExOTBjODc4YzlhMmM0NDk2YzFlMTAyNTdjNmM0ZWExMzgwN2Q3MmMxNWQ3YWM2YWIzYTdhOWE4ZGMifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDAyMDM1NzMsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y0NjI0YTlhOGM2OWNhMjA0NTA0YWJiMDQzZDQ3NDU2Y2Q5YjA5NzQ5YTM2MzU3NDYyMzAzZjI3NmEyMjlkNCJ9fX0=",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDExNDUyMjIsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M5MTllNWI4ZDU2ZjA2MmEyMWQyMjRkZTE0YWY3NzFlMmY1NWQwOWI1OWU3YjA5OWQwOWRhYTU3NTQwYjc5Y2YiLCJtZXRhZGF0YSI6eyJtb2RlbCI6InNsaW0ifX19fQ==",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA1MzgzODIsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2E4OWY2MzAzYWY4NTg3NzYxMDkxMmRjMDRiOGIxZTg5NzI0NzUyZjBhN2VlYTA1YWI2NTQ3ZTIyODE3OWMwNmYiLCJtZXRhZGF0YSI6eyJtb2RlbCI6InNsaW0ifX19fQ==",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA5ODk1NTgsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzY3MjM3ZWRkYWViZGJiZGFhY2ZhOTEyODg1NTYwY2NkYzY1ZGE5M2I0YzNkNTEzNTMyODY4ZWMyM2JiNWI0NDgifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA0OTUwMjgsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2ZmMTg0YzE5ZTcyNTYyM2QzMjgyOGEwYTRlNzQxZTg2ZjEzNWFjNjNkYmM4MjhmZjNjODQ2ODMzOGYzNjgzYiJ9fX0=",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDEwMzA3NjUsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVjY2NkNTNmNTE5MWMyOWE5ZGM4ZjAxNzBmYmRjNGU1OWU2NjQ3NmFhZTMzZGUyN2I0NjhmMWRlMWI3Y2YzYjIifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA5MTc4NzYsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2I1YmE3NmUwMmNhYjcyZmE3ZDhhYzU0Y2VlYzg0OTk3NmFiMGIwMGEwMTA2OGQ2OGMyNjY3NjZiZjcwYzM5OTcifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA3Njk2MTQsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2FhMjNjOGNkZTI5NDNjODQyNDlkZTgzNTFiYzM1NDBiZTVmOGFmYWFiYThiMmNiMDMyZmM1YWNhZDc4YTI2OWIifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA4MTg4MDMsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzkxNzFmMzViOGY1MDgxNDJiZDhjNjU0MTdkMGYzMjQxNTNhYjkxNDc3MzllZTRkMTBkZWE3MzNjYzgwZWFhMjAifX19",
        "eyJ0aW1lc3RhbXAiOjE1ODYwNDA5NTY0MjIsInByb2ZpbGVJZCI6ImRhNDk4YWM0ZTkzNzRlNWNiNjEyN2IzODA4NTU3OTgzIiwicHJvZmlsZU5hbWUiOiJOaXRyb2hvbGljXzIiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdkMTJiMmFkZTQxM2E2Y2Q3Y2NhM2M5NWU5NjFiYTlmMGFlNzE2NWZhNDFmYzdiNWQ1ZjA5NGEwMTI0MGM2MDkifX19",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTZjM2UzMWNmYzY2NzMzMjc1YzQyZmNmYjVkOWE0NDM0MmQ2NDNiNTVjZDE0YzljNzdkMjczYTIzNTIifX19",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzE2OTIxMSwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODQyMWJhNWI4ZTM1NzNlZjk3YmViNWI0MGUxNWQxNWIyMGYzMDYzMWM0YzUzMzBjM2RlZGEzMDQ3ZGYwZTkyIgogICAgfQogIH0KfQ==",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzExMjUwMCwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQyMjc3MmY3NjkwNDVmZGM1YmU4MTlhZDY4YjAxYTk3YWMwNGM2MDg4NmQyY2E3YWZlZTM5YjI4MmY3YTM4MyIKICAgIH0KICB9Cn0=",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzM4Njc5NCwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQ2N2Y5N2Q3ZjgyMTcyOWJlYjM0YTgyYzNmMTM1OTJiNDA0MzlmZTUyNDhlNzI1NzZmZGU3YWExODBiZjc3IgogICAgfQogIH0KfQ==",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzIxNTkwNSwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmIzOTczYTc1MmIyNGEyZjNhYmIwMDM0MjdmNmRiZTZjYTNhNjFkYjBhMWJjZjM1MWM2ZWFiMjdlYzI3ZTUwIgogICAgfQogIH0KfQ==",
        "eyJ0aW1lc3RhbXAiOjE1NzQ0MTkzMTAxNjQsInByb2ZpbGVJZCI6Ijc1MTQ0NDgxOTFlNjQ1NDY4Yzk3MzlhNmUzOTU3YmViIiwicHJvZmlsZU5hbWUiOiJUaGFua3NNb2phbmciLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzEyNzE2ZWNiZjViOGRhMDBiMDVmMzE2ZWM2YWY2MWU4YmQwMjgwNWIyMWViOGU0NDAxNTE0NjhkYzY1NjU0OWMifX19",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTkyMzAyODAxNSwKICAicHJvZmlsZUlkIiA6ICJhMmY4MzQ1OTVjODk0YTI3YWRkMzA0OTcxNmNhOTEwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJiUHVuY2giLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI2MDMyNTE3MWE3YmE4NDYwODMwYzBlZWE1MTVjNzU3YTY2NWU1YjE2YTE0MjA3YmExYTMxODI3NTJiZWU4NyIKICAgIH0KICB9Cn0=",
        "ewogICJ0aW1lc3RhbXAiIDogMTU5NTQyODIyMDAyMCwKICAicHJvZmlsZUlkIiA6ICJkYTQ5OGFjNGU5Mzc0ZTVjYjYxMjdiMzgwODU1Nzk4MyIsCiAgInByb2ZpbGVOYW1lIiA6ICJOaXRyb2hvbGljXzIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjJkOGZkM2FhNTYxN2IxZGFjMGFhZTljODFmNmRkNzBhZDkzYTU5OTQyZjQ2MGQyN2U0ZDU1YTVjYjg5MThlOCIKICAgIH0KICB9Cn0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTZmYzg1NGJiODRjZjRiNzY5NzI5Nzk3M2UwMmI3OWJjMTA2OTg0NjBiNTFhNjM5YzYwZTVlNDE3NzM0ZTExIn19fQ==",
        "ewogICJ0aW1lc3RhbXAiIDogMTU4OTc5MzA2ODgzOSwKICAicHJvZmlsZUlkIiA6ICIyYzEwNjRmY2Q5MTc0MjgyODRlM2JmN2ZhYTdlM2UxYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOYWVtZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83ZGUyYmJiZGYyMmJmZTE3OTgwZDRlMjA2ODdlMzg2ZjExZDU5ZWUxZGI2ZjhiNDc2MjM5MWI3OWE1YWM1MzJkIgogICAgfQogIH0KfQ==",
        "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzI1OTM1NywKICAicHJvZmlsZUlkIiA6ICJlNzkzYjJjYTdhMmY0MTI2YTA5ODA5MmQ3Yzk5NDE3YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaGVfSG9zdGVyX01hbiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jMTAwN2M1YjcxMTRhYmVjNzM0MjA2ZDRmYzYxM2RhNGYzYTBlOTlmNzFmZjk0OWNlZGFkYzk5MDc5MTM1YTBiIgogICAgfQogIH0KfQ=="
    );

    // Watcher greeting patterns (blood room start)
    private static final Pattern BLOOD_START_REGEX = Pattern.compile(
        "^\\[BOSS] The Watcher: (" +
            "Congratulations, you made it through the Entrance\\.|" +
            "Ah, you've finally arrived\\.|" +
            "Ah, we meet again\\.\\.\\.|" +
            "So you made it this far\\.\\.\\. interesting\\.|" +
            "You've managed to scratch and claw your way here, eh\\?|" +
            "I'm starting to get tired of seeing you around here\\.\\.\\.|" +
            "Oh\\.\\.\\. hello\\?|" +
            "Things feel a little more roomy now, eh\\?" +
        ")$"
    );
    private static final Pattern BLOOD_MOVE_REGEX = Pattern.compile(
        "^\\[BOSS] The Watcher: Let's see how you can handle this\\.$"
    );
    private static final Pattern BLOOD_DONE_REGEX = Pattern.compile(
        "^\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.$",
        Pattern.CASE_INSENSITIVE
    );

    private boolean bloodActive = false;
    private boolean bloodDone = false;
    private boolean firstSpawns = true;
    private long bloodStartTick = 0;
    private long currentTick = 0;
    private Float killTimerSeconds = null;

    private int watcherEntityId = -1;

    private final Map<Integer, MobTracker> trackedMobs = new LinkedHashMap<>();

    private static final class MobTracker {
        final long startedTick;
        Vec3 prevPos;
        Vec3 velocity;        // smoothed velocity (blocks/tick)
        Vec3 predictedEndpoint;
        boolean isMoving;
        boolean everMoved;

        MobTracker(Vec3 startPos, long startedTick) {
            this.startedTick = startedTick;
            this.prevPos = startPos;
            this.velocity = Vec3.ZERO;
            this.predictedEndpoint = null;
            this.isMoving = false;
            this.everMoved = false;
        }

        void update(Vec3 currentPos, Minecraft mc) {
            Vec3 delta = currentPos.subtract(prevPos);
            double speed = delta.length();

            if (speed > VELOCITY_THRESHOLD) {
                // Smooth velocity with EMA
                if (velocity.lengthSqr() < 0.001) {
                    velocity = delta;
                } else {
                    velocity = velocity.scale(0.6).add(delta.scale(0.4));
                }
                isMoving = true;
                everMoved = true;

                // Predict endpoint: raycast along velocity direction
                Vec3 dir = velocity.normalize();
                double maxDist = 16.0;
                Vec3 best = currentPos.add(dir.scale(maxDist));

                // Step along path, stop at first solid block
                if (mc.level != null) {
                    for (double d = 1.0; d <= maxDist; d += 0.5) {
                        Vec3 check = currentPos.add(dir.scale(d));
                        BlockPos bp = BlockPos.containing(check.x, check.y, check.z);
                        BlockState state = mc.level.getBlockState(bp);
                        if (!state.isAir() && !state.liquid()) {
                            best = currentPos.add(dir.scale(Math.max(0, d - 1.0)));
                            break;
                        }
                    }
                }
                predictedEndpoint = best;
            } else if (isMoving && speed < 0.02) {
                // Mob stopped — use current position as endpoint
                isMoving = false;
                predictedEndpoint = currentPos;
            }

            prevPos = currentPos;
        }
    }

    public void handleChatMessage(String rawMessage, boolean enabled) {
        if (!enabled || rawMessage == null) return;
        String plain = FORMATTING.matcher(rawMessage).replaceAll("").strip();

        if (BLOOD_START_REGEX.matcher(plain).matches()) {
            bloodActive = true;
            bloodDone = false;
            firstSpawns = true;
            bloodStartTick = currentTick;
            watcherEntityId = -1;
            trackedMobs.clear();
            killTimerSeconds = null;
            return;
        }

        if (!bloodActive) return;

        if (BLOOD_MOVE_REGEX.matcher(plain).matches()) {
            firstSpawns = false;

            long elapsedTicks = (currentTick - bloodStartTick);
            long moveTicks = elapsedTicks / 20;
            int predTicks;
            if (moveTicks >= 31 && moveTicks < 34) predTicks = 36;
            else if (moveTicks >= 28 && moveTicks < 31) predTicks = 33;
            else if (moveTicks >= 25 && moveTicks < 28) predTicks = 30;
            else if (moveTicks >= 22 && moveTicks < 25) predTicks = 27;
            else if (moveTicks >= 1 && moveTicks < 22) predTicks = 24;
            else predTicks = (int) moveTicks + 3;

            killTimerSeconds = predTicks / 20f;
            return;
        }

        if (BLOOD_DONE_REGEX.matcher(plain).matches()) {
            bloodDone = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gui != null) {
                float elapsed = (currentTick - bloodStartTick) * 50 / 1000f;
                String time = String.format("%.1fs", elapsed);
                mc.gui.setTitle(Component.literal("Blood Done!").withStyle(ChatFormatting.RED));
                mc.gui.setSubtitle(Component.literal(time).withStyle(ChatFormatting.GOLD));
                mc.gui.setTimes(5, 40, 10);
            }
        }
    }

    public void tick(Minecraft mc) {
        currentTick++;
        if (!bloodActive || bloodDone || mc == null || mc.level == null || mc.player == null) return;

        // Decrement kill timer
        if (killTimerSeconds != null) {
            killTimerSeconds -= 0.05f;
            if (killTimerSeconds <= 0) {
                if (mc.gui != null) {
                    mc.gui.setTitle(Component.literal("Kill Mobs!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    mc.gui.setSubtitle(Component.empty());
                    mc.gui.setTimes(5, 40, 10);
                }
                killTimerSeconds = null;
            }
        }

        // Find Watcher entity
        if (watcherEntityId == -1) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (isWatcher(e)) {
                    watcherEntityId = e.getId();
                    break;
                }
            }
        }

        if (watcherEntityId != -1) {
            Entity watcher = mc.level.getEntity(watcherEntityId);
            if (watcher == null || !watcher.isAlive()) {
                watcherEntityId = -1;
            }
        }

        if (watcherEntityId == -1) return;

        Entity watcher = mc.level.getEntity(watcherEntityId);
        if (watcher == null) return;

        // Scan for ArmorStands with blood mob skull textures near Watcher
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand as)) continue;
            int id = e.getId();
            if (trackedMobs.containsKey(id)) continue;
            if (e.distanceTo(watcher) > 20) continue;
            if (!isBloodMobSkull(as)) continue;
            trackedMobs.put(id, new MobTracker(e.position(), currentTick));
        }

        // Update tracked mobs
        trackedMobs.entrySet().removeIf(entry -> {
            Entity e = mc.level.getEntity(entry.getKey());
            if (e == null || !e.isAlive()) return true;
            MobTracker tracker = entry.getValue();
            tracker.update(e.position(), mc);
            // Remove static entities after timeout (wall decorations)
            if (!tracker.everMoved && (currentTick - tracker.startedTick) > STATIC_TIMEOUT_TICKS) return true;
            return false;
        });
    }

    private static boolean isBloodMobSkull(ArmorStand as) {
        ItemStack helmet = as.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || helmet.getItem() != Items.PLAYER_HEAD) return false;

        if (!(helmet.get(DataComponents.PROFILE) instanceof ResolvableProfile profile)) return false;

        var textures = profile.partialProfile().properties().get("textures");
        if (textures == null || textures.isEmpty()) return false;

        for (Property prop : textures) {
            if (ALLOWED_MOB_SKULLS.contains(prop.value())) return true;
        }
        return false;
    }

    private static boolean isWatcher(Entity e) {
        if (!(e instanceof ArmorStand as)) return false;
        String name = as.getDisplayName() != null ? as.getDisplayName().getString() : "";
        String custom = as.getCustomName() != null ? as.getCustomName().getString() : "";
        String combined = FORMATTING.matcher(name + " " + custom).replaceAll("").strip();
        return combined.contains("The Watcher");
    }

    public void renderWorld(LevelRenderContext ctx, Minecraft mc, boolean enabled) {
        if (!enabled || !bloodActive || mc.level == null || mc.player == null) return;

        for (Map.Entry<Integer, MobTracker> entry : trackedMobs.entrySet()) {
            Entity e = mc.level.getEntity(entry.getKey());
            if (e == null || !e.isAlive()) continue;

            MobTracker tracker = entry.getValue();
            // Only render for mobs that are currently moving
            if (!tracker.isMoving) continue;
            if (tracker.predictedEndpoint == null) continue;

            Vec3 curr = tracker.prevPos;
            Vec3 endpoint = tracker.predictedEndpoint;

            boolean merged = curr.distanceTo(endpoint) < 1.5;

            double s = 0.3;
            AABB endBox = new AABB(
                endpoint.x - s, endpoint.y + 1.0 - s, endpoint.z - s,
                endpoint.x + s, endpoint.y + 1.0 + s, endpoint.z + s
            );

            if (merged) {
                DungeonRenderUtil.drawBox(ctx, endBox, COLOR_MERGED, 1, false);
            } else {
                DungeonRenderUtil.drawBox(ctx, endBox, COLOR_SPAWN, 1, false);
                AABB posBox = new AABB(
                    curr.x - s, curr.y + 1.0 - s, curr.z - s,
                    curr.x + s, curr.y + 1.0 + s, curr.z + s
                );
                DungeonRenderUtil.drawBox(ctx, posBox, COLOR_POSITION, 1, false);
            }

            Vec3 lineStart = new Vec3(curr.x, curr.y + 2.0, curr.z);
            Vec3 lineEnd = new Vec3(endpoint.x, endpoint.y + 2.0, endpoint.z);
            DungeonRenderUtil.drawLine(ctx, List.of(lineStart, lineEnd), COLOR_LINE, false);
        }
    }

    public boolean isBloodActive() { return bloodActive && !bloodDone; }
    public boolean isBloodDone() { return bloodDone; }

    public float getElapsedSeconds() {
        if (!bloodActive) return 0;
        return (currentTick - bloodStartTick) * 50 / 1000f;
    }

    public String getStatusText() {
        if (!bloodActive) return "";
        if (bloodDone) return String.format("Blood Done (%.1fs)", getElapsedSeconds());
        return String.format("Blood (%.1fs)", getElapsedSeconds());
    }

    public void reset() {
        bloodActive = false;
        bloodDone = false;
        firstSpawns = true;
        bloodStartTick = 0;
        watcherEntityId = -1;
        trackedMobs.clear();
        killTimerSeconds = null;
    }
}
