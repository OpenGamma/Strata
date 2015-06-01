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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurveUnitParameterSensitivities}.
 */
@Test
public class CurveUnitParameterSensitivitiesTest {

  private static final double FACTOR1 = 3.14;
  private static final double[] VECTOR1 = new double[] {100, 200, 300, 123};
  private static final double[] VECTOR2 = new double[] {1000, 250, 321, 123};
  private static final double[] VECTOR_ZERO = new double[] {0, 0, 0, 0};
  private static final double[] TOTAL_USD = new double[] {1100, 450, 621, 246};
  private static final double[] VECTOR3 = new double[] {1000, 250, 321, 123, 321};
  private static final CurveName NAME0 = CurveName.of("NAME-0");
  private static final CurveName NAME1 = CurveName.of("NAME-1");
  private static final CurveName NAME2 = CurveName.of("NAME-2");
  private static final CurveName NAME3 = CurveName.of("NAME-3");

  private static final CurveUnitParameterSensitivity ENTRY_USD =
      CurveUnitParameterSensitivity.of(NAME1, VECTOR1);
  private static final CurveUnitParameterSensitivity ENTRY_USD2 =
      CurveUnitParameterSensitivity.of(NAME1, VECTOR2);
  private static final CurveUnitParameterSensitivity ENTRY_USD_TOTAL =
      CurveUnitParameterSensitivity.of(NAME1, TOTAL_USD);
  private static final CurveUnitParameterSensitivity ENTRY_USD_SMALL =
      CurveUnitParameterSensitivity.of(NAME1, new double[] {100d});
  private static final CurveUnitParameterSensitivity ENTRY_EUR =
      CurveUnitParameterSensitivity.of(NAME2, VECTOR3);
  private static final CurveUnitParameterSensitivity ENTRY_ZERO0 =
      CurveUnitParameterSensitivity.of(NAME0, VECTOR_ZERO);
  private static final CurveUnitParameterSensitivity ENTRY_ZERO3 =
      CurveUnitParameterSensitivity.of(NAME3, VECTOR_ZERO);

  private static final CurveUnitParameterSensitivities SENSI_1 = CurveUnitParameterSensitivities.of(ENTRY_USD);
  private static final CurveUnitParameterSensitivities SENSI_2 =
      CurveUnitParameterSensitivities.of(ImmutableList.of(ENTRY_USD2, ENTRY_EUR));

  private static final double TOLERENCE_CMP = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_empty() {
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.empty();
    assertThat(test.size()).isEqualTo(0);
    assertThat(test.getSensitivities()).hasSize(0);
  }

  public void test_of_single() {
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(ENTRY_USD);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD);
  }

  public void test_of_list_none() {
    ImmutableList<CurveUnitParameterSensitivity> list = ImmutableList.of();
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(0);
  }

  public void test_of_list_notNormalized() {
    ImmutableList<CurveUnitParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_EUR);
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD, ENTRY_EUR);
  }

  public void test_of_list_normalized() {
    ImmutableList<CurveUnitParameterSensitivity> list = ImmutableList.of(ENTRY_USD, ENTRY_USD2);
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD_TOTAL);
  }

  //-------------------------------------------------------------------------
  public void test_getSensitivity() {
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(ENTRY_USD);
    assertThat(test.getSensitivity(NAME1)).isEqualTo(ENTRY_USD);
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME0));
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_one_notNormalized() {
    CurveUnitParameterSensitivities test = SENSI_1.combinedWith(ENTRY_EUR);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD, ENTRY_EUR);
  }

  public void test_combinedWith_one_normalized() {
    CurveUnitParameterSensitivities test = SENSI_1.combinedWith(ENTRY_USD2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD_TOTAL);
  }

  public void test_combinedWith_one_sizeMismatch() {
    assertThrowsIllegalArg(() -> SENSI_1.combinedWith(ENTRY_USD_SMALL));
  }

  public void test_combinedWith_other() {
    CurveUnitParameterSensitivities test = SENSI_1.combinedWith(SENSI_2);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD_TOTAL, ENTRY_EUR);
  }

  public void test_combinedWith_otherEmpty() {
    CurveUnitParameterSensitivities test = SENSI_1.combinedWith(CurveUnitParameterSensitivities.empty());
    assertThat(test).isEqualTo(SENSI_1);
  }

  public void test_combinedWith_empty() {
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.empty().combinedWith(SENSI_1);
    assertThat(test).isEqualTo(SENSI_1);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CurveUnitParameterSensitivities multiplied = SENSI_1.multipliedBy(FACTOR1);
    double[] test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR1.length; i++) {
      assertThat(test[i]).isEqualTo(VECTOR1[i] * FACTOR1);
    }
  }

  public void test_mapSensitivities() {
    CurveUnitParameterSensitivities multiplied = SENSI_1.mapSensitivities(a -> 1 / a);
    double[] test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR1.length; i++) {
      assertThat(test[i]).isEqualTo(1 / VECTOR1[i]);
    }
  }

  public void test_multipliedBy_vs_combinedWith() {
    CurveUnitParameterSensitivities multiplied = SENSI_2.multipliedBy(2d);
    CurveUnitParameterSensitivities added = SENSI_2.combinedWith(SENSI_2);
    assertThat(multiplied).isEqualTo(added);
  }

  //-------------------------------------------------------------------------
  public void test_equalWithTolerance() {
    CurveUnitParameterSensitivities sensUsdTotal = CurveUnitParameterSensitivities.of(ENTRY_USD_TOTAL);
    CurveUnitParameterSensitivities sensEur = CurveUnitParameterSensitivities.of(ENTRY_EUR);
    CurveUnitParameterSensitivities sens1plus2 = SENSI_1.combinedWith(ENTRY_USD2);
    CurveUnitParameterSensitivities sensZeroA = CurveUnitParameterSensitivities.of(ENTRY_ZERO3);
    CurveUnitParameterSensitivities sensZeroB = CurveUnitParameterSensitivities.of(ENTRY_ZERO0);
    CurveUnitParameterSensitivities sens1plus2plus0a = SENSI_1.combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO0);
    CurveUnitParameterSensitivities sens1plus2plus0b = SENSI_1.combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO3);
    CurveUnitParameterSensitivities sens1plus2plus0 = SENSI_1
        .combinedWith(ENTRY_USD2).combinedWith(ENTRY_ZERO0).combinedWith(ENTRY_ZERO3);
    CurveUnitParameterSensitivities sens2plus0 = SENSI_2.combinedWith(sensZeroA);
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

    assertThat(sensZeroA.equalWithTolerance(CurveUnitParameterSensitivities.empty(), TOLERENCE_CMP)).isTrue();
    assertThat(CurveUnitParameterSensitivities.empty().equalWithTolerance(sensZeroA, TOLERENCE_CMP)).isTrue();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(CurveUnitParameterSensitivities.empty());
    coverImmutableBean(SENSI_1);
    coverBeanEquals(SENSI_1, SENSI_2);
  }

}
