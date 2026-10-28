package dev.dtzudontsay.knownworld.world.generation.settlement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * C4.9 standalone, deterministic terrain-anchor preflight policy.
 *
 * Connect this to the existing authored relief and land samplers in a FUTURE
 * offline site-plan compile step, never during every chunk decoration.
 * This class reserves no blocks and does NOT authorize Minecraft construction.
 * Main-core plot and every later lot still require the existing C2 per-lot survey.
 */
public final class KnownWorldC49TerrainPolicy {
    private KnownWorldC49TerrainPolicy() { }

    public interface TerrainSampler {
        boolean isDryLand(int x, int z);
        int surfaceHeight(int x, int z);
    }

    public record Site(String siteId, String name, List<String> markerIds,
                       int originalX, int originalZ, String tier,
                       int population, int homes, int workplaces) {
        public Site {
            Objects.requireNonNull(siteId);
            Objects.requireNonNull(name);
            markerIds = List.copyOf(markerIds);
            if (population < 0 || homes < 0 || workplaces < 0)
                throw new IllegalArgumentException("Negative site demand");
        }
    }

    /**
     * searchRadius: permitted MOVE of physical anchor, never a city size cap.
     * coreRadius: half-width of the main complex clearance in blocks.
     * expansionRadius: reserve for initial city streets and inland expansion.
     * requiredDryCells: viable CONNECTED cells, not nearest single land block.
     */
    public record Policy(int searchRadius, int coreRadius, int expansionRadius,
                         int samplingStride, int requiredDryCells,
                         int maximumCoreRelief, int maximumLocalStep) {
        public Policy {
            if (searchRadius < 0 || coreRadius < 0 || expansionRadius <= coreRadius ||
                samplingStride < 1 || requiredDryCells < 1 || maximumCoreRelief < 0 ||
                maximumLocalStep < 0 || requiredDryCells >
                (2 * (expansionRadius / samplingStride) + 1) *
                (2 * (expansionRadius / samplingStride) + 1))
                throw new IllegalArgumentException("Invalid C4.9 terrain policy");
        }

        public static Policy defaultFor(String tier) {
            // Central keep/civic core only. The rest of the city is streamed
            // unbounded from housing/job demand after lot-level approvals.
            int core = switch (tier) {
                case "SMALL" -> 24;
                case "MEDIUM" -> 40;
                case "MAJOR" -> 96;
                default -> 40;
            };
            return new Policy(2048, core, core + 256, 16, 145, 28, 8);
        }
    }

    public record Placement(String siteId, int originalX, int originalZ,
                            int constructionX, int constructionZ,
                            int inlandDx, int inlandDz, int connectedDryCells,
                            int coreRelief, boolean terracesRequired,
                            boolean retainingWallsRequired,
                            boolean switchbackRoadsRequired,
                            String status) { }

    public record Failure(String siteId, String settlementName,
                          List<String> markerIds, int originalX, int originalZ,
                          int attemptedX, int attemptedZ,
                          String tier, int population, int homesDeferred,
                          int workplacesDeferred, String reason) { }

    public record Result(Placement placement, Failure failure) {
        public Result {
            if ((placement == null) == (failure == null))
                throw new IllegalArgumentException("Exactly one outcome required");
        }
        public boolean successful() { return placement != null; }
    }

