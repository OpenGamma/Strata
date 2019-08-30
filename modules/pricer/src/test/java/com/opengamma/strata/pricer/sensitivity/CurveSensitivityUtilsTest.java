/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;

/**
 * Tests {@link CurveSensitivityUtils}.
 */
public class CurveSensitivityUtilsTest {

  private static final CurveName NAME_1 = CurveName.of("CURVE 1");
  private static final Currency CCY_1 = Currency.EUR;
  private static final CurveName NAME_2 = CurveName.of("CURVE 2");
  private static final Currency CCY_2 = Currency.USD;
  private static final List<LocalDate> TARGET_DATES = new ArrayList<>();
  static {
    TARGET_DATES.add(LocalDate.of(2016, 8, 18));
    TARGET_DATES.add(LocalDate.of(2020, 1, 5));
    TARGET_DATES.add(LocalDate.of(2025, 12, 20));
    TARGET_DATES.add(LocalDate.of(2045, 7, 4));
  }
  private static final List<LocalDate> SENSITIVITY_DATES = new ArrayList<>();
  static {
    SENSITIVITY_DATES.add(LocalDate.of(2016, 8, 17));
    SENSITIVITY_DATES.add(LocalDate.of(2016, 8, 18));
    SENSITIVITY_DATES.add(LocalDate.of(2016, 8, 19));
    SENSITIVITY_DATES.add(LocalDate.of(2019, 1, 5));
    SENSITIVITY_DATES.add(LocalDate.of(2020, 1, 5));
    SENSITIVITY_DATES.add(LocalDate.of(2021, 1, 5));
    SENSITIVITY_DATES.add(LocalDate.of(2024, 12, 25));
    SENSITIVITY_DATES.add(LocalDate.of(2025, 12, 20));
    SENSITIVITY_DATES.add(LocalDate.of(2026, 12, 15));
    SENSITIVITY_DATES.add(LocalDate.of(2045, 7, 4));
    SENSITIVITY_DATES.add(LocalDate.of(2055, 7, 4));
  }
  private static final double SENSITIVITY_AMOUNT = 123.45;
  private static final double[] WEIGHTS_HC =
  {1.0, 1.0, 0.999190283, 0.295546559, 0.0, 0.831801471, 0.165441176, 1.0, 0.94955157, 0.0, 0.0 };
  // weights externally provided and hard-coded here
  private static final int[] WEIGHTS_START = {0, 0, 0, 0, 0, 1, 1, 2, 2, 2, 2 };
  private static final double TOLERANCE_SENSI = 1.0E-5;

  @Test
  public void hard_coded_value_one_curve_one_date_dated() {
    Function<LocalDate, ParameterMetadata> parameterMetadataFunction =
        (d) -> LabelDateParameterMetadata.of(d, "test");
    Function<CurrencyParameterSensitivities, CurrencyParameterSensitivities> rebucketFunction =
        (s) -> CurveSensitivityUtils.linearRebucketing(s, TARGET_DATES);
    test_from_functions_one_curve_one_date(parameterMetadataFunction, rebucketFunction);
  }

  @Test
  public void hard_coded_value_one_curve_one_date_tenor() {
    final LocalDate sensitivityDate = LocalDate.of(2015, 8, 18);
    Function<LocalDate, ParameterMetadata> parameterMetadataFunction =
        (d) -> TenorParameterMetadata.of(Tenor
            .of(Period.ofDays((int) (d.toEpochDay() - sensitivityDate.toEpochDay()))));
    Function<CurrencyParameterSensitivities, CurrencyParameterSensitivities> rebucketFunction =
        (s) -> CurveSensitivityUtils.linearRebucketing(s, TARGET_DATES, sensitivityDate);
    test_from_functions_one_curve_one_date(parameterMetadataFunction, rebucketFunction);
  }

  @Test
  public void hard_coded_value_one_curve_one_date_dated_sd() {
    final LocalDate sensitivityDate = LocalDate.of(2015, 8, 18);
    Function<LocalDate, ParameterMetadata> parameterMetadataFunction =
        (d) -> LabelDateParameterMetadata.of(d, "test");
    Function<CurrencyParameterSensitivities, CurrencyParameterSensitivities> rebucketFunction =
        (s) -> CurveSensitivityUtils.linearRebucketing(s, TARGET_DATES, sensitivityDate);
    test_from_functions_one_curve_one_date(parameterMetadataFunction, rebucketFunction);
  }

