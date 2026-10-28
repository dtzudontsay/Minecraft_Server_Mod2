import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
SPEC=importlib.util.spec_from_file_location('b4_1b_install',ROOT/'tools/geodata/b4_1b_install.py')
mod=importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(mod)
ACT=ROOT/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB41Activation.java'

PKT='''package demo;
import dev.dtzudontsay.knownworld.simulation.NpcSimulation;
public class WinterfellConversationPackets {
    public static void registerCommon() {
        ServerPlayNetworking.registerGlobalReceiver(OpenRequest.TYPE, (request, context) -> {
            ServerPlayer player = context.player();
            WinterfellConversationService.open(player, request.entityId());
            lockActor(player, request.entityId());
        });
        ServerPlayNetworking.registerGlobalReceiver(ChatRequest.TYPE, (request, context) ->
                WinterfellConversationService.send(context.player(),
                        request.session(), request.text()));
        ServerPlayNetworking.registerGlobalReceiver(CloseRequest.TYPE, (request, context) -> {
            ServerPlayer player = context.player();
            WinterfellConversationService.close(player, request.entityId());
        });
    }
    private static void lockActor(ServerPlayer player, int entityId) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Entity actor = level.getEntity(entityId);
        if (!(actor instanceof Villager villager)
                || !actor.isAlive()
                || !actor.entityTags().contains(ACTOR_TAG)
                || player.distanceToSqr(actor) > MAX_RANGE_SQUARED) return;
        String authored = null;
        for (String tag : actor.entityTags()) {
            if (tag.startsWith(ID_PREFIX)) {
                authored = tag.substring(ID_PREFIX.length());
                break;
            }
        }
        if (authored == null) return;
        NpcSimulation sim = NpcSimulation.getNullable();
        NpcId npcId = sim == null ? null :
                sim.authoredIds().findNpc(authored).orElse(null);
        if (npcId == null || sim.registry().find(npcId)
                .filter(npc -> npc.isAlive()).isEmpty()) return;
    }
}
'''
UI='''public class WinterfellDialogueScreen {
    void render() {
        gfx.text(font,
                "WINTERFELL  /  CONVERSATION",
                x,y,color,true);
    }
}'''

class InstallTests(unittest.TestCase):
    def test_packet_routing_rewrites_once(self):
        s=mod.patch_packets(PKT)
        self.assertEqual(s,mod.patch_packets(s))
        self.assertIn('KnownWorldB41DialogueService.open',s)
        self.assertIn('WinterfellConversationService.open',s)
        self.assertIn('KnownWorldB41DialogueService.send',s)
        self.assertIn('KnownWorldB41DialogueService.validActor',s)

    @unittest.skipUnless(ACT.is_file(), "installed B4.1A.1/C4.15 source unavailable in isolated build")
    def test_projection_reuse_exact_person_and_seed(self):
        raw=ACT.read_text('utf-8')
        patched=mod.patch_activation(raw)
        self.assertEqual(patched,mod.patch_activation(patched))
        self.assertIn('p.id,p.name',patched)
        self.assertIn('actor.getUUID().equals(projected.get(p.id))',patched)
        self.assertIn('enrich(profile,p,site)',patched)
        self.assertIn('Winterfell V3',patched)

    def test_screen_preserves_winterfell_label(self):
        src=mod.patch_ui(UI)
        self.assertIn('subtitle.startsWith("Known World | ")',src)
        self.assertIn('WINTERFELL  /  CONVERSATION',src)
        self.assertEqual(src,mod.patch_ui(src))

    def test_bad_anchors_fail_without_writing(self):
        with self.assertRaises(ValueError):mod.patch_packets('broken')
        with self.assertRaises(ValueError):mod.patch_activation('bad')
        with self.assertRaises(ValueError):mod.patch_ui('bad')

    @unittest.skipUnless(ACT.is_file(), "installed B4.1A.1/C4.15 source unavailable in isolated build")
    def test_realistic_installer_check_and_backups(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            for rel,text in ((mod.PKT,PKT),(mod.ACT,ACT.read_text('utf-8')),(mod.UI,UI),
                             (mod.SERVICE,'package demo;'),(mod.TRUTH,'package demo;')):
                path=root/rel;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,'utf-8')
            before=(root/mod.PKT).read_bytes()
            mod.apply(root,True)
            self.assertEqual((root/mod.PKT).read_bytes(),before)
            mod.apply(root,False)
            self.assertNotEqual((root/mod.PKT).read_bytes(),before)
            second=(root/mod.PKT).read_bytes()
            mod.apply(root,False)
            self.assertEqual((root/mod.PKT).read_bytes(),second)
            backups=list((root/'tools/geodata/output/b4_1b_source_backups').rglob('WinterfellConversationPackets.java'))
            self.assertEqual(len(backups),1)
            self.assertEqual(backups[0].read_bytes(),before)

if __name__=='__main__':unittest.main()

class SourceSafetyTests(unittest.TestCase):
    def test_current_user_turn_is_sent_to_model(self):
        s=(ROOT/mod.SERVICE).read_text('utf-8')
        self.assertIn('request = body(session, player, words)',s)
        self.assertIn('current.addProperty("content",currentWords)',s)
        self.assertIn('SESSIONS.get(session.player)!=session',s)
        self.assertIn('LEDGER.remember(session.identity.id(),session.player',s)

    def test_no_population_recreation_or_trusting_llm_as_event(self):
        s=(ROOT/mod.SERVICE).read_text('utf-8')
        for forbidden in ('registry().create(', 'sim.events().create(', 'sim.knowledge().believe(', 'population.generate(', 'registerNpc('):
            self.assertNotIn(forbidden,s)
        self.assertIn('Never tell the player that you did something',s)

    def test_per_person_and_player_store(self):
        s=(ROOT/mod.SERVICE).read_text('utf-8')
        self.assertIn('LEDGER.history(person.id(), player.getUUID())',s)
        self.assertIn('RELATIONS.view(person.id(), player.getUUID()',s)
        self.assertIn('KnownWorldB41DialogueTruth.review',s)
