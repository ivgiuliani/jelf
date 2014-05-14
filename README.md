JELF, Java Experimental Lock Free Data Structures
=================================================

A collection of experimental lock free algorithms and data structures.
I've done this mostly for experimentation, so it's definitely not production
ready.


Setup
-----
If using Intellij IDEA:

    ./gradlew idea


If using eclipse:

    ./gradlew eclipse


Running tests
-------------
Give at least 1Gb of RAM (`-Xms1G`) or the garbage collector will slow you down
considerably. Generally, 3Gb should be enough to run all the tests without
incurring in garbage collection penalties (`-Xms3G -Xmx3G` seems to work just fine
on my Intel i5).


Benchmarking
------------

    ./gradlew perf

It's possible to pass custom arguments to JMH, for example:

    ./gradlew perf -Pargs='com.github.kratorius.jefs.LFStackBenchmark.* -wi 3 -i 3'

If no arguments are specified, JMH will execute all the benchmarks (which might take
a while).

License
-------
This software is distributed under the BSD license. See the LICENSE file
for more informations.

[![Analytics](https://ga-beacon.appspot.com/UA-184881-14/jelf)](https://github.com/igrigorik/ga-beacon)
