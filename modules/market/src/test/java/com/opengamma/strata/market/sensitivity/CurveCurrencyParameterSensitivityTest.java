/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;

/**
 * Test {@link CurveCurrencyParameterSensitivity}.
 */
@Test
public class CurveCurrencyParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final double[] VECTOR_USD1 = new double[] {100, 200, 300, 123};
  private static final double[] VECTOR_USD_FACTOR = new double[] {100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1};
  private static final double[] VECTOR_EUR1 = new double[] {1000, 250, 321, 123, 321};
  private static final double[] VECTOR_EUR1_IN_USD = new double[] {1000 * 1.5, 250 * 1.5, 321 * 1.5, 123 * 1.5, 321 * 1.5};
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.5d);
  private static final CurveName NAME1 = CurveName.of("NAME-1");
  private static final CurveName NAME2 = CurveName.of("NAME-2");

  //-------------------------------------------------------------------------
  public void test_of_stringName() {
    CurveCurrencyParameterSensitivity test = CurveCurrencyParameterSensitivity.of("Name", USD, VECTOR_USD1);
    assertThat(test.getMetadata()).isEqualTo(CurveMetadata.of("Name"));
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD1.length);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD1);
  }

  public void test_of_curveName() {
    CurveCurrencyParameterSensitivity test = CurveCurrencyParameterSensitivity.of(CurveName.of("Name"), USD, VECTOR_USD1);
    assertThat(test.getMetadata()).isEqualTo(CurveMetadata.of("Name"));
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD1.length);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD1);
  }

  public void test_of_metadata() {
    CurveMetadata metadata = CurveMetadata.of("Name", CurveParameterMetadata.listOfEmpty(VECTOR_USD1.length));
    CurveCurrencyParameterSensitivity test = CurveCurrencyParameterSensitivity.of(metadata, USD, VECTOR_USD1);
    assertThat(test.getMetadata()).isEqualTo(metadata);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD1.length);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD1);
  }

  public void test_of_metadata_badMetadata() {
    assertThrowsIllegalArg(() -> CurveCurrencyParameterSensitivity.of(
        CurveMetadata.of("Name", CurveParameterMetadata.listOfEmpty(VECTOR_USD1.length + 1)), USD, VECTOR_USD1));
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    CurveCurrencyParameterSensitivity base = CurveCurrencyParameterSensitivity.of("Name", EUR, VECTOR_EUR1);
    CurveCurrencyParameterSensitivity test = base.convertedTo(USD, FX_RATE);
    assertThat(test).isEqualTo(CurveCurrencyParameterSensitivity.of("Name", USD, VECTOR_EUR1_IN_USD));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CurveCurrencyParameterSensitivity base = CurveCurrencyParameterSensitivity.of("Name", USD, VECTOR_USD1);
    CurveCurrencyParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertThat(test).isEqualTo(CurveCurrencyParameterSensitivity.of("Name", USD, VECTOR_USD_FACTOR));
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    CurveCurrencyParameterSensitivity base = CurveCurrencyParameterSensitivity.of("Name", USD, VECTOR_USD1);
    CurveCurrencyParameterSensitivity test = base.withSensitivity(VECTOR_USD_FACTOR);
    assertThat(test).isEqualTo(CurveCurrencyParameterSensitivity.of("Name", USD, VECTOR_USD_FACTOR));
    assertThrowsIllegalArg(() -> base.withSensitivity(new double[] {1d}));
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    CurveCurrencyParameterSensitivity base = CurveCurrencyParameterSensitivity.of("Name", USD, VECTOR_USD1);
    CurrencyAmount test = base.total();
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getAmount()).isEqualTo(VECTOR_USD1[0] + VECTOR_USD1[1] + VECTOR_USD1[2] + VECTOR_USD1[3]);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveCurrencyParameterSensitivity test = CurveCurrencyParameterSensitivity.of(NAME1, USD, VECTOR_USD1);
    coverImmutableBean(test);
    CurveCurrencyParameterSensitivity test2 = CurveCurrencyParameterSensitivity.of(NAME2, EUR, VECTOR_EUR1);
    coverBeanEquals(test, test2);
  }

}
