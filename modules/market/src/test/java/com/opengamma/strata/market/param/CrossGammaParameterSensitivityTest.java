/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CrossGammaParameterSensitivity}.
 */
public class CrossGammaParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleMatrix MATRIX_USD1 = DoubleMatrix.of(2, 2, 100, 200, 300, 123);
  private static final DoubleMatrix MATRIX_USD_FACTOR =
      DoubleMatrix.of(2, 2, 100 * FACTOR1, 200 * FACTOR1, 300 * FACTOR1, 123 * FACTOR1);
  private static final DoubleMatrix MATRIX_EUR1 = DoubleMatrix.of(2, 2, 1000, 250, 321, 123);
  private static final DoubleMatrix MATRIX_EUR1_IN_USD =
      DoubleMatrix.of(2, 2, 1000 * 1.5, 250 * 1.5, 321 * 1.5, 123 * 1.5);
  private static final DoubleMatrix MATRIX_USD_EUR = DoubleMatrix.of(2, 4, 100, 200, 1000, 250, 300, 123, 321, 123);
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.5d);
  private static final MarketDataName<?> NAME1 = CurveName.of("NAME-1");
  private static final MarketDataName<?> NAME2 = CurveName.of("NAME-2");
  private static final List<ParameterMetadata> METADATA_USD1 = ParameterMetadata.listOfEmpty(2);
  private static final List<ParameterMetadata> METADATA_EUR1 = ParameterMetadata.listOfEmpty(2);
  private static final List<ParameterMetadata> METADATA_BAD = ParameterMetadata.listOfEmpty(1);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_metadata() {
    CrossGammaParameterSensitivity test = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    assertThat(test.getMarketDataName()).isEqualTo(NAME1);
    assertThat(test.getParameterCount()).isEqualTo(2);
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_USD1);
    assertThat(test.getParameterMetadata(0)).isEqualTo(METADATA_USD1.get(0));
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getSensitivity()).isEqualTo(MATRIX_USD1);
    assertThat(test.getOrder()).containsExactly(Pair.of(NAME1, METADATA_USD1));
  }

  @Test
  public void test_of_eurUsd() {
    CrossGammaParameterSensitivity test = CrossGammaParameterSensitivity.of(
        NAME1, METADATA_USD1, ImmutableList.of(Pair.of(NAME1, METADATA_USD1), Pair.of(NAME2, METADATA_EUR1)), USD,
        MATRIX_USD_EUR);
    assertThat(test.getMarketDataName()).isEqualTo(NAME1);
    assertThat(test.getParameterCount()).isEqualTo(2);
    assertThat(test.getParameterMetadata()).isEqualTo(METADATA_USD1);
    assertThat(test.getParameterMetadata(0)).isEqualTo(METADATA_USD1.get(0));
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getSensitivity()).isEqualTo(MATRIX_USD_EUR);
    assertThat(test.getOrder()).containsExactly(Pair.of(NAME1, METADATA_USD1), Pair.of(NAME2, METADATA_EUR1));
  }

  @Test
  public void test_of_metadata_badMetadata() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CrossGammaParameterSensitivity.of(NAME1, METADATA_BAD, USD, MATRIX_USD1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_convertedTo() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_EUR1, EUR, MATRIX_EUR1);
    CrossGammaParameterSensitivity test = base.convertedTo(USD, FX_RATE);
    assertThat(base.convertedTo(EUR, FX_RATE)).isEqualTo(base);
    assertThat(test).isEqualTo(CrossGammaParameterSensitivity.of(NAME1, METADATA_EUR1, USD, MATRIX_EUR1_IN_USD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    CrossGammaParameterSensitivity test = base.multipliedBy(FACTOR1);
    assertThat(test).isEqualTo(CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD_FACTOR));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withSensitivity() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    CrossGammaParameterSensitivity test = base.withSensitivity(MATRIX_USD_FACTOR);
    assertThat(test).isEqualTo(CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD_FACTOR));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.withSensitivity(DoubleMatrix.of(1, 1, 1d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_total() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    CurrencyAmount test = base.total();
    assertThat(test.getCurrency()).isEqualTo(USD);
    double expected = MATRIX_USD1.get(0, 0) + MATRIX_USD1.get(0, 1) + MATRIX_USD1.get(1, 0) + MATRIX_USD1.get(1, 1);
    assertThat(test.getAmount()).isEqualTo(expected);
  }

  @Test
  public void test_diagonal() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    CurrencyParameterSensitivity test = base.diagonal();
    DoubleArray value = DoubleArray.of(MATRIX_USD1.get(0, 0), MATRIX_USD1.get(1, 1));
    assertThat(test).isEqualTo(CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, value));
  }

  @Test
  public void test_diagonal_eurUsd() {
    CrossGammaParameterSensitivity base = CrossGammaParameterSensitivity.of(
        NAME1, METADATA_USD1, ImmutableList.of(Pair.of(NAME1, METADATA_USD1), Pair.of(NAME2, METADATA_EUR1)), USD,
        MATRIX_USD_EUR);
    CurrencyParameterSensitivity test = base.diagonal();
    DoubleArray value = DoubleArray.of(MATRIX_USD1.get(0, 0), MATRIX_USD1.get(1, 1));
    assertThat(test).isEqualTo(CurrencyParameterSensitivity.of(NAME1, METADATA_USD1, USD, value));
  }

  @Test
  public void test_getSensitivity_eurUsd() {
    CrossGammaParameterSensitivity test = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1,
        ImmutableList.of(Pair.of(NAME1, METADATA_USD1), Pair.of(NAME2, METADATA_EUR1)), USD, MATRIX_USD_EUR);
    CrossGammaParameterSensitivity expected1 = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    assertThat(test.getSensitivity(NAME1)).isEqualTo(expected1);
    CrossGammaParameterSensitivity expected2 =
        CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, NAME2, METADATA_EUR1, USD, MATRIX_EUR1);
    assertThat(test.getSensitivity(NAME2)).isEqualTo(expected2);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getSensitivity(CurveName.of("NAME-3")));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CrossGammaParameterSensitivity test = CrossGammaParameterSensitivity.of(NAME1, METADATA_USD1, USD, MATRIX_USD1);
    coverImmutableBean(test);
    CrossGammaParameterSensitivity test2 = CrossGammaParameterSensitivity.of(NAME2, METADATA_EUR1, EUR, MATRIX_EUR1);
    coverBeanEquals(test, test2);
  }

}
