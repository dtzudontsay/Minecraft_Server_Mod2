package dev.dtzudontsay.knownworld.client.casestudy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Read-only client presentation of the live server's inspected NPC profile. */
public final class WinterfellCharacterScreen extends Screen {
    private static final int GOLD = 0xFFE1BC75;
    private static final int LIGHT = 0xFFE5DBC4;
    private static final int DIM = 0xFFB1A79A;
    private final JsonObject data;
    private int tab;
    private int scroll;

    public WinterfellCharacterScreen(String json) {
        super(Component.literal("Character"));
        data = JsonParser.parseString(json).getAsJsonObject();
    }
    private String value(String key) {
        JsonElement e = data.get(key);
        return e == null || e.isJsonNull() ? "—" : e.getAsString();
    }
    private int age() {
        JsonElement v = data.get("age");
        return v == null ? 0 : v.getAsInt();
    }
    private static String pretty(String s) {
        return s.replace('_',' ').replace('-',' ');
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx,mouseX,mouseY,partialTick);
        gfx.fill(0,0,width,height,0xDC0A1116);
        int w = Math.min(550,width-30), h = Math.min(385,height-22);
        int x = (width-w)/2, y=(height-h)/2;
        gfx.fill(x,y,x+w,y+h,0xFF1C2629);
        gfx.outline(x,y,w,h,0xFFB99B63);
        gfx.fill(x+2,y+2,x+w-2,y+58,0xFF303936);
        gfx.text(font, "WINTERFELL  •  CHARACTER RECORD",x+15,y+10,GOLD,false);
        gfx.text(font, value("name"),x+15,y+29,LIGHT,true);
        gfx.text(font, "Age " + age() + "  |  " + pretty(value("occupation")), x+15,y+45,DIM,false);
        gfx.fill(x+w-26,y+8,x+w-8,y+26,0xFF613D37);
        gfx.text(font,"X",x+w-20,y+12,0xFFFFFFFF,true);

        int ty=y+68;
        gfx.fill(x+12,ty,x+130,ty+25,tab==0?0xFF866942:0xFF353D3A);
        gfx.fill(x+134,ty,x+252,ty+25,tab==1?0xFF866942:0xFF353D3A);
        gfx.text(font,"Identity & Family",x+18,ty+8,LIGHT,false);
        gfx.text(font,"Skills & Traits",x+140,ty+8,LIGHT,false);
        int base=ty+39;
        gfx.enableScissor(x+10,base,x+w-10,y+h-30);
        if (tab==0) {
            row(gfx,x,base,"Culture",pretty(value("culture")),0);
            row(gfx,x,base,"Religion",pretty(value("religion")),1);
            row(gfx,x,base,"Sex",pretty(value("sex")),2);
            row(gfx,x,base,"Occupation",pretty(value("occupation")),3);
            row(gfx,x,base,"Lord",value("lord"),4);
            row(gfx,x,base,"Household",value("household"),5);
            row(gfx,x,base,"Home",value("residence"),6);
            row(gfx,x,base,"Workplace",value("workplace"),7);
            row(gfx,x,base,"Scenario status",pretty(value("status")),8);
            row(gfx,x,base,"Persistent ID",value("id"),9);
        } else {
            JsonElement traits = data.get("traits");
            List<String> allTraits = new ArrayList<>();
            if (traits != null && traits.isJsonArray())
                traits.getAsJsonArray().forEach(e -> allTraits.add(pretty(e.getAsString())));
            gfx.text(font,"TRAITS",x+17,base,GOLD,true);
            int line=1;
            for (String trait : allTraits) {
                row(gfx,x,base,"•",trait,line++);
            }
            int skillsHeading = base + 22 * (Math.max(1,allTraits.size())+1);
            gfx.text(font,"TOP SKILLS",x+17,skillsHeading,GOLD,true);
            JsonObject skills = data.getAsJsonObject("skills");
            if (skills != null) {
                List<Map.Entry<String,JsonElement>> sorted=new ArrayList<>(skills.entrySet());
                sorted.sort(Comparator.comparingDouble((Map.Entry<String,JsonElement> e)->e.getValue().getAsDouble()).reversed());
                int i=0;
                for(var e:sorted) {
                    if(i++>=12)break;
                    String score=String.format(Locale.ROOT,"%.0f / 100",e.getValue().getAsDouble()*100);
                    int yy=skillsHeading+12+22*i-scroll;
                    gfx.text(font,pretty(e.getKey()),x+18,yy,LIGHT,false);
                    gfx.text(font,score,x+w-112,yy,GOLD,false);
                }
            }
        }
        gfx.disableScissor();
        gfx.text(font,"Live simulation identity  •  read-only inspection  •  Esc to close",x+14,y+h-18,DIM,false);
    }
    private void row(GuiGraphicsExtractor gfx,int x,int base,String label,String val,int index) {
        int yy=base+8+index*22-scroll;
        gfx.text(font,label,x+18,yy,DIM,false);
        gfx.text(font,val,x+150,yy,LIGHT,false);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx=event.x(),my=event.y();
        int w = Math.min(550,width-30), h = Math.min(385,height-22);
        int x=(width-w)/2, y=(height-h)/2;
        if(event.button()==0) {
            if(mx>=x+w-29&&mx<=x+w-5&&my>=y+5&&my<=y+30) {onClose();return true;}
            if(my>=y+68&&my<=y+93&&mx>=x+12&&mx<=x+252) {
                tab = mx < x+132 ? 0:1;scroll=0;return true;
            }
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double sx,double sy) {
        scroll = Math.max(0,Math.min(240,scroll-(int)(sy*18)));
        return true;
    }
    @Override public boolean isPauseScreen() {return false;}
}
