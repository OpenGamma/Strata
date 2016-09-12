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
public class CdsPricingCompany01TminusThreeTests {

  final LocalDate valuationDate = LocalDate.of(2014, 10, 16);
  final LocalDate cashSettleDate = LocalDate.of(2014, 10, 16);
  final double feeAmount = -3_694_117.73D;
  final BuySell buySell = BuySell.BUY;

  private TestHarness.TradeFactory onTrade() {
    return withCompany01(buySell, feeAmount, cashSettleDate);
  }

  public void test_pvs_on_company_01_t_minus_3() {
    onTrade().pvShouldBe(-3_694_038.9745626342).on(valuationDate);
  }

  public void test_par_rate_on_company_01_t_minus_3() {
    onTrade().parRateShouldBe(0.002800000823400466).on(valuationDate);
  }

  public void test_recovery01_on_company_01_t_minus_3() {
    onTrade().recovery01ShouldBe(7.254387962631881).on(valuationDate);
  }

  public void test_jump_to_default_on_company_01_t_minus_3() {
    onTrade().jumpToDefaultShouldBe(-56_305_961.02543736).on(valuationDate);
  }

  public void test_ir01_parallel_par_on_company_01_t_minus_3() {
    onTrade().ir01ParallelParShouldBe(968.7077457923442).on(valuationDate);
  }

  public void test_ir01_bucketed_par_on_company_01_t_minus_3() {
    onTrade()
        .ir01BucketedParShouldBe(
            4.1218389491550620,
            3.1341894390061498,
            1.5775036020204425,
            12.7210429105907680,
            52.7185466168448300,
            138.1065479712560800,
            206.8782773185521400,
            275.0521688465960300,
            257.4313263925724000,
            17.1471436149440700,
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

  public void test_cs01_parallel_par_on_company_01_t_minus_3() {
    onTrade().cs01ParallelParShouldBe(51873.83797712205).on(valuationDate);
  }

  public void test_cs01_bucketed_par_on_company_01_t_minus_3() {
    onTrade()
        .cs01BucketedParShouldBe(
            46.6094936723820900,
            103.8638124936260300,
            252.1060386202298000,
            364.7911099907942000,
            484.2151752961799500,
            50640.1112342355800000)
        .on(valuationDate);
  }

}
