"""
Full Suite Generator for Bell Armor (玄铎·钟铸古仪重铠):
1. Upgraded 3D Armor Textures (bell_layer_1.png & bell_layer_2.png) with masterwork BACK optimization:
   - Ornate Ceremonial Chime Rack (编钟古阁) on the backplate with 3 relief bells and glowing cyan clappers
   - Sculpted Golden Dragon/Beast Spine (饕餮脊骨重甲) with glowing energy vertebrae
   - Tiered Ceremonial Neck Guard (錾金重鳞护项) with cloud/thunder pattern ridges
   - Heavy Rear Battle Fauld (后摆鱼鳞重甲) with golden sound-studs and jade talisman
   - Tendon protection plates and greave fluting on calf backs
2. Masterwork 2D Item Icons (16x16) matching the 3D model 1:1:
   - bell_helmet.png: Pagoda posts, bell finial, brow beam, cyan diamond crest gem, glowing slit eyes, ear flanges
   - bell_chestplate.png: Stepped eave pauldrons with bell pendants, 3D sacred chest bell with radiant core, collar lintel
   - bell_leggings.png: Golden beast buckle with twin cyan eyes, central front fauld apron with fringe, hip tassets, knee cops with inlays
   - bell_boots.png: Stepped ankle cuffs, square temple sabatons with cyan resonance gems, heavy iron soles
3. Strict zero-overlap verification ([32..64, 0..16] is 100% transparent).
"""
import os
from PIL import Image

# -------------------------------------------------------------
# Color Palettes
# -------------------------------------------------------------
# Gold
G_SPEC = (255, 248, 215, 255)  # Specular shine
G_HI   = (242, 210, 110, 255)  # Highlight bevel
G_MID  = (198, 155, 65, 255)   # Body gold
G_LOW  = (150, 108, 38, 255)   # Antique gold mid-shadow
G_DEEP = (95, 65, 20, 255)     # Crevice shadow
G_DARK = (52, 34, 10, 255)     # Deep line

# Verdigris Patina
P_CHALK= (115, 192, 168, 255)  # Chalky bright patina bloom
P_HI   = (82, 152, 132, 255)   # Highlight patina
P_MID  = (53, 108, 92, 255)    # Core boss patina
P_LOW  = (36, 75, 64, 255)     # Shadowed patina
P_DEEP = (22, 48, 40, 255)     # Recessed groove
P_DARK = (14, 28, 24, 255)     # Deepest seam

# Bronze & Iron
B_HI   = (118, 95, 70, 255)    # Bronze edge
B_MID  = (85, 68, 48, 255)     # Cast bronze plate
B_LOW  = (58, 46, 32, 255)     # Bronze shadow
I_HI   = (48, 56, 54, 255)     # Iron seam highlight
I_MID  = (32, 38, 36, 255)     # Dark joint iron
I_DEEP = (18, 22, 21, 255)     # Undersuit crevice

# Radiant Cyan Resonance Core
C_CORE = (255, 255, 255, 255)  # Blazing white center
C_BRT  = (185, 255, 242, 255)  # Radiant light cyan
C_CYAN = (58, 242, 196, 255)   # Intense resonance cyan
C_JADE = (24, 185, 142, 255)   # Deep sacred jade
C_DEEP = (12, 105, 80, 255)    # Edge aura
C_DARK = (6, 52, 40, 255)      # Shadow ring

def stamp_grid(im, x0, y0, grid_rows):
    for dy, row in enumerate(grid_rows):
        for dx, col in enumerate(row):
            if col is not None:
                im.putpixel((x0 + dx, y0 + dy), col)

def make_box_faces(im, u, v, w, h, d, face_dict):
    face_coords = {
        'top':    (u+d, v),
        'bottom': (u+d+w, v),
        'right':  (u, v+d),
        'front':  (u+d, v+d),
        'left':   (u+d+w, v+d),
        'back':   (u+2*d+w, v+d),
    }
    for fname, grid in face_dict.items():
        if fname in face_coords:
            x0, y0 = face_coords[fname]
            stamp_grid(im, x0, y0, grid)

def generate_metal_face(w, h, base='patina', border='gold', rivets=False, pattern='lamellar'):
    rows = []
    for y in range(h):
        row = []
        for x in range(w):
            is_top = (y == 0)
            is_bot = (y == h - 1)
            is_left = (x == 0)
            is_right = (x == w - 1)
            is_border = is_top or is_bot or is_left or is_right

            if border == 'gold' and is_border:
                if is_top and is_left:
                    col = G_SPEC
                elif is_top or (is_left and not is_bot):
                    col = G_HI
                elif is_bot and is_right:
                    col = G_DARK
                elif is_bot or is_right:
                    col = G_DEEP
                else:
                    col = G_MID
                if rivets and ((x in (1, w-2)) and (y in (1, h-2))):
                    col = G_SPEC
            elif border == 'bronze' and is_border:
                col = B_HI if (is_top or is_left) else B_LOW
            else:
                if base == 'patina':
                    if pattern == 'lamellar':
                        col = P_HI if (x + y) % 2 == 0 else P_MID
                    elif pattern == 'speckled':
                        pat = (x * 7 + y * 13) % 5
                        col = [P_LOW, P_MID, P_HI, P_MID, P_CHALK][pat]
                    else:
                        col = P_MID
                elif base == 'gold':
                    col = G_MID if (x + y) % 2 == 0 else G_LOW
                elif base == 'bronze':
                    col = B_MID if (x + y) % 2 == 0 else B_LOW
                elif base == 'iron':
                    col = I_MID if (x + y) % 2 == 0 else I_DEEP
                else:
                    col = P_MID
            row.append(col)
        rows.append(row)
    return rows

def generate_box(w, h, d, base='patina', border='gold', rivets=False, pattern='lamellar'):
    return {
        'top':    generate_metal_face(w, d, base='gold' if border=='gold' else base, border=border),
        'bottom': generate_metal_face(w, d, base='iron', border='bronze'),
        'front':  generate_metal_face(w, h, base=base, border=border, rivets=rivets, pattern=pattern),
        'back':   generate_metal_face(w, h, base=base, border=border, rivets=rivets, pattern=pattern),
        'right':  generate_metal_face(d, h, base=base, border=border, pattern=pattern),
        'left':   generate_metal_face(d, h, base=base, border=border, pattern=pattern),
    }

