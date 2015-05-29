/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.function.marketdata.MarketDataTestUtils.CURVE_GROUP_NAME;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.collect.CollectProjectAssertions;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.function.marketdata.MarketDataTestUtils;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.ZeroRateDiscountFactorsId;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Test {@link ZeroRateDiscountFactorsMarketDataFunction}.
 */
@Test
public class ZeroRateDiscountFactorsMarketDataFunctionTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_build() {
    YieldCurve curve = MarketDataTestUtils.discountingCurve(1, AUD, MarketDataTestUtils.curveGroup());
    DiscountCurveId curveId = DiscountCurveId.of(AUD, CURVE_GROUP_NAME, FEED);
    BaseMarketData marketData = BaseMarketData.builder(VAL_DATE)
        .addValue(curveId, curve)
        .build();
    ZeroRateDiscountFactorsMarketDataFunction builder = new ZeroRateDiscountFactorsMarketDataFunction();

    DiscountFactors expected1 = ZeroRateDiscountFactors.of(AUD, VAL_DATE, ACT_ACT_ISDA, Legacy.curve(curve));

    ZeroRateDiscountFactorsId dfId = ZeroRateDiscountFactorsId.of(AUD, CURVE_GROUP_NAME, FEED);
    Result<DiscountFactors> result = builder.build(dfId, marketData, MarketDataConfig.empty());
    CollectProjectAssertions.assertThat(result).hasValue(expected1);
  }

}