  private void test_from_functions_one_curve_one_date(
      Function<LocalDate, ParameterMetadata> parameterMetadataFunction,
      Function<CurrencyParameterSensitivities, CurrencyParameterSensitivities> rebucketFunction) {
    for (int loopdate = 0; loopdate < SENSITIVITY_DATES.size(); loopdate++) {
      List<ParameterMetadata> pmdInput = new ArrayList<>();
      pmdInput.add(parameterMetadataFunction.apply(SENSITIVITY_DATES.get(loopdate)));
      CurrencyParameterSensitivity s =
          CurrencyParameterSensitivity.of(NAME_1, pmdInput, CCY_1, DoubleArray.of(SENSITIVITY_AMOUNT));
      CurrencyParameterSensitivities s2 = CurrencyParameterSensitivities.of(s);
      CurrencyParameterSensitivities sTarget = rebucketFunction.apply(s2);
      assertThat(sTarget.getSensitivities().size() == 1).isTrue();
      CurrencyParameterSensitivity sTarget1 = sTarget.getSensitivities().get(0);
      assertThat(sTarget1.getMarketDataName().equals(NAME_1)).isTrue();
      assertThat(sTarget1.getCurrency().equals(CCY_1)).isTrue();
      assertThat(sTarget1.getSensitivity().size() == TARGET_DATES.size()).isTrue();
      assertThat(sTarget1.getSensitivity().get(WEIGHTS_START[loopdate])).isCloseTo(WEIGHTS_HC[loopdate] * SENSITIVITY_AMOUNT, offset(TOLERANCE_SENSI));
      assertThat(sTarget1.getSensitivity().get(WEIGHTS_START[loopdate] + 1)).isCloseTo((1.0d - WEIGHTS_HC[loopdate]) * SENSITIVITY_AMOUNT, offset(TOLERANCE_SENSI));
    }
  }

  @Test
  public void hard_coded_value_one_curve_all_dates() {
    Function<LocalDate, ParameterMetadata> parameterMetadataFunction =
        (d) -> LabelDateParameterMetadata.of(d, "test");
    Function<CurrencyParameterSensitivities, CurrencyParameterSensitivities> rebucketFunction =
        (s) -> CurveSensitivityUtils.linearRebucketing(s, TARGET_DATES);
    test_from_functions_one_curve_all_dates(parameterMetadataFunction, rebucketFunction);
  }

  @Test
  public void hard_coded_value_one_curve_all_dates_tenor() {
    final LocalDate sensitivityDate = LocalDate.of(2015, 8, 18);
    Function<LocalDate, ParameterMetadata> parameterMetadataFunction =
        (d) -> TenorParameterMetadata.of(Tenor
            .of(Period.ofDays((int) (d.toEpochDay() - sensitivityDate.toEpochDay()))));
    Function<CurrencyParameterSensitivities, CurrencyParameterSensitivities> rebucketFunction =
        (s) -> CurveSensitivityUtils.linearRebucketing(s, TARGET_DATES, sensitivityDate);
    test_from_functions_one_curve_all_dates(parameterMetadataFunction, rebucketFunction);
  }

  @Test
  public void hard_coded_value_one_curve_all_dates_dated_sd() {
    final LocalDate sensitivityDate = LocalDate.of(2015, 8, 18);
    Function<LocalDate, ParameterMetadata> parameterMetadataFunction =
        (d) -> LabelDateParameterMetadata.of(d, "test");
    Function<CurrencyParameterSensitivities, CurrencyParameterSensitivities> rebucketFunction =
        (s) -> CurveSensitivityUtils.linearRebucketing(s, TARGET_DATES, sensitivityDate);
    test_from_functions_one_curve_all_dates(parameterMetadataFunction, rebucketFunction);
  }

  private void test_from_functions_one_curve_all_dates(
      Function<LocalDate, ParameterMetadata> parameterMetadataFunction,
      Function<CurrencyParameterSensitivities, CurrencyParameterSensitivities> rebucketFunction) {
    List<ParameterMetadata> pmdInput = new ArrayList<>();
    double[] sensiExpected = new double[TARGET_DATES.size()];
    for (int loopdate = 0; loopdate < SENSITIVITY_DATES.size(); loopdate++) {
      pmdInput.add(parameterMetadataFunction.apply(SENSITIVITY_DATES.get(loopdate)));
      sensiExpected[WEIGHTS_START[loopdate]] += WEIGHTS_HC[loopdate] * SENSITIVITY_AMOUNT;
      sensiExpected[WEIGHTS_START[loopdate] + 1] += (1.0d - WEIGHTS_HC[loopdate]) * SENSITIVITY_AMOUNT;
    }
    DoubleArray sens = DoubleArray.of(SENSITIVITY_DATES.size(), (d) -> SENSITIVITY_AMOUNT);
    CurrencyParameterSensitivity s = CurrencyParameterSensitivity.of(NAME_1, pmdInput, CCY_1, sens);
    CurrencyParameterSensitivities s2 = CurrencyParameterSensitivities.of(s);
    CurrencyParameterSensitivities sTarget = rebucketFunction.apply(s2);
    assertThat(sTarget.getSensitivities().size() == 1).isTrue();
    CurrencyParameterSensitivity sTarget1 = sTarget.getSensitivities().get(0);
    assertThat(sTarget1.getMarketDataName().equals(NAME_1)).isTrue();
    assertThat(sTarget1.getCurrency().equals(CCY_1)).isTrue();
    assertThat(sTarget1.getSensitivity().size() == TARGET_DATES.size()).isTrue();
    for (int looptarget = 0; looptarget < TARGET_DATES.size(); looptarget++) {
      assertThat(sTarget1.getSensitivity().get(looptarget)).isCloseTo(sensiExpected[looptarget], offset(TOLERANCE_SENSI));
    }
  }

