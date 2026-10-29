package dev.dtzudontsay.knownworld.simulation.attributes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterAptitude;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterDisposition;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterHealthState;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterLegalStatus;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterOrientation;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSkill;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterSocialNorm;
import dev.dtzudontsay.knownworld.simulation.npc.profile.CharacterValue;
import dev.dtzudontsay.knownworld.simulation.npc.religion.ReligiousOrderCategory;
import dev.dtzudontsay.knownworld.simulation.npc.routine.NpcRoleType;
import dev.dtzudontsay.knownworld.simulation.social.holding.HoldingType;
import dev.dtzudontsay.knownworld.simulation.social.society.NonDynasticSocietyType;
import dev.dtzudontsay.knownworld.simulation.social.succession.SuccessionLaw;
import dev.dtzudontsay.knownworld.simulation.social.title.TitleType;
import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AV1: validated, READ-ONLY vocabulary and legacy spelling crosswalk.
 *
 * This class never creates an NpcId, grants an inventory, changes a profile,
 * modifies a world or alters a saved game. Its source coverage is the complete
 * AV0 annotated worksheet; the actual enums and reference catalogs remain the
 * authoritative definitions. It is not a third character-state manager.
 *
 * AV2+ may consume a Definition after verifying its owner/authority. A lookup
 * does not authorize mutations or imply that gameplay behavior is connected.
 */
public final class KnownWorldAv1Vocabulary {
    private static final String RESOURCE =
        "/data/knownworld/reference/av1_attribute_vocabulary_v1.json";
    private static final String SCHEMA = "knownworld.av1.attribute_vocabulary.v1";
    private static volatile Index instance;

    public record Definition(String domain, String canonicalId, String kind,
                             String owner, int sourceRow, String sourceLabel,
                             boolean typeResolvable, boolean runtimeAssignable,
                             boolean legacyAlias) { }

    public record SourceRow(int row, String label, String domain, boolean heading,
                            String definitionId) { }

    public record Index(Map<String, Definition> canonical,
                        Map<String, String> aliases,
                        Map<Integer, SourceRow> sourceRows,
                        Map<String, Integer> domainCounts,
                        int headingRows) {
        public Index {
            canonical = Map.copyOf(canonical);
            aliases = Map.copyOf(aliases);
            sourceRows = Map.copyOf(sourceRows);
            domainCounts = Map.copyOf(domainCounts);
        }
        public int definitions() { return canonical.size(); }
        public int historicalAliases() { return aliases.size(); }
    }

    // Explicit allowlist: a resource cannot name arbitrary classes to load.
    private static final Map<String,Class<? extends Enum<?>>> ENUM_OWNERS = Map.ofEntries(
        Map.entry("CHARACTER_SKILL", CharacterSkill.class),
        Map.entry("NATURAL_APTITUDE", CharacterAptitude.class),
        Map.entry("DISPOSITION", CharacterDisposition.class),
        Map.entry("CHARACTER_VALUE", CharacterValue.class),
        Map.entry("REGIONAL_CHARACTER_VALUE", CharacterValue.class),
        Map.entry("SOCIAL_NORM", CharacterSocialNorm.class),
        Map.entry("REGIONAL_SOCIAL_NORM", CharacterSocialNorm.class),
        Map.entry("HEALTH_STATE", CharacterHealthState.class),
        Map.entry("LEGAL_STATUS", CharacterLegalStatus.class),
        Map.entry("ORIENTATION", CharacterOrientation.class),
        Map.entry("ROUTINE_ROLE", NpcRoleType.class),
        Map.entry("SETTLEMENT_TYPE", SettlementType.class),
        Map.entry("HOLDING_TYPE", HoldingType.class),
        Map.entry("TITLE_TYPE", TitleType.class),
        Map.entry("SUCCESSION_LAW", SuccessionLaw.class),
        Map.entry("SOCIETY_TYPE", NonDynasticSocietyType.class),
        Map.entry("RELIGIOUS_INSTITUTION_TYPE", ReligiousOrderCategory.class)
    );

    // These are existing catalog paths. No second occupation/culture/religion
    // catalog is created or made authoritative by AV1.
    private static final Map<String,String[]> REFERENCE_FILES = Map.of(
        "CULTURE",new String[]{"/data/knownworld/reference/cultures.json"},
        "RELIGION",new String[]{"/data/knownworld/reference/religions.json"},
        "LANGUAGE",new String[]{"/data/knownworld/reference/languages.json"},
        "OCCUPATION",new String[]{"/data/knownworld/reference/occupations.json",
                                  "/data/knownworld/reference/occupations_extensions.json"},
        "HOUSE_TEMPLATE",new String[]{"/data/knownworld/scenarios/agot_298_ac/architecture_templates_c3_v1.json"}
    );

