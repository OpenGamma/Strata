/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.MissingMappingId;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.IndexRateId;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.PriceIndexValuesKey;
import com.opengamma.strata.market.value.PriceIndexValues;

/**
 * Test {@link MarketDataMappingsBuilder}.
 */
@Test
public class MarketDataMappingsBuilderTest {

  /**
   * Tests mapping a key to an ID where there is a mapping available for the key type.
   */
  public void mappingsAvailable() {
    CurveGroupName curveGroupName = CurveGroupName.of("curve group");
    MarketDataMappings mappings =
        MarketDataMappingsBuilder.create()
            .curveGroup(curveGroupName)
            .build();
    MarketDataId<Curve> id = mappings.getIdForKey(DiscountCurveKey.of(Currency.GBP));
    assertThat(id).isEqualTo(DiscountCurveId.of(Currency.GBP, curveGroupName));
  }

  /**
   * Tests mapping a key to an ID where there is no mapping available for the key type.
   */
  public void noMappingAvailable() {
    MarketDataMappings mappings = MarketDataMappingsBuilder.create().build();
    DiscountCurveKey key = DiscountCurveKey.of(Currency.GBP);
    MarketDataId<Curve> id = mappings.getIdForKey(key);
    assertThat(id).isEqualTo(MissingMappingId.of(key));
  }

  /**
   * Tests mapping multiple keys to IDs where there is a mapping available for one key type but not another.
   */
  public void mappingAvailableForSomeTypesButNotAll() {
    CurveGroupName curveGroupName = CurveGroupName.of("curve group");
    MarketDataMappings mappings =
        MarketDataMappingsBuilder.create()
            .curveGroup(curveGroupName)
            .build();
    PriceIndexValuesKey key = PriceIndexValuesKey.of(PriceIndices.CH_CPI);

    MarketDataId<Curve> curveId = mappings.getIdForKey(DiscountCurveKey.of(Currency.GBP));
    MarketDataId<PriceIndexValues> id = mappings.getIdForKey(key);

    assertThat(curveId).isEqualTo(DiscountCurveId.of(Currency.GBP, curveGroupName));
    assertThat(id).isEqualTo(MissingMappingId.of(key));
  }

  /**
   * Tests the correct MarketDataFeed is set when converting observable keys to IDs
   */
  public void observableMarketDataFeed() {
    MarketDataFeed feed = MarketDataFeed.of("FeedName");
    MarketDataMappings mappings = MarketDataMappingsBuilder.create(feed).build();
    MarketDataId<Double> id = mappings.getIdForObservableKey(IndexRateKey.of(IborIndices.CHF_LIBOR_12M));
    assertThat(id).isEqualTo(IndexRateId.of(IborIndices.CHF_LIBOR_12M, feed));
  }

  /**
   * Tests mapping of observable keys to IDs when no MarketDataFeed is specified
   */
  public void defaultObservablesMarketDataFeed() {
    MarketDataMappings mappings = MarketDataMappingsBuilder.create().build();
    MarketDataId<Double> id = mappings.getIdForObservableKey(IndexRateKey.of(IborIndices.CHF_LIBOR_12M));
    assertThat(id).isEqualTo(IndexRateId.of(IborIndices.CHF_LIBOR_12M, MarketDataFeed.NONE));
  }

}
