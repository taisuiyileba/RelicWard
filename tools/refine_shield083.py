"""Code-native bronze tower shield with separate rest and guard hand transforms."""
import json
from model_surfaces import clean_model
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'src/main/resources/assets/relicward/models/item'

def build():
    elements=[]
    def box(name,a,b,texture):
        elements.append({'name':name,'from':a,'to':b,'faces':{face:{'texture':'#'+texture,'uv':[0,0,16,16]} for face in ['up','down','north','south','east','west']}})
    # A 16 x 24 shield; grip is centered behind the plate rather than inside it.
    box('bronze_core',[1,-3,5],[15,19,7],'green')
    box('inset_face',[3,-1,4.5],[13,17,5],'dark')
    box('raised_panel',[4,0,4],[12,16,4.5],'green')
    for x in [0,14]:box('side_rail',[x,-2,3.5],[x+2,18,7.5],'gold')
    for y in [-4,18]:box('end_cap',[2,y,3.5],[14,y+2,7.5],'gold')
    for x in [1,13]:
        for y in [-3,17]:box('stepped_corner',[x,y,3.25],[x+2,y+2,7.5],'gold')
    for x in [2,13]:box('inner_bead',[x,-1,3.7],[x+1,17,4.3],'gold')
    # Stepped bell relief and clapper, matching the Bell Warden silhouette.
    box('bell_crown',[7,12,2.5],[9,14,4],'gold')
    box('bell_shoulder',[6,10,2.5],[10,12,4],'gold')
    box('bell_body',[5.5,6,2.5],[10.5,10,4],'gold')
    box('bell_mouth',[5,5,2],[11,6.5,4],'gold')
    box('bell_inset',[7,7,2.25],[9,10,2.5],'dark')
    box('bell_clapper',[7.5,3.5,2.5],[8.5,5,4],'gold')
    for x in [.5,14.5]:
        for y in [0,8,16]:box('bronze_rivet',[x,y,3],[x+1,y+1,3.5],'gold')
    for y in [2,13]:box('rear_brace',[2,y,7],[14,y+1,8],'wood')
    for y in [5,10]:box('grip_anchor',[6.5,y,7],[9.5,y+1,10],'gold')
    box('leather_grip',[7,5.5,9.5],[9,10.5,11],'wood')

    def transform(rotation,translation,scale=1):return {'rotation':rotation,'translation':translation,'scale':[scale]*3}
    display={
        'gui':transform([12,-25,0],[0,0,0],.61),
        'ground':transform([0,0,0],[0,3,0],.4),
        'fixed':transform([0,180,0],[0,0,0],.65),
        'thirdperson_righthand':transform([70,-90,0],[0,3,0]),
        'thirdperson_lefthand':transform([70,-90,0],[0,3,0]),
        'firstperson_righthand':transform([0,-55,-4],[1,-3,-5],.95),
        'firstperson_lefthand':transform([0,-55,-4],[1,-3,-5],.95),
    }
    model={'textures':{'particle':'relicward:block/bronze_bricks','green':'relicward:block/bronze_bricks','gold':'relicward:block/maul_bronze','wood':'relicward:block/maul_grip','dark':'relicward:block/maul_grip'},'elements':elements,'display':display,'overrides':[{'predicate':{'blocking':1},'model':'relicward:item/bell_shield_blocking'}]}
    guard={'parent':'relicward:item/bell_shield','display':{
        'thirdperson_righthand':transform([36,-30,0],[0,2,0]),
        'thirdperson_lefthand':transform([36,-30,0],[0,2,0]),
        'firstperson_righthand':transform([0,0,-4],[-1,0,-6],.95),
        'firstperson_lefthand':transform([0,0,-4],[-1,0,-6],.95),
    }}
    model=clean_model(model)
    for name,data in [('bell_shield',model),('bell_shield_blocking',guard)]:
        (OUT/(name+'.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('Wrote bronze shield:',len(elements),'elements, rest/guard transforms for both hands')

if __name__=='__main__':build()
