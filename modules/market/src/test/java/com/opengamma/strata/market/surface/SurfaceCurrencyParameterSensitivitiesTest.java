/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.BasicProjectAssertions.assertThat;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.DoubleArrayMath.sum;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;

/**
 * Test {@link SurfaceCurrencyParameterSensitivities}.
 */
@Test
public class SurfaceCurrencyParameterSensitivitiesTest {
  private static final double FACTOR = 2.5d;
  private static final double RATE = 1.6d;
  private static final FxRate FX_RATE = FxRate.of(EUR, USD, RATE);

  private static final DoubleArray SENSI_USD1 = DoubleArray.of(-2.4d, -20.56d, 0.344d, -2562.2d);
  private static final SurfaceName NAME_USD1 = SurfaceName.of("SurfaceUsd1");
  private static final SurfaceMetadata META_USD1 = DefaultSurfaceMetadata.of(NAME_USD1);
  private static final SurfaceCurrencyParameterSensitivity ENTRY_USD1 =
      SurfaceCurrencyParameterSensitivity.of(META_USD1, USD, SENSI_USD1);

  private static final DoubleArray SENSI_USD2 = DoubleArray.of(100d, 200d, 300d);
  private static final SurfaceName NAME_USD2 = SurfaceName.of("SurfaceUsd2");
  private static final SurfaceMetadata META_USD2 = DefaultSurfaceMetadata.of(NAME_USD2);
  private static final SurfaceCurrencyParameterSensitivity ENTRY_USD2 =
      SurfaceCurrencyParameterSensitivity.of(META_USD2, USD, SENSI_USD2);

  private static final DoubleArray SENSI_USD2_ANOTHER = DoubleArray.of(150d, -250d, 200d);
  private static final SurfaceCurrencyParameterSensitivity ENTRY_USD2_ANOTHER =
      SurfaceCurrencyParameterSensitivity.of(META_USD2, USD, SENSI_USD2_ANOTHER);

  private static final DoubleArray SENSI_USD2_COMB = DoubleArray.of(SENSI_USD2.get(0) + SENSI_USD2_ANOTHER.get(0),
      SENSI_USD2.get(1) + SENSI_USD2_ANOTHER.get(1), SENSI_USD2.get(2) + SENSI_USD2_ANOTHER.get(2));
  private static final SurfaceCurrencyParameterSensitivity ENTRY_USD2_COMB =
      SurfaceCurrencyParameterSensitivity.of(META_USD2, USD, SENSI_USD2_COMB);

  private static final DoubleArray SENSI_USD2_MISMATCH = DoubleArray.of(-0d, 0d);
  private static final SurfaceCurrencyParameterSensitivity ENTRY_USD2_MISMATCH =
      SurfaceCurrencyParameterSensitivity.of(META_USD2, USD, SENSI_USD2_MISMATCH);

  private static final DoubleArray SENSI_EUR1 = DoubleArray.of(100d, 200d, 300d);
  private static final SurfaceCurrencyParameterSensitivity ENTRY_EUR1 =
      SurfaceCurrencyParameterSensitivity.of(META_USD2, EUR, SENSI_EUR1);
  private static final DoubleArray SENSI_EUR1_CONV = DoubleArray.of(100d * RATE, 200d * RATE, 300d * RATE);
  private static final SurfaceCurrencyParameterSensitivity ENTRY_EUR1_CONV =
      SurfaceCurrencyParameterSensitivity.of(META_USD2, USD, SENSI_EUR1_CONV);

  private static final DoubleArray SENSI_USD2_TOTAL = DoubleArray.of(
      SENSI_USD2.get(0) + SENSI_USD2_ANOTHER.get(0) + SENSI_EUR1_CONV.get(0),
      SENSI_USD2.get(1) + SENSI_USD2_ANOTHER.get(1) + SENSI_EUR1_CONV.get(1),
      SENSI_USD2.get(2) + SENSI_USD2_ANOTHER.get(2) + SENSI_EUR1_CONV.get(2));
  private static final SurfaceCurrencyParameterSensitivity ENTRY_USD2_TOTAL =
      SurfaceCurrencyParameterSensitivity.of(META_USD2, USD, SENSI_USD2_TOTAL);

  public void test_empty() {
    SurfaceCurrencyParameterSensitivities test = SurfaceCurrencyParameterSensitivities.empty();
    assertEquals(test.size(), 0);
    assertEquals(test.getSensitivities().size(), 0);
  }

  public void test_of_single() {
    SurfaceCurrencyParameterSensitivities test = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    assertEquals(test.size(), 1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD1);
  }

  public void test_list_none() {
    ImmutableList<SurfaceCurrencyParameterSensitivity> list = ImmutableList.of();
    SurfaceCurrencyParameterSensitivities test = SurfaceCurrencyParameterSensitivities.of(list);
    assertEquals(test.size(), 0);
    assertEquals(test.getSensitivities().size(), 0);
  }

  public void test_of_list_notNormalized() {
    ImmutableList<SurfaceCurrencyParameterSensitivity> list = ImmutableList.of(ENTRY_USD1, ENTRY_USD2);
    SurfaceCurrencyParameterSensitivities test = SurfaceCurrencyParameterSensitivities.of(list);
    assertEquals(test.size(), 2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD1, ENTRY_USD2);
  }

