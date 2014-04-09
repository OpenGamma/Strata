/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.swaption;

import com.opengamma.analytics.financial.interestrate.PresentValueSABRSensitivityDataBundle;
import com.opengamma.analytics.financial.interestrate.SABRSensitivityNodeCalculator;
import com.opengamma.analytics.financial.interestrate.swaption.derivative.SwaptionPhysicalFixedIbor;
import com.opengamma.analytics.financial.interestrate.swaption.provider.SwaptionPhysicalFixedIborSABRMethod;
import com.opengamma.analytics.financial.model.option.definition.SABRInterestRateParameters;
import com.opengamma.analytics.financial.provider.calculator.discounting.PV01CurveParametersCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.MarketQuoteSensitivityBlockCalculator;
import com.opengamma.analytics.financial.provider.calculator.sabrswaption.PresentValueCurveSensitivitySABRSwaptionCalculator;
import com.opengamma.analytics.financial.provider.calculator.sabrswaption.PresentValueSABRSensitivitySABRSwaptionCalculator;
import com.opengamma.analytics.financial.provider.calculator.sabrswaption.PresentValueSABRSwaptionCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.SABRSwaptionProviderDiscount;
import com.opengamma.analytics.financial.provider.description.interestrate.SABRSwaptionProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.parameter.ParameterSensitivityParameterCalculator;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Calculator for swaptions using SABR volatility data.
 */
public class SABRSwaptionCalculator implements SwaptionCalculator {

  /**
   * Provides scaling to/from basis points.
   */
  private static final double BASIS_POINT_FACTOR = 1.0E-4;

  /**
   * Calculator for the present value of the swaption.
   */
  private static final PresentValueSABRSwaptionCalculator PV_CALCULATOR =
      PresentValueSABRSwaptionCalculator.getInstance();

  /**
   * Calculator for the implied volatility for the swaption.
   */
  // todo - this should be wrapped in a calculator like the others - PLAT-6364
  private static final SwaptionPhysicalFixedIborSABRMethod IMPLIED_VOL_CALCULATOR =
      SwaptionPhysicalFixedIborSABRMethod.getInstance();

  /**
   * Calculator for bucketed SABR risk for the swaption.
   */
  private static final PresentValueSABRSensitivitySABRSwaptionCalculator BUCKETED_SABR_RISK_CALCULATOR =
      PresentValueSABRSensitivitySABRSwaptionCalculator.getInstance();

  /**
   * Calculator for bucketed PV01 for the swaption.
   */
  private static final MarketQuoteSensitivityBlockCalculator<SABRSwaptionProviderInterface> BUCKETED_PV01_CALCULATOR =
      createBucketedPV01Calculator();


  private static final PV01CurveParametersCalculator<SABRSwaptionProviderInterface> PV01_CALCULATOR =
      createPV01Calculator();

  /**
   * Derivative form of the security.
   */
  private final SwaptionPhysicalFixedIbor _swaption;

  /**
   * The multicurve bundle, including the curves and SABR parameters.
   */
  private final SABRSwaptionProviderDiscount _bundle;

  /**
   * The curve building block for the multicurve bundle.
   */
  private final CurveBuildingBlockBundle _block;

  /**
   * The SABR parameter data.
   */
  private final SABRInterestRateParameters _sabrInterestRateParameters;

  /**
   * Creates a calculator for a swaption.
   *
   * @param swaption the swaption to calculate values for, not null
   * @param bundle the multicurve bundle, including the curves and SABR parameters, not null
   * @param block the curve building block for the multicurve bundle, not null
   * @param sabrInterestRateParameters the SABR parameter data, not null
   */
  public SABRSwaptionCalculator(SwaptionPhysicalFixedIbor swaption,
                                SABRSwaptionProviderDiscount bundle,
                                CurveBuildingBlockBundle block,
                                SABRInterestRateParameters sabrInterestRateParameters) {

    _swaption = ArgumentChecker.notNull(swaption, "swaption");
    _bundle = ArgumentChecker.notNull(bundle, "bundle");
    _block = ArgumentChecker.notNull(block, "block");
    _sabrInterestRateParameters = ArgumentChecker.notNull(sabrInterestRateParameters, "sabrInterestRateParameters");
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(_swaption.accept(PV_CALCULATOR, _bundle));
  }

  @Override
  public Result<Double> calculateImpliedVolatility() {
    return Result.success(IMPLIED_VOL_CALCULATOR.impliedVolatility(_swaption, _bundle));
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01() {
    return Result.success(_swaption.accept(PV01_CALCULATOR, _bundle));
  }

  @Override
  public Result<MultipleCurrencyParameterSensitivity> calculateBucketedPV01() {
    return Result.success(BUCKETED_PV01_CALCULATOR.fromInstrument(_swaption, _bundle, _block).multipliedBy(BASIS_POINT_FACTOR));
  }

  @Override
  public Result<PresentValueSABRSensitivityDataBundle> calculateBucketedSABRRisk() {

    PresentValueSABRSensitivityDataBundle pvssComputed = _swaption.accept(BUCKETED_SABR_RISK_CALCULATOR, _bundle);
    return Result.success(
        SABRSensitivityNodeCalculator.calculateNodeSensitivities(pvssComputed, _sabrInterestRateParameters));
  }

  private static MarketQuoteSensitivityBlockCalculator<SABRSwaptionProviderInterface> createBucketedPV01Calculator() {

    PresentValueCurveSensitivitySABRSwaptionCalculator pvCurveSensitivityCalculator =
        PresentValueCurveSensitivitySABRSwaptionCalculator.getInstance();

    ParameterSensitivityParameterCalculator<SABRSwaptionProviderInterface> parameterSensitivityCalculator =
        new ParameterSensitivityParameterCalculator<>(pvCurveSensitivityCalculator);

    return new MarketQuoteSensitivityBlockCalculator<>(parameterSensitivityCalculator);
  }

  private static PV01CurveParametersCalculator<SABRSwaptionProviderInterface> createPV01Calculator() {
    return new PV01CurveParametersCalculator<>(PresentValueCurveSensitivitySABRSwaptionCalculator.getInstance());
  }
}
