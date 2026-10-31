"""Non-invasive synthetic contract checks; never opens or writes Minecraft saves."""
import importlib.util
from pathlib import Path
import unittest

SCRIPT = Path(__file__).with_name('av4_qol2.py')
spec = importlib.util.spec_from_file_location('qol2_patch_tested',SCRIPT)
qol = importlib.util.module_from_spec(spec)
spec.loader.exec_module(qol)

ACTIVATION = '''package example;
import com.google.gson.JsonParser;
public class Example {
KnownWorldAv3Formation.register();
KnownWorldAv4Decisions.register();
CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->
                dispatcher.register(Commands.literal("kwactors")
                        .then(Commands.literal("status").executes(ctx->status(ctx.getSource())))
                        .then(Commands.literal("nearby").executes(ctx->nearby(ctx.getSource())))));
    private static void inspect(ServerPlayer player,Entity actor) {
        if(player.distanceToSqr(actor)>64)return;
    }
    private static void say(CommandSourceStack src,String s){src.sendSuccess(()->Component.literal(s),false);}
}
'''
DIALOGUE = '''private static final String DEFAULT_MODEL = "qwen3.5:9b";
options.addProperty("num_predict",384);
Reply in first person, usually in one to four sentences. No markdown or stage directions.
If asked about something unknown, admit uncertainty naturally.
for(String key:new String[]{"personality","traits","dialogueStyle","characterValues","socialNorms","skills","derivedTendencies","av4FreeTimeDecision","newFutureValue"}) {
}
'''

class TestPatcher(unittest.TestCase):
    def test_existing_av4_fields_preserved_and_new_attributes_added(self):
        out = qol.patched_dialogue(DIALOGUE)
        self.assertIn('"av4FreeTimeDecision"',out)
        self.assertIn('"newFutureValue"',out)
        self.assertIn('"dispositions"',out)
        self.assertIn('"aptitudes"',out)
        self.assertIn('qwen3.5:27b',out)
        self.assertEqual(out, qol.patched_dialogue(out))
    def test_commands_are_admin_only_and_dont_rewrite_inspection(self):
        out = qol.patched_activation(ACTIVATION)
        self.assertIn('Commands.literal("goto")',out)
        self.assertIn('Commands.literal("sheet")',out)
        self.assertIn('Commands.LEVEL_GAMEMASTERS',out)
        self.assertIn('inspect(player, actor, false)',out)
        self.assertIn('AV4_QOL_V2_BEGIN',out)
        self.assertEqual(out,qol.patched_activation(out))
    def test_unknown_anchors_block(self):
        with self.assertRaises(qol.Blocked):
            qol.patched_dialogue(DIALOGUE.replace('qwen3.5:9b','custom-model'))
    def test_old_inspector_not_silently_removed(self):
        with self.assertRaises(qol.Blocked):
            qol.patched_activation(ACTIVATION.replace('if(player.distanceToSqr(actor)>64)return;', 'if(false)return;'))

if __name__=='__main__': unittest.main(verbosity=2)
