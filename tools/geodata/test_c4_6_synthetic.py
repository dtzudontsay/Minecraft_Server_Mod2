"""Standalone layout conservation smoke test, no real source files altered."""
import copy,json,random,sys,time
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from c4_6_compact_winterfell import pack

def mock():
    homes=[];work=[];build=[];res=[];households=[]
    for i in range(1136):
        ident=f'castle_home_{i+1:04d}' if i<76 else f'village5_01_home_{i+1:04d}'
        site='castle' if i<76 else 'village5_01'
        x=(-135+(i%19)*14) if i<76 else 1400+(i%30)*18
        z=(-98+(i//19)*44) if i<76 else -800+(i//30)*24
        if i in (3,4):x,z=-402+i*19,204
        h={'id':ident,'site':site,'x':x,'z':z,'w':11,'d':12,'template':'COTTAGE'}
        homes.append(copy.deepcopy(h))
        build.append({'id':ident,'site':site,'x':x,'z':z,'w':11,'d':12,'t':'COTTAGE'})
    # avoid old plot overlap checks: C4.6 ignores old coordinates and repacks except castle
    for i in range(378):
        ident=f'wf2_village5_01_work_{i:03d}';x=2000+(i%25)*24;z=1000+(i//25)*26
        w={'id':ident,'site':'village5_01','x':x,'z':z,'w':12,'d':11,'role':'FARMER' if i%4==0 else 'SMITH','template':'FARMHOUSE' if i%4==0 else 'WORKSHOP'}
        work.append(copy.deepcopy(w));build.append({'id':ident,'site':'village5_01','x':x,'z':z,'w':12,'d':11,'t':w['template']})
    for i in range(399):
        ident=f'wf3_c45_village5_01_test_{i:04d}';x=3000+(i%25)*30;z=1500+(i//25)*30
        build.append({'id':ident,'site':'village5_01','x':x,'z':z,'w':19,'d':15,'t':'C3:BREWERY'})
    for i in range(5000):res.append({'id':f'npc_{i}','homeId':homes[i%1136]['id'],'householdId':f'household_{i%1136}','job':'FARMER'})
    for i in range(1136):households.append({'id':f'household_{i}','residence':homes[i]['id']})
    v2={'residents':res,'homes':homes,'households':households,'workplaces':work,'sites':[{'id':'castle'},{'id':'village5_01'}],
        'roads':[[0,15]],'c451':{'relocatedCastleHomes':24}}
    v3={'buildings':build,'chunks':[],'c45':{'addedWorkplaces':399},'c451':{'newInnerWorkplaces':12}}
    return v2,v3

if __name__=='__main__':
    t=time.monotonic();a,b=mock();a1,b1,stats,moved=pack(a,b)
    assert len(a1['homes'])==1136 and len(a1['residents'])==5000
    assert stats['visibleFarmFields']==24 and stats['plotsAfterFields']==1937
    assert len(moved)>1800
    print('PASS synthetic population conservation, 24 fields, compact world plan')
    print(json.dumps(stats,indent=2));print('elapsed',round(time.monotonic()-t,2),'seconds')
