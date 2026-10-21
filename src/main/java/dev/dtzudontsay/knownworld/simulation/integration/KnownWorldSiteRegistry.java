package dev.dtzudontsay.knownworld.simulation.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.dtzudontsay.knownworld.KnownWorld;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * B2+B3, authored world-wide physical-site and political-holding planning registry.
 *
 * This is READ-ONLY scenario data. It does not spawn structures, create people,
 * move NPCs, mutate existing holdings, or replace the original world references.
 * Coordinates are accepted map anchors, NOT safe build positions.
 *
 * Geographic references and actual landed holding ownership still belong to
 * WorldReferenceCatalog and LandedHoldingManager respectively.
 *
 * This catalog can be consumed by future C worldgen and B4 identities, but
 * those consumers MUST validate terrain and only count actual resident NpcIds.
 */
public final class KnownWorldSiteRegistry implements ModInitializer {

    public static final String RESOURCE =
            "data/knownworld/scenarios/agot_298_ac/world_sites_b2_b3_v1.json";

    public record Marker(
            String markerId,
            String siteId,
            String primaryMarkerId,
            String name,
            String zone,
            String geographicId,
            long x,
            long z,
            String identityStatus,
            boolean planningAnchorAccepted,
            boolean constructionApproved
    ) { }

    public record Site(
            String siteId,
            String primaryMarkerId,
            String name,
            String geographicId,
            String zone,
            long x,
            long z,
            String tier,
            String tierEvidence,
            String archetype,
            String physicalType,
            String habitation,
            String materialPalette,
            String governmentHint,
            String cultureContext,
            String religionContext,
            String regionalInfluenceSource,
            int plannedPopulationMin,
            int plannedPopulationBaseline,
            int plannedPopulationMax,
            Map<String, Integer> workerSectorPercent,
            Map<String, Integer> workerRolePercent,
            List<String> markerIds,
            List<String> holdingIds,
            boolean constructionApproved,
            boolean createsPeople
    ) {
        public Site {
            workerSectorPercent = Map.copyOf(workerSectorPercent);
            workerRolePercent = Map.copyOf(workerRolePercent);
            markerIds = List.copyOf(markerIds);
            holdingIds = List.copyOf(holdingIds);
        }
    }

    public record Holding(
            String holdingId,
            String name,
            String worldLocationId,
            String holdingType,
            String tier,
            String archetype,
            String placementStatus,
            String ownerDynastyId,
            String holderNpcId,
            String parentHoldingId,
            String parentCityGeographicId,
            List<String> linkedSiteIds,
            List<String> referenceAnchorSiteIds,
            boolean politicalDomain,
            boolean constructionApproved
    ) {
        public Holding {
            linkedSiteIds = List.copyOf(linkedSiteIds);
            referenceAnchorSiteIds = List.copyOf(referenceAnchorSiteIds);
        }
    }

    /** Regional geography is sourced from WorldReferenceCatalog and its existing
     *  territory polygon and raster subregion data; this is a linked view only. */
    public record Region(
            String regionId,
            String displayName,
            String geographicKind,
            String parentLocationId,
            String sourceProvenance,
            String effectiveInfluenceSourceId,
            boolean hasMappedBoundaryOrMask,
            Map<String, Double> cultureWeights,
            Map<String, Double> religionWeights,
            Map<String, Double> languageWeights,
            List<String> domainHoldingIds,
            List<String> descendantSiteIds
    ) {
        public Region {
            cultureWeights = Map.copyOf(cultureWeights);
            religionWeights = Map.copyOf(religionWeights);
            languageWeights = Map.copyOf(languageWeights);
            domainHoldingIds = List.copyOf(domainHoldingIds);
            descendantSiteIds = List.copyOf(descendantSiteIds);
        }
    }

    /** Every canonical geographic ID is retained even without a mapped marker
     * or a political holding. Its original WorldReferenceCatalog is still
     * authoritative for classification and parentage. */
    public record PlaceReference(
            String referenceId,
            String displayName,
            String geographicKind,
            String parentId,
            String sourceProvenance,
            String regionalInfluenceSourceId,
            List<String> markerIds,
            List<String> siteIds,
            List<String> holdingIds
    ) {
        public PlaceReference {
            markerIds = List.copyOf(markerIds);
            siteIds = List.copyOf(siteIds);
            holdingIds = List.copyOf(holdingIds);
        }
    }

