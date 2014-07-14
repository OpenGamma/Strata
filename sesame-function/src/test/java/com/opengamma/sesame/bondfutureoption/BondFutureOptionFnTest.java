/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfutureoption;

import static org.testng.AssertJUnit.fail;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;

import com.google.common.collect.ImmutableMap;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.sources.BondMockSources;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * Test for bond future options using the black calculator.
 */
@Test(groups = TestGroup.UNIT)
public class BondFutureOptionFnTest {

  private BondFutureOptionFn _bondFutureOptionFn;
  
  @BeforeClass
  public void setUp() {

    ImmutableMap<Class<?>, Object> components = BondMockSources.generateBaseComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);
    
    _bondFutureOptionFn = FunctionModel.build(BondFutureOptionFn.class,
                                              BondMockSources.getConfig(),
                                              ComponentMap.of(components));
  }

  @Test
  public void testPresentValue() {
    Result<MultipleCurrencyAmount> pvComputed = _bondFutureOptionFn.calculatePV(BondMockSources.ENV,
                                                                                BondMockSources.BOND_FUTURE_OPTION_TRADE);
    if (!pvComputed.isSuccess()) {
      fail(pvComputed.getFailureMessage());
    }
  }
}
