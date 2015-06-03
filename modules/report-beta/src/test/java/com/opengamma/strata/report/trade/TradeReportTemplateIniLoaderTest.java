/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.engine.config.Measure;

/**
 * Test {@link TradeReportTemplateIniLoader}.
 */
@Test
public class TradeReportTemplateIniLoaderTest {

  public void test_simple_values() {
    TradeReportTemplate template = parseIni("trade-report-test-simple.ini");
    
    TradeReportColumn maturityDateColumn = TradeReportColumn.builder()
        .measure(Measure.MATURITY_DATE)
        .header("Maturity Date")
        .build();
    
    TradeReportColumn pvColumn = TradeReportColumn.builder()
        .measure(Measure.PRESENT_VALUE)
        .header("Present Value")
        .build();
    
    assertEquals(template.getColumns().size(), 2);
    assertEquals(template.getColumns().get(0), maturityDateColumn);
    assertEquals(template.getColumns().get(1), pvColumn);
  }
  
  public void test_path() {
    TradeReportTemplate template = parseIni("trade-report-test-path.ini");
    
    TradeReportColumn payLegCcyColumn = TradeReportColumn.builder()
        .measure(Measure.LEG_INITIAL_NOTIONAL)
        .path("pay", "currency")
        .header("Pay Leg Ccy")
        .build();
    
    assertEquals(template.getColumns().size(), 1);
    assertEquals(template.getColumns().get(0), payLegCcyColumn);
  }
  
  private TradeReportTemplate parseIni(String resourceName) {
    ResourceLocator locator = ResourceLocator.of("classpath:" + resourceName);
    IniFile ini = IniFile.of(locator.getCharSource());
    TradeReportTemplateIniLoader loader = new TradeReportTemplateIniLoader();
    return loader.load(ini);
  }
  
}
