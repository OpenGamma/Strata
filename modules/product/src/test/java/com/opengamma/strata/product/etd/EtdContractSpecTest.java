/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.strata.product.etd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.PutCall;

@Test
public class EtdContractSpecTest {

  private static final EtdContractSpec FUTURE_CONTRACT = EtdContractSpec.builder()
      .id(EtdContractSpecId.of("test", "123"))
      .contractCode("FOO")
      .productType(EtdProductType.FUTURE)
      .exchangeId(ExchangeIds.ECAG)
      .description("A test future template")
      .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
      .build();

  private static final EtdContractSpec OPTION_CONTRACT = EtdContractSpec.builder()
      .id(EtdContractSpecId.of("test", "234"))
      .contractCode("BAR")
      .productType(EtdProductType.OPTION)
      .exchangeId(ExchangeIds.IFEN)
      .description("A test option template")
      .priceInfo(SecurityPriceInfo.of(Currency.EUR, 10))
      .build();

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
    assertThatThrownBy(() -> OPTION_CONTRACT.createFuture(SecurityId.of("test", "future"), YearMonth.of(2015, 6)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot create a future from a template with product type OPTION");
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
    assertThatThrownBy(
        () -> FUTURE_CONTRACT.createOption(
            SecurityId.of("test", "option"),
            PutCall.CALL,
            123.45,
            YearMonth.of(2015, 6),
            "W1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot create an option from a template with product type FUTURE");
  }
}
