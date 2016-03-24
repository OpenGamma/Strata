/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.view.PriceIndexValues;

public class ImmutableRatesProviderUtils {
  
  /**
   * Merge several {@link ImmutableRatesProvider} into a new one.
   * <p>
   * If the two providers have curves or time series for the same currency or index, 
   * an {@link IllegalAccessException} is thrown. 
   * The FxRateProviders is not populated with the given provider; no attempt is done on merging the embedded FX providers.
   * 
   * @param fx  the FX provider for the resulting rate provider
   * @param providers  the rates providers to be merged
   * @return the combined rates provider
   */
  public static ImmutableRatesProvider merge(FxRateProvider fx, ImmutableRatesProvider... providers) {
    ArgChecker.isTrue(providers.length > 0, "at least one provider requested");
    ImmutableRatesProvider merged = ImmutableRatesProvider.builder(providers[0].getValuationDate()).build();
    for(ImmutableRatesProvider provider: providers) {
      merged = merge(merged, provider);
    }
    return merged.toBuilder().fxRateProvider(fx).build();
  }
  
  /**
   * Merge two providers into a new one.
   * <p> 
   * If the two providers have curves or time series for the same currency or index, 
   * an {@link IllegalAccessException} is thrown. The FxRateProviders is not populated. 
   * 
   * @param provider2  the first rates provider
   * @param provider1  the second rates provider
   * @return the merged provider
   */
  private static ImmutableRatesProvider merge(
      ImmutableRatesProvider provider1,
      ImmutableRatesProvider provider2) {
    ImmutableRatesProviderBuilder merged = provider2.toBuilder();
    // Discount
    ImmutableMap<Currency, Curve> dscMap1 = provider1.getDiscountCurves();
    ImmutableMap<Currency, Curve> dscMap2 = provider2.getDiscountCurves();
    for(Entry<Currency, Curve> entry: dscMap1.entrySet()) {
      ArgChecker.isTrue(!dscMap2.containsKey(entry.getKey()), 
          "conflict on discount curve, currency {} appears twice in the providers", entry.getKey());
      merged.discountCurve(entry.getKey(), entry.getValue());
    }
    // Ibor and Overnight
    ImmutableMap<Index, Curve> indexMap1 = provider1.getIndexCurves();
    ImmutableMap<Index, Curve> indexMap2 = provider2.getIndexCurves();
    for (Entry<Index, Curve> entry : indexMap1.entrySet()) {
      ArgChecker.isTrue(!indexMap2.containsKey(entry.getKey()),
          "conflict on index curve, index {} appears twice in the providers", entry.getKey());
      if (entry.getKey() instanceof IborIndex) { // TODO: this logic should be in the builder
        merged.iborIndexCurve((IborIndex) entry.getKey(), entry.getValue());
      } else {
        merged.overnightIndexCurve((OvernightIndex) entry.getKey(), entry.getValue());
      }
    }
    // Price index
    ImmutableMap<PriceIndex, PriceIndexValues> priceMap1 = provider1.getPriceIndexValues();
    ImmutableMap<PriceIndex, PriceIndexValues> priceMap2 = provider2.getPriceIndexValues();
    for(Entry<PriceIndex, PriceIndexValues> entry: priceMap1.entrySet()) {
      ArgChecker.isTrue(!priceMap2.containsKey(entry.getKey()), 
          "conflict on price index curve, price index {} appears twice in the providers", entry.getKey());
      merged.priceIndexValues(entry.getValue());
    }
    // Time series
    Map<Index, LocalDateDoubleTimeSeries> tsMap1 = provider1.getTimeSeries();
    Map<Index, LocalDateDoubleTimeSeries> tsMap2 = provider2.getTimeSeries();
    for(Entry<Index, LocalDateDoubleTimeSeries> entry: tsMap1.entrySet()) {
      ArgChecker.isTrue(!tsMap2.containsKey(entry.getKey()), 
          "conflict on time series, index {} appears twice in the providers", entry.getKey());
      merged.timeSeries(entry.getKey(), entry.getValue());
    }
    return merged.build();
  }

}
