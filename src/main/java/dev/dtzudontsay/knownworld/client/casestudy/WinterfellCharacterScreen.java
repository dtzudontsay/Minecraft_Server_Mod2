package dev.dtzudontsay.knownworld.client.casestudy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Human-readable, scrollable presentation of the authoritative character profile.
 * Widgets use Minecraft's built-in hit testing/focus rather than painted boxes.
 * Stable runtime IDs and internal scenario keys are deliberately not player UI.
 */
public final class WinterfellCharacterScreen extends Screen {
    private static final int GOLD = 0xFFE1BC75;
    private static final int LIGHT = 0xFFE5DBC4;
    private static final int DIM = 0xFFB1A79A;
    private static final String[] TABS = {"Identity", "Skills", "Mind", "Society", "Life", "GM"};
    private final JsonObject data;
    private int tab;
    private int scroll;
    private boolean gm;
    private record Item(String label, String value, boolean heading) { }

    public WinterfellCharacterScreen(String json) {
        super(Component.literal("Character Record"));
        data = JsonParser.parseString(json).getAsJsonObject();
        gm = data.has("gamemaster") && data.get("gamemaster").getAsBoolean();
    }

    private String value(String key) {
        JsonElement e = data.get(key);
        return e == null || e.isJsonNull() || e.isJsonObject() || e.isJsonArray() ? "" : e.getAsString();
    }

    /** Display name conversion lives only in the UI. Authoritative keys never change. */
    private static String label(String s) {
        if(s==null || s.isBlank()) return "—";
        if(s.equalsIgnoreCase("old_gods")) return "Old Gods";
        if(s.equalsIgnoreCase("faith_of_the_seven")) return "Faith of the Seven";
        String[] tokens=s.replace('_',' ').replace('-',' ').trim().split("\\s+");
        StringBuilder b=new StringBuilder();
        for(String token:tokens) {
            if(b.length()>0)b.append(' ');
            if(token.length()>0)b.append(token.substring(0,1).toUpperCase(Locale.ROOT))
                    .append(token.substring(1).toLowerCase(Locale.ROOT));
        }
        return b.toString();
    }

    private static void add(List<Item> rows,String name,String val) {
        if(val!=null && !val.isBlank() && !"unknown".equalsIgnoreCase(val)
                && !"unspecified".equalsIgnoreCase(val) && !"none".equalsIgnoreCase(val))
            rows.add(new Item(name,val,false));
    }
    private static void section(List<Item> rows,String name) {rows.add(new Item(name,"",true));}
    private void v(List<Item> rows,String title,String key) {add(rows,title,label(value(key)));}
    private void raw(List<Item> rows,String title,String key) {add(rows,title,value(key));}

    private void array(List<Item> rows,String key,String section) {
        JsonArray items = data.has(key) && data.get(key).isJsonArray() ? data.getAsJsonArray(key):null;
        if(items==null || items.size()==0)return;
        section(rows,section);
        for(JsonElement e:items) if(e.isJsonPrimitive()) add(rows,"",label(e.getAsString()));
    }
    private void map(List<Item> rows,String key,String section,boolean sortByValue) {
        JsonObject o=data.has(key)&&data.get(key).isJsonObject()?data.getAsJsonObject(key):null;
        if(o==null||o.size()==0)return;
        section(rows,section);
        List<Map.Entry<String,JsonElement>> entries=new ArrayList<>(o.entrySet());
        if(sortByValue)entries.sort(Comparator.comparingDouble((Map.Entry<String,JsonElement> e)
                -> e.getValue().getAsDouble()).reversed());
        for(var e:entries) {
            String display;
            if(e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) {
                double n=e.getValue().getAsDouble();
                display=String.format(Locale.ROOT,"%.0f / 100",n*100.0);
            } else display=label(e.getValue().getAsString());
            add(rows,label(e.getKey()),display);
        }
    }

