/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.SimpleMarketDataKey;

@Test
public class DefaultMarketDataMappingsTest {

  /**
   * Test that keys that implement SimpleMarketDataKey are converted to IDs without needing a mapping.
   */
  public void simpleMarketDataKey() {
    MarketDataFeed marketDataFeed = MarketDataFeed.of("testFeed");
    MarketDataMappings mappings = MarketDataMappings.of(marketDataFeed);
    MarketDataId<String> id = mappings.getIdForKey(new Key());
    assertThat(id).isInstanceOf(Id.class);
    assertThat(((Id) id).marketDataFeed).isEqualTo(marketDataFeed);
  }

  /**
   * A market data key that implements SimpleMarketDataKey and can be converted to a market data ID without
   * needing any data apart from the MarketDataFeed.
   */
  private static final class Key implements SimpleMarketDataKey<String> {

    @Override
    public MarketDataId<String> toMarketDataId(MarketDataFeed marketDataFeed) {
      return new Id(marketDataFeed);
    }

    @Override
    public Class<String> getMarketDataType() {
      return String.class;
    }
  }

  /**
   * A test market data ID.
   */
  private static final class Id implements MarketDataId<String> {

    private final MarketDataFeed marketDataFeed;

    private Id(MarketDataFeed marketDataFeed) {
      this.marketDataFeed = marketDataFeed;
    }

    @Override
    public Class<String> getMarketDataType() {
      return String.class;
    }
  }
}
