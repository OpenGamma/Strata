/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link NumberFormatter}.
 */
@Test
public class NumberFormatterTest {

  private static final String NAN = DecimalFormatSymbols.getInstance(Locale.ENGLISH).getNaN();
  private static final String INF = DecimalFormatSymbols.getInstance(Locale.ENGLISH).getInfinity();

  //-------------------------------------------------------------------------
  @DataProvider(name = "standard")
  Object[][] data_standard() {
    return new Object[][] {
        {true, 0, 0, 123, "123", 123d},
        {true, 0, 0, 12345.678, "12,346", 12346d},
        {true, 0, 0, 12345678.9, "12,345,679", 12345679d},

        {false, 0, 0, 123, "123", 123},
        {false, 0, 0, 12345.678, "12346", 12346d},
        {false, 0, 0, 12345678.9, "12345679", 12345679d},

        {true, 1, 1, 123, "123.0", 123d},
        {true, 1, 1, 12345.678, "12,345.7", 12345.7d},
        {true, 1, 1, 12345678.9, "12,345,678.9", 12345678.9d},

        {true, 1, 3, 123, "123.0", 123d},
        {true, 1, 3, 12345.678, "12,345.678", 12345.678d},
        {true, 1, 3, 12345678.9, "12,345,678.9", 12345678.9d},
        {true, 1, 3, 12345678.91, "12,345,678.91", 12345678.91d},

        {true, 0, 3, -12345.67d, "-12,345.67", -12345.67d},
        {true, 0, 3, -12345.67e30, "-12,345,670,000,000,000,000,000,000,000,000,000", -12345.67e30},

        {true, 0, 3, -0d, "-0", -0d},
        {true, 0, 3, Double.NaN, NAN, Double.NaN},
        {true, 0, 3, Double.POSITIVE_INFINITY, INF, Double.POSITIVE_INFINITY},
        {true, 0, 3, Double.NEGATIVE_INFINITY, "-" + INF, Double.NEGATIVE_INFINITY},
    };
  }

  @Test(dataProvider = "standard")
  public void test_of_3arg(boolean grouping, int minDp, int maxDp, double value, String expected, double parsed) {
    String text = NumberFormatter.of(grouping, minDp, maxDp).format(value);
    assertEquals(text, expected);
  }

  @Test(dataProvider = "standard")
  public void test_of_2arg(boolean grouping, int minDp, int maxDp, double value, String expected, double parsed) {
    if (minDp == maxDp) {
      String text = NumberFormatter.of(grouping, minDp).format(value);
      assertEquals(text, expected);
    }
  }

  @Test(dataProvider = "standard")
  public void test_parse(boolean grouping, int minDp, int maxDp, double value, String expected, double parsed) {
    NumberFormatter formatter = NumberFormatter.of(grouping, minDp, maxDp);
    String text = formatter.format(value);
    double actual = formatter.parse(text);
    assertEquals(actual, parsed, 0d);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "percentage")
  Object[][] data_percentage() {
    return new Object[][] {
        {true, 0, 0, 1.23, "123%"},
        {true, 0, 0, 123.45678, "12,346%"},
        {true, 0, 0, 123456.789, "12,345,679%"},

        {false, 0, 0, 1.23, "123%"},
        {false, 0, 0, 123.4578, "12346%"},
        {false, 0, 0, 123456.789, "12345679%"},

        {true, 1, 1, 1.23, "123.0%"},
        {true, 1, 1, 123.45678, "12,345.7%"},
        {true, 1, 1, 123456.789, "12,345,678.9%"},

        {true, 1, 3, 1.23, "123.0%"},
        {true, 1, 3, 123.45678, "12,345.678%"},
        {true, 1, 3, 123456.789, "12,345,678.9%"},
        {true, 1, 3, 123456.7891, "12,345,678.91%"},
    };
  }