    public static Result find(Site site, TerrainSampler terrain, Policy policy) {
        Objects.requireNonNull(site);
        Objects.requireNonNull(terrain);
        Objects.requireNonNull(policy);
        int stride = policy.samplingStride();
        List<int[]> candidateOffsets = new ArrayList<>();
        // No intrinsic limit on the city's final width/number of houses.
        // Search only where its initial physical anchor may be moved.
        for (int dx = -policy.searchRadius(); dx <= policy.searchRadius(); dx += stride) {
            for (int dz = -policy.searchRadius(); dz <= policy.searchRadius(); dz += stride) {
                if ((long)dx * dx + (long)dz * dz <=
                    (long) policy.searchRadius() * policy.searchRadius()) {
                    candidateOffsets.add(new int[]{dx, dz});
                }
            }
        }
        candidateOffsets.sort(Comparator.<int[]>comparingLong(d ->
            (long)d[0] * d[0] + (long)d[1] * d[1])
            .thenComparingInt(d -> d[0]).thenComparingInt(d -> d[1]));
        String lastRejection = "NO_CONTIGUOUS_DRY_LAND";
        int lastX = site.originalX(), lastZ = site.originalZ();
        for (int[] delta : candidateOffsets) {
            int x = site.originalX() + delta[0];
            int z = site.originalZ() + delta[1];
            lastX = x; lastZ = z;
            Core c = inspectCore(x, z, terrain, policy);
            if (!c.safe) {
                lastRejection = c.reason;
                continue;
            }
            DryRegion region = contiguousDryExpansion(x,z,terrain,policy);
            if (region.cellCount < policy.requiredDryCells()) {
                lastRejection = "INSUFFICIENT_CONNECTED_INLAND_EXPANSION";
                continue;
            }
            boolean terraces = c.relief > 5;
            int dx = Integer.compare(region.sumX, x);
            int dz = Integer.compare(region.sumZ, z);
            if (dx == 0 && dz == 0) {
                // On very broad dry land either direction is safe by this survey.
                // Favor the fixed canonical +Z direction for repeatability.
                dz = 1;
            }
            return new Result(new Placement(site.siteId(), site.originalX(),site.originalZ(),
                    x,z,dx,dz,region.cellCount,c.relief,terraces,terraces,
                    terraces,"ANCHOR_PREFLIGHT_ONLY_EACH_LOT_MUST_STILL_BE_SURVEYED"),null);
        }
        return new Result(null,new Failure(site.siteId(),site.name(),site.markerIds(),
                    site.originalX(),site.originalZ(),lastX,lastZ,
                    site.tier(),site.population(),site.homes(),site.workplaces(),
                    lastRejection));
    }

    private record Core(boolean safe, int relief, String reason) { }
    private static Core inspectCore(int cx,int cz,TerrainSampler terrain,Policy p) {
        int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;
        int stride=p.samplingStride();
        // Overscan one stride at the edge; avoids approving a single dry pixel.
        for (int x=cx-p.coreRadius();x<=cx+p.coreRadius();x+=stride) {
            for (int z=cz-p.coreRadius();z<=cz+p.coreRadius();z+=stride) {
                if (!terrain.isDryLand(x,z))
                    return new Core(false,0,"COAST_OR_WATER_INTERSECTS_MAIN_COMPLEX");
                int h=terrain.surfaceHeight(x,z);
                lo=Math.min(lo,h);hi=Math.max(hi,h);
                if (x+stride<=cx+p.coreRadius() &&
                    Math.abs(h-terrain.surfaceHeight(x+stride,z))>p.maximumLocalStep())
                    return new Core(false,hi-lo,"CORE_CLIFF_EXCEEDS_TERRACE_STEP");
                if (z+stride<=cz+p.coreRadius() &&
                    Math.abs(h-terrain.surfaceHeight(x,z+stride))>p.maximumLocalStep())
                    return new Core(false,hi-lo,"CORE_CLIFF_EXCEEDS_TERRACE_STEP");
            }
        }
        int relief=hi-lo;
        if (relief>p.maximumCoreRelief())
            return new Core(false,relief,"CENTRAL_KEEP_REQUIRES_MORE_THAN_SAFE_TERRACING");
        return new Core(true,relief,"OK");
    }

    private record DryRegion(int cellCount, int sumX, int sumZ) { }
    private static DryRegion contiguousDryExpansion(int cx,int cz,TerrainSampler terrain,Policy p) {
        int stride=p.samplingStride();
        int n=p.expansionRadius()/stride;
        int width=2*n+1;
        boolean[] seen=new boolean[width*width];
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        int center=n*width+n;
        seen[center]=true;
        queue.add(center);
        int count=0;
        long sumX=0,sumZ=0;
        final int[] ddx={1,-1,0,0};
        final int[] ddz={0,0,1,-1};
        while(!queue.isEmpty()) {
            int index=queue.removeFirst();
            int a=index%width,b=index/width;
            int x=cx+(a-n)*stride,z=cz+(b-n)*stride;
            if(!terrain.isDryLand(x,z)) continue;
            count++;sumX+=x;sumZ+=z;
            for(int k=0;k<4;k++) {
                int nx=a+ddx[k],nz=b+ddz[k];
                if(nx<0||nz<0||nx>=width||nz>=width)continue;
                int next=nz*width+nx;
                if(!seen[next]){seen[next]=true;queue.addLast(next);}
            }
        }
        // directional averages are normalized to avoid overflowing int at
        // >4m coordinate positions with thousands of sampled cells.
        return new DryRegion(count, count==0?cx:(int)(sumX/count),
                                    count==0?cz:(int)(sumZ/count));
    }
}