# =============================================================
# Build Layer 1
# =============================================================
def build_layer_1():
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))

    # 1. Base Humanoid Head: (0, 0, 8, 8, 8)
    head_front = [
        [G_HI, G_SPEC, G_HI, G_SPEC, G_SPEC, G_HI, G_SPEC, G_HI],
        [G_SPEC, B_LOW, B_MID, G_LOW, G_LOW, B_MID, B_LOW, G_SPEC],
        [P_DARK, P_DEEP, P_DARK, P_DEEP, P_DEEP, P_DARK, P_DEEP, P_DARK],
        [P_DEEP, I_DEEP, C_CORE, I_DEEP, I_DEEP, C_CORE, I_DEEP, P_DEEP],
        [G_LOW, C_DEEP, C_CYAN, G_MID, G_MID, C_CYAN, C_DEEP, G_LOW],
        [G_HI, P_MID, P_HI, G_DEEP, G_DEEP, P_HI, P_MID, G_HI],
        [G_MID, P_LOW, P_MID, B_MID, B_MID, P_MID, P_LOW, G_MID],
        [G_LOW, G_MID, G_HI, G_SPEC, G_SPEC, G_HI, G_MID, G_LOW],
    ]
    # BACK OF HEAD: Tiered Ceremonial Neck Guard (錾金重鳞护项)
    head_back = [
        # Golden dome rim
        [G_SPEC, G_HI, G_SPEC, G_HI, G_HI, G_SPEC, G_HI, G_SPEC],
        # Bronze dome base with rivets
        [G_HI, G_SPEC, P_DEEP, G_SPEC, G_SPEC, P_DEEP, G_SPEC, G_HI],
        # Tier 1 scale ridge (gold edge with verdigris plates)
        [G_SPEC, G_HI, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_HI, G_SPEC],
        [P_HI, P_MID, P_HI, G_MID, G_MID, P_HI, P_MID, P_HI],
        # Shadow seam
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        # Tier 2 scale ridge
        [G_HI, G_SPEC, G_HI, G_SPEC, G_SPEC, G_HI, G_SPEC, G_HI],
        [P_MID, P_LOW, P_MID, G_LOW, G_LOW, P_MID, P_LOW, P_MID],
        # Bottom flared golden rim
        [G_LOW, G_MID, G_HI, G_SPEC, G_SPEC, G_HI, G_MID, G_LOW],
    ]
    make_box_faces(im, 0, 0, 8, 8, 8, {
        'front': head_front,
        'back': head_back,
        'top': generate_metal_face(8, 8, base='patina', border='gold', pattern='speckled'),
        'bottom': generate_metal_face(8, 8, base='iron', border='bronze'),
        'right': generate_metal_face(8, 8, base='patina', border='gold', rivets=True, pattern='lamellar'),
        'left': generate_metal_face(8, 8, base='patina', border='gold', rivets=True, pattern='lamellar'),
    })

    # 2. Base Body: (16, 16, 8, 12, 4)
    body_front = [
        [G_SPEC, G_HI, G_HI, G_SPEC, G_SPEC, G_HI, G_HI, G_SPEC],
        [G_HI, B_LOW, P_DEEP, G_MID, G_MID, P_DEEP, B_LOW, G_HI],
        [G_MID, P_HI, P_MID, G_LOW, G_LOW, P_MID, P_HI, G_MID],
        [G_LOW, P_MID, P_LOW, B_LOW, B_LOW, P_LOW, P_MID, G_LOW],
        [G_HI, G_SPEC, P_DEEP, G_HI, G_HI, P_DEEP, G_SPEC, G_HI],
        [P_HI, P_MID, P_HI, P_MID, P_MID, P_HI, P_MID, P_HI],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_MID, P_HI, G_SPEC, P_MID, P_MID, G_SPEC, P_HI, G_MID],
        [P_MID, P_LOW, P_MID, P_LOW, P_LOW, P_MID, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_LOW, G_MID, G_HI, G_MID, G_MID, G_HI, G_MID, G_LOW],
        [G_DEEP, G_LOW, G_LOW, G_MID, G_MID, G_LOW, G_LOW, G_DEEP],
    ]
    # BACK OF BODY: Sculpted Golden Beast Spine & Resonating Vertebrae (饕餮脊骨重甲)
    body_back = [
        # Neck collar gold rim
        [G_SPEC, G_HI, G_HI, G_SPEC, G_SPEC, G_HI, G_HI, G_SPEC],
        # Upper shoulder plates with spinal anchor
        [P_HI, P_MID, G_HI, G_SPEC, G_SPEC, G_HI, P_MID, P_HI],
        # Spinal vertebra 1 with cyan resonance node
        [P_MID, P_DEEP, G_SPEC, C_CYAN, C_CYAN, G_SPEC, P_DEEP, P_MID],
        [P_DEEP, P_MID, G_HI, C_CORE, C_CORE, G_HI, P_MID, P_DEEP],
        # Spinal vertebra 2
        [G_HI, P_HI, G_SPEC, C_CYAN, C_CYAN, G_SPEC, P_HI, G_HI],
        [P_HI, P_MID, G_MID, G_SPEC, G_SPEC, G_MID, P_MID, P_HI],
        # Spinal vertebra 3
        [P_DEEP, P_DEEP, G_HI, C_CYAN, C_CYAN, G_HI, P_DEEP, P_DEEP],
        [G_MID, P_HI, G_SPEC, C_CORE, C_CORE, G_SPEC, P_HI, G_MID],
        # Lower back articulated plate
        [P_MID, P_LOW, G_HI, G_SPEC, G_SPEC, G_HI, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, G_MID, G_MID, P_DEEP, P_DEEP, P_DEEP],
        # Girdle support rim
        [G_LOW, G_MID, G_HI, G_SPEC, G_SPEC, G_HI, G_MID, G_LOW],
        [G_DEEP, G_LOW, G_LOW, G_MID, G_MID, G_LOW, G_LOW, G_DEEP],
    ]
    make_box_faces(im, 16, 16, 8, 12, 4, {
        'front': body_front,
        'back': body_back,
        'top': generate_metal_face(8, 4, base='gold', border='gold'),
        'bottom': generate_metal_face(8, 4, base='iron', border='bronze'),
        'right': generate_metal_face(4, 12, base='patina', border='gold', pattern='lamellar'),
        'left': generate_metal_face(4, 12, base='patina', border='gold', pattern='lamellar'),
    })

    # 3. Base Arms: (40, 16, 4, 12, 4)
    arm_front = [
        [G_SPEC, G_HI, G_HI, G_SPEC],
        [P_HI, P_MID, P_MID, P_HI],
        [P_MID, P_LOW, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_HI, G_SPEC, G_SPEC, G_HI],
        [P_HI, P_MID, P_MID, P_HI],
        [P_MID, P_LOW, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_MID, G_HI, G_HI, G_MID],
        [P_HI, P_MID, P_MID, P_HI],
        [G_HI, G_SPEC, G_SPEC, G_HI],
        [G_LOW, G_MID, G_MID, G_LOW],
    ]
    make_box_faces(im, 40, 16, 4, 12, 4, {
        'front': arm_front,
        'back': arm_front,
        'right': generate_metal_face(4, 12, base='patina', border='gold', pattern='lamellar'),
        'left': generate_metal_face(4, 12, base='patina', border='gold', pattern='lamellar'),
        'top': generate_metal_face(4, 4, base='gold', border='gold'),
        'bottom': generate_metal_face(4, 4, base='iron', border='bronze'),
    })

    # 4. Base Leg/Boots: (0, 16, 4, 12, 4)
    leg_front = [
        [G_SPEC, G_HI, G_HI, G_SPEC],
        [P_HI, P_MID, P_MID, P_HI],
        [P_MID, P_LOW, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_HI, G_SPEC, G_SPEC, G_HI],
        [P_HI, P_MID, P_MID, P_HI],
        [P_MID, P_LOW, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_MID, G_SPEC, G_SPEC, G_MID],
        [B_MID, C_CYAN, C_CYAN, B_MID],
        [G_HI, G_SPEC, G_SPEC, G_HI],
        [I_DEEP, I_MID, I_MID, I_DEEP],
    ]
    # Calf back: tendon guard and bronze greave fluting
    leg_back = [
        [G_SPEC, G_HI, G_HI, G_SPEC],
        [P_HI, P_MID, P_MID, P_HI],
        [P_MID, P_LOW, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_HI, G_SPEC, G_SPEC, G_HI],
        [B_MID, P_MID, P_MID, B_MID],
        [B_LOW, P_LOW, P_LOW, B_LOW],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_MID, G_SPEC, G_SPEC, G_MID],
        [B_LOW, B_MID, B_MID, B_LOW],
        [G_HI, G_SPEC, G_SPEC, G_HI],
        [I_DEEP, I_MID, I_MID, I_DEEP],
    ]
    make_box_faces(im, 0, 16, 4, 12, 4, {
        'front': leg_front,
        'back': leg_back,
        'right': generate_metal_face(4, 12, base='patina', border='gold', pattern='lamellar'),
        'left': generate_metal_face(4, 12, base='patina', border='gold', pattern='lamellar'),
        'top': generate_metal_face(4, 4, base='gold', border='gold'),
        'bottom': generate_metal_face(4, 4, base='iron', border='bronze'),
    })

    # NOTE: Hat [32..64, 0..16] is 100% UNTOUCHED (all alpha=0)!

    # 5. Pauldron Main: (0, 32, 5, 5, 5) -> 20x10 rect
    pauldron_front = [
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        [G_HI, P_DEEP, G_SPEC, P_DEEP, G_HI],
        [G_MID, C_CYAN, C_CORE, C_CYAN, G_MID],
        [G_LOW, G_MID, C_JADE, G_MID, G_LOW],
        [G_DEEP, G_LOW, G_MID, G_LOW, G_DEEP],
    ]
    pauldron_faces = generate_box(5, 5, 5, base='patina', border='gold', rivets=True)
    pauldron_faces['front'] = pauldron_front
    pauldron_faces['back'] = pauldron_front
    make_box_faces(im, 0, 32, 5, 5, 5, pauldron_faces)

    # 6. Collar Lintel: (20, 32, 9, 2, 5) -> 28x7 rect
    collar_faces = generate_box(9, 2, 5, base='gold', border='gold', pattern='groove')
    collar_front = [
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        [G_DEEP, G_LOW, B_LOW, G_MID, B_MID, G_MID, B_LOW, G_LOW, G_DEEP],
    ]
    collar_faces['front'] = collar_front
    collar_faces['back'] = collar_front
    make_box_faces(im, 20, 32, 9, 2, 5, collar_faces)

    # 7. Back Chime Rack: (48, 32, 6, 8, 2) -> 16x10 rect
    # CRITICAL: Both 'back' (which faces rearwards in 3D) and 'front' get the sacred 3-chime shrine!
    chime_art = [
        # Golden arched temple pagoda pediment
        [G_SPEC, G_HI, G_SPEC, G_SPEC, G_HI, G_SPEC],
        # Bronze crossbeam with golden hanging hooks
        [G_HI, G_SPEC, B_LOW, B_LOW, G_SPEC, G_HI],
        # 3 Hanging ritual chimes with cyan resonant clappers
        [G_MID, C_CYAN, G_LOW, G_LOW, C_CYAN, G_MID],
        [G_LOW, C_CORE, G_MID, G_MID, C_CORE, G_LOW],
        # Flared chime sound rims
        [G_MID, G_SPEC, G_LOW, G_LOW, G_SPEC, G_MID],
        # Bronze vertical pillar brackets
        [B_MID, P_LOW, B_LOW, B_LOW, P_LOW, B_MID],
        # Lower carved ancient bell script
        [G_HI, P_MID, G_MID, G_MID, P_MID, G_HI],
        # Base mounting brackets
        [G_DEEP, G_LOW, G_DEEP, G_DEEP, G_LOW, G_DEEP],
    ]
    chime_faces = generate_box(6, 8, 2, base='bronze', border='gold')
    chime_faces['front'] = chime_art
    chime_faces['back']  = chime_art
    chime_faces['top']   = [[G_SPEC, G_HI, G_SPEC, G_SPEC, G_HI, G_SPEC], [G_HI, G_SPEC, G_HI, G_HI, G_SPEC, G_HI]]
    make_box_faces(im, 48, 32, 6, 8, 2, chime_faces)

    # 8. Pauldron Roof Eave: (20, 39, 6, 2, 6) -> 24x8 rect
    roof_faces = generate_box(6, 2, 6, base='gold', border='gold')
    roof_art = [
        [G_SPEC, G_HI, G_SPEC, G_SPEC, G_HI, G_SPEC],
        [G_LOW, G_MID, B_LOW, B_LOW, G_MID, G_LOW],
    ]
    roof_faces['front'] = roof_art
    roof_faces['back'] = roof_art
    make_box_faces(im, 20, 39, 6, 2, 6, roof_faces)

    # 9. Crown Cap: (0, 42, 3, 1, 3) -> 12x4 rect
    cap_faces = generate_box(3, 1, 3, base='gold', border='gold')
    make_box_faces(im, 0, 42, 3, 1, 3, cap_faces)

    # 10. Sabaton Toe: (44, 42, 5, 3, 5) -> 20x8 rect
    sabaton_front = [
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        [G_HI, C_CYAN, C_CORE, C_CYAN, G_HI],
        [I_DEEP, I_MID, I_MID, I_MID, I_DEEP],
    ]
    sabaton_faces = generate_box(5, 3, 5, base='patina', border='gold')
    sabaton_faces['front'] = sabaton_front
    make_box_faces(im, 44, 42, 5, 3, 5, sabaton_faces)

    # 11. Pauldron Flared Trim: (0, 47, 6, 2, 6) -> 24x8 rect
    trim_faces = generate_box(6, 2, 6, base='gold', border='gold')
    make_box_faces(im, 0, 47, 6, 2, 6, trim_faces)

    # 12. Ankle Cuff: (24, 47, 5, 2, 5) -> 20x7 rect
    cuff_faces = generate_box(5, 2, 5, base='gold', border='gold')
    make_box_faces(im, 24, 47, 5, 2, 5, cuff_faces)

    # 13. Sacred Temple Bell on Chest: (44, 50, 5, 6, 2) -> 14x8 rect
    bell_front = [
        [B_LOW, G_MID, G_SPEC, G_MID, B_LOW],
        [G_HI, G_SPEC, G_HI, G_SPEC, G_HI],
        [G_SPEC, C_DEEP, C_CYAN, C_DEEP, G_SPEC],
        [G_MID, C_CYAN, C_CORE, C_CYAN, G_MID],
        [G_LOW, C_JADE, C_CYAN, C_JADE, G_LOW],
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
    ]
    bell_faces = generate_box(5, 6, 2, base='gold', border='gold')
    bell_faces['front'] = bell_front
    make_box_faces(im, 44, 50, 5, 6, 2, bell_faces)

    # 14. Crown Center Bell Finial: (0, 55, 3, 3, 3) -> 12x6 rect
    finial_front = [
        [G_LOW, G_SPEC, G_LOW],
        [G_SPEC, C_CORE, G_SPEC],
        [G_HI, G_SPEC, G_HI],
    ]
    finial_faces = generate_box(3, 3, 3, base='gold', border='gold')
    finial_faces['front'] = finial_front
    finial_faces['back'] = finial_front
    make_box_faces(im, 0, 55, 3, 3, 3, finial_faces)

    # 15. Brow Beam: (34, 58, 10, 2, 1) -> 22x3 rect
    brow_front = [
        [G_SPEC, G_HI, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_HI, G_SPEC],
        [G_LOW, G_MID, B_LOW, B_LOW, G_MID, G_MID, B_LOW, B_LOW, G_MID, G_LOW],
    ]
    brow_faces = generate_box(10, 2, 1, base='gold', border='gold')
    brow_faces['front'] = brow_front
    make_box_faces(im, 34, 58, 10, 2, 1, brow_faces)

    # 16. Brow Crest (Badge with radiant jade): (34, 54, 4, 3, 1) -> 10x4 rect
    crest_front = [
        [G_SPEC, G_HI, G_HI, G_SPEC],
        [G_HI, C_CORE, C_CYAN, G_HI],
        [G_DEEP, G_MID, G_MID, G_DEEP],
    ]
    crest_faces = generate_box(4, 3, 1, base='gold', border='gold')
    crest_faces['front'] = crest_front
    make_box_faces(im, 34, 54, 4, 3, 1, crest_faces)

    # 17. Ear Plates: (24, 54, 1, 6, 4) -> 10x10 rect
    ear_faces = generate_box(1, 6, 4, base='patina', border='gold', pattern='lamellar')
    make_box_faces(im, 24, 54, 1, 6, 4, ear_faces)

    # 18. Mask Jaw: (0, 61, 6, 2, 1) -> 14x3 rect
    jaw_front = [
        [G_SPEC, G_HI, P_MID, P_MID, G_HI, G_SPEC],
        [G_HI, G_MID, G_SPEC, G_SPEC, G_MID, G_HI],
    ]
    jaw_faces = generate_box(6, 2, 1, base='bronze', border='gold')
    jaw_faces['front'] = jaw_front
    make_box_faces(im, 0, 61, 6, 2, 1, jaw_faces)

    # 19. Crown Prong: (56, 16, 2, 5, 2) -> 8x7 rect
    prong_front = [
        [G_SPEC, G_SPEC],
        [P_HI, P_MID],
        [P_MID, P_LOW],
        [G_HI, G_MID],
        [G_LOW, G_DEEP],
    ]
    prong_faces = generate_box(2, 5, 2, base='patina', border='gold')
    prong_faces['front'] = prong_front
    prong_faces['back'] = prong_front
    make_box_faces(im, 56, 16, 2, 5, 2, prong_faces)

    # 20. Bell Pendant: (56, 23, 2, 3, 2) -> 8x5 rect
    pendant_front = [
        [G_SPEC, G_SPEC],
        [C_CYAN, C_CORE],
        [G_HI, G_SPEC],
    ]
    pendant_faces = generate_box(2, 3, 2, base='gold', border='gold')
    pendant_faces['front'] = pendant_front
    pendant_faces['back'] = pendant_front
    make_box_faces(im, 56, 23, 2, 3, 2, pendant_faces)

    return im

