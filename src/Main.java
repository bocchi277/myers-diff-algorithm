import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assignment 1: Myers' diff.
 *
 *   java Main lines     A B    Part A: minimal line diff of file A to file B
 *   java Main highlight A B    Part B: the same diff plus changed-character ranges
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !(args[0].equals("lines") || args[0].equals("highlight"))) {
            System.err.println("usage: java Main lines|highlight A B");
            System.exit(2);
        }
        boolean highlight = args[0].equals("highlight");

        // Read both files before printing anything: on an error, stdout must stay empty.
        String[] a;
        String[] b;
        try {
            a = readLines(args[1]);
            b = readLines(args[2]);
        } catch (IOException e) {
            System.err.println("error: cannot read input: " + e);
            System.exit(2);
            return;                                   // not reached, but javac needs it
        }

        // Myers compares ints, so give every distinct line a number.
        Map<String, Integer> ids = new HashMap<>();
        String script = Myers.diff(toIds(a, ids), toIds(b, ids));

        OutputStream out = new BufferedOutputStream(System.out, 1 << 16);
        printDiff(out, script, a, b, highlight);
        out.flush();
    }

    /**
     * Reads a file as raw bytes and splits it on '\n'. A final '\n' does not create an
     * extra empty line, and a '\r' stays part of its line. ISO-8859-1 turns every byte
     * into exactly one char, so the String keeps the bytes unchanged.
     */
    static String[] readLines(String path) throws IOException {
        byte[] data = Files.readAllBytes(Path.of(path));
        List<String> lines = new ArrayList<>();
        int start = 0;                                // where the current line starts
        for (int i = 0; i < data.length; i++) {
            if (data[i] == '\n') {
                lines.add(new String(data, start, i - start, StandardCharsets.ISO_8859_1));
                start = i + 1;
            }
        }
        if (start < data.length) {                    // the last line has no '\n'
            lines.add(new String(data, start, data.length - start, StandardCharsets.ISO_8859_1));
        }
        return lines.toArray(new String[0]);
    }

    /** Replaces each line by a number. Equal lines get equal numbers: 0, 1, 2, ... */
    static int[] toIds(String[] lines, Map<String, Integer> ids) {
        int[] result = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            result[i] = ids.computeIfAbsent(lines[i], line -> ids.size());
        }
        return result;
    }

    /**
     * Prints the edit script. A change block is a run of '-' and '+' steps with no ' '
     * between them. Inside a block all '-' lines are printed first (the delete-first rule).
     */
    static void printDiff(OutputStream out, String script, String[] a, String[] b,
                          boolean highlight) throws IOException {
        int i = 0;                                    // next line of a
        int j = 0;                                    // next line of b
        int p = 0;                                    // position in the script
        while (p < script.length()) {
            if (script.charAt(p) == ' ') {            // keep: the line is in both files
                printLine(out, ' ', a[i]);
                i++;
                j++;
                p++;
                continue;
            }
            int deletes = 0;                          // a change block: count its steps
            int inserts = 0;
            while (p < script.length() && script.charAt(p) != ' ') {
                if (script.charAt(p) == '-') {
                    deletes++;
                } else {
                    inserts++;
                }
                p++;
            }
            for (int t = 0; t < deletes; t++) {
                printLine(out, '-', a[i + t]);
            }
            for (int t = 0; t < inserts; t++) {
                printLine(out, '+', b[j + t]);
                if (highlight && t < deletes) {       // the t-th '+' pairs with the t-th '-'
                    printHighlight(out, a[i + t], b[j + t]);
                }
            }
            i += deletes;
            j += inserts;
        }
    }

    static void printLine(OutputStream out, char prefix, String line) throws IOException {
        out.write(prefix);
        out.write(line.getBytes(StandardCharsets.ISO_8859_1));    // the original bytes
        out.write('\n');
    }

    /** Part B: diffs the characters of a line pair and prints "? old | new". */
    static void printHighlight(OutputStream out, String oldLine, String newLine)
            throws IOException {
        int[] oldChars = codePoints(oldLine);
        int[] newChars = codePoints(newLine);
        String script = Myers.diff(oldChars, newChars);

        boolean[] oldChanged = new boolean[oldChars.length];
        boolean[] newChanged = new boolean[newChars.length];
        int i = 0;                                    // position in oldChars
        int j = 0;                                    // position in newChars
        for (int p = 0; p < script.length(); p++) {
            char step = script.charAt(p);
            if (step == ' ') {
                i++;
                j++;
            } else if (step == '-') {
                oldChanged[i] = true;
                i++;
            } else {
                newChanged[j] = true;
                j++;
            }
        }
        String text = "? " + ranges(oldChanged) + " | " + ranges(newChanged) + "\n";
        out.write(text.getBytes(StandardCharsets.US_ASCII));
    }

    /** The characters of a line as Unicode code points (an emoji is one code point). */
    static int[] codePoints(String line) {
        byte[] bytes = line.getBytes(StandardCharsets.ISO_8859_1);  // back to the raw bytes
        return new String(bytes, StandardCharsets.UTF_8).codePoints().toArray();
    }

    /** Turns changed positions into ranges like "3-5,9-12" (end not included), or ".". */
    static String ranges(boolean[] changed) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < changed.length) {
            if (!changed[i]) {
                i++;
                continue;
            }
            int start = i;
            while (i < changed.length && changed[i]) {    // a run of changes is one range
                i++;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(start).append('-').append(i);
        }
        return sb.length() == 0 ? "." : sb.toString();
    }
}
