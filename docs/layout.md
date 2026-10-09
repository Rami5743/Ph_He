# The he_ph1 layout, as read out of the Windows DLL

The archive the layout was shipped in (`he_ph1 - Copy.zip`) is an MSKLC
package: an installer plus one DLL per architecture. A layout DLL contains no
real code — it exports a single function, `KbdLayerDescriptor`, whose whole
body is

```
mov eax, <address of the KBDTABLES structure>
ret
```

so the tables can be read straight out of the file. `tools/dump_msklc_dll.py`
does exactly that; `docs/reference/he_ph1_i386.dll` is the copy it was run on.

```
python3 tools/dump_msklc_dll.py docs/reference/he_ph1_i386.dll
```

## What the tables say

* `fLocaleFlags` `0x00010000`, no dead keys, three modifier columns
  (unshifted, Shift, Ctrl) and one ligature.
* The Ctrl column carries only the control characters every layout has
  (`Ctrl`+`[` is `U+001B` and so on), so nothing is lost by dropping it on a
  phone, which has no Ctrl key.
* The single ligature is on `J`: it produces two characters, `U+05D2 U+0027`,
  that is gimel followed by an ASCII apostrophe — the spelling of the `ג'`
  sound.

## The character table

Each cell is `unshifted` / `Shift`, in the QWERTY position of the key.

| key | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 0 |
|---|---|---|---|---|---|---|---|---|---|---|
| | 1 / ! | 2 / @ | 3 / # | 4 / $ | 5 / % | 6 / ^ | 7 / & | 8 / * | 9 / ( | 0 / ) |

| key | Q | W | E | R | T | Y | U | I | O | P |
|---|---|---|---|---|---|---|---|---|---|---|
| | ק / Q | ש / W | e / E | ר / R | ת / ט | ט / Y | ו / U | י / I | ו / O | פ / ף |

| key | A | S | D | F | G | H | J | K | L | ' |
|---|---|---|---|---|---|---|---|---|---|---|
| | א / ע | ס / S | ד / D | פ / ף | ג / G | ה / ח | ג' / J | כ / ך | ל / L | ' / " |

| key | Z | X | C | V | B | N | M | , | . | / |
|---|---|---|---|---|---|---|---|---|---|---|
| | ז / Z | ק / X | צ / ץ | ו / V | ב / B | נ / ן | מ / ם | , / < | . / > | / / ? |

All twenty-two letters and all five final forms are reachable. Three
characteristics of the layout are worth naming, because they look like
mistakes and are reproduced on purpose — the Android keyboard is meant to
behave exactly like the Windows one:

* `E` gives the **Latin** letter `e`, not a Hebrew letter.
* Several letters sit on more than one key: `ק` on `Q` and `X`, `ו` on `U`,
  `O` and `V`, `פ` on `P` and `F`, `ט` on `Y` and on `Shift`+`T`.
* Every key whose Shift position is not used by a Hebrew letter gives the
  Latin capital of that key instead, so the whole Latin alphabet except
  `A C E H K M N P T` is available through Shift.

If any of these should be changed on the phone, the one place to edit is
`android/src/org/rami5743/heph1/Layout.java`.

## What the phone keyboard adds

A phone has no Ctrl key and no second physical row of punctuation, so the
app adds what a soft keyboard needs and the DLL has no place for:

* a `?123` layer with the digits, the punctuation of the Windows layout and
  the shekel sign;
* an English layer — a plain QWERTY on the same keys, so one key gives the
  Hebrew letter and its Latin counterpart depending on the layer — and a
  language key next to `?123` that moves between the two. Held down rather
  than tapped, that key opens the phone's own list of keyboards, which is
  the way out to a keyboard this app does not provide;
* backspace, Enter, space and a Shift key that locks on a second tap;
* the apostrophe key (the layout's `OEM_7`) is kept at the end of the home
  row, where it is on a physical keyboard.

The chosen language survives moving between text fields; the Shift state and
the symbols layer do not.
