/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link ResolvedIborFutureOption}. 
 */
public class ResolvedIborFutureOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFutureOption PRODUCT = IborFutureOptionTest.sut();
  private static final IborFutureOption PRODUCT2 = IborFutureOptionTest.sut2();

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedIborFutureOption test = sut();
    assertThat(test.getSecurityId()).isEqualTo(PRODUCT.getSecurityId());
    assertThat(test.getPutCall()).isEqualTo(PRODUCT.getPutCall());
    assertThat(test.getStrikePrice()).isEqualTo(PRODUCT.getStrikePrice());
    assertThat(test.getPremiumStyle()).isEqualTo(PRODUCT.getPremiumStyle());
    assertThat(test.getExpiry()).isEqualTo(PRODUCT.getExpiry());
    assertThat(test.getExpiryDate()).isEqualTo(PRODUCT.getExpiryDate());
    assertThat(test.getRounding()).isEqualTo(PRODUCT.getRounding());
    assertThat(test.getUnderlyingFuture()).isEqualTo(PRODUCT.getUnderlyingFuture().resolve(REF_DATA));
    assertThat(test.getIndex()).isEqualTo(PRODUCT.getUnderlyingFuture().getIndex());
  }

  @Test
  public void test_builder_expiryNotAfterTradeDate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedIborFutureOption.builder()
            .securityId(PRODUCT.getSecurityId())
            .putCall(CALL)
            .expiry(PRODUCT.getUnderlyingFuture().getLastTradeDate().plusDays(1).atStartOfDay(ZoneOffset.UTC))
            .strikePrice(PRODUCT.getStrikePrice())
            .underlyingFuture(PRODUCT.getUnderlyingFuture().resolve(REF_DATA))
            .build());
  }

  @Test
  public void test_builder_badPrice() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> sut().toBuilder().strikePrice(2.1).build());
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
  static ResolvedIborFutureOption sut() {
    return PRODUCT.resolve(REF_DATA);
  }

  static ResolvedIborFutureOption sut2() {
    return PRODUCT2.resolve(REF_DATA);
  }

}
