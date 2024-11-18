/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.basics.StandardSchemes.OG_ETD_SCHEME;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.product.etd.EtdVariant.MONTHLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

  private static final YearMonth EXPIRY = YearMonth.of(2017, 6);
  private static final EtdContractCode OGBS = EtdContractCode.of("OGBS");
  private static final EtdContractCode FGBS = EtdContractCode.of("FGBS");
  private static final EtdContractCode BAJAJ_AUTO = EtdContractCode.of("BAJAJ-AUTO");

  @Test
  public void test_contractSpecId_future() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(EtdType.FUTURE, ExchangeIds.ECAG, FGBS);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "F-ECAG-FGBS"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdContractSpecId.builder()
            .specId(test)
            .type(EtdType.FUTURE)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .build());
  }

  @Test
  void test_contractSpecIdWithExtraHyphen_future() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(EtdType.FUTURE, ExchangeIds.XNSE, BAJAJ_AUTO);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "F-XNSE-BAJAJ-AUTO"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdContractSpecId.builder()
            .specId(test)
            .type(EtdType.FUTURE)
            .exchangeId(ExchangeIds.XNSE)
            .contractCode(BAJAJ_AUTO)
            .build());
  }

  @Test
  public void test_contractSpecId_option() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(EtdType.OPTION, ExchangeIds.ECAG, OGBS);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-OGBS"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdContractSpecId.builder()
            .specId(test)
            .type(EtdType.OPTION)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(OGBS)
            .build());
  }

  @Test
  void test_contractSpecIdWithExtraHyphen_option() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(EtdType.OPTION, ExchangeIds.XNSE, BAJAJ_AUTO);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-XNSE-BAJAJ-AUTO"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdContractSpecId.builder()
            .specId(test)
            .type(EtdType.OPTION)
            .exchangeId(ExchangeIds.XNSE)
            .contractCode(BAJAJ_AUTO)
            .build());
  }

  @Test
  public void test_contractSpecId_future_from_securityId() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(SecurityId.of(OG_ETD_SCHEME, "F-ECAG-FGBS-202305"));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "F-ECAG-FGBS"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdContractSpecId.builder()
            .specId(test)
            .type(EtdType.FUTURE)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .build());
  }

  @Test
  public void test_contractSpecId_option_from_securityId() {
    EtdContractSpecId test = EtdIdUtils.contractSpecId(SecurityId.of(OG_ETD_SCHEME, "O-ECAG-OGBS-202305-P1"));
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-OGBS"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdContractSpecId.builder()
            .specId(test)
            .type(EtdType.OPTION)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(OGBS)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_futureId_monthly() {
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, EXPIRY, MONTHLY);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "F-ECAG-FGBS-201706"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(MONTHLY)
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_futureIdWithExtraHyphen_monthly() {
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.XNSE, BAJAJ_AUTO, EXPIRY, MONTHLY);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "F-XNSE-BAJAJ-AUTO-201706"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.XNSE)
            .contractCode(BAJAJ_AUTO)
            .expiry(EXPIRY)
            .variant(MONTHLY)
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.XNSE);
  }

  @Test
  public void test_futureId_weekly() {
    EtdVariant variant = EtdVariant.ofWeekly(2);
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, EXPIRY, variant);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "F-ECAG-FGBS-201706W2"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_futureId_daily() {
    EtdVariant variant = EtdVariant.ofDaily(2);
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, EXPIRY, variant);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "F-ECAG-FGBS-20170602"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_futureId_flex() {
    EtdVariant variant = EtdVariant.ofFlexFuture(26, EtdSettlementType.DERIVATIVE);
    SecurityId test = EtdIdUtils.futureId(ExchangeIds.ECAG, FGBS, EXPIRY, variant);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "F-ECAG-FGBS-20170626D"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_optionId_monthly() {
    SecurityId test = EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, EXPIRY, MONTHLY, 0, PutCall.PUT, 12.34);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-201706-P12.34"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(MONTHLY)
            .option(SplitEtdOption.of(0, PutCall.PUT, 12.34))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_optionId_weekly() {
    EtdVariant variant = EtdVariant.ofWeekly(3);
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, EXPIRY, variant, 0, PutCall.CALL, -1.45);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-201706W3-CM1.45"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .option(SplitEtdOption.of(0, PutCall.CALL, -1.45))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_optionId_daily9_version() {
    EtdVariant variant = EtdVariant.ofDaily(9);
    SecurityId test =
        EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, EXPIRY, variant, 3, PutCall.PUT, 12.34);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-20170609-V3-P12.34"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .option(SplitEtdOption.of(3, PutCall.PUT, 12.34))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_optionId_daily21_version() {
    EtdVariant variant = EtdVariant.ofDaily(21);
    SecurityId test =
        EtdIdUtils.optionId(ExchangeIds.ECAG, FGBS, EXPIRY, variant, 11, PutCall.PUT, 12.34);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-20170621-V11-P12.34"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .option(SplitEtdOption.of(11, PutCall.PUT, 12.34))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_optionIdUnderlying_monthly() {
    YearMonth underlyingMonth = YearMonth.of(2017, 9);
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, EXPIRY, MONTHLY, 0, PutCall.PUT, 12.34, underlyingMonth);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-201706-P12.34-U201709"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(MONTHLY)
            .option(SplitEtdOption.of(0, PutCall.PUT, 12.34, underlyingMonth))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_optionIdUnderlyingWithHyphen_monthly() {
    YearMonth underlyingMonth = YearMonth.of(2017, 9);
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.XNSE, BAJAJ_AUTO, EXPIRY, MONTHLY, 0, PutCall.PUT, 12.34, underlyingMonth);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-XNSE-BAJAJ-AUTO-201706-P12.34-U201709"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.XNSE)
            .contractCode(BAJAJ_AUTO)
            .expiry(EXPIRY)
            .variant(MONTHLY)
            .option(SplitEtdOption.of(0, PutCall.PUT, 12.34, underlyingMonth))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.XNSE);
  }

  @Test
  public void test_optionIdUnderlying_monthlySameMonth() {
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, EXPIRY, MONTHLY, 0, PutCall.PUT, 12.34, EXPIRY);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-201706-P12.34"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(MONTHLY)
            .option(SplitEtdOption.of(0, PutCall.PUT, 12.34))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_optionIdUnderlying_weekly() {
    EtdVariant variant = EtdVariant.ofWeekly(3);
    YearMonth underlyingMonth = YearMonth.of(2017, 9);
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, EXPIRY, variant, 0, PutCall.CALL, -1.45, underlyingMonth);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-201706W3-CM1.45-U201709"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .option(SplitEtdOption.of(0, PutCall.CALL, -1.45, underlyingMonth))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_optionIdUnderlying_daily9_version() {
    EtdVariant variant = EtdVariant.ofDaily(9);
    YearMonth underlyingMonth = YearMonth.of(2017, 9);
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, EXPIRY, variant, 3, PutCall.PUT, 12.34, underlyingMonth);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-20170609-V3-P12.34-U201709"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .option(SplitEtdOption.of(3, PutCall.PUT, 12.34, underlyingMonth))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  @Test
  public void test_optionIdUnderlying_daily21_version() {
    EtdVariant variant = EtdVariant.ofDaily(21);
    YearMonth underlyingMonth = YearMonth.of(2017, 9);
    SecurityId test = EtdIdUtils.optionId(
        ExchangeIds.ECAG, FGBS, EXPIRY, variant, 11, PutCall.PUT, 12.34, underlyingMonth);
    assertThat(test.getStandardId()).isEqualTo(StandardId.of(OG_ETD_SCHEME, "O-ECAG-FGBS-20170621-V11-P12.34-U201709"));
    assertThat(EtdIdUtils.splitId(test))
        .isEqualTo(SplitEtdId.builder()
            .securityId(test)
            .exchangeId(ExchangeIds.ECAG)
            .contractCode(FGBS)
            .expiry(EXPIRY)
            .variant(variant)
            .option(SplitEtdOption.of(11, PutCall.PUT, 12.34, underlyingMonth))
            .build());
    assertThat(EtdIdUtils.splitIdToExchangeId(test)).isEqualTo(ExchangeIds.ECAG);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_splitContractSpec() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(EtdContractSpecId.of("A", "B")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(EtdContractSpecId.of(OG_ETD_SCHEME, "B")));
  }

  @Test
  public void test_split() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of("A", "B")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "B")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "F-ECAG-AB-ABCDEF")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "F-ECAG-AB-20206")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "F-ECAG-AB-202012-V1")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "O-ECAG-AB-202012")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "O-ECAG-AB-202012-X1")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "O-ECAG-AB-202012-P1A")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "O-ECAG-AB-202012-P1-X202012")));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdIdUtils.splitId(SecurityId.of(OG_ETD_SCHEME, "O-ECAG-AB-202012-P1-U123")));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_coverage() {
    coverPrivateConstructor(EtdIdUtils.class);
  }

}
