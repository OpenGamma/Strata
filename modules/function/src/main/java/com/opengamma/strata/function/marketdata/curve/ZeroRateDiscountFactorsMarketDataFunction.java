/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;

import com.opengamma.analytics.env.AnalyticsEnvironment;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.marketdata.MarketDataLookup;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.config.MarketDataConfig;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.ZeroRateDiscountFactorsId;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Market data function that builds discount factors.
 * <p>
 * This function creates an instance of {@link ZeroRateDiscountFactors} based on an underlying curve.
 * The curve is not built in this class. Instead, it is extracted from an existing {@link CurveGroup}.
 * The curve group must be available in the {@code MarketDataLookup} passed to the
 * {@link #build} method.
 * <p>
 * This builder assumes that the underlying discount curves are of type {@link YieldCurve}.
 * Note that the signature of {@code CurveGroup} allows them to be {@link YieldAndDiscountCurve},
 * but any other types would throw an exception.
 */
public class ZeroRateDiscountFactorsMarketDataFunction
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

    // find curve
    DiscountCurveId curveId = id.toCurveId();
    if (!marketData.containsValue(curveId)) {
      return Result.failure(FailureReason.MISSING_DATA, "No curve found: {}", id);
    }
    YieldCurve yieldCurve = marketData.getValue(curveId);

    // create discount factors
    return Result.of(() -> createDiscountFactors(id.getCurrency(), marketData.getValuationDate(), yieldCurve));
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
      YieldAndDiscountCurve yieldCurve) {
    
    Curve curve = Legacy.curve(yieldCurve);
    DayCount modelDayCount = Legacy.dayCount(AnalyticsEnvironment.getInstance().getModelDayCount());
    return ZeroRateDiscountFactors.of(currency, valuationDate, modelDayCount, curve);
  }

}
