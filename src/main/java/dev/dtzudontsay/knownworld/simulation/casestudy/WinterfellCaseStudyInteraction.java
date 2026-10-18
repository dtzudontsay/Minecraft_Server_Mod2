package dev.dtzudontsay.knownworld.simulation.casestudy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only, opt-in Winterfell ACTOR PROXY inspector. Does not create
 * simulation NPCs or mutate the save. The source roster is a draft.
 * All generated actors must be migrated into NpcRegistry + AuthoredIdRegistry
 * exactly once before population simulation is enabled.
 */
public final class WinterfellCaseStudyInteraction {
    public static final boolean ENABLED = true; // Set false to remove the test feature.
    private static final String PREFIX = "kwfid_";
    private static final String DATA = "/data/knownworld/case_study/winterfell_roster.json";
    private static Map<String, JsonObject> cached;

    private WinterfellCaseStudyInteraction() { }

    public static void register() {
        if (!ENABLED) return;
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            String personId = null;
            for (String tag : entity.entityTags()) {
                if (tag.startsWith(PREFIX)) {
                    personId = tag.substring(PREFIX.length());
                    break;
                }
            }
            if (personId == null) return InteractionResult.PASS;
            // The client acknowledges interaction; only the server reads data and writes chat.
            if (!level.isClientSide()) {
                try {
                    JsonObject person = people().get(personId);
                    if (person == null) {
                        player.sendSystemMessage(Component.literal(
                            "[Winterfell] No roster entry for " + personId));
                    } else {
                        String name = get(person, "name");
                        String age = get(person, "age");
                        String role = get(person, "job");
                        String culture = get(person, "culture");
                        String faith = get(person, "religion");
                        String residence = get(person, "homeBuildingId");
                        String work = get(person, "workBuildingId");
                        String provenance = get(person, "status");
                        player.sendSystemMessage(Component.literal(
                            "══ " + name + " — Winterfell character card (draft) ══"));
                        player.sendSystemMessage(Component.literal(
                            "ID: " + personId + " | Age: " + age +
                            " | Status: " + provenance));
                        player.sendSystemMessage(Component.literal(
                            "Occupation: " + role + " | Lord: Eddard Stark"));
                        player.sendSystemMessage(Component.literal(
                            "Culture: " + culture + " | Faith: " + faith));
                        player.sendSystemMessage(Component.literal(
                            "Residence: " + residence + " | Workplace: " + work));
                        player.sendSystemMessage(Component.literal(
                            "Traits: " + traits(person)));
                        player.sendSystemMessage(Component.literal(
                            "Skills: " + skills(person)));
                        player.sendSystemMessage(Component.literal(
                            "Preview only: not yet linked to an authoritative NpcId."));
                    }
                } catch (RuntimeException e) {
                    player.sendSystemMessage(Component.literal(
                        "[Winterfell] Character data unavailable: " + e.getMessage()));
                }
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static Map<String, JsonObject> people() {
        if (cached != null) return cached;
        synchronized (WinterfellCaseStudyInteraction.class) {
            if (cached != null) return cached;
            try (InputStream raw = WinterfellCaseStudyInteraction.class.getResourceAsStream(DATA)) {
                if (raw == null) throw new IllegalStateException("Missing case-study roster; run Python builder");
                JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(raw, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray residents = root.getAsJsonArray("residents");
                Map<String, JsonObject> found = new HashMap<>();
                for (JsonElement e : residents) {
                    JsonObject person = e.getAsJsonObject();
                    found.put(person.get("id").getAsString(), person);
                }
                cached = Map.copyOf(found);
                return cached;
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Could not load Winterfell data", e);
            }
        }
    }

    private static String get(JsonObject p, String name) {
        JsonElement value = p.get(name);
        return value == null || value.isJsonNull() ? "—" : value.getAsString();
    }

    private static String traits(JsonObject p) {
        JsonArray a = p.getAsJsonArray("traits");
        if (a == null || a.size() == 0) return "not recorded";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(4, a.size()); i++) {
            if (i > 0) result.append(", ");
            result.append(a.get(i).getAsString());
        }
        return result.toString();
    }

    private static String skills(JsonObject p) {
        JsonObject skills = p.getAsJsonObject("skills");
        if (skills == null || skills.size() == 0) return "not recorded";
        return skills.entrySet().stream()
            .filter(e -> e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber())
            .sorted((a, b) -> Double.compare(b.getValue().getAsDouble(), a.getValue().getAsDouble()))
            .limit(3)
            .map(e -> e.getKey() + " " + String.format(Locale.ROOT, "%.0f%%", 100*e.getValue().getAsDouble()))
            .reduce((a, b) -> a + ", " + b)
            .orElse("not recorded");
    }
}
