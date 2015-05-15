/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.id.StandardId;

/**
 * Test {@link ObservableId}.
 */
@Test
public class ObservableIdTest {

  private static ObservableId TEST = new ObservableId() {

    @Override
    public StandardId getStandardId() {
      throw new UnsupportedOperationException();
    }

    @Override
    public FieldName getFieldName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public MarketDataFeed getMarketDataFeed() {
      return null;
    }
  };

  //-----------------------------------------------------------------------
  public void test_getMarketDataType() {
    assertEquals(TEST.getMarketDataType(), Double.class);
  }

}
