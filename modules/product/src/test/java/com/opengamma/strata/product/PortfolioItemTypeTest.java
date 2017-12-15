/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link PortfolioItemType}.
 */
@Test
public class PortfolioItemTypeTest {

  //-------------------------------------------------------------------------
  public void test_constants() {
    assertEquals(PortfolioItemType.POSITION.getName(), "Position");
    assertEquals(PortfolioItemType.TRADE.getName(), "Trade");
    assertEquals(PortfolioItemType.OTHER.getName(), "Other");
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    assertEquals(PortfolioItemType.of("Position"), PortfolioItemType.POSITION);
    assertEquals(PortfolioItemType.of("position"), PortfolioItemType.POSITION);
    assertEquals(PortfolioItemType.of("POSITION"), PortfolioItemType.POSITION);
  }

}