  @Test
  public void hard_coded_value_two_curves_one_date() {
    for (int loopdate = 0; loopdate < SENSITIVITY_DATES.size() - 1; loopdate++) {
      List<ParameterMetadata> pmdInput1 = new ArrayList<>();
      pmdInput1.add(LabelDateParameterMetadata.of(SENSITIVITY_DATES.get(loopdate), "test"));
      CurrencyParameterSensitivity s1 =
          CurrencyParameterSensitivity.of(NAME_1, pmdInput1, CCY_1, DoubleArray.of(SENSITIVITY_AMOUNT));
      List<ParameterMetadata> pmdInput2 = new ArrayList<>();
      pmdInput2.add(LabelDateParameterMetadata.of(SENSITIVITY_DATES.get(loopdate + 1), "test"));
      CurrencyParameterSensitivity s2 =
          CurrencyParameterSensitivity.of(NAME_2, pmdInput2, CCY_2, DoubleArray.of(SENSITIVITY_AMOUNT));
      CurrencyParameterSensitivities sList = CurrencyParameterSensitivities.of(s1, s2);
      CurrencyParameterSensitivities sTarget = CurveSensitivityUtils.linearRebucketing(sList, TARGET_DATES);
      assertThat(sTarget.getSensitivities().size() == 2).isTrue();
      CurrencyParameterSensitivity sTarget1 = sTarget.getSensitivities().get(0);
      assertThat(sTarget1.getMarketDataName().equals(NAME_1)).isTrue();
      assertThat(sTarget1.getCurrency().equals(CCY_1)).isTrue();
      assertThat(sTarget1.getSensitivity().size() == TARGET_DATES.size()).isTrue();
      assertThat(sTarget1.getSensitivity().get(WEIGHTS_START[loopdate])).isCloseTo(WEIGHTS_HC[loopdate] * SENSITIVITY_AMOUNT, offset(TOLERANCE_SENSI));
      assertThat(sTarget1.getSensitivity().get(WEIGHTS_START[loopdate] + 1)).isCloseTo((1.0d - WEIGHTS_HC[loopdate]) * SENSITIVITY_AMOUNT, offset(TOLERANCE_SENSI));
      CurrencyParameterSensitivity sTarget2 = sTarget.getSensitivities().get(1);
      assertThat(sTarget2.getMarketDataName().equals(NAME_2)).isTrue();
      assertThat(sTarget2.getCurrency().equals(CCY_2)).isTrue();
      assertThat(sTarget2.getSensitivity().size() == TARGET_DATES.size()).isTrue();
      assertThat(sTarget2.getSensitivity().get(WEIGHTS_START[loopdate + 1])).isCloseTo(WEIGHTS_HC[loopdate + 1] * SENSITIVITY_AMOUNT, offset(TOLERANCE_SENSI));
      assertThat(sTarget2.getSensitivity().get(WEIGHTS_START[loopdate + 1] + 1)).isCloseTo((1.0d - WEIGHTS_HC[loopdate + 1]) * SENSITIVITY_AMOUNT, offset(TOLERANCE_SENSI));
    }
  }

  @Test
  public void missing_metadata() {
    CurrencyParameterSensitivity s1 =
        CurrencyParameterSensitivity.of(NAME_1, CCY_1, DoubleArray.of(SENSITIVITY_AMOUNT));
    CurrencyParameterSensitivities s2 = CurrencyParameterSensitivities.of(s1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveSensitivityUtils.linearRebucketing(s2, TARGET_DATES));
    final LocalDate sensitivityDate = LocalDate.of(2015, 8, 18);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveSensitivityUtils.linearRebucketing(s2, TARGET_DATES, sensitivityDate));
  }

  @Test
  public void wrong_metadata() {
    List<ParameterMetadata> pmdInput = new ArrayList<>();
    pmdInput.add(TenorParameterMetadata.of(Tenor.TENOR_10M));
    CurrencyParameterSensitivity s1 =
        CurrencyParameterSensitivity.of(NAME_1, pmdInput, CCY_1, DoubleArray.of(SENSITIVITY_AMOUNT));
    CurrencyParameterSensitivities s2 = CurrencyParameterSensitivities.of(s1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveSensitivityUtils.linearRebucketing(s2, TARGET_DATES));
  }

}
