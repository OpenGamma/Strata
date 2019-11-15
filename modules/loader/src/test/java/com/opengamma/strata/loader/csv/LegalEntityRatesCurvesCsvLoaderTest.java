/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.LegalEntityCurveGroup;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;

/**
 * Test {@link LegalEntityRatesCurvesCsvLoader}.
 */
public class LegalEntityRatesCurvesCsvLoaderTest {

  private static final String GROUPS = "classpath:com/opengamma/strata/loader/csv/legal-entity-groups.csv";
  private static final String SETTINGS = "classpath:com/opengamma/strata/loader/csv/legal-entity-settings.csv";
  private static final String CURVES_1 = "classpath:com/opengamma/strata/loader/csv/legal-entity-curves-1.csv";
  private static final String CURVES_2 = "classpath:com/opengamma/strata/loader/csv/legal-entity-curves-2.csv";

  private static final String GROUPS_INVALID_CURVE_TYPE =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-groups-invalid-curve-type.csv";
  private static final String SETTINGS_INVALID_DCC =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-settings-invalid-day-count.csv";
  private static final String SETTINGS_INVALID_INTERP =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-settings-invalid-interpolator.csv";
  private static final String SETTINGS_INVALID_LEFT =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-settings-invalid-left-extrapolator.csv";
  private static final String SETTINGS_INVALID_RIGHT =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-settings-invalid-right-extrapolator.csv";
  private static final String SETTINGS_INVALID_VALUE =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-settings-invalid-curve-type.csv";
  private static final String SETTINGS_MISSING_CURVE_NAME =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-settings-missing-header.csv";
  private static final String CURVES_2_REPO_MISSING =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-curves-2-missing-repo.csv";
  private static final String CURVES_2_ISSUER_MISSING =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-curves-2-missing-issuer.csv";
  private static final String CURVES_1_DEUPLICATE_POINTS =
      "classpath:com/opengamma/strata/loader/csv/legal-entity-curves-1-duplicate-points.csv";

  private static final List<LocalDate> ALL_DATES = ImmutableList.copyOf(new LocalDate[] {
      LocalDate.of(2017, 4, 24), LocalDate.of(2017, 4, 23), LocalDate.of(2017, 4, 22), LocalDate.of(2017, 4, 21),
      LocalDate.of(2017, 4, 20),
      LocalDate.of(2017, 4, 19), LocalDate.of(2017, 4, 18), LocalDate.of(2017, 4, 17), LocalDate.of(2017, 4, 16),
      LocalDate.of(2017, 4, 15),
      LocalDate.of(2017, 4, 14), LocalDate.of(2017, 4, 13), LocalDate.of(2017, 4, 12), LocalDate.of(2017, 4, 11),
      LocalDate.of(2017, 4, 10),
      LocalDate.of(2017, 4, 9), LocalDate.of(2017, 4, 8), LocalDate.of(2017, 4, 7), LocalDate.of(2017, 4, 6)});

