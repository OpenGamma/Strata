/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.runner.CalculationParameters;

/**
 * Test {@link Column}.
 */
@Test
public class ColumnTest {

  private static final MarketDataRules MD_RULES = MarketDataRules.empty();

  //-------------------------------------------------------------------------
  public void test_builder_columnNameFromMeasure() {
    Column test = Column.builder().measure(Measures.PRESENT_VALUE).marketDataRules(MD_RULES).build();
    assertEquals(test.getName(), ColumnName.of(Measures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getMarketDataRules(), MD_RULES);
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_builder_columnNameSpecified() {
    Column test = Column.builder().measure(Measures.PRESENT_VALUE).name(ColumnName.of("NPV")).build();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getMarketDataRules(), MarketDataRules.empty());
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_builder_missingData() {
    assertThrowsIllegalArg(() -> Column.builder().build());
  }

  //-------------------------------------------------------------------------
  public void test_of_Measure() {
    Column test = Column.of(Measures.PRESENT_VALUE);
    assertEquals(test.getName(), ColumnName.of(Measures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getMarketDataRules(), MarketDataRules.empty());
    assertEquals(test.getParameters(), CalculationParameters.empty());
    assertEquals(test.getReportingCurrency(), ReportingCurrency.NATURAL);
  }

  public void test_of_MeasureCurrency() {
    Column test = Column.of(Measures.PRESENT_VALUE, USD);
    assertEquals(test.getName(), ColumnName.of(Measures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getMarketDataRules(), MarketDataRules.empty());
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
    assertEquals(test.getReportingCurrency(), ReportingCurrency.of(USD));
  }

  public void test_of_MeasureCalculationParameters() {
    Column test = Column.of(Measures.PRESENT_VALUE, ReportingCurrency.of(USD));
    assertEquals(test.getName(), ColumnName.of(Measures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getMarketDataRules(), MarketDataRules.empty());
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
    assertEquals(test.getReportingCurrency(), ReportingCurrency.of(USD));
  }

  public void test_of_MeasureString() {
    Column test = Column.of(Measures.PRESENT_VALUE, "NPV");
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getMarketDataRules(), MarketDataRules.empty());
    assertEquals(test.getParameters(), CalculationParameters.empty());
    assertEquals(test.getReportingCurrency(), ReportingCurrency.NATURAL);
  }

  public void test_of_MeasureStringCurrency() {
    Column test = Column.of(Measures.PRESENT_VALUE, "NPV", USD);
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getMarketDataRules(), MarketDataRules.empty());
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
    assertEquals(test.getReportingCurrency(), ReportingCurrency.of(USD));
  }

  public void test_of_MeasureStringCalculationParameters() {
    Column test = Column.of(Measures.PRESENT_VALUE, "NPV", ReportingCurrency.of(USD));
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getMarketDataRules(), MarketDataRules.empty());
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.of(USD)));
    assertEquals(test.getReportingCurrency(), ReportingCurrency.of(USD));
  }

  //-------------------------------------------------------------------------
  public void test_toHeader_withCurrency() {
    ColumnHeader test = Column.of(Measures.PRESENT_VALUE, "NPV", USD).toHeader();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getCurrency(), Optional.of(USD));
  }

  public void test_toHeader_withoutCurrency() {
    ColumnHeader test = Column.of(Measures.PRESENT_VALUE, "NPV").toHeader();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getCurrency(), Optional.empty());
  }

  public void test_toHeader_withNonConvertibleMeasure() {
    ColumnHeader test = Column.of(Measures.PAR_RATE, "NPV", USD).toHeader();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PAR_RATE);
    assertEquals(test.getCurrency(), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Column test = Column.of(Measures.PRESENT_VALUE, "NPV", ReportingCurrency.NATURAL);
    coverImmutableBean(test);
    Column test2 = Column.of(Measures.CASH_FLOWS);
    coverBeanEquals(test, test2);
  }

}
