/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link SurfaceCurrencyParameterSensitivity}.
 */
@Test
public class SurfaceCurrencyParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR_USD1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR_USD_FACTOR =
      DoubleArray.of(100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1);
  private static final DoubleArray VECTOR_EUR1 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final DoubleArray VECTOR_EUR1_IN_USD =
      DoubleArray.of(1000 * 1.5, 250 * 1.5, 321 * 1.5, 123 * 1.5, 321 * 1.5);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.5d);
  private static final SurfaceName NAME1 = SurfaceName.of("NAME-1");
  private static final SurfaceMetadata METADATA1 = DefaultSurfaceMetadata.of(NAME1);
  private static final SurfaceName NAME2 = SurfaceName.of("NAME-2");
  private static final SurfaceMetadata METADATA2 = DefaultSurfaceMetadata.of(NAME2);

  //-------------------------------------------------------------------------
  public void test_of_metadata() {
    SurfaceCurrencyParameterSensitivity test = SurfaceCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD1);
    assertThat(test.getMetadata()).isEqualTo(METADATA1);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getSurfaceName()).isEqualTo(NAME1);
    assertThat(test.getParameterCount()).isEqualTo(VECTOR_USD1.size());
    assertThat(test.getSensitivity()).isEqualTo(VECTOR_USD1);
  }

  public void test_of_metadata_badMetadata() {
    DefaultSurfaceMetadata metadata = DefaultSurfaceMetadata.builder()
        .surfaceName(NAME1)
        .parameterMetadata(SurfaceParameterMetadata.listOfEmpty(VECTOR_USD1.size() + 1))
        .build();
    assertThrowsIllegalArg(() -> SurfaceCurrencyParameterSensitivity.of(metadata, USD, VECTOR_USD1));
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    SurfaceCurrencyParameterSensitivity base = SurfaceCurrencyParameterSensitivity.of(METADATA1, EUR, VECTOR_EUR1);
    SurfaceCurrencyParameterSensitivity test = base.convertedTo(USD, FX_RATE);
    assertThat(test).isEqualTo(SurfaceCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_EUR1_IN_USD));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    SurfaceCurrencyParameterSensitivity base = SurfaceCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD1);
    SurfaceCurrencyParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertThat(test).isEqualTo(SurfaceCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD_FACTOR));
  }

  //-------------------------------------------------------------------------
  public void test_withSensitivity() {
    SurfaceCurrencyParameterSensitivity base = SurfaceCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD1);
    SurfaceCurrencyParameterSensitivity test = base.withSensitivity(VECTOR_USD_FACTOR);
    assertThat(test).isEqualTo(SurfaceCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD_FACTOR));
    assertThrowsIllegalArg(() -> base.withSensitivity(DoubleArray.of(1d)));
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    SurfaceCurrencyParameterSensitivity base = SurfaceCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD1);
    CurrencyAmount test = base.total();
    assertThat(test.getCurrency()).isEqualTo(USD);
    Object expected = VECTOR_USD1.get(0) + VECTOR_USD1.get(1) + VECTOR_USD1.get(2) + VECTOR_USD1.get(3);
    assertThat(test.getAmount()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SurfaceCurrencyParameterSensitivity test1 = SurfaceCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD1);
    coverImmutableBean(test1);
    SurfaceCurrencyParameterSensitivity test2 = SurfaceCurrencyParameterSensitivity.of(METADATA2, EUR, VECTOR_EUR1);
    coverBeanEquals(test1, test2);
  }

}
