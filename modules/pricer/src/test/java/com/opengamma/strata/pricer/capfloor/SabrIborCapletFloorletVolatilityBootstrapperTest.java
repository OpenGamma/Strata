/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Test {@link SabrIborCapletFloorletVolatilityBootstrapper}.
 */
@Test
public class SabrIborCapletFloorletVolatilityBootstrapperTest extends CapletStrippingSetup {

  private static final SabrIborCapletFloorletVolatilityBootstrapper CALIBRATOR =
      SabrIborCapletFloorletVolatilityBootstrapper.DEFAULT;
  private static final BlackIborCapFloorLegPricer LEG_PRICER_BLACK = BlackIborCapFloorLegPricer.DEFAULT;
  private static final NormalIborCapFloorLegPricer LEG_PRICER_NORMAL = NormalIborCapFloorLegPricer.DEFAULT;
  private static final SabrIborCapFloorLegPricer LEG_PRICER_SABR = SabrIborCapFloorLegPricer.DEFAULT;
  private static final double TOL = 1.0e-3;

  public void test_recovery_black() {
    SabrIborCapletFloorletBootstrapDefinition definition = SabrIborCapletFloorletBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, 0.85, CurveInterpolators.LINEAR,
        SabrHaganVolatilityFunctionProvider.DEFAULT);
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), createBlackStrikes(), ValueType.STRIKE, createFullBlackDataMatrix(), ValueType.BLACK_VOLATILITY);
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
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 3d);
      }
    }
    assertTrue(res.getChiSquare() > 0d);
    assertEquals(resVols.getIndex(), USD_LIBOR_3M);
    assertEquals(resVols.getName(), definition.getName());
    assertEquals(resVols.getValuationDateTime(), CALIBRATION_TIME);
    assertEquals(resVols.getParameters().getShiftCurve(), definition.getShiftCurve());
    assertEquals(resVols.getParameters().getBetaCurve(), definition.getBetaCurve().get());
  }

  public void test_recovery_black_shift() {
    SabrIborCapletFloorletBootstrapDefinition definition =
        SabrIborCapletFloorletBootstrapDefinition.of(IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA,
            0.95, 0.02, CurveInterpolators.LINEAR,
            SabrHaganVolatilityFunctionProvider.DEFAULT);
    RawOptionData data =
        RawOptionData.of(createBlackMaturities(), createBlackStrikes(), ValueType.STRIKE, createFullBlackDataMatrix(),
            ValueType.BLACK_VOLATILITY);
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
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL * 3d);
      }
    }
    assertTrue(res.getChiSquare() > 0d);
    assertEquals(resVols.getIndex(), USD_LIBOR_3M);
    assertEquals(resVols.getName(), definition.getName());
    assertEquals(resVols.getValuationDateTime(), CALIBRATION_TIME);
    assertEquals(resVols.getParameters().getShiftCurve(), definition.getShiftCurve());
    assertEquals(resVols.getParameters().getBetaCurve(), definition.getBetaCurve().get());
  }

  public void test_recovery_normal() {
    SabrIborCapletFloorletBootstrapDefinition definition = SabrIborCapletFloorletBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"),
        USD_LIBOR_3M,
        ACT_ACT_ISDA,
        0.85,
        CurveInterpolators.LINEAR,
        SabrHaganVolatilityFunctionProvider.DEFAULT);
    RawOptionData data = RawOptionData.of(
        createNormalEquivMaturities(),
        createNormalEquivStrikes(),
        ValueType.STRIKE,
        createFullNormalEquivDataMatrix(),
        ValueType.NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols = (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
    for (int i = 1; i < NUM_BLACK_STRIKES; ++i) {
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
    assertTrue(res.getChiSquare() > 0d);
    assertEquals(resVols.getIndex(), USD_LIBOR_3M);
    assertEquals(resVols.getName(), definition.getName());
    assertEquals(resVols.getValuationDateTime(), CALIBRATION_TIME);
  }

  @Test
  public void test_recovery_flatVol() {
    double beta = 0.99;
    SabrIborCapletFloorletBootstrapDefinition definition = SabrIborCapletFloorletBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"),
        USD_LIBOR_3M,
        ACT_ACT_ISDA,
        beta,
        CurveInterpolators.STEP_UPPER,
        SabrHaganVolatilityFunctionProvider.DEFAULT);
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(),
        createBlackStrikes(),
        ValueType.STRIKE,
        createFullFlatBlackDataMatrix(),
        ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols = (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
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
        double priceCalib = LEG_PRICER_SABR.presentValue(caps.get(j), RATES_PROVIDER, resVols).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
  }

}
