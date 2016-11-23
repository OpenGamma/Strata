/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit;

import static com.opengamma.strata.examples.finance.credit.harness.TestHarness.TradeFactory.withIndex0001;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.examples.finance.credit.harness.TestHarness;

@Test
public class CdsPricingIndex0001BaseIntegrationTests {

  final LocalDate valuationDate = LocalDate.of(2014, 10, 16);

  private TestHarness.TradeFactory onTrade() {
    return withIndex0001();
  }

  public void test_pvs_on_index_0001_base() {
    onTrade().pvShouldBe(-81969.80760349892).on(valuationDate);
  }

  public void test_par_rate_on_index_0001_base() {
    onTrade().parRateShouldBe(0.04550001684800108).on(valuationDate);
  }

  public void test_recovery01_on_index_0001_base() {
    onTrade().recovery01ShouldBe(35.42033064458519).on(valuationDate);
  }

  public void test_jump_to_default_on_index_0001_base() {
    onTrade().jumpToDefaultShouldBe(-69_918_030.1923965).on(valuationDate);
  }

  public void test_ir01_parallel_par_on_index_0001_base_case() {
    onTrade().ir01ParallelParShouldBe(400.30528782797046).on(valuationDate);
  }

  public void test_ir01_bucketed_par_on_index_0001_base_case() {
    onTrade()
        .ir01BucketedParShouldBe(
            -0.3070388601627201,
            1.6373047360684723,
            1.1052612347994000,
            7.6235177204944190,
            30.1579847754910600,
            75.1671701536979500,
            105.8998447621706900,
            130.1639933648985000,
            48.9234452510718260,
            0.0,
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

  public void test_cs01_parallel_par_on_index_0001_base_case() {
    onTrade().cs01ParallelParShouldBe(39322.262514410075).on(valuationDate);
  }

  public void test_cs01_bucketed_par_on_index_0001_base_case() {
    onTrade()
        .cs01BucketedParShouldBe(
            108.8729186840355400,
            200.6141297162976000,
            39019.7590950187300000,
            0.0,
            0.0)
        .on(valuationDate);
  }

}
