/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import static com.opengamma.strata.collect.TestHelper.caputureSystemOut;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;

import org.joda.beans.ser.JodaBeanSer;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.examples.finance.CalibrationCheckExample;
import com.opengamma.strata.examples.finance.CalibrationEur3CheckExample;
import com.opengamma.strata.examples.finance.CalibrationSimpleForwardCheckExample;
import com.opengamma.strata.examples.finance.CalibrationUsdCpiExample;
import com.opengamma.strata.examples.finance.CalibrationUsdFfsExample;
import com.opengamma.strata.examples.finance.CalibrationXCcyCheckExample;
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
public class ExamplesTest {

  private static final String[] NO_ARGS = new String[0];

  //-------------------------------------------------------------------------
  @Test
  public void test_dsfPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> DsfPricingExample.main(NO_ARGS)));
  }

  @Test
  public void test_dsfPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("dsf"))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_fraPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> FraPricingExample.main(NO_ARGS)));
  }

  @Test
  public void test_fraPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("fra"))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_fxPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> FxPricingExample.main(NO_ARGS)));
  }

  @Test
  public void test_fxPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("fx"))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_genericSecurityPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> GenericSecurityPricingExample.main(NO_ARGS)));
  }

  @Test
  public void test_genericSecurityPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("security"))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_iborFuturePricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> StirFuturePricingExample.main(NO_ARGS)));
  }

  @Test
  public void test_iborFuturePricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("stir-future"))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_swapPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> SwapPricingExample.main(NO_ARGS)));
  }

  @Test
  public void test_swapPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("swap"))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_swapPricingCcp_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> SwapPricingCcpExample.main(NO_ARGS)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_swapPricingWithCalibration_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> SwapPricingWithCalibrationExample.main(NO_ARGS)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_termDepositPricing_standalone() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> TermDepositPricingExample.main(NO_ARGS)));
  }

  @Test
  public void test_termDepositPricing_tool() {
    assertValidCapturedAsciiTable(caputureSystemOut(() -> ReportRunnerTool.main(toolArgs("term-deposit"))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_curveScenario() {
    String captured = caputureSystemOut(() -> CurveScenarioExample.main(NO_ARGS));
    assertThat(captured.contains("PV01")).isTrue();
    assertValidCaptured(captured);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_historicalScenario() {
    String captured = caputureSystemOut(() -> HistoricalScenarioExample.main(NO_ARGS));
    assertThat(captured.contains("Base PV")).isTrue();
    assertValidCaptured(captured);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_swapTrade() {
    String captured = caputureSystemOut(() -> SwapTradeExample.main(NO_ARGS));
    assertThat(captured.contains("<product>")).isTrue();
    assertValidCaptured(captured);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_portfolios() throws Exception {
    File baseDir = new File("src/main/resources/example-portfolios");
    assertThat(baseDir.exists()).isTrue();
    for (File file : baseDir.listFiles(f -> f.getName().endsWith(".xml"))) {
      try (FileInputStream in = new FileInputStream(file)) {
        TradeList tradeList = JodaBeanSer.COMPACT.xmlReader().read(in, TradeList.class);
        assertThat(tradeList.getTrades().size() > 0).isTrue();
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_calibration() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationCheckExample.main(NO_ARGS));
    assertThat(captured.contains("Checked PV for all instruments used in the calibration set are near to zero")).isTrue();
    assertThat(captured.contains("ERROR")).isFalse();
    assertThat(captured.contains("Exception")).isFalse();
  }

  @Test
  public void test_calibration_xccy() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationXCcyCheckExample.main(NO_ARGS));
    assertThat(captured.contains("Checked PV for all instruments used in the calibration set are near to zero")).isTrue();
    assertThat(captured.contains("ERROR")).isFalse();
    assertThat(captured.contains("Exception")).isFalse();
  }

  @Test
  public void test_calibration_eur3() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationEur3CheckExample.main(NO_ARGS));
    assertThat(captured.contains("Checked PV for all instruments used in the calibration set are near to zero")).isTrue();
    assertThat(captured.contains("ERROR")).isFalse();
    assertThat(captured.contains("Exception")).isFalse();
  }

  @Test
  public void test_calibration_simple_forward() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationSimpleForwardCheckExample.main(NO_ARGS));
    assertThat(captured.contains("Checked PV for all instruments used in the calibration set are near to zero")).isTrue();
    assertThat(captured.contains("ERROR")).isFalse();
    assertThat(captured.contains("Exception")).isFalse();
  }

  @Test
  public void test_calibration_usd_ffs() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationUsdFfsExample.main(NO_ARGS));
    assertThat(captured.contains("Checked PV for all instruments used in the calibration set are near to zero")).isTrue();
    assertThat(captured.contains("ERROR")).isFalse();
    assertThat(captured.contains("Exception")).isFalse();
  }

  @Test
  public void test_calibration_cpi() throws Exception {
    String captured = caputureSystemOut(() -> CalibrationUsdCpiExample.main(NO_ARGS));
    assertThat(captured.contains("Checked PV for all instruments used in the calibration set are near to zero")).isTrue();
    assertThat(captured.contains("ERROR")).isFalse();
    assertThat(captured.contains("Exception")).isFalse();
  }

  @Test
  public void test_sabr_swaption_calibration() throws Exception {
    String captured = caputureSystemOut(() -> SabrSwaptionCubeCalibrationExample.main(NO_ARGS));
    assertThat(captured.contains("End calibration")).isTrue();
    assertThat(captured.contains("ERROR")).isFalse();
    assertThat(captured.contains("Exception")).isFalse();
  }

  @Test
  public void test_sabr_swaption_calibration_pv_risk() throws Exception {
    String captured = caputureSystemOut(() -> SabrSwaptionCubePvRiskExample.main(NO_ARGS));
    assertThat(captured.contains("PV and risk time")).isTrue();
    assertThat(captured.contains("ERROR")).isFalse();
    assertThat(captured.contains("Exception")).isFalse();
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
    assertThat(captured.contains("+------")).as(captured).isTrue();
    assertValidCaptured(captured);
  }

  private void assertValidCaptured(String captured) {
    assertThat(captured.contains("ERROR")).as(captured).isFalse();
    assertThat(captured.contains("FAIL")).as(captured).isFalse();
    assertThat(captured.contains("Exception")).as(captured).isFalse();
    assertThat(captured.contains("drill down")).as(captured).isFalse();
  }

}
