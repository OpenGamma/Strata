/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit;

import static com.opengamma.strata.examples.finance.credit.harness.TestHarness.TradeFactory.withCompany02;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.examples.finance.credit.harness.TestHarness;

@Test
public class CdsPricingCompany02BaseIntegrationTests {

  final LocalDate valuationDate = LocalDate.of(2014, 10, 16);

  private TestHarness.TradeFactory onTrade() {
    return withCompany02();
  }

  public void test_pvs_on_company_02_base() {
    onTrade().pvShouldBe(-2721105.5508106905).on(valuationDate);
  }

  public void test_par_rate_on_company_02_base() {
    onTrade().parRateShouldBe(0.04760001685586991).on(valuationDate);
  }

  public void test_recovery01_on_company_02_base() {
    onTrade().recovery01ShouldBe(31.899601166136563).on(valuationDate);
  }

  public void test_jump_to_default_on_company_02_base() {
    onTrade().jumpToDefaultShouldBe(-57_278_894.44918931).on(valuationDate);
  }

  public void test_ir01_parallel_par_on_company_02_base_case() {
    onTrade().ir01ParallelParShouldBe(251.36710846284404).on(valuationDate);
  }

  public void test_ir01_bucketed_par_on_company_02_base_case() {
    onTrade()
        .ir01BucketedParShouldBe(
            3.5543490587733686,
            0.8299544495530427,
            0.6575630567967892,
            4.1310315253213050,
            16.1657721209339800,
            39.7754475218243900,
            55.0854671462439000,
            67.7958203800953900,
            59.6295414022170000,
            3.7863637502305210,
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

  public void test_cs01_parallel_par_on_company_02_base_case() {
    onTrade().cs01ParallelParShouldBe(42190.92682103673).on(valuationDate);
  }

  public void test_cs01_bucketed_par_on_company_02_base_case() {
    onTrade()
        .cs01BucketedParShouldBe(
            10.8196099274791780,
            24.7800554852001370,
            61.9692914900369940,
            93.1008208445273300,
            128.6326188095845300,
            41885.0515951500300000)
        .on(valuationDate);
  }

}
