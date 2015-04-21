/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.engine.calculations.MissingMappingId;
import com.opengamma.strata.marketdata.id.DiscountingCurveId;
import com.opengamma.strata.marketdata.id.IndexRateId;
import com.opengamma.strata.marketdata.id.MarketDataFeed;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.marketdata.key.FxRateKey;
import com.opengamma.strata.marketdata.key.IndexRateKey;

@Test
public class MarketDataMappingsTest {

  /**
   * Tests mapping a key to an ID where there is a mapping available for the key type.
   */
  public void mappingsAvailable() {
    String curveGroupName = "curve group";
    MarketDataMappings mappings =
        MarketDataMappings.builder()
            .curveGroup(curveGroupName)
            .build();
    MarketDataId<YieldCurve> id = mappings.getIdForKey(DiscountingCurveKey.of(Currency.GBP));
    assertThat(id).isEqualTo(DiscountingCurveId.of(Currency.GBP, curveGroupName));
  }

  /**
   * Tests mapping a key to an ID where there is no mapping available for the key type.
   */
  public void noMappingAvailable() {
    MarketDataMappings mappings = MarketDataMappings.builder().build();
    DiscountingCurveKey key = DiscountingCurveKey.of(Currency.GBP);
    MarketDataId<YieldCurve> id = mappings.getIdForKey(key);
    assertThat(id).isEqualTo(MissingMappingId.of(key));
  }

  /**
   * Tests mapping multiple keys to IDs where there is a mapping available for one key type but not another.
   */
  public void mappingAvailableForSomeTypesButNotAll() {
    String curveGroupName = "curve group";
    MarketDataMappings mappings =
        MarketDataMappings.builder()
            .curveGroup(curveGroupName)
            .build();
    FxRateKey fxKey = FxRateKey.of(Currency.EUR, Currency.USD);

    MarketDataId<YieldCurve> curveId = mappings.getIdForKey(DiscountingCurveKey.of(Currency.GBP));
    MarketDataId<Double> fxId = mappings.getIdForKey(fxKey);

    assertThat(curveId).isEqualTo(DiscountingCurveId.of(Currency.GBP, curveGroupName));
    assertThat(fxId).isEqualTo(MissingMappingId.of(fxKey));
  }

  /**
   * Tests the correct MarketDataFeed is set when converting observable keys to IDs
   */
  public void observableMarketDataFeed() {
    MarketDataFeed feed = MarketDataFeed.of("FeedName");
    MarketDataMappings mappings = MarketDataMappings.builder().marketDataFeed(feed).build();
    MarketDataId<Double> id = mappings.getIdForObservableKey(IndexRateKey.of(IborIndices.CHF_LIBOR_12M));
    assertThat(id).isEqualTo(IndexRateId.of(IborIndices.CHF_LIBOR_12M, feed));
  }

  /**
   * Tests mapping of observable keys to IDs when no MarketDataFeed is specified
   */
  public void defaultObservablesMarketDataFeed() {
    MarketDataMappings mappings = MarketDataMappings.builder().build();
    MarketDataId<Double> id = mappings.getIdForObservableKey(IndexRateKey.of(IborIndices.CHF_LIBOR_12M));
    assertThat(id).isEqualTo(IndexRateId.of(IborIndices.CHF_LIBOR_12M, MarketDataFeed.NONE));
  }
}
