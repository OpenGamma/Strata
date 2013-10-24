/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.id.ExternalSchemes;
import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.Injector;
import com.opengamma.util.money.Currency;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class EquityDescriptionTest {

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
    Injector injector = new Injector();
    EquityDescriptionFunction fn = injector.create(EquityDescriptionFunction.class);
    String description = fn.getDescription(SECURITY);
    assertEquals(description, SECURITY_NAME);
  }

  @Test
  public void idImplDefaultArgs() {
    Injector injector = new Injector();
    Map<Class<?>, Class<?>> typeMap = ImmutableMap.<Class<?>, Class<?>>of(EquityDescriptionFunction.class,
                                                                          EquityIdDescription.class);
    Map<Class<?>, FunctionArguments> argsMap = Collections.emptyMap();
    FunctionConfig config = new FunctionConfig(typeMap, argsMap);
    EquityDescriptionFunction fn = injector.create(EquityDescriptionFunction.class, config);
    String description = fn.getDescription(SECURITY);
    assertEquals(description, BLOOMBERG_VALUE);
  }

  @Test
  public void idImplOverriddenArgs() {
    Injector injector = new Injector();
    Map<Class<?>, Class<?>> typeMap = ImmutableMap.<Class<?>, Class<?>>of(EquityDescriptionFunction.class,
                                                                          EquityIdDescription.class);
    Map<String, Object> argsMap = ImmutableMap.<String, Object>of("scheme", ExternalSchemes.ACTIVFEED_TICKER);
    FunctionArguments fnArgs = new FunctionArguments(argsMap);
    Map<Class<?>, FunctionArguments> args = ImmutableMap.<Class<?>, FunctionArguments>of(IdScheme.class, fnArgs);
    FunctionConfig config = new FunctionConfig(typeMap, args);
    EquityDescriptionFunction fn = injector.create(EquityDescriptionFunction.class, config);
    String description = fn.getDescription(SECURITY);
    assertEquals(description, ACTIV_VALUE);
  }
}
