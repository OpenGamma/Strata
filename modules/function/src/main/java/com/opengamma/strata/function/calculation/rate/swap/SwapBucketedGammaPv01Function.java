/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate.swap;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static java.util.stream.Collectors.toSet;

import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.engine.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.MarketDataKeys;
import com.opengamma.strata.market.key.RateIndexCurveKey;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
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
  protected CurveCurrencyParameterSensitivities execute(
      ExpandedSwap product, SingleCalculationMarketData marketData) {
    
    Set<Currency> currencies = product.getLegs().stream()
        .map(SwapLeg::getCurrency)
        .collect(toSet());
    if (currencies.size() > 1) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports a single curve, but swap is cross-currency: {}", currencies));
    }
    Currency currency = Iterables.getOnlyElement(currencies);
    
    ImmutableSet.Builder<Index> indexBuilder = ImmutableSet.builder();
    product.getLegs().stream().forEach(leg -> leg.collectIndices(indexBuilder));
    Set<Index> indices = indexBuilder.build();
    Set<RateIndexCurveKey> indexCurveKeys =
        indices.stream()
            .map(MarketDataKeys::indexCurve)
            .collect(toImmutableSet());

    Curve curve = marketData.getValue(DiscountCurveKey.of(currency));
    if (!(curve instanceof NodalCurve)) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports nodal curves; unsupported curve type: {}", curve.getClass().getSimpleName()));
    }
    NodalCurve nodalCurve = (NodalCurve) curve;
    
    Set<RateIndexCurveKey> differentForwardCurves = indexCurveKeys.stream()
        .filter(k -> !curve.equals(marketData.getValue(k)))
        .collect(toSet());
    if (!differentForwardCurves.isEmpty()) {
      throw new IllegalArgumentException(
          Messages.format("Implementation only supports a single curve, but discounting curve is different from " +
              "index curves for indices: {}", differentForwardCurves));
    }
    
    CurveCurrencyParameterSensitivity gamma = CurveGammaCalculator.DEFAULT.calculateSemiParallelGamma(
        nodalCurve,
        currency,
        c -> getCurveSensitivity(product, currency, indices, marketData, c));
    return CurveCurrencyParameterSensitivities.of(gamma);
  }

  @Override
  protected CurveCurrencyParameterSensitivities execute(ExpandedSwap product, RatesProvider provider) {
    throw new UnsupportedOperationException();
  }
  
  //-------------------------------------------------------------------------
  private CurveCurrencyParameterSensitivity getCurveSensitivity(
      ExpandedSwap product,
      Currency currency,
      Set<Index> indices,
      SingleCalculationMarketData marketData,
      NodalCurve bumpedCurve) {
    
    RatesProvider bumpedRatesProvider = ImmutableRatesProvider.builder()
        .valuationDate(marketData.getValuationDate())
        .dayCount(DayCounts.ACT_ACT_ISDA)
        .discountCurves(ImmutableMap.of(currency, bumpedCurve))
        .indexCurves(indices.stream()
            .collect(toImmutableMap(Function.identity(), k -> bumpedCurve)))
        .timeSeries(indices.stream()
            .collect(toImmutableMap(Function.identity(), k -> marketData.getTimeSeries(IndexRateKey.of(k)))))
        .build();
    PointSensitivities pointSensitivities = pricer().presentValueSensitivity(product, bumpedRatesProvider).build();
    CurveCurrencyParameterSensitivities paramSensitivities = bumpedRatesProvider.curveParameterSensitivity(pointSensitivities);
    return Iterables.getOnlyElement(paramSensitivities.getSensitivities());
  }

}