# =============================================================
# Build Layer 2 (Leggings, Waist Belt, Faulds, Knees)
# =============================================================
def build_layer_2():
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))

    # Base legs: (0, 16, 4, 12, 4)
    leg_front = [
        [G_SPEC, G_HI, G_HI, G_SPEC],
        [P_HI, P_MID, P_MID, P_HI],
        [P_MID, P_LOW, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_HI, G_SPEC, G_SPEC, G_HI],
        [P_HI, P_MID, P_MID, P_HI],
        [P_MID, P_LOW, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_MID, G_HI, G_HI, G_MID],
        [P_HI, P_MID, P_MID, P_HI],
        [G_HI, G_SPEC, G_SPEC, G_HI],
        [G_LOW, G_MID, G_MID, G_LOW],
    ]
    make_box_faces(im, 0, 16, 4, 12, 4, {
        'front': leg_front,
        'back': leg_front,
        'right': generate_metal_face(4, 12, base='patina', border='gold', pattern='lamellar'),
        'left': generate_metal_face(4, 12, base='patina', border='gold', pattern='lamellar'),
        'top': generate_metal_face(4, 4, base='gold', border='gold'),
        'bottom': generate_metal_face(4, 4, base='iron', border='bronze'),
    })

    # 1. Waist Belt: (12, 0, 9, 3, 5) -> 28x8 rect
    belt_front = [
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        [G_HI, B_LOW, G_SPEC, B_MID, G_SPEC, B_MID, G_SPEC, B_LOW, G_HI],
        [G_DEEP, G_LOW, G_MID, G_LOW, G_MID, G_LOW, G_MID, G_LOW, G_DEEP],
    ]
    belt_back = [
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        [G_HI, B_LOW, B_MID, G_SPEC, G_SPEC, B_MID, B_LOW, G_HI, G_HI],
        [G_DEEP, G_LOW, G_MID, G_LOW, G_LOW, G_MID, G_LOW, G_DEEP, G_DEEP],
    ]
    belt_faces = generate_box(9, 3, 5, base='bronze', border='gold')
    belt_faces['front'] = belt_front
    belt_faces['back']  = belt_back
    make_box_faces(im, 12, 0, 9, 3, 5, belt_faces)

    # 2. Beast Buckle Boss: (54, 0, 4, 4, 1) -> 10x5 rect
    boss_front = [
        [G_SPEC, G_HI, G_HI, G_SPEC],
        [G_HI, C_CORE, C_CORE, G_HI],
        [G_MID, G_SPEC, G_SPEC, G_MID],
        [G_DEEP, G_MID, G_MID, G_DEEP],
    ]
    boss_faces = generate_box(4, 4, 1, base='gold', border='gold')
    boss_faces['front'] = boss_front
    make_box_faces(im, 54, 0, 4, 4, 1, boss_faces)

    # 3. Front Fauld Tasset: (40, 0, 6, 5, 1) -> 14x6 rect
    fauld_front = [
        [G_SPEC, G_HI, G_SPEC, G_SPEC, G_HI, G_SPEC],
        [G_HI, P_MID, G_MID, G_MID, P_MID, G_HI],
        [G_MID, C_CYAN, C_CORE, C_CORE, C_CYAN, G_MID],
        [P_HI, P_MID, C_JADE, C_JADE, P_MID, P_HI],
        [G_SPEC, G_MID, G_SPEC, G_SPEC, G_MID, G_SPEC],
    ]
    fauld_faces = generate_box(6, 5, 1, base='patina', border='gold')
    fauld_faces['front'] = fauld_front
    make_box_faces(im, 40, 0, 6, 5, 1, fauld_faces)

    # 4. Left & Right Hip Faulds: (0, 0, 1, 5, 5) -> 12x10 rect
    side_fauld_faces = generate_box(1, 5, 5, base='patina', border='gold', pattern='lamellar')
    side_fauld_art = [
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        [P_HI, P_MID, P_HI, P_MID, P_HI],
        [P_MID, P_LOW, P_MID, P_LOW, P_MID],
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        [G_SPEC, G_MID, G_SPEC, G_MID, G_SPEC],
    ]
    side_fauld_faces['right'] = side_fauld_art
    side_fauld_faces['left'] = side_fauld_art
    make_box_faces(im, 0, 0, 1, 5, 5, side_fauld_faces)

    # 5. Rear Fauld: (12, 8, 6, 4, 1) -> 14x5 rect
    # CRITICAL: rear_fauld faces backwards, so 'back' face is what players see!
    rear_fauld_art = [
        # Golden mounting bar with two sound studs
        [G_SPEC, G_HI, G_SPEC, G_SPEC, G_HI, G_SPEC],
        # Tiered lamellar scales with golden edge
        [G_HI, P_MID, G_MID, G_MID, P_MID, G_HI],
        # Central cyan jade talisman gem
        [G_MID, C_CYAN, C_CORE, C_CORE, C_CYAN, G_MID],
        # Scalloped golden fringe tassels
        [G_SPEC, G_MID, G_SPEC, G_SPEC, G_MID, G_SPEC],
    ]
    rear_faces = generate_box(6, 4, 1, base='patina', border='gold', pattern='lamellar')
    rear_faces['front'] = rear_fauld_art
    rear_faces['back']  = rear_fauld_art
    make_box_faces(im, 12, 8, 6, 4, 1, rear_faces)

    # 6. Knee Guard: (40, 6, 5, 4, 2) -> 14x6 rect
    knee_front = [
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        [G_HI, P_DEEP, P_DEEP, P_DEEP, G_HI],
        [G_MID, P_DEEP, P_DEEP, P_DEEP, G_MID],
        [G_DEEP, G_LOW, G_MID, G_LOW, G_DEEP],
    ]
    knee_faces = generate_box(5, 4, 2, base='patina', border='gold')
    knee_faces['front'] = knee_front
    make_box_faces(im, 40, 6, 5, 4, 2, knee_faces)

    # 7. Knee Inlay: (54, 5, 3, 2, 1) -> 8x3 rect
    inlay_front = [
        [C_CYAN, C_CORE, C_CYAN],
        [C_JADE, C_CYAN, C_JADE],
    ]
    inlay_faces = generate_box(3, 2, 1, base='gold', border='gold')
    inlay_faces['front'] = inlay_front
    make_box_faces(im, 54, 5, 3, 2, 1, inlay_faces)

    return im

