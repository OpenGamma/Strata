package com.opengamma.strata.examples.exampleccp.marketdatarules;

import com.opengamma.strata.engine.config.MarketDataRule;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.function.marketdata.mapping.MarketDataMappingsBuilder;
import com.opengamma.strata.market.curve.CurveGroupName;

public class MyMarketDataRules {

  public static MarketDataRules create() {
    return MarketDataRules.of(
        MarketDataRule.anyTarget(
            MarketDataMappingsBuilder
                .create()
                .curveGroup(CurveGroupName.of("Default"))
                .build()
        )
    );
  }

}
