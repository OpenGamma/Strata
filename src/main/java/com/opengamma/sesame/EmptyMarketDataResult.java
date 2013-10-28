/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

public class EmptyMarketDataResult implements MarketDataResult {

  public static final MarketDataResult INSTANCE = new EmptyMarketDataResult();

  private EmptyMarketDataResult() {
  }

  @Override
  public MarketDataStatus getStatus(MarketDataRequirement requirement) {
    return MarketDataStatus.UNAVAILABLE;
  }

  @Override
  public MarketDataResult combine(MarketDataResult result) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public MarketDataValue getMarketValue(MarketDataRequirement requirement) {
    throw new IllegalStateException("Cannot return value for requirement [" + requirement +
                                        "] as it is unavailable");
  }
}
