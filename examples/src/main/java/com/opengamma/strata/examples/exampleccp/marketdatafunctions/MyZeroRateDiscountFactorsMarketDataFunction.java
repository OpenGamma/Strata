/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.exampleccp.marketdatafunctions;

import java.time.LocalDate;

import com.opengamma.analytics.env.AnalyticsEnvironment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.examples.exampleccp.curves.MyCurves;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.id.ZeroRateDiscountFactorsId;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Market data function that builds discount factors.
 * <p>
 * This function creates an instance of {@link ZeroRateDiscountFactors} based on an underlying curve.
 * The curve is hardcoded to be build here.
 */
public class MyZeroRateDiscountFactorsMarketDataFunction
    implements MarketDataFunction<DiscountFactors, ZeroRateDiscountFactorsId> {

  @Override
  public MarketDataRequirements requirements(ZeroRateDiscountFactorsId id, MarketDataConfig marketDataConfig) {
    return MarketDataRequirements.builder()
        .addValues(id.toCurveId())
        .build();
  }

  @Override
  public Result<DiscountFactors> build(
      ZeroRateDiscountFactorsId id,
      MarketDataLookup marketData,
      MarketDataConfig marketDataConfig) {

    // create curve
    Curve curve = MyCurves.oisCurveDiscount();

    // create discount factors
    return Result.of(() -> createDiscountFactors(id.getCurrency(), marketData.getValuationDate(), curve));
  }

  @Override
  public Class<ZeroRateDiscountFactorsId> getMarketDataIdType() {
    return ZeroRateDiscountFactorsId.class;
  }

  //-------------------------------------------------------------------------
  // create the instance of ZeroRateDiscountFactors
  private DiscountFactors createDiscountFactors(
      Currency currency,
      LocalDate valuationDate,
      Curve curve) {

    DayCount modelDayCount = Legacy.dayCount(AnalyticsEnvironment.getInstance().getModelDayCount());
    return ZeroRateDiscountFactors.of(currency, valuationDate, modelDayCount, curve);
  }

  public static MarketDataFunction<?, ?> create() {
    return new MyZeroRateDiscountFactorsMarketDataFunction();
  }

}
