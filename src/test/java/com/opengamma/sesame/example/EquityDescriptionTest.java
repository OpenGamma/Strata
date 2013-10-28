/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.overrides;
import static org.testng.AssertJUnit.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.EmptyMarketData;
import com.opengamma.sesame.StandardResultGenerator;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.graph.FunctionTree;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class EquityDescriptionTest {

  private static final Map<Class<?>, Object> INFRASTRUCTURE = Collections.emptyMap();
  private static final EquitySecurity SECURITY;
  private static final String SECURITY_NAME = "Apple Equity";
  private static final String BLOOMBERG_VALUE = "AAPL US Equity";
  private static final String ACTIV_VALUE = "AAPL.";

  static {
    SECURITY = new EquitySecurity("Exchange Name", "EXH", "Apple", Currency.USD);
    ExternalId bbg = ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, BLOOMBERG_VALUE);
    ExternalId activ = ExternalId.of(ExternalSchemes.ACTIVFEED_TICKER, ACTIV_VALUE);
    SECURITY.setExternalIdBundle(ExternalIdBundle.of(bbg, activ));
    SECURITY.setName(SECURITY_NAME);
  }

  @Test
  public void defaultImpl() {
    FunctionTree<EquityDescriptionFunction> functionTree = FunctionTree.forFunction(EquityDescriptionFunction.class);
    EquityDescriptionFunction fn = functionTree.build(INFRASTRUCTURE);
    String description = fn.execute(createEmptyMarketData(), SECURITY);
    assertEquals(description, SECURITY_NAME);
  }

  @Test
  public void idImplDefaultArgs() {
    FunctionConfig config = config(overrides(EquityDescriptionFunction.class, EquityIdDescription.class));
    FunctionTree<EquityDescriptionFunction> functionTree = FunctionTree.forFunction(EquityDescriptionFunction.class,
                                                                                    config);
    EquityDescriptionFunction fn = functionTree.build(INFRASTRUCTURE);
    String description = fn.execute(createEmptyMarketData(), SECURITY);
    assertEquals(description, BLOOMBERG_VALUE);
  }

  @Test
  public void idImplOverriddenArgs() {
    FunctionConfig config =
        config(overrides(EquityDescriptionFunction.class, EquityIdDescription.class),
               arguments(
                   function(IdScheme.class,
                            argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))));
    FunctionTree<EquityDescriptionFunction> functionTree = FunctionTree.forFunction(EquityDescriptionFunction.class,
                                                                                    config);
    EquityDescriptionFunction fn = functionTree.build(INFRASTRUCTURE);
    String description = fn.execute(createEmptyMarketData(), SECURITY);
    assertEquals(description, ACTIV_VALUE);
  }

  private EmptyMarketData createEmptyMarketData() {
    return new EmptyMarketData(new StandardResultGenerator());
  }
}
