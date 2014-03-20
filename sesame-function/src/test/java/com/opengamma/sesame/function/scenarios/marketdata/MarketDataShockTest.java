/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.marketdata;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class MarketDataShockTest {

  private static final String SCHEME = "scheme";
  private static final String VALUE = "value";
  private static final MarketDataMatcher MATCHER = MarketDataMatcher.idEquals(SCHEME, VALUE);
  private static final ExternalIdBundle MATCHING_ID = ExternalIdBundle.of("scheme", "value");
  private static final ExternalIdBundle NON_MATCHING_ID = ExternalIdBundle.of("scheme", "differentValue");
  private static final double DELTA = 0.00000001;

  @Test
  public void absoluteShift() {
    MarketDataShock shockUp = MarketDataShock.absoluteShift(2d, MATCHER);
    assertEquals(5d, shockUp.apply(MATCHING_ID, 3d), DELTA);
    assertEquals(3d, shockUp.apply(NON_MATCHING_ID, 3d), DELTA);

    MarketDataShock shockDown = MarketDataShock.absoluteShift(-2d, MATCHER);
    assertEquals(1d, shockDown.apply(MATCHING_ID, 3d), DELTA);
  }

  @Test
  public void relativeShift() {
    MarketDataShock shockUp = MarketDataShock.relativeShift(0.5, MATCHER);
    assertEquals(3d, shockUp.apply(MATCHING_ID, 2d), DELTA);
    assertEquals(2d, shockUp.apply(NON_MATCHING_ID, 2d), DELTA);

    MarketDataShock shockDown = MarketDataShock.relativeShift(-0.5, MATCHER);
    assertEquals(1d, shockDown.apply(MATCHING_ID, 2d), DELTA);
  }

  @Test
  public void replace() {
    MarketDataShock shock = MarketDataShock.replace(7d, MATCHER);
    assertEquals(7d, shock.apply(MATCHING_ID, 1d));
    assertEquals(1d, shock.apply(NON_MATCHING_ID, 1d));
  }
}
