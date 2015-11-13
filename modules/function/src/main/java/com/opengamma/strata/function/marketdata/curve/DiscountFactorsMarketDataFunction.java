/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.marketdata.MarketDataLookup;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.id.DiscountCurveId;
import com.opengamma.strata.market.id.DiscountFactorsId;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.SimpleDiscountFactors;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.market.value.ZeroRateDiscountFactors;

/**
 * Market data function that builds discount factors.
 * <p>
 * This function creates an instance of {@link ZeroRateDiscountFactors} or {@link SimpleDiscountFactors}
 * based on an underlying curve. The type is chosen based on the {@linkplain ValueType value type} held in
 * the {@linkplain CurveMetadata#getYValueType() y-value metadata}.
 * <p>
 * The curve is not actually built in this class, it is extracted from an existing {@link CurveGroup}.
 * The curve group must be available in the {@code MarketDataLookup} passed to the {@link #build} method.
 */
public class DiscountFactorsMarketDataFunction
    implements MarketDataFunction<DiscountFactors, DiscountFactorsId> {

  @Override
  public MarketDataRequirements requirements(DiscountFactorsId id, MarketDataConfig config) {
    DiscountCurveId curveId = DiscountCurveId.of(id.getCurrency(), id.getCurveGroupName(), id.getMarketDataFeed());
    return MarketDataRequirements.builder()
        .addValues(curveId)
        .build();
  }

  @Override
  public Result<MarketDataBox<DiscountFactors>> build(
      DiscountFactorsId id,
      MarketDataLookup marketData,
      MarketDataConfig config) {

    // find curve
    DiscountCurveId curveId = DiscountCurveId.of(id.getCurrency(), id.getCurveGroupName(), id.getMarketDataFeed());
    if (!marketData.containsValue(curveId)) {
      return Result.failure(
          FailureReason.MISSING_DATA,
          "No discount curve found: Currency: {}, Group: {}, Feed: {}",
          id.getCurrency(),
          id.getCurveGroupName(),
          id.getMarketDataFeed());
    }
    MarketDataBox<Curve> curveBox = marketData.getValue(curveId);
    MarketDataBox<LocalDate> valDateBox = marketData.getValuationDate();
    return curveBox.combineWith(valDateBox, (curve, valDate) -> createDiscountFactors(id.getCurrency(), valDate, curve));
  }

  @Override
  public Class<DiscountFactorsId> getMarketDataIdType() {
    return DiscountFactorsId.class;
  }

  // create the instance of DiscountFactors
  private Result<DiscountFactors> createDiscountFactors(
      Currency currency, 
      LocalDate valuationDate, 
      Curve curve) {
    
    ValueType yValueType = curve.getMetadata().getYValueType();
    if (ValueType.ZERO_RATE.equals(yValueType)) {
      return Result.success(ZeroRateDiscountFactors.of(currency, valuationDate, curve));

    } else if (ValueType.DISCOUNT_FACTOR.equals(yValueType)) {
      return Result.success(SimpleDiscountFactors.of(currency, valuationDate, curve));

    } else {
      return Result.failure(
          FailureReason.MISSING_DATA,
          "Invalid curve, must have ValueType of 'ZeroRate' or 'DiscountFactor', but was: {}",
          yValueType);
    }
  }

}
