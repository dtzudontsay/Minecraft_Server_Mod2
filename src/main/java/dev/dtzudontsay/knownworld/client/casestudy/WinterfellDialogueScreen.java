package dev.dtzudontsay.knownworld.client.casestudy;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellConversationPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated K-key dialogue view. Deliberately does not replace, instantiate or
 * modify the existing WinterfellCharacterScreen opened by right-click.
 */
public final class WinterfellDialogueScreen extends Screen {
    private static final int GOLD = 0xFFE1BC75;
    private static final int LIGHT = 0xFFE5DBC4;
    private static final int DIM = 0xFFB1A79A;
    private static final int PLAYER = 0xFFB9D7C2;
    private static final int PANEL = 0xFF1C2529;

    private record ChatLine(String speaker, String text) { }
    private record DrawLine(String text, int color) { }

    private final String session;
    private final String npcName;
    private final String subtitle;
    private final int entityId;
    private final List<ChatLine> transcript = new ArrayList<>();
    private EditBox input;
    private Button sendButton;
    private String status = "Awaiting your words.";
    private boolean busy;
    /** Zero pins the latest exchange to the bottom; positive values scroll upward. */
    private int scrollLines;

    public WinterfellDialogueScreen(String session, String npcName, String subtitle, int entityId) {
        super(Component.literal("Speak with " + npcName));
        this.session = session;
        this.npcName = npcName;
        this.subtitle = subtitle;
        this.entityId = entityId;
        transcript.add(new ChatLine("system", "You have approached " + npcName + "."));
    }

    public boolean hasSession(String token) { return session.equals(token); }

    public void addLine(String speaker, String text) {
        if (text == null || text.isBlank()) return;
        transcript.add(new ChatLine(speaker, text));
        while (transcript.size() > 70) transcript.removeFirst();
        scrollLines = 0;
    }

    public void updateStatus(String next, boolean isBusy) {
        this.status = next;
        this.busy = isBusy;
        if (sendButton != null) sendButton.active = !busy;
    }

    private int panelW() { return Math.min(width - 22, 920); }
    private int panelH() { return Math.min(height - 18, 510); }
    private int left() { return (width - panelW()) / 2; }
    private int top() { return (height - panelH()) / 2; }
    private int portraitW() { return Math.min(220, Math.max(135, panelW() / 4)); }
    private int conversationX() { return left() + portraitW() + 20; }
    private int conversationW() { return left() + panelW() - conversationX() - 14; }

    @Override
    protected void init() {
        super.init();
        String draft = input == null ? "" : input.getValue();
        int x = conversationX(), y = top(), w = conversationW(), h = panelH();
        int inputY = y + h - 60;

        input = new EditBox(font, x + 4, inputY, Math.max(35, w - 77), 20,
                Component.literal("Say something"));
        input.setMaxLength(400);
        input.setValue(draft);
        addRenderableWidget(input);

        sendButton = addRenderableWidget(Button.builder(Component.literal("Say"), b -> send())
                .bounds(x + w - 69, inputY, 65, 20).build());
        sendButton.active = !busy;
        addRenderableWidget(Button.builder(Component.literal("X"), b -> onClose())
                .bounds(left() + panelW() - 28, top() + 9, 18, 18).build());
        setInitialFocus(input);
    }

