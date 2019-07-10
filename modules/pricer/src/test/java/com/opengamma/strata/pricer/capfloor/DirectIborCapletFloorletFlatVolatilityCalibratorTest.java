/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.STRIKE;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.time.Period;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Test {@link DirectIborCapletFloorletFlatVolatilityCalibrator}.
 */
@Test
public class DirectIborCapletFloorletFlatVolatilityCalibratorTest
    extends CapletStrippingSetup {

  private static final IborCapletFloorletVolatilitiesName NAME = IborCapletFloorletVolatilitiesName.of("test");
  private static final DirectIborCapletFloorletFlatVolatilityCalibrator CALIBRATOR =
      DirectIborCapletFloorletFlatVolatilityCalibrator.standard();
  private static final BlackIborCapFloorLegPricer LEG_PRICER_BLACK = BlackIborCapFloorLegPricer.DEFAULT;
  private static final NormalIborCapFloorLegPricer LEG_PRICER_NORMAL = NormalIborCapFloorLegPricer.DEFAULT;
  private static final double TOL = 1.0e-5;

  public void test_recovery_black() {
    double lambdaT = 0.07;
    double error = 1.0e-5;
    DirectIborCapletFloorletFlatVolatilityDefinition definition = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, LINEAR);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackAtmStrike();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createAtmBlackDataMatrix(), errorMatrix, BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    BlackIborCapletFloorletExpiryFlatVolatilities resVols =
        (BlackIborCapletFloorletExpiryFlatVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsBlackAtmVols(strikes.get(i));
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
  }

  public void test_recovery_normal() {
    double lambdaT = 0.07;
    double error = 1.0e-5;
    DirectIborCapletFloorletFlatVolatilityDefinition definition = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, LINEAR);
    ImmutableList<Period> maturities = createNormalMaturities();
    DoubleArray strikes = createNormalAtmStrike();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createAtmNormalDataMatrix(), errorMatrix, NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    NormalIborCapletFloorletExpiryFlatVolatilities resVol =
        (NormalIborCapletFloorletExpiryFlatVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
      Pair<List<ResolvedIborCapFloorLeg>, List<Double>> capsAndVols = getCapsNormalAtmVols(strikes.get(i));
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

  public void test_wrong_data() {
    double lambdaT = 0.07;
    double error = 1.0e-5;
    DirectIborCapletFloorletFlatVolatilityDefinition definition = DirectIborCapletFloorletFlatVolatilityDefinition.of(
        NAME, USD_LIBOR_3M, ACT_ACT_ISDA, lambdaT, LINEAR);
    ImmutableList<Period> maturities = createBlackMaturities();
    DoubleArray strikes = createBlackStrikes();
    DoubleMatrix errorMatrix = DoubleMatrix.filled(maturities.size(), strikes.size(), error);
    RawOptionData data = RawOptionData.of(
        maturities, strikes, STRIKE, createFullBlackDataMatrix(), errorMatrix, BLACK_VOLATILITY);
    assertThrows(IllegalArgumentException.class, () -> CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER));
  }

}
