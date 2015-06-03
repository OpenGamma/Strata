/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;

/**
 * Test {@link CurveUnitParameterSensitivity}.
 */
@Test
public class CurveUnitParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final double[] VECTOR1 = new double[] {100, 200, 300, 123};
  private static final double[] VECTOR1_FACTOR = new double[] {100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1};
  private static final double[] VECTOR2 = new double[] {1000, 250, 321, 123, 321};
  private static final CurveName NAME1 = CurveName.of("NAME-1");
  private static final CurveName NAME2 = CurveName.of("NAME-2");

  //-------------------------------------------------------------------------
  public void test_of_stringName() {
    CurveUnitParameterSensitivity test = CurveUnitParameterSensitivity.of("Name", VECTOR1);
    assertThat(test.getMetadata()).isEqualTo(CurveMetadata.of("Name"));
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getParameterCount()).isEqualTo(VECTOR1.length);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR1);
  }

  public void test_of_curveName() {
    CurveUnitParameterSensitivity test = CurveUnitParameterSensitivity.of(CurveName.of("Name"), VECTOR1);
    assertThat(test.getMetadata()).isEqualTo(CurveMetadata.of("Name"));
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getParameterCount()).isEqualTo(VECTOR1.length);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR1);
  }

  public void test_of_metadata() {
    CurveMetadata metadata = CurveMetadata.of("Name", CurveParameterMetadata.listOfEmpty(VECTOR1.length));
    CurveUnitParameterSensitivity test = CurveUnitParameterSensitivity.of(metadata, VECTOR1);
    assertThat(test.getMetadata()).isEqualTo(metadata);
    assertThat(test.getCurveName()).isEqualTo(CurveName.of("Name"));
    assertThat(test.getParameterCount()).isEqualTo(VECTOR1.length);
    assertThat(test.getSensitivity()).isEqualTo(VECTOR1);
  }

  public void test_of_metadata_badMetadata() {
    assertThrowsIllegalArg(() -> CurveUnitParameterSensitivity.of(
        CurveMetadata.of("Name", CurveParameterMetadata.listOfEmpty(VECTOR1.length + 1)), VECTOR1));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy_currency() {
    CurveUnitParameterSensitivity base = CurveUnitParameterSensitivity.of("Name", VECTOR1);
    CurveCurrencyParameterSensitivity test = base.multipliedBy(USD, FACTOR1);
    assertThat(test).isEqualTo(CurveCurrencyParameterSensitivity.of("Name", USD, VECTOR1_FACTOR));
  }

  public void test_multipliedBy() {
    CurveUnitParameterSensitivity base = CurveUnitParameterSensitivity.of("Name", VECTOR1);
    CurveUnitParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertThat(test).isEqualTo(CurveUnitParameterSensitivity.of("Name", VECTOR1_FACTOR));
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    CurveUnitParameterSensitivity base = CurveUnitParameterSensitivity.of("Name", VECTOR1);
    CurveUnitParameterSensitivity test = base.withSensitivity(VECTOR1_FACTOR);
    assertThat(test).isEqualTo(CurveUnitParameterSensitivity.of("Name", VECTOR1_FACTOR));
    assertThrowsIllegalArg(() -> base.withSensitivity(new double[] {1d}));
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    CurveUnitParameterSensitivity base = CurveUnitParameterSensitivity.of("Name", VECTOR1);
    double test = base.total();
    assertThat(test).isEqualTo(VECTOR1[0] + VECTOR1[1] + VECTOR1[2] + VECTOR1[3]);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveUnitParameterSensitivity test = CurveUnitParameterSensitivity.of(NAME1, VECTOR1);
    coverImmutableBean(test);
    CurveUnitParameterSensitivity test2 = CurveUnitParameterSensitivity.of(NAME2, VECTOR2);
    coverBeanEquals(test, test2);
  }

}
