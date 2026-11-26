package io.dwg.api;

import java.util.ArrayList;
import java.util.List;

/**
 * The text of TEXT and MTEXT without their formatting codes, as a person reads it: for search,
 * indexing or display.
 * <ul>
 *   <li>Single line text: {@code %%d} °, {@code %%p} ±, {@code %%c} Ø, {@code %%%} %; the
 *       underline, overline and strike through toggles ({@code %%u}, {@code %%o}, {@code %%k})
 *       are dropped; {@code \U+XXXX} is the character.</li>
 *   <li>MTEXT also: {@code \P} (and {@code \X}, {@code \N}) start a new line, {@code \~} is a
 *       space, the codes with an argument ({@code \f}, {@code \H}, {@code \C}, ...) and the
 *       grouping braces are dropped, stacked text {@code \Sa^b;} is a/b, and {@code \\},
 *       {@code \{}, {@code \}} are the characters.</li>
 * </ul>
 */
public final class PlainText {

    private PlainText() {
    }

    /** Single line text (TEXT, ATTRIB, ATTDEF) without its control codes. */
    public static String ofText(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length() && s.charAt(i + 1) == '%') {
                char code = Character.toLowerCase(s.charAt(i + 2));
                i += 2;
                switch (code) {
                case 'd': sb.append('°'); break;
                case 'p': sb.append('±'); break;
                case 'c': sb.append('Ø'); break;
                case '%': sb.append('%'); break;
                default:  break;                // %%u, %%o, %%k : underline, overline, strike through
                }
            } else if (c == '\\' && s.startsWith("U+", i + 1) && i + 7 <= s.length()) {
                try {
                    sb.append((char) Integer.parseInt(s.substring(i + 3, i + 7), 16));
                    i += 6;
                } catch (NumberFormatException e) {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** The lines of an MTEXT content without its formatting codes. */
    public static List<String> linesOfMText(String s) {
        List<String> lines = new ArrayList<>();
        if (s == null) {
            lines.add("");
            return lines;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char code = s.charAt(++i);
                switch (code) {
                case 'P': case 'X': case 'N':
                    lines.add(sb.toString());
                    sb.setLength(0);
                    break;
                case '~':
                    sb.append(' ');
                    break;
                case '\\': case '{': case '}':
                    sb.append(code);
                    break;
                case 'U':
                    if (s.startsWith("+", i + 1) && i + 6 <= s.length()) {
                        try {
                            sb.append((char) Integer.parseInt(s.substring(i + 2, i + 6), 16));
                            i += 5;
                        } catch (NumberFormatException e) {
                            // not a code point : drop the escape
                        }
                    }
                    break;
                case 'S': {
                    // stacked text "\Sa^b;" or "\Sa/b;" -> a/b
                    int end = s.indexOf(';', i);
                    String stacked = end < 0 ? s.substring(i + 1) : s.substring(i + 1, end);
                    sb.append(stacked.replace('^', '/').replace('#', '/'));
                    i = end < 0 ? s.length() : end;
                    break;
                }
                case 'f': case 'F': case 'H': case 'W': case 'C': case 'c': case 'T': case 'Q': case 'A': case 'p': {
                    // codes with an argument ending in ';'
                    int end = s.indexOf(';', i);
                    i = end < 0 ? s.length() : end;
                    break;
                }
                default:
                    break;                      // \L \l \O \o \K \k : underline, overline, strike through
                }
            } else if (c == '{' || c == '}') {
                // grouping only
            } else if (c == '\n') {
                lines.add(sb.toString());
                sb.setLength(0);
            } else if (c != '\r') {
                sb.append(c);
            }
        }
        lines.add(sb.toString());
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(ofText(line));
        }
        return result;
    }

    /** An MTEXT content without its formatting codes, its lines joined by '\n'. */
    public static String ofMText(String s) {
        return String.join("\n", linesOfMText(s));
    }
}
