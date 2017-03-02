/*
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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupEntry;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link RatesCalibrationCsvLoader}.
 */
@Test
public class RatesCalibrationCsvLoaderTest {

  private static final String GROUPS_1 = "classpath:com/opengamma/strata/loader/csv/groups.csv";
  private static final String SETTINGS_1 = "classpath:com/opengamma/strata/loader/csv/settings.csv";
  private static final String SEASONALITY_1 = "classpath:com/opengamma/strata/loader/csv/seasonality.csv";
  private static final String CALIBRATION_1 = "classpath:com/opengamma/strata/loader/csv/calibration-1.csv";

  private static final String SETTINGS_EMPTY = "classpath:com/opengamma/strata/loader/csv/settings-empty.csv";
  private static final String CALIBRATION_INVALID_TYPE =
      "classpath:com/opengamma/strata/loader/csv/calibration-invalid-type.csv";

  //-------------------------------------------------------------------------
  public void test_parsing() {
    Map<CurveGroupName, CurveGroupDefinition> test = RatesCalibrationCsvLoader.loadWithSeasonality(
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ResourceLocator.of(SEASONALITY_1),
        ImmutableList.of(ResourceLocator.of(CALIBRATION_1)));
    assertEquals(test.size(), 1);

    assertDefinition(test.get(CurveGroupName.of("Default")));
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
    assertEquals(defn.getEntries().size(), 3);
    assertEquals(defn.getSeasonalityDefinitions().size(), 1);
    assertEquals(defn.getSeasonalityDefinitions().get(CurveName.of("USD-CPI")).getAdjustmentType(), ShiftType.SCALED);

    CurveGroupEntry entry0 = findEntry(defn, "USD-Disc");
    CurveGroupEntry entry1 = findEntry(defn, "USD-3ML");
    CurveGroupEntry entry2 = findEntry(defn, "USD-CPI");
    CurveDefinition defn0 = defn.findCurveDefinition(entry0.getCurveName()).get();
    CurveDefinition defn1 = defn.findCurveDefinition(entry1.getCurveName()).get();
    CurveDefinition defn2 = defn.findCurveDefinition(entry2.getCurveName()).get();

    assertEquals(entry0.getDiscountCurrencies(), ImmutableSet.of(Currency.USD));
    assertEquals(entry0.getIndices(), ImmutableSet.of());
    assertEquals(defn0.getName(), CurveName.of("USD-Disc"));
    assertEquals(defn0.getYValueType(), ValueType.ZERO_RATE);
    assertEquals(defn0.getParameterCount(), 17);

    assertEquals(entry1.getDiscountCurrencies(), ImmutableSet.of());
    assertEquals(entry1.getIndices(), ImmutableSet.of(IborIndices.USD_LIBOR_3M));
    assertEquals(defn1.getName(), CurveName.of("USD-3ML"));
    assertEquals(defn1.getYValueType(), ValueType.ZERO_RATE);
    assertEquals(defn1.getParameterCount(), 27);

    assertEquals(entry2.getDiscountCurrencies(), ImmutableSet.of());
    assertEquals(entry2.getIndices(), ImmutableSet.of(PriceIndices.US_CPI_U));
    assertEquals(defn2.getName(), CurveName.of("USD-CPI"));
    assertEquals(defn2.getYValueType(), ValueType.PRICE_INDEX);
    assertEquals(defn2.getParameterCount(), 2);
  }

  private CurveGroupEntry findEntry(CurveGroupDefinition defn, String curveName) {
    return defn.getEntries().stream().filter(d -> d.getCurveName().getName().equals(curveName)).findFirst().get();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(RatesCalibrationCsvLoader.class);
  }

}
