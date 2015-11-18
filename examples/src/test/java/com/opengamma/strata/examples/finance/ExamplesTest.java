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

import com.opengamma.strata.examples.report.ReportRunnerTool;
import com.opengamma.strata.examples.report.TradePortfolio;

/**
 * Test examples do not throw exceptions.
 */
@Test
public class ExamplesTest {

  private static final String[] NO_ARGS = new String[0];

  //-------------------------------------------------------------------------
  public void test_cdsPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> CdsPricingExample.main(NO_ARGS)));
  }

  public void test_cdsPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("cds"))));
  }

  //-------------------------------------------------------------------------
  public void test_dsfPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> DeliverableSwapFuturePricingExample.main(NO_ARGS)));
  }

  public void test_dsfPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("dsf"))));
  }

  //-------------------------------------------------------------------------
  public void test_fraPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> FraPricingExample.main(NO_ARGS)));
  }

  public void test_fraPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("fra"))));
  }

  //-------------------------------------------------------------------------
  public void test_fxPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> FxPricingExample.main(NO_ARGS)));
  }

  public void test_fxPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("fx"))));
  }

  //-------------------------------------------------------------------------
  public void test_genericFuturePricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> GenericFuturePricingExample.main(NO_ARGS)));
  }

  public void test_genericFuturePricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("future"))));
  }

  //-------------------------------------------------------------------------
  public void test_iborFuturePricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> StirFuturePricingExample.main(NO_ARGS)));
  }

  public void test_iborFuturePricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("stir-future"))));
  }

  //-------------------------------------------------------------------------
  public void test_swapPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> SwapPricingExample.main(NO_ARGS)));
  }

  public void test_swapPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("swap"))));
  }

  //-------------------------------------------------------------------------
  public void test_termDepositPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> TermDepositPricingExample.main(NO_ARGS)));
  }

  public void test_termDepositPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("term-deposit"))));
  }

  //-------------------------------------------------------------------------
  public void test_curveScenario() {
    String captured = caputureSystemOut(() -> CurveScenarioExample.main(NO_ARGS));
    assertTrue(captured.contains("PV01"));
    assertValidCaptured(captured);
  }

  //-------------------------------------------------------------------------
  public void test_historicalScenario() {
    String captured = caputureSystemOut(() -> HistoricalScenarioExample.main(NO_ARGS));
    assertTrue(captured.contains("Base PV"));
    assertValidCaptured(captured);
  }

  //-------------------------------------------------------------------------
  public void test_cdsTrade() {
    String captured = caputureSystemOut(() -> CdsTradeExample.main(NO_ARGS));
    assertTrue(captured.contains("<product>"));
    assertValidCaptured(captured);
  }

  //-------------------------------------------------------------------------
  public void test_swapTrade() {
    String captured = caputureSystemOut(() -> SwapTradeModelDemo.main(NO_ARGS));
    assertTrue(captured.contains("<product>"));
    assertValidCaptured(captured);
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

  //-------------------------------------------------------------------------
  public void test_calibration() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationCheckExample.main(NO_ARGS));
    assertTrue(captured.contains("Checked PV for all instruments used in the calibration set are near to zero"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  public void test_calibration_xccy() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationXCcyCheckExample.main(NO_ARGS));
    assertTrue(captured.contains("Checked PV for all instruments used in the calibration set are near to zero"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  //-------------------------------------------------------------------------
  private String[] toolArgs(String name) {
    return new String[] {
        "-p", "src/main/resources/example-portfolios/" + name + "-portfolio.xml",
        "-t", "src/main/resources/example-reports/" + name + "-report-template.ini",
        "-d", "2014-01-22",
    };
  }

  private void assertValidCapturedAsciiTable(String captured) {
    assertTrue(captured.contains("+------"), captured);
    assertValidCaptured(captured);
  }

  private void assertValidCaptured(String captured) {
    assertFalse(captured.contains("ERROR"), captured);
    assertFalse(captured.contains("FAIL"), captured);
    assertFalse(captured.contains("Exception"), captured);
  }

}