    public static final class Registry {
        private final Map<String, Marker> markers;
        private final Map<String, Site> sites;
        private final Map<String, Holding> holdings;
        private final Map<String, Region> regions;
        private final Map<String, PlaceReference> references;
        private final Map<String, List<Site>> byGeographicId;

        private Registry(Map<String, Marker> markers, Map<String, Site> sites,
                         Map<String, Holding> holdings, Map<String, Region> regions,
                         Map<String, PlaceReference> references) {
            this.markers = Collections.unmodifiableMap(new LinkedHashMap<>(markers));
            this.sites = Collections.unmodifiableMap(new LinkedHashMap<>(sites));
            this.holdings = Collections.unmodifiableMap(new LinkedHashMap<>(holdings));
            this.regions = Collections.unmodifiableMap(new LinkedHashMap<>(regions));
            this.references = Collections.unmodifiableMap(new LinkedHashMap<>(references));
            Map<String, List<Site>> byGeo = new HashMap<>();
            for (Site site : sites.values()) {
                if (site.geographicId() != null && !site.geographicId().isBlank()) {
                    byGeo.computeIfAbsent(site.geographicId(), ignored -> new ArrayList<>()).add(site);
                }
            }
            byGeo.replaceAll((ignored, value) -> List.copyOf(value));
            this.byGeographicId = Map.copyOf(byGeo);
        }

        public Map<String, Marker> markers() { return markers; }
        public Map<String, Site> sites() { return sites; }
        public Map<String, Holding> holdings() { return holdings; }
        public Map<String, Region> regions() { return regions; }
        public Map<String, PlaceReference> references() { return references; }
        public Optional<PlaceReference> placeReference(String id) { return Optional.ofNullable(references.get(id)); }
        public Optional<Region> region(String id) { return Optional.ofNullable(regions.get(id)); }
        public Optional<Marker> marker(String id) { return Optional.ofNullable(markers.get(id)); }
        public Optional<Site> site(String id) { return Optional.ofNullable(sites.get(id)); }
        public Optional<Holding> holding(String id) { return Optional.ofNullable(holdings.get(id)); }
        public List<Site> sitesAtGeographicId(String id) {
            return byGeographicId.getOrDefault(id, List.of());
        }

        public Optional<Site> siteAtMarker(String markerId) {
            Marker marker = markers.get(markerId);
            return marker == null ? Optional.empty() : Optional.ofNullable(sites.get(marker.siteId()));
        }
    }