    private void send() {
        if (busy || input == null) return;
        String words = input.getValue().trim();
        if (words.isBlank()) return;
        ClientPlayNetworking.send(new WinterfellConversationPackets.ChatRequest(session, words));
        input.setValue("");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float tick) {
        int x = left(), y = top(), w = panelW(), h = panelH(), port = portraitW();
        int cx = conversationX(), cw = conversationW();
        gfx.fill(0, 0, width, height, 0xE90A1019);
        gfx.fill(x, y, x + w, y + h, PANEL);
        gfx.outline(x, y, w, h, 0xFFC8A86D);
        gfx.fill(x + 2, y + 2, x + w - 2, y + 49, 0xFF303936);
        gfx.text(font, "WINTERFELL  /  CONVERSATION", x + 14, y + 10, GOLD, true);
        gfx.text(font, npcName, x + 14, y + 29, LIGHT, true);

        // Show the actual selected Minecraft villager, enlarged and reactive to
        // mouse movement, instead of inventing an illustrated likeness.
        int px = x + 11, py = y + 62, pw = port - 10, ph = h - 78;
        gfx.fill(px, py, px + pw, py + ph, 0xFF232F32);
        gfx.outline(px, py, pw, ph, 0xFF786B55);
        Entity target = minecraft == null || minecraft.level == null
                ? null : minecraft.level.getEntity(entityId);
        if (target instanceof LivingEntity living) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    gfx, px + 6, py + 8, px + pw - 6, py + ph - 56,
                    Math.min(108, Math.max(50, ph / 3)), 0.0F,
                    (float) mouseX, (float) mouseY, living);
        } else {
            // Fallback for a projection despawned while this screen is open.
            int mx = px + pw / 2;
            int headW = Math.max(30, pw / 3);
            int headH = Math.min(83, Math.max(45, ph / 4));
            int headY = py + Math.max(34, ph / 5);
            gfx.fill(mx - headW / 2, headY, mx + headW / 2,
                    headY + headH, 0xFF62625C);
            gfx.fill(px + 23, headY + headH + 8, px + pw - 23,
                    Math.min(py + ph - 62, headY + headH + 120), 0xFF494D4B);
        }
        gfx.text(font, npcName, px + 9, py + ph - 47, GOLD, true);
        List<String> sub = wrap(subtitle, Math.max(30, pw - 18));
        for (int i = 0; i < Math.min(3, sub.size()); i++) {
            gfx.text(font, sub.get(i), px + 9, py + ph - 33 + i * 10, DIM, false);
        }

        int textTop = y + 61, textBottom = y + h - 84;
        gfx.fill(cx, textTop, cx + cw, textBottom, 0xFF172024);
        gfx.outline(cx, textTop, cw, textBottom - textTop, 0xFF51534C);

        List<DrawLine> visibleRows = new ArrayList<>();
        for (ChatLine message : transcript) {
            int color = "npc".equals(message.speaker()) ? LIGHT
                    : "player".equals(message.speaker()) ? PLAYER : DIM;
            String prefix = "npc".equals(message.speaker()) ? npcName + ": "
                    : "player".equals(message.speaker()) ? "You: " : "";
            List<String> segments = wrap(prefix + message.text().replace('\r', ' '),
                    Math.max(45, cw - 19));
            for (String segment : segments) visibleRows.add(new DrawLine(segment, color));
            visibleRows.add(new DrawLine("", color));
        }
        int lineH = 13;
        int maximum = Math.max(0, (textBottom - textTop - 13) / lineH);
        int maxScroll = Math.max(0, visibleRows.size() - maximum);
        scrollLines = Math.max(0, Math.min(maxScroll, scrollLines));
        int start = Math.max(0, visibleRows.size() - maximum - scrollLines);
        gfx.enableScissor(cx + 6, textTop + 5, cx + cw - 6, textBottom - 3);
        for (int i = start; i < Math.min(visibleRows.size(), start + maximum); i++) {
            DrawLine row = visibleRows.get(i);
            gfx.text(font, row.text(), cx + 10, textTop + 8 + (i - start) * lineH,
                    row.color(), false);
        }
        gfx.disableScissor();
        gfx.text(font, status, cx + 5, y + h - 77, DIM, false);
        gfx.text(font, "Enter / Say: speak  •  Mouse wheel: history  •  Esc / K: leave",
                cx + 5, y + h - 30, DIM, false);
        super.extractRenderState(gfx, mouseX, mouseY, tick);
    }

    /** Use pixel widths to wrap without relying on version-dependent text layout types. */
    private List<String> wrap(String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                if (word.isEmpty()) continue;
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                    result.add(line.toString());
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
            if (!line.isEmpty()) result.add(line.toString());
        }
        if (result.isEmpty()) result.add("");
        return result;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        scrollLines = Math.max(0, scrollLines + (int) Math.signum(dy) * 3);
        return true;
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN
                || event.key() == InputConstants.KEY_NUMPADENTER) {
            send();
            return true;
        }
        if (event.key() == InputConstants.KEY_K) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean isPauseScreen() { return false; }
}
