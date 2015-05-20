package com.opengamma.strata.examples.exampleccp;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.function.OpenGammaPricingRules;

public class MyCalculationRules {

  public static CalculationRules create(MarketDataRules rules) {
    return CalculationRules.builder()
        .pricingRules(OpenGammaPricingRules.standard())
        .marketDataRules(rules)
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();
  }
}
