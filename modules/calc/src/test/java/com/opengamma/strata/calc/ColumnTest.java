/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.TestParameter;

/**
 * Test {@link Column}.
 */
public class ColumnTest {

  private static final CalculationParameter PARAM = new TestParameter();

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_columnNameFromMeasure() {
    Column test = Column.builder().measure(TestingMeasures.PRESENT_VALUE).build();
    assertThat(test.getName()).isEqualTo(ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.empty());
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.empty());
  }

  @Test
  public void test_builder_columnNameSpecified() {
    Column test = Column.builder()
        .measure(TestingMeasures.PRESENT_VALUE)
        .name(ColumnName.of("NPV"))
        .reportingCurrency(ReportingCurrency.NATURAL)
        .build();
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.of(ReportingCurrency.NATURAL));
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.empty());
  }

  @Test
  public void test_builder_missingData() {
    assertThatIllegalArgumentException().isThrownBy(() -> Column.builder().build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_Measure() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getName()).isEqualTo(ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.empty());
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.empty());
  }

  @Test
  public void test_of_MeasureCurrency() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, USD);
    assertThat(test.getName()).isEqualTo(ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.of(ReportingCurrency.of(USD)));
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.empty());
  }

  @Test
  public void test_of_MeasureCalculationParameters() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, PARAM);
    assertThat(test.getName()).isEqualTo(ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.empty());
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.of(PARAM));
  }

  @Test
  public void test_of_MeasureCurrencyCalculationParameters() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, USD, PARAM);
    assertThat(test.getName()).isEqualTo(ColumnName.of(TestingMeasures.PRESENT_VALUE.getName()));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.of(ReportingCurrency.of(USD)));
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.of(PARAM));
  }

  @Test
  public void test_of_MeasureString() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV");
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.empty());
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.empty());
  }

  @Test
  public void test_of_MeasureStringCurrency() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", USD);
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.of(ReportingCurrency.of(USD)));
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.empty());
  }

  @Test
  public void test_of_MeasureStringCalculationParameters() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", PARAM);
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.empty());
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.of(PARAM));
  }

  @Test
  public void test_of_MeasureStringCurrencyCalculationParameters() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", USD, PARAM);
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(Optional.of(ReportingCurrency.of(USD)));
    assertThat(test.getParameters()).isEqualTo(CalculationParameters.of(PARAM));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toHeader_withCurrency() {
    ColumnHeader test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", USD).toHeader();
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getCurrency()).isEqualTo(Optional.of(USD));
  }

  @Test
  public void test_toHeader_withoutCurrency() {
    ColumnHeader test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV").toHeader();
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getCurrency()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_toHeader_withNonConvertibleMeasure() {
    ColumnHeader test = Column.of(TestingMeasures.PAR_RATE, "NPV", USD).toHeader();
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PAR_RATE);
    assertThat(test.getCurrency()).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Column test = Column.of(TestingMeasures.PRESENT_VALUE, "NPV", USD, PARAM);
    coverImmutableBean(test);
    Column test2 = Column.of(TestingMeasures.CASH_FLOWS);
    coverBeanEquals(test, test2);
  }

}
