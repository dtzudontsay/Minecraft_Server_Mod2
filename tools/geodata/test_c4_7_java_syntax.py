#!/usr/bin/env python3
"""Compile architectural details against harmless stubs, catching syntax errors.
Not a Minecraft Gradle compile and not proof all Minecraft API names exist.
"""
from pathlib import Path
import re,subprocess,tempfile,csv
S=Path(__file__).resolve().parents[0]/'templates/KnownWorldC47ArchitecturalDetails.java.txt'
s=S.read_text(encoding='utf-8')
blocks=set(re.findall(r'Blocks\.([A-Z][A-Z0-9_]+)',s))
if 'ORANGE_STAINED_GLASS' in blocks: raise SystemExit('STOP: obsolete 26.3 constant ORANGE_STAINED_GLASS')
with tempfile.TemporaryDirectory() as d:
 root=Path(d)
 def write(rel,data):
  f=root/rel;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(data,encoding='utf8');return f
 classrel='dev/dtzudontsay/knownworld/world/generation/settlement'
 write('net/minecraft/core/Direction.java','package net.minecraft.core; public enum Direction { EAST,WEST,NORTH,SOUTH; public enum Axis {X,Y,Z;} }')
 write('net/minecraft/world/level/block/state/BlockState.java','package net.minecraft.world.level.block.state; public class BlockState { public <T> BlockState setValue(Object property, T value){return this;} }')
 write('net/minecraft/world/level/block/BedBlock.java','package net.minecraft.world.level.block; public class BedBlock {public static final Object FACING=new Object();public static final Object PART=new Object();}')
 write('net/minecraft/world/level/block/state/properties/BedPart.java','package net.minecraft.world.level.block.state.properties; public enum BedPart { FOOT, HEAD; }')
 write('net/minecraft/world/level/block/RotatedPillarBlock.java','package net.minecraft.world.level.block; public class RotatedPillarBlock {public static final Object AXIS=new Object();}')
 write('net/minecraft/world/level/block/LadderBlock.java','package net.minecraft.world.level.block; public class LadderBlock { public static final Object FACING = new Object(); }')
 write('net/minecraft/world/level/chunk/ChunkAccess.java','package net.minecraft.world.level.chunk; public class ChunkAccess {}')
 bnames=sorted(blocks-{'WOOL','BED'})
 block_txt='package net.minecraft.world.level.block;\nimport net.minecraft.world.level.block.state.BlockState;\npublic class Blocks { public static class Block {public BlockState defaultBlockState(){return new BlockState();}} '
 block_txt+='public static class Bed {public Block red(){return new Block();}} public static final Bed BED=new Bed();\n'
 block_txt+='public static class Wool {public Block red(){return new Block();} public Block white(){return new Block();} public Block brown(){return new Block();}} public static final Wool WOOL = new Wool();\n'
 block_txt+='\n'.join('public static final Block '+x+'=new Block();' for x in bnames)+'\n}'
 write('net/minecraft/world/level/block/Blocks.java',block_txt)
 write(classrel+'/KnownWorldUniversalWorldgenC2.java','package dev.dtzudontsay.knownworld.world.generation.settlement; public class KnownWorldUniversalWorldgenC2 {public record Lot(String id,String kind,int x,int z,int width,int depth,String face,int capacity,int maxStaff){} }')
 write(classrel+'/KnownWorldC3Architecture.java','package dev.dtzudontsay.knownworld.world.generation.settlement; import java.util.List; import net.minecraft.world.level.chunk.ChunkAccess; import net.minecraft.world.level.block.state.BlockState; public class KnownWorldC3Architecture {public record Template(String id,String family,int floors,int maxStaff,boolean openAir,boolean requiresShore,boolean requiresMountainResource,boolean requiresPolicy,List<String> stations){} static KnownWorldUniversalWorldgenC2.Lot current; static long count; static void c47Put(ChunkAccess c,int x,int y,int z,BlockState state){ if(current==null || x<current.x()||x>=current.x()+current.width()||z<current.z()||z>=current.z()+current.depth()||y<90||y>127)throw new IllegalStateException("Out-of-lot write: "+x+","+y+","+z);count++;} }')
 write(classrel+'/KnownWorldC47ArchitecturalDetails.java',s)
 rows=list(csv.DictReader((S.parents[1]/'c4_7_review_catalog.csv').open(encoding='utf8')))
 cases=[]
 for row in rows:
  ident=row['id'];wd=row['footprint_width'];dd=row['footprint_depth'];ff=row['floors']
  outdoors='true' if ident in ['COURTYARD','GARDEN','MONUMENT','PASTURE','RITUAL_GROUND','RUIN','WELL'] else 'false'
  family='SHELTER' if ident in ['CAMP','FOREST_CAMP','TEMPORARY_CAMP'] else 'HALL'
  cases.append('check(\"'+ident+'\",'+wd+','+dd+','+ff+','+outdoors+',\"'+family+'\");')
 harness="""package dev.dtzudontsay.knownworld.world.generation.settlement;
public class GeometryHarness {
 static void check(String id,int w,int d,int floors,boolean open,String family){
  var lot=new KnownWorldUniversalWorldgenC2.Lot(id,id,1000,2000,w,d,"SOUTH",0,10);
  var template=new KnownWorldC3Architecture.Template(id,family,floors,10,open,false,false,false,java.util.List.of());
  KnownWorldC3Architecture.current=lot;
  long before=KnownWorldC3Architecture.count;
  KnownWorldC47ArchitecturalDetails.decorate(null,lot,100,template,"NORTHERN");
  if(KnownWorldC3Architecture.count<=before)throw new AssertionError("No detail for "+id);
 }
 public static void main(String[] args) {
"""+'\n'.join('  '+case for case in cases)+"""
  System.out.println("PASS 61 structures, coordinate bounds and details");
 }
}"""
 write(classrel+'/GeometryHarness.java',harness)
 cmd=['javac' ,'-encoding','UTF-8','-d',str(root/'classes')]+[str(p) for p in root.rglob('*.java')]
 r=subprocess.run(cmd,capture_output=True,text=True)
 print('Local Java syntax check:', 'PASS' if r.returncode==0 else 'FAIL')
 if r.returncode:print(r.stderr);raise SystemExit(1)
 cp=subprocess.run(['java','-cp',str(root/'classes'),'dev.dtzudontsay.knownworld.world.generation.settlement.GeometryHarness'],capture_output=True,text=True)
 if cp.returncode:print(cp.stdout,cp.stderr);raise SystemExit(1)
 print(cp.stdout.strip())
 print('Note: stubs validate Java syntax/logic only. Real Minecraft compile is still required.')
