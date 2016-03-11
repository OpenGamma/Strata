/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.BasicProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveUnitParameterSensitivities;

/**
 * Test {@link SurfaceUnitParameterSensitivities}.
 */
@Test
public class SurfaceUnitParameterSensitivitiesTest {
  private static final double FACTOR = 2.5d;

  private static final DoubleArray SENSI_USD1 = DoubleArray.of(-2.4d, -20.56d, 0.344d, -2562.2d);
  private static final SurfaceName NAME_USD1 = SurfaceName.of("SurfaceUsd1");
  private static final SurfaceMetadata META_USD1 = DefaultSurfaceMetadata.of(NAME_USD1);
  private static final SurfaceUnitParameterSensitivity ENTRY_USD1 =
      SurfaceUnitParameterSensitivity.of(META_USD1, SENSI_USD1);

  private static final DoubleArray SENSI_USD2 = DoubleArray.of(100d, 200d, 300d);
  private static final SurfaceName NAME_USD2 = SurfaceName.of("SurfaceUsd2");
  private static final SurfaceMetadata META_USD2 = DefaultSurfaceMetadata.of(NAME_USD2);
  private static final SurfaceUnitParameterSensitivity ENTRY_USD2 =
      SurfaceUnitParameterSensitivity.of(META_USD2, SENSI_USD2);
  
  private static final SurfaceName NAME_USD3 = SurfaceName.of("SurfaceUsd3");
  private static final SurfaceMetadata META_USD3 = DefaultSurfaceMetadata.of(NAME_USD3);

  private static final DoubleArray SENSI_USD2_ANOTHER = DoubleArray.of(150d, -250d, 200d);
  private static final SurfaceUnitParameterSensitivity ENTRY_USD2_ANOTHER =
      SurfaceUnitParameterSensitivity.of(META_USD2, SENSI_USD2_ANOTHER);

  private static final DoubleArray SENSI_USD2_COMB = DoubleArray.of(SENSI_USD2.get(0) + SENSI_USD2_ANOTHER.get(0),
      SENSI_USD2.get(1) + SENSI_USD2_ANOTHER.get(1), SENSI_USD2.get(2) + SENSI_USD2_ANOTHER.get(2));
  private static final SurfaceUnitParameterSensitivity ENTRY_USD2_COMB =
      SurfaceUnitParameterSensitivity.of(META_USD2, SENSI_USD2_COMB);

  private static final DoubleArray SENSI_USD2_MISMATCH = DoubleArray.of(-0d, 0d);
  private static final SurfaceUnitParameterSensitivity ENTRY_USD2_MISMATCH =
      SurfaceUnitParameterSensitivity.of(META_USD2, SENSI_USD2_MISMATCH);

  private static final DoubleArray SENSI_EUR1 = DoubleArray.of(100d, 200d, 300d);
  private static final SurfaceUnitParameterSensitivity ENTRY_EUR1 =
      SurfaceUnitParameterSensitivity.of(META_USD2, SENSI_EUR1);

  private static final SurfaceUnitParameterSensitivity ENTRY_EUR2 =
      SurfaceUnitParameterSensitivity.of(META_USD3, SENSI_EUR1);

  public void test_empty() {
    SurfaceUnitParameterSensitivities test = SurfaceUnitParameterSensitivities.empty();
    assertEquals(test.size(), 0);
    assertEquals(test.getSensitivities().size(), 0);
  }

  public void test_of_single() {
    SurfaceUnitParameterSensitivities test = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    assertEquals(test.size(), 1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD1);
  }

  public void test_list_none() {
    ImmutableList<SurfaceUnitParameterSensitivity> list = ImmutableList.of();
    SurfaceUnitParameterSensitivities test = SurfaceUnitParameterSensitivities.of(list);
    assertEquals(test.size(), 0);
    assertEquals(test.getSensitivities().size(), 0);
  }

