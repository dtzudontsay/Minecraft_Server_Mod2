#!/usr/bin/env python3
"""Generate a 2D inspection diagram directly from Step 3 Winterfell plot data."""
from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[2]
q=json.loads((ROOT/'docs/step3/WINTERFELL_TEST_BUILDING_PLOTS.json').read_text('utf8'))['plots']
S=3;OX=-175;OZ=-175
X=lambda n:18+(n-OX)*S
Z=lambda n:18+(n-OZ)*S
img=Image.new('RGB',(1080,1630),'#e8e4d7');d=ImageDraw.Draw(img)
try:
    ff=ImageFont.truetype('DejaVuSans.ttf',17);title=ImageFont.truetype('DejaVuSans.ttf',26)
except OSError:ff=ImageFont.load_default();title=ff
outer=[(-145,-136),(124,-136),(124,-114),(148,-114),(148,62),(128,62),(128,140),(-102,140),(-102,125),(-150,125),(-150,-100),(-145,-100),(-145,-136)]
inner=[(-116,-100),(101,-100),(101,-80),(112,-80),(112,95),(92,95),(92,108),(-92,108),(-92,90),(-120,90),(-120,-90),(-116,-90),(-116,-100)]
for line,color,thick in [(outer,'#7b7061',17),(inner,'#484b51',18)]:
    d.line([(X(x),Z(z)) for x,z in line],fill=color,width=thick,joint='curve')
colors={'LORDLY_RESIDENCE':'#822c31','FEASTING_HALL':'#a45036','ANCIENT_KEEP':'#72617b','SACRED_GODS_WOOD':'#49794b','HEATED_GREENHOUSE':'#8cc6a3','GEOTHERMAL_POOL':'#75b5c1'}
for plot in q:
    name=plot['id'].removeprefix('winterfell_');kind=plot['kind'];v=plot['bounds']
    if kind=='DEFENSIVE_ENVELOPE':continue
    box=(X(v['xmin']),Z(v['zmin']),X(v['xmax']),Z(v['zmax']))
    if kind in ('ROAD','SETTLEMENT_ROAD'):d.rectangle(box,fill='#b4a88b')
    elif kind=='GEOTHERMAL_POOL':d.ellipse(box,fill='#6bacc6')
    else:d.rectangle(box,fill=colors.get(kind,'#b2a38e'),outline='#5b5550',width=2)
    if kind not in ('ROAD','SETTLEMENT_ROAD') and not name.startswith('wintertown_cottage'):
        # Suppress crypt label since it is underground, partly beneath structures.
        if kind=='ANCIENT_CRYPT':continue
        d.text((box[0]+3,box[1]+3),name.replace('_',' '),font=ff,fill='#1d2529')
d.text((24,10),'WINTERFELL — STEP 3 BESPOKE TEST-ONLY STRUCTURE PLAN',font=title,fill='#192832')
d.text((24,46),'1 block = 1 metre · 298 AC · awaiting actual terrain survey · NO NPCs',font=ff,fill='#444d4f')
d.text((X(3),Z(148)),"SOUTH GATE",font=title,fill='#242d34')
d.text((X(12),Z(243)),"WINTER TOWN",font=title,fill='#242d34')
d.text((26,1580),'55 permanent plot IDs · 34 seasonally occupied cottage templates · render source is generator JSON',font=ff,fill='#3b4245')
out=ROOT/'docs/step3/WINTERFELL_TEST_OVERHEAD_PLAN.png';out.parent.mkdir(parents=True,exist_ok=True);img.save(out)
print(out)
