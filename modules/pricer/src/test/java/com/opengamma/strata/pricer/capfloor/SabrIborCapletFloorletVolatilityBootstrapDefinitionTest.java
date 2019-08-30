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
import static com.opengamma.strata.market.ValueType.SABR_ALPHA;
import static com.opengamma.strata.market.ValueType.SABR_BETA;
import static com.opengamma.strata.market.ValueType.SABR_NU;
import static com.opengamma.strata.market.ValueType.SABR_RHO;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.STEP_UPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;

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
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.IborRateCalculation;

/**
 * Test {@link SabrIborCapletFloorletVolatilityBootstrapDefinition}.
 */
public class SabrIborCapletFloorletVolatilityBootstrapDefinitionTest {

  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("TestName");

  @Test
  public void test_ofFixedBeta() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition test = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(LINEAR);
    assertThat(test.getExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getExtrapolatorRight()).isEqualTo(FLAT);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getBetaCurve().get()).isEqualTo(ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_ACT_ISDA, SABR_BETA), 0.5));
    assertThat(test.getRhoCurve().isPresent()).isFalse();
    assertThat(test.getSabrVolatilityFormula()).isEqualTo(SabrVolatilityFormula.hagan());
    assertThat(test.getShiftCurve()).isEqualTo(ConstantCurve.of("Zero shift", 0d));
  }

  @Test
  public void test_ofFixedBeta_shift() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition test = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, 0.01, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(LINEAR);
    assertThat(test.getExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getExtrapolatorRight()).isEqualTo(FLAT);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getBetaCurve().get()).isEqualTo(ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_ACT_ISDA, SABR_BETA), 0.5));
    assertThat(test.getRhoCurve().isPresent()).isFalse();
    assertThat(test.getSabrVolatilityFormula()).isEqualTo(SabrVolatilityFormula.hagan());
    assertThat(test.getShiftCurve()).isEqualTo(ConstantCurve.of("Shift curve", 0.01));
  }

  @Test
  public void test_ofFixedRho() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition test = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedRho(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(LINEAR);
    assertThat(test.getExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getExtrapolatorRight()).isEqualTo(FLAT);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getRhoCurve().get()).isEqualTo(ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Rho", ACT_ACT_ISDA, SABR_RHO), 0.5));
    assertThat(test.getBetaCurve().isPresent()).isFalse();
    assertThat(test.getSabrVolatilityFormula()).isEqualTo(SabrVolatilityFormula.hagan());
    assertThat(test.getShiftCurve()).isEqualTo(ConstantCurve.of("Zero shift", 0d));
  }

  @Test
  public void test_ofFixedRho_shift() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition test = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedRho(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, 0.01, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(LINEAR);
    assertThat(test.getExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getExtrapolatorRight()).isEqualTo(FLAT);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getRhoCurve().get()).isEqualTo(ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Rho", ACT_ACT_ISDA, SABR_RHO), 0.5));
    assertThat(test.getBetaCurve().isPresent()).isFalse();
    assertThat(test.getSabrVolatilityFormula()).isEqualTo(SabrVolatilityFormula.hagan());
    assertThat(test.getShiftCurve()).isEqualTo(ConstantCurve.of("Shift curve", 0.01));
  }

  @Test
  public void test_builder() {
    Curve betaCurve = ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_ACT_ISDA, SABR_BETA), 0.65);
    SabrIborCapletFloorletVolatilityBootstrapDefinition test = SabrIborCapletFloorletVolatilityBootstrapDefinition.builder()
        .index(USD_LIBOR_3M)
        .name(NAME)
        .interpolator(LINEAR)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(CurveExtrapolators.LINEAR)
        .dayCount(ACT_ACT_ISDA)
        .sabrVolatilityFormula(SabrVolatilityFormula.hagan())
        .betaCurve(betaCurve)
        .build();
    assertThat(test.getDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(test.getInterpolator()).isEqualTo(LINEAR);
    assertThat(test.getExtrapolatorLeft()).isEqualTo(FLAT);
    assertThat(test.getExtrapolatorRight()).isEqualTo(CurveExtrapolators.LINEAR);
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getBetaCurve().get()).isEqualTo(betaCurve);
    assertThat(test.getRhoCurve().isPresent()).isFalse();
    assertThat(test.getSabrVolatilityFormula()).isEqualTo(SabrVolatilityFormula.hagan());
    assertThat(test.getShiftCurve()).isEqualTo(ConstantCurve.of("Zero shift", 0d));
  }

  @Test
  public void test_createCap() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition base = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, STEP_UPPER, FLAT, FLAT, SabrVolatilityFormula.hagan());
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
  public void test_createSabrParameterMetadata() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition base = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    ImmutableList<CurveMetadata> expected = ImmutableList.of(
        Curves.sabrParameterByExpiry(NAME.getName() + "-Alpha", ACT_ACT_ISDA, SABR_ALPHA),
        Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_ACT_ISDA, SABR_BETA),
        Curves.sabrParameterByExpiry(NAME.getName() + "-Rho", ACT_ACT_ISDA, SABR_RHO),
        Curves.sabrParameterByExpiry(NAME.getName() + "-Nu", ACT_ACT_ISDA, SABR_NU));
    ImmutableList<CurveMetadata> computed = base.createSabrParameterMetadata();
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_createMetadata_normal() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition base = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    RawOptionData capData = RawOptionData.of(
        ImmutableList.of(Period.ofYears(1), Period.ofYears(5)),
        DoubleArray.of(0.005, 0.01, 0.015),
        ValueType.STRIKE,
        DoubleMatrix.copyOf(new double[][] {{0.15, 0.12, 0.13}, {0.1, Double.NaN, 0.09}}),
        ValueType.NORMAL_VOLATILITY);
    SurfaceMetadata expected = Surfaces.normalVolatilityByExpiryStrike(NAME.getName(), ACT_ACT_ISDA);
    SurfaceMetadata computed = base.createMetadata(capData);
    assertThat(computed).isEqualTo(expected);
  }

  @Test
  public void test_createMetadata_black() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition base = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    RawOptionData capData = RawOptionData.of(
        ImmutableList.of(Period.ofYears(1), Period.ofYears(5)),
        DoubleArray.of(0.005, 0.01, 0.015),
        ValueType.STRIKE,
        DoubleMatrix.copyOf(new double[][] {{0.15, 0.12, 0.13}, {0.1, 0.08, 0.09}}),
        ValueType.BLACK_VOLATILITY);
    SurfaceMetadata expected = Surfaces.blackVolatilityByExpiryStrike(NAME.getName(), ACT_ACT_ISDA);
    SurfaceMetadata computed = base.createMetadata(capData);
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_wrongInterpolator() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, DOUBLE_QUADRATIC, FLAT, FLAT, SabrVolatilityFormula.hagan()));

  }

  @Test
  public void test_createMetadata_wrongValueType() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition base = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
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
    SabrIborCapletFloorletVolatilityBootstrapDefinition test1 = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    coverImmutableBean(test1);
    SabrIborCapletFloorletVolatilityBootstrapDefinition test2 = SabrIborCapletFloorletVolatilityBootstrapDefinition.builder()
        .index(GBP_LIBOR_3M)
        .name(IborCapletFloorletVolatilitiesName.of("other"))
        .interpolator(STEP_UPPER)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(CurveExtrapolators.LINEAR)
        .rhoCurve(ConstantCurve.of("rho", 0.1d))
        .shiftCurve(ConstantCurve.of("shift", 0.01d))
        .dayCount(ACT_365F)
        .sabrVolatilityFormula(SabrVolatilityFormula.hagan())
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition test = SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, 0.5, LINEAR, FLAT, FLAT, SabrVolatilityFormula.hagan());
    assertSerialization(test);
  }

}
