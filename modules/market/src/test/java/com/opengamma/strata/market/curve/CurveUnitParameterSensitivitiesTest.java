/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link CurveUnitParameterSensitivities}.
 */
@Test
public class CurveUnitParameterSensitivitiesTest {

  private static final double FACTOR1 = 3.14;
  private static final DoubleArray VECTOR1 = DoubleArray.of(100, 200, 300, 123);
  private static final DoubleArray VECTOR2 = DoubleArray.of(1000, 250, 321, 123);
  private static final DoubleArray VECTOR_ZERO = DoubleArray.of(0, 0, 0, 0);
  private static final DoubleArray TOTAL_USD = DoubleArray.of(1100, 450, 621, 246);
  private static final DoubleArray VECTOR3 = DoubleArray.of(1000, 250, 321, 123, 321);
  private static final CurveName NAME0 = CurveName.of("NAME-0");
  private static final CurveMetadata METADATA0 = DefaultCurveMetadata.of(NAME0);
  private static final CurveName NAME1 = CurveName.of("NAME-1");
  private static final CurveMetadata METADATA1 = DefaultCurveMetadata.of(NAME1);
  private static final CurveName NAME2 = CurveName.of("NAME-2");
  private static final CurveMetadata METADATA2 = DefaultCurveMetadata.of(NAME2);
  private static final CurveName NAME3 = CurveName.of("NAME-3");
  private static final CurveMetadata METADATA3 = DefaultCurveMetadata.of(NAME3);

  private static final CurveUnitParameterSensitivity ENTRY1 =
      CurveUnitParameterSensitivity.of(METADATA1, VECTOR1);
  private static final CurveUnitParameterSensitivity ENTRY2 =
      CurveUnitParameterSensitivity.of(METADATA1, VECTOR2);
  private static final CurveUnitParameterSensitivity ENTRY_TOTAL_1_2 =
      CurveUnitParameterSensitivity.of(METADATA1, TOTAL_USD);
  private static final CurveUnitParameterSensitivity ENTRY_SMALL =
      CurveUnitParameterSensitivity.of(METADATA1, DoubleArray.of(100d));
  private static final CurveUnitParameterSensitivity ENTRY3 =
      CurveUnitParameterSensitivity.of(METADATA2, VECTOR3);
  private static final CurveUnitParameterSensitivity ENTRY_ZERO0 =
      CurveUnitParameterSensitivity.of(METADATA0, VECTOR_ZERO);
  private static final CurveUnitParameterSensitivity ENTRY_ZERO3 =
      CurveUnitParameterSensitivity.of(METADATA3, VECTOR_ZERO);

  private static final CurveUnitParameterSensitivities SENSI_1 = CurveUnitParameterSensitivities.of(ENTRY1);
  private static final CurveUnitParameterSensitivities SENSI_2 =
      CurveUnitParameterSensitivities.of(ImmutableList.of(ENTRY2, ENTRY3));

  private static final double TOLERENCE_CMP = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_empty() {
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.empty();
    assertThat(test.size()).isEqualTo(0);
    assertThat(test.getSensitivities()).hasSize(0);
  }

