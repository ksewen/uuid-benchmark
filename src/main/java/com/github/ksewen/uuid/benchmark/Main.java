package com.github.ksewen.uuid.benchmark;

import com.github.ksewen.uuid.enums.CaseType;
import java.text.MessageFormat;
import java.util.Scanner;
import org.apache.commons.lang3.StringUtils;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author ksewen
 * @date 2019-12-1016:50
 */
public class Main {

  private static final String DEFAULT_LOG_FILE = "benchmark-{0}-thread-{1}.log";

  public static void main(String[] args) throws RunnerException {
    Scanner sc = new Scanner(System.in);
    System.out.println("please input the benchmark type UUID/KUID: ");
    String type = sc.nextLine();
    System.out.println("please input the output file: ");
    String output = sc.nextLine();
    System.out.println("please input the thread count: ");
    int threads = sc.nextInt();

    if (StringUtils.isBlank(type)) {
      throw new IllegalArgumentException("empty type is invalid");
    }

    if (threads == 0) {
      threads = 1;
      System.out.println("unspecified threads, use default value = 1");
    }

    if (StringUtils.isBlank(output)) {
      output = MessageFormat.format(DEFAULT_LOG_FILE, type, threads);
      System.out.println("unspecified output file, use default = " + output);
    }

    Options options =
        new OptionsBuilder()
            .include(CaseType.forName(type).getType())
            .threads(threads)
            .output(output)
            .build();
    new Runner(options).run();
  }
}
