/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.offset;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.RatesCurveGroup;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link RatesCurvesCsvLoader}.
 */
public class RatesCurvesCsvLoaderTest {

  private static final String GROUPS_1 = "classpath:com/opengamma/strata/loader/csv/groups.csv";
  private static final String SETTINGS_1 = "classpath:com/opengamma/strata/loader/csv/settings.csv";
  private static final String CURVES_1 = "classpath:com/opengamma/strata/loader/csv/curves-1.csv";
  private static final String CURVES_2 = "classpath:com/opengamma/strata/loader/csv/curves-2.csv";
  private static final String CURVES_3 = "classpath:com/opengamma/strata/loader/csv/curves-3.csv";
  private static final String CURVES_1_AND_2 = "classpath:com/opengamma/strata/loader/csv/curves-1-and-2.csv";

  private static final String SETTINGS_INVALID_DAY_COUNT =
      "classpath:com/opengamma/strata/loader/csv/settings-invalid-day-count.csv";
  private static final String SETTINGS_INVALID_INTERPOLATOR =
      "classpath:com/opengamma/strata/loader/csv/settings-invalid-interpolator.csv";
  private static final String SETTINGS_INVALID_LEFT_EXTRAPOLATOR =
      "classpath:com/opengamma/strata/loader/csv/settings-invalid-left-extrapolator.csv";
  private static final String SETTINGS_INVALID_RIGHT_EXTRAPOLATOR =
      "classpath:com/opengamma/strata/loader/csv/settings-invalid-right-extrapolator.csv";
  private static final String SETTINGS_INVALID_MISSING_COLUMN =
      "classpath:com/opengamma/strata/loader/csv/settings-invalid-missing-column.csv";
  private static final String SETTINGS_INVALID_VALUE_TYPE =
      "classpath:com/opengamma/strata/loader/csv/settings-invalid-value-type.csv";
  private static final String SETTINGS_EMPTY =
      "classpath:com/opengamma/strata/loader/csv/settings-empty.csv";

  private static final String GROUPS_INVALID_CURVE_TYPE =
      "classpath:com/opengamma/strata/loader/csv/groups-invalid-curve-type.csv";
  private static final String GROUPS_INVALID_REFERENCE_INDEX =
      "classpath:com/opengamma/strata/loader/csv/groups-invalid-reference-index.csv";
  private static final String CURVES_INVALID_DUPLICATE_POINTS =
      "classpath:com/opengamma/strata/loader/csv/curves-invalid-duplicate-points.csv";

  // curve date used in the test data
  private static final LocalDate CURVE_DATE = LocalDate.of(2009, 7, 31);
  private static final LocalDate CURVE_DATE_CURVES_3 = LocalDate.of(2009, 7, 30);

  // tolerance
  private static final double TOLERANCE = 1.0E-4;

