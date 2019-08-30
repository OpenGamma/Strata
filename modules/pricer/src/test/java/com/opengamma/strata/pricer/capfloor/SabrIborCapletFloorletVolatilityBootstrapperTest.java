/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Test {@link SabrIborCapletFloorletVolatilityBootstrapper}.
 */
public class SabrIborCapletFloorletVolatilityBootstrapperTest extends CapletStrippingSetup {

  private static final SabrIborCapletFloorletVolatilityBootstrapper CALIBRATOR =
      SabrIborCapletFloorletVolatilityBootstrapper.DEFAULT;
  private static final double TOL = 1.0e-3;

  @Test
  public void test_recovery_black() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition definition =
        SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
            IborCapletFloorletVolatilitiesName.of("test"),
            USD_LIBOR_3M,
            ACT_ACT_ISDA,
            0.85,
            CurveInterpolators.STEP_UPPER,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT,
            SabrHaganVolatilityFunctionProvider.DEFAULT);
    DoubleMatrix volData = createFullBlackDataMatrix();
    double errorValue = 1.0e-3;
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), errorValue);
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), createBlackStrikes(), ValueType.STRIKE, volData, error, ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols = (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
    double expSq = 0d;
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
        expSq += Math.pow((priceOrg - priceCalib) / priceOrg / errorValue, 2);
        assertThat(priceOrg).isCloseTo(priceCalib, offset(Math.max(priceOrg, 1d) * TOL * 3d));
      }
    }
    assertThat(res.getChiSquare()).isCloseTo(expSq, offset(expSq * 1.0e-14));
    assertThat(resVols.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(resVols.getName()).isEqualTo(definition.getName());
    assertThat(resVols.getValuationDateTime()).isEqualTo(CALIBRATION_TIME);
    assertThat(resVols.getParameters().getShiftCurve()).isEqualTo(definition.getShiftCurve());
    assertThat(resVols.getParameters().getBetaCurve()).isEqualTo(definition.getBetaCurve().get());
  }

  @Test
  public void test_recovery_black_fixedRho() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition definition =
        SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedRho(
            IborCapletFloorletVolatilitiesName.of("test"),
            USD_LIBOR_3M,
            ACT_ACT_ISDA,
            0.0,
            CurveInterpolators.STEP_UPPER,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT,
            SabrHaganVolatilityFunctionProvider.DEFAULT);
    DoubleMatrix volData = createFullBlackDataMatrix();
    double errorValue = 1.0e-3;
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), errorValue);
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), createBlackStrikes(), ValueType.STRIKE, volData, error, ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols = (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
    double expSq = 0d;
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
        expSq += Math.pow((priceOrg - priceCalib) / priceOrg / errorValue, 2);
        assertThat(priceOrg).isCloseTo(priceCalib, offset(Math.max(priceOrg, 1d) * TOL * 3d));
      }
    }
    assertThat(res.getChiSquare()).isCloseTo(expSq, offset(expSq * 1.0e-14));
    assertThat(resVols.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(resVols.getName()).isEqualTo(definition.getName());
    assertThat(resVols.getValuationDateTime()).isEqualTo(CALIBRATION_TIME);
    assertThat(resVols.getParameters().getShiftCurve()).isEqualTo(definition.getShiftCurve());
    assertThat(resVols.getParameters().getRhoCurve()).isEqualTo(definition.getRhoCurve().get());
  }

  @Test
  public void test_invalid_data() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition definition =
        SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
            IborCapletFloorletVolatilitiesName.of("test"),
            USD_LIBOR_3M,
            ACT_ACT_ISDA,
            0.85,
            CurveInterpolators.LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT,
            SabrHaganVolatilityFunctionProvider.DEFAULT);
    RawOptionData data = RawOptionData.of(createBlackMaturities(), createBlackStrikes(), ValueType.STRIKE,
        createFullBlackDataMatrixInvalid(), ValueType.BLACK_VOLATILITY);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER));
  }

  @Test
  public void test_recovery_black_shift() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition definition =
        SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
            IborCapletFloorletVolatilitiesName.of("test"),
            USD_LIBOR_3M,
            ACT_ACT_ISDA,
            0.95,
            0.02,
            CurveInterpolators.LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT,
            SabrHaganVolatilityFunctionProvider.DEFAULT);
    DoubleMatrix volData = createFullBlackDataMatrix();
    double errorValue = 1.0e-3;
    DoubleMatrix error = DoubleMatrix.filled(volData.rowCount(), volData.columnCount(), errorValue);
    RawOptionData data = RawOptionData.of(
        createBlackMaturities(), createBlackStrikes(), ValueType.STRIKE, volData, error, ValueType.BLACK_VOLATILITY);
    IborCapletFloorletVolatilityCalibrationResult res = CALIBRATOR.calibrate(definition, CALIBRATION_TIME, data, RATES_PROVIDER);
    SabrParametersIborCapletFloorletVolatilities resVols = (SabrParametersIborCapletFloorletVolatilities) res.getVolatilities();
    double expSq = 0d;
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
        expSq += Math.pow((priceOrg - priceCalib) / priceOrg / errorValue, 2);
        assertThat(priceOrg).isCloseTo(priceCalib, offset(Math.max(priceOrg, 1d) * TOL * 10d));
      }
    }
    assertThat(res.getChiSquare()).isCloseTo(expSq, offset(expSq * 1.0e-14));
    assertThat(resVols.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(resVols.getName()).isEqualTo(definition.getName());
    assertThat(resVols.getValuationDateTime()).isEqualTo(CALIBRATION_TIME);
    assertThat(resVols.getParameters().getShiftCurve()).isEqualTo(definition.getShiftCurve());
    assertThat(resVols.getParameters().getBetaCurve()).isEqualTo(definition.getBetaCurve().get());
  }

  @Test
  public void test_recovery_normal() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition definition =
        SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
            IborCapletFloorletVolatilitiesName.of("test"),
            USD_LIBOR_3M,
            ACT_ACT_ISDA,
            0.85,
            CurveInterpolators.LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT,
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
        assertThat(priceOrg).isCloseTo(priceCalib, offset(Math.max(priceOrg, 1d) * TOL * 3d));
      }
    }
    assertThat(res.getChiSquare() > 0d).isTrue();
    assertThat(resVols.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(resVols.getName()).isEqualTo(definition.getName());
    assertThat(resVols.getValuationDateTime()).isEqualTo(CALIBRATION_TIME);
  }

  @Test
  public void test_recovery_normal_fixedRho() {
    SabrIborCapletFloorletVolatilityBootstrapDefinition definition =
        SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedRho(
            IborCapletFloorletVolatilitiesName.of("test"),
            USD_LIBOR_3M,
            ACT_ACT_ISDA,
            0.0,
            CurveInterpolators.LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT,
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
        assertThat(priceOrg).isCloseTo(priceCalib, offset(Math.max(priceOrg, 1d) * TOL * 3d));
      }
    }
    assertThat(res.getChiSquare() > 0d).isTrue();
    assertThat(resVols.getIndex()).isEqualTo(USD_LIBOR_3M);
    assertThat(resVols.getName()).isEqualTo(definition.getName());
    assertThat(resVols.getValuationDateTime()).isEqualTo(CALIBRATION_TIME);
  }

  @Test
  public void test_recovery_flatVol() {
    double beta = 0.8;
    SabrIborCapletFloorletVolatilityBootstrapDefinition definition =
        SabrIborCapletFloorletVolatilityBootstrapDefinition.ofFixedBeta(
            IborCapletFloorletVolatilitiesName.of("test"),
            USD_LIBOR_3M,
            ACT_ACT_ISDA,
            beta,
            CurveInterpolators.STEP_UPPER,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.FLAT,
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
        assertThat(priceOrg).isCloseTo(priceCalib, offset(Math.max(priceOrg, 1d) * TOL));
      }
    }
  }

}
