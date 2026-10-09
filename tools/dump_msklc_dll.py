#!/usr/bin/env python3
"""Print the key tables of a Windows keyboard layout DLL built by MSKLC.

    python3 tools/dump_msklc_dll.py path/to/he_ph1.dll

A layout DLL has no code beyond one exported function, KbdLayerDescriptor,
whose body is `mov eax, <address of KBDTABLES>; ret`. This script follows
that address and walks the structures it points at:

    KBDTABLES -> VK_TO_WCHAR_TABLE -> VK_TO_WCHARS   the character of each key
              -> MODIFIERS -> VK_TO_BIT              which columns mean Shift, Ctrl, AltGr
              -> DEADKEY                             accent + base -> composed
              -> LIGATURE                            one key -> several characters

Only the 32-bit (i386) DLL is read; the amd64 one holds the same tables with
8-byte pointers.
"""

import struct
import sys

WCH_NONE, WCH_DEAD, WCH_LGTR = 0xF000, 0xF001, 0xF002
MOD_NAMES = {0: '', 1: 'Shift', 2: 'Ctrl', 3: 'Shift+Ctrl',
             4: 'Alt', 5: 'Shift+Alt', 6: 'AltGr', 7: 'Shift+AltGr'}

VK_NAMES = {0x08: 'BACK', 0x09: 'TAB', 0x0D: 'RETURN', 0x1B: 'ESCAPE', 0x20: 'SPACE',
            0xBA: 'OEM_1 ;', 0xBB: 'OEM_PLUS =', 0xBC: 'OEM_COMMA ,', 0xBD: 'OEM_MINUS -',
            0xBE: 'OEM_PERIOD .', 0xBF: 'OEM_2 /', 0xC0: 'OEM_3 `', 0xDB: 'OEM_4 [',
            0xDC: 'OEM_5 \\', 0xDD: 'OEM_6 ]', 0xDE: "OEM_7 '", 0xE2: 'OEM_102'}
VK_NAMES.update({c: chr(c) for c in list(range(0x30, 0x3A)) + list(range(0x41, 0x5B))})


class PE:
    """Just enough of the PE format to resolve addresses and read exports."""

    def __init__(self, path):
        self.d = d = open(path, 'rb').read()
        pe = struct.unpack_from('<I', d, 0x3C)[0]
        if d[pe:pe + 4] != b'PE\0\0':
            raise ValueError('not a PE file')
        nsec, optsize = struct.unpack_from('<H', d, pe + 6)[0], struct.unpack_from('<H', d, pe + 20)[0]
        opt = pe + 24
        if struct.unpack_from('<H', d, opt)[0] == 0x20B:
            raise ValueError('64-bit DLL; pass the i386 one')
        self.imagebase = struct.unpack_from('<I', d, opt + 28)[0]
        sect = opt + optsize
        self.sections = [struct.unpack_from('<IIII', d, sect + i * 40 + 8) for i in range(nsec)]
        self.exp_rva = struct.unpack_from('<I', d, opt + 96)[0]

    def rva2off(self, rva):
        for vsize, vaddr, rawsize, rawptr in self.sections:
            if vaddr <= rva < vaddr + max(vsize, rawsize):
                return rawptr + rva - vaddr
        raise ValueError('rva %#x outside every section' % rva)

    def va2off(self, va):
        return self.rva2off(va - self.imagebase)

    def export(self, name):
        d, o = self.d, self.rva2off(self.exp_rva)
        nname = struct.unpack_from('<I', d, o + 24)[0]
        afunc, aname, aord = struct.unpack_from('<III', d, o + 28)
        fo, no, oo = self.rva2off(afunc), self.rva2off(aname), self.rva2off(aord)
        for i in range(nname):
            p = self.rva2off(struct.unpack_from('<I', d, no + 4 * i)[0])
            if d[p:d.index(b'\0', p)].decode('latin1') == name:
                idx = struct.unpack_from('<H', d, oo + 2 * i)[0]
                return struct.unpack_from('<I', d, fo + 4 * idx)[0]
        raise KeyError(name)


def wch(w):
    if w == WCH_NONE:
        return '--'
    if w == WCH_DEAD:
        return 'DEAD'
    if w == WCH_LGTR:
        return 'LIGATURE'
    return 'U+%04X %s' % (w, chr(w) if w >= 0x20 else '')


def main(path):
    p = PE(path)
    d = p.d
    stub = p.rva2off(p.export('KbdLayerDescriptor'))
    if d[stub] != 0xB8:                       # mov eax, imm32
        raise ValueError('unexpected export stub')
    o = p.va2off(struct.unpack_from('<I', d, stub + 1)[0])

    (p_mod, p_vk, p_dead, _names, _ext, _dead_names, _vsc, _max, _pad,
     _e0, _e1, locale_flags, n_lg, cb_lg, p_lig, dw_type, dw_sub) = \
        struct.unpack_from('<IIIIIIIBBxxIIIBBxxIII', d, o)
    print('fLocaleFlags %#010x  dwType %d  dwSubType %d\n' % (locale_flags, dw_type, dw_sub))

    mo = p.va2off(p_mod)
    p_bit, max_bits = struct.unpack_from('<IH', d, mo)
    col_of_bits = list(d[mo + 6: mo + 7 + max_bits])
    bits_of_col = {}
    for bits, col in enumerate(col_of_bits):
        bits_of_col.setdefault(col, bits)

    bo = p.va2off(p_bit)
    while d[bo]:
        print('modifier VK %#04x contributes bit %#04x' % (d[bo], d[bo + 1]))
        bo += 2
    print()

    print('=== VK -> characters ===')
    to = p.va2off(p_vk)
    while True:
        pv, nmod, cb = struct.unpack_from('<IBB', d, to)
        if not pv:
            break
        eo = p.va2off(pv)
        while d[eo]:
            vk, caps = d[eo], d[eo + 1]
            chars = struct.unpack_from('<%dH' % nmod, d, eo + 2)
            cols = ' | '.join('%s=%s' % (MOD_NAMES.get(bits_of_col.get(i, i), 'col%d' % i), wch(w))
                              for i, w in enumerate(chars))
            print('VK %02X %-14s caps=%d : %s' % (vk, VK_NAMES.get(vk, ''), caps, cols))
            eo += cb
        to += 8

    print('\n=== dead keys ===')
    if p_dead:
        do = p.va2off(p_dead)
        while True:
            both, comp, flags = struct.unpack_from('<IHH', d, do)
            if not both:
                break
            print('U+%04X + U+%04X -> U+%04X (flags %d)' % (both >> 16, both & 0xFFFF, comp, flags))
            do += 8

    print('\n=== ligatures ===')
    if p_lig:
        lo = p.va2off(p_lig)
        while True:
            vk, mod = struct.unpack_from('<HH', d, lo)
            if not vk:
                break
            chars = struct.unpack_from('<%dH' % n_lg, d, lo + 4)
            text = ''.join(chr(w) for w in chars if w != WCH_NONE)
            codes = ' '.join('U+%04X' % w for w in chars if w != WCH_NONE)
            print('VK %02X column %d -> %s   (%s)' % (vk, mod, text, codes))
            lo += cb_lg


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