  public void test_of_single() {
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(ENTRY1);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY1);
  }

  public void test_of_list_none() {
    ImmutableList<CurveUnitParameterSensitivity> list = ImmutableList.of();
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(0);
  }

  public void test_of_list_notNormalized() {
    ImmutableList<CurveUnitParameterSensitivity> list = ImmutableList.of(ENTRY1, ENTRY3);
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY1, ENTRY3);
  }

  public void test_of_list_normalized() {
    ImmutableList<CurveUnitParameterSensitivity> list = ImmutableList.of(ENTRY1, ENTRY2);
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(list);
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_TOTAL_1_2);
  }

  //-------------------------------------------------------------------------
  public void test_getSensitivity() {
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(ENTRY1);
    assertThat(test.getSensitivity(NAME1)).isEqualTo(ENTRY1);
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME0));
  }

  public void test_findSensitivity() {
    CurveUnitParameterSensitivities test = CurveUnitParameterSensitivities.of(ENTRY1);
    assertThat(test.findSensitivity(NAME1)).hasValue(ENTRY1);
    assertThat(test.findSensitivity(NAME0)).isEmpty();
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_one_notNormalized() {
    CurveUnitParameterSensitivities test = SENSI_1.combinedWith(ENTRY3);
    assertThat(test.getSensitivities()).containsExactly(ENTRY1, ENTRY3);
  }

  public void test_combinedWith_one_normalized() {
    CurveUnitParameterSensitivities test = SENSI_1.combinedWith(ENTRY2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_TOTAL_1_2);
  }

  public void test_combinedWith_one_sizeMismatch() {
    assertThrowsIllegalArg(() -> SENSI_1.combinedWith(ENTRY_SMALL));
  }

  public void test_combinedWith_other() {
    CurveUnitParameterSensitivities test = SENSI_1.combinedWith(SENSI_2);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_TOTAL_1_2, ENTRY3);
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
  public void test_multipliedBy_currency() {
    CurveCurrencyParameterSensitivities multiplied = SENSI_2.multipliedBy(USD, FACTOR1);
    assertThat(multiplied.size()).isEqualTo(2);
    DoubleArray test1 = multiplied.getSensitivity(NAME1, USD).getSensitivity();
    for (int i = 0; i < VECTOR1.size(); i++) {
      assertThat(test1.get(i)).isEqualTo(VECTOR2.get(i) * FACTOR1);
    }
    DoubleArray test2 = multiplied.getSensitivity(NAME2, USD).getSensitivity();
    for (int i = 0; i < VECTOR1.size(); i++) {
      assertThat(test2.get(i)).isEqualTo(VECTOR3.get(i) * FACTOR1);
    }
  }

  public void test_multipliedBy() {
    CurveUnitParameterSensitivities multiplied = SENSI_1.multipliedBy(FACTOR1);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(VECTOR1.get(i) * FACTOR1);
    }
  }

  public void test_mapSensitivities() {
    CurveUnitParameterSensitivities multiplied = SENSI_1.mapSensitivities(a -> 1 / a);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < VECTOR1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(1 / VECTOR1.get(i));
    }
  }

  public void test_multipliedBy_vs_combinedWith() {
    CurveUnitParameterSensitivities multiplied = SENSI_2.multipliedBy(2d);
    CurveUnitParameterSensitivities added = SENSI_2.combinedWith(SENSI_2);
    assertThat(multiplied).isEqualTo(added);
  }

  //-------------------------------------------------------------------------
  public void test_equalWithTolerance() {
    CurveUnitParameterSensitivities sensUsdTotal = CurveUnitParameterSensitivities.of(ENTRY_TOTAL_1_2);
    CurveUnitParameterSensitivities sensEur = CurveUnitParameterSensitivities.of(ENTRY3);
    CurveUnitParameterSensitivities sens1plus2 = SENSI_1.combinedWith(ENTRY2);
    CurveUnitParameterSensitivities sensZeroA = CurveUnitParameterSensitivities.of(ENTRY_ZERO3);
    CurveUnitParameterSensitivities sensZeroB = CurveUnitParameterSensitivities.of(ENTRY_ZERO0);
    CurveUnitParameterSensitivities sens1plus2plus0a = SENSI_1.combinedWith(ENTRY2).combinedWith(ENTRY_ZERO0);
    CurveUnitParameterSensitivities sens1plus2plus0b = SENSI_1.combinedWith(ENTRY2).combinedWith(ENTRY_ZERO3);
    CurveUnitParameterSensitivities sens1plus2plus0 = SENSI_1
        .combinedWith(ENTRY2).combinedWith(ENTRY_ZERO0).combinedWith(ENTRY_ZERO3);
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
