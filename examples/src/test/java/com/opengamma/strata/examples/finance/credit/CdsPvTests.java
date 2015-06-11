/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit;

import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.examples.finance.credit.api.Calculator;
import com.opengamma.strata.examples.finance.credit.api.TradeSource;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

@Test
public class CdsPvTests {

  final LocalDate baseDate = LocalDate.of(2014, 10, 16);

  @Test
  public void test_pvs_on_raytheon_base_case() {
    pvShouldBe(0.004943638574331999).on(baseDate);
    pvShouldBe(159.42422560881823).on(baseDate.plusDays(1));
    pvShouldBe(318.6899835015647).on(baseDate.plusDays(2));
    pvShouldBe(-3693744.191810222).on(baseDate.plusMonths(1));
    pvShouldBe(-3693927.244211306).on(baseDate.plusYears(1));
    pvShouldBe(-3693745.0095456652).on(baseDate.plusYears(5)); // seems super wrong, should be converging on zero
  }

  // = test harness =============================================================
  public static PvOnADay pvShouldBe(double expected) {
    return new PvOnADay(expected);
  }

  public static class PvOnADay {
    private final double epsilon = 10e-9;
    private final TradeSource tradeSource = ExampleTradeSource.of();
    private final Calculator calculator = ExampleCalculator.of();

    private final double expected;

    public PvOnADay(double expected) {
      this.expected = expected;
    }

    public void on(LocalDate valuationDate) {
      double pv = calculator.calculateSimpleValue(valuationDate, tradeSource, Measure.PRESENT_VALUE).getAmount();
      Assert.assertEquals(pv, expected, epsilon);
    }
  }
}
