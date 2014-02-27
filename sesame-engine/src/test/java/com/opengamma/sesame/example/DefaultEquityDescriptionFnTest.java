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
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class DefaultEquityDescriptionFnTest {

  private static final EquitySecurity SECURITY;
  private static final String SECURITY_NAME = "Apple Equity";
  private static final String BLOOMBERG_VALUE = "AAPL US Equity";
  private static final String ACTIV_VALUE = "AAPL.";
  private static final FunctionMetadata METADATA =
      EngineUtils.createMetadata(EquityDescriptionFn.class, "getDescription");

  static {
    SECURITY = new EquitySecurity("Exchange Name", "EXH", "Apple", Currency.USD);
    ExternalId bbg = ExternalId.of(ExternalSchemes.BLOOMBERG_TICKER, BLOOMBERG_VALUE);
    ExternalId activ = ExternalId.of(ExternalSchemes.ACTIVFEED_TICKER, ACTIV_VALUE);
    SECURITY.setExternalIdBundle(ExternalIdBundle.of(bbg, activ));
    SECURITY.setName(SECURITY_NAME);
  }


  @Test
  public void defaultImpl() {
    FunctionModelConfig config = config(implementations(EquityDescriptionFn.class, DefaultEquityDescriptionFn.class));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    EquityDescriptionFn fn = (EquityDescriptionFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    String description = fn.getDescription(SECURITY);
    assertEquals(description, SECURITY_NAME);
  }

  @Test
  public void idImplOverriddenArgs() {
    FunctionModelConfig config =
        config(implementations(EquityDescriptionFn.class, EquityIdDescriptionFn.class,
                               IdSchemeFn.class, DefaultIdSchemeFn.class),
               arguments(
                   function(DefaultIdSchemeFn.class,
                            argument("scheme", ExternalSchemes.ACTIVFEED_TICKER))));
    FunctionModel functionModel = FunctionModel.forFunction(METADATA, config);
    EquityDescriptionFn fn = (EquityDescriptionFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    String description = fn.getDescription(SECURITY);
    assertEquals(description, ACTIV_VALUE);
  }
}
