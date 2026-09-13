"""Validate delivery files, UV safety and separation of development-only tests."""
from pathlib import Path
import json
import zipfile
import re
from PIL import Image

root=Path(__file__).resolve().parents[1]
spec=json.loads((root/'art/model_spec.json').read_text())
bb=json.loads((root/'art/blockbench/bell_warden.bbmodel').read_text(encoding='utf-8'))
atlas=Image.open(root/'src/main/resources/assets/relicward/textures/entity/bell_warden.png')
glow=Image.open(root/'src/main/resources/assets/relicward/textures/entity/bell_warden_glow.png')
assert atlas.size==glow.size==(512,512)
assert len(spec['cubes'])==len(bb['elements'])==140
assert len(spec['bones'])==18
assert len({e['uuid'] for e in bb['elements']})==140
for element in bb['elements']:
    for face in element['faces'].values():
        x0,y0,x1,y1=face['uv']
        assert 0<=x0<x1<=512 and 0<=y0<y1<=512
        assert atlas.crop((x0,y0,x1,y1)).getchannel('A').getextrema()==(255,255), element['name']
for name in ['zh_cn','en_us']:
    lang=json.loads((root/f'src/main/resources/assets/relicward/lang/{name}.json').read_text(encoding='utf-8'))
    assert lang['entity.relicward.bell_warden']
    assert lang['item.relicward.bell_warden_spawn_egg']

for item in ['bell_core','bronze_fragment','echo_maul','warden_pendant']:
    assert Image.open(root/f'src/main/resources/assets/relicward/textures/item/{item}.png').size==(16,16)
faces={c['name']:c for c in spec['cubes'] if c['name'].startswith('hammer_face_')}
assert set(faces)=={'hammer_face_front','hammer_face_rear'}
assert faces['hammer_face_front']['pos'][2]==-15
assert faces['hammer_face_rear']['pos'][2]==11
assert all(c['size']==[16,10,4] for c in faces.values())
for name in ['court_altar','teaching_bell','chime_lantern']:
    assert json.loads((root/f'src/main/resources/assets/relicward/models/item/{name}.json').read_text())['parent']==f'relicward:block/{name}'
maul=json.loads((root/'src/main/resources/assets/relicward/models/item/echo_maul.json').read_text())
assert maul['parent']=='relicward:item/echo_maul_held' and 'loader' not in maul
version=re.search(r"^version\s*=\s*'([^']+)'",(root/'build.gradle').read_text(),re.MULTILINE).group(1)
jar=root/f'build/libs/relicward-{version}.jar'
with zipfile.ZipFile(jar) as z:
    names=z.namelist()
    for expected in [
        'META-INF/mods.toml', 'cn/suiyi/relicward/entity/BellWarden.class',
        'cn/suiyi/relicward/client/BellWardenModel.class',
        'cn/suiyi/relicward/client/BellWardenRenderer.class',
        'assets/relicward/textures/entity/bell_warden.png',
        'assets/relicward/textures/entity/bell_warden_glow.png',
        'assets/relicward/models/item/bell_warden_spawn_egg.json',
        'data/relicward/structures/resonant_court.nbt',
        'data/relicward/worldgen/structure/resonant_court.json',
        'data/relicward/damage_type/resonance.json',
        'data/relicward/recipes/echo_maul.json',
        'assets/relicward/textures/effect/resonance.png',
        'assets/relicward/models/block/warden_trophy.json',
        'data/curios/tags/items/necklace.json',
        'data/relicward/curios/entities/player.json',
        'cn/suiyi/relicward/ChapterContent.class',
        'cn/suiyi/relicward/compat/JadePlugin.class',
        'cn/suiyi/relicward/world/CourtPopulation.class',
        'data/relicward/recipes/binding_seal.json',
        'data/relicward/recipes/bell_anvil.json',
        'data/relicward/advancements/recipes/bell_shield.json',
        'assets/relicward/textures/models/armor/bell_layer_1.png',
        'assets/relicward/textures/models/armor/bell_layer_2.png',
    ]: assert expected in names,expected
    assert not any('/test/' in n or 'creature_room' in n for n in names), 'Test content leaked into release jar'
trophy=json.loads((root/'src/main/resources/assets/relicward/models/block/warden_trophy.json').read_text(encoding='utf-8'))
assert len({e['name'] for e in trophy['elements']})==19,'17 boss head solids plus 2 plinth solids'
from model_surfaces import assert_no_overlaps
assert_no_overlaps(trophy)
assert_no_overlaps(json.loads((root/'src/main/resources/assets/relicward/models/item/bell_shield.json').read_text(encoding='utf-8')))
assert trophy['textures']['warden']=='relicward:block/warden_trophy'
assert (root/'src/main/resources/assets/relicward/textures/block/warden_trophy.png').read_bytes()==(root/'src/main/resources/assets/relicward/textures/entity/bell_warden.png').read_bytes()
print('PASS: 140 cuboids / 18 bones, opaque valid UV faces, bilingual names, chapter data, no test classes.')

