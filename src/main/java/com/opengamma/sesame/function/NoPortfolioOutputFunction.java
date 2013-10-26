/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.core.position.PositionOrTrade;

/**
 * Function that returns nothing for outputs where there is no requirement.
 */
public final class NoPortfolioOutputFunction implements PortfolioOutputFunction<PositionOrTrade, Void> {

  @Override
  public Void execute(PositionOrTrade positionOrTrade) {
    // maybe this should return a sentinel value
    return null;
  }
}