    private KnownWorldAv1Vocabulary() { }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(Commands.literal("kwattributes")
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("domain")
                    .then(Commands.argument("domain", StringArgumentType.word())
                        .executes(c -> domain(c.getSource(),
                            StringArgumentType.getString(c, "domain")))))
                .then(Commands.literal("row")
                    .then(Commands.argument("sourceRow", IntegerArgumentType.integer(2, 762))
                        .executes(c -> row(c.getSource(),
                            IntegerArgumentType.getInteger(c, "sourceRow")))))
                .then(Commands.literal("resolve")
                    .then(Commands.argument("domain", StringArgumentType.word())
                        .then(Commands.argument("value", StringArgumentType.word())
                            .executes(c -> resolve(c.getSource(),
                                StringArgumentType.getString(c, "domain"),
                                StringArgumentType.getString(c, "value"))))))));
    }

    /** Source values are queried on demand, never read every server tick. */
    public static Index index() {
        Index ready = instance;
        if (ready != null) return ready;
        synchronized (KnownWorldAv1Vocabulary.class) {
            if (instance == null) instance = loadAndValidate();
            return instance;
        }
    }

    /** Returns null for unknown input; never manufactures a new definition. */
    public static Definition lookup(String domain, String userInput) {
        if (domain == null || userInput == null) return null;
        Index i = index();
        String prefix = normalizeDomain(domain) + ":";
        String input = userInput.trim();
        String key = prefix + input;
        Definition exact = i.canonical().get(key);
        if (exact != null) return exact;
        String aliasKey = normalizeAlias(domain, input);
        String canonicalKey = i.aliases().get(aliasKey);
        if (canonicalKey != null) {
            Definition d = i.canonical().get(canonicalKey);
            if (d == null) throw new IllegalStateException("Broken AV1 alias " + aliasKey);
            return new Definition(d.domain(), d.canonicalId(), d.kind(), d.owner(),
                d.sourceRow(), d.sourceLabel(), d.typeResolvable(),
                d.runtimeAssignable(), true);
        }
        // Common casing may be accepted as input, but is never emitted as an ID.
        for (Definition d : i.canonical().values()) {
            if (d.domain().equals(normalizeDomain(domain)) &&
                d.canonicalId().equalsIgnoreCase(input)) return d;
        }
        return null;
    }

    private static String normalizeDomain(String domain) {
        return domain.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }
    private static String normalizeAlias(String domain, String alias) {
        return normalizeDomain(domain) + ":" + alias.trim().toLowerCase(Locale.ROOT);
    }

    private static Index loadAndValidate() {
        try (InputStream in = KnownWorldAv1Vocabulary.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Missing AV1 vocabulary resource");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!SCHEMA.equals(text(root,"schema")) ||
                root.get("sourceRowsTotal").getAsInt() != 762 ||
                root.get("includedRows").getAsInt() != 761 ||
                root.get("definitionsCount").getAsInt() != 706 ||
                root.get("headingRows").getAsInt() != 55 ||
                !root.getAsJsonObject("policy").get("neverMutateSaves").getAsBoolean() ||
                !root.getAsJsonObject("policy").get("doesNotActivateNewBehaviors").getAsBoolean())
                throw new IllegalStateException("AV1 vocabulary version/authority mismatch");
            Map<String, Set<String>> validReferences = readReferenceIds();
            Map<String, Definition> definitions = new LinkedHashMap<>();
            Map<String, String> aliases = new HashMap<>();
            Map<Integer, SourceRow> rows = new LinkedHashMap<>();
            Map<String,Integer> perDomain = new LinkedHashMap<>();
            Set<Integer> definitionRows = new HashSet<>();
            int headingCount = 0;
            for (JsonElement el : root.getAsJsonArray("definitions")) {
                JsonObject obj = el.getAsJsonObject();
                String domain = text(obj,"domain"), canonical = text(obj,"canonicalId");
                String kind = text(obj,"kind"), owner = text(obj,"owner");
                int row = obj.get("sourceRow").getAsInt();
                String id = domain + ":" + canonical;
                if (!id.equals(text(obj,"id")) || row < 2 || row > 762 ||
                    canonical.isBlank() || domain.isBlank() || !definitionRows.add(row))
                    throw new IllegalStateException("Invalid or duplicate AV1 definition row " + row);
                validateCanonical(domain, kind, canonical, validReferences);
                boolean resolvable = obj.get("typeResolvable").getAsBoolean();
                boolean assignable = obj.get("runtimeAssignable").getAsBoolean();
                if (assignable) throw new IllegalStateException("AV1 must not activate assignments");
                Definition def = new Definition(domain,canonical,kind,owner,row,
                    text(obj,"sourceLabel"),resolvable,assignable,false);
                if (definitions.putIfAbsent(id,def) != null)
                    throw new IllegalStateException("Duplicate AV1 ID: " + id);
                perDomain.merge(domain,1,Integer::sum);
                for (JsonElement alias : obj.getAsJsonArray("aliases")) {
                    String aliasText = alias.getAsString();
                    if (aliasText.isBlank() || aliasText.equals(canonical))
                        throw new IllegalStateException("Spurious AV1 alias for " + id);
                    String aliasKey = normalizeAlias(domain,aliasText);
                    if (aliases.putIfAbsent(aliasKey,id) != null)
                        throw new IllegalStateException("Ambiguous AV1 alias: " + aliasKey);
                }
            }
            for (JsonElement el : root.getAsJsonArray("sourceRows")) {
                JsonObject o = el.getAsJsonObject();
                int row=o.get("row").getAsInt();
                String domain=text(o,"domain"), label=text(o,"label");
                boolean heading=o.get("heading").getAsBoolean();
                JsonElement ref=o.get("definitionId");
                String definitionId = ref == null || ref.isJsonNull() ? null : ref.getAsString();
                if (row < 2 || row > 762 || rows.containsKey(row) ||
                    (heading != (definitionId == null)) ||
                    (!heading && !definitions.containsKey(definitionId)))
                    throw new IllegalStateException("Invalid AV1 spreadsheet lineage row " + row);
                if (heading) headingCount++;
                else if (definitions.get(definitionId).sourceRow() != row)
                    throw new IllegalStateException("Misbound AV1 row " + row);
                rows.put(row,new SourceRow(row,label,domain,heading,definitionId));
            }
            if (definitions.size()!=706 || rows.size()!=761 || headingCount!=55 ||
                definitionRows.size()!=706 || perDomain.size()!=30)
                throw new IllegalStateException("Incomplete AV1 spreadsheet or domains");
            // The header is row 1 and is not a gameplay definition.
            for (int row=2;row<=762;row++) if (!rows.containsKey(row))
                throw new IllegalStateException("Missing Excel AV0 source row " + row);
            return new Index(definitions,aliases,rows,perDomain,headingCount);
        } catch (RuntimeException | java.io.IOException e) {
            throw new IllegalStateException("AV1 vocabulary validation failed (no state changed): " + e.getMessage(),e);
        }
    }

    private static void validateCanonical(String domain, String kind, String id,
                                          Map<String, Set<String>> refs) {
        Class<? extends Enum<?>> enumClass = ENUM_OWNERS.get(domain);
        if (enumClass != null) {
            if (!"ENUM".equals(kind)) throw new IllegalStateException("Wrong enum kind " + domain);
            boolean present = false;
            for (Enum<?> e : enumClass.getEnumConstants()) if (e.name().equals(id)) present=true;
            if (!present) throw new IllegalStateException("Missing enum " + domain + ":" + id);
            return;
        }
        Set<String> catalog = refs.get(domain);
        if (catalog != null) {
            if (!"REFERENCE".equals(kind) || !catalog.contains(id))
                throw new IllegalStateException("Unknown reference " + domain + ":" + id);
            return;
        }
        switch (domain) {
            case "PERSONALITY_SCALE" -> {
                if (!"RECORD_FIELD".equals(kind) ||
                    !Set.of("courage","ambition","compassion","honor","patience","sociability").contains(id))
                    throw new IllegalStateException("Unknown personality dimension " + id);
            }
            case "PLANNER_TIER" -> {
                if (!"PLANNER_CLASS".equals(kind) ||
                    !Set.of("SMALL","MEDIUM","MAJOR","SPECIAL").contains(id))
                    throw new IllegalStateException("Invalid C4 planning tier " + id);
            }
            case "BLUEPRINT_MODE" -> {
                if (!"BLUEPRINT_CLASS".equals(kind) || !"RUIN_OR_LANDMARK_ONLY".equals(id))
                    throw new IllegalStateException("Unknown C4 blueprint mode " + id);
            }
            case "TRAIT_TAG" -> { if (!"LEGACY_UNTYPED_TRAIT".equals(kind)) throw new IllegalStateException("Trait authority mismatch"); }
            case "EDUCATION" -> { if (!"PROPOSED_EDUCATION_ID".equals(kind)) throw new IllegalStateException("Education authority mismatch"); }
            case "PROFILE_FIELD" -> { if (!"COMPOSITE_FIELD_FAMILY".equals(kind)) throw new IllegalStateException("Field family authority mismatch"); }
            case "FAITH_ATTRIBUTE", "CULTURE_ATTRIBUTE" -> {
                if (!"CUSTOM_FIELD".equals(kind) || !(id.startsWith("religion.") || id.startsWith("culture.")))
                    throw new IllegalStateException("Invalid custom-key authority " + id);
            }
            default -> throw new IllegalStateException("Unrecognized AV1 domain: " + domain);
        }
    }

    private static Map<String,Set<String>> readReferenceIds() throws java.io.IOException {
        Map<String,Set<String>> result = new HashMap<>();
        for (Map.Entry<String,String[]> entry : REFERENCE_FILES.entrySet()) {
            Set<String> ids = new LinkedHashSet<>();
            for (String resource : entry.getValue()) {
                try (InputStream in = KnownWorldAv1Vocabulary.class.getResourceAsStream(resource)) {
                    if (in == null) throw new java.io.IOException("Missing authoritative catalog " + resource);
                    JsonElement parsed = JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8));
                    JsonArray array = parsed.isJsonArray() ? parsed.getAsJsonArray() :
                        parsed.getAsJsonObject().getAsJsonArray("templates");
                    if (array == null) throw new IllegalStateException("Unrecognized catalog " + resource);
                    for (JsonElement item : array) {
                        String id = text(item.getAsJsonObject(),"id");
                        if (!ids.add(id)) throw new IllegalStateException("Duplicate catalog ID " + id);
                    }
                }
            }
            result.put(entry.getKey(),Collections.unmodifiableSet(ids));
        }
        return result;
    }

    private static String text(JsonObject obj, String key) {
        JsonElement e=Objects.requireNonNull(obj,"json").get(key);
        if(e==null || e.isJsonNull()) throw new IllegalStateException("Missing field " + key);
        return e.getAsString();
    }

    private static void say(CommandSourceStack source,String message) {
        source.sendSuccess(() -> Component.literal(message),false);
    }
    private static int status(CommandSourceStack source) {
        try {
            Index i=index();
            say(source,"[AV1] VERIFIED: "+i.definitions()+" definitions; 30 domains; " +
                i.sourceRows().size()+" original data/heading rows; "+i.headingRows()+" headings; "+
                i.historicalAliases()+" legacy label aliases.");
            say(source,"[AV1] Canonical enums/catalogs validated. NO worldwide profile migration, " +
                "NO trait behavior activation, NO save or economy mutations.");
            return 1;
        }catch(Exception e){source.sendFailure(Component.literal("[AV1] BLOCKED: "+e.getMessage()));return 0;}
    }
    private static int domain(CommandSourceStack source,String domain) {
        try {
            String id=normalizeDomain(domain);
            Integer count=index().domainCounts().get(id);
            if (count==null) throw new IllegalArgumentException("Unknown domain "+id);
            say(source,"[AV1] "+id+": "+count+" definitions (source labels preserved).");
            return 1;
        }catch(Exception e){source.sendFailure(Component.literal("[AV1] "+e.getMessage()));return 0;}
    }
    private static int row(CommandSourceStack source,int sourceRow) {
        try {
            SourceRow r=index().sourceRows().get(sourceRow);
            if (r==null) throw new IllegalArgumentException("Unknown source row " + sourceRow);
            if (r.heading()) say(source,"[AV1] Workbook row "+sourceRow+": SECTION " + r.label());
            else {
                Definition d=index().canonical().get(r.definitionId());
                say(source,"[AV1] row "+sourceRow+" ["+d.domain()+"] "+r.label()+" -> "+d.canonicalId()+
                    " / "+d.kind()+" / owner="+d.owner()+"; AV2+ activation pending");
            }
            return 1;
        }catch(Exception e){source.sendFailure(Component.literal("[AV1] "+e.getMessage()));return 0;}
    }
    private static int resolve(CommandSourceStack source,String domain,String input) {
        try {
            Definition d=lookup(domain,input);
            if (d==null) throw new IllegalArgumentException("Unknown value for " + normalizeDomain(domain) + ": " + input);
            say(source,"[AV1] "+d.domain()+"/"+input+" -> "+d.canonicalId()+
                (d.legacyAlias()?" (LEGACY ALIAS; use canonical ID)":"")+
                "; kind="+d.kind()+"; typeResolvable="+d.typeResolvable()+
                "; GAMEPLAY ACTIVATION=OFF");
            return 1;
        }catch(Exception e){source.sendFailure(Component.literal("[AV1] "+e.getMessage()));return 0;}
    }
}