    private static volatile Registry loaded;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var root = Commands.literal("kwsites");
            root.then(Commands.literal("status")
                    .executes(ctx -> status(ctx.getSource())));
            root.then(Commands.literal("marker")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> markerInfo(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id")))));
            root.then(Commands.literal("site")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> siteInfo(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id")))));
            root.then(Commands.literal("holding")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> holdingInfo(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id")))));
            root.then(Commands.literal("region")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> regionInfo(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id")))));
            root.then(Commands.literal("place")
                    .then(Commands.argument("id", StringArgumentType.word())
                            .executes(ctx -> placeInfo(ctx.getSource(),
                                    StringArgumentType.getString(ctx, "id")))));
            dispatcher.register(root);
        });
        KnownWorld.LOGGER.info("B2+B3 world site registry and commands registered");
    }

    public static Registry registry() {
        Registry current = loaded;
        if (current == null) {
            synchronized (KnownWorldSiteRegistry.class) {
                current = loaded;
                if (current == null) {
                    current = load();
                    loaded = current;
                }
            }
        }
        return current;
    }

    public static Optional<Site> siteAtMarker(String markerId) {
        return registry().siteAtMarker(Objects.requireNonNull(markerId));
    }

    public static Optional<Holding> holding(String holdingId) {
        return registry().holding(Objects.requireNonNull(holdingId));
    }

    private static Registry load() {
        try (InputStream stream = KnownWorldSiteRegistry.class.getClassLoader()
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("B2+B3 scenario registry is missing: " + RESOURCE
                        + ". Run py tools/geodata/build_b2_b3_world_registry.py before building.");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (number(root, "schemaVersion") != 1
                    || !"PLANNING_ONLY".equals(string(root, "buildAuthority"))) {
                throw new IllegalStateException("Unsupported or unsafe B2+B3 registry version");
            }
            JsonObject policy = root.getAsJsonObject("policy");
            if (!bool(policy,"workingMapCoordinatesAccepted")
                    || bool(policy,"terrainFootprintsApproved")
                    || !bool(policy,"doNotGenerateChunks")
                    || !bool(policy,"doNotCreateOrModifyPopulation")) {
                throw new IllegalStateException("Unsafe B2+B3 registry policy");
            }

            Map<String, Marker> markerMap = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("markers")) {
                JsonObject e = element.getAsJsonObject();
                JsonObject anchor = e.getAsJsonObject("workingAnchor");
                Marker marker = new Marker(
                        string(e,"markerId"), string(e,"siteId"),
                        string(e,"primaryMarkerId"), string(e,"name"),
                        string(e,"zone"), nullable(e,"geographicId"),
                        anchor.get("x").getAsLong(), anchor.get("z").getAsLong(),
                        string(e,"markerIdentityStatus"),
                        "ACCEPTED_FOR_PLANNING".equals(string(anchor,"status")),
                        bool(anchor,"isBuildApproved"));
                if (!marker.planningAnchorAccepted() || marker.constructionApproved()) {
                    throw new IllegalStateException("Unsafe marker: " + marker.markerId());
                }
                if (markerMap.putIfAbsent(marker.markerId(),marker) != null) {
                    throw new IllegalStateException("Duplicate map marker ID " + marker.markerId());
                }
            }

            Map<String, Site> siteMap = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("physicalSites")) {
                JsonObject e = element.getAsJsonObject();
                JsonObject anchor = e.getAsJsonObject("workingAnchor");
                JsonObject population = e.getAsJsonObject("populationPlanning");
                JsonObject occ = e.getAsJsonObject("occupationDistribution");
                Site site = new Site(
                        string(e,"siteId"),string(e,"primaryMarkerId"),
                        string(e,"name"),nullable(e,"geographicId"),string(e,"zone"),
                        anchor.get("x").getAsLong(),anchor.get("z").getAsLong(),
                        string(e,"tier"),string(e,"tierEvidence"),string(e,"archetype"),
                        string(e.getAsJsonObject("physicalType"),"value"),
                        string(e.getAsJsonObject("habitation"),"value"),
                        string(e,"materialPalette"),valueAsString(e.getAsJsonObject("government")),
                        valueAsString(e.getAsJsonObject("culturalContext")),
                        valueAsString(e.getAsJsonObject("religiousContext")),
                        nullable(e.getAsJsonObject("regionalInfluences"),"effectiveSourceLocationId"),
                        number(population,"min"),number(population,"baseline"),number(population,"max"),
                        integerMap(occ.getAsJsonObject("workerSectorsPercent")),
                        integerMap(occ.getAsJsonObject("workerRolesPercent")),
                        stringList(e.getAsJsonArray("markerIds")),
                        stringList(e.getAsJsonArray("linkedPoliticalHoldingIds")),
                        bool(anchor,"isBuildApproved") || bool(e,"canAutoGenerate"),
                        bool(e,"createResidentsNow"));
                validateSite(site);
                if (siteMap.putIfAbsent(site.siteId(),site)!=null) {
                    throw new IllegalStateException("Duplicate physical site ID " + site.siteId());
                }
            }

            Map<String, Holding> holdingMap = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("holdings")) {
                JsonObject e = element.getAsJsonObject();
                Holding holding = new Holding(
                        string(e,"holdingId"),string(e,"name"),string(e,"worldLocationId"),
                        string(e,"holdingType"),string(e,"tier"),string(e,"archetype"),
                        string(e,"placementStatus"),nullable(e,"ownerDynastyId"),
                        nullable(e,"holderNpcId"),nullable(e,"parentHoldingId"),
                        nullable(e,"parentCityGeographicId"),
                        stringList(e.getAsJsonArray("linkedSiteIds")),
                        stringList(e.getAsJsonArray("referenceAnchorSiteIds")),
                        bool(e,"isPoliticalDomain"),bool(e,"constructionApproved"));
                if (holding.constructionApproved()) {
                    throw new IllegalStateException("Holding is accidentally build-approved: " + holding.holdingId());
                }
                if (holding.politicalDomain() && !"NO_SETTLEMENT".equals(holding.tier())) {
                    throw new IllegalStateException("Political domain classified as town: " + holding.holdingId());
                }
                if (holdingMap.putIfAbsent(holding.holdingId(),holding)!=null) {
                    throw new IllegalStateException("Duplicate holding ID " + holding.holdingId());
                }
            }

            Map<String, Region> regionMap = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("regions")) {
                JsonObject e = element.getAsJsonObject();
                JsonObject influences = e.getAsJsonObject("regionalInfluences");
                Region region = new Region(
                        string(e,"regionId"),string(e,"displayName"),
                        string(e,"geographicKind"),nullable(e,"parentLocationId"),
                        string(e,"sourceProvenance"),
                        nullable(influences,"effectiveSourceLocationId"),
                        e.getAsJsonArray("spatialDefinitions").size() > 0,
                        weights(influences.getAsJsonObject("cultureWeights")),
                        weights(influences.getAsJsonObject("religionWeights")),
                        weights(influences.getAsJsonObject("languageWeights")),
                        stringList(e.getAsJsonArray("directDomainHoldingIds")),
                        stringList(e.getAsJsonArray("descendantPhysicalSiteIds")));
                if (regionMap.putIfAbsent(region.regionId(),region)!=null) {
                    throw new IllegalStateException("Duplicate geographic region " + region.regionId());
                }
            }
            if (regionMap.isEmpty()) {
                throw new IllegalStateException("Missing B2 regional profiles");
            }
            for (Region region : regionMap.values()) {
                for (String sid : region.descendantSiteIds()) {
                    if (!siteMap.containsKey(sid))
                        throw new IllegalStateException("Unknown site in regional profile " + sid);
                }
                for (String hid : region.domainHoldingIds()) {
                    if (!holdingMap.containsKey(hid))
                        throw new IllegalStateException("Unknown holding in regional profile " + hid);
                }
            }

            Map<String, PlaceReference> placeMap = new LinkedHashMap<>();
            for (JsonElement element : root.getAsJsonArray("referencePlaces")) {
                JsonObject e = element.getAsJsonObject();
                PlaceReference p = new PlaceReference(
                        string(e,"referenceId"),string(e,"displayName"),
                        string(e,"geographicKind"),nullable(e,"parentId"),
                        string(e,"sourceProvenance"),nullable(e,"regionalInfluenceSourceId"),
                        stringList(e.getAsJsonArray("mappedMarkerIds")),
                        stringList(e.getAsJsonArray("physicalSiteIds")),
                        stringList(e.getAsJsonArray("linkedPoliticalHoldingIds")));
                if (placeMap.putIfAbsent(p.referenceId(),p)!=null)
                    throw new IllegalStateException("Duplicate canonical reference " + p.referenceId());
            }
            if (placeMap.isEmpty())
                throw new IllegalStateException("Missing B2 canonical place references");
            for (PlaceReference p : placeMap.values()) {
                for (String mid : p.markerIds()) {
                    if (!markerMap.containsKey(mid))
                        throw new IllegalStateException("Invalid marker reference at " + p.referenceId());
                }
                for (String sid : p.siteIds()) {
                    if (!siteMap.containsKey(sid))
                        throw new IllegalStateException("Invalid site reference at " + p.referenceId());
                }
                for (String hid : p.holdingIds()) {
                    if (!holdingMap.containsKey(hid))
                        throw new IllegalStateException("Invalid holding reference at " + p.referenceId());
                }
            }

            if (markerMap.size()!=253 || holdingMap.size()!=259) {
                throw new IllegalStateException("B2+B3 record coverage is incomplete: markers="
                        +markerMap.size()+", holdings="+holdingMap.size());
            }
            Set<String> seenMarkers = new HashSet<>();
            for (Site site : siteMap.values()) {
                for (String mid : site.markerIds()) {
                    Marker marker = markerMap.get(mid);
                    if (marker==null || !site.siteId().equals(marker.siteId())) {
                        throw new IllegalStateException("Physical site has invalid marker pointer " + mid);
                    }
                    if (!seenMarkers.add(mid)) {
                        throw new IllegalStateException("Duplicate use of marker " + mid);
                    }
                }
                if (!markerMap.containsKey(site.primaryMarkerId())
                        || !site.siteId().equals(markerMap.get(site.primaryMarkerId()).siteId())) {
                    throw new IllegalStateException("Missing primary site marker " + site.siteId());
                }
                for (String hid : site.holdingIds()) {
                    if (!holdingMap.containsKey(hid)) {
                        throw new IllegalStateException("Site references unknown holding " + hid);
                    }
                }
            }
            if (!seenMarkers.equals(markerMap.keySet())) {
                throw new IllegalStateException("Not every marker belongs to exactly one site");
            }
            for (Holding holding : holdingMap.values()) {
                for (String sid : holding.referenceAnchorSiteIds()) {
                    if (!siteMap.containsKey(sid)) {
                        throw new IllegalStateException("Holding links to missing site " + sid);
                    }
                }
            }
            KnownWorld.LOGGER.info("B2+B3 registry read: {} markers, {} physical sites, {} holdings, {} regions, {} canonical references",
                    markerMap.size(), siteMap.size(), holdingMap.size(), regionMap.size(), placeMap.size());
            return new Registry(markerMap,siteMap,holdingMap,regionMap,placeMap);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read B2+B3 site registry",exception);
        }
    }

    private static void validateSite(Site site) {
        if (site.constructionApproved() || site.createsPeople()) {
            throw new IllegalStateException("B2+B3 registry must not modify world/NPC state " + site.siteId());
        }
        if (!Set.of("MAJOR","MEDIUM","SMALL","SPECIAL","NO_SETTLEMENT").contains(site.tier())) {
            throw new IllegalStateException("Unknown generation tier " + site.tier());
        }
        if (site.plannedPopulationMin()<0 || site.plannedPopulationBaseline()<site.plannedPopulationMin()
                || site.plannedPopulationMax()<site.plannedPopulationBaseline()) {
            throw new IllegalStateException("Invalid population envelope " + site.siteId());
        }
        int coarse = site.workerSectorPercent().values().stream().mapToInt(Integer::intValue).sum();
        int fine = site.workerRolePercent().values().stream().mapToInt(Integer::intValue).sum();
        if (site.workerSectorPercent().values().stream().anyMatch(x -> x < 0 || x > 100)
                || site.workerRolePercent().values().stream().anyMatch(x -> x < 0 || x > 100)
                || !((coarse==100&&fine==100)||(coarse==0&&fine==0))) {
            throw new IllegalStateException("Occupation fractions invalid " + site.siteId());
        }
    }

    private static int number(JsonObject e,String key) {return e.get(key).getAsInt();}
    private static String string(JsonObject e,String key) {return e.get(key).getAsString();}
    private static String nullable(JsonObject e,String key) {
        JsonElement raw = e.get(key);
        return raw == null || raw.isJsonNull() ? null : raw.getAsString();
    }
    private static String valueAsString(JsonObject evidence) {return nullable(evidence,"value");}
    private static boolean bool(JsonObject e,String key) {return e.get(key).getAsBoolean();}
    private static List<String> stringList(JsonArray json) {
        List<String> result = new ArrayList<>();
        for (JsonElement e : json) result.add(e.getAsString());
        return List.copyOf(result);
    }
    private static Map<String,Integer> integerMap(JsonObject json) {
        Map<String,Integer> result=new LinkedHashMap<>();
        for (Map.Entry<String,JsonElement> entry : json.entrySet())
            result.put(entry.getKey(),entry.getValue().getAsInt());
        return result;
    }
    private static Map<String,Double> weights(JsonObject json) {
        Map<String,Double> result = new LinkedHashMap<>();
        for (Map.Entry<String,JsonElement> entry : json.entrySet()) {
            JsonObject evidence = entry.getValue().getAsJsonObject();
            JsonElement raw = evidence.get("weight");
            if (raw == null) raw = evidence.get("value");
            if (raw != null && !raw.isJsonNull()) result.put(entry.getKey(),raw.getAsDouble());
        }
        return result;
    }

    private static void say(CommandSourceStack source,String message) {
        source.sendSuccess(() -> Component.literal(message),false);
    }
    private static int status(CommandSourceStack source) {
        Registry r=registry();
        long major=r.sites().values().stream().filter(x->"MAJOR".equals(x.tier())).count();
        long medium=r.sites().values().stream().filter(x->"MEDIUM".equals(x.tier())).count();
        long small=r.sites().values().stream().filter(x->"SMALL".equals(x.tier())).count();
        long special=r.sites().values().stream().filter(x->"SPECIAL".equals(x.tier())).count();
        say(source,"[B2+B3] markers="+r.markers().size()+" physical sites="+r.sites().size()
                +" holdings="+r.holdings().size()+" regions="+r.regions().size()+" references="+r.references().size()+" | major="+major+" medium="+medium
                +" small="+small+" special="+special+" | build-approved=0");
        return 1;
    }
    private static int markerInfo(CommandSourceStack source,String id) {
        Marker m=registry().marker(id).orElse(null);
        if (m==null) {source.sendFailure(Component.literal("Unknown map marker: "+id));return 0;}
        Site site=registry().site(m.siteId()).orElseThrow();
        say(source,"[B2 Marker] "+m.markerId()+" "+m.name()+" at X="+m.x()+" Z="+m.z()
                +" | geo="+m.geographicId()+" | identity="+m.identityStatus());
        say(source,"Physical site="+site.siteId()+" tier="+site.tier()+" archetype="+site.archetype()
                +" | original markers="+site.markerIds()+" | build safe=NOT YET");
        return 1;
    }
    private static int siteInfo(CommandSourceStack source,String id) {
        Site s=registry().site(id).orElse(null);
        if(s==null){source.sendFailure(Component.literal("Unknown physical site: "+id));return 0;}
        say(source,"[B3 Site] "+s.name()+" ("+s.siteId()+") X="+s.x()+" Z="+s.z()
                +" | "+s.tier()+" / "+s.archetype()+" | state="+s.habitation());
        say(source,"Population planning="+s.plannedPopulationMin()+".."+s.plannedPopulationBaseline()
                +".."+s.plannedPopulationMax()+" | material="+s.materialPalette()
                +" | related holdings="+s.holdingIds());
        say(source,"Culture="+s.cultureContext()+" | religion="+s.religionContext()
                +" | regional influence="+s.regionalInfluenceSource());
        say(source,"PLAN ONLY: no generated structures, no instantiated residents, no terrain approval.");
        return 1;
    }
    private static int regionInfo(CommandSourceStack source,String id) {
        Region r=registry().region(id).orElse(null);
        if (r==null){source.sendFailure(Component.literal("Unknown region: "+id));return 0;}
        say(source,"[B2 Region] "+r.displayName()+" ("+id+") type="+r.geographicKind()
                +" | parent="+r.parentLocationId()+" | mapped boundary/mask="+r.hasMappedBoundaryOrMask());
        say(source,"Culture weights="+r.cultureWeights()+" | religion weights="+r.religionWeights()
                +" | language weights="+r.languageWeights());
        say(source,"Domain holdings="+r.domainHoldingIds()+" | descendant physical sites="
                +r.descendantSiteIds().size()+" | NO additive population census");
        return 1;
    }

    private static int placeInfo(CommandSourceStack source,String id) {
        PlaceReference p=registry().placeReference(id).orElse(null);
        if(p==null){source.sendFailure(Component.literal("Unknown geographic reference: "+id));return 0;}
        say(source,"[B2 Place] "+p.displayName()+" ("+id+") type="+p.geographicKind()
                +" | parent="+p.parentId()+" | provenance="+p.sourceProvenance());
        say(source,"Mapped marker IDs="+p.markerIds()+" | physical sites="+p.siteIds()
                +" | political holdings="+p.holdingIds());
        if (p.markerIds().isEmpty())
            say(source,"No separate observed map marker; canonical geographic name still exists.");
        return 1;
    }

    private static int holdingInfo(CommandSourceStack source,String id) {
        Holding h=registry().holding(id).orElse(null);
        if(h==null){source.sendFailure(Component.literal("Unknown holding: "+id));return 0;}
        say(source,"[B3 Holding] "+h.name()+" ("+id+") "+h.holdingType()+" | tier="+h.tier()
                +" archetype="+h.archetype()+" | placement="+h.placementStatus());
        say(source,"Owner="+h.ownerDynastyId()+" | holder NPC="+h.holderNpcId()
                +" | parent="+h.parentHoldingId()+" | geo="+h.worldLocationId());
        say(source,"Physical sites="+h.linkedSiteIds()+" | parent-city sites="+h.referenceAnchorSiteIds()
                +" | no new residents are allocated.");
        return 1;
    }
}