    private List<Item> rows() {
        List<Item> rows=new ArrayList<>();
        switch(tab) {
            case 0 -> {
                section(rows,"IDENTITY & BACKGROUND");
                raw(rows,"Name","name"); raw(rows,"Age","age");
                v(rows,"Sex","sex");v(rows,"Culture","culture");v(rows,"Religion","religion");
                v(rows,"Education","education");v(rows,"Occupation","occupation");
                v(rows,"Health","health");v(rows,"Legal status","legalStatus");
                v(rows,"Birthplace","birthplace");v(rows,"Raised in","upbringing");
                section(rows,"FAMILY & HOME");
                raw(rows,"Lord","lord");raw(rows,"Household","householdLabel");
                raw(rows,"Residence","homeLabel");raw(rows,"Workplace","workplaceLabel");
                section(rows,"APPEARANCE");
                raw(rows,"Description","appearance");raw(rows,"Hair","hair");
                raw(rows,"Eyes","eyes");raw(rows,"Build","build");
            }
            case 1 -> {
                map(rows,"skills","SKILLS",true);
                map(rows,"aptitudes","APTITUDES",true);
            }
            case 2 -> {
                array(rows,"traits","CHARACTER TRAITS");
                map(rows,"personality","PERSONALITY",false);
                map(rows,"dialogueStyle","DIALOGUE STYLE",false);
                map(rows,"dispositions","DISPOSITIONS",false);
                map(rows,"characterValues","PERSONAL VALUES",false);
                map(rows,"socialNorms","SOCIAL NORMS",false);
                map(rows,"politics","POLITICAL PREFERENCES",false);
            }
            case 3 -> {
                section(rows,"SOCIAL STANDING");
                raw(rows,"Wealth","wealth");raw(rows,"Social status","socialStatus");
                raw(rows,"Reputation","reputation");
                array(rows,"occupations","OCCUPATIONS");array(rows,"offices","OFFICES");
                array(rows,"courtRoles","COURT ROLES");array(rows,"militaryRoles","MILITARY ROLES");
                array(rows,"combatSpecialties","COMBAT SPECIALTIES");
                array(rows,"aliases","KNOWN ALIASES");
                map(rows,"languages","LANGUAGES",true);
                map(rows,"publicFacts","KNOWN FACTS",false);
            }
            case 4 -> {
                // B4_2_DAILY_LIFE_INTEGRATION: inferred schedules, NOT observed actions.
                raw(rows,"Scheduled current activity","dailyActivity");
                raw(rows,"Work shift (Minecraft ticks)","dailyWorkSchedule");
                raw(rows,"Modeled workdays","modeledWorkdays");
                raw(rows,"Estimated travel ticks","commuteTicksEstimate");
                raw(rows,"Estimated practice gain","practiceGainEstimate");
                array(rows,"motivations","MOTIVATIONS");array(rows,"goals","GOALS");
                array(rows,"fears","FEARS");array(rows,"desires","DESIRES");
            }
            case 5 -> {
                if(!gm) break;
                section(rows,"GAMEMASTER DIAGNOSTICS — NOT PLAYER KNOWLEDGE");
                raw(rows,"Authored key","id");raw(rows,"Simulation ID","runtimeId");
                raw(rows,"Scenario provenance","status");
                raw(rows,"Household key","household");raw(rows,"Home key","residence");
                raw(rows,"Workplace key","workplace");
                v(rows,"Private orientation","orientation");
                raw(rows,"Preferred form of address","dialoguePreferredAddress");
                raw(rows,"Dialogue guidance","dialogueGuidance");
                array(rows,"secrets","CHARACTER SECRETS");
                array(rows,"knownSecrets","KNOWN SECRETS");
                map(rows,"privateFacts","PRIVATE FACTS",false);
                map(rows,"customValues","EXTENDED CHARACTER VALUES",false);
            }
            default -> { }
        }
        if(rows.isEmpty())add(rows,"","Nothing is recorded in this category yet.");
        return rows;
    }

    @Override
    protected void init() {
        super.init();
        int w=Math.min(690,width-18),h=Math.min(440,height-14);
        int x=(width-w)/2, y=(height-h)/2;
        int count=gm?TABS.length:TABS.length-1;
        int each=Math.max(48,(w-24-(count-1)*4)/count);
        for(int i=0;i<count;i++) {
            final int next=i;
            addRenderableWidget(Button.builder(Component.literal(TABS[i]), b->{tab=next;scroll=0;})
                    .bounds(x+12+i*(each+4),y+67,each,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("X"),b->onClose())
                .bounds(x+w-30,y+8,20,20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx,int mouseX,int mouseY,float partialTick) {
        int w=Math.min(690,width-18),h=Math.min(440,height-14);
        int x=(width-w)/2,y=(height-h)/2;
        gfx.fill(0,0,width,height,0xDE0B121A);
        gfx.fill(x,y,x+w,y+h,0xFF1C2529);
        gfx.outline(x,y,w,h,0xFFC8A86D);
        gfx.fill(x+2,y+2,x+w-2,y+61,0xFF303936);
        gfx.text(font,(value("id").startsWith("b4:") ? "KNOWN WORLD" : "WINTERFELL") + "  •  CHARACTER RECORD",x+13,y+8,GOLD,true);
        gfx.text(font,value("name"),x+13,y+28,LIGHT,true);
        gfx.text(font,"Age "+value("age")+"  •  "+label(value("occupation")),x+13,y+45,DIM,false);
        int top=y+100,bottom=y+h-31;
        List<Item> dataRows=rows();
        int visibleHeight=bottom-top;
        int lineHeight=20;
        int maxScroll=Math.max(0,dataRows.size()*lineHeight-visibleHeight+12);
        scroll=Math.max(0,Math.min(maxScroll,scroll));
        gfx.enableScissor(x+10,top,x+w-10,bottom);
        for(int i=0;i<dataRows.size();i++) {
            int yy=top+6+i*lineHeight-scroll;
            if(yy<top-20||yy>bottom)continue;
            Item item=dataRows.get(i);
            if(item.heading) {
                gfx.fill(x+13,yy-2,x+w-13,yy+15,0xFF333B3D);
                gfx.text(font,item.label,x+19,yy+1,GOLD,true);
            } else if(item.label.isBlank()) {
                gfx.text(font,item.value,x+24,yy+2,LIGHT,false);
            } else {
                gfx.text(font,item.label,x+21,yy+2,DIM,false);
                String display=item.value;
                // Value text remains readable in narrow GUI sizes.
                gfx.text(font,display,x+Math.max(145,w/3),yy+2,LIGHT,false);
            }
        }
        gfx.disableScissor();
        gfx.text(font,"Mouse wheel: scroll  •  Esc / X: close  •  "
                +(tab+1)+"/"+(gm?6:5),x+14,y+h-19,DIM,false);
        // REAL Button widgets are rendered after the custom panel, on top,
        // and receive regular Minecraft mouse hit-testing and focus.
        super.extractRenderState(gfx,mouseX,mouseY,partialTick);
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double dx,double dy) {
        scroll=Math.max(0,scroll-(int)Math.signum(dy)*37);
        return true;
    }
    @Override public boolean isPauseScreen() {return false;}
}
