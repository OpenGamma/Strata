/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupEntry;

/**
 * Test {@link RatesCalibrationCsvLoader}.
 */
public class RatesCalibrationCsvLoaderTest {

  private static final String GROUPS_1 = "classpath:com/opengamma/strata/loader/csv/groups.csv";
  private static final String SETTINGS_1 = "classpath:com/opengamma/strata/loader/csv/settings.csv";
  private static final String SEASONALITY_1 = "classpath:com/opengamma/strata/loader/csv/seasonality.csv";
  private static final String CALIBRATION_1 = "classpath:com/opengamma/strata/loader/csv/calibration-1.csv";

  private static final String SETTINGS_EMPTY = "classpath:com/opengamma/strata/loader/csv/settings-empty.csv";
  private static final String CALIBRATION_INVALID_TYPE =
      "classpath:com/opengamma/strata/loader/csv/calibration-invalid-type.csv";

  //-------------------------------------------------------------------------
  @Test
  public void test_parsing() {
    Map<CurveGroupName, RatesCurveGroupDefinition> test = RatesCalibrationCsvLoader.loadWithSeasonality(
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ResourceLocator.of(SEASONALITY_1),
        ImmutableList.of(ResourceLocator.of(CALIBRATION_1)));
    assertThat(test).hasSize(1);

    assertDefinition(test.get(CurveGroupName.of("Default")));
  }

  @Test
  public void test_noSettings() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RatesCalibrationCsvLoader.load(
            ResourceLocator.of(GROUPS_1),
            ResourceLocator.of(SETTINGS_EMPTY),
            ResourceLocator.of(CALIBRATION_1)))
        .withMessageMatching("Missing settings for curve: .*");
  }

  @Test
  public void test_single_curve_multiple_Files() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RatesCalibrationCsvLoader.load(
            ResourceLocator.of(GROUPS_1),
            ResourceLocator.of(SETTINGS_1),
            ImmutableList.of(ResourceLocator.of(CALIBRATION_1), ResourceLocator.of(CALIBRATION_1))))
        .withMessageMatching("Multiple entries with same key: .*");
  }

  @Test
  public void test_invalid_curve_duplicate_points() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RatesCalibrationCsvLoader.load(
            ResourceLocator.of(GROUPS_1),
            ResourceLocator.of(SETTINGS_1),
            ImmutableList.of(ResourceLocator.of(CALIBRATION_INVALID_TYPE))));
  }

  //-------------------------------------------------------------------------
  private void assertDefinition(RatesCurveGroupDefinition defn) {
    assertThat(defn.getName()).isEqualTo(CurveGroupName.of("Default"));
    assertThat(defn.getEntries()).hasSize(3);
    assertThat(defn.getSeasonalityDefinitions()).hasSize(1);
    assertThat(defn.getSeasonalityDefinitions().get(CurveName.of("USD-CPI")).getAdjustmentType()).isEqualTo(ShiftType.SCALED);

    RatesCurveGroupEntry entry0 = findEntry(defn, "USD-Disc");
    RatesCurveGroupEntry entry1 = findEntry(defn, "USD-3ML");
    RatesCurveGroupEntry entry2 = findEntry(defn, "USD-CPI");
    CurveDefinition defn0 = defn.findCurveDefinition(entry0.getCurveName()).get();
    CurveDefinition defn1 = defn.findCurveDefinition(entry1.getCurveName()).get();
    CurveDefinition defn2 = defn.findCurveDefinition(entry2.getCurveName()).get();

    assertThat(entry0.getDiscountCurrencies()).containsOnly(Currency.USD);
    assertThat(entry0.getIndices()).isEmpty();
    assertThat(defn0.getName()).isEqualTo(CurveName.of("USD-Disc"));
    assertThat(defn0.getYValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThat(defn0.getParameterCount()).isEqualTo(17);

    assertThat(entry1.getDiscountCurrencies()).isEmpty();
    assertThat(entry1.getIndices()).containsOnly(IborIndices.USD_LIBOR_3M);
    assertThat(defn1.getName()).isEqualTo(CurveName.of("USD-3ML"));
    assertThat(defn1.getYValueType()).isEqualTo(ValueType.ZERO_RATE);
    assertThat(defn1.getParameterCount()).isEqualTo(27);

    assertThat(entry2.getDiscountCurrencies()).isEmpty();
    assertThat(entry2.getIndices()).containsOnly(PriceIndices.US_CPI_U);
    assertThat(defn2.getName()).isEqualTo(CurveName.of("USD-CPI"));
    assertThat(defn2.getYValueType()).isEqualTo(ValueType.PRICE_INDEX);
    assertThat(defn2.getParameterCount()).isEqualTo(2);
  }

  private RatesCurveGroupEntry findEntry(RatesCurveGroupDefinition defn, String curveName) {
    return defn.getEntries().stream().filter(d -> d.getCurveName().getName().equals(curveName)).findFirst().get();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(RatesCalibrationCsvLoader.class);
  }

}
