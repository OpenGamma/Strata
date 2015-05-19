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
import static org.assertj.core.api.Assertions.within;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;

/**
 * Test {@link CurveParameterSensitivity}.
 */
@Test
public class CurveParameterSensitivityTest {

  private static final double FACTOR1 = 3.14;
  private static final double[] VECTOR_USD1 = new double[] {100.0, 200.0, 300.0, 123.0};
  private static final double[] VECTOR_USD2 = new double[] {1000.0, 250.0, 321.0, 123.0};
  private static final double[] VECTOR_ZERO_4 = new double[] {0.0, 0.0, 0.0, 0.0};
  private static final double[] TOTAL_USD = new double[] {1100.0, 450.0, 621.0, 246.0};
  private static final double[] VECTOR_EUR1 = new double[] {1000.0, 250.0, 321.0, 123.0, 321.0};
  private static final Currency USD = Currency.USD;
  private static final Currency EUR = Currency.EUR;
  private static final String NAME1 = "NAME-1";
  private static final String NAME2 = "NAME-2";
  private static final NameCurrencySensitivityKey KEY_USD = NameCurrencySensitivityKey.of(NAME1, USD);
  private static final NameCurrencySensitivityKey KEY_EUR = NameCurrencySensitivityKey.of(NAME2, EUR);
  private static final NameCurrencySensitivityKey KEY_3 = NameCurrencySensitivityKey.of(NAME2, USD);

  private static final Map<SensitivityKey, double[]> MAP_0 = ImmutableMap.of(KEY_3, VECTOR_ZERO_4);
  private static final CurveParameterSensitivity SENSI_0 =
      CurveParameterSensitivity.builder().sensitivities(MAP_0).build();

  private static final Map<SensitivityKey, double[]> MAP_1 = ImmutableMap.of(KEY_USD, VECTOR_USD1);
  private static final CurveParameterSensitivity SENSI_1 =
      CurveParameterSensitivity.builder().sensitivities(MAP_1).build();

  private static final Map<SensitivityKey, double[]> MAP_2 = ImmutableMap.of(KEY_USD, VECTOR_USD2, KEY_EUR, VECTOR_EUR1);
  private static final CurveParameterSensitivity SENSI_2 =
      CurveParameterSensitivity.builder().sensitivities(MAP_2).build();

  private static final double TOLERENCE_CMP = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_empty() {
    CurveParameterSensitivity test = CurveParameterSensitivity.empty();
    assertThat(test.getSensitivities()).hasSize(0);
  }

  public void test_of_single() {
    CurveParameterSensitivity test = CurveParameterSensitivity.of(KEY_USD, VECTOR_USD2);
    assertThat(test.getSensitivities())
        .hasSize(1)
        .containsKey(KEY_USD)
        .containsEntry(KEY_USD, VECTOR_USD2);
  }

