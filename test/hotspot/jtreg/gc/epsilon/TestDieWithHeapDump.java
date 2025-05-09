/*
 * Copyright (c) 2017, 2025, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package gc.epsilon;

/**
 * @test TestDieWithHeapDump
 * @requires vm.gc.Epsilon
 * @summary Epsilon GC should die on heap exhaustion with error handler attached
 * @library /test/lib
 * @run driver gc.epsilon.TestDieWithHeapDump
 */

import java.io.*;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestDieWithHeapDump {

  public static void passWith(String... args) throws Exception {
    OutputAnalyzer out = ProcessTools.executeTestJava(args);
    out.shouldNotContain("OutOfMemoryError");
    out.shouldHaveExitValue(0);
  }

  public static void failWith(String... args) throws Exception {
    ProcessBuilder pb = ProcessTools.createTestJavaProcessBuilder(args);
    Process p = pb.start();
    OutputAnalyzer out = new OutputAnalyzer(p);
    out.shouldContain("OutOfMemoryError");
    if (out.getExitValue() == 0) {
      throw new IllegalStateException("Should have failed with non-zero exit code");
    }
    String heapDump = "java_pid" + p.pid() + ".hprof";
    if (!new File(heapDump).exists()) {
      throw new IllegalStateException("Should have produced the heap dump at: " + heapDump);
    }
  }

  public static void main(String[] args) throws Exception {
    passWith("-Xmx64m",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-Dcount=1",
             "-XX:+HeapDumpOnOutOfMemoryError",
             TestDieWithHeapDump.Workload.class.getName());

    failWith("-Xmx64m",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:+HeapDumpOnOutOfMemoryError",
             TestDieWithHeapDump.Workload.class.getName());

    failWith("-Xmx64m",
             "-Xint",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:+HeapDumpOnOutOfMemoryError",
             TestDieWithHeapDump.Workload.class.getName());

    failWith("-Xmx64m",
             "-Xbatch",
             "-Xcomp",
             "-XX:TieredStopAtLevel=1",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:+HeapDumpOnOutOfMemoryError",
             TestDieWithHeapDump.Workload.class.getName());

    failWith("-Xmx64m",
             "-Xbatch",
             "-Xcomp",
             "-XX:-TieredCompilation",
             "-XX:+UnlockExperimentalVMOptions",
             "-XX:+UseEpsilonGC",
             "-XX:+HeapDumpOnOutOfMemoryError",
             TestDieWithHeapDump.Workload.class.getName());
  }

  public static class Workload {
    static int COUNT = Integer.getInteger("count", 1_000_000_000); // ~24 GB allocation

    static volatile Object sink;

    public static void main(String... args) {
      for (int c = 0; c < COUNT; c++) {
        sink = new Object();
      }
    }
  }

}
