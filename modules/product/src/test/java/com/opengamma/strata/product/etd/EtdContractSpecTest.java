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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.SecurityAttributeType;
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
  public void test_attributes() {
    assertEquals(sut2().getAttribute(SecurityAttributeType.NAME), "NAME");
    assertEquals(sut2().findAttribute(SecurityAttributeType.NAME).get(), "NAME");
    assertThrows(IllegalArgumentException.class, () -> sut2().getAttribute(SecurityAttributeType.of("Foo")));
  }

  //-------------------------------------------------------------------------
  public void createFutureAutoId() {
    EtdFutureSecurity security = FUTURE_CONTRACT.createFuture(YearMonth.of(2015, 6), EtdVariant.MONTHLY);

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of(EtdIdUtils.ETD_SCHEME, "F-ECAG-FOO-201506"));
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(FUTURE_CONTRACT.getId());
    assertThat(security.getVariant()).isEqualTo(EtdVariant.MONTHLY);
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(FUTURE_CONTRACT.getPriceInfo());
  }

  public void createFutureFromOptionContractSpec() {
    assertThatThrownBy(() -> OPTION_CONTRACT.createFuture(YearMonth.of(2015, 6), EtdVariant.MONTHLY))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Cannot create an EtdFutureSecurity from a contract specification of type 'Option'");
  }

  //-------------------------------------------------------------------------
  public void createOptionAutoId() {
    EtdOptionSecurity security = OPTION_CONTRACT.createOption(YearMonth.of(2015, 6), EtdVariant.MONTHLY, 0, PutCall.CALL, 123.45);

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of(EtdIdUtils.ETD_SCHEME, "O-IFEN-BAR-201506-C123.45"));
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(OPTION_CONTRACT.getId());
    assertThat(security.getVariant()).isEqualTo(EtdVariant.MONTHLY);
    assertThat(security.getPutCall()).isEqualTo(PutCall.CALL);
    assertThat(security.getStrikePrice()).isEqualTo(123.45);
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(OPTION_CONTRACT.getPriceInfo());
  }

  public void createOptionFromFutureContractSpec() {
    assertThatThrownBy(
        () -> FUTURE_CONTRACT.createOption(YearMonth.of(2015, 6), EtdVariant.MONTHLY, 0, PutCall.CALL, 123.45))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot create an EtdOptionSecurity from a contract specification of type 'Future'");
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
        .contractCode(EtdContractCode.of("FOO"))
        .description("A test future template")
        .priceInfo(SecurityPriceInfo.of(Currency.GBP, 100))
        .build();
  }

  static EtdContractSpec sut2() {
    return EtdContractSpec.builder()
        .type(EtdType.OPTION)
        .exchangeId(ExchangeIds.IFEN)
        .contractCode(EtdContractCode.of("BAR"))
        .description("A test option template")
        .priceInfo(SecurityPriceInfo.of(Currency.EUR, 10))
        .addAttribute(SecurityAttributeType.NAME, "NAME")
        .build();
  }

}