# =============================================================
# Master 16x16 2D Item Icons (Matching 3D Model 1:1)
# =============================================================
_ = None  # transparent

def build_2d_helmet():
    """
    1:1 matching the 3D Bell Helmet:
    - Crown posts at x=(2..3, 12..13) with flared gold caps
    - Central bell finial on crown at x=(7..8), y=(1..3)
    - Forehead brow beam at x=(2..13), y=(6..7)
    - Raised brow crest with glowing cyan diamond gem at center
    - Dark visor opening with TWO glowing cyan eyes at x=(4, 11)
    - Flared cheek ear plates and golden chin guard
    """
    grid = [
        # y=0: Finial and post caps
        [_, _, G_SPEC, G_HI, _, _, _, G_SPEC, G_SPEC, _, _, _, G_HI, G_SPEC, _, _],
        # y=1: Post capitals and center finial body
        [_, _, G_HI, G_MID, _, _, G_HI, C_CORE, C_CORE, G_HI, _, _, G_MID, G_HI, _, _],
        # y=2: Post columns and finial lip
        [_, _, P_HI, P_MID, _, _, G_SPEC, G_HI, G_HI, G_SPEC, _, _, P_MID, P_HI, _, _],
        # y=3: Post columns
        [_, _, P_MID, P_LOW, _, _, _, _, _, _, _, _, P_LOW, P_MID, _, _],
        # y=4: Crown crest badge with diamond cyan resonance eye
        [_, _, G_SPEC, G_HI, _, G_HI, G_SPEC, C_CORE, C_CORE, G_SPEC, G_HI, _, G_HI, G_SPEC, _, _],
        # y=5: Forehead dome slope
        [_, G_SPEC, G_HI, P_HI, G_HI, G_MID, C_CYAN, C_CORE, C_CORE, C_CYAN, G_MID, G_HI, P_HI, G_HI, G_SPEC, _],
        # y=6: Massive Brow Beam (Horizontal temple lintel)
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        # y=7: Brow beam shadow & rivets
        [G_DEEP, G_LOW, G_SPEC, B_LOW, B_MID, G_MID, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_MID, B_MID, B_LOW, G_SPEC, G_LOW, G_DEEP],
        # y=8: Visor recess & glowing cyan eyes at x=(4, 11)
        [P_HI, G_SPEC, P_DARK, P_DEEP, C_CORE, C_CYAN, P_DARK, P_DARK, P_DARK, P_DARK, C_CYAN, C_CORE, P_DEEP, P_DARK, G_SPEC, P_HI],
        # y=9: Nose bridge and cheek plate tops
        [P_MID, G_HI, P_DEEP, G_LOW, C_CYAN, C_JADE, G_MID, G_SPEC, G_SPEC, G_MID, C_JADE, C_CYAN, G_LOW, P_DEEP, G_HI, P_MID],
        # y=10: Ear flange plates (stepped eaves)
        [G_SPEC, G_MID, P_HI, P_MID, G_DEEP, G_LOW, P_MID, P_HI, P_HI, P_MID, G_LOW, G_DEEP, P_MID, P_HI, G_MID, G_SPEC],
        # y=11: Ear plates and cheek armor
        [G_HI, G_LOW, P_MID, P_LOW, B_LOW, P_DEEP, P_LOW, P_MID, P_MID, P_LOW, P_DEEP, B_LOW, P_LOW, P_MID, G_LOW, G_HI],
        # y=12: Lower jaw ceremonial bronze guard
        [_, G_DEEP, G_SPEC, G_HI, P_MID, P_LOW, G_HI, G_SPEC, G_SPEC, G_HI, P_LOW, P_MID, G_HI, G_SPEC, G_DEEP, _],
        # y=13: Chin cup with gold chevron
        [_, _, G_LOW, G_MID, G_SPEC, G_SPEC, G_MID, G_LOW, G_LOW, G_MID, G_SPEC, G_SPEC, G_MID, G_LOW, _, _],
        # y=14: Bottom chin point
        [_, _, _, _, G_DEEP, G_LOW, G_MID, G_SPEC, G_SPEC, G_MID, G_LOW, G_DEEP, _, _, _, _],
        # y=15: Empty
        [_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _],
    ]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    stamp_grid(im, 0, 0, grid)
    return im

