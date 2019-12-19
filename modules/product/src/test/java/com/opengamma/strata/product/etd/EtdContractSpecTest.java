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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.Attributes;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.ExchangeIds;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link EtdContractSpec}.
 */
public class EtdContractSpecTest {

  private static final EtdContractSpec FUTURE_CONTRACT = sut();
  private static final EtdContractSpec OPTION_CONTRACT = sut2();

  //-------------------------------------------------------------------------
  @Test
  public void test_attributes() {
    assertThat(sut2().getAttributeTypes()).containsOnly(AttributeType.NAME);
    assertThat(sut2().getAttribute(AttributeType.NAME)).isEqualTo("NAME");
    assertThat(sut2().findAttribute(AttributeType.NAME)).hasValue("NAME");
    assertThatIllegalArgumentException().isThrownBy(() -> sut2().getAttribute(AttributeType.of("Foo")));
    EtdContractSpec updated = sut2().withAttribute(AttributeType.NAME, "FOO");
    assertThat(updated.getAttribute(AttributeType.NAME)).isEqualTo("FOO");
  }

  @Test
  public void test_attributes_with_bulk() {
    Attributes override = Attributes.of(AttributeType.DESCRIPTION, "B").withAttribute(AttributeType.NAME, "C");
    EtdContractSpec test = sut()
        .withAttribute(AttributeType.DESCRIPTION, "A")
        .withAttributes(override);
    assertThat(test.getAttributeTypes()).containsOnly(AttributeType.DESCRIPTION, AttributeType.NAME);
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("B");
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("C");
  }

  //-------------------------------------------------------------------------
  @Test
  public void createFutureAutoId() {
    EtdFutureSecurity security = FUTURE_CONTRACT.createFuture(YearMonth.of(2015, 6), EtdVariant.MONTHLY);

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of(EtdIdUtils.ETD_SCHEME, "F-ECAG-FOO-201506"));
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(FUTURE_CONTRACT.getId());
    assertThat(security.getVariant()).isEqualTo(EtdVariant.MONTHLY);
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(FUTURE_CONTRACT.getPriceInfo());
  }

  @Test
  public void createFutureFromOptionContractSpec() {
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> OPTION_CONTRACT.createFuture(YearMonth.of(2015, 6), EtdVariant.MONTHLY))
        .withMessage("Cannot create an EtdFutureSecurity from a contract specification of type 'Option'");
  }

  //-------------------------------------------------------------------------
  @Test
  public void createOptionAutoId() {
    EtdOptionSecurity security = OPTION_CONTRACT.createOption(YearMonth.of(2015, 6), EtdVariant.MONTHLY, 0, PutCall.CALL, 123.45);

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of(EtdIdUtils.ETD_SCHEME, "O-IFEN-BAR-201506-C123.45"));
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(OPTION_CONTRACT.getId());
    assertThat(security.getVariant()).isEqualTo(EtdVariant.MONTHLY);
    assertThat(security.getPutCall()).isEqualTo(PutCall.CALL);
    assertThat(security.getStrikePrice()).isEqualTo(123.45);
    assertThat(security.getUnderlyingExpiryMonth()).isEmpty();
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(OPTION_CONTRACT.getPriceInfo());
  }

  @Test
  public void createOptionFromFutureContractSpec() {
    assertThatThrownBy(
        () -> FUTURE_CONTRACT.createOption(YearMonth.of(2015, 6), EtdVariant.MONTHLY, 0, PutCall.CALL, 123.45))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot create an EtdOptionSecurity from a contract specification of type 'Future'");
  }

  //-------------------------------------------------------------------------
  @Test
  public void createOptionWithUnderlyingAutoId() {
    EtdOptionSecurity security = OPTION_CONTRACT.createOption(
        YearMonth.of(2015, 6), EtdVariant.MONTHLY, 0, PutCall.CALL, 123.45, YearMonth.of(2015, 9));

    assertThat(security.getSecurityId()).isEqualTo(SecurityId.of(EtdIdUtils.ETD_SCHEME, "O-IFEN-BAR-201506-C123.45-U201509"));
    assertThat(security.getExpiry()).isEqualTo(YearMonth.of(2015, 6));
    assertThat(security.getContractSpecId()).isEqualTo(OPTION_CONTRACT.getId());
    assertThat(security.getVariant()).isEqualTo(EtdVariant.MONTHLY);
    assertThat(security.getPutCall()).isEqualTo(PutCall.CALL);
    assertThat(security.getStrikePrice()).isEqualTo(123.45);
    assertThat(security.getUnderlyingExpiryMonth()).hasValue(YearMonth.of(2015, 9));
    assertThat(security.getInfo().getPriceInfo()).isEqualTo(OPTION_CONTRACT.getPriceInfo());
  }

  @Test
  public void createOptionWithUnderlyingFromFutureContractSpec() {
    assertThatThrownBy(
        () -> FUTURE_CONTRACT.createOption(
            YearMonth.of(2015, 6), EtdVariant.MONTHLY, 0, PutCall.CALL, 123.45, YearMonth.of(2015, 9)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create an EtdOptionSecurity from a contract specification of type 'Future'");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
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
        .addAttribute(AttributeType.NAME, "NAME")
        .build();
  }

}
