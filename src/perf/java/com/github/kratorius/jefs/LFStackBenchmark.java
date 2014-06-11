package com.github.kratorius.jefs;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
@Threads(4)
@State(Scope.Group)
public class LFStackBenchmark {
  private int constValue = 123;
  private LFStack<Integer> stack = new LFStack<>();

  @Benchmark
  @Group("stack")
  public void add() {
    stack.push(constValue);
  }

  @Benchmark
  @Group("stack")
  public Integer remove() {
    Integer v = stack.remove();
    if (v == null) {
      Thread.yield();
    }
    return v;
  }

  @TearDown(Level.Iteration)
  public void tearDown() {
    stack.clear();
  }

  public static void main(String[] args) throws RunnerException {
    Options options = new OptionsBuilder()
        .include(".*")
        .forks(1)
        .build();

    new Runner(options).run();
  }
}
