/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.financial.security.equity.EquitySecurity;

public class EquityPresentValue implements EquityPresentValueFunction {

  @Override
  public FunctionResult<Double> presentValue(MarketData marketData, EquitySecurity security) {

    MarketDataFunctionResult result = marketData.retrieveItem(
        StandardMarketDataRequirement.of(security, MarketDataRequirementNames.MARKET_VALUE));

    // todo remove the nasty cast
    return result.isFullyAvailable() ?
        result.generateSuccessResult((Double) result.getSingleMarketDataValue().getValue()) :
        result.<Double>generateFailureResult();
  }
}
