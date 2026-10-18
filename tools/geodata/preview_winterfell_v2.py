#!/usr/bin/env python3
"""Draw an annotated overview of the deterministic local plan; not a Minecraft screenshot."""
from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[2]
obj=json.loads((ROOT/'src/main/resources/data/knownworld/case_study/winterfell_v2.json').read_text(encoding='utf-8'))
sites=obj['sites'];homes=obj['homes'];work=obj['workplaces'];roads=obj['roads']
minx=min(min(x[0] for x in roads),min(h['x'] for h in homes));maxx=max(max(x[0] for x in roads),max(h['x']+h['w'] for h in homes))
minz=min(min(x[1] for x in roads),min(h['z'] for h in homes));maxz=max(max(x[1] for x in roads),max(h['z']+h['d'] for h in homes))
w,h=2200,2100;margin=110
scale=min((w-2*margin)/(maxx-minx),(h-2*margin)/(maxz-minz))
point=lambda x,z:(int(margin+(x-minx)*scale),int(margin+(z-minz)*scale))
im=Image.new('RGB',(w,h),(25,42,46));d=ImageDraw.Draw(im)
for x,z in roads:
    px,py=point(x,z);d.point((px,py),fill=(115,135,118))
colors={'FARMHOUSE':(183,153,96),'WORKSHOP':(172,115,81),'MERCHANT':(120,151,195),
        'COTTAGE':(194,184,157),'GUARD_QUARTER':(192,114,111),'CASTLE_QUARTER':(184,187,195)}
for plot in homes+work:
    a=point(plot['x'],plot['z']);b=point(plot['x']+plot['w'],plot['z']+plot['d'])
    c=colors.get(plot['template'],(185,180,160));d.rectangle([a,(max(a[0]+1,b[0]),max(a[1]+1,b[1]))],fill=c)
for rad in (148,171):
    a=point(-rad,-rad);b=point(rad,rad);d.rectangle([a,b],outline=(245,238,214),width=2)
for s in sites:
    px,py=point(s['cx'],s['cz']);d.ellipse((px-4,py-4,px+4,py+4),fill=(246,212,107))
    label=s['id'].replace('village5_','5× ').replace('hamlet_','1× ').replace('wintertown','10× winter town')
    d.text((px+8,py-16),label,fill=(255,251,220))
d.text((60,45),'WINTERFELL V2  |  5,000 PERSON TEST PLAN',fill=(245,238,214),stroke_width=0)
d.text((60,74),'1 castle + 1 x 10× village + 4 x 5× villages + 15 x 1× hamlets',fill=(194,206,201))
d.text((60,100),'1,136 residences  ·  378 workplaces  ·  connected road network  ·  NOT final map position',fill=(194,206,201))
target=ROOT/'docs/step3/WINTERFELL_V2_OVERHEAD_PLAN.png';target.parent.mkdir(parents=True,exist_ok=True)
im.save(target,optimize=True);print(target, im.size)
