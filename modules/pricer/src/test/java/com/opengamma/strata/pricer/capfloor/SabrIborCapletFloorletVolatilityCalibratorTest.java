/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PCHIP;
import static org.testng.Assert.assertEquals;

import java.time.Period;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Test {@link SabrIborCapletFloorletVolatilityCalibrator}.
 */
@Test
public class SabrIborCapletFloorletVolatilityCalibratorTest
    extends CapletStrippingSetup {

  private static final SabrIborCapletFloorletVolatilityCalibrator CALIBRATOR =
      SabrIborCapletFloorletVolatilityCalibrator.DEFAULT;
  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("test");
  private static final SabrVolatilityFormula HAGAN = SabrVolatilityFormula.hagan();
  // choose nodes close to expiries of caps - 0.25y before end dates
  private static final DoubleArray ALPHA_KNOTS = DoubleArray.of(0.75, 1.75, 2.75, 4.75, 6.75, 9.75);
  private static final DoubleArray BETA_RHO_KNOTS = DoubleArray.of(0.75, 2.75, 4.75);
  private static final DoubleArray NU_KNOTS = DoubleArray.of(0.75, 1.75, 2.75, 4.75, 6.75, 9.75);
  private static final double TOL = 1.0e-3;

  public void recovery_test_black() {
    double beta = 0.7;
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_ACT_ISDA, beta, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, FLAT, HAGAN);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix volData = createFullBlackDataMatrix();
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), 1.0e-3);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, ValueType.STRIKE, volData, error, ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols =
        (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < NUM_BLACK_STRIKES; ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsBlackVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.blackVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        BlackIborCapletFloorletExpiryStrikeVolatilities constVol = BlackIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 3d);
      }
    }
    assertEquals(resVols.getIndex(), USD_LIBOR_3M);
    assertEquals(resVols.getName(), definition.getName());
    assertEquals(resVols.getValuationDateTime(), CALIBRATION_TIME);
    assertEquals(resVols.getParameterCount(), ALPHA_KNOTS.size() + BETA_RHO_KNOTS.size() + NU_KNOTS.size() + 2); // beta, shift counted
    assertEquals(resVols.getParameters().getShiftCurve(), definition.getShiftCurve());
    assertEquals(resVols.getParameters().getBetaCurve(), definition.getBetaCurve().get());
  }

  public void recovery_test_black_fixedRho() {
    double rho = 0.15;
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(
            NAME, USD_LIBOR_3M, ACT_ACT_ISDA, rho, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, FLAT, HAGAN);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix volData = createFullBlackDataMatrix();
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), 1.0e-3);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, ValueType.STRIKE, volData, error, ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols =
        (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < NUM_BLACK_STRIKES; ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsBlackVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.blackVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        BlackIborCapletFloorletExpiryStrikeVolatilities constVol = BlackIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 5d);
      }
    }
    assertEquals(resVols.getIndex(), USD_LIBOR_3M);
    assertEquals(resVols.getName(), definition.getName());
    assertEquals(resVols.getValuationDateTime(), CALIBRATION_TIME);
    assertEquals(resVols.getParameterCount(), ALPHA_KNOTS.size() + BETA_RHO_KNOTS.size() + NU_KNOTS.size() + 2); // beta, shift counted
    assertEquals(resVols.getParameters().getShiftCurve(), definition.getShiftCurve());
    assertEquals(resVols.getParameters().getRhoCurve(), definition.getRhoCurve().get());
  }

  public void recovery_test_black_shift() {
    double shift = 0.05;
    DoubleArray initial = DoubleArray.of(0.03, 0.7, 0.15, 0.9);
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_ACT_ISDA, shift, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, initial, PCHIP, FLAT, FLAT, HAGAN);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix volData = createFullBlackDataMatrix();
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), 1.0e-3);
    RawOptionData data = RawOptionData.of(maturities, strikes, ValueType.STRIKE, volData, error, ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols = (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < NUM_BLACK_STRIKES; ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsBlackVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.blackVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        BlackIborCapletFloorletExpiryStrikeVolatilities constVol = BlackIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 5d);
      }
    }
    assertEquals(resVols.getParameters().getBetaCurve(), definition.getBetaCurve().get());
    assertEquals(resVols.getParameters().getShiftCurve(), definition.getShiftCurve());
  }

  public void recovery_test_black_shift_fixedRho() {
    double shift = 0.05;
    DoubleArray initial = DoubleArray.of(0.03, 0.7, 0.35, 0.9);
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(
            NAME, USD_LIBOR_3M, ACT_ACT_ISDA, shift, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, initial, PCHIP, FLAT, FLAT, HAGAN);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix volData =createFullBlackDataMatrix();
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), 1.0e-3);
    RawOptionData data = RawOptionData.of(maturities, strikes, ValueType.STRIKE, volData, error, ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols = (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < NUM_BLACK_STRIKES; ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsBlackVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.blackVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        BlackIborCapletFloorletExpiryStrikeVolatilities constVol = BlackIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 5d);
      }
    }
    assertEquals(resVols.getParameters().getShiftCurve(), definition.getShiftCurve());
    assertEquals(resVols.getParameters().getRhoCurve(), definition.getRhoCurve().get());
  }

  public void recovery_test_flat() {
    DoubleArray initial = DoubleArray.of(0.4, 0.95, 0.5, 0.05);
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_ACT_ISDA, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, initial, LINEAR, FLAT, FLAT, HAGAN);
    DoubleArray strikes = createBlackStrikes();
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), strikes, ValueType.STRIKE, createFullFlatBlackDataMatrix(), ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrIborCapletFloorletVolatilities resVol =
        (SabrIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < NUM_BLACK_STRIKES; ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsFlatBlackVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.blackVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        BlackIborCapletFloorletExpiryStrikeVolatilities constVol = BlackIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVol).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
  }

  public void recovery_test_normal() {
    double beta = 0.7;
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(
            NAME, USD_LIBOR_3M, ACT_ACT_ISDA, beta, ALPHA_KNOTS, BETA_RHO_KNOTS, NU_KNOTS, DOUBLE_QUADRATIC, FLAT, FLAT, HAGAN);
    ImmutableList<Period> maturities = createNormalEquivMaturities();
    DoubleArray strikes = createNormalEquivStrikes();
    DoubleMatrix volData = createFullNormalEquivDataMatrix();
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), 1.0e-3);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, ValueType.STRIKE, volData, error, ValueType.NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrIborCapletFloorletVolatilities resVols =
        (SabrIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsNormalEquivVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.normalVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        NormalIborCapletFloorletExpiryStrikeVolatilities constVol = NormalIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_NORMAL.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 3d);
      }
    }
  }

  public void recovery_test_normal_fixedRho() {
    DoubleArray initial = DoubleArray.of(0.05, 0.7, 0.35, 0.9);
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(NAME, USD_LIBOR_3M, ACT_ACT_ISDA, ALPHA_KNOTS,
            BETA_RHO_KNOTS, NU_KNOTS, initial, DOUBLE_QUADRATIC, FLAT, FLAT, HAGAN);
    ImmutableList<Period> maturities = createNormalEquivMaturities();
    DoubleArray strikes = createNormalEquivStrikes();
    DoubleMatrix volData = createFullNormalEquivDataMatrix();
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), 1.0e-3);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, ValueType.STRIKE, volData, error, ValueType.NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrIborCapletFloorletVolatilities resVols =
        (SabrIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsNormalEquivVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.normalVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        NormalIborCapletFloorletExpiryStrikeVolatilities constVol = NormalIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_NORMAL.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 5d);
      }
    }
  }

  public void recovery_test_normal_shift() {
    double shift = 0.02;
    DoubleArray initial = DoubleArray.of(0.05, 0.7, 0.35, 0.9);
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedBeta(NAME, USD_LIBOR_3M, ACT_ACT_ISDA, shift, ALPHA_KNOTS,
            BETA_RHO_KNOTS, NU_KNOTS, initial, DOUBLE_QUADRATIC, FLAT, FLAT, HAGAN);
    ImmutableList<Period> maturities = createNormalEquivMaturities();
    DoubleArray strikes = createNormalEquivStrikes();
    DoubleMatrix volData = createFullNormalEquivDataMatrix();
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), 1.0e-3);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, ValueType.STRIKE, volData, error, ValueType.NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrIborCapletFloorletVolatilities resVols =
        (SabrIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsNormalEquivVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.normalVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        NormalIborCapletFloorletExpiryStrikeVolatilities constVol = NormalIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_NORMAL.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 5d);
      }
    }
  }

  public void recovery_test_normal_shift_fixedRho() {
    double shift = 0.02;
    DoubleArray initial = DoubleArray.of(0.05, 0.35, 0.0, 0.9);
    SabrIborCapletFloorletVolatilityCalibrationDefinition definition =
        SabrIborCapletFloorletVolatilityCalibrationDefinition.ofFixedRho(NAME, USD_LIBOR_3M, ACT_ACT_ISDA, shift, ALPHA_KNOTS,
            BETA_RHO_KNOTS, NU_KNOTS, initial, DOUBLE_QUADRATIC, FLAT, FLAT, HAGAN);
    ImmutableList<Period> maturities = createNormalEquivMaturities();
    DoubleArray strikes = createNormalEquivStrikes();
    DoubleMatrix volData = createFullNormalEquivDataMatrix();
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), 1.0e-3);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, ValueType.STRIKE, volData, error, ValueType.NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrIborCapletFloorletVolatilities resVols =
        (SabrIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsNormalEquivVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.normalVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        NormalIborCapletFloorletExpiryStrikeVolatilities constVol = NormalIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_NORMAL.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 5d);
      }
    }
  }

}
