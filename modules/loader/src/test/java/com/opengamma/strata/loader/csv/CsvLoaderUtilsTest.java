/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.etd.EtdSettlementType;

/**
 * Test {@link CsvLoaderUtils}.
 */
@Test
public class CsvLoaderUtilsTest {

  public void test_parseEtdSettlementType() {
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("C"), EtdSettlementType.CASH);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("CASH"), EtdSettlementType.CASH);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("c"), EtdSettlementType.CASH);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("E"), EtdSettlementType.PHYSICAL);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("PHYSICAL"), EtdSettlementType.PHYSICAL);
    assertEquals(CsvLoaderUtils.parseEtdSettlementType("e"), EtdSettlementType.PHYSICAL);
    assertThrowsIllegalArg(() -> CsvLoaderUtils.parseEtdSettlementType(""));
  }

  public void test_parseEtdOptionType() {
    assertEquals(CsvLoaderUtils.parseEtdOptionType("A"), EtdOptionType.AMERICAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("AMERICAN"), EtdOptionType.AMERICAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("a"), EtdOptionType.AMERICAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("E"), EtdOptionType.EUROPEAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("EUROPEAN"), EtdOptionType.EUROPEAN);
    assertEquals(CsvLoaderUtils.parseEtdOptionType("e"), EtdOptionType.EUROPEAN);
    assertThrowsIllegalArg(() -> CsvLoaderUtils.parseEtdOptionType(""));
  }

}