def build_2d_chestplate():
    """
    1:1 matching the 3D Bell Chestplate:
    - Back chime rack posts visible behind neck
    - Architectural collar lintel
    - Multi-tiered pagoda pauldrons with gold roofs and hanging bell pendants with cyan sound beads
    - Centerpiece: 3D Sacred Temple Bell with radiant resonance core (white core -> cyan halo -> jade)
    - Lamellar torso plates and heavy gold belt rim
    """
    grid = [
        # y=0: Chime rack posts behind neck & pauldron roof tips
        [G_SPEC, G_HI, _, _, _, G_SPEC, G_HI, _, _, G_HI, G_SPEC, _, _, _, G_HI, G_SPEC],
        # y=1: Pauldron roof eaves & collar lintel top
        [G_HI, G_SPEC, G_SPEC, _, G_SPEC, G_HI, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_HI, G_SPEC, _, G_SPEC, G_SPEC, G_HI],
        # y=2: Pauldron eaves & collar lintel groove
        [G_MID, P_HI, P_MID, G_SPEC, G_HI, B_LOW, G_MID, G_SPEC, G_SPEC, G_MID, B_LOW, G_HI, G_SPEC, P_MID, P_HI, G_MID],
        # y=3: Pauldron main beast face & bell suspension loop
        [G_SPEC, C_CYAN, C_CORE, G_SPEC, G_LOW, B_LOW, G_MID, G_SPEC, G_SPEC, G_MID, B_LOW, G_LOW, G_SPEC, C_CORE, C_CYAN, G_SPEC],
        # y=4: Hanging bell pendant & Sacred Temple Bell crown
        [C_CYAN, C_CORE, C_CYAN, G_LOW, G_HI, G_SPEC, G_HI, G_SPEC, G_SPEC, G_HI, G_SPEC, G_HI, G_LOW, C_CYAN, C_CORE, C_CYAN],
        # y=5: Sacred Bell sound nipples & cyan resonance aura
        [G_SPEC, G_MID, G_DEEP, P_DEEP, G_SPEC, C_DEEP, C_CYAN, C_BRT, C_BRT, C_CYAN, C_DEEP, G_SPEC, P_DEEP, G_DEEP, G_MID, G_SPEC],
        # y=6: Sacred Bell CENTER: Blazing White-Hot Divine Core!
        [_, G_SPEC, G_LOW, P_MID, G_MID, C_CYAN, C_CORE, C_CORE, C_CORE, C_CORE, C_CYAN, G_MID, P_MID, G_LOW, G_SPEC, _],
        # y=7: Sacred Bell lower aura & ancient gold ribbing
        [_, G_HI, G_DEEP, P_LOW, G_LOW, C_JADE, C_CYAN, C_BRT, C_BRT, C_CYAN, C_JADE, G_LOW, P_LOW, G_DEEP, G_HI, _],
        # y=8: Sacred Bell flared golden sound-lip (钟于)
        [_, _, _, P_DEEP, G_SPEC, G_HI, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_HI, G_SPEC, P_DEEP, _, _, _],
        # y=9: Torso lamellar band 1
        [_, _, _, G_HI, G_SPEC, P_DEEP, P_HI, G_SPEC, G_SPEC, P_HI, P_DEEP, G_SPEC, G_HI, _, _, _],
        # y=10: Torso plate row 1
        [_, _, _, P_HI, P_MID, P_HI, P_MID, G_HI, G_HI, P_MID, P_HI, P_MID, P_HI, _, _, _],
        # y=11: Seam groove
        [_, _, _, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, P_DEEP, _, _, _],
        # y=12: Torso lamellar band 2 (gold sound studs)
        [_, _, _, G_MID, G_SPEC, P_HI, G_SPEC, P_MID, P_MID, G_SPEC, P_HI, G_SPEC, G_MID, _, _, _],
        # y=13: Lower torso plate
        [_, _, _, P_MID, P_LOW, P_MID, P_LOW, P_MID, P_MID, P_LOW, P_MID, P_LOW, P_MID, _, _, _],
        # y=14: Heavy golden belt rim
        [_, _, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, _, _],
        # y=15: Belt drop shadow
        [_, _, G_DEEP, G_LOW, G_DEEP, G_LOW, G_DEEP, G_DEEP, G_DEEP, G_DEEP, G_LOW, G_DEEP, G_LOW, G_DEEP, _, _],
    ]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    stamp_grid(im, 0, 0, grid)
    return im

