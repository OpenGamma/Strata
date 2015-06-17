/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import static com.opengamma.strata.engine.calculations.function.FunctionUtils.toScenarioResult;
import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.calculation.rate.MarketDataUtils;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.market.key.RateIndexCurveKey;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.CurveGammaCalculator;

/**
 * Calculates Gamma PV01, the second-order present value sensitivity of a {@link SwapTrade}
 * for each of a set of scenarios.
 * <p>
 * This implementation only supports calculating the measure when using a single curve for
 * discounting and forecasting.
 */
public class SwapBucketedGammaPv01Function
    extends AbstractSwapFunction<CurveCurrencyParameterSensitivities>{
  
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
    throw new UnsupportedOperationException();
  }
  
  //-------------------------------------------------------------------------
  private CurveCurrencyParameterSensitivities execute(
      Swap swap, ExpandedSwap expandedSwap, SingleCalculationMarketData marketData) {
    
    Set<Currency> currencies = expandedSwap.getLegs().stream()
        .map(SwapLeg::getCurrency)
        .collect(toSet());
    if (currencies.size() > 1) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports a single curve, but swap is cross-currency: {}", currencies));
    }
    Currency currency = Iterables.getOnlyElement(currencies);

    Curve curve = marketData.getValue(DiscountCurveKey.of(currency));
    if (!(curve instanceof NodalCurve)) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports nodal curves; unsupported curve type: {}", curve.getClass().getSimpleName()));
    }
    NodalCurve nodalCurve = (NodalCurve) curve;

    Set<Index> indices = swap.allIndices();
    Set<RateIndexCurveKey> differentForwardCurves = indices.stream()
        .map(MarketDataKeys::indexCurve)
        .filter(k -> !curve.equals(marketData.getValue(k)))
        .collect(toSet());
    if (!differentForwardCurves.isEmpty()) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports a single curve, but discounting curve is different from " +
              "index curves for indices: {}", differentForwardCurves));
    }
    
    CurveCurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
        nodalCurve, currency,
        c -> getCurveSensitivity(expandedSwap, currency, indices, marketData, c));
    return CurveCurrencyParameterSensitivities.of(gamma).multipliedBy(ONE_BASIS_POINT * ONE_BASIS_POINT);
  }
  
  private CurveCurrencyParameterSensitivity getCurveSensitivity(
      ExpandedSwap expandedSwap,
      Currency currency,
      Set<Index> indices,
      SingleCalculationMarketData marketData,
      NodalCurve bumpedCurve) {
    
    RatesProvider ratesProvider = MarketDataUtils.toSingleCurveRatesProvider(marketData, currency, indices, bumpedCurve);
    PointSensitivities pointSensitivities = pricer().presentValueSensitivity(expandedSwap, ratesProvider).build();
    CurveCurrencyParameterSensitivities paramSensitivities = ratesProvider.curveParameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

}
