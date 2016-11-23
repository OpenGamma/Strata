/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import static com.opengamma.strata.collect.TestHelper.caputureSystemOut;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;

import org.joda.beans.ser.JodaBeanSer;
import org.testng.annotations.Test;

import com.opengamma.strata.examples.CdsPricingExample;
import com.opengamma.strata.examples.DsfPricingExample;
import com.opengamma.strata.examples.FraPricingExample;
import com.opengamma.strata.examples.FxPricingExample;
import com.opengamma.strata.examples.GenericSecurityPricingExample;
import com.opengamma.strata.examples.StirFuturePricingExample;
import com.opengamma.strata.examples.SwapPricingExample;
import com.opengamma.strata.examples.TermDepositPricingExample;
import com.opengamma.strata.examples.finance.CalibrationCheckExample;
import com.opengamma.strata.examples.finance.CalibrationEur3CheckExample;
import com.opengamma.strata.examples.finance.CalibrationSimpleForwardCheckExample;
import com.opengamma.strata.examples.finance.CalibrationUsdCpiExample;
import com.opengamma.strata.examples.finance.CalibrationUsdFfsExample;
import com.opengamma.strata.examples.finance.CalibrationXCcyCheckExample;
import com.opengamma.strata.examples.finance.CdsScenarioExample;
import com.opengamma.strata.examples.finance.CdsTradeExample;
import com.opengamma.strata.examples.finance.CurveScenarioExample;
import com.opengamma.strata.examples.finance.HistoricalScenarioExample;
import com.opengamma.strata.examples.finance.SabrSwaptionCubeCalibrationExample;
import com.opengamma.strata.examples.finance.SabrSwaptionCubePvRiskExample;
import com.opengamma.strata.examples.finance.SwapPricingCcpExample;
import com.opengamma.strata.examples.finance.SwapPricingWithCalibrationExample;
import com.opengamma.strata.examples.finance.SwapTradeExample;
import com.opengamma.strata.examples.report.ReportRunnerTool;
import com.opengamma.strata.examples.report.TradeList;

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
    assertValidCapturedAsciiTable(caputureSystemOut(() -> DsfPricingExample.main(NO_ARGS)));
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
  public void test_genericSecurityPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> GenericSecurityPricingExample.main(NO_ARGS)));
  }

  public void test_genericSecurityPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("security"))));
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
  public void test_swapPricingCcp_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> SwapPricingCcpExample.main(NO_ARGS)));
  }

  //-------------------------------------------------------------------------
  public void test_swapPricingWithCalibration_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> SwapPricingWithCalibrationExample.main(NO_ARGS)));
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
    String captured = caputureSystemOut(() -> SwapTradeExample.main(NO_ARGS));
    assertTrue(captured.contains("<product>"));
    assertValidCaptured(captured);
  }

  //-------------------------------------------------------------------------
  public void test_portfolios() throws Exception {
    File baseDir = new File("src/main/resources/example-portfolios");
    assertTrue(baseDir.exists());
    for (File file : baseDir.listFiles(f -> f.getName().endsWith(".xml"))) {
      try (FileInputStream in = new FileInputStream(file)) {
        TradeList tradeList = JodaBeanSer.COMPACT.xmlReader().read(in, TradeList.class);
        assertTrue(tradeList.getTrades().size() > 0);
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

  public void test_calibration_eur3() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationEur3CheckExample.main(NO_ARGS));
    assertTrue(captured.contains("Checked PV for all instruments used in the calibration set are near to zero"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  public void test_calibration_simple_forward() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationSimpleForwardCheckExample.main(NO_ARGS));
    assertTrue(captured.contains("Checked PV for all instruments used in the calibration set are near to zero"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  public void test_calibration_usd_ffs() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationUsdFfsExample.main(NO_ARGS));
    assertTrue(captured.contains("Checked PV for all instruments used in the calibration set are near to zero"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  public void test_calibration_cpi() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationUsdCpiExample.main(NO_ARGS));
    assertTrue(captured.contains("Checked PV for all instruments used in the calibration set are near to zero"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  public void test_sabr_swaption_calibration() throws Exception {
    String captured = caputureSystemOut(() -> SabrSwaptionCubeCalibrationExample.main(NO_ARGS));
    assertTrue(captured.contains("End calibration"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  public void test_sabr_swaption_calibration_pv_risk() throws Exception {
    String captured = caputureSystemOut(() -> SabrSwaptionCubePvRiskExample.main(NO_ARGS));
    assertTrue(captured.contains("PV and risk time"));
    assertFalse(captured.contains("ERROR"));
    assertFalse(captured.contains("Exception"));
  }

  public void test_cds_scenario() throws Exception {
    String captured = caputureSystemOut(() -> CdsScenarioExample.main(NO_ARGS));
    assertTrue(captured.contains("95% VaR"));
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
    assertFalse(captured.contains("drill down"), captured);
  }

}