  public void test_of_map() {
    Map<SensitivityKey, double[]> map = ImmutableMap.of(KEY_USD, TOTAL_USD);
    CurveParameterSensitivity test = CurveParameterSensitivity.of(map);
    assertThat(test.getSensitivities())
        .hasSize(1)
        .containsKey(KEY_USD)
        .containsEntry(KEY_USD, TOTAL_USD);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_one() {
    CurveParameterSensitivity test = SENSI_1.combinedWith(KEY_USD, VECTOR_USD2);
    Map<SensitivityKey, double[]> map = ImmutableMap.of(KEY_USD, TOTAL_USD);
    CurveParameterSensitivity expected = CurveParameterSensitivity.builder().sensitivities(map).build();
    assertThat(test).isEqualTo(expected);
  }

  public void test_combinedWith_one_sizeMismatch() {
    assertThrowsIllegalArg(() -> SENSI_1.combinedWith(KEY_USD, VECTOR_EUR1));
  }

  public void test_combinedWith_other() {
    CurveParameterSensitivity test = SENSI_1.combinedWith(SENSI_2);
    Map<SensitivityKey, double[]> map = ImmutableMap.of(KEY_USD, TOTAL_USD, KEY_EUR, VECTOR_EUR1);
    CurveParameterSensitivity expected = CurveParameterSensitivity.builder().sensitivities(map).build();
    assertThat(test).isEqualTo(expected);
  }

  public void test_combinedWith_otherEmpty() {
    CurveParameterSensitivity test = SENSI_1.combinedWith(CurveParameterSensitivity.empty());
    assertThat(test).isSameAs(SENSI_1);
  }

  public void test_combinedWith_empty() {
    CurveParameterSensitivity test = CurveParameterSensitivity.empty().combinedWith(SENSI_1);
    assertThat(test).isSameAs(SENSI_1);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    CurveParameterSensitivity multiplied = SENSI_1.multipliedBy(FACTOR1);
    double[] test = multiplied.getSensitivities().get(KEY_USD);
    for (int i = 0; i < VECTOR_USD1.length; i++) {
      assertThat(test[i]).isEqualTo(VECTOR_USD1[i] * FACTOR1);
    }
  }

  public void test_mapSensitivity() {
    CurveParameterSensitivity multiplied = SENSI_1.mapSensitivity(a -> 1 / a);
    double[] test = multiplied.getSensitivities().get(KEY_USD);
    for (int i = 0; i < VECTOR_USD1.length; i++) {
      assertThat(test[i]).isEqualTo(1 / VECTOR_USD1[i]);
    }
  }

  public void test_multipliedBy_vs_combinedWith() {
    CurveParameterSensitivity multiplied = SENSI_2.multipliedBy(2d);
    CurveParameterSensitivity added = SENSI_2.combinedWith(SENSI_2);
    assertThat(multiplied).isEqualTo(added);
  }

  //-------------------------------------------------------------------------
  public void test_totalPerKey() {
    assertThat(SENSI_1.totalPerKey().size()).isEqualTo(1);
    assertThat(SENSI_1.totalPerKey().get(KEY_USD)).isCloseTo(sum(VECTOR_USD1), within(1E-8));

    assertThat(SENSI_2.totalPerKey().size()).isEqualTo(2);
    assertThat(SENSI_2.totalPerKey().get(KEY_USD)).isCloseTo(sum(VECTOR_USD2), within(1E-8));
    assertThat(SENSI_2.totalPerKey().get(KEY_EUR)).isCloseTo(sum(VECTOR_EUR1), within(1E-8));
  }

  public void test_total() {
    assertThat(SENSI_1.total()).isCloseTo(sum(VECTOR_USD1), within(1E-8));
    assertThat(SENSI_2.total()).isCloseTo(sum(VECTOR_USD2) + sum(VECTOR_EUR1), within(1E-8));
  }

  private static double sum(double[] array) {
    double total = 0d;
    for (int i = 0; i < array.length; i++) {
      total += array[i];
    }
    return total;
  }

  //-------------------------------------------------------------------------
  public void test_equalWithTolerance() {
    assertThat(SENSI_1.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isTrue();
    assertThat(SENSI_1.equalWithTolerance(CurveParameterSensitivity.of(KEY_USD, TOTAL_USD), TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(CurveParameterSensitivity.of(KEY_USD, VECTOR_EUR1), TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_1.equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_2.equalWithTolerance(SENSI_1, TOLERENCE_CMP)).isFalse();
    assertThat(SENSI_0.equalWithTolerance(CurveParameterSensitivity.empty(), TOLERENCE_CMP)).isTrue();
    assertThat(CurveParameterSensitivity.empty().equalWithTolerance(SENSI_0, TOLERENCE_CMP)).isTrue();
    assertThat(SENSI_2.equalWithTolerance(SENSI_2.combinedWith(SENSI_0), TOLERENCE_CMP)).isTrue();
    assertThat(SENSI_2.combinedWith(SENSI_0).equalWithTolerance(SENSI_2, TOLERENCE_CMP)).isTrue();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(CurveParameterSensitivity.empty());
    coverImmutableBean(SENSI_1);
    coverBeanEquals(SENSI_1, SENSI_2);
    CurveParameterSensitivity test = CurveParameterSensitivity.of(KEY_USD, VECTOR_USD2);
    coverBeanEquals(SENSI_1, test);
  }

}