  @Test(dataProvider = "percentage")
  public void test_ofPercentage(boolean grouping, int minDp, int maxDp, double value, String expected) {
    String text = NumberFormatter.ofPercentage(grouping, minDp, maxDp).format(value);
    assertEquals(text, expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "patterns")
  Object[][] data_patterns() {
    return new Object[][] {
        {"0", 12345.678, "12346"},
        {"00", 12345.678, "12346"},
        {"#,##0", 12345.678, "12,346"},

        {"#,##0.00", 12345, "12,345.00"},
        {"#,##0.00", 12345.6, "12,345.60"},
        {"#,##0.00", 12345.678, "12,345.68"},

        {"#,##0.##", 12345, "12,345"},
        {"#,##0.##", 12345.6, "12,345.6"},
        {"#,##0.##", 12345.678, "12,345.68"},
    };
  }

  @Test(dataProvider = "patterns")
  public void test_ofPattern(String pattern, double value, String expected) {
    String java = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(value);
    String strata = NumberFormatter.ofPattern(pattern, Locale.ENGLISH).format(value);
    assertEquals(strata, java);
    assertEquals(strata, expected);
  }

  //-------------------------------------------------------------------------
  public void test_ofLocalizedNumber() {
    String text = NumberFormatter.ofLocalizedNumber(Locale.ENGLISH).format(12345.678);
    assertEquals(text, "12,345.678");
  }

  //-------------------------------------------------------------------------
  @Test(enabled = false)
  public void test_javaBroken() throws Exception {
    // uncomment system out to see how broken it is
    // very specific format instance needed
    DecimalFormat format = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.ENGLISH));
    Random random = new Random(1);
    CountDownLatch latch = new CountDownLatch(1);
    AtomicInteger broken = new AtomicInteger();
    int threadCount = 15;
    for (int i = 0; i < threadCount; i++) {
      Runnable runner = () -> {
        try {
          latch.await();
          int val = random.nextInt(999);
          String a = format.format((double) val);
          String b = Integer.valueOf(val).toString();
          System.out.println(a + " " + b);
          if (!a.equals(b)) {
            broken.incrementAndGet();
          }
        } catch (Exception ex) {
          System.out.println("Exception: " + ex.getMessage());
        }
      };
      new Thread(runner, "TestThread" + i).start();
    }
    // start all threads together
    latch.countDown();
    Thread.sleep(1000);
    System.out.println("Broken: " + broken.get());
    assertTrue(broken.get() > 0);
  }

  //-------------------------------------------------------------------------
  @Test(enabled = false)
  public void test_performance() throws Exception {
    ThreadLocal<DecimalFormat> thread =
        ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.ENGLISH)));
    DecimalFormat java = new DecimalFormat("#,##0.###", new DecimalFormatSymbols(Locale.ENGLISH));
    NumberFormatter strata = NumberFormatter.of(true, 0, 3);
    Random random = new Random(1);

    for (int i = 0; i < 20; i++) {
      long start0 = System.nanoTime();
      for (int j = 0; j < 100_000; j++) {
        double val = random.nextDouble();
        String str = java.format(val);
        if (str.length() == 0) {
          throw new IllegalStateException("Just to avoid dead code elimination: " + str);
        }
      }
      long end0 = System.nanoTime();
      System.out.println("  Java: " + ((end0 - start0) / 1_000_000d) + "ms");

      long start1 = System.nanoTime();
      for (int j = 0; j < 100_000; j++) {
        double val = random.nextDouble();
        String str = thread.get().format(val);
        if (str.length() == 0) {
          throw new IllegalStateException("Just to avoid dead code elimination: " + str);
        }
      }
      long end1 = System.nanoTime();
      System.out.println("JavaTL: " + ((end1 - start1) / 1_000_000d) + "ms");

      long start1b = System.nanoTime();
      for (int j = 0; j < 100_000; j++) {
        double val = random.nextDouble();
        String str = ((NumberFormat) java.clone()).format(val);
        if (str.length() == 0) {
          throw new IllegalStateException("Just to avoid dead code elimination: " + str);
        }
      }
      long end1b = System.nanoTime();
      System.out.println("JavaCl: " + ((end1b - start1b) / 1_000_000d) + "ms");

      long start2 = System.nanoTime();
      for (int j = 0; j < 100_000; j++) {
        double val = random.nextDouble();
        String str = strata.format(val);
        if (str.length() == 0) {
          throw new IllegalStateException("Just to avoid dead code elimination: " + str);
        }
      }
      long end2 = System.nanoTime();
      System.out.println("Strata: " + ((end2 - start2) / 1_000_000d) + "ms");
    }
  }

}
