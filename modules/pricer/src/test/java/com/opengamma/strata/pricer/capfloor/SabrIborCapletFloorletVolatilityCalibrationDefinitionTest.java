/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.SABR_ALPHA;
import static com.opengamma.strata.market.ValueType.SABR_BETA;
import static com.opengamma.strata.market.ValueType.SABR_NU;
import static com.opengamma.strata.market.ValueType.SABR_RHO;
import static com.opengamma.strata.market.ValueType.STRIKE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.Period;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.math.impl.minimization.DoubleRangeLimitTransform;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform.LimitType;
import com.opengamma.strata.math.impl.minimization.SingleRangeLimitTransform;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;
import com.opengamma.strata.pricer.option.RawOptionData;

/**
 * Test {@link SabrIborCapletFloorletVolatilityCalibrationDefinition}.
 */
@Test
public class SabrIborCapletFloorletVolatilityCalibrationDefinitionTest {

  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("test");
  private static final SabrVolatilityFormula HAGAN = SabrVolatilityFormula.hagan();
  // choose nodes close to expiries of caps - 0.25y before end dates
  private static final DoubleArray ALPHA_KNOTS = DoubleArray.of(0.75, 1.75, 2.75, 4.75, 6.75, 9.75);
  private static final DoubleArray BETA_RHO_KNOTS = DoubleArray.of(0.75, 2.75, 4.75);
  private static final DoubleArray NU_KNOTS = DoubleArray.of(0.75, 1.75, 2.75, 4.75, 6.75, 9.75);
  private static final double BETA_RHO = 0.55;
  private static final double SHIFT = 0.05;
  private static final ImmutableList<Period> EXPIRIES = ImmutableList.of(Period.ofYears(1), Period.ofYears(3));
  private static final DoubleArray STRIKES = DoubleArray.of(0.01, 0.02, 0.03);
  private static final DoubleMatrix DATA = DoubleMatrix.copyOf(new double[][] {{0.22, 0.18, 0.18}, {0.17, 0.15, 0.165}});
  private static final RawOptionData SAMPLE_BLACK = RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, BLACK_VOLATILITY);
  private static final RawOptionData SAMPLE_NORMAL = RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, NORMAL_VOLATILITY);

  public void test_ofFixedBeta() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition test =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    assertEquals(test.getBetaCurve().get(),
        ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_365F, SABR_BETA), BETA_RHO));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getExtrapolatorLeft(), FLAT);
    assertEquals(test.getExtrapolatorRight(), LINEAR);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInitialParameters(), DoubleArray.of(0.1, BETA_RHO, -0.2, 0.5));
    assertEquals(test.getInterpolator(), DOUBLE_QUADRATIC);
    assertEquals(test.getName(), NAME);
    assertFalse(test.getRhoCurve().isPresent());
    assertEquals(test.getSabrVolatilityFormula(), HAGAN);
    assertEquals(test.getShiftCurve(), ConstantCurve.of("Zero shift", 0d));
  }

  public void test_ofFixedBeta_shift() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition test =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, SHIFT, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR,
            HAGAN);
    assertEquals(test.getBetaCurve().get(),
        ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_365F, SABR_BETA), BETA_RHO));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getExtrapolatorLeft(), FLAT);
    assertEquals(test.getExtrapolatorRight(), LINEAR);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInitialParameters(), DoubleArray.of(0.1, BETA_RHO, -0.2, 0.5));
    assertEquals(test.getInterpolator(), DOUBLE_QUADRATIC);
    assertEquals(test.getName(), NAME);
    assertFalse(test.getRhoCurve().isPresent());
    assertEquals(test.getSabrVolatilityFormula(), HAGAN);
    assertEquals(test.getShiftCurve(), ConstantCurve.of("Shift curve", SHIFT));
  }


  public void test_ofFixedRho() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition test =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    assertFalse(test.getBetaCurve().isPresent());
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getExtrapolatorLeft(), FLAT);
    assertEquals(test.getExtrapolatorRight(), LINEAR);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInitialParameters(), DoubleArray.of(0.1, 0.7, BETA_RHO, 0.5));
    assertEquals(test.getInterpolator(), DOUBLE_QUADRATIC);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getRhoCurve().get(),
        ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Rho", ACT_365F, SABR_RHO), BETA_RHO));
    assertEquals(test.getSabrVolatilityFormula(), HAGAN);
    assertEquals(test.getShiftCurve(), ConstantCurve.of("Zero shift", 0d));
  }

  public void test_ofFixedRho_shift() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition test =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, SHIFT, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR,
            HAGAN);
    assertFalse(test.getBetaCurve().isPresent());
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getExtrapolatorLeft(), FLAT);
    assertEquals(test.getExtrapolatorRight(), LINEAR);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInitialParameters(), DoubleArray.of(0.1, 0.7, BETA_RHO, 0.5));
    assertEquals(test.getInterpolator(), DOUBLE_QUADRATIC);
    assertEquals(test.getName(), NAME);
    assertEquals(test.getRhoCurve().get(),
        ConstantCurve.of(Curves.sabrParameterByExpiry(NAME.getName() + "-Rho", ACT_365F, SABR_RHO), BETA_RHO));
    assertEquals(test.getSabrVolatilityFormula(), HAGAN);
    assertEquals(test.getShiftCurve(), ConstantCurve.of("Shift curve", SHIFT));
  }

  public void test_builder() {
    Curve betaCurve = InterpolatedNodalCurve.of(
        Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_365F, SABR_BETA),
        DoubleArray.of(2d, 5d),
        DoubleArray.of(0.5, 0.8),
        CurveInterpolators.PCHIP);
    Curve shiftCurve = ConstantCurve.of("shift curve", 0.03d);
    DoubleArray initial = DoubleArray.of(0.34, 0.5, -0.22, 1.2);
    ImmutableList<DoubleArray> knots = ImmutableList.of(ALPHA_KNOTS, DoubleArray.of(), BETA_RHO_KNOTS, NU_KNOTS);
    SabrIborCapletFloorletVolatilityCalibrationDefinition test =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.builder()
            .betaCurve(betaCurve)
            .dayCount(ACT_365F)
            .extrapolatorLeft(FLAT)
            .extrapolatorRight(FLAT)
            .interpolator(DOUBLE_QUADRATIC)
            .index(USD_LIBOR_3M)
            .initialParameters(initial)
            .name(NAME)
            .parameterCurveNodes(knots)
            .sabrVolatilityFormula(HAGAN)
            .shiftCurve(shiftCurve)
            .build();
    assertEquals(test.getBetaCurve().get(), betaCurve);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getExtrapolatorLeft(), FLAT);
    assertEquals(test.getExtrapolatorRight(), FLAT);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getInitialParameters(), initial);
    assertEquals(test.getInterpolator(), DOUBLE_QUADRATIC);
    assertEquals(test.getName(), NAME);
    assertFalse(test.getRhoCurve().isPresent());
    assertEquals(test.getSabrVolatilityFormula(), HAGAN);
    assertEquals(test.getShiftCurve(), shiftCurve);
  }

  public void test_build_fail() {
    Curve betaCurve = InterpolatedNodalCurve.of(
        Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_365F, SABR_BETA),
        DoubleArray.of(2d, 5d),
        DoubleArray.of(0.5, 0.8),
        CurveInterpolators.PCHIP);
    Curve rhoCurve = InterpolatedNodalCurve.of(
        Curves.sabrParameterByExpiry(NAME.getName() + "-Rho", ACT_365F, SABR_RHO),
        DoubleArray.of(2d, 5d),
        DoubleArray.of(0.5, 0.8),
        CurveInterpolators.PCHIP);
    Curve shiftCurve = ConstantCurve.of("shift curve", 0.03d);
    DoubleArray initial = DoubleArray.of(0.34, 0.5, -0.22, 1.2);
    ImmutableList<DoubleArray> knotsEmptyBeta = ImmutableList.of(ALPHA_KNOTS, DoubleArray.of(), BETA_RHO_KNOTS,  NU_KNOTS);
    ImmutableList<DoubleArray> knotsEmptyRho = ImmutableList.of(ALPHA_KNOTS,  BETA_RHO_KNOTS,DoubleArray.of(),  NU_KNOTS);
    // beta, rho not set
    assertThrowsIllegalArg(() -> SabrIborCapletFloorletVolatilityCalibrationDefinition.builder()
        .dayCount(ACT_365F)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(FLAT)
        .interpolator(DOUBLE_QUADRATIC)
        .index(USD_LIBOR_3M)
        .initialParameters(initial)
        .name(NAME)
        .parameterCurveNodes(knotsEmptyBeta)
        .sabrVolatilityFormula(HAGAN)
        .shiftCurve(shiftCurve)
        .build());
    // beta set, but rho knots not defined
    assertThrowsIllegalArg(() -> SabrIborCapletFloorletVolatilityCalibrationDefinition.builder()
        .dayCount(ACT_365F)
        .betaCurve(betaCurve)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(FLAT)
        .interpolator(DOUBLE_QUADRATIC)
        .index(USD_LIBOR_3M)
        .initialParameters(initial)
        .name(NAME)
        .parameterCurveNodes(knotsEmptyRho)
        .sabrVolatilityFormula(HAGAN)
        .shiftCurve(shiftCurve)
        .build());
    // beta rho set
    assertThrowsIllegalArg(() -> SabrIborCapletFloorletVolatilityCalibrationDefinition.builder()
        .dayCount(ACT_365F)
        .betaCurve(betaCurve)
        .rhoCurve(rhoCurve)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(FLAT)
        .interpolator(DOUBLE_QUADRATIC)
        .index(USD_LIBOR_3M)
        .initialParameters(initial)
        .name(NAME)
        .parameterCurveNodes(knotsEmptyBeta)
        .sabrVolatilityFormula(HAGAN)
        .shiftCurve(shiftCurve)
        .build());
    // wrong initial value array size
    assertThrowsIllegalArg(() -> SabrIborCapletFloorletVolatilityCalibrationDefinition.builder()
        .dayCount(ACT_365F)
        .betaCurve(betaCurve)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(FLAT)
        .interpolator(DOUBLE_QUADRATIC)
        .index(USD_LIBOR_3M)
        .initialParameters(DoubleArray.of(0.34, 0.5, -0.22))
        .name(NAME)
        .parameterCurveNodes(knotsEmptyBeta)
        .sabrVolatilityFormula(HAGAN)
        .shiftCurve(shiftCurve)
        .build());
    assertThrowsIllegalArg(() -> SabrIborCapletFloorletVolatilityCalibrationDefinition.builder()
        .dayCount(ACT_365F)
        .betaCurve(betaCurve)
        .extrapolatorLeft(FLAT)
        .extrapolatorRight(FLAT)
        .interpolator(DOUBLE_QUADRATIC)
        .index(USD_LIBOR_3M)
        .initialParameters(initial)
        .name(NAME)
        .parameterCurveNodes(ImmutableList.of(ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS))
        .sabrVolatilityFormula(HAGAN)
        .shiftCurve(shiftCurve)
        .build());
  }

  public void test_createMetadata() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition base =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    assertEquals(base.createMetadata(SAMPLE_BLACK), Surfaces.blackVolatilityByExpiryStrike(NAME.getName(), ACT_365F));
    assertEquals(base.createMetadata(SAMPLE_NORMAL), Surfaces.normalVolatilityByExpiryStrike(NAME.getName(), ACT_365F));
    assertThrowsIllegalArg(() -> base.createMetadata(RawOptionData.of(EXPIRIES, STRIKES, STRIKE, DATA, ValueType.PRICE)));
  }

  public void test_createSabrParameterMetadata() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition base =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    ImmutableList<CurveMetadata> expected = ImmutableList.of(
        Curves.sabrParameterByExpiry(NAME.getName() + "-Alpha", ACT_365F, SABR_ALPHA),
        Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_365F, SABR_BETA),
        Curves.sabrParameterByExpiry(NAME.getName() + "-Rho", ACT_365F, SABR_RHO),
        Curves.sabrParameterByExpiry(NAME.getName() + "-Nu", ACT_365F, SABR_NU));
    ImmutableList<CurveMetadata> computed = base.createSabrParameterMetadata();
    assertEquals(computed, expected);
  }

  public void test_createSabrParameterCurve() {
    DoubleArray nuKnots = DoubleArray.of(5.0);
    SabrIborCapletFloorletVolatilityCalibrationDefinition fixedBeta =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, nuKnots, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    SabrIborCapletFloorletVolatilityCalibrationDefinition fixedRho =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, nuKnots, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    ImmutableList<CurveMetadata> metadata = fixedBeta.createSabrParameterMetadata();
    DoubleArray newValues = DoubleArray.of(
        0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.02, 0.02, 0.02, 0.05);
    DoubleArray newValues1 = DoubleArray.of(0.01, 0.01, 0.01, 0.01, 0.01, 0.01);
    DoubleArray newValues2 = DoubleArray.of(0.02, 0.02, 0.02);
    DoubleArray newValues3 = DoubleArray.of(0.05);
    List<Curve> computedFixedBeta = fixedBeta.createSabrParameterCurve(metadata, newValues);
    List<Curve> computedFixedRho = fixedRho.createSabrParameterCurve(metadata, newValues);
    Curve curveAlpha = InterpolatedNodalCurve.of(
        metadata.get(0), ALPHA_KNOTS, newValues1, DOUBLE_QUADRATIC, FLAT, LINEAR);
    Curve curveBeta = InterpolatedNodalCurve.of(
        metadata.get(1), BETA_RHO_KNOTS, newValues2, DOUBLE_QUADRATIC, FLAT, LINEAR);
    Curve curveRho = InterpolatedNodalCurve.of(
        metadata.get(2), BETA_RHO_KNOTS, newValues2, DOUBLE_QUADRATIC, FLAT, LINEAR);
    Curve curveNu = ConstantNodalCurve.of(metadata.get(3), nuKnots.get(0), newValues3.get(0));
    assertEquals(computedFixedBeta, ImmutableList.of(curveAlpha, fixedBeta.getBetaCurve().get(), curveRho, curveNu));
    assertEquals(computedFixedRho, ImmutableList.of(curveAlpha, curveBeta, fixedRho.getRhoCurve().get(), curveNu));
  }

  public void test_createFullTransform() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition fixedBeta =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    SabrIborCapletFloorletVolatilityCalibrationDefinition fixedRho =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    ParameterLimitsTransform[] transf = new ParameterLimitsTransform[] {
        new SingleRangeLimitTransform(0.0, LimitType.GREATER_THAN),
        new DoubleRangeLimitTransform(0.0, 1.0),
        new DoubleRangeLimitTransform(-0.99, 0.99),
        new DoubleRangeLimitTransform(0.001d, 2.50d)};
    ParameterLimitsTransform[] computedFixedBeta = fixedBeta.createFullTransform(transf);
    ParameterLimitsTransform[] computedFixedRho = fixedRho.createFullTransform(transf);
    ParameterLimitsTransform[] expectedFixedBeta = new ParameterLimitsTransform[] {
        transf[0], transf[0], transf[0], transf[0], transf[0], transf[0], transf[2], transf[2], transf[2],
        transf[3], transf[3], transf[3], transf[3], transf[3], transf[3]};
    ParameterLimitsTransform[] expectedFixedRho = new ParameterLimitsTransform[] {
        transf[0], transf[0], transf[0], transf[0], transf[0], transf[0], transf[1], transf[1], transf[1],
        transf[3], transf[3], transf[3], transf[3], transf[3], transf[3]};
    assertEquals(computedFixedBeta, expectedFixedBeta);
    assertEquals(computedFixedRho, expectedFixedRho);
  }

  public void test_createFullInitialValues() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition fixedBeta =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    SabrIborCapletFloorletVolatilityCalibrationDefinition fixedRho =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    DoubleArray computedFixedBeta = fixedBeta.createFullInitialValues();
    DoubleArray computedFixedRho = fixedRho.createFullInitialValues();
    double alpha = fixedBeta.getInitialParameters().get(0);
    double rho = fixedBeta.getInitialParameters().get(2);
    double nu = fixedBeta.getInitialParameters().get(3);
    DoubleArray expectedFixedBeta = DoubleArray.ofUnsafe(new double[] {
        alpha, alpha, alpha, alpha, alpha, alpha, rho, rho, rho, nu, nu, nu, nu, nu, nu});
    double alpha1 = fixedRho.getInitialParameters().get(0);
    double beta1 = fixedRho.getInitialParameters().get(1);
    double nu1 = fixedRho.getInitialParameters().get(3);
    DoubleArray expectedFixedRho = DoubleArray.ofUnsafe(new double[] {
        alpha1, alpha1, alpha1, alpha1, alpha1, alpha1, beta1, beta1, beta1, nu1, nu1, nu1, nu1, nu1, nu1});
    assertEquals(computedFixedBeta, expectedFixedBeta);
    assertEquals(computedFixedRho, expectedFixedRho);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition test1 =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    coverImmutableBean(test1);
    Curve betaCurve = InterpolatedNodalCurve.of(
        Curves.sabrParameterByExpiry(NAME.getName() + "-Beta", ACT_365F, SABR_BETA),
        DoubleArray.of(2d, 5d),
        DoubleArray.of(0.5, 0.8),
        CurveInterpolators.PCHIP);
    Curve shiftCurve = ConstantCurve.of("shift curve", 0.03d);
    DoubleArray initial = DoubleArray.of(0.34, 0.5, -0.22, 1.2);
    ImmutableList<DoubleArray> knots = ImmutableList.of(ALPHA_KNOTS, DoubleArray.of(), BETA_RHO_KNOTS, DoubleArray.of(1.1));
    SabrIborCapletFloorletVolatilityCalibrationDefinition test2 =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.builder()
            .betaCurve(betaCurve)
            .dayCount(ACT_360)
            .extrapolatorLeft(LINEAR)
            .extrapolatorRight(FLAT)
            .interpolator(PCHIP)
            .index(GBP_LIBOR_3M)
            .initialParameters(initial)
            .name(IborCapletFloorletVolatilitiesName.of("other"))
            .parameterCurveNodes(knots)
            .sabrVolatilityFormula(HAGAN)
            .shiftCurve(shiftCurve)
            .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    SabrIborCapletFloorletVolatilityCalibrationDefinition test =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_365F, BETA_RHO, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, LINEAR, HAGAN);
    assertSerialization(test);
  }

}
