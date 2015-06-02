package com.opengamma.strata.examples.exampleccp.uselessboilerplate;

import com.opengamma.strata.engine.marketdata.BaseMarketData;

import java.time.LocalDate;

public class MyUselessBaseMarketData {

  public static BaseMarketData create(LocalDate valuationDate) {
    return BaseMarketData.empty(valuationDate);
  }

}
