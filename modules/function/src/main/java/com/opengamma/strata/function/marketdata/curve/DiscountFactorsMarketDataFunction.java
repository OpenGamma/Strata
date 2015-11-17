/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.function.MarketDataFunction;
import com.opengamma.strata.calc.marketdata.scenario.MarketDataBox;
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
  public MarketDataBox<DiscountFactors> build(
      DiscountFactorsId id,
      CalculationEnvironment marketData,
      MarketDataConfig config) {

    // find curve
    DiscountCurveId curveId = DiscountCurveId.of(id.getCurrency(), id.getCurveGroupName(), id.getMarketDataFeed());
    MarketDataBox<Curve> curveBox = marketData.getValue(curveId);
    MarketDataBox<LocalDate> valDateBox = marketData.getValuationDate();
    return curveBox.combineWith(valDateBox, (curve, valDate) -> createDiscountFactors(id.getCurrency(), valDate, curve));
  }

  @Override
  public Class<DiscountFactorsId> getMarketDataIdType() {
    return DiscountFactorsId.class;
  }

  // create the instance of DiscountFactors
  private DiscountFactors createDiscountFactors(
      Currency currency, 
      LocalDate valuationDate, 
      Curve curve) {
    
    return DiscountFactors.of(currency, valuationDate, curve);
  }

}
