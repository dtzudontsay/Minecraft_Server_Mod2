package dev.dtzudontsay.knownworld.simulation.economy;

import dev.dtzudontsay.knownworld.simulation.world.settlement.SettlementId;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An intentionally NON-AUTOMATED native settlement->verified NPC transaction
 * bridge. Cannot invent B4.0 NpcId objects, create new balances, grant wages
 * to provisional household IDs, or run on every clock tick.
 *
 * All records are transfers, so conserved grain/coin stay conserved.
 * Deliberately limited to 64 entries per explicit verified settlement/day.
 * An exclusive pending receipt is written BEFORE economic mutations. Failed
 * save leaves receipt pending and blocks retry; manual recovery required.
 *
 * Caller MUST supply already verified native inventory IDs / recipient NPC IDs.
 */
public final class KnownWorldB42B4NativeDailyTransactions {
    public record Transfer(InventoryId destination, ResourceId resource,
                           long quantity, String reason) {
        public Transfer {
            Objects.requireNonNull(destination,"destination");
            Objects.requireNonNull(resource,"resource");
            if(quantity<=0)throw new IllegalArgumentException("Quantity must be positive");
            if(reason==null || reason.isBlank() || reason.length()>150)
                throw new IllegalArgumentException("Invalid verified activity reason");
            if(!resource.value().equals("coin") && !resource.value().equals("grain"))
                throw new IllegalArgumentException("Only standard coin/grain are supported");
        }
    }
    public record Preview(long grainKg,long coinUnits,int operations,boolean executable) { }
    private KnownWorldB42B4NativeDailyTransactions(){ }

    /** All preflight checks run under the manager monitor, without any writes. */
    public static Preview preflight(EconomyRuntime runtime, SettlementId site,
                                    InventoryId source, List<Transfer> transfers) {
        Objects.requireNonNull(runtime,"economy");Objects.requireNonNull(site,"canonical settlement");
        Objects.requireNonNull(source,"treasury inventory");Objects.requireNonNull(transfers,"transfers");
        if(runtime.authorityMode()!=EconomyAuthorityService.Mode.SNAPSHOT_PRIMARY)
            throw new IllegalStateException("Snapshot economic authority not active");
        if(transfers.isEmpty() || transfers.size()>64)throw new IllegalArgumentException("Verify 1..64 native transfers per operation");
        InventoryManager manager=runtime.inventories();
        synchronized(manager) {
            if(manager.remainingJournalCapacity()<transfers.size())
                throw new IllegalStateException("Journal capacity too low for full verified transaction");
            ResourceInventory original=manager.require(source);
            if(original.owner().kind()!=EconomicOwner.Kind.SETTLEMENT ||
                original.owner().id()!=site.value() || !original.location().equals(site))
                throw new IllegalStateException("Source is not canonical settlement-owned inventory");
            Map<ResourceId,Long> required=new HashMap<>();
            Map<String,Long> credited=new HashMap<>();
            long g=0,c=0;
            for(Transfer t:transfers){
                if(t.destination().equals(source))throw new IllegalArgumentException("Cannot transfer to same inventory");
                ResourceInventory dest=manager.require(t.destination());
                if(dest.owner().kind()!=EconomicOwner.Kind.NPC || !dest.location().equals(site))
                    throw new IllegalStateException("Recipient is not a native NPC inventory at this settlement");
                required.merge(t.resource(),t.quantity(),Math::addExact);
                String key=t.destination().value()+":"+t.resource().value();
                credited.merge(key,t.quantity(),Math::addExact);
                if(t.resource().value().equals("grain"))g=Math.addExact(g,t.quantity());
                else c=Math.addExact(c,t.quantity());
            }
            for(var entry:required.entrySet())
                if(original.quantity(entry.getKey())<entry.getValue())
                    throw new IllegalStateException("Verified source has insufficient stock: "+entry.getKey());
            for(Transfer t:transfers) {
                String key=t.destination().value()+":"+t.resource().value();
                Math.addExact(manager.require(t.destination()).quantity(t.resource()),credited.get(key));
            }
            return new Preview(g,c,transfers.size(),true);
        }
    }

    /**
     * Explicit coordinator integration point. There is deliberately NO tick
     * event or chat command calling this method until cross-domain household
     * identity and save durability have been established.
     */
    public static Preview commitVerifiedDay(EconomyRuntime runtime, Path worldDataDirectory,
                                             SettlementId site, long campaignDay,
                                             InventoryId source, List<Transfer> transfers) throws IOException {
        if(campaignDay<0)throw new IllegalArgumentException("Negative campaign day");
        Objects.requireNonNull(worldDataDirectory,"world data directory");
        if(Files.isSymbolicLink(worldDataDirectory))throw new IOException("Linked world directory rejected");
        Path receipts=worldDataDirectory.resolve("b42b4_native_verified_daily_receipts");
        if(Files.isSymbolicLink(receipts))throw new IOException("Linked receipts directory rejected");
        String name="settlement_"+site.value()+"_day_"+campaignDay;
        Path pending=receipts.resolve(name+".pending");
        Path committed=receipts.resolve(name+".committed");
        InventoryManager manager=runtime.inventories();
        synchronized(manager) {
            if(Files.exists(pending,LinkOption.NOFOLLOW_LINKS) || Files.exists(committed,LinkOption.NOFOLLOW_LINKS))
                throw new IOException("Existing day receipt: NEVER replay or delete without independent reconciliation");
            Preview p=preflight(runtime,site,source,transfers);
            Files.createDirectories(receipts);
            // Audit record is durable before mutations. A pending record permanently
            // blocks replay after any interruption until independently reconciled.
            StringBuilder audit=new StringBuilder("B4.2B.4_NATIVE_DAILY_PENDING\nsite=")
                .append(site.value()).append("\nday=").append(campaignDay)
                .append("\nsourceInventory=").append(source.value())
                .append("\ngrain=").append(p.grainKg()).append("\ncoin=").append(p.coinUnits()).append('\n');
            for(Transfer t:transfers)audit.append(t.destination().value()).append('\t')
                .append(t.resource().value()).append('\t').append(t.quantity()).append('\t')
                .append(t.reason().replace('\n',' ').replace('\r',' ')).append('\n');
            try(FileChannel f=FileChannel.open(pending,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
                ByteBuffer b=ByteBuffer.wrap(audit.toString().getBytes(StandardCharsets.UTF_8));
                while(b.hasRemaining())f.write(b);
                f.force(true);
            }
            // Every transfer goes through InventoryManager's existing journal.
            for(Transfer t:transfers)manager.transfer(source,t.destination(),t.resource(),t.quantity(),
                "B4.2B.4 verified day "+campaignDay+" settlement "+site.value()+": "+t.reason());
            // Native verified snapshot is the transaction authority. An interrupted
            // receipt prevents replay even if this save fails after some mutation.
            runtime.commitB42B3SnapshotNow();
            try { Files.move(pending,committed,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) {
                throw new IOException("Economy checkpoint committed, but receipt publish unavailable. DO NOT RETRY.",e);
            }
            return p;
        }
    }
}
