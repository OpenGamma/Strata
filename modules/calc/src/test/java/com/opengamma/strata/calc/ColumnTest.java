/*
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

import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.TestParameter;

/**
 * Test {@link Column}.
 */
@Test
public class ColumnTest {

  private static final CalculationParameter PARAM = new TestParameter();

  //-------------------------------------------------------------------------
  public void test_builder_columnNameFromMeasure() {
    Column test = Column.builder().measure(TestingMeasures.PRESENT_VALUE).build();
    assertEquals(test.getName(), ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.empty());
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_builder_columnNameSpecified() {
    Column test = Column.builder()
        .measure(TestingMeasures.PRESENT_VALUE)
        .name(ColumnName.of("NPV"))
        .reportingCurrency(ReportingCurrency.NATURAL)
        .build();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.of(ReportingCurrency.NATURAL));
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_builder_missingData() {
    assertThrowsIllegalArg(() -> Column.builder().build());
  }

  //-------------------------------------------------------------------------
  public void test_of_Measure() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getName(), ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.empty());
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_MeasureCurrency() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, USD);
    assertEquals(test.getName(), ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.of(ReportingCurrency.of(USD)));
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_MeasureCalculationParameters() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, PARAM);
    assertEquals(test.getName(), ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.empty());
    assertEquals(test.getParameters(), CalculationParameters.of(PARAM));
  }

  public void test_of_MeasureCurrencyCalculationParameters() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, USD, PARAM);
    assertEquals(test.getName(), ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.of(ReportingCurrency.of(USD)));
    assertEquals(test.getParameters(), CalculationParameters.of(PARAM));
  }

  public void test_of_MeasureString() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV");
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.empty());
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_MeasureStringCurrency() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", USD);
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.of(ReportingCurrency.of(USD)));
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_MeasureStringCalculationParameters() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", PARAM);
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.empty());
    assertEquals(test.getParameters(), CalculationParameters.of(PARAM));
  }

  public void test_of_MeasureStringCurrencyCalculationParameters() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", USD, PARAM);
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), Optional.of(ReportingCurrency.of(USD)));
    assertEquals(test.getParameters(), CalculationParameters.of(PARAM));
  }

  //-------------------------------------------------------------------------
  public void test_toHeader_withCurrency() {
    ColumnHeader test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", USD).toHeader();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getCurrency(), Optional.of(USD));
  }

  public void test_toHeader_withoutCurrency() {
    ColumnHeader test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV").toHeader();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getCurrency(), Optional.empty());
  }

  public void test_toHeader_withNonConvertibleMeasure() {
    ColumnHeader test = Column.of(TestingMeasures.PAR_RATE, "NPV", USD).toHeader();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PAR_RATE);
    assertEquals(test.getCurrency(), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", USD, PARAM);
    coverImmutableBean(test);
    Column test2 = Column.of(TestingMeasures.CASH_FLOWS);
    coverBeanEquals(test, test2);
  }

}
