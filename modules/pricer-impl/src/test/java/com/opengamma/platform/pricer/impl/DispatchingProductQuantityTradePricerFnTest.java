/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.platform.finance.MockSimpleProduct.MOCK1_SECURITY;
import static org.mockito.Mockito.mock;

import org.testng.annotations.Test;

import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.MockSimpleProduct;
import com.opengamma.platform.finance.QuantityTrade;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Test.
 */
@Test
public class DispatchingProductQuantityTradePricerFnTest {

  private static final PricingEnvironment MOCK_ENV = mock(PricingEnvironment.class);

  public void test_presentValue_unknownType() {
    QuantityTrade<MockSimpleProduct> mockTrade =
        QuantityTrade.builder(MOCK1_SECURITY).standardId(StandardId.of("OG-Trade", "1")).build();
    DispatchingProductQuantityTradePricerFn test = DispatchingProductQuantityTradePricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(MOCK_ENV, mockTrade));
  }

}