def build_2d_leggings():
    """
    1:1 matching the 3D Bell Leggings & Faulds:
    - Ceremonial waist belt with Golden Beast Buckle (饕餮神兽纽) & twin glowing cyan eyes
    - Centerpiece: Central Front Fauld Apron with glowing cyan jade core & golden fringe tassels
    - Draping hip fauld plates on sides
    - Bronze Knee Cops with glowing cyan inlays
    - Segmented shin greaves
    """
    grid = [
        # y=0: Belt top gold highlight
        [G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC, G_HI, G_SPEC],
        # y=1: Belt body with Beast Buckle Horns & twin glowing cyan eyes at x=(7, 8)
        [G_HI, B_LOW, G_SPEC, B_MID, G_HI, G_SPEC, G_HI, C_CORE, C_CORE, G_HI, G_SPEC, G_HI, B_MID, G_SPEC, B_LOW, G_HI],
        # y=2: Beast Buckle Snout & Fauld mounting bar
        [G_DEEP, G_LOW, G_MID, G_LOW, G_SPEC, G_HI, G_SPEC, G_SPEC, G_SPEC, G_SPEC, G_HI, G_SPEC, G_LOW, G_MID, G_LOW, G_DEEP],
        # y=3: Front Fauld panel & Hip Fauld draping
        [G_SPEC, P_HI, P_MID, G_SPEC, _, G_HI, P_MID, G_SPEC, G_SPEC, P_MID, G_HI, _, G_SPEC, P_MID, P_HI, G_SPEC],
        # y=4: Front Fauld RESONANCE JADE EMBLEM (White-Hot Core)
        [G_HI, P_MID, P_LOW, G_HI, _, G_MID, C_CYAN, C_CORE, C_CORE, C_CYAN, G_MID, _, G_HI, P_LOW, P_MID, G_HI],
        # y=5: Front Fauld scalloped jade rim
        [G_MID, P_DEEP, P_DEEP, G_MID, _, P_HI, P_MID, C_JADE, C_JADE, P_MID, P_HI, _, G_MID, P_DEEP, P_DEEP, G_MID],
        # y=6: Golden Fringe Tassels on Fauld bottom
        [G_LOW, G_MID, G_MID, G_LOW, _, G_SPEC, G_MID, G_SPEC, G_SPEC, G_MID, G_SPEC, _, G_LOW, G_MID, G_MID, G_LOW],
        # y=7: Thigh cuisse plates
        [P_HI, P_MID, P_MID, P_HI, _, _, _, _, _, _, _, _, P_HI, P_MID, P_MID, P_HI],
        # y=8: Knee Cop top bevel
        [G_SPEC, G_HI, G_SPEC, G_HI, _, _, _, _, _, _, _, _, G_HI, G_SPEC, G_HI, G_SPEC],
        # y=9: Knee Cop with Glowing Cyan Inlay (C_CYAN, C_CORE)
        [G_HI, C_CYAN, C_CORE, G_HI, _, _, _, _, _, _, _, _, G_HI, C_CORE, C_CYAN, G_HI],
        # y=10: Knee Cop lower rim
        [G_DEEP, G_LOW, G_MID, G_DEEP, _, _, _, _, _, _, _, _, G_DEEP, G_MID, G_LOW, G_DEEP],
        # y=11: Upper greave plate
        [P_HI, P_MID, P_MID, P_HI, _, _, _, _, _, _, _, _, P_HI, P_MID, P_MID, P_HI],
        # y=12: Greave middle seam
        [P_DEEP, P_DEEP, P_DEEP, P_DEEP, _, _, _, _, _, _, _, _, P_DEEP, P_DEEP, P_DEEP, P_DEEP],
        # y=13: Lower greave lamellar
        [G_MID, P_HI, P_HI, G_MID, _, _, _, _, _, _, _, _, G_MID, P_HI, P_HI, G_MID],
        # y=14: Ankle gold cuff
        [G_SPEC, G_HI, G_HI, G_SPEC, _, _, _, _, _, _, _, _, G_SPEC, G_HI, G_HI, G_SPEC],
        # y=15: Greave shadow bottom
        [G_DEEP, G_LOW, G_LOW, G_DEEP, _, _, _, _, _, _, _, _, G_DEEP, G_LOW, G_LOW, G_DEEP],
    ]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    stamp_grid(im, 0, 0, grid)
    return im

