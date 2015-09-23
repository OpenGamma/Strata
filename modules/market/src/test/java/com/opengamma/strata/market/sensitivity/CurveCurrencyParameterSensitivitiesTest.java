/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.basics.BasicProjectAssertions.assertThat;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.DoubleArrayMath.sum;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;

/**
 * Test {@link CurveCurrencyParameterSensitivities}.
 */
@Test
public class CurveCurrencyParameterSensitivitiesTest {

  private static final double FACTOR1 = 3.14;
  private static final double[] VECTOR_USD1 = new double[] {100, 200, 300, 123};
  private static final double[] VECTOR_USD2 = new double[] {1000, 250, 321, 123};
  private static final double[] VECTOR_USD2_IN_EUR = new double[] {1000 / 1.6, 250 / 1.6, 321 / 1.6, 123 / 1.6};
  private static final double[] VECTOR_ZERO = new double[] {0, 0, 0, 0};
  private static final double[] TOTAL_USD = new double[] {1100, 450, 621, 246};
  private static final double[] VECTOR_EUR1 = new double[] {1000, 250, 321, 123, 321};
  private static final double[] VECTOR_EUR1_IN_USD = new double[] {1000 * 1.6, 250 * 1.6, 321 * 1.6, 123 * 1.6, 321 * 1.6};
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, 1.6d);
  private static final CurveName NAME0 = CurveName.of("NAME-0");
  private static final CurveMetadata METADATA0 = DefaultCurveMetadata.of(NAME0);
  private static final CurveName NAME1 = CurveName.of("NAME-1");
  private static final CurveMetadata METADATA1 = DefaultCurveMetadata.of(NAME1);
  private static final CurveName NAME2 = CurveName.of("NAME-2");
  private static final CurveMetadata METADATA2 = DefaultCurveMetadata.of(NAME2);
  private static final CurveName NAME3 = CurveName.of("NAME-3");
  private static final CurveMetadata METADATA3 = DefaultCurveMetadata.of(NAME3);

  private static final CurveCurrencyParameterSensitivity ENTRY_USD =
      CurveCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD1);
  private static final CurveCurrencyParameterSensitivity ENTRY_USD2 =
      CurveCurrencyParameterSensitivity.of(METADATA1, USD, VECTOR_USD2);
  private static final CurveCurrencyParameterSensitivity ENTRY_USD_TOTAL =
      CurveCurrencyParameterSensitivity.of(METADATA1, USD, TOTAL_USD);
  private static final CurveCurrencyParameterSensitivity ENTRY_USD_SMALL =
      CurveCurrencyParameterSensitivity.of(METADATA1, USD, new double[] {100d});
  private static final CurveCurrencyParameterSensitivity ENTRY_USD2_IN_EUR =
      CurveCurrencyParameterSensitivity.of(METADATA1, EUR, VECTOR_USD2_IN_EUR);
  private static final CurveCurrencyParameterSensitivity ENTRY_EUR =
      CurveCurrencyParameterSensitivity.of(METADATA2, EUR, VECTOR_EUR1);
  private static final CurveCurrencyParameterSensitivity ENTRY_EUR_IN_USD =
      CurveCurrencyParameterSensitivity.of(METADATA2, USD, VECTOR_EUR1_IN_USD);
  private static final CurveCurrencyParameterSensitivity ENTRY_ZERO0 =
      CurveCurrencyParameterSensitivity.of(METADATA0, USD, VECTOR_ZERO);
  private static final CurveCurrencyParameterSensitivity ENTRY_ZERO3 =
      CurveCurrencyParameterSensitivity.of(METADATA3, USD, VECTOR_ZERO);

  private static final CurveCurrencyParameterSensitivities SENSI_1 = CurveCurrencyParameterSensitivities.of(ENTRY_USD);
  private static final CurveCurrencyParameterSensitivities SENSI_2 =
      CurveCurrencyParameterSensitivities.of(ImmutableList.of(ENTRY_USD2, ENTRY_EUR));

  private static final double TOLERENCE_CMP = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_empty() {
    CurveCurrencyParameterSensitivities test = CurveCurrencyParameterSensitivities.empty();
    assertThat(test.size()).isEqualTo(0);
    assertThat(test.getSensitivities()).hasSize(0);
  }

  public void test_of_single() {
    CurveCurrencyParameterSensitivities test = CurveCurrencyParameterSensitivities.of(ENTRY_USD);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD);
  }

  public void test_of_list_none() {
    ImmutableList<CurveCurrencyParameterSensitivity> list = ImmutableList.of();
    CurveCurrencyParameterSensitivities test = CurveCurrencyParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(0);
  }

  public void test_of_list_notNormalized() {
    ImmutableList<CurveCurrencyParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_EUR);
    CurveCurrencyParameterSensitivities test = CurveCurrencyParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD, ENTRY_EUR);
  }

  public void test_of_list_normalized() {
    ImmutableList<CurveCurrencyParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_USD2);
    CurveCurrencyParameterSensitivities test = CurveCurrencyParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD_TOTAL);
  }

  //-------------------------------------------------------------------------
  public void test_getSensitivity() {
    CurveCurrencyParameterSensitivities test = CurveCurrencyParameterSensitivities.of(ENTRY_USD);
    assertThat(test.getSensitivity(NAME1, USD)).isEqualTo(ENTRY_USD);
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME1, EUR));
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME0, USD));
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME0, EUR));
  }

  public void test_findSensitivity() {
    CurveCurrencyParameterSensitivities test = CurveCurrencyParameterSensitivities.of(ENTRY_USD);
    assertThat(test.findSensitivity(NAME1, USD)).hasValue(ENTRY_USD);
    assertThat(test.findSensitivity(NAME1, EUR)).isEmpty();
    assertThat(test.findSensitivity(NAME0, USD)).isEmpty();
    assertThat(test.findSensitivity(NAME0, EUR)).isEmpty();
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_one_notNormalized() {
    CurveCurrencyParameterSensitivities test = SENSI_1.combinedWith(ENTRY_EUR);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD, ENTRY_EUR);
  }

  public void test_combinedWith_one_normalized() {
    CurveCurrencyParameterSensitivities test = SENSI_1.combinedWith(ENTRY_USD2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD_TOTAL);
  }

  public void test_combinedWith_one_sizeMismatch() {
    assertThrowsIllegalArg(() -> SENSI_1.combinedWith(ENTRY_USD_SMALL));
  }

  public void test_combinedWith_other() {
    CurveCurrencyParameterSensitivities test = SENSI_1.combinedWith(SENSI_2);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD_TOTAL, ENTRY_EUR);
  }

  public void test_combinedWith_otherEmpty() {
    CurveCurrencyParameterSensitivities test = SENSI_1.combinedWith(CurveCurrencyParameterSensitivities.empty());
    assertThat(test).isEqualTo(SENSI_1);
  }

  public void test_combinedWith_empty() {
    CurveCurrencyParameterSensitivities test = CurveCurrencyParameterSensitivities.empty().combinedWith(SENSI_1);
    assertThat(test).isEqualTo(SENSI_1);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo_singleCurrency() {
    CurveCurrencyParameterSensitivities test = SENSI_1.convertedTo(USD, FxMatrix.empty());
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD);
  }

  public void test_convertedTo_multipleCurrency() {
    CurveCurrencyParameterSensitivities test = SENSI_2.convertedTo(USD, FX_RATE);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD2, ENTRY_EUR_IN_USD);
  }

  public void test_convertedTo_multipleCurrency_mergeWhenSameCurveName() {
    CurveCurrencyParameterSensitivities test = SENSI_1.combinedWith(ENTRY_USD2_IN_EUR).convertedTo(USD, FX_RATE);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD_TOTAL);
  }

  //-------------------------------------------------------------------------
  public void test_total_singleCurrency() {
    assertThat(SENSI_1.total(USD, FxMatrix.empty()))
        .hasAmount(sum(VECTOR_USD1), within(1E-8));
  }

  public void test_total_multipleCurrency() {
    assertThat(SENSI_2.total(USD, FX_RATE))
        .hasAmount(sum(VECTOR_USD2) + sum(VECTOR_EUR1) * 1.6d, within(1E-8));
  }

  public void test_totalMulti_singleCurrency() {
    assertThat(SENSI_1.total().size()).isEqualTo(1);
    assertThat(SENSI_1.total().getAmount(USD).getAmount()).isCloseTo(sum(VECTOR_USD1), within(1E-8));
  }

  public void test_totalMulti_multipleCurrency() {
    assertThat(SENSI_2.total().size()).isEqualTo(2);
    assertThat(SENSI_2.total().getAmount(USD).getAmount()).isCloseTo(sum(VECTOR_USD2), within(1E-8));
    assertThat(SENSI_2.total().getAmount(EUR).getAmount()).isCloseTo(sum(VECTOR_EUR1), within(1E-8));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CurveCurrencyParameterSensitivities multiplied = SENSI_1.multipliedBy(FACTOR1);
    double[] test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR_USD1.length; i++) {
      assertThat(test[i]).isEqualTo(VECTOR_USD1[i] * FACTOR1);
    }
  }

  public void test_mapSensitivities() {
    CurveCurrencyParameterSensitivities multiplied = SENSI_1.mapSensitivities(a -> 1 / a);
    double[] test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR_USD1.length; i++) {
      assertThat(test[i]).isEqualTo(1 / VECTOR_USD1[i]);
    }
  }

  public void test_multipliedBy_vs_combinedWith() {
    CurveCurrencyParameterSensitivities multiplied = SENSI_2.multipliedBy(2d);
    CurveCurrencyParameterSensitivities added = SENSI_2.combinedWith(SENSI_2);
    assertThat(multiplied).isEqualTo(added);
  }

  //-------------------------------------------------------------------------
  public void test_equalWithTolerance() {
    CurveCurrencyParameterSensitivities sensUsdTotal = CurveCurrencyParameterSensitivities.of(ENTRY_USD_TOTAL);
    CurveCurrencyParameterSensitivities sensEur = CurveCurrencyParameterSensitivities.of(ENTRY_EUR);
    CurveCurrencyParameterSensitivities sens1plus2 = SENSI_1.combinedWith(ENTRY_USD2);
    CurveCurrencyParameterSensitivities sensZeroA = CurveCurrencyParameterSensitivities.of(ENTRY_ZERO3);
    CurveCurrencyParameterSensitivities sensZeroB = CurveCurrencyParameterSensitivities.of(ENTRY_ZERO0);
    CurveCurrencyParameterSensitivities sens1plus2plus0a = SENSI_1.combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO0);
    CurveCurrencyParameterSensitivities sens1plus2plus0b = SENSI_1.combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO3);
    CurveCurrencyParameterSensitivities sens1plus2plus0 = SENSI_1
        .combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO0).combinedWith(ENTRY_ZERO3);
    CurveCurrencyParameterSensitivities sens2plus0 = SENSI_2.combinedWith(sensZeroA);
    assertThat(SENSI_1.equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isTrue();
    assertThat(SENSI_1.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(sensEur, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isFalse();

    assertThat(SENSI_2.equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isTrue();
    assertThat(SENSI_2.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(sensEur, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isTrue();

    assertThat(sensZeroA.equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isTrue();
    assertThat(sensZeroA.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sensEur, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroA.equalWithTolerance(sensZeroB, TOLERENCE_CMP)).isTrue();

    assertThat(sensZeroB.equalWithTolerance(sensZeroB, TOLERENCE_CMP)).isTrue();
    assertThat(sensZeroB.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sensUsdTotal, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sensEur, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isFalse();
    assertThat(sensZeroB.equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isTrue();

    assertThat(sens1plus2.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0a.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0a.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0b.equalWithTolerance(sens1plus2, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0a, TOLERENCE_CMP)).isTrue();
    assertThat(sens1plus2plus0b.equalWithTolerance(sens1plus2plus0b, TOLERENCE_CMP)).isTrue();
    assertThat(sens2plus0.equalWithTolerance(sens2plus0, TOLERENCE_CMP)).isTrue();

    assertThat(sensZeroA.equalWithTolerance(CurveCurrencyParameterSensitivities.empty(), TOLERENCE_CMP)).isTrue();
    assertThat(CurveCurrencyParameterSensitivities.empty().equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isTrue();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(CurveCurrencyParameterSensitivities.empty());
    coverImmutableBean(SENSI_1);
    coverBeanEquals(SENSI_1, SENSI_2);
  }

}
