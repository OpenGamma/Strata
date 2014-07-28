/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bondfuture;

import static org.testng.AssertJUnit.fail;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;

import com.google.common.collect.ImmutableClassToInstanceMap;
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
 * Tests for bond future functions using the discounting calculator.
 */
@Test(groups = TestGroup.UNIT)
public class BondFutureFnTest {

  private BondFutureFn _bondFutureFn;

  @BeforeClass
  public void setUp() {

    ImmutableClassToInstanceMap<Object> components = BondMockSources.generateBaseComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);
    
    _bondFutureFn = FunctionModel.build(BondFutureFn.class, BondMockSources.getConfig(), ComponentMap.of(components));
  }

  @Test
  public void testPresentValue() {
    Result<MultipleCurrencyAmount> pvComputed = _bondFutureFn.calculatePV(BondMockSources.ENV,
                                                                          BondMockSources.BOND_FUTURE_TRADE);
    if (!pvComputed.isSuccess()) {
      fail(pvComputed.getFailureMessage());
    }
  }
}
