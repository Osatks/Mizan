"""
Lower the Quranic pause marks (U+06D6..U+06DC) in Amiri Quran.

Tanzil's Uthmani text writes every pause mark as its own token after a space
("ٱلْقُدُسِ ۗ وَلَوْ"). The space is not a mark base in the font, so the mark is
drawn at its raw outline position, 1.3-1.8 em above the baseline, which lands in
the line above whenever lines are closer than in the printed Mushaf.

This script scales each mark to 82 %, centres it over the space that precedes it,
and lowers it so it sits just above the letters (bottom at ~0.78 em). The mark
anchors used by the font's mark-to-base lookups get the same transform, so marks
that do attach to a base (tatweel, dotted circle, sad forms) keep their position.
"""
import sys
from fontTools.ttLib import TTFont
from fontTools.pens.boundsPen import BoundsPen

src, dst = sys.argv[1], sys.argv[2]
SCALE = 0.82
BOTTOM = 780          # new ink bottom, font units (upm 1000)
f = TTFont(src)
cmap = f.getBestCmap()
glyf = f['glyf']; hmtx = f['hmtx']; gs = f.getGlyphSet()
space = hmtx[cmap[0x20]][0]
marks = [cmap[c] for c in range(0x06D6, 0x06DD)]
transforms = {}
for g in marks:
    bp = BoundsPen(gs); gs[g].draw(bp)
    x0, y0, x1, y1 = bp.bounds
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    h = (y1 - y0) * SCALE
    nx = space / 2                       # centre over the space before the mark
    ny = BOTTOM + h / 2
    def t(x, y, cx=cx, cy=cy, nx=nx, ny=ny):
        return round(nx + (x - cx) * SCALE), round(ny + (y - cy) * SCALE)
    transforms[g] = t
    gl = glyf[g]
    assert not gl.isComposite()
    coords = gl.coordinates
    for i in range(len(coords)):
        coords[i] = t(*coords[i])
    gl.recalcBounds(glyf)
    adv = hmtx[g][0]
    hmtx[g] = (adv, gl.xMin)

# the same transform for every mark anchor of these glyphs
gpos = f['GPOS'].table
moved = 0
for lk in gpos.LookupList.Lookup:
    for st in lk.SubTable:
        t = st.ExtSubTable if lk.LookupType == 9 else st
        lt = t.LookupType if lk.LookupType == 9 else lk.LookupType
        if lt not in (4, 5, 6):
            continue
        cov = t.Mark1Coverage.glyphs if lt == 6 else t.MarkCoverage.glyphs
        arr = t.Mark1Array if lt == 6 else t.MarkArray
        for g in marks:
            if g in cov:
                a = arr.MarkRecord[cov.index(g)].MarkAnchor
                a.XCoordinate, a.YCoordinate = transforms[g](a.XCoordinate, a.YCoordinate)
                moved += 1
        if lt == 6:
            cov2 = t.Mark2Coverage.glyphs
            for g in marks:
                if g in cov2:
                    for a in t.Mark2Array.Mark2Record[cov2.index(g)].Mark2Anchor:
                        if a is not None:
                            a.XCoordinate, a.YCoordinate = transforms[g](a.XCoordinate, a.YCoordinate)
                            moved += 1

# identify the modified version (OFL: modified versions should say so)
for rec in f['name'].names:
    if rec.nameID in (3, 5):
        rec.string = rec.toUnicode() + '; Mizan: Quranic pause marks lowered'
f.save(dst)
print('marks', len(marks), 'anchors moved', moved)
