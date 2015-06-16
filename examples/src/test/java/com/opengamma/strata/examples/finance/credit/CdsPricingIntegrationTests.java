/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit;

import org.testng.annotations.Test;

import java.time.LocalDate;

import static com.opengamma.strata.examples.finance.credit.TestHarness.cs01ParallelParShouldBe;
import static com.opengamma.strata.examples.finance.credit.TestHarness.ir01ParallelParShouldBe;
import static com.opengamma.strata.examples.finance.credit.TestHarness.pvShouldBe;

@Test
public class CdsPricingIntegrationTests {

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

  @Test
  public void test_ir01_parallel_par_on_raytheon_base_case() {
    ir01ParallelParShouldBe(963.5778398220427).on(baseDate);
  }

  @Test
  public void test_cs01_parallel_par_on_raytheon_base_case() {
    cs01ParallelParShouldBe(51873.837977122515).on(baseDate);
  }


}
