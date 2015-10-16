/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveParameterMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;

/**
 * Test {@link CurveUnitParameterSensitivity}.
 */
@Test
public class CurveUnitParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR1_FACTOR =
      DoubleArray.of(100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1);
  private static final DoubleArray VECTOR2 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final CurveName NAME1 = CurveName.of("NAME-1");
  private static final CurveMetadata METADATA1 = DefaultCurveMetadata.of(NAME1);
  private static final CurveName NAME2 = CurveName.of("NAME-2");
  private static final CurveMetadata METADATA2 = DefaultCurveMetadata.of(NAME2);

  //-------------------------------------------------------------------------
  public void test_of_metadata() {
    CurveUnitParameterSensitivity test = CurveUnitParameterSensitivity.of(METADATA1, VECTOR1);
    assertThat(test.getMetadata()).isEqualTo(METADATA1);
    assertThat(test.getCurveName()).isEqualTo(NAME1);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR1.size());
    assertThat(test.getSensitivity()).isEqualTo(VECTOR1);
  }

  public void test_of_metadata_badMetadata() {
    CurveMetadata metadata = Curves.zeroRates(
        CurveName.of("Name"), ACT_365F, CurveParameterMetadata.listOfEmpty(VECTOR1.size() + 1));
    assertThrowsIllegalArg(() -> CurveUnitParameterSensitivity.of(metadata, VECTOR1));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy_currency() {
    CurveUnitParameterSensitivity base = CurveUnitParameterSensitivity.of(METADATA1, VECTOR1);
    CurveCurrencyParameterSensitivity test = base.multipliedBy(USD, FACTOR1);
    assertThat(test).isEqualTo(CurveCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR1_FACTOR));
  }

  public void test_multipliedBy() {
    CurveUnitParameterSensitivity base = CurveUnitParameterSensitivity.of(METADATA1, VECTOR1);
    CurveUnitParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertThat(test).isEqualTo(CurveUnitParameterSensitivity.of(METADATA1, VECTOR1_FACTOR));
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    CurveUnitParameterSensitivity base = CurveUnitParameterSensitivity.of(METADATA1, VECTOR1);
    CurveUnitParameterSensitivity test = base.withSensitivity(VECTOR1_FACTOR);
    assertThat(test).isEqualTo(CurveUnitParameterSensitivity.of(METADATA1, VECTOR1_FACTOR));
    assertThrowsIllegalArg(() -> base.withSensitivity(DoubleArray.of(1d)));
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    CurveUnitParameterSensitivity base = CurveUnitParameterSensitivity.of(METADATA1, VECTOR1);
    double test = base.total();
    assertThat(test).isEqualTo(VECTOR1.get(0) + VECTOR1.get(1) + VECTOR1.get(2) + VECTOR1.get(3));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveUnitParameterSensitivity test = CurveUnitParameterSensitivity.of(METADATA1, VECTOR1);
    coverImmutableBean(test);
    CurveUnitParameterSensitivity test2 = CurveUnitParameterSensitivity.of(METADATA2, VECTOR2);
    coverBeanEquals(test, test2);
  }

}
