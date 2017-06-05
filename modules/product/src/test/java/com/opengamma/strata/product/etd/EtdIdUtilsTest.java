/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.product.etd.EtdVariant.MONTHLY;
import static org.testng.Assert.assertEquals;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.ExchangeIds;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link EtdIdUtils}.
 */
@Test
public class EtdIdUtilsTest {

  private static final EtdContractCode OGBS = EtdContractCode.of("OGBS");
  private static final EtdContractCode FGBS = EtdContractCode.of("FGBS");

  public void test_contractSpecId_future() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(EtdType.FUTURE, ExchangeIds.ECAG, FGBS);
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "F-ECAG-FGBS"));
  }

  public void test_contractSpecId_option() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(EtdType.OPTION, ExchangeIds.ECAG, OGBS);
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "O-ECAG-OGBS"));
  }

  //-------------------------------------------------------------------------
  public void test_futureId_monthly() {
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), MONTHLY);
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "F-ECAG-FGBS-201706"));
  }

  public void test_futureId_weekly() {
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofWeekly(2));
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "F-ECAG-FGBS-201706W2"));
  }

  public void test_futureId_daily() {
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofDaily(2));
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "F-ECAG-FGBS-20170602"));
  }

  public void test_futureId_flex() {
    SecurityId test = EtdIdUtils.futureId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofFlexFuture(26, EtdSettlementType.DERIVATIVE));
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "F-ECAG-FGBS-20170626D"));
  }

  //-------------------------------------------------------------------------
  public void test_optionId_monthly() {
    SecurityId test = EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), MONTHLY, 0, PutCall.PUT, 12.34);
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "O-ECAG-FGBS-201706-P12.34"));
  }

  public void test_optionId_weekly() {
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofWeekly(3), 0, PutCall.CALL, -1.45);
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "O-ECAG-FGBS-201706W3-CM1.45"));
  }

  public void test_optionId_daily9_version() {
    SecurityId test =
        EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofDaily(9), 3, PutCall.PUT, 12.34);
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "O-ECAG-FGBS-20170609-V3-P12.34"));
  }

  public void test_optionId_daily21_version() {
    SecurityId test =
        EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofDaily(21), 11, PutCall.PUT, 12.34);
    assertEquals(test.getStandardId(), StandardId.of("OG-ETD", "O-ECAG-FGBS-20170621-V11-P12.34"));
  }

  //-------------------------------------------------------------------------
  public void test_coverage() {
    coverPrivateConstructor(EtdIdUtils.class);
  }

}
