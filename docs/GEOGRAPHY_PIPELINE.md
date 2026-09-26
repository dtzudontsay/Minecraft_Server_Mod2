# Known World Geography Pipeline

## Locked rules

- Minecraft version: 26.3
- Loader: Fabric
- Horizontal scale: 1 block = 1 metre
- Primary canon: books and official ASOIAF cartography

## Minecraft coordinate convention

- +X = east
- -X = west
- -Z = north
- +Z = south

## Geographic confidence

Every future geographic feature should be classified as:

- CANON
- STRONGLY_INFERRED
- INTERPOLATED

## Source-map pipeline

1. Select one exact master map image.
2. Preserve its original dimensions.
3. Record canonical anchor locations.
4. Determine map scale.
5. Convert map pixels into world metres.
6. Validate landmark distances.
7. Build coastline data.
8. Add elevation.
9. Add rivers.
10. Add regions, roads and settlements.

The next milestone is choosing and calibrating the exact master map image.