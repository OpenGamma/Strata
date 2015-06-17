/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import com.opengamma.strata.examples.finance.credit.ExampleTradeSource;
import com.opengamma.strata.examples.report.TradePortfolio;
import org.joda.beans.ser.JodaBeanSer;

/**
 * Demonstrate use of the API for credit default swaps.
 * <p>
 * This class exists for demonstration purposes to aid with understanding credit default swaps.
 * It is not intended to be used in a production environment.
 */
public class CdsTradeModelDemo {

  /**
   * Launch demo, no arguments needed.
   *
   * @param args no arguments needed
   */
  public static void main(String[] args) {

    TradePortfolio trades = TradePortfolio.of(
        ExampleTradeSource.of().trades()
    );

    System.out.println(JodaBeanSer.PRETTY.xmlWriter().write(trades));

  }

}
