package dev.dtzudontsay.knownworld.world.geography.zones;

public enum MapZoneId {

    NW("North Westeros"),
    SW("South Westeros"),
    SI("Summer Isles"),

    NWE("North-West Essos"),
    SWE("South-West Essos"),
    NE("North Essos"),
    SE("South Essos"),
    NEE("North-East Essos"),
    SEE("South-East Essos"),

    SO("Sothoryos"),
    UL("Ulthos");

    private final String displayName;

    MapZoneId(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static MapZoneId fromCode(String code) {
        for (MapZoneId id : values()) {
            if (id.name().equalsIgnoreCase(code)) {
                return id;
            }
        }

        throw new IllegalArgumentException(
                "Unknown map zone: " + code
        );
    }
}