def build_2d_boots():
    """
    1:1 matching the 3D Bell Boots:
    - Flared stepped golden ankle cuffs with verdigris patina
    - Articulated bronze calf band
    - Square-toed temple sabatons with reinforced golden toe cap
    - Glowing cyan resonance gems on the toe caps
    - Heavy cast iron tread soles
    """
    grid = [
        # y=0..4: Empty
        [_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _],
        [_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _],
        [_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _],
        [_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _],
        [_, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _],
        # y=5: Flared golden ankle cuff top
        [_, G_SPEC, G_HI, G_SPEC, G_SPEC, _, _, _, _, _, G_SPEC, G_SPEC, G_HI, G_SPEC, _, _],
        # y=6: Ankle cuff body with verdigris patina
        [_, G_HI, P_HI, P_MID, G_HI, _, _, _, _, _, G_HI, P_MID, P_HI, G_HI, _, _],
        # y=7: Ankle cuff bottom bevel
        [_, G_SPEC, G_MID, G_LOW, G_SPEC, _, _, _, _, _, G_SPEC, G_LOW, G_MID, G_SPEC, _, _],
        # y=8: Joint leather band
        [_, B_LOW, I_MID, I_MID, B_LOW, _, _, _, _, _, B_LOW, I_MID, I_MID, B_LOW, _, _],
        # y=9: Sabaton upper foot plate
        [G_SPEC, G_HI, P_HI, P_MID, G_HI, G_SPEC, _, _, G_SPEC, G_HI, P_MID, P_HI, G_HI, G_SPEC, _, _],
        # y=10: Sabaton toe with GLOWING CYAN RESONANCE GEM!
        [G_HI, C_CYAN, C_CORE, C_CYAN, P_MID, G_HI, _, _, G_HI, P_MID, C_CYAN, C_CORE, C_CYAN, G_HI, _, _],
        # y=11: Reinforced golden toe cap
        [G_SPEC, G_HI, C_CYAN, G_SPEC, G_MID, G_SPEC, _, _, G_SPEC, G_MID, G_SPEC, C_CYAN, G_HI, G_SPEC, _, _],
        # y=12: Sabaton side plate
        [G_LOW, G_MID, G_SPEC, G_MID, G_LOW, G_DEEP, _, _, G_DEEP, G_LOW, G_MID, G_SPEC, G_MID, G_LOW, _, _],
        # y=13: Cast iron sole top edge
        [I_HI, I_HI, I_HI, I_HI, I_HI, I_HI, _, _, I_HI, I_HI, I_HI, I_HI, I_HI, I_HI, _, _],
        # y=14: Heavy ribbed iron tread sole
        [I_DEEP, I_MID, I_DEEP, I_MID, I_DEEP, I_DEEP, _, _, I_DEEP, I_DEEP, I_MID, I_DEEP, I_MID, I_DEEP, _, _],
        # y=15: Sole ground shadow
        [I_DEEP, I_DEEP, I_DEEP, I_DEEP, I_DEEP, I_DEEP, _, _, I_DEEP, I_DEEP, I_DEEP, I_DEEP, I_DEEP, I_DEEP, _, _],
    ]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    stamp_grid(im, 0, 0, grid)
    return im

