/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link EtdOptionSecurity}.
 */
@Test
public class EtdOptionSecurityTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void test() {
    EtdOptionSecurity test = sut();
    assertEquals(test.getVariant(), EtdVariant.MONTHLY);
    assertEquals(test.getType(), EtdType.OPTION);
    assertEquals(test.getCurrency(), Currency.GBP);
    assertEquals(test.getUnderlyingIds(), ImmutableSet.of());
    assertEquals(test.createProduct(REF_DATA), test);
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
  static EtdOptionSecurity sut() {
    return EtdOptionSecurity.builder()
        .info(SecurityInfo.of(SecurityId.of("A", "B"), SecurityPriceInfo.of(Currency.GBP, 100)))
        .contractSpecId(EtdContractSpecId.of("test", "123"))
        .expiry(YearMonth.of(2017, 6))
        .putCall(PutCall.PUT)
        .strikePrice(2)
        .build();
  }

  static EtdOptionSecurity sut2() {
    return EtdOptionSecurity.builder()
        .info(SecurityInfo.of(SecurityId.of("B", "C"), SecurityPriceInfo.of(Currency.EUR, 10)))
        .contractSpecId(EtdContractSpecId.of("test", "234"))
        .expiry(YearMonth.of(2017, 9))
        .variant(EtdVariant.ofWeekly(2))
        .putCall(PutCall.CALL)
        .strikePrice(3)
        .build();
  }

}
