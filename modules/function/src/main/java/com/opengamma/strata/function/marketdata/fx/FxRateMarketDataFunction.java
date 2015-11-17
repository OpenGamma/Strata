/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.fx;

import java.util.Optional;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.market.key.QuoteKey;

/**
 * Function which builds {@link FxRate} instances from observable market data.
 * <p>
 * {@link FxRateConfig} defines which market data is used to build the FX rates.
 */
public class FxRateMarketDataFunction implements MarketDataFunction<FxRate, FxRateId> {

  @Override
  public MarketDataRequirements requirements(FxRateId id, MarketDataConfig marketDataConfig) {
    FxRateConfig fxRateConfig = marketDataConfig.get(FxRateConfig.class);
    Optional<QuoteKey> optional = fxRateConfig.getObservableRateKey(id.getPair());
    MarketDataFeed feed = id.getMarketDataFeed();
    return optional.map(key -> MarketDataRequirements.of(key.toMarketDataId(feed))).orElse(MarketDataRequirements.empty());
  }

  @Override
  public MarketDataBox<FxRate> build(FxRateId id, CalculationEnvironment marketData, MarketDataConfig marketDataConfig) {
    FxRateConfig fxRateConfig = marketDataConfig.get(FxRateConfig.class);
    Optional<QuoteKey> optional = fxRateConfig.getObservableRateKey(id.getPair());
    MarketDataFeed feed = id.getMarketDataFeed();
    return optional.map(key -> buildFxRate(id, key, feed, marketData))
        .orElseThrow(() -> new IllegalArgumentException("No FX rate configuration available for " + id.getPair()));
  }

  private MarketDataBox<FxRate> buildFxRate(
      FxRateId id,
      QuoteKey key,
      MarketDataFeed feed,
      CalculationEnvironment marketData) {

    MarketDataBox<Double> quote = marketData.getValue(key.toMarketDataId(feed));
    return quote.apply(rate -> FxRate.of(id.getPair(), rate));
  }

  @Override
  public Class<FxRateId> getMarketDataIdType() {
    return FxRateId.class;
  }
}
