/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.ExchangeIds;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link EtdContractSpec}.
 */
@Test
public class EtdContractSpecTest {

  private static final EtdContractSpec FUTURE_CONTRACT = sut();
  private static final EtdContractSpec OPTION_CONTRACT = sut2();

  //-------------------------------------------------------------------------
  public void createStandardOption() {
    EtdOptionSecurity security = OPTION_CONTRACT.createOption(
        SecurityId.of("test", "option"),
        PutCall.CALL,
        123.45,
        YearMonth.of(2015, 6));

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of("test", "option"));
    assertThat(security.getPutCall()).isEqualTo(PutCall.CALL);
    assertThat(security.getStrikePrice()).isEqualTo(123.45);
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(OPTION_CONTRACT.getId());
    assertThat(security.getExpiryDateCode()).isEmpty();
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(OPTION_CONTRACT.getPriceInfo());
  }

  public void createNonStandardOption() {
    EtdOptionSecurity security = OPTION_CONTRACT.createOption(
        SecurityId.of("test", "option"),
        PutCall.CALL,
        123.45,
        YearMonth.of(2015, 6),
        "W1");

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of("test", "option"));
    assertThat(security.getPutCall()).isEqualTo(PutCall.CALL);
    assertThat(security.getStrikePrice()).isEqualTo(123.45);
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(OPTION_CONTRACT.getId());
    assertThat(security.getExpiryDateCode()).hasValue("W1");
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(OPTION_CONTRACT.getPriceInfo());
  }

  public void createFutureFromOptionContractSpec() {
    SecurityId secId = SecurityId.of("test", "future");
    assertThatThrownBy(() -> OPTION_CONTRACT.createFuture(secId, YearMonth.of(2015, 6)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot create a Future from a contract specification of type 'Option'");
    assertThatThrownBy(() -> OPTION_CONTRACT.createFuture(secId, YearMonth.of(2015, 6), "W1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot create a Future from a contract specification of type 'Option'");
  }

  public void createStandardFuture() {
    EtdFutureSecurity security = FUTURE_CONTRACT.createFuture(
        SecurityId.of("test", "future"),
        YearMonth.of(2015, 6));

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of("test", "future"));
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(FUTURE_CONTRACT.getId());
    assertThat(security.getExpiryDateCode()).isEmpty();
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(FUTURE_CONTRACT.getPriceInfo());
  }

  public void createNonStandardFuture() {
    EtdFutureSecurity security = FUTURE_CONTRACT.createFuture(
        SecurityId.of("test", "future"),
        YearMonth.of(2015, 6),
        "W1");

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of("test", "future"));
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(FUTURE_CONTRACT.getId());
    assertThat(security.getExpiryDateCode()).hasValue("W1");
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(FUTURE_CONTRACT.getPriceInfo());
  }

  public void createOptionFromFutureContractSpec() {
    SecurityId secId = SecurityId.of("test", "option");
    assertThatThrownBy(
        () -> FUTURE_CONTRACT.createOption(secId, PutCall.CALL, 123.45, YearMonth.of(2015, 6)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot create an Option from a contract specification of type 'Future'");
    assertThatThrownBy(
        () -> FUTURE_CONTRACT.createOption(secId, PutCall.CALL, 123.45, YearMonth.of(2015, 6), "W1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot create an Option from a contract specification of type 'Future'");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static EtdContractSpec sut() {
    return EtdContractSpec.builder()
        .id(EtdContractSpecId.of("test", "123"))
        .type(EtdType.FUTURE)
        .exchangeId(ExchangeIds.ECAG)
        .contractCode("FOO")
        .description("A test future template")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();
  }

  static EtdContractSpec sut2() {
    return EtdContractSpec.builder()
        .id(EtdContractSpecId.of("test", "234"))
        .type(EtdType.OPTION)
        .exchangeId(ExchangeIds.IFEN)
        .contractCode("BAR")
        .description("A test option template")
        .priceInfo(SecurityPriceInfo.of(Currency.EUR, 10))
        .build();
  }

}
