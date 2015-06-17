/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit;

import com.opengamma.strata.basics.BuySell;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static com.opengamma.strata.examples.finance.credit.TestHarness.TradeFactory.withTrade;

@Test
public class CdsPricingRaytheonTminusOneTests {

  final LocalDate valuationDate = LocalDate.of(2014, 10, 16);
  final LocalDate cashSettleDate = LocalDate.of(2014, 10, 20);
  final double feeAmount = -3_694_117.73D;
  final BuySell buySell = BuySell.SELL;

  private TestHarness.TradeFactory onTrade() {
    return withTrade(buySell, feeAmount, cashSettleDate);
  }

  @Test
  public void test_pvs_on_raytheon_t_minus_1() {
    onTrade().pvShouldBe(7_388_093.704033349).on(valuationDate);
  }

  @Test
  public void test_ir01_parallel_par_on_raytheon_t_minus_1() {
    onTrade().ir01ParallelParShouldBe(-972.8116886373609).on(valuationDate);
  }

  @Test
  public void test_ir01_bucketed_par_on_raytheon_t_minus_1() {
    onTrade()
        .ir01BucketedParShouldBe(
            -8.225781793706119,
            -3.1341894399374723,
            -1.5775036020204425,
            -12.721042910590768,
            -52.718546617776155,
            -138.10654797125608,
            -206.87827731855214,
            -275.0521688470617,
            -257.43132639396936,
            -17.147143615409732,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0)
        .on(valuationDate);
  }

  @Test
  public void test_cs01_parallel_par_on_raytheon_t_minus_1() {
    onTrade().cs01ParallelParShouldBe(-51873.837977122515).on(valuationDate);
  }

  @Test
  public void test_cs01_bucketed_par_on_raytheon_t_minus_1() {
    onTrade()
        .cs01BucketedParShouldBe(
            -46.60949367284775,
            -103.86381249502301,
            -252.10603862069547,
            -364.7911099921912,
            -484.2151752971113,
            -50640.11123423651,
            0.0,
            0.0)
        .on(valuationDate);
  }


}
