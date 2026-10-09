package org.rami5743.heph1;

/**
 * The key tables of the Windows layout he_ph1, read out of the KBDTABLES
 * structure of he_ph1.dll (see tools/dump_msklc_dll.py and docs/layout.md).
 *
 * Every letter key carries the character the Windows layout produces
 * unshifted and the character it produces with Shift, in the QWERTY
 * position it occupies there.
 */
final class Layout {

    /** A single key of the on-screen keyboard. */
    static final class Key {
        static final int NORMAL = 0;
        static final int SHIFT = 1;
        static final int BACKSPACE = 2;
        static final int ENTER = 3;
        static final int TO_SYMBOLS = 4;
        static final int TO_LETTERS = 5;
        static final int SPACE = 6;

        final int code;
        final String base;      // label and output in the unshifted state
        final String shifted;   // label and output with Shift held
        final String label;     // overrides base as the label, for special keys
        final float weight;

        private Key(int code, String base, String shifted, String label, float weight) {
            this.code = code;
            this.base = base;
            this.shifted = shifted;
            this.label = label;
            this.weight = weight;
        }

        static Key of(String base, String shifted) {
            return new Key(NORMAL, base, shifted, null, 1f);
        }

        static Key special(int code, String label, float weight) {
            return new Key(code, "", "", label, weight);
        }

        String labelFor(boolean shift) {
            if (label != null) return label;
            return shift ? shifted : base;
        }

        String outputFor(boolean shift) {
            return shift ? shifted : base;
        }
    }

    private static Key[] row(Key... keys) {
        return keys;
    }

    /** "base \t shifted" pairs, one per key, in QWERTY order. */
    private static Key[] pairs(String... spec) {
        Key[] keys = new Key[spec.length];
        for (int i = 0; i < spec.length; i++) {
            int tab = spec[i].indexOf('\t');
            keys[i] = Key.of(spec[i].substring(0, tab), spec[i].substring(tab + 1));
        }
        return keys;
    }

    private static final String SHIFT_LABEL = "⇧";
    private static final String BACKSPACE_LABEL = "⌫";
    private static final String ENTER_LABEL = "↵";
    private static final String SPACE_LABEL = "עברית";

    /**
     * The Hebrew layer. Rows 2-4 are exactly the VK_TO_WCHAR table of
     * he_ph1.dll; the apostrophe key at the end of row 3 is that layout's
     * OEM_7 key, and J carries its two-character ligature gimel+apostrophe.
     */
    static final Key[][] LETTERS = {
        pairs("1\t!", "2\t@", "3\t#", "4\t$", "5\t%",
              "6\t^", "7\t&", "8\t*", "9\t(", "0\t)"),
        pairs("ק\tQ", "ש\tW", "e\tE", "ר\tR", "ת\tט",
              "ט\tY", "ו\tU", "י\tI", "ו\tO", "פ\tף"),
        pairs("א\tע", "ס\tS", "ד\tD", "פ\tף", "ג\tG",
              "ה\tח", "ג'\tJ", "כ\tך", "ל\tL", "'\t\""),
        row(Key.special(Key.SHIFT, SHIFT_LABEL, 1.5f),
            Key.of("ז", "Z"), Key.of("ק", "X"), Key.of("צ", "ץ"),
            Key.of("ו", "V"), Key.of("ב", "B"), Key.of("נ", "ן"),
            Key.of("מ", "ם"),
            Key.special(Key.BACKSPACE, BACKSPACE_LABEL, 1.5f)),
        row(Key.special(Key.TO_SYMBOLS, "?123", 1.5f),
            Key.of(",", "<"),
            Key.special(Key.SPACE, SPACE_LABEL, 4f),
            Key.of(".", ">"),
            Key.special(Key.ENTER, ENTER_LABEL, 1.5f)),
    };

    /** Digits and punctuation, reached through the ?123 key. */
    static final Key[][] SYMBOLS = {
        pairs("1\t1", "2\t2", "3\t3", "4\t4", "5\t5",
              "6\t6", "7\t7", "8\t8", "9\t9", "0\t0"),
        pairs("-\t-", "=\t=", "[\t[", "]\t]", "\\\t\\",
              ";\t;", "'\t'", "`\t`", "/\t/", "₪\t₪"),
        pairs("_\t_", "+\t+", "{\t{", "}\t}", "|\t|",
              ":\t:", "\"\t\"", "~\t~", "?\t?", "@\t@"),
        row(Key.of("!", "!"), Key.of("#", "#"), Key.of("$", "$"), Key.of("%", "%"),
            Key.of("^", "^"), Key.of("&", "&"), Key.of("*", "*"), Key.of("(", "("),
            Key.of(")", ")"),
            Key.special(Key.BACKSPACE, BACKSPACE_LABEL, 1.5f)),
        row(Key.special(Key.TO_LETTERS, "אבג", 1.5f),
            Key.of(",", ","),
            Key.special(Key.SPACE, SPACE_LABEL, 4f),
            Key.of(".", "."),
            Key.special(Key.ENTER, ENTER_LABEL, 1.5f)),
    };

    private Layout() {}
}
