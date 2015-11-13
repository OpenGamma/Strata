/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.loader.LoaderUtils;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveGroupEntry;
import com.opengamma.strata.market.value.ValueType;

/**
 * Test {@link RatesCalibrationCsvLoader}.
 */
@Test
public class RatesCalibrationCsvLoaderTest {

  private static final String GROUPS_1 = "classpath:com/opengamma/strata/loader/csv/groups.csv";
  private static final String SETTINGS_1 = "classpath:com/opengamma/strata/loader/csv/settings.csv";
  private static final String CALIBRATION_1 = "classpath:com/opengamma/strata/loader/csv/calibration-1.csv";

  private static final String SETTINGS_EMPTY = "classpath:com/opengamma/strata/loader/csv/settings-empty.csv";
  private static final String CALIBRATION_INVALID_TYPE =
      "classpath:com/opengamma/strata/loader/csv/calibration-invalid-type.csv";

  static {
    // TODO: remove when Joda-Beans issue fixed
    LoaderUtils.findIndex("USD-LIBOR-3M");
  }

  //-------------------------------------------------------------------------
  public void test_parsing() {
    Map<CurveGroupName, CurveGroupDefinition> test = RatesCalibrationCsvLoader.load(
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ResourceLocator.of(CALIBRATION_1));
    assertEquals(test.size(), 1);

    CurveGroupDefinition defn = Iterables.getOnlyElement(test.values());
    assertDefinition(defn);
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Missing settings for curve: .*")
  public void test_noSettings() {
    RatesCalibrationCsvLoader.load(
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_EMPTY),
        ResourceLocator.of(CALIBRATION_1));
  }

  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = "Multiple entries with same key: .*")
  public void test_single_curve_multiple_Files() {
    RatesCalibrationCsvLoader.load(
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CALIBRATION_1), ResourceLocator.of(CALIBRATION_1)));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_invalid_curve_duplicate_points() {
    RatesCalibrationCsvLoader.load(
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CALIBRATION_INVALID_TYPE)));
  }

  //-------------------------------------------------------------------------
  private void assertDefinition(CurveGroupDefinition defn) {
    assertEquals(defn.getName(), CurveGroupName.of("Default"));
    assertEquals(defn.getEntries().size(), 2);

    CurveGroupEntry entry0 = defn.getEntries().get(0);
    CurveGroupEntry entry1 = defn.getEntries().get(1);
    if (entry0.getCurveDefinition().getName().equals(CurveName.of("USD-3ML"))) {
      CurveGroupEntry temp = entry0;
      entry0 = entry1;
      entry1 = temp;
    }

    assertEquals(entry0.getDiscountCurrencies(), ImmutableSet.of(Currency.USD));
    assertEquals(entry0.getIborIndices(), ImmutableSet.of());
    assertEquals(entry0.getOvernightIndices(), ImmutableSet.of());
    assertEquals(entry0.getCurveDefinition().getName(), CurveName.of("USD-Disc"));
    assertEquals(entry0.getCurveDefinition().getYValueType(), ValueType.ZERO_RATE);
    assertEquals(entry0.getCurveDefinition().getParameterCount(), 15);

    assertEquals(entry1.getDiscountCurrencies(), ImmutableSet.of());
    assertEquals(entry1.getIborIndices(), ImmutableSet.of(IborIndices.USD_LIBOR_3M));
    assertEquals(entry1.getOvernightIndices(), ImmutableSet.of());
    assertEquals(entry1.getCurveDefinition().getName(), CurveName.of("USD-3ML"));
    assertEquals(entry1.getCurveDefinition().getYValueType(), ValueType.ZERO_RATE);
    assertEquals(entry1.getCurveDefinition().getParameterCount(), 19);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(RatesCalibrationCsvLoader.class);
  }

}
