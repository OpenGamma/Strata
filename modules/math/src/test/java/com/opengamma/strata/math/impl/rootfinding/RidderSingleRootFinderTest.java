/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class RidderSingleRootFinderTest extends RealSingleRootFinderTestCase {
  private static final RealSingleRootFinder FINDER = new RidderSingleRootFinder();

  @Override
  protected RealSingleRootFinder getRootFinder() {
    return FINDER;
  }
}
