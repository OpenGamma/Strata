package com.opengamma.strata.examples.exampleccp;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;

public class MyUselessObservableMarketDataFunction {

  public static ObservableMarketDataFunction create() {
    return requirements -> ImmutableMap.of();
  }

}
