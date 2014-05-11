package com.github.kratorius.jefs;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
@State(Scope.Group)
public class LFStackBenchmark {
  private static final Integer VALUE = 123;
  private LFStack<Integer> stack = new LFStack<>();

  @GenerateMicroBenchmark
  @GroupThreads(2)
  @Group("stack")
  public void add() {
    stack.push(VALUE);
  }

  @GenerateMicroBenchmark
  @Group("stack")
  public void remove() {
    if (stack.remove() == null) {
      Thread.yield();
    }
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
