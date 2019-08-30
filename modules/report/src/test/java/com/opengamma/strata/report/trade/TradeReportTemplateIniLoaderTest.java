/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * Test {@link TradeReportTemplateIniLoader}.
 */
public class TradeReportTemplateIniLoaderTest {

  @Test
  public void test_simple_values() {
    TradeReportTemplate template = parseIni("trade-report-test-simple.ini");

    TradeReportColumn productColumn = TradeReportColumn.builder()
        .value("Product")
        .header("Product")
        .build();

    TradeReportColumn pvColumn = TradeReportColumn.builder()
        .value("Measures.PresentValue")
        .header("Present Value")
        .build();

    assertThat(template.getColumns()).hasSize(2);
    assertThat(template.getColumns().get(0)).isEqualTo(productColumn);
    assertThat(template.getColumns().get(1)).isEqualTo(pvColumn);
  }

  @Test
  public void test_path() {
    TradeReportTemplate template = parseIni("trade-report-test-path.ini");

    TradeReportColumn payLegCcyColumn = TradeReportColumn.builder()
        .value("Measures.LegInitialNotional.pay.currency")
        .header("Pay Leg Ccy")
        .build();

    assertThat(template.getColumns()).hasSize(1);
    assertThat(template.getColumns().get(0)).isEqualTo(payLegCcyColumn);
  }

  @Test
  public void test_ignore_failures() {
    TradeReportTemplate template = parseIni("trade-report-test-ignore-failures.ini");

    TradeReportColumn payLegCcyColumn = TradeReportColumn.builder()
        .value("Measures.LegInitialNotional.pay.currency")
        .header("Pay Leg Ccy")
        .build();

    TradeReportColumn pvColumn = TradeReportColumn.builder()
        .value("Measures.PresentValue")
        .header("Present Value")
        .ignoreFailures(true)
        .build();

    assertThat(template.getColumns()).hasSize(2);
    assertThat(template.getColumns().get(0)).isEqualTo(payLegCcyColumn);
    assertThat(template.getColumns().get(1)).isEqualTo(pvColumn);
  }

  private TradeReportTemplate parseIni(String resourceName) {
    ResourceLocator locator = ResourceLocator.of("classpath:" + resourceName);
    IniFile ini = IniFile.of(locator.getCharSource());
    TradeReportTemplateIniLoader loader = new TradeReportTemplateIniLoader();
    return loader.load(ini);
  }

}
