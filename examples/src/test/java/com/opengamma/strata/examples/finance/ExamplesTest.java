/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.collect.TestHelper.caputureSystemOut;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Test examples do not throw exceptions.
 */
@Test
public class ExamplesTest {

  private static final String[] NO_ARGS = new String[0];

  public void test_csdPricing() {
    String captured = caputureSystemOut(() -> CdsPricingExample.main(NO_ARGS));
    assertTrue(captured.contains("+------"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  //-------------------------------------------------------------------------
  public void test_fraPricing() {
    String captured = caputureSystemOut(() -> FraPricingExample.main(NO_ARGS));
    assertTrue(captured.contains("+------"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  //-------------------------------------------------------------------------
  public void test_genericFuturePricing() {
    String captured = caputureSystemOut(() -> GenericFuturePricingExample.main(NO_ARGS));
    assertTrue(captured.contains("+------"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  //-------------------------------------------------------------------------
  public void test_swapPricing() {
    String captured = caputureSystemOut(() -> SwapPricingExample.main(NO_ARGS));
    assertTrue(captured.contains("+------"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  //-------------------------------------------------------------------------
  public void test_curveScenario() {
    String captured = caputureSystemOut(() -> CurveScenarioExample.main(NO_ARGS));
    assertTrue(captured.contains("PV01"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  //-------------------------------------------------------------------------
  public void test_historicalScenario() {
    String captured = caputureSystemOut(() -> HistoricalScenarioExample.main(NO_ARGS));
    assertTrue(captured.contains("Base PV"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  //-------------------------------------------------------------------------
  public void test_cdsTrade() {
    String captured = caputureSystemOut(() -> CdsTradeExample.main(NO_ARGS));
    assertTrue(captured.contains("<product>"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  //-------------------------------------------------------------------------
  public void test_swapTrade() {
    String captured = caputureSystemOut(() -> SwapTradeModelDemo.main(NO_ARGS));
    assertTrue(captured.contains("<product>"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

}
