package dev.dtzudontsay.knownworld.client.atlas;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Full known-world map. No chunk loading and no server-side dependencies. */
public final class KnownWorldAtlasScreen extends Screen {
    private static final Identifier ATLAS = Identifier.fromNamespaceAndPath(
            "knownworld", "textures/gui/known_world_atlas.png");
    private static final int HEADER = 29;
    private static final int FOOTER = 55;
    private static final int PAD = 9;
    private static final int RED = 0xFFFF3434;
    private static final int SELECTED = 0xFFFFFF54;
    private static final int WHITE = 0xFFF6EEDB;
    private static final int FAINT = 0xFFB8B7B1;

    private final List<KnownWorldAtlasMarkers.Marker> markers = KnownWorldAtlasMarkers.all();
    private double centerX = KnownWorldAtlasConfig.MASTER_ORIGIN_X;
    private double centerY = KnownWorldAtlasConfig.MASTER_ORIGIN_Y;
    private double zoom = KnownWorldAtlasConfig.INITIAL_ZOOM;
    private String selectedId = "";
    private boolean showUnlinked = true;
    private boolean dragging;

    public KnownWorldAtlasScreen() {
        super(Component.literal("Known World Atlas"));
    }

    @Override
    protected void init() {
        // Player's position determines the initial *view*, not the map's available area.
        if (minecraft.player != null) {
            centerX = KnownWorldAtlasConfig.MASTER_ORIGIN_X
                    + minecraft.player.getX() / KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL;
            centerY = KnownWorldAtlasConfig.MASTER_ORIGIN_Y
                    + minecraft.player.getZ() / KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL;
        }
        centerX = clamp(centerX, 0, KnownWorldAtlasConfig.MAP_WIDTH);
        centerY = clamp(centerY, 0, KnownWorldAtlasConfig.MAP_HEIGHT);
        keepViewInBounds();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX,
                                   int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        gfx.fill(0, 0, width, height, 0xF70E161D);
        int left = PAD, top = HEADER;
        int right = width - PAD, bottom = height - FOOTER;
        gfx.fill(left, top, right, bottom, 0xFF10232E);

        double scale = pixelsPerMasterPixel();
        double drawLeft = viewMiddleX() - centerX * scale;
        double drawTop = viewMiddleY() - centerY * scale;

        gfx.enableScissor(left, top, right, bottom);
        // IMPORTANT: the destination size changes with zoom, but the sampled
        // source area must ALWAYS remain the whole 2048x1357 atlas texture.
        // The old 10-argument blit implicitly set srcWidth/srcHeight to the
        // zoomed destination width/height, causing the map image to drift
        // and/or tile while all pins continued to use the correct transform.
        // Minecraft 26.3: use the 12-argument overload specifying both sizes.
        gfx.blit(RenderPipelines.GUI_TEXTURED, ATLAS,
                (int)Math.round(drawLeft), (int)Math.round(drawTop),
                0.0F, 0.0F,
                Math.max(1, (int)Math.round(KnownWorldAtlasConfig.MAP_WIDTH * scale)),
                Math.max(1, (int)Math.round(KnownWorldAtlasConfig.MAP_HEIGHT * scale)),
                KnownWorldAtlasConfig.MAP_WIDTH, KnownWorldAtlasConfig.MAP_HEIGHT,
                KnownWorldAtlasConfig.MAP_WIDTH, KnownWorldAtlasConfig.MAP_HEIGHT);

        // All markers are pre-computed from the regional Xs. Draw regardless of exploration.
        for (KnownWorldAtlasMarkers.Marker mark : markers) {
            if (!showUnlinked && !mark.named()) continue;
            int px = (int)Math.round(drawLeft + mark.masterX() * scale);
            int py = (int)Math.round(drawTop + mark.masterY() * scale);
            if (px < left - 9 || px > right + 9 || py < top - 9 || py > bottom + 9) continue;
            int ink = mark.id().equals(selectedId) ? SELECTED :
                    mark.named() ? RED : 0xFFFF9756;
            gfx.horizontalLine(px - 4, px + 4, py, 0xFF101010);
            gfx.verticalLine(px, py - 4, py + 4, 0xFF101010);
            gfx.horizontalLine(px - 3, px + 3, py, ink);
            gfx.verticalLine(px, py - 3, py + 3, ink);
        }
        gfx.disableScissor();

        KnownWorldAtlasMarkers.Marker hover = nearestMarker(mouseX, mouseY);
        KnownWorldAtlasMarkers.Marker selected = findSelected();
        KnownWorldAtlasMarkers.Marker info = hover != null ? hover : selected;
        gfx.fill(0, 0, width, HEADER - 2, 0xF817242C);
        gfx.fill(0, height - FOOTER + 2, width, height, 0xF817242C);
        gfx.text(font, "KNOWN WORLD  •  298 AC", 12, 10, WHITE, true);
        gfx.text(font, "MAP  |  " + (int)Math.round(zoom * 100) + "%  |  " + markers.size()
                + " source places", 190, 10, FAINT, false);
        // Continuous map cursor readout: authoritative map projection, provisional terrain.
        // This shows the coordinate under the cursor, NOT the selected marker position.
        boolean overMap = mouseX >= left && mouseX < right
                && mouseY >= top && mouseY < bottom;
        double cursorMasterX = centerX + (mouseX - viewMiddleX()) / scale;
        double cursorMasterY = centerY + (mouseY - viewMiddleY()) / scale;
        boolean insideMaster = overMap
                && cursorMasterX >= 0 && cursorMasterX < KnownWorldAtlasConfig.MAP_WIDTH
                && cursorMasterY >= 0 && cursorMasterY < KnownWorldAtlasConfig.MAP_HEIGHT;

        if (insideMaster) {
            // The master map uses +Z toward image south (as Minecraft does).
            long cursorWorldX = Math.round((cursorMasterX
                    - KnownWorldAtlasConfig.MASTER_ORIGIN_X)
                    * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);
            long cursorWorldZ = Math.round((cursorMasterY
                    - KnownWorldAtlasConfig.MASTER_ORIGIN_Y)
                    * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);

            gfx.text(font, "CURSOR  X: " + cursorWorldX + "   Z: " + cursorWorldZ
                    + "   |   Y: not surveyed", 12, height - 45, WHITE, true);
            gfx.text(font, "Teleport command: /tp @s " + cursorWorldX
                    + " ~ " + cursorWorldZ, 12, height - 32, 0xFF72E9DE, false);

            // A tiny pointer crosshair marks exactly which map position was sampled.
            gfx.horizontalLine(mouseX - 3, mouseX + 3, mouseY, 0xFF72E9DE);
            gfx.verticalLine(mouseX, mouseY - 3, mouseY + 3, 0xFF72E9DE);
        } else {
            gfx.text(font, "CURSOR  Move over the map for Minecraft X/Z", 12,
                    height - 45, FAINT, false);
            gfx.text(font, "Teleport: X/Z only; terrain height must be checked in game",
                    12, height - 32, 0xFFFFB668, false);
        }
        gfx.text(font, "Wheel: zoom  |  Drag: pan  |  M/Esc: close  |  U: unlinked  |  F: full map",
                12, height - 17, FAINT, false);

        if (minecraft.player != null) {
            int px = (int)Math.round(drawLeft + (KnownWorldAtlasConfig.MASTER_ORIGIN_X
                    + minecraft.player.getX() / KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL) * scale);
            int py = (int)Math.round(drawTop + (KnownWorldAtlasConfig.MASTER_ORIGIN_Y
                    + minecraft.player.getZ() / KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL) * scale);
            if (px >= left && px <= right && py >= top && py <= bottom) {
                gfx.outline(px - 3, py - 3, 7, 7, 0xFF55F9EE);
            }
        }

        if (info != null) {
            int boxWidth = Math.min(340, Math.max(180, width - 20));
            int tx = Math.min(Math.max(mouseX + 12, 9), Math.max(9, width - boxWidth - 9));
            int ty = Math.min(Math.max(mouseY + 15, 30), Math.max(30, height - 134));
            int mx = (int)Math.round((info.masterX() - KnownWorldAtlasConfig.MASTER_ORIGIN_X)
                    * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);
            int mz = (int)Math.round((info.masterY() - KnownWorldAtlasConfig.MASTER_ORIGIN_Y)
                    * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);
            gfx.fill(tx, ty, tx + boxWidth, ty + 98, 0xF20B1924);
            gfx.outline(tx, ty, boxWidth, 98, 0xFFB6A37C);
            gfx.text(font, info.name(), tx + 8, ty + 8, WHITE, true);
            gfx.text(font, "Sources: " + info.sources(), tx + 8, ty + 23, FAINT, false);
            gfx.text(font, "Place key: " + info.placeId(), tx + 8, ty + 37, FAINT, false);
            gfx.text(font, "Game X/Z (unapproved): " + mx + ", " + mz,
                    tx + 8, ty + 51, FAINT, false);
            gfx.text(font, "Master: " + String.format(java.util.Locale.ROOT, "%.2f, %.2f",
                    info.masterX(), info.masterY()), tx + 8, ty + 65, FAINT, false);
            gfx.text(font, info.status(), tx + 8, ty + 79, 0xFFFFB668, false);
        }
    }

