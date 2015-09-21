/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.collect.TestHelper.caputureSystemOut;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.joda.beans.ser.JodaBeanSer;
import org.testng.annotations.Test;

import com.opengamma.strata.examples.report.TradePortfolio;

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
  public void test_fxPricing() {
    String captured = caputureSystemOut(() -> FxPricingExample.main(NO_ARGS));
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

  //-------------------------------------------------------------------------
  public void test_portfolios() throws Exception {
    File baseDir = new File("src/main/resources/example-portfolios");
    assertTrue(baseDir.exists());
    for (File file : baseDir.listFiles(f -> f.getName().endsWith(".xml"))) {
      try (FileInputStream in = new FileInputStream(file)) {
        TradePortfolio portfolio = JodaBeanSer.COMPACT.xmlReader().read(in, TradePortfolio.class);
        assertTrue(portfolio.getTrades().size() > 0);
      }
    }
  }

}
