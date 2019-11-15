/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.STEP_UPPER;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.TIME_SQUARE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.option.SimpleStrike;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfacePeriodParameterMetadata;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.IborRateCalculation;

/**
 * Test {@link SurfaceIborCapletFloorletVolatilityBootstrapDefinition}.
 */
public class SurfaceIborCapletFloorletVolatilityBootstrapDefinitionTest {

  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("TestName");
  private static final ConstantCurve SHIFT = ConstantCurve.of("Black shift", 0.02);

  @Test
  public void test_of() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition test = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, STEP_UPPER, DOUBLE_QUADRATIC);
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(GridSurfaceInterpolator.of(STEP_UPPER, DOUBLE_QUADRATIC));
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getShiftCurve().isPresent()).isFalse();
  }

  @Test
  public void test_of_surface() {
    GridSurfaceInterpolator interp = GridSurfaceInterpolator.of(LINEAR, LINEAR);
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition test =
        SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(NAME, USD_LIBOR_3M, ACT_ACT_ISDA, interp);
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(interp);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getShiftCurve().isPresent()).isFalse();
  }

  @Test
  public void test_of_shift() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition test = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, STEP_UPPER, DOUBLE_QUADRATIC, SHIFT);
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(GridSurfaceInterpolator.of(STEP_UPPER, DOUBLE_QUADRATIC));
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getShiftCurve().get()).isEqualTo(SHIFT);
  }

  @Test
  public void test_of_surface_shift() {
    GridSurfaceInterpolator interp = GridSurfaceInterpolator.of(LINEAR, LINEAR);
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition test =
        SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(NAME, USD_LIBOR_3M, ACT_ACT_ISDA, interp, SHIFT);
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(interp);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getShiftCurve().get()).isEqualTo(SHIFT);
  }

  @Test
  public void test_createCap() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition base = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, TIME_SQUARE, DOUBLE_QUADRATIC);
    LocalDate startDate = LocalDate.of(2012, 4, 20);
    LocalDate endDate = LocalDate.of(2017, 4, 20);
    double strike = 0.01;
    IborCapFloorLeg expected = IborCapFloorLeg.builder()
        .calculation(IborRateCalculation.of(USD_LIBOR_3M))
        .capSchedule(ValueSchedule.of(strike))
        .currency(USD_LIBOR_3M.getCurrency())
        .notional(ValueSchedule.ALWAYS_1)
        .paymentDateOffset(DaysAdjustment.NONE)
        .paymentSchedule(
            PeriodicSchedule.of(
                startDate,
                endDate,
                Frequency.of(USD_LIBOR_3M.getTenor().getPeriod()),
                BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, USD_LIBOR_3M.getFixingCalendar()),
                StubConvention.NONE,
                RollConventions.NONE))
        .payReceive(PayReceive.RECEIVE)
        .build();
    IborCapFloorLeg computed = base.createCap(startDate, endDate, strike);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_createMetadata_normal() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition base = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, DOUBLE_QUADRATIC);
    RawOptionData capData = RawOptionData.of(
        ImmutableList.of(Period.ofYears(1), Period.ofYears(5)),
        DoubleArray.of(0.005, 0.01, 0.015),
        ValueType.STRIKE,
        DoubleMatrix.copyOf(new double[][] {{0.15, 0.12, 0.13}, {0.1, Double.NaN, 0.09}}),
        ValueType.NORMAL_VOLATILITY);
    List<GenericVolatilitySurfacePeriodParameterMetadata> list = new ArrayList<>();
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(1), SimpleStrike.of(0.005)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(1), SimpleStrike.of(0.01)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(1), SimpleStrike.of(0.015)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(5), SimpleStrike.of(0.005)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(5), SimpleStrike.of(0.015)));
    SurfaceMetadata expected = Surfaces.normalVolatilityByExpiryStrike(
        NAME.getName(), ACT_ACT_ISDA).withParameterMetadata(list);
    SurfaceMetadata computed = base.createMetadata(capData);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_createMetadata_black() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition base = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, DOUBLE_QUADRATIC);
    RawOptionData capData = RawOptionData.of(
        ImmutableList.of(Period.ofYears(1), Period.ofYears(5)),
        DoubleArray.of(0.005, 0.01, 0.015),
        ValueType.STRIKE,
        DoubleMatrix.copyOf(new double[][] {{0.15, 0.12, 0.13}, {0.1, 0.08, 0.09}}),
        ValueType.BLACK_VOLATILITY);
    List<GenericVolatilitySurfacePeriodParameterMetadata> list = new ArrayList<>();
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(1), SimpleStrike.of(0.005)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(1), SimpleStrike.of(0.01)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(1), SimpleStrike.of(0.015)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(5), SimpleStrike.of(0.005)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(5), SimpleStrike.of(0.01)));
    list.add(GenericVolatilitySurfacePeriodParameterMetadata.of(Period.ofYears(5), SimpleStrike.of(0.015)));
    SurfaceMetadata expected = Surfaces.blackVolatilityByExpiryStrike(
        NAME.getName(), ACT_ACT_ISDA).withParameterMetadata(list);
    SurfaceMetadata computed = base.createMetadata(capData);
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_wrongInterpolator() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, DOUBLE_QUADRATIC, DOUBLE_QUADRATIC));

  }
  @Test
  public void test_createMetadata_wrongValueType() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition base = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, DOUBLE_QUADRATIC);
    RawOptionData capData = RawOptionData.of(
        ImmutableList.of(Period.ofYears(1), Period.ofYears(5)),
        DoubleArray.of(0.005, 0.01, 0.015),
        ValueType.STRIKE,
        DoubleMatrix.copyOf(new double[][] {{0.15, 0.12, 0.13}, {0.1, 0.08, 0.09}}),
        ValueType.PRICE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.createMetadata(capData));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition test1 = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, DOUBLE_QUADRATIC);
    coverImmutableBean(test1);
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition test2 = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("other"), GBP_LIBOR_3M, ACT_365F, LINEAR, LINEAR, SHIFT);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition test = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, DOUBLE_QUADRATIC);
    assertSerialization(test);
  }

}