    private KnownWorldAtlasMarkers.Marker nearestMarker(double mouseX, double mouseY) {
        if (mouseX < PAD || mouseX > width - PAD ||
                mouseY < HEADER || mouseY > height - FOOTER) return null;
        double scale = pixelsPerMasterPixel();
        double sourceX = centerX + (mouseX - viewMiddleX()) / scale;
        double sourceY = centerY + (mouseY - viewMiddleY()) / scale;
        double maxDistanceSquared = Math.pow(9.0 / scale, 2);
        KnownWorldAtlasMarkers.Marker best = null;
        for (KnownWorldAtlasMarkers.Marker marker : markers) {
            if (!showUnlinked && !marker.named()) continue;
            double dx = marker.masterX() - sourceX;
            double dy = marker.masterY() - sourceY;
            double distance = dx * dx + dy * dy;
            if (distance < maxDistanceSquared) {
                maxDistanceSquared = distance;
                best = marker;
            }
        }
        return best;
    }

    private KnownWorldAtlasMarkers.Marker findSelected() {
        for (KnownWorldAtlasMarkers.Marker marker : markers) {
            if (marker.id().equals(selectedId)) return marker;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            KnownWorldAtlasMarkers.Marker marker = nearestMarker(event.x(), event.y());
            if (marker != null) selectedId = marker.id();
            dragging = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            centerX -= dx / pixelsPerMasterPixel();
            centerY -= dy / pixelsPerMasterPixel();
            keepViewInBounds();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double scrollX, double scrollY) {
        if (scrollY == 0) return false;
        double oldScale = pixelsPerMasterPixel();
        double sourceX = centerX + (mouseX - viewMiddleX()) / oldScale;
        double sourceY = centerY + (mouseY - viewMiddleY()) / oldScale;
        zoom = clamp(zoom * Math.pow(1.2, scrollY), 1.0,
                KnownWorldAtlasConfig.MAX_ZOOM);
        double newScale = pixelsPerMasterPixel();
        centerX = sourceX - (mouseX - viewMiddleX()) / newScale;
        centerY = sourceY - (mouseY - viewMiddleY()) / newScale;
        keepViewInBounds();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_M || event.isEscape()) {
            onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_F) {
            zoom = 1.0;
            centerX = KnownWorldAtlasConfig.MASTER_ORIGIN_X;
            centerY = KnownWorldAtlasConfig.MASTER_ORIGIN_Y;
            return true;
        }
        if (event.key() == InputConstants.KEY_U) {
            showUnlinked = !showUnlinked;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() { minecraft.gui.setScreen(null); }

    private double pixelsPerMasterPixel() {
        double availableX = Math.max(1.0, width - PAD * 2.0);
        double availableY = Math.max(1.0, height - HEADER - FOOTER);
        return zoom * Math.min(availableX / KnownWorldAtlasConfig.MAP_WIDTH,
                availableY / KnownWorldAtlasConfig.MAP_HEIGHT);
    }

    private double viewMiddleX() { return width * 0.5; }
    private double viewMiddleY() { return (HEADER + (height - FOOTER)) * 0.5; }

    private void keepViewInBounds() {
        double scale = pixelsPerMasterPixel();
        double radiusX = Math.max(0, (width - PAD * 2.0) * 0.5 / scale);
        double radiusY = Math.max(0, (height - HEADER - FOOTER) * 0.5 / scale);
        centerX = radiusX >= KnownWorldAtlasConfig.MAP_WIDTH / 2.0
                ? KnownWorldAtlasConfig.MAP_WIDTH / 2.0
                : clamp(centerX, radiusX, KnownWorldAtlasConfig.MAP_WIDTH - radiusX);
        centerY = radiusY >= KnownWorldAtlasConfig.MAP_HEIGHT / 2.0
                ? KnownWorldAtlasConfig.MAP_HEIGHT / 2.0
                : clamp(centerY, radiusY, KnownWorldAtlasConfig.MAP_HEIGHT - radiusY);
    }

    private static double clamp(double n, double lo, double hi) {
        return Math.min(hi, Math.max(lo, n));
    }
}
