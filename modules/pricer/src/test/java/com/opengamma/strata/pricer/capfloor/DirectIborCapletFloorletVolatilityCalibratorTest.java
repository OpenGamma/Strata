/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.STRIKE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.Period;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Test {@link DirectIborCapletFloorletVolatilityCalibrator}.
 */
@Test
public class DirectIborCapletFloorletVolatilityCalibratorTest
    extends CapletStrippingSetup {

  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("test");
  private static final GridSurfaceInterpolator INTERPOLATOR =
      GridSurfaceInterpolator.of(CurveInterpolators.LINEAR, CurveInterpolators.LINEAR);
  private static final DirectIborCapletFloorletVolatilityCalibrator CALIBRATOR =
      DirectIborCapletFloorletVolatilityCalibrator.DEFAULT;
  private static final BlackIborCapFloorLegPricer LEG_PRICER_BLACK = BlackIborCapFloorLegPricer.DEFAULT;
  private static final NormalIborCapFloorLegPricer LEG_PRICER_NORMAL = NormalIborCapFloorLegPricer.DEFAULT;
  private static final double TOL = 1.0e-4;

  public void test_recovery_black() {
    double lambdaT = 0.07;
    double lambdaK = 0.07;
    double error = 1.0e-5;
    DirectIborCapletFloorletVolatilityDefinition definition = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, lambdaK, INTERPOLATOR);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createFullBlackDataMatrix(), errorMatrix, BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    BlackIborCapletFloorletExpiryStrikeVolatilities resVols =
        (BlackIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
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
        double priceCalib = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 5d);
      }
    }
    assertTrue(res.getChiSquare() > 0d);
    assertEquals(resVols.getIndex(), USD_LIBOR_3M);
    assertEquals(resVols.getName(), definition.getName());
    assertEquals(resVols.getValuationDateTime(), CALIBRATION_TIME);
  }

  public void recovery_test_shiftedBlack() {
    double lambdaT = 0.07;
    double lambdaK = 0.07;
    double error = 1.0e-5;
    ConstantCurve shiftCurve = ConstantCurve.of("Black shift", 0.02);
    DirectIborCapletFloorletVolatilityDefinition definition = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, lambdaK, INTERPOLATOR, shiftCurve);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createFullBlackDataMatrix(), errorMatrix, BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities resVols =
        (ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
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
        double priceCalib = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
    assertTrue(res.getChiSquare() > 0d);
    assertEquals(resVols.getIndex(), USD_LIBOR_3M);
    assertEquals(resVols.getName(), definition.getName());
    assertEquals(resVols.getValuationDateTime(), CALIBRATION_TIME);
    assertEquals(resVols.getShiftCurve(), definition.getShiftCurve().get());
  }

  public void recovery_test_flat() {
    double lambdaT = 0.01;
    double lambdaK = 0.01;
    double error = 1.0e-3;
    DirectIborCapletFloorletVolatilityDefinition definition = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, lambdaK, INTERPOLATOR);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createFullFlatBlackDataMatrix(), errorMatrix, BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    BlackIborCapletFloorletExpiryStrikeVolatilities resVol =
        (BlackIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
    Surface resSurface = resVol.getSurface();
    int nParams = resSurface.getParameterCount();
    for (int i = 0; i < nParams; ++i) {
      assertEquals(resSurface.getParameter(i), 0.5, 1.0e-11);
    }
  }

  public void recovery_test_normalFlat() {
    double lambdaT = 0.01;
    double lambdaK = 0.01;
    double error = 1.0e-3;
    DirectIborCapletFloorletVolatilityDefinition definition = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, lambdaK, INTERPOLATOR);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createFullFlatBlackDataMatrix(), errorMatrix, NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    NormalIborCapletFloorletExpiryStrikeVolatilities resVol =
        (NormalIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
    Surface resSurface = resVol.getSurface();
    int nParams = resSurface.getParameterCount();
    for (int i = 0; i < nParams; ++i) {
      assertEquals(resSurface.getParameter(i), 0.5, 1.0e-12);
    }
  }

  public void recovery_test_normal() {
    double lambdaT = 0.07;
    double lambdaK = 0.07;
    double error = 1.0e-5;
    DirectIborCapletFloorletVolatilityDefinition definition = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, lambdaK, INTERPOLATOR);
    ImmutableList<Period> maturities = createNormalMaturities();
    DoubleArray strikes = createNormalStrikes();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createFullNormalDataMatrix(), errorMatrix, NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    NormalIborCapletFloorletExpiryStrikeVolatilities resVol =
        (NormalIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsNormalVols(i);
      List<ResolvedIborCapFloorLeg> caps = capsAndVols.getFirst();
      List<Double> vols = capsAndVols.getSecond();
      int nCaps = caps.size();
      for (int j = 0; j < nCaps; ++j) {
        ConstantSurface volSurface = ConstantSurface.of(
            Surfaces.normalVolatilityByExpiryStrike("test", ACT_ACT_ISDA), vols.get(j));
        NormalIborCapletFloorletExpiryStrikeVolatilities constVol = NormalIborCapletFloorletExpiryStrikeVolatilities.of(
            USD_LIBOR_3M, CALIBRATION_TIME, volSurface);
        double priceOrg = LEG_PRICER_NORMAL.presentValue(caps.get(j), RATES_PROVIDER, constVol).getAmount();
        double priceCalib = LEG_PRICER_NORMAL.presentValue(caps.get(j), RATES_PROVIDER, resVol).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
  }

  public void recovery_test_normalToBlack() {
    double lambdaT = 0.07;
    double lambdaK = 0.07;
    double error = 1.0e-5;
    ConstantCurve shiftCurve = ConstantCurve.of("Black shift", 0.02);
    DirectIborCapletFloorletVolatilityDefinition definition = DirectIborCapletFloorletVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, lambdaK, INTERPOLATOR, shiftCurve);
    ImmutableList<Period> maturities = createNormalEquivMaturities();
    DoubleArray strikes = createNormalEquivStrikes();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createFullNormalEquivDataMatrix(), errorMatrix, NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities resVol =
        (ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
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
        double priceCalib = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, resVol).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
  }

}
