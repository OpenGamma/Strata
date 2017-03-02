/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fx;

import java.util.Optional;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.observable.QuoteId;

/**
 * Function which builds {@link FxRate} instances from observable market data.
 * <p>
 * {@link FxRateConfig} defines which market data is used to build the FX rates.
 */
public class FxRateMarketDataFunction implements MarketDataFunction<FxRate, FxRateId> {

  @Override
  public MarketDataRequirements requirements(FxRateId id, MarketDataConfig marketDataConfig) {
    FxRateConfig fxRateConfig = marketDataConfig.get(FxRateConfig.class, id.getObservableSource());
    Optional<QuoteId> optional = fxRateConfig.getObservableRateKey(id.getPair());
    return optional.map(key -> MarketDataRequirements.of(key)).orElse(MarketDataRequirements.empty());
  }

  @Override
  public MarketDataBox<FxRate> build(
      FxRateId id,
      MarketDataConfig marketDataConfig,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    FxRateConfig fxRateConfig = marketDataConfig.get(FxRateConfig.class, id.getObservableSource());
    Optional<QuoteId> optional = fxRateConfig.getObservableRateKey(id.getPair());
    return optional.map(key -> buildFxRate(id, key, marketData))
        .orElseThrow(() -> new IllegalArgumentException("No FX rate configuration available for " + id.getPair()));
  }

  private MarketDataBox<FxRate> buildFxRate(
      FxRateId id,
      QuoteId key,
      ScenarioMarketData marketData) {

    MarketDataBox<Double> quote = marketData.getValue(key);
    return quote.map(rate -> FxRate.of(id.getPair(), rate));
  }

  @Override
  public Class<FxRateId> getMarketDataIdType() {
    return FxRateId.class;
  }

}
