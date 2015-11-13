/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.rate;

import static com.opengamma.strata.collect.Guavate.toImmutableMap;

import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.calc.marketdata.SingleCalculationMarketData;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Utilities for manipulating market data.
 */
public final class MarketDataUtils {

  /**
   * Restricted constructor.
   */
  private MarketDataUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a rates provider from a set of market data containing a single discounting curve,
   * and forward curves and fixing series for a given set of indices.
   * All curves are overridden by a given replacement. 
   * 
   * @param marketData  the market data
   * @param currency  the currency of the discounting curve
   * @param indices  the indices
   * @param curveOverride  the curve override
   * @return the rates provider
   */
  public static RatesProvider toSingleCurveRatesProvider(
      SingleCalculationMarketData marketData,
      Currency currency,
      Set<? extends Index> indices,
      NodalCurve curveOverride) {

    // TODO - we should be able to replace curves more easily than having to pick out all the
    // market data into a new rates provider.

    return ImmutableRatesProvider.builder()
        .valuationDate(marketData.getValuationDate())
        .discountCurves(ImmutableMap.of(currency, curveOverride))
        .indexCurves(indices.stream()
            .collect(toImmutableMap(Function.identity(), k -> curveOverride)))
        .timeSeries(indices.stream()
            .collect(toImmutableMap(Function.identity(), k -> marketData.getTimeSeries(IndexRateKey.of(k)))))
        .build();
  }

}