  public void test_of_list_notNormalized() {
    ImmutableList<SurfaceUnitParameterSensitivity> list = ImmutableList.of(ENTRY_USD1, ENTRY_USD2);
    SurfaceUnitParameterSensitivities test = SurfaceUnitParameterSensitivities.of(list);
    assertEquals(test.size(), 2);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD1, ENTRY_USD2);
  }

  public void test_of_list_normalized() {
    ImmutableList<SurfaceUnitParameterSensitivity> list = ImmutableList.of(ENTRY_USD2, ENTRY_USD2_ANOTHER);
    SurfaceUnitParameterSensitivities test = SurfaceUnitParameterSensitivities.of(list);
    assertEquals(test.size(), 1);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD2_COMB);
  }

  //-------------------------------------------------------------------------
  public void test_getSensitivity() {
    SurfaceUnitParameterSensitivities test = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    assertEquals(test.getSensitivity(NAME_USD1), ENTRY_USD1);
    assertThrowsIllegalArg(() -> test.getSensitivity(NAME_USD2));
  }

  public void test_findSensitivity() {
    SurfaceUnitParameterSensitivities test = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    assertThat(test.findSensitivity(NAME_USD1)).hasValue(ENTRY_USD1);
    assertThat(test.findSensitivity(NAME_USD2)).isEmpty();
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_one_notNormalized() {
    SurfaceUnitParameterSensitivities base = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    SurfaceUnitParameterSensitivities test = base.combinedWith(ENTRY_EUR1);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD1, ENTRY_EUR1);
  }

  public void test_combinedWith_one_normalized() {
    SurfaceUnitParameterSensitivities base = SurfaceUnitParameterSensitivities.of(ENTRY_USD2);
    SurfaceUnitParameterSensitivities test = base.combinedWith(ENTRY_USD2_ANOTHER);
    assertThat(test.getSensitivities()).containsExactly(ENTRY_USD2_COMB);
  }

  public void test_combinedWith_one_sizeMismatch() {
    SurfaceUnitParameterSensitivities base = SurfaceUnitParameterSensitivities.of(ENTRY_USD2);
    assertThrowsIllegalArg(() -> base.combinedWith(ENTRY_USD2_MISMATCH));
  }

  public void test_combinedWith_other() {
    SurfaceUnitParameterSensitivities base1 = SurfaceUnitParameterSensitivities.of(ENTRY_USD1, ENTRY_USD2);
    SurfaceUnitParameterSensitivities base2 = SurfaceUnitParameterSensitivities.of(ENTRY_EUR2);
    SurfaceUnitParameterSensitivities test = base1.combinedWith(base2);
    assertThat(test.getSensitivities()).containsOnly(ENTRY_USD1, ENTRY_USD2, ENTRY_EUR2);
  }

  public void test_combinedWith_otherEmpty() {
    SurfaceUnitParameterSensitivities base = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    SurfaceUnitParameterSensitivities test = base.combinedWith(SurfaceUnitParameterSensitivities.empty());
    assertEquals(test, base);
  }

  public void test_combinedWith_empty() {
    SurfaceUnitParameterSensitivities base = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    SurfaceUnitParameterSensitivities test = SurfaceUnitParameterSensitivities.empty().combinedWith(base);
    assertEquals(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    SurfaceUnitParameterSensitivities base = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    SurfaceUnitParameterSensitivities multiplied = base.multipliedBy(FACTOR);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < SENSI_USD1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(SENSI_USD1.get(i) * FACTOR);
    }
  }

  public void test_mapSensitivities() {
    SurfaceUnitParameterSensitivities base = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    SurfaceUnitParameterSensitivities multiplied = base.mapSensitivities(a -> 1 / a);
    DoubleArray test = multiplied.getSensitivities().get(0).getSensitivity();
    for (int i = 0; i < SENSI_USD1.size(); i++) {
      assertThat(test.get(i)).isEqualTo(1 / SENSI_USD1.get(i));
    }
  }

  public void test_multipliedBy_vs_combinedWith() {
    SurfaceUnitParameterSensitivities base = SurfaceUnitParameterSensitivities.of(ENTRY_USD1, ENTRY_EUR1);
    SurfaceUnitParameterSensitivities multiplied = base.multipliedBy(2d);
    SurfaceUnitParameterSensitivities added = base.combinedWith(base);
    assertThat(multiplied).isEqualTo(added);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SurfaceUnitParameterSensitivities test1 = SurfaceUnitParameterSensitivities.of(ENTRY_USD1);
    SurfaceUnitParameterSensitivities test2 = SurfaceUnitParameterSensitivities.of(ENTRY_EUR1, ENTRY_USD2);
    coverImmutableBean(CurveUnitParameterSensitivities.empty());
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

}
