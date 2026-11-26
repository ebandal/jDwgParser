package io.dwg.tools;

import io.dwg.api.*;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * The read results of all files under the given folders, by version: files, files that open,
 * objects, objects with an issue, section issues, read time (each file read twice, the second
 * time measured), and the files that do not open with the reason.
 */
public class SuccessReport {
    public static void main(String[] a) throws Exception {
        List<Path> files = new ArrayList<>();
        for (String d : a) try (Stream<Path> s = Files.walk(Paths.get(d))) {
            s.filter(p -> p.toString().toLowerCase().endsWith(".dwg")).sorted().forEach(files::add);
        }
        PrintStream out = System.out;
        System.setOut(new PrintStream(PrintStream.nullOutputStream()));
        System.setErr(new PrintStream(PrintStream.nullOutputStream()));
        Map<String, long[]> v = new TreeMap<>(); // files, opened, objects, objects with issue, section issues, nanos, bytes
        List<String> failed = new ArrayList<>();
        for (Path p : files) {
            byte[] b = Files.readAllBytes(p);
            String ver = b.length >= 6 ? new String(b, 0, 6) : "?";
            long[] s = v.computeIfAbsent(ver, k -> new long[7]);
            s[0]++;
            try {
                DwgReader.defaultReader().open(b);
                long t0 = System.nanoTime();
                DwgDocument doc = DwgReader.defaultReader().open(b);
                s[5] += System.nanoTime() - t0;
                s[6] += b.length;
                s[1]++;
                s[2] += doc.objectMap().size();
                Set<Long> bad = new HashSet<>();
                for (ReadIssue i : doc.issues()) {
                    if (i.kind() == ReadIssue.Kind.SECTION) s[4]++; else bad.add(i.handle());
                }
                s[3] += bad.size();
            } catch (Exception e) {
                failed.add(p.getFileName() + " (" + ver + "): " + e.getMessage());
            }
        }
        out.println("| version | files | open | objects | objects with an issue | section issues | MB | read time (s) | MB/s |");
        out.println("|---|---|---|---|---|---|---|---|---|");
        long[] t = new long[7];
        v.forEach((k, s) -> {
            for (int i = 0; i < 7; i++) t[i] += s[i];
            out.println(row(k, s));
        });
        out.println(row("all", t));
        out.println();
        failed.forEach(f -> out.println("- " + f));
    }

    static String row(String k, long[] s) {
        double mb = s[6] / 1e6, sec = s[5] / 1e9;
        return String.format("| %s | %d | %d | %d | %d | %d | %.1f | %.2f | %.1f |", k, s[0], s[1], s[2], s[3], s[4], mb, sec, sec > 0 ? mb / sec : 0);
    }
}
