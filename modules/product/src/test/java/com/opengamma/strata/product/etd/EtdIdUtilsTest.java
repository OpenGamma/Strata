/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.product.etd.EtdVariant.MONTHLY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.ExchangeIds;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link EtdIdUtils}.
 */
public class EtdIdUtilsTest {

  private static final EtdContractCode OGBS = EtdContractCode.of("OGBS");
  private static final EtdContractCode FGBS = EtdContractCode.of("FGBS");

  @Test
  public void test_contractSpecId_future() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(EtdType.FUTURE, ExchangeIds.ECAG, FGBS);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "F-ECAG-FGBS"));
  }

  @Test
  public void test_contractSpecId_option() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(EtdType.OPTION, ExchangeIds.ECAG, OGBS);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-OGBS"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_futureId_monthly() {
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), MONTHLY);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "F-ECAG-FGBS-201706"));
  }

  @Test
  public void test_futureId_weekly() {
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofWeekly(2));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "F-ECAG-FGBS-201706W2"));
  }

  @Test
  public void test_futureId_daily() {
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofDaily(2));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "F-ECAG-FGBS-20170602"));
  }

  @Test
  public void test_futureId_flex() {
    SecurityId test = EtdIdUtils.futureId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofFlexFuture(26, EtdSettlementType.DERIVATIVE));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "F-ECAG-FGBS-20170626D"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_optionId_monthly() {
    SecurityId test = EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), MONTHLY, 0, PutCall.PUT, 12.34);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-201706-P12.34"));
  }

  @Test
  public void test_optionId_weekly() {
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofWeekly(3), 0, PutCall.CALL, -1.45);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-201706W3-CM1.45"));
  }

  @Test
  public void test_optionId_daily9_version() {
    SecurityId test =
        EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofDaily(9), 3, PutCall.PUT, 12.34);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-20170609-V3-P12.34"));
  }

  @Test
  public void test_optionId_daily21_version() {
    SecurityId test =
        EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofDaily(21), 11, PutCall.PUT, 12.34);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-20170621-V11-P12.34"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_optionIdUnderlying_monthly() {
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), MONTHLY, 0, PutCall.PUT, 12.34, YearMonth.of(2017, 9));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-201706-P12.34-U201709"));
  }

  @Test
  public void test_optionIdUnderlying_monthlySameMonth() {
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), MONTHLY, 0, PutCall.PUT, 12.34, YearMonth.of(2017, 6));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-201706-P12.34"));
  }

  @Test
  public void test_optionIdUnderlying_weekly() {
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofWeekly(3), 0, PutCall.CALL, -1.45, YearMonth.of(2017, 9));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-201706W3-CM1.45-U201709"));
  }

  @Test
  public void test_optionIdUnderlying_daily9_version() {
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofDaily(9), 3, PutCall.PUT, 12.34, YearMonth.of(2017, 9));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-20170609-V3-P12.34-U201709"));
  }

  @Test
  public void test_optionIdUnderlying_daily21_version() {
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, YearMonth.of(2017, 6), EtdVariant.ofDaily(21), 11, PutCall.PUT, 12.34, YearMonth.of(2017, 9));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of("OG-ETD", "O-ECAG-FGBS-20170621-V11-P12.34-U201709"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_coverage() {
    coverPrivateConstructor(EtdIdUtils.class);
  }

}
