package io.dwg.tools;

import io.dwg.api.*;
import java.io.PrintStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * Fuzzes DwgReader.open: each run takes a sample (files under the given folders, at most
 * -Dmaxsize bytes), changes it at random (byte flips, overwritten ranges, zeroed ranges,
 * truncation), opens it with a time limit, and counts the outcome: opened, an exception (by
 * class and the first frame in io.dwg), a timeout, OutOfMemoryError or StackOverflowError.
 * Runs are numbered from a fixed seed (-Dseed, -Druns), so a run can be reproduced (-Donly=N
 * writes that case to -Dcase=file).
 */
public class ReadFuzz {
    public static void main(String[] a) throws Exception {
        long seed = Long.getLong("seed", 1);
        int runs = Integer.getInteger("runs", 2000);
        long maxSize = Long.getLong("maxsize", 300_000);
        long timeoutMs = Long.getLong("timeout", 10_000);
        List<Path> files = new ArrayList<>();
        for (String d : a) try (Stream<Path> s = Files.walk(Paths.get(d))) {
            s.filter(p -> p.toString().toLowerCase().endsWith(".dwg")).filter(p -> p.toFile().length() <= maxSize)
             .sorted().forEach(files::add);
        }
        List<byte[]> data = new ArrayList<>();
        for (Path p : files) data.add(Files.readAllBytes(p));
        PrintStream out = System.out;
        System.setOut(new PrintStream(PrintStream.nullOutputStream()));
        System.setErr(new PrintStream(PrintStream.nullOutputStream()));
        out.println(files.size() + " files, " + runs + " runs, seed " + seed);
        Map<String, Integer> outcomes = new TreeMap<>();
        Map<String, String> first = new TreeMap<>();
        ExecutorService pool = Executors.newCachedThreadPool(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
        Integer only = Integer.getInteger("only");
        for (int run = 0; run < runs; run++) {
            Random rnd = new Random(seed * 1_000_003L + run);
            int which = rnd.nextInt(data.size());
            byte[] d = mutate(data.get(which), rnd);
            if (only != null) {
                if (run != only) continue;
                Files.write(Paths.get(System.getProperty("case", "case.dwg")), d);
                out.println("run " + run + " from " + files.get(which));
            }
            Future<String> f = pool.submit(() -> {
                try {
                    DwgDocument doc = DwgReader.defaultReader().open(d);
                    return "opened";
                } catch (Throwable t) {
                    if (only != null) t.printStackTrace(out);
                    return describe(t);
                }
            });
            String k;
            try {
                k = f.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                f.cancel(true);
                k = "TIMEOUT";
            }
            outcomes.merge(k, 1, Integer::sum);
            first.putIfAbsent(k, "run " + run + " (" + files.get(which).getFileName() + ")");
            if (k.equals("TIMEOUT")) {
                out.println("timeout at run " + run + " from " + files.get(which));
            }
        }
        outcomes.forEach((k, v) -> out.println(String.format("%6d  %s   first: %s", v, k, first.get(k))));
        System.exit(0);
    }

    static String describe(Throwable t) {
        if (t instanceof DwgReadException && t.getCause() != null && t.getCause() != t) {
            return "DwgReadException <- " + describe(t.getCause());
        }
        String where = "";
        for (StackTraceElement e : t.getStackTrace()) {
            if (e.getClassName().startsWith("io.dwg")) { where = e.getClassName().replaceAll(".*\\.", "") + "." + e.getMethodName() + ":" + e.getLineNumber(); break; }
        }
        return t.getClass().getSimpleName() + " @ " + where;
    }

    static byte[] mutate(byte[] src, Random r) {
        byte[] d = src.clone();
        switch (r.nextInt(5)) {
            case 0: for (int i = 1 + r.nextInt(20); i > 0; i--) d[r.nextInt(d.length)] ^= (byte) (1 << r.nextInt(8)); break;
            case 1: for (int i = 1 + r.nextInt(5); i > 0; i--) d[r.nextInt(d.length)] = (byte) r.nextInt(256); break;
            case 2: { int at = r.nextInt(d.length), n = Math.min(d.length - at, 1 + r.nextInt(64)); for (int i = 0; i < n; i++) d[at + i] = (byte) r.nextInt(256); break; }
            case 3: { int at = r.nextInt(d.length), n = Math.min(d.length - at, 1 + r.nextInt(256)); Arrays.fill(d, at, at + n, (byte) (r.nextBoolean() ? 0 : 0xFF)); break; }
            default: d = Arrays.copyOf(d, Math.max(6, r.nextInt(d.length))); break;
        }
        return d;
    }
}
