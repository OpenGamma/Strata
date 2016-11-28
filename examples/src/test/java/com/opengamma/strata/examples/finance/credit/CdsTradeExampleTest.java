/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.examples.finance.CdsTradeExample;

/**
 * Test.
 */
@Test
public class CdsTradeExampleTest {

  public void test_load_from_classpath() {
    String xmlStringFromClasspath = CdsTradeExample.loadExamplePortfolio();
    String xmlStringFromClasspathCompact = CdsTradeExample.serializeCompact(CdsTradeExample.deserialize(xmlStringFromClasspath));
    String xmlStringFromCode = CdsTradeExample.serializeCompact(CdsTradeExample.TRADE_LIST);
    assertEquals(xmlStringFromClasspathCompact, xmlStringFromCode);
  }

}