# =============================================================
# Main Execution & Verification
# =============================================================
if __name__ == '__main__':
    # 1. Build and save 3D armor texture layers
    l1 = build_layer_1()
    l2 = build_layer_2()

    # Verify hat region [32..64, 0..16] is 100% transparent
    hat_box = [l1.getpixel((x, y)) for x in range(32, 64) for y in range(0, 16)]
    opaque_hat = sum(1 for p in hat_box if p[3] > 0)
    print(f'Layer 1 Hat Opaque Count: {opaque_hat} (MUST be 0)')
    assert opaque_hat == 0, "Layer 1 hat area has opaque pixels!"

    # Save 3D textures
    t_dir = 'src/main/resources/assets/relicward/textures/models/armor'
    l1.save(os.path.join(t_dir, 'bell_layer_1.png'))
    l2.save(os.path.join(t_dir, 'bell_layer_2.png'))
    l1.save(os.path.join(t_dir, 'bell_armor.png'))
    l2.save(os.path.join(t_dir, 'bell_armor_legs.png'))

    # 2. Build and save 2D item icons
    i_dir = 'src/main/resources/assets/relicward/textures/item'
    h_icon = build_2d_helmet()
    c_icon = build_2d_chestplate()
    l_icon = build_2d_leggings()
    b_icon = build_2d_boots()

    h_icon.save(os.path.join(i_dir, 'bell_helmet.png'))
    c_icon.save(os.path.join(i_dir, 'bell_chestplate.png'))
    l_icon.save(os.path.join(i_dir, 'bell_leggings.png'))
    b_icon.save(os.path.join(i_dir, 'bell_boots.png'))

    print('3D textures and 2D icons updated successfully!')
