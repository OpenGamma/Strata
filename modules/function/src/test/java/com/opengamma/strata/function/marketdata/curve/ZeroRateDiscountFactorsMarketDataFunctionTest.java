/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketEnvironment;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.ZeroRateDiscountFactorsId;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

/**
 * Test {@link ZeroRateDiscountFactorsMarketDataFunction}.
 */
@Test
public class ZeroRateDiscountFactorsMarketDataFunctionTest {

  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final MarketDataFeed FEED = MarketDataFeed.of("Feed");

  //-------------------------------------------------------------------------
  public void test_build() {
    Curve curve = ConstantNodalCurve.of(DefaultCurveMetadata.of("AUD Discounting", ACT_ACT_ISDA), (double) 1);
    CurveGroupName curveGroupName = CurveGroupName.of("groupName");
    DiscountCurveId curveId = DiscountCurveId.of(AUD, curveGroupName, FEED);
    MarketEnvironment marketData = MarketEnvironment.builder(VAL_DATE)
        .addValue(curveId, curve)
        .build();
    ZeroRateDiscountFactorsMarketDataFunction builder = new ZeroRateDiscountFactorsMarketDataFunction();

    DiscountFactors expected1 = ZeroRateDiscountFactors.of(AUD, VAL_DATE, curve);

    ZeroRateDiscountFactorsId dfId = ZeroRateDiscountFactorsId.of(AUD, curveGroupName, FEED);
    Result<DiscountFactors> result = builder.build(dfId, marketData, MarketDataConfig.empty());
    assertThat(result).hasValue(expected1);
  }

}
