#!/usr/bin/env python3
"""Cumulative installer: V3 chunk-worldgen + Stage3 NPC UI + atlas context teleport.
Patch only known anchors, back up the originals, and refuse unexpected layouts.
Run after extracting the cumulative ZIP into the repo root. Idempotent.
"""
from pathlib import Path
import sys
import importlib.util

ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/'src/main/java/dev/dtzudontsay/knownworld'

# Load original Stage 1+2 installer from the supplied cumulative ZIP.
original=ROOT/'tools/geodata/install_winterfell_v3_worldgen.py'
if not original.exists():sys.exit('Missing V3 worldgen installer. Extract the full ZIP first.')
spec=importlib.util.spec_from_file_location('wf_v3_original_installer',original)
mod=importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
mod.main()

def patch(rel, substitutions):
    path=ROOT/rel
    if not path.exists():sys.exit(f'Missing expected repository file: {rel}')
    initial=path.read_text(encoding='utf8')
    content=initial
    for before,after in substitutions:
        if after in content:continue
        if content.count(before)!=1:
            sys.exit(f'Unexpected source layout in {rel}: required anchor not uniquely present: {before[:110]!r}')
        content=content.replace(before,after,1)
    if content!=initial:
        backup=path.with_suffix(path.suffix+'.before-v3-stage3')
        if not backup.exists():backup.write_text(initial,encoding='utf8')
        path.write_text(content,encoding='utf8')
        print(f'Installed Stage3: {rel}')
    else:print(f'Already Stage3: {rel}')

entry='src/main/java/dev/dtzudontsay/knownworld/KnownWorld.java'
patch(entry,[
    ('import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellV2;',
     'import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellV2;\n'
     'import dev.dtzudontsay.knownworld.simulation.casestudy.WinterfellV3Runtime;\n'
     'import dev.dtzudontsay.knownworld.simulation.casestudy.KnownWorldUiPackets;'),
    ('        // Read-only interaction with tagged case-study proxy villagers.\n'
     '        // The handler registers no new simulation identities and is independently\n'
     '        // disabled by WinterfellCaseStudyInteraction.ENABLED = false.\n'
     '        WinterfellCaseStudyInteraction.register();\n\n'
     '        // Opt-in 5000-resident Winterfell test-world village, home and NPC vertical slice.\n'
     '        WinterfellV2.register();',
     '        // V3: worldgen alone constructs blocks; V3Runtime materializes authoritative NPCs.\n'
     '        // The two old V2/preview handlers are deliberately NOT registered.\n'
     '        KnownWorldUiPackets.registerCommon();\n'
     '        WinterfellV3Runtime.register();')
])
client='src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasClient.java'
patch(client,[
    ('import net.minecraft.resources.Identifier;',
     'import net.minecraft.resources.Identifier;\n'
     'import dev.dtzudontsay.knownworld.client.casestudy.WinterfellClientUi;'),
    ('        if (!KnownWorldAtlasConfig.ENABLED) return;',
     '        if (!KnownWorldAtlasConfig.ENABLED) return;\n\n'
     '        WinterfellClientUi.register();')
])

atlas='src/main/java/dev/dtzudontsay/knownworld/client/atlas/KnownWorldAtlasScreen.java'
fields='''    // Right-click any image pixel to open this explicit, permission-gated teleport menu.
    private boolean teleportMenu;
    private int teleportTargetX, teleportTargetZ;
    private int teleportMenuX, teleportMenuY;
    private static final int TELEPORT_MENU_W=196, TELEPORT_MENU_H=70;
'''
painting='''        // Draw after marker tooltips so this button stays visible and clickable.
        if (teleportMenu) {
            int bx=teleportMenuX, by=teleportMenuY;
            gfx.fill(bx,by,bx+TELEPORT_MENU_W,by+TELEPORT_MENU_H,0xF51B2730);
            gfx.outline(bx,by,TELEPORT_MENU_W,TELEPORT_MENU_H,0xFFE6BF77);
            gfx.text(font,"Teleport to X="+teleportTargetX, bx+9,by+9,0xFFF7E8CD,false);
            gfx.text(font,"Z="+teleportTargetZ+" ?",bx+9,by+22,0xFFF7E8CD,false);
            gfx.fill(bx+9,by+40,bx+TELEPORT_MENU_W-9,by+61,0xFF526849);
            gfx.text(font,"TELEPORT  (Creative / OP)",bx+16,by+46,0xFFFFFFFF,false);
        }
'''
click='''    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // The popup catches left clicks before the marker selector/pan handler.
        if (teleportMenu) {
            if (event.button()==InputConstants.MOUSE_BUTTON_LEFT) {
                boolean inside = event.x() >= teleportMenuX+9
                        && event.x() < teleportMenuX+TELEPORT_MENU_W-9
                        && event.y() >= teleportMenuY+40 && event.y() < teleportMenuY+61;
                if (inside) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            new dev.dtzudontsay.knownworld.simulation.casestudy.KnownWorldUiPackets.AtlasTeleportRequest(
                                    teleportTargetX,teleportTargetZ));
                    teleportMenu=false;
                    onClose();
                } else teleportMenu=false;
                return true;
            }
            teleportMenu=false;
        }
        if (event.button()==InputConstants.MOUSE_BUTTON_RIGHT) {
            double px=event.x(), pz=event.y();
            if (px >= PAD && px < width-PAD && pz >= HEADER && pz < height-FOOTER) {
                double scale=pixelsPerMasterPixel();
                double mx=centerX+(px-viewMiddleX())/scale;
                double mz=centerY+(pz-viewMiddleY())/scale;
                if(mx>=0 && mx<KnownWorldAtlasConfig.MAP_WIDTH && mz>=0 && mz<KnownWorldAtlasConfig.MAP_HEIGHT) {
                    teleportTargetX=(int)Math.round((mx-KnownWorldAtlasConfig.MASTER_ORIGIN_X)
                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);
                    teleportTargetZ=(int)Math.round((mz-KnownWorldAtlasConfig.MASTER_ORIGIN_Y)
                            * KnownWorldAtlasConfig.METRES_PER_MASTER_PIXEL);
                    teleportMenuX=(int)Math.max(10,Math.min(width-TELEPORT_MENU_W-10,px));
                    teleportMenuY=(int)Math.max(HEADER+1,Math.min(height-TELEPORT_MENU_H-10,pz));
                    teleportMenu=true;dragging=false;return true;
                }
            }
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {'''
patch(atlas,[
    ('    private boolean dragging;', '    private boolean dragging;\n'+fields),
    ('        if (info != null) {', '        if (info != null) {'), # position below is patched separately
    ('        }\n    }\n\n    private KnownWorldAtlasMarkers.Marker nearestMarker(double mouseX, double mouseY) {',
     '        }\n'+painting+'    }\n\n    private KnownWorldAtlasMarkers.Marker nearestMarker(double mouseX, double mouseY) {'),
    ('    @Override\n    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {\n'
     '        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {',click),
    ('Wheel: zoom  |  Drag: pan  |  M/Esc: close  |  U: unlinked  |  F: full map',
     'Wheel: zoom  |  Drag: pan  |  Right-click: teleport  |  M/Esc: close  |  U: unlinked')
])
print('V3 Stage3 + Character Sheet + M Atlas teleport installed. Run Gradle build.')