  @Test
  public void test_loadAllDates() {
    LocalDate sampleDate = ALL_DATES.get(3); // 2017-04-21
    ImmutableList<LocalDate> expDates = ImmutableList.of(
        LocalDate.of(2017, 7, 21), LocalDate.of(2017, 10, 7), LocalDate.of(2018, 4, 13), LocalDate.of(2019, 4, 12),
        LocalDate.of(2020, 3, 20), LocalDate.of(2021, 3, 19), LocalDate.of(2022, 3, 19), LocalDate.of(2023, 3, 17),
        LocalDate.of(2024, 6, 17), LocalDate.of(2025, 3, 18), LocalDate.of(2026, 3, 20), LocalDate.of(2027, 3, 20),
        LocalDate.of(2031, 12, 19), LocalDate.of(2037, 3, 17), LocalDate.of(2047, 3, 17), LocalDate.of(2056, 3, 17));
    ImmutableList<String> expTenors = ImmutableList.of(
        "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "30Y", "40Y");
    RepoGroup repoGroup = RepoGroup.of("JP-REPO");
    DoubleArray expRepoXValues = DoubleArray.of(3, n -> ACT_365F.relativeYearFraction(sampleDate, expDates.get(n)));
    DoubleArray expRepoYValues = DoubleArray.of(-0.0019521, -0.0016021, -0.0022521);
    ImmutableList<LabelDateParameterMetadata> expRepoMetadata = IntStream.range(0, 3)
        .mapToObj(n -> LabelDateParameterMetadata.of(expDates.get(n), expTenors.get(n)))
        .collect(Guavate.toImmutableList());
    LegalEntityGroup legalEntityGroup = LegalEntityGroup.of("JP-GOVT");
    DoubleArray expIssuerXValues =
        DoubleArray.of(expDates.size(), n -> ACT_365F.relativeYearFraction(sampleDate, expDates.get(n)));
    DoubleArray expIssuerYValues = DoubleArray.of(
        -0.0019511690511744527, -0.001497422302092893, -0.0021798583657932176, -0.002215700360912938, -0.0021722324679574866,
        -0.001922059591219172, -0.0015461646763548528, -0.0014835851245462084, -0.001118669580570464, -5.476767138782941E-4,
        -2.2155596172855965E-4, 2.0333291172821893E-5, 0.00284500423293463, 0.005876533417933958, 0.007957581583531789,
        0.009134630405512047);
    ImmutableList<LabelDateParameterMetadata> expIssuerMetadata = IntStream.range(0, expDates.size())
        .mapToObj(n -> LabelDateParameterMetadata.of(expDates.get(n), expTenors.get(n)))
        .collect(Guavate.toImmutableList());

    ImmutableListMultimap<LocalDate, LegalEntityCurveGroup> allCurves = LegalEntityRatesCurvesCsvLoader.loadAllDates(
        ResourceLocator.of(GROUPS),
        ResourceLocator.of(SETTINGS),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2)));
    assertThat(allCurves.keySet().containsAll(ALL_DATES)).isTrue();
    ImmutableList<LegalEntityCurveGroup> groups = allCurves.get(sampleDate);
    assertThat(groups).hasSize(2);
    // group 0
    LegalEntityCurveGroup group0 = groups.get(0);
    assertThat(group0.getName()).isEqualTo(CurveGroupName.of("Default1"));
    // repo
    assertThat(group0.getRepoCurves()).hasSize(1);
    Curve repoCurve = group0.getRepoCurves().get(Pair.of(repoGroup, JPY));
    InterpolatedNodalCurve expectedRepoCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("JP-REPO-1"), ACT_365F, expRepoMetadata),
        expRepoXValues, expRepoYValues, CurveInterpolators.LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
    assertThat(repoCurve).isEqualTo(expectedRepoCurve);
    // issuer
    assertThat(group0.getIssuerCurves()).hasSize(2);
    Curve issuerCurve = group0.getIssuerCurves().get(Pair.of(legalEntityGroup, JPY));
    InterpolatedNodalCurve expectedIssuerCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("JP-GOVT-1"), ACT_365F, expIssuerMetadata),
        expIssuerXValues, expIssuerYValues, CurveInterpolators.LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
    assertThat(issuerCurve).isEqualTo(expectedIssuerCurve);
    Curve usIssuerCurve = group0.getIssuerCurves().get(Pair.of(LegalEntityGroup.of("US-GOVT"), USD));
    expectedIssuerCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("US-GOVT"), ACT_360, expIssuerMetadata),
        DoubleArray.of(expDates.size(), n -> ACT_360.relativeYearFraction(sampleDate, expDates.get(n))),
        expIssuerYValues, CurveInterpolators.NATURAL_SPLINE, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
    assertThat(usIssuerCurve).isEqualTo(expectedIssuerCurve);
    // group 1
    LegalEntityCurveGroup group1 = groups.get(1);
    assertThat(group1.getName()).isEqualTo(CurveGroupName.of("Default2"));
    // repo
    repoCurve = group1.getRepoCurves().get(Pair.of(repoGroup, JPY));
    expectedRepoCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("JP-REPO-2"), ACT_365F, expRepoMetadata),
        expRepoXValues, expRepoYValues, CurveInterpolators.DOUBLE_QUADRATIC, CurveExtrapolators.LINEAR,
        CurveExtrapolators.LINEAR);
    assertThat(repoCurve).isEqualTo(expectedRepoCurve);
    // issuer
    assertThat(group1.getIssuerCurves()).hasSize(1);
    issuerCurve = group1.getIssuerCurves().get(Pair.of(legalEntityGroup, JPY));
    expectedIssuerCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(CurveName.of("JP-GOVT-2"), ACT_365F, expIssuerMetadata),
        expIssuerXValues, expIssuerYValues, CurveInterpolators.DOUBLE_QUADRATIC, CurveExtrapolators.LINEAR,
        CurveExtrapolators.LINEAR);
    assertThat(issuerCurve).isEqualTo(expectedIssuerCurve);
  }

  @Test
  public void test_load() {
    ImmutableListMultimap<LocalDate, LegalEntityCurveGroup> allCurves = LegalEntityRatesCurvesCsvLoader.loadAllDates(
        ResourceLocator.of(GROUPS),
        ResourceLocator.of(SETTINGS),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2)));
    for (LocalDate date : ALL_DATES) {
      ImmutableList<LegalEntityCurveGroup> oneDayCurves = LegalEntityRatesCurvesCsvLoader.load(
          date,
          ResourceLocator.of(GROUPS),
          ResourceLocator.of(SETTINGS),
          ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2)));
      assertThat(oneDayCurves).isEqualTo(allCurves.get(date));
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_setting_invalid_path() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(4),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of("classpath:invalid"),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))));
  }

  @Test
  public void test_invalid_settings_missing_column_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(6),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS_MISSING_CURVE_NAME),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))))
        .withMessage("Header not found: 'Curve Name'");
  }

  @Test
  public void test_invalid_settings_day_count_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(1),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS_INVALID_DCC),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))))
        .withMessageMatching("Unknown DayCount value.*");
  }

  @Test
  public void test_invalid_settings_interpolator_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(6),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS_INVALID_INTERP),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))))
        .withMessage("CurveInterpolator name not found: Polynomial");
  }

  @Test
  public void test_invalid_settings_left_extrapolator_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(2),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS_INVALID_LEFT),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))))
        .withMessage("CurveExtrapolator name not found: Polynomial");
  }

  @Test
  public void test_invalid_settings_right_extrapolator_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(4),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS_INVALID_RIGHT),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))))
        .withMessage("CurveExtrapolator name not found: Cubic");
  }

  @Test
  public void test_invalid_settings_value_type_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(0),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS_INVALID_VALUE),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))))
        .withMessage("Unsupported Value Type in curve settings: Forward");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_missing_groups_file() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(6),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of("classpath:invalid"),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))));
  }

  @Test
  public void test_invalid_curve_type() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(6),
            ResourceLocator.of(GROUPS_INVALID_CURVE_TYPE),
            ResourceLocator.of(SETTINGS),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))))
        .withMessage("Unsupported curve type: Forward");
  }

  @Test
  public void test_missing_issuer_curve() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(2),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2_ISSUER_MISSING))))
        .withMessageMatching("Issuer curve values .*");
  }

  @Test
  public void test_missing_repo_curve() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(5),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2_REPO_MISSING))))
        .withMessageMatching("Repo curve values .*");
  }

  @Test
  public void test_date_outside_range() {
    ImmutableList<LegalEntityCurveGroup> result = LegalEntityRatesCurvesCsvLoader.load(
        LocalDate.of(2017, 1, 24),
        ResourceLocator.of(GROUPS),
        ResourceLocator.of(SETTINGS),
        ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2)));
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  public void test_multiple_curves() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.load(
            ALL_DATES.get(6),
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS),
            ImmutableList.of(ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_1), ResourceLocator.of(CURVES_2))))
        .withMessageMatching("Rates curve loader found multiple curves with the same name: .*");
  }

  @Test
  public void test_invalid_curve_duplicate_points() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LegalEntityRatesCurvesCsvLoader.loadAllDates(
            ResourceLocator.of(GROUPS),
            ResourceLocator.of(SETTINGS),
            ImmutableList.of(ResourceLocator.of(CURVES_1_DEUPLICATE_POINTS), ResourceLocator.of(CURVES_2))));
  }

}