  //-------------------------------------------------------------------------
  @Test
  public void test_missing_settings_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testSettings("classpath:invalid"));
  }

  @Test
  public void test_invalid_settings_missing_column_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testSettings(SETTINGS_INVALID_MISSING_COLUMN))
        .withMessage("Header not found: 'Curve Name'");
  }

  @Test
  public void test_invalid_settings_day_count_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testSettings(SETTINGS_INVALID_DAY_COUNT))
        .withMessageMatching("Unknown DayCount value.*");
  }

  @Test
  public void test_invalid_settings_interpolator_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testSettings(SETTINGS_INVALID_INTERPOLATOR))
        .withMessage("CurveInterpolator name not found: Wacky");
  }

  @Test
  public void test_invalid_settings_left_extrapolator_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testSettings(SETTINGS_INVALID_LEFT_EXTRAPOLATOR))
        .withMessage("CurveExtrapolator name not found: Polynomial");
  }

  @Test
  public void test_invalid_settings_right_extrapolator_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testSettings(SETTINGS_INVALID_RIGHT_EXTRAPOLATOR))
        .withMessage("CurveExtrapolator name not found: Polynomial");
  }

  @Test
  public void test_invalid_settings_value_type_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testSettings(SETTINGS_INVALID_VALUE_TYPE))
        .withMessage("Unsupported Value Type in curve settings: DS");
  }

  private void testSettings(String settingsResource) {
    RatesCurvesCsvLoader.load(
        CURVE_DATE,
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(settingsResource),
        ImmutableList.of(ResourceLocator.of(CURVES_1)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_missing_groups_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testGroups("classpath:invalid"));
  }

  @Test
  public void test_invalid_groups_curve_type_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testGroups(GROUPS_INVALID_CURVE_TYPE))
        .withMessage("Unsupported curve type: Inflation");
  }

  @Test
  public void test_invalid_groups_reference_index_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> testGroups(GROUPS_INVALID_REFERENCE_INDEX))
        .withMessage("Index name not found: LIBOR");
  }

  private void testGroups(String groupsResource) {
    RatesCurvesCsvLoader.load(
        CURVE_DATE,
        ResourceLocator.of(groupsResource),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CURVES_1)));
  }

  @Test
  public void test_noSettings() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RatesCurvesCsvLoader.load(
            CURVE_DATE,
            ResourceLocator.of(GROUPS_1),
            ResourceLocator.of(SETTINGS_EMPTY),
            ImmutableList.of(ResourceLocator.of(CURVES_1))))
        .withMessageMatching("Missing settings for curve: .*");
  }

  @Test
  public void test_single_curve_multiple_Files() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RatesCurvesCsvLoader.load(
            CURVE_DATE,
            ResourceLocator.of(GROUPS_1),
            ResourceLocator.of(SETTINGS_1),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_1))))
        .withMessageMatching("Rates curve loader found multiple curves with the same name: .*");
  }

  @Test
  public void test_multiple_curves_single_file() {
    List<RatesCurveGroup> curveGroups = RatesCurvesCsvLoader.load(
        CURVE_DATE,
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CURVES_1_AND_2)));

    assertCurves(curveGroups);
  }

  @Test
  public void test_multiple_curves_multiple_files() {
    List<RatesCurveGroup> curveGroups = RatesCurvesCsvLoader.load(
        CURVE_DATE,
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2)));

    assertCurves(curveGroups);
  }

  @Test
  public void test_invalid_curve_duplicate_points() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RatesCurvesCsvLoader.load(
            CURVE_DATE,
            ResourceLocator.of(GROUPS_1),
            ResourceLocator.of(SETTINGS_1),
            ImmutableList.of(ResourceLocator.of(CURVES_INVALID_DUPLICATE_POINTS))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_load_all_curves() {
    ListMultimap<LocalDate, RatesCurveGroup> allGroups = RatesCurvesCsvLoader.loadAllDates(
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2), ResourceLocator.of(CURVES_3)));

    assertThat(allGroups.size()).isEqualTo(2);
    assertCurves(allGroups.get(CURVE_DATE));

    List<RatesCurveGroup> curves3 = allGroups.get(CURVE_DATE_CURVES_3);
    assertThat(curves3).hasSize(1);
    RatesCurveGroup group = curves3.get(0);

    // All curve points are set to 0 in test data to ensure these are really different curve instances
    Curve usdDisc = group.findDiscountCurve(Currency.USD).get();
    InterpolatedNodalCurve usdDiscNodal = (InterpolatedNodalCurve) usdDisc;
    assertThat(usdDiscNodal.getMetadata().getCurveName()).isEqualTo(CurveName.of("USD-Disc"));
    assertThat(usdDiscNodal.getYValues().equalZeroWithTolerance(0d)).isTrue();

    Curve usd3ml = group.findForwardCurve(IborIndices.USD_LIBOR_3M).get();
    InterpolatedNodalCurve usd3mlNodal = (InterpolatedNodalCurve) usd3ml;
    assertThat(usd3mlNodal.getMetadata().getCurveName()).isEqualTo(CurveName.of("USD-3ML"));
    assertThat(usd3mlNodal.getYValues().equalZeroWithTolerance(0d)).isTrue();
  }

  @Test
  public void test_load_curves_date_filtering() {
    List<RatesCurveGroup> curves = RatesCurvesCsvLoader.load(
        CURVE_DATE,
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2), ResourceLocator.of(CURVES_3)));

    assertCurves(curves);
  }

  //-------------------------------------------------------------------------
  private void assertCurves(List<RatesCurveGroup> curveGroups) {
    assertThat(curveGroups).isNotNull();
    assertThat(curveGroups).hasSize(1);

    RatesCurveGroup curveGroup = curveGroups.get(0);
    assertThat(curveGroup.getName()).isEqualTo(CurveGroupName.of("Default"));
    assertUsdDisc(curveGroup.findDiscountCurve(Currency.USD).get());

    Curve usd3ml = curveGroup.findForwardCurve(IborIndices.USD_LIBOR_3M).get();
    assertUsd3ml(usd3ml);
  }

  private void assertUsdDisc(Curve curve) {
    assertThat(curve instanceof InterpolatedNodalCurve).isTrue();
    InterpolatedNodalCurve nodalCurve = (InterpolatedNodalCurve) curve;
    assertThat(nodalCurve.getMetadata().getCurveName()).isEqualTo(CurveName.of("USD-Disc"));

    LocalDate valuationDate = LocalDate.of(2009, 7, 31);
    LocalDate[] nodeDates = new LocalDate[] {
        LocalDate.of(2009, 11, 6),
        LocalDate.of(2010, 2, 8),
        LocalDate.of(2010, 8, 6),
        LocalDate.of(2011, 8, 8),
        LocalDate.of(2012, 8, 8),
        LocalDate.of(2014, 8, 6),
        LocalDate.of(2019, 8, 7)
    };
    String[] labels = new String[] {"3M", "6M", "1Y", "2Y", "3Y", "5Y", "10Y"};

    for (int i = 0; i < nodalCurve.getXValues().size(); i++) {
      LocalDate nodeDate = nodeDates[i];
      double actualYearFraction = nodalCurve.getXValues().get(i);
      double expectedYearFraction = getYearFraction(valuationDate, nodeDate);
      assertThat(actualYearFraction).isCloseTo(expectedYearFraction, offset(TOLERANCE));

      ParameterMetadata nodeMetadata = nodalCurve.getMetadata().getParameterMetadata().get().get(i);
      assertThat(nodeMetadata.getLabel()).isEqualTo(labels[i]);
    }

    DoubleArray expectedYValues = DoubleArray.of(
        0.001763775,
        0.002187884,
        0.004437206,
        0.011476741,
        0.017859057,
        0.026257102,
        0.035521988);
    assertThat(nodalCurve.getYValues()).isEqualTo(expectedYValues);
  }

  private void assertUsd3ml(Curve curve) {
    assertThat(curve instanceof InterpolatedNodalCurve).isTrue();
    InterpolatedNodalCurve nodalCurve = (InterpolatedNodalCurve) curve;
    assertThat(nodalCurve.getMetadata().getCurveName()).isEqualTo(CurveName.of("USD-3ML"));

    LocalDate valuationDate = LocalDate.of(2009, 7, 31);
    LocalDate[] nodeDates = new LocalDate[] {
        LocalDate.of(2009, 11, 4),
        LocalDate.of(2010, 8, 4),
        LocalDate.of(2011, 8, 4),
        LocalDate.of(2012, 8, 6),
        LocalDate.of(2014, 8, 5),
        LocalDate.of(2019, 8, 6)
    };
    String[] labels = new String[] {"3M", "1Y", "2Y", "3Y", "5Y", "10Y"};

    for (int i = 0; i < nodalCurve.getXValues().size(); i++) {
      LocalDate nodeDate = nodeDates[i];
      double actualYearFraction = nodalCurve.getXValues().get(i);
      double expectedYearFraction = getYearFraction(valuationDate, nodeDate);
      assertThat(actualYearFraction).isCloseTo(expectedYearFraction, offset(TOLERANCE));

      ParameterMetadata nodeMetadata = nodalCurve.getMetadata().getParameterMetadata().get().get(i);
      assertThat(nodeMetadata.getLabel()).isEqualTo(labels[i]);
    }

    DoubleArray expectedYValues = DoubleArray.of(
        0.007596889,
        0.008091541,
        0.015244398,
        0.021598026,
        0.029984216,
        0.039245812);
    assertThat(nodalCurve.getYValues()).isEqualTo(expectedYValues);
  }

  private double getYearFraction(LocalDate fromDate, LocalDate toDate) {
    return DayCounts.ACT_ACT_ISDA.yearFraction(fromDate, toDate);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_writer_curve_settings() {
    List<RatesCurveGroup> curveGroups = RatesCurvesCsvLoader.load(
        CURVE_DATE,
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2)));
    Appendable underlying = new StringBuilder();
    RatesCurvesCsvLoader.writeCurveSettings(underlying, curveGroups.get(0));
    String created = underlying.toString();
    String expected =
        "Curve Name,Value Type,Day Count,Interpolator,Left Extrapolator,Right Extrapolator" + System.lineSeparator() +
            "USD-Disc,zero,Act/Act ISDA,Linear,Flat,Flat" + System.lineSeparator() +
            "USD-3ML,zero,Act/Act ISDA,Linear,Flat,Flat" + System.lineSeparator();
    assertThat(created).isEqualTo(expected);
  }

  @Test
  public void test_writer_curve_nodes() {
    List<RatesCurveGroup> curveGroups = RatesCurvesCsvLoader.load(
        CURVE_DATE,
        ResourceLocator.of(GROUPS_1),
        ResourceLocator.of(SETTINGS_1),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2)));
    Appendable underlying = new StringBuilder();
    RatesCurvesCsvLoader.writeCurveNodes(underlying, CURVE_DATE, curveGroups.get(0));
    String created = underlying.toString();
    String expected =
        "Valuation Date,Curve Name,Date,Value,Label" + System.lineSeparator() +
            "2009-07-31,USD-Disc,2009-11-06,0.001763775,3M" + System.lineSeparator() +
            "2009-07-31,USD-Disc,2010-02-08,0.002187884,6M" + System.lineSeparator() +
            "2009-07-31,USD-Disc,2010-08-06,0.004437206,1Y" + System.lineSeparator() +
            "2009-07-31,USD-Disc,2011-08-08,0.011476741,2Y" + System.lineSeparator() +
            "2009-07-31,USD-Disc,2012-08-08,0.017859057,3Y" + System.lineSeparator() +
            "2009-07-31,USD-Disc,2014-08-06,0.026257102,5Y" + System.lineSeparator() +
            "2009-07-31,USD-Disc,2019-08-07,0.035521988,10Y" + System.lineSeparator() +
            "2009-07-31,USD-3ML,2009-11-04,0.007596889,3M" + System.lineSeparator() +
            "2009-07-31,USD-3ML,2010-08-04,0.008091541,1Y" + System.lineSeparator() +
            "2009-07-31,USD-3ML,2011-08-04,0.015244398,2Y" + System.lineSeparator() +
            "2009-07-31,USD-3ML,2012-08-06,0.021598026,3Y" + System.lineSeparator() +
            "2009-07-31,USD-3ML,2014-08-05,0.029984216,5Y" + System.lineSeparator() +
            "2009-07-31,USD-3ML,2019-08-06,0.039245812,10Y" + System.lineSeparator();
    assertThat(created).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(RatesCurvesCsvLoader.class);
    LoadedCurveKey.meta();
    coverImmutableBean(LoadedCurveKey.of(CURVE_DATE, CurveName.of("Test")));
    LoadedCurveNode.meta();
    coverImmutableBean(LoadedCurveNode.of(CURVE_DATE, 1d, "Test"));
    LoadedCurveSettings.meta();
    LoadedCurveSettings settings1 = LoadedCurveSettings.of(
        CurveName.of("Test"), ValueType.YEAR_FRACTION, ValueType.ZERO_RATE, DayCounts.ACT_365F,
        CurveInterpolators.LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
    LoadedCurveSettings settings2 = LoadedCurveSettings.of(
        CurveName.of("Test2"), ValueType.YEAR_FRACTION, ValueType.DISCOUNT_FACTOR, DayCounts.ACT_ACT_ISDA,
        CurveInterpolators.LOG_LINEAR, CurveExtrapolators.LINEAR, CurveExtrapolators.LINEAR);
    coverImmutableBean(settings1);
    coverBeanEquals(settings1, settings2);
  }

}
