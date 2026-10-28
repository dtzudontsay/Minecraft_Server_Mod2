import importlib.util, json, pathlib, subprocess, tempfile, unittest, shutil
BASE=pathlib.Path(__file__).resolve().parents[3]
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
WORK=module('b4_2_workplace_index',BASE/'tools/geodata/b4_2_workplace_index.py')
INSTALL=module('b4_2_install',BASE/'tools/geodata/b4_2_install.py')
JAVA=BASE/'src/main/java/dev/dtzudontsay/knownworld/simulation/population/KnownWorldB42Schedule.java'
class B42Tests(unittest.TestCase):
    def test_legitimate_decriptive_site_id(self):
        self.assertTrue(WORK.valid('site_nw_missing_twins_01'))
        self.assertFalse(WORK.valid('site_../../saves'))
    def test_worksite_no_house(self):
        with tempfile.TemporaryDirectory() as d:
            base=pathlib.Path(d)/WORK.TILES/'site_sw_001';base.mkdir(parents=True)
            content={'siteId':'site_sw_001','lots':[{'id':'site_sw_001:home:1','kind':'HOUSE','permission':'C3_MODULE_AVAILABLE','x':0,'z':0,'w':10,'d':10,'s':0},
                {'id':'site_sw_001:work:SMITHY:1','kind':'SMITHY','permission':'C3_MODULE_AVAILABLE','x':12,'z':12,'w':15,'d':12,'s':6},
                {'id':'site_sw_001:work:SMITHY:1','kind':'SMITHY','permission':'C3_MODULE_AVAILABLE','x':12,'z':12,'w':15,'d':12,'s':6},
                {'id':'site_sw_001:work:DEFERRED:1','kind':'SMITHY','permission':'TEMPLATE_DEFERRED','x':20,'z':30,'w':3,'d':3,'s':2}]}
            (base/'0_0.json').write_text(json.dumps(content))
            obj,_=WORK.index_site(pathlib.Path(d),{'siteId':'site_sw_001','tiles':['0_0']})
            self.assertEqual(1,len(obj['workplaces']));self.assertEqual(6,obj['workplaces'][0]['maxStaff'])
    def test_collision_fails_closed(self):
        with tempfile.TemporaryDirectory() as d:
            base=pathlib.Path(d)/WORK.TILES/'site_sw_001';base.mkdir(parents=True)
            def row(x):return {'siteId':'site_sw_001','lots':[{'id':'site_sw_001:work:1','kind':'SMITHY','permission':'C3_MODULE_AVAILABLE','x':x,'z':2,'w':4,'d':4,'s':6}]}
            (base/'a.json').write_text(json.dumps(row(1)));(base/'b.json').write_text(json.dumps(row(3)))
            with self.assertRaises(ValueError):WORK.index_site(pathlib.Path(d),{'siteId':'site_sw_001','tiles':['a','b']})
    def test_install_requires_b41b(self):
        with self.assertRaises(ValueError):INSTALL.patch_activation('hello world')
    def test_installer_preserves_stable_ids(self):
        text=(BASE/'tools/geodata/b4_2_install.py').read_text()
        self.assertNotIn('b4_0_world_population.generate(',text)
        self.assertNotIn('shutil.rmtree(root/',text)
    def test_pure_java_schedule(self):
        if not shutil.which('javac') or not shutil.which('java'):
            self.skipTest('Java JDK is not on PATH; Gradle will perform the full compile')
        with tempfile.TemporaryDirectory() as d:
            p=pathlib.Path(d)/'ScheduleCheck.java'
            p.write_text('''import dev.dtzudontsay.knownworld.simulation.population.KnownWorldB42Schedule;
public class ScheduleCheck {
 static void ok(boolean yes){if(!yes)throw new AssertionError();}
 public static void main(String[] args){
 long day=109000;
 var first=KnownWorldB42Schedule.evaluate("b4:site_sw_001:00000001","Farmer",260,298,day,3500,day,true,60);
 ok(first.scheduledWorkday() || first.activity().equals("REST_DAY"));
 ok(first.modeledWorkdays()==0);
 var second=KnownWorldB42Schedule.evaluate("b4:site_sw_001:00000001","Farmer",260,298,day+14,3500,day,true,60);
 ok(second.modeledWorkdays()==12);
 ok(second.practiceGain()>0);
 ok(KnownWorldB42Schedule.evaluate("b4:site_sw_001:00000001",null,289,298,day,4000,day,false,0).activity().equals("HOME_AND_CARE"));
 ok(KnownWorldB42Schedule.evaluate("b4:site_sw_001:00000001","Smith",260,298,day,21000,day,true,60).activity().equals("SLEEP"));
 try{KnownWorldB42Schedule.evaluate("",null,289,298,day,0,day,false,0);throw new AssertionError();}
 catch(IllegalArgumentException success){}
 System.out.println("B4.2 schedule assertions passed");
 }}''')
            src=pathlib.Path(d)/'src';src.mkdir()
            out=pathlib.Path(d)/'classes';out.mkdir()
            cmd=['javac','-d',str(out),str(JAVA),str(p)]
            subprocess.run(cmd,check=True,capture_output=True,text=True)
            result=subprocess.run(['java','-cp',str(out),'ScheduleCheck'],check=True,capture_output=True,text=True)
            self.assertIn('passed',result.stdout)
if __name__=='__main__':unittest.main()
