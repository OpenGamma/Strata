/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit;

import static com.opengamma.strata.examples.finance.credit.harness.TestHarness.TradeFactory.withCompany01;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.examples.finance.credit.harness.TestHarness;
import com.opengamma.strata.product.common.BuySell;

@Test
public class CdsPricingCompany01TminusOneTests {

  final LocalDate valuationDate = LocalDate.of(2014, 10, 16);
  final LocalDate cashSettleDate = LocalDate.of(2014, 10, 20);
  final double feeAmount = -3_694_117.73D;
  final BuySell buySell = BuySell.SELL;

  private TestHarness.TradeFactory onTrade() {
    return withCompany01(buySell, feeAmount, cashSettleDate);
  }

  public void test_pvs_on_company_01_t_minus_1() {
    onTrade().pvShouldBe(7_388_093.704033349).on(valuationDate);
  }

  public void test_par_rate_on_company_01_t_minus_1() {
    onTrade().parRateShouldBe(0.002800000823400466).on(valuationDate);
  }

  public void test_recovery01_on_company_01_minus_1() {
    onTrade().recovery01ShouldBe(-7.254387963563204).on(valuationDate);
  }

  public void test_jump_to_default_on_company_01_minus_1() {
    onTrade().jumpToDefaultShouldBe(-67_388_093.70403334).on(valuationDate);
  }

  public void test_ir01_parallel_par_on_company_01_t_minus_1() {
    onTrade().ir01ParallelParShouldBe(-972.8116886373609).on(valuationDate);
  }

  public void test_ir01_bucketed_par_on_company_01_t_minus_1() {
    onTrade()
        .ir01BucketedParShouldBe(
            -8.2257817937061190,
            -3.1341894399374723,
            -1.5775036029517650,
            -12.7210429105907680,
            -52.7185466177761550,
            -138.1065479721874000,
            -206.8782773185521400,
            -275.0521688470617000,
            -257.4313263930380300,
            -17.1471436154097320,
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

  public void test_cs01_parallel_par_on_company_01_t_minus_1() {
    onTrade().cs01ParallelParShouldBe(-51873.837977122515).on(valuationDate);
  }

  public void test_cs01_bucketed_par_on_company_01_t_minus_1() {
    onTrade()
        .cs01BucketedParShouldBe(
            -46.6094936728477500,
            -103.8638124940916900,
            -252.1060386206954700,
            -364.7911099912599000,
            -484.2151752971113000,
            -50640.1112342365100000)
        .on(valuationDate);
  }

}
