/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import java.util.HashMap;
import java.util.Map;

import org.threeten.bp.ZonedDateTime;

import com.opengamma.analytics.financial.instrument.InstrumentDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivativeVisitor;
import com.opengamma.analytics.financial.provider.calculator.discounting.PV01CurveParametersCalculator;
import com.opengamma.analytics.financial.provider.calculator.generic.MarketQuoteSensitivityBlockCalculator;
import com.opengamma.analytics.financial.provider.calculator.issuer.PresentValueCurveSensitivityIssuerCalculator;
import com.opengamma.analytics.financial.provider.calculator.issuer.PresentValueIssuerCalculator;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.ParameterIssuerProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyMulticurveSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.MultipleCurrencyParameterSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.parameter.ParameterSensitivityParameterCalculator;
import com.opengamma.analytics.math.matrix.DoubleMatrix1D;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.DoubleLabelledMatrix1D;
import com.opengamma.financial.analytics.conversion.BondAndBondFutureTradeConverter;
import com.opengamma.financial.analytics.curve.CurveDefinition;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.financial.analytics.model.multicurve.MultiCurveUtils;
import com.opengamma.sesame.trade.BondTrade;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Pair;

/**
 * Discounting calculator for bond.
 */
public class DiscountingBondCalculator implements BondCalculator {

  /**
   * Calculator for PV
   */
  private static final PresentValueIssuerCalculator PVIC = PresentValueIssuerCalculator.getInstance();

  /**
   * Calculator for PV01
   */
  private static final InstrumentDerivativeVisitor<ParameterIssuerProviderInterface,
                                                   ReferenceAmount<Pair<String, Currency>>> PV01C =
      new PV01CurveParametersCalculator<>(PresentValueCurveSensitivityIssuerCalculator.getInstance());

  /** The curve sensitivity calculator */
  private static final InstrumentDerivativeVisitor<ParameterIssuerProviderInterface,
                                                   MultipleCurrencyMulticurveSensitivity> PVCSDC =
      PresentValueCurveSensitivityIssuerCalculator.getInstance();

  /** The parameter sensitivity calculator */
  private static final ParameterSensitivityParameterCalculator<ParameterIssuerProviderInterface> PSC =
      new ParameterSensitivityParameterCalculator<>(PVCSDC);

  /** The market quote sensitivity calculator */
  private static final MarketQuoteSensitivityBlockCalculator<ParameterIssuerProviderInterface> MQSBC =
      new MarketQuoteSensitivityBlockCalculator<>(PSC);

  /**
   * Provides scaling to/from basis points.
   */
  private static final double BASIS_POINT_FACTOR = 1.0E-4;

  /**
   * Derivative form of the security.
   */
  private final InstrumentDerivative _derivative;

  /**
   * The issuer and multicurve provider.
   */
  private final ParameterIssuerProviderInterface _curves;

  /**
   * Bundle of curves names and the curve building block associated.
   */
  private final CurveBuildingBlockBundle _blocks;

  /**
   * The curve definitions
   */
  private final Map<String, CurveDefinition> _curveDefinitions;

  /**
   * Creates a calculator for a InterestRateSwapSecurity.
   *
   * @param trade the bond trade to calculate values for
   * @param curves the ParameterIssuerProviderInterface
   * @param blocks the CurveBuildingBlockBundle
   * @param converter the BondAndBondFutureTradeConverter
   * @param valuationTime the ZonedDateTime
   * @param curveDefinitions the curve definitions
   */
  public DiscountingBondCalculator(BondTrade trade,
                                   ParameterIssuerProviderInterface curves,
                                   CurveBuildingBlockBundle blocks,
                                   BondAndBondFutureTradeConverter converter,
                                   ZonedDateTime valuationTime,
                                   Map<String, CurveDefinition> curveDefinitions) {
    _derivative = createInstrumentDerivative(trade, converter, valuationTime);
    _blocks = blocks;
    _curves = curves;
    _curveDefinitions = ArgumentChecker.notNull(curveDefinitions, "curveDefinitions");
  }

  @Override
  public Result<MultipleCurrencyAmount> calculatePV() {
    return Result.success(_derivative.accept(PVIC, _curves));
  }

  @Override
  public Result<BucketedCurveSensitivities> calculateBucketedPV01() {

    MultipleCurrencyParameterSensitivity sensitivity = MQSBC
        .fromInstrument(_derivative, _curves, _blocks)
        .multipliedBy(BASIS_POINT_FACTOR);
    Map<Pair<String, Currency>, DoubleLabelledMatrix1D> labelledMatrix1DMap = new HashMap<>();
    for (Map.Entry<Pair<String, Currency>, DoubleMatrix1D> entry : sensitivity.getSensitivities().entrySet()) {
      CurveDefinition curveDefinition = _curveDefinitions.get(entry.getKey().getFirst());
      DoubleLabelledMatrix1D matrix = MultiCurveUtils.getLabelledMatrix(entry.getValue(), curveDefinition);
      labelledMatrix1DMap.put(entry.getKey(), matrix);
    }
    return Result.success(BucketedCurveSensitivities.of(labelledMatrix1DMap));
  }

  @Override
  public Result<ReferenceAmount<Pair<String, Currency>>> calculatePV01() {
    return Result.success(_derivative.accept(PV01C, _curves));
  }

  private InstrumentDerivative createInstrumentDerivative(BondTrade trade,
                                                          BondAndBondFutureTradeConverter converter,
                                                          ZonedDateTime valuationTime) {
    InstrumentDefinition<?> definition = converter.convert(trade);
    return definition.toDerivative(valuationTime);
  }
}
