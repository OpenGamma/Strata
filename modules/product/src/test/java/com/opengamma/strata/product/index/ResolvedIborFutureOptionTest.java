/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static org.testng.Assert.assertEquals;

import java.time.ZoneOffset;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link ResolvedIborFutureOption}. 
 */
@Test
public class ResolvedIborFutureOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFutureOption PRODUCT = IborFutureOptionTest.sut();
  private static final IborFutureOption PRODUCT2 = IborFutureOptionTest.sut2();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFutureOption test = sut();
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getPutCall(), PRODUCT.getPutCall());
    assertEquals(test.getStrikePrice(), PRODUCT.getStrikePrice());
    assertEquals(test.getPremiumStyle(), PRODUCT.getPremiumStyle());
    assertEquals(test.getExpiry(), PRODUCT.getExpiry());
    assertEquals(test.getExpiryDate(), PRODUCT.getExpiryDate());
    assertEquals(test.getRounding(), PRODUCT.getRounding());
    assertEquals(test.getUnderlyingFuture(), PRODUCT.getUnderlyingFuture().resolve(REF_DATA));
    assertEquals(test.getIndex(), PRODUCT.getUnderlyingFuture().getIndex());
  }

  public void test_builder_expiryNotAfterTradeDate() {
    assertThrowsIllegalArg(() -> ResolvedIborFutureOption.builder()
        .securityId(PRODUCT.getSecurityId())
        .putCall(CALL)
        .expiry(PRODUCT.getUnderlyingFuture().getLastTradeDate().plusDays(1).atStartOfDay(ZoneOffset.UTC))
        .strikePrice(PRODUCT.getStrikePrice())
        .underlyingFuture(PRODUCT.getUnderlyingFuture().resolve(REF_DATA))
        .build());
  }

  public void test_builder_badPrice() {
    assertThrowsIllegalArg(() -> sut().toBuilder().strikePrice(2.1).build());
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
  static ResolvedIborFutureOption sut() {
    return PRODUCT.resolve(REF_DATA);
  }

  static ResolvedIborFutureOption sut2() {
    return PRODUCT2.resolve(REF_DATA);
  }

}
