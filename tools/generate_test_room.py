"""Minimal gzip NBT structure for the isolated Forge GameTest source set."""
from pathlib import Path
import gzip
import struct

def s(text):
    b=text.encode('utf-8'); return struct.pack('>H',len(b))+b
def named(t,n,payload): return bytes([t])+s(n)+payload
def integer(n): return struct.pack('>i',n)
def ints(name,values): return named(9,name,b'\x03'+integer(len(values))+b''.join(integer(v) for v in values))
def compound(parts): return b''.join(parts)+b'\x00'
palette=compound([named(8,'Name',s('minecraft:air'))])
nbt=named(10,'',compound([
    named(3,'DataVersion',integer(3465)),ints('size',[16,12,16]),
    named(9,'palette',b'\x0a'+integer(1)+palette),
    named(9,'blocks',b'\x0a'+integer(0)),named(9,'entities',b'\x0a'+integer(0)),
]))
out=Path(__file__).resolve().parents[1]/'src/gametest/resources/data/relicward/structures/creature_room.nbt'
out.parent.mkdir(parents=True,exist_ok=True)
out.write_bytes(gzip.compress(nbt,mtime=0))
print(out)

stone=compound([named(8,'Name',s('minecraft:stone'))])
floor=[]
for x in range(53):
    for z in range(53):
        floor.append(compound([ints('pos',[x,0,z]),named(3,'state',integer(0))]))
arena=named(10,'',compound([
    named(3,'DataVersion',integer(3465)),ints('size',[53,16,53]),
    named(9,'palette',b'\x0a'+integer(1)+stone),
    named(9,'blocks',b'\x0a'+integer(len(floor))+b''.join(floor)),
    named(9,'entities',b'\x0a'+integer(0)),
]))
(out.parent/'arena_test.nbt').write_bytes(gzip.compress(arena,mtime=0))
