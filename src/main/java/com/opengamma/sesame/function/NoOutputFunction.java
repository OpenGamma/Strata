/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.sesame.MarketData;

/**
 * Function that returns nothing for outputs where there is no requirement.
 */
public final class NoOutputFunction implements OutputFunction<PositionOrTrade, Void> {

  @Override
  public Void execute(MarketData marketData, PositionOrTrade positionOrTrade) {
    // maybe this should return a sentinel value
    return null;
  }
}
