package io.dwg.bench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import io.dwg.api.DwgDocument;
import io.dwg.api.DwgReader;

/**
 * The time of one {@link DwgReader#open(byte[])}, for files of each version. The file is read
 * into memory first, so the disk is not measured. Files are given relative to the samples
 * folder ({@code -Ddwg.samples}, default {@code samples}); absolute paths work too. Run with
 * {@code -prof gc} to see the bytes allocated per read. See RELIABILITY.md for how to run it.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class ReadBenchmark {

    @Param({"example_r14.dwg", "example_2000.dwg", "example_2004.dwg", "example_2007.dwg",
            "example_2010.dwg", "example_2013.dwg", "example_2018.dwg", "2018/Dynblocks.dwg"})
    public String file;

    private byte[] data;

    @Setup(Level.Trial)
    public void load() throws Exception {
        Path p = Paths.get(file);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("dwg.samples", "samples"), file);
        }
        data = Files.readAllBytes(p);
    }

    @Benchmark
    public DwgDocument open() throws Exception {
        return DwgReader.defaultReader().open(data);
    }
}