  public void test_of_list_normalized() {
    ImmutableList<SurfaceCurrencyParameterSensitivity> list = ImmutableList.of(ENTRY_USD2, ENTRY_USD2_ANOTHER);
    SurfaceCurrencyParameterSensitivities test = SurfaceCurrencyParameterSensitivities.of(list);
    assertEquals(test.size(), 1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD2_COMB);
  }

  //-------------------------------------------------------------------------
  public void test_getSensitivity() {
    SurfaceCurrencyParameterSensitivities test = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    assertEquals(test.getSensitivity(NAME_USD1, USD), ENTRY_USD1);
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME_USD1, EUR));
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME_USD2, USD));
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME_USD2, EUR));
  }

  public void test_findSensitivity() {
    SurfaceCurrencyParameterSensitivities test = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    assertThat(test.findSensitivity(NAME_USD1, USD)).hasValue(ENTRY_USD1);
    assertThat(test.findSensitivity(NAME_USD1, EUR)).isEmpty();
    assertThat(test.findSensitivity(NAME_USD2, USD)).isEmpty();
    assertThat(test.findSensitivity(NAME_USD2, EUR)).isEmpty();
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_one_notNormalized() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    SurfaceCurrencyParameterSensitivities test = base.combinedWith(ENTRY_EUR1);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD1, ENTRY_EUR1);
  }

  public void test_combinedWith_one_normalized() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD2);
    SurfaceCurrencyParameterSensitivities test = base.combinedWith(ENTRY_USD2_ANOTHER);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD2_COMB);
  }

  public void test_combinedWith_one_sizeMismatch() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD2);
    assertThrowsIllegalArg(() -> base.combinedWith(ENTRY_USD2_MISMATCH));
  }

  public void test_combinedWith_other() {
    SurfaceCurrencyParameterSensitivities base1 = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1, ENTRY_USD2);
    SurfaceCurrencyParameterSensitivities base2 = SurfaceCurrencyParameterSensitivities.of(ENTRY_EUR1);
    SurfaceCurrencyParameterSensitivities test = base1.combinedWith(base2);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD1, ENTRY_USD2, ENTRY_EUR1);
  }

  public void test_combinedWith_otherEmpty() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    SurfaceCurrencyParameterSensitivities test = base.combinedWith(SurfaceCurrencyParameterSensitivities.empty());
    assertEquals(test, base);
  }

  public void test_combinedWith_empty() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    SurfaceCurrencyParameterSensitivities test = SurfaceCurrencyParameterSensitivities.empty().combinedWith(base);
    assertEquals(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo_singleCurrency() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    SurfaceCurrencyParameterSensitivities test = base.convertedTo(USD, FxMatrix.empty());
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD1);
  }

  public void test_convertedTo_multipleCurrency() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1, ENTRY_EUR1);
    SurfaceCurrencyParameterSensitivities test = base.convertedTo(USD, FX_RATE);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD1, ENTRY_EUR1_CONV);
  }

  public void test_convertedTo_multipleCurrency_mergeWhenSameCurveName() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD2_COMB);
    SurfaceCurrencyParameterSensitivities test = base.combinedWith(ENTRY_EUR1).convertedTo(USD, FX_RATE);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD2_TOTAL);
  }

  //-------------------------------------------------------------------------
  public void test_total_singleCurrency() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    assertThat(base.total(USD, FxMatrix.empty()))
        .hasAmount(sum(SENSI_USD1.toArray()), within(1E-8));
  }

  public void test_total_multipleCurrency() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1, ENTRY_EUR1);
    assertThat(base.total(USD, FX_RATE)).hasAmount(sum(SENSI_USD1.toArray()) + sum(SENSI_EUR1_CONV.toArray()), within(1E-8));
  }

  public void test_totalMulti_singleCurrency() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    assertThat(base.total().size()).isEqualTo(1);
    assertThat(base.total().getAmount(USD).getAmount()).isCloseTo(sum(SENSI_USD1.toArray()), within(1E-8));
  }

  public void test_totalMulti_multipleCurrency() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1, ENTRY_EUR1);
    assertThat(base.total().size()).isEqualTo(2);
    assertThat(base.total().getAmount(USD).getAmount()).isCloseTo(sum(SENSI_USD1.toArray()), within(1E-8));
    assertThat(base.total().getAmount(EUR).getAmount()).isCloseTo(sum(SENSI_EUR1.toArray()), within(1E-8));
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    SurfaceCurrencyParameterSensitivities multiplied = base.multipliedBy(FACTOR);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < SENSI_USD1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(SENSI_USD1.get(i) * FACTOR);
    }
  }

  public void test_mapSensitivities() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    SurfaceCurrencyParameterSensitivities multiplied = base.mapSensitivities(a -> 1 / a);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < SENSI_USD1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(1 / SENSI_USD1.get(i));
    }
  }

  public void test_multipliedBy_vs_combinedWith() {
    SurfaceCurrencyParameterSensitivities base = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1, ENTRY_EUR1);
    SurfaceCurrencyParameterSensitivities multiplied = base.multipliedBy(2d);
    SurfaceCurrencyParameterSensitivities added = base.combinedWith(base);
    assertThat(multiplied).isEqualTo(added);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SurfaceCurrencyParameterSensitivities test1 = SurfaceCurrencyParameterSensitivities.of(ENTRY_USD1);
    SurfaceCurrencyParameterSensitivities test2 = SurfaceCurrencyParameterSensitivities.of(ENTRY_EUR1, ENTRY_USD2);
    coverImmutableBean(CurveCurrencyParameterSensitivities.empty());
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

}
