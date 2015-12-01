/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swap;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;
import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.function.calculation.rate.MarketDataUtils;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveGammaCalculator;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Calculates Gamma PV01, the second-order present value sensitivity of a {@link SwapTrade}
 * for each of a set of scenarios.
 * <p>
 * This implementation only supports calculating the measure when using a single curve for
 * discounting and forecasting.
 */
public class SwapBucketedGammaPv01Function
    extends AbstractSwapFunction<CurveCurrencyParameterSensitivities> {

  @Override
  public ScenarioResult<CurveCurrencyParameterSensitivities> execute(SwapTrade trade, CalculationMarketData marketData) {
    ExpandedSwap expandedSwap = trade.getProduct().expand();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(md -> execute(trade.getProduct(), expandedSwap, md))
        .collect(toScenarioResult());
  }

  @Override
  protected CurveCurrencyParameterSensitivities execute(ExpandedSwap product, RatesProvider provider) {
    throw new UnsupportedOperationException("execute(SwapTrade) overridden instead");
  }

  //-------------------------------------------------------------------------
  // calculate the gamma sensitivity
  private CurveCurrencyParameterSensitivities execute(
      Swap swap,
      ExpandedSwap expandedSwap,
      SingleCalculationMarketData marketData) {

    // find the curve and check it is valid
    if (swap.isCrossCurrency()) {
      throw new IllegalArgumentException("Implementation only supports a single curve, but swap is cross-currency");
    }
    Currency currency = swap.getLegs().get(0).getCurrency();
    NodalCurve nodalCurve = findNodalCurve(marketData, currency);

    // find indices and validate there is only one curve
    Set<Index> indices = swap.allIndices();
    validateSingleCurve(indices, marketData, nodalCurve);

    // calculate gamma
    CurveCurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
        nodalCurve, currency, c -> calculateCurveSensitivity(expandedSwap, currency, indices, marketData, c));
    return CurveCurrencyParameterSensitivities.of(gamma).multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
  }

  // finds the discount curve and ensures it is a NodalCurve
  private NodalCurve findNodalCurve(SingleCalculationMarketData marketData, Currency currency) {
    Curve curve = marketData.getValue(DiscountCurveKey.of(currency));
    if (!(curve instanceof NodalCurve)) {
      throw new IllegalArgumentException(Messages.format(
          "Implementation only supports nodal curves; unsupported curve type: {}", curve.getClass().getSimpleName()));
    }
    return (NodalCurve) curve;
  }

  // validates that the indices all resolve to the single specified curve
  private void validateSingleCurve(Set<Index> indices, SingleCalculationMarketData marketData, NodalCurve nodalCurve) {
    Set<MarketDataKey<?>> differentForwardCurves = indices.stream()
        .map(MarketDataKeys::indexCurve)
        .filter(k -> !nodalCurve.equals(marketData.getValue(k)))
        .collect(toSet());
    if (!differentForwardCurves.isEmpty()) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports a single curve, but discounting curve is different from " +
              "index curves for indices: {}", differentForwardCurves));
    }
  }

  // calculates the sensitivity
  private CurveCurrencyParameterSensitivity calculateCurveSensitivity(
      ExpandedSwap expandedSwap,
      Currency currency,
      Set<? extends Index> indices,
      SingleCalculationMarketData marketData,
      NodalCurve bumpedCurve) {

    RatesProvider ratesProvider = MarketDataUtils.toSingleCurveRatesProvider(marketData, currency, indices, bumpedCurve);
    PointSensitivities pointSensitivities = pricer().presentValueSensitivity(expandedSwap, ratesProvider).build();
    CurveCurrencyParameterSensitivities paramSensitivities = ratesProvider.curveParameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

}
