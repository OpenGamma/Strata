/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.bond;

import static org.testng.AssertJUnit.fail;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.threeten.bp.Instant;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.util.amount.ReferenceAmount;
import com.opengamma.financial.analytics.model.fixedincome.BucketedCurveSensitivities;
import com.opengamma.service.ServiceContext;
import com.opengamma.service.ThreadLocalServiceContext;
import com.opengamma.service.VersionCorrectionProvider;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.engine.FixedInstantVersionCorrectionProvider;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.sources.BondMockSources;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.tuple.Pair;

/**
 * Tests for bond future functions using the discounting calculator.
 */
@Test(groups = TestGroup.UNIT)
public class BondFnTest {

  private BondFn _bondFn;

  @BeforeClass
  public void setUp() {

    ImmutableMap<Class<?>, Object> components = BondMockSources.generateBaseComponents();
    VersionCorrectionProvider vcProvider = new FixedInstantVersionCorrectionProvider(Instant.now());
    ServiceContext serviceContext = ServiceContext.of(components).with(VersionCorrectionProvider.class, vcProvider);
    ThreadLocalServiceContext.init(serviceContext);

    _bondFn = FunctionModel.build(BondFn.class, BondMockSources.getConfig(), ComponentMap.of(components));
  }

  @Test
  public void testPresentValue() {
    Result<MultipleCurrencyAmount> pvComputed = _bondFn.calculatePV(BondMockSources.ENV,
                                                                    BondMockSources.BOND_TRADE);
    if (!pvComputed.isSuccess()) {
      fail(pvComputed.getFailureMessage());
    }
  }

  @Test
  public void testPV01() {
    Result<ReferenceAmount<Pair<String,Currency>>> pvComputed = _bondFn.calculatePV01(BondMockSources.ENV,
                                                                                      BondMockSources.BOND_TRADE);
    if (!pvComputed.isSuccess()) {
      fail(pvComputed.getFailureMessage());
    }
  }

  @Test
  public void testBucketedPV01() {
    Result<BucketedCurveSensitivities> pvComputed = _bondFn.calculateBucketedPV01(BondMockSources.ENV,
                                                                                  BondMockSources.BOND_TRADE);
    if (!pvComputed.isSuccess()) {
      fail(pvComputed.getFailureMessage());
    }
  }

}
