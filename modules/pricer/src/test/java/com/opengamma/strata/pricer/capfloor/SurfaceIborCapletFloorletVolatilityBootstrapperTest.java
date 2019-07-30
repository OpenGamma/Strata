/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.DOUBLE_QUADRATIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfacePeriodParameterMetadata;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Test {@link SurfaceIborCapletFloorletVolatilityBootstrapper}.
 */
@Test
public class SurfaceIborCapletFloorletVolatilityBootstrapperTest extends CapletStrippingSetup {

  private static final SurfaceIborCapletFloorletVolatilityBootstrapper CALIBRATOR =
      SurfaceIborCapletFloorletVolatilityBootstrapper.DEFAULT;
  private static final double TOL = 1.0e-14;

  public void recovery_test_blackSurface() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition definition = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, LINEAR);
    DoubleArray strikes = createBlackStrikes();
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), strikes, ValueType.STRIKE, createFullBlackDataMatrix(), ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    BlackIborCapletFloorletExpiryStrikeVolatilities resVol =
        (BlackIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
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
        double priceCalib = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, resVol).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
    assertEquals(res.getChiSquare(), 0d);
    assertEquals(resVol.getIndex(), USD_LIBOR_3M);
    assertEquals(resVol.getName(), definition.getName());
    assertEquals(resVol.getValuationDateTime(), CALIBRATION_TIME);
    InterpolatedNodalSurface surface = (InterpolatedNodalSurface) resVol.getSurface();
    for (int i = 0; i < surface.getParameterCount(); ++i) {
      GenericVolatilitySurfacePeriodParameterMetadata metadata =
          (GenericVolatilitySurfacePeriodParameterMetadata) surface.getParameterMetadata(i);
      assertEquals(metadata.getStrike().getValue(), surface.getYValues().get(i));
    }
  }

  public void test_invalid_data() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition definition = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, LINEAR);
    DoubleArray strikes = createBlackStrikes();
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), strikes, ValueType.STRIKE, createFullBlackDataMatrixInvalid(), ValueType.BLACK_VOLATILITY);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER));
  }

  public void recovery_test_blackSurface_shift() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition definition = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, LINEAR,
        ConstantCurve.of("Black shift", 0.02));
    DoubleArray strikes = createBlackStrikes();
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), strikes, ValueType.STRIKE, createFullBlackDataMatrix(), ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities resVol =
        (ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
    for (int i = 0; i < strikes.size(); ++i) {
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
        double priceCalib = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, resVol).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
    assertEquals(res.getChiSquare(), 0d);
    assertEquals(resVol.getIndex(), USD_LIBOR_3M);
    assertEquals(resVol.getName(), definition.getName());
    assertEquals(resVol.getValuationDateTime(), CALIBRATION_TIME);
    assertEquals(resVol.getShiftCurve(), definition.getShiftCurve().get());
    InterpolatedNodalSurface surface = (InterpolatedNodalSurface) resVol.getSurface();
    for (int i = 0; i < surface.getParameterCount(); ++i) {
      GenericVolatilitySurfacePeriodParameterMetadata metadata =
          (GenericVolatilitySurfacePeriodParameterMetadata) surface.getParameterMetadata(i);
      assertEquals(metadata.getStrike().getValue() + 0.02, surface.getYValues().get(i));
    }
  }

  public void recovery_test_blackCurve() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition definition = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, LINEAR);
    DoubleArray strikes = createBlackStrikes();
    for (int i = 0; i < strikes.size(); ++i) {
      Pair<List<Period>, DoubleMatrix> trimedData = trimData(createBlackMaturities(), createBlackDataMatrixForStrike(i));
      RawOptionData data = RawOptionData.of(
          trimedData.getFirst(), DoubleArray.of(strikes.get(i)), ValueType.STRIKE, trimedData.getSecond(),
          ValueType.BLACK_VOLATILITY);
      IborCapletFloorletVolatilityCalibrationResult res =
          CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
      BlackIborCapletFloorletExpiryStrikeVolatilities resVol =
          (BlackIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
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
        double priceCalib = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, resVol).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
  }

  public void recovery_test_flat() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition definition = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, LINEAR);
    DoubleArray strikes = createBlackStrikes();
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), strikes, ValueType.STRIKE, createFullFlatBlackDataMatrix(), ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    BlackIborCapletFloorletExpiryStrikeVolatilities resVol =
        (BlackIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
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
        double priceCalib = LEG_PRICER_BLACK.presentValue(caps.get(j), RATES_PROVIDER, resVol).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
    assertEquals(res.getChiSquare(), 0d);
  }

  public void recovery_test_normal1() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition definition = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, DOUBLE_QUADRATIC);
    DoubleArray strikes = createNormalStrikes();
    RawOptionData data = RawOptionData.of(
        createNormalMaturities(), strikes, ValueType.STRIKE, createFullNormalDataMatrix(), ValueType.NORMAL_VOLATILITY);
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
    assertEquals(res.getChiSquare(), 0d);
    assertEquals(res.getChiSquare(), 0d);
    assertEquals(resVol.getIndex(), USD_LIBOR_3M);
    assertEquals(resVol.getName(), definition.getName());
    assertEquals(resVol.getValuationDateTime(), CALIBRATION_TIME);
    InterpolatedNodalSurface surface = (InterpolatedNodalSurface) resVol.getSurface();
    for (int i = 0; i < surface.getParameterCount(); ++i) {
      GenericVolatilitySurfacePeriodParameterMetadata metadata =
          (GenericVolatilitySurfacePeriodParameterMetadata) surface.getParameterMetadata(i);
      assertEquals(metadata.getStrike().getValue(), surface.getYValues().get(i));
    }
  }

  public void recovery_test_normal2() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition definition = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, DOUBLE_QUADRATIC);
    DoubleArray strikes = createNormalEquivStrikes();
    RawOptionData data = RawOptionData.of(
        createNormalEquivMaturities(), strikes, ValueType.STRIKE, createFullNormalEquivDataMatrix(), ValueType.NORMAL_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    NormalIborCapletFloorletExpiryStrikeVolatilities resVol =
        (NormalIborCapletFloorletExpiryStrikeVolatilities) res.getVolatilities();
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
        double priceCalib = LEG_PRICER_NORMAL.presentValue(caps.get(j), RATES_PROVIDER, resVol).getAmount();
        assertEquals(priceOrg, priceCalib, Math.max(priceOrg, 1d) * TOL);
      }
    }
    assertEquals(res.getChiSquare(), 0d);
  }

  public void recovery_test_normal2_shift() {
    SurfaceIborCapletFloorletVolatilityBootstrapDefinition definition = SurfaceIborCapletFloorletVolatilityBootstrapDefinition.of(
        IborCapletFloorletVolatilitiesName.of("test"), USD_LIBOR_3M, ACT_ACT_ISDA, LINEAR, DOUBLE_QUADRATIC,
        ConstantCurve.of("Black shift", 0.02));
    DoubleArray strikes = createNormalEquivStrikes();
    RawOptionData data = RawOptionData.of(
        createNormalEquivMaturities(), strikes, ValueType.STRIKE, createFullNormalEquivDataMatrix(), ValueType.NORMAL_VOLATILITY);
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
    assertEquals(res.getChiSquare(), 0d);
  }

  //-------------------------------------------------------------------------
  // remove null for one-dimensional bootstrapping
  private Pair<List<Period>, DoubleMatrix> trimData(List<Period> expiries, DoubleMatrix vols) {
    List<Period> resExpiries = new ArrayList<>();
    List<Double> resVols = new ArrayList<>();
    int size = vols.size();
    for (int i = 0; i < size; ++i) {
      if (Double.isFinite(vols.get(i, 0))) {
        resExpiries.add(expiries.get(i));
        resVols.add(vols.get(i, 0));
      }
    }
    return Pair.of(resExpiries, DoubleMatrix.of(resExpiries.size(), 1, DoubleArray.copyOf(resVols).toArray()));
  }

}
