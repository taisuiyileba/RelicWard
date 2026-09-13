#!/usr/bin/env python3
"""
Generate Cataclysm-tier Boss Health Bar Icons and Debuff Effect Textures for RelicWard:
1. super_gravity.png (18x18) - Gravitational singularity with downward crushing force waves
2. armor_fracture.png (18x18) - Shattered ancient bronze breastplate with molten resonant fissure
3. bell_warden_phase1.png (28x28) - Bell Warden ancient bronze mask with serene cyan resonant eyes
4. bell_warden_phase2.png (28x28) - Awakened Bell Warden mask with blazing fiery eyes and molten fracture fissures
5. bar_right_bracket.png (16x24) - Ornate ancient bronze end finial & bell seal
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/relicward/textures'
EFFECT_DIR = ROOT / 'mob_effect'
BOSS_BAR_DIR = ROOT / 'gui/boss_bar'
PREVIEW_DIR = Path(__file__).resolve().parents[1] / 'art/preview'

EFFECT_DIR.mkdir(parents=True, exist_ok=True)
BOSS_BAR_DIR.mkdir(parents=True, exist_ok=True)
PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

def parse_grid(rows, palette, width, height):
    im = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    pix = im.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            pix[x, y] = palette[ch]
    return im

# ==========================================
# 1. ARMOR FRACTURE (18x18)
# ==========================================
def create_armor_fracture():
    pal = {
        '.': (0, 0, 0, 0),
        '#': (24, 15, 17, 255),    # Dark crisp outline
        's': (62, 38, 22, 255),    # Deep shadow bronze
        'd': (108, 68, 34, 255),   # Dark bronze
        'm': (158, 106, 48, 255),  # Mid bronze
        'l': (206, 146, 68, 255),  # Light bronze
        'h': (242, 192, 112, 255), # Bronze highlight
        'g': (255, 230, 166, 255), # Specular gold
        'p': (38, 76, 68, 255),    # Dark verdigris patina
        'q': (58, 122, 110, 255),  # Light verdigris patina
        'k': (48, 14, 10, 255),    # Charred fracture shadow
        'r': (216, 40, 10, 255),   # Red-hot fracture edge
        'o': (255, 112, 14, 255),  # Blazing orange rupture
        'y': (255, 206, 48, 255),  # Molten yellow core
        'w': (255, 255, 224, 255), # Blinding white-hot seam
    }
    # 18x18 Breastplate diagonally split apart with jagged rupture
    rows = [
        '..................',
        '...##........##...',
        '..#gh#......#hg#..',
        '..#lmd#....#mld#..',
        '.#ghmd######dmld#.',
        '.#lmd#krrk#dmmld#.',
        '.#md#kooyk#dmmld#.',
        '.#m#koyyok##dmd#..',
        '.#m#kywyk#smdmd#..',
        '..##kyyok#smdms#..',
        '..#koyyok#smms#...',
        '..#kooyk#smms#....',
        '..#krrk#smdms#....',
        '..#pqs#smqqs#.....',
        '..#dms#sddms#.....',
        '...#d##sddms#.....',
        '....####sdms#.....',
        '........####......',
    ]
    return parse_grid(rows, pal, 18, 18)

# ==========================================
# 2. SUPER GRAVITY (18x18)
# ==========================================
def create_super_gravity():
    pal = {
        '.': (0, 0, 0, 0),
        '#': (18, 8, 32, 255),     # Abyssal dark outline
        'v': (34, 14, 60, 255),    # Void shadow
        'd': (64, 24, 114, 255),   # Cosmic violet dark
        'm': (102, 38, 186, 255),  # Vibrant amethyst mid
        'l': (148, 68, 238, 255),  # Radiant purple light
        'h': (194, 126, 255, 255), # Lilac highlight
        'c': (232, 190, 255, 255), # Core aura
        'w': (255, 255, 255, 255), # Blinding singularity white
        'g': (28, 10, 48, 255),    # Event horizon center
    }
    rows = [
        '.....########.....',
        '..###chhhhhhc###..',
        '.#chwllmmmmllwhc#.',
        '.#hwldd#gg#ddlwh#.',
        '#hld#d#gggg#d#dlh#',
        '#wld#gggwwggg#dlw#',
        '#hmd#ggwwwwgg#dmh#',
        '#dmd##ggwwgg##dmd#',
        '.#dmh#d#gg#d#hmd#.',
        '.#d#hll####llh#d#.',
        '..##mhwwwwwwhm##..',
        '...#dhlcwwclhd#...',
        '...#ddhlmclhdd#...',
        '....#ddhllhdd#....',
        '.....#ddhldd#.....',
        '......#ddmd#......',
        '.......#dd#.......',
        '........##........',
    ]
    return parse_grid(rows, pal, 18, 18)

# ==========================================
# 3. BELL WARDEN BOSS ICON - PHASE 1 (28x28)
# ==========================================
def create_bell_warden_phase1():
    pal = {
        '.': (0, 0, 0, 0),
        '#': (20, 16, 18, 255),    # Crisp dark bronze outline
        'k': (36, 26, 24, 255),    # Deep bronze shadow
        's': (68, 46, 28, 255),    # Shadow bronze
        'd': (106, 74, 38, 255),   # Dark bronze
        'm': (154, 110, 54, 255),  # Mid bronze
        'l': (202, 150, 78, 255),  # Light bronze
        'h': (238, 192, 112, 255), # Bronze highlight
        'g': (255, 228, 156, 255), # Gilded filigree / gold cap
        
        # Ancient Verdigris Patina
        'p': (30, 64, 56, 255),    # Dark verdigris
        'q': (48, 108, 94, 255),   # Mid verdigris
        't': (78, 164, 144, 255),  # Light verdigris
        
        # Resonant Cyan Eyes (Phase 1)
        'e': (22, 72, 68, 255),    # Eye shadow socket
        'c': (54, 168, 148, 255),  # Cyan glow mid
        'a': (96, 242, 216, 255),  # Cyan bright
        'w': (232, 255, 250, 255), # Eye white core
    }
    # Perfectly centered 28x28 grid
    rows = [
        '.....##............##.......', # 0
        '....#gh#....##....#hg#......', # 1 - Horned prong tips & pagoda crown finial
        '...#hml#...#gh#...#lmh#.....', # 2
        '...#mlh#..#hllh#..#hlm#.....', # 3
        '..#hllm#..#lmml#..#mllh#....', # 4 - Horn curvature
        '..#lmds#.#dmmmmd#.#sdml#....', # 5
        '..#mdk##.#mmmmmm#..##kdm#...', # 6 - Crown base joins temples
        '.#hlll############lllh#.....', # 7 - Massive brow beam starts
        '.#lmmmmhghhllhhghmmmmml#....', # 8 - Forehead relief
        '.#dmmmsdqttqqttqdsmmmld#....', # 9 - Verdigris patina carving
        '.#sddddkkkkkkkkkkddddds#....', # 10 - Deep brow shadow
        '.##sd##############ds##.....', # 11 - Eye socket line
        '#pq#d#ecccc##cccce#d#qp#....', # 12 - Deep recessed eye cavities
        '#qt#s#caawc##cwaac#s#tq#....', # 13 - Glowing cyan resonant eyes
        '.#p#k#ecccc##cccce#k#p#.....', # 14 - Nose bridge & temple armor
        '.##mdd#kssssssssk#ddm##.....', # 15 - Tiered bronze cheekplates
        '..#lmmd#sdddddds#dmml#......', # 16 - Angular jawline
        '..#dmmd#k#k##k#k#dmmd#......', # 17 - Mouth speaker vents
        '...#sdd#ssssssss#dds#.......', # 18 - Lower jaw
        '...##sd#dmmmmmmd#ds##.......', # 19 - Chin point
        '.....###kksssskk###.........', # 20 - Chin joint
        '.......#dhhhhhhd#...........', # 21 - Hanging bell suspension crown
        '......#hqqqqqqqqh#..........', # 22 - Bell shoulder patina
        '.....#lqddddddddql#.........', # 23 - Bell body
        '.....#mddkkwwkkddm#.........', # 24 - Resonant clapper weight
        '.....#hlllllhlllllh#........', # 25 - Flared bell sound rim
        '......##kkk####kkk##........', # 26 - Bell base shadow
        '............................', # 27
    ]
    return parse_grid(rows, pal, 28, 28)

# ==========================================
# 4. BELL WARDEN BOSS ICON - PHASE 2 (28x28)
# ==========================================
def create_bell_warden_phase2():
    pal = {
        '.': (0, 0, 0, 0),
        '#': (22, 14, 16, 255),    # Dark outline
        'k': (38, 24, 22, 255),    # Deep bronze shadow
        's': (72, 44, 26, 255),    # Shadow bronze
        'd': (112, 72, 36, 255),   # Dark bronze
        'm': (162, 108, 50, 255),  # Mid bronze
        'l': (212, 152, 74, 255),  # Light bronze
        'h': (246, 196, 110, 255), # Bronze highlight
        'g': (255, 230, 154, 255), # Gilded cap
        
        # Scorched Patina & Heat
        'p': (38, 54, 52, 255),    # Scorched verdigris
        'q': (62, 92, 86, 255),    # Heated patina
        't': (96, 144, 132, 255),  # Pale patina
        
        # Blazing Fiery Eyes & Molten Fissures (Phase 2)
        'f': (82, 16, 10, 255),    # Deep ember fissure shadow
        'r': (224, 38, 12, 255),   # Crimson fire
        'o': (255, 114, 18, 255),  # Blazing orange
        'y': (255, 206, 44, 255),  # Bright solar gold
        'w': (255, 255, 220, 255), # White-hot core
    }
    rows = [
        '.....##............##.......', # 0
        '....#yo#....##....#oy#......', # 1 - Molten energized tips
        '...#hml#...#yo#...#lmh#.....', # 2
        '...#mlh#..#hllh#..#hlm#.....', # 3
        '..#hllm#..#lmml#..#mllh#....', # 4
        '..#lmds#.#dmmmmd#.#sdml#....', # 5
        '..#mdk##.#mmmmmm#..##kdm#...', # 6
        '.#hlll############lllh#.....', # 7 - Brow beam
        '.#lmmmfooyhllhhfyommmml#....', # 8 - Jagged molten cracks across brow!
        '.#dmmmsfooyqqttqdsmmmld#....', # 9 - Fissures leaking solar plasma
        '.#sddddkfooykkkkkddddds#....', # 10 - Fracture continues downward
        '.##sd#####f########ds##.....', # 11
        '#pq#d#frooo##ooorf#d#qp#....', # 12 - Deep fiery eye sockets
        '#qt#s#oyywo##owyyof#s#tq#...', # 13 - Burning incandescent orange eyes
        '.#p#k#frooo##ooorf#k#p#.....', # 14 - Fire bleeding into cheekplates
        '.##mdd#kfooyyyysk#ddm##.....', # 15 - Fissure spreading across jaw
        '..#lmmd#sdfooyds#dmml#......', # 16
        '..#dmmd#k#k##foy#dmmd#......', # 17 - Molten glow inside mouth vents
        '...#sdd#sssssfoy#dds#.......', # 18
        '...##sd#dmmmmmfo#ds##.......', # 19 - Cracked chin
        '.....###kksssskk###.........', # 20
        '.......#dhhhhhhd#...........', # 21 - Superheated bell crown
        '......#hyyyyyyyyh#..........', # 22 - Molten resonance
        '.....#lyoooooooooyl#........', # 23 - Radiant bell body
        '.....#moddkwwkdoym#.........', # 24 - Super-gravity blazing clapper core
        '.....#hlllllhlllllh#........', # 25
        '......##kkk####kkk##........', # 26
        '............................', # 27
    ]
    return parse_grid(rows, pal, 28, 28)

# ==========================================
# 5. BOSS BAR RIGHT BRACKET / FINIAL (16x24)
# ==========================================
def create_bar_right_bracket(phase=1):
    pal = {
        '.': (0, 0, 0, 0),
        '#': (20, 16, 18, 255),    # Outline
        'k': (36, 26, 24, 255),    # Deep shadow bronze
        's': (68, 46, 28, 255),    # Shadow bronze
        'd': (106, 74, 38, 255),   # Dark bronze
        'm': (154, 110, 54, 255),  # Mid bronze
        'l': (202, 150, 78, 255),  # Light bronze
        'h': (238, 192, 112, 255), # Highlight
        'g': (255, 228, 156, 255), # Gilded gold
        'q': (52, 112, 98, 255),   # Verdigris patina
        'c': (65, 185, 162, 255) if phase == 1 else (255, 116, 18, 255),  # Resonant jewel (cyan P1, fiery P2)
        'a': (96, 242, 216, 255) if phase == 1 else (255, 206, 52, 255),  # Bright jewel core
        'w': (235, 255, 250, 255) if phase == 1 else (255, 255, 220, 255),# Jewel white highlight
    }
    rows = [
        '......##........', # 0
        '.....#gh#.......', # 1 - Golden finial peak
        '....#hml#.......', # 2
        '...#hmlg#.......', # 3
        '..#hllh##.......', # 4 - Upper bracket wing
        '.#hlm##..##.....', # 5
        '#lmm#..#hgh#....', # 6 - Lintel extension
        '#dmm###qqqqh#...', # 7 - Patina filigree
        '#sdd#mcwwwch#...', # 8 - Embedded resonant jewel
        '#sdd#mcwaach#...', # 9
        '#sdd#mcwwwch#...', # 10
        '#dmm###qqqqh#...', # 11
        '#lmm#..#hgh#....', # 12
        '.#hlm##..##.....', # 13 - Lower bracket wing
        '..#hllh##.......', # 14
        '...#hmlg#.......', # 15
        '....#hml#.......', # 16
        '.....#gh#.......', # 17 - Bottom finial
        '......##........', # 18
        '................', # 19
        '................', # 20
        '................', # 21
        '................', # 22
        '................', # 23
    ]
    return parse_grid(rows, pal, 16, 24)

def main():
    armor = create_armor_fracture()
    gravity = create_super_gravity()
    p1 = create_bell_warden_phase1()
    p2 = create_bell_warden_phase2()
    bracket_p1 = create_bar_right_bracket(1)
    bracket_p2 = create_bar_right_bracket(2)

    # Save to game assets
    armor.save(EFFECT_DIR / 'armor_fracture.png')
    gravity.save(EFFECT_DIR / 'super_gravity.png')
    p1.save(BOSS_BAR_DIR / 'bell_warden_phase1.png')
    p2.save(BOSS_BAR_DIR / 'bell_warden_phase2.png')
    bracket_p1.save(BOSS_BAR_DIR / 'bar_right_bracket_p1.png')
    bracket_p2.save(BOSS_BAR_DIR / 'bar_right_bracket_p2.png')
    # Backward compatibility
    bracket_p1.save(BOSS_BAR_DIR / 'bar_right_bracket.png')
    print('Saved textures to mod assets.')

    # Generate preview sheet at 4x and 10x scale for visual validation
    preview = Image.new('RGBA', (320, 160), (24, 28, 32, 255))
    # Paste icons at 4x scale
    def paste_scaled(img, x, y, scale=4):
        s = img.resize((img.width * scale, img.height * scale), Image.Resampling.NEAREST)
        preview.paste(s, (x, y), s)

    paste_scaled(armor, 16, 20, 4)
    paste_scaled(gravity, 100, 20, 4)
    paste_scaled(p1, 184, 16, 3)
    paste_scaled(p2, 250, 16, 3)
    preview.save(PREVIEW_DIR / 'icons_preview_sheet.png')
    print('Saved visual preview sheet to art/preview/icons_preview_sheet.png')

if __name__ == '__main__':
    main()
