"""Optional synthetic end-to-end installer test (does not touch the real repository)."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from test_patch import ACTIVATION, DIALOGUE

with tempfile.TemporaryDirectory(prefix='av4-qol2-test-') as tmp:
    root=Path(tmp)/'repo'
    pop=root/'src/main/java/dev/dtzudontsay/knownworld/simulation/population'
    pop.mkdir(parents=True)
    (root/'.git').mkdir()
    (root/'gradlew.bat').write_text('rem synthetic fake gradle\n')
    dest=root/'tools/av4_qol2'
    dest.mkdir(parents=True)
    orig=Path(__file__).parent
    for name in ('av4_qol2.py','actor_methods.java.txt'):
        shutil.copy2(orig/name,dest/name)
    a=pop/'KnownWorldB41Activation.java';d=pop/'KnownWorldB41DialogueService.java'
    a.write_text(ACTIVATION,encoding='utf-8',newline='\r\n')
    d.write_text(DIALOGUE,encoding='utf-8',newline='\r\n')
    fakebin=Path(tmp)/'bin';fakebin.mkdir()
    fcmd=fakebin/'cmd.exe'
    fcmd.write_text('#!/bin/sh\nexit ${MOCK_GRADLE_EXIT:-0}\n')
    fcmd.chmod(0o755)
    env=os.environ.copy();env['PATH']=str(fakebin)+os.pathsep+env['PATH']
    program=dest/'av4_qol2.py'
    def run(op,fail=False):
        custom={**env,'MOCK_GRADLE_EXIT':('1' if fail else '0')}
        return subprocess.run([sys.executable,str(program),op],cwd=root,env=custom,
                capture_output=True,text=True)
    initial=(a.read_bytes(),d.read_bytes())
    status=run('--check');assert status.returncode==0,status.stderr
    assert (a.read_bytes(),d.read_bytes())==initial
    failed=run('--install',True);assert failed.returncode==1,failed.stderr
    assert (a.read_bytes(),d.read_bytes())==initial, 'Failed installation did not restore originals'
    good=run('--install');assert good.returncode==0,good.stderr
    assert b'AV4_QOL_V2_BEGIN' in a.read_bytes() and b'AV4_QOL_V2_DIALOGUE' in d.read_bytes()
    assert b'\r\n' in a.read_bytes() and b'\r\n' in d.read_bytes()
    again=run('--check');assert again.returncode==0 and 'ALREADY INSTALLED' in again.stdout
    print('PASS: --check did not mutate source; compile failure restored both Java files; compile-success install preserved CRLF and is idempotent.')
