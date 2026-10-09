package sharkus.extras;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class Hud {
    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static boolean wasDown;

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static void tick(Minecraft mc) {
        if (mc.getWindow() == null) return;
        boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown) clicks.add(System.currentTimeMillis());
        wasDown = down;
        long now = System.currentTimeMillis();
        while (!clicks.isEmpty() && now - clicks.peek() > 1000) clicks.poll();
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("sharkus-extras", "hud"), Hud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Config c = Config.I;
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // Ziel, auf das das Fadenkreuz zeigt
        LivingEntity target = null;
        if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
            target = le;
        }

        // ---------- Textzeilen oben links ----------
        List<String> lines = new ArrayList<>();
        if (c.fps) lines.add("FPS: " + mc.getFps());
        if (c.cps) lines.add("CPS: " + clicks.size());
        if (c.ping && mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            lines.add("Ping: " + (info == null ? "?" : info.getLatency()) + " ms");
        }
        if (c.healthIndicator) {
            lines.add(String.format("HP: %.1f / %.1f", mc.player.getHealth(), mc.player.getMaxHealth()));
            if (target != null) {
                lines.add(String.format("%s: %.1f / %.1f", target.getName().getString(),
                        target.getHealth(), target.getMaxHealth()));
            }
        }
        if (c.shieldStatus) {
            ItemStack main = mc.player.getMainHandItem();
            ItemStack off = mc.player.getOffhandItem();
            ItemStack shield = main.is(Items.SHIELD) ? main : (off.is(Items.SHIELD) ? off : ItemStack.EMPTY);
            if (!shield.isEmpty()) {
                String state;
                if (mc.player.getCooldowns().isOnCooldown(shield)) state = "Cooldown";
                else if (mc.player.isBlocking()) state = "blockt";
                else state = "bereit";
                lines.add("Schild: " + state);
            }
        }
        int y = 4;
        for (String s : lines) {
            g.text(font, s, 4, y, 0xFFFFFFFF);
            y += 10;
        }

        // ---------- Armor Display ----------
        if (c.armorDisplay) {
            y += 4;
            for (EquipmentSlot slot : ARMOR) {
                ItemStack stack = mc.player.getItemBySlot(slot);
                if (stack.isEmpty()) continue;
                g.item(stack, 4, y);
                if (stack.isDamageableItem()) {
                    int left = stack.getMaxDamage() - stack.getDamageValue();
                    g.text(font, String.valueOf(left), 24, y + 4, 0xFFFFFFFF);
                }
                y += 18;
            }
        }

        // ---------- Crosshair Indicator ----------
        if (c.crosshairIndicator && target != null) {
            boolean ready = mc.player.getAttackStrengthScale(0f) >= 1.0f;
            int color = ready ? 0xFF30FF30 : 0xFFFF3030;
            int cx = sw / 2, cy = sh / 2;
            g.fill(cx + 8, cy - 2, cx + 12, cy + 2, color);
        }

        // ---------- Team Tracker (rechts oben) ----------
        if (c.teamTracker) {
            int ty = 4;
            for (Player p : mc.level.players()) {
                if (p == mc.player || p.getTeam() == null || !p.isAlliedTo(mc.player)) continue;
                String s = p.getName().getString() + "  " + (int) mc.player.distanceTo(p) + "m  "
                        + (int) p.getHealth() + " HP";
                g.text(font, s, sw - font.width(s) - 4, ty, 0xFF55FF55);
                ty += 10;
            }
        }
    }
}
