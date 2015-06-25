/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit;

import com.opengamma.strata.examples.finance.CdsTradeExample;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test.
 */
@Test
public class CdsTradeExampleTest {

  public void test_load_from_classpath() {
    String xmlStringFromClasspath = CdsTradeExample.loadExamplePortfolio();
    String xmlStringFromClasspathCompact = CdsTradeExample.serializeCompact(CdsTradeExample.deserialize(xmlStringFromClasspath));
    String xmlStringFromCode = CdsTradeExample.serializeCompact(CdsTradeExample.portfolio);
    assertEquals(xmlStringFromClasspathCompact, xmlStringFromCode);
  }

}
