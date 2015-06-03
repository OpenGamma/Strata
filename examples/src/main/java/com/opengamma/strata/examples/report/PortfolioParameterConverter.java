/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;


/**
 * Parameter converter for {@link TradePortfolio}.
 */
public class PortfolioParameterConverter extends JodaBeanParameterConverter<TradePortfolio> {

  @Override
  Class<TradePortfolio> getExpectedType() {
    return TradePortfolio.class;
  }

}
