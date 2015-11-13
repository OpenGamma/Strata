/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.future;

import static com.opengamma.strata.basics.PutCall.CALL;
import static com.opengamma.strata.basics.PutCall.PUT;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.UnitSecurity;

/**
 * Test {@link GenericFutureOption}.
 */
@Test
public class GenericFutureOptionTest {

  private static final StandardId SYMBOL = StandardId.of("Exchange", "Sym01");
  private static final StandardId SYMBOL2 = StandardId.of("Exchange", "Sym02");
  private static final YearMonth YM_2015_06 = YearMonth.of(2015, 6);
  private static final YearMonth YM_2015_09 = YearMonth.of(2015, 9);
  private static final LocalDate DATE_2015_06 = date(2015, 6, 15);
  private static final LocalDate DATE_2015_09 = date(2015, 9, 15);
  private static final CurrencyAmount USD_10 = CurrencyAmount.of(USD, 10);
  private static final CurrencyAmount GBP_20 = CurrencyAmount.of(GBP, 20);

  private static final GenericFuture PRODUCT = GenericFuture.builder()
      .productId(StandardId.of("Exchange", "Sym01"))
      .expiryMonth(YearMonth.of(2015, 6))
      .expiryDate(date(2015, 6, 15))
      .tickSize(0.0001)
      .tickValue(CurrencyAmount.of(USD, 10))
      .build();
  private static final Security<GenericFuture> SECURITY = UnitSecurity.builder(PRODUCT)
      .standardId(StandardId.of("OG-Ticker", "OG"))
      .build();
  private static final SecurityLink<GenericFuture> RESOLVABLE_LINK =
      SecurityLink.resolvable(StandardId.of("OG-Ticker", "OG"), GenericFuture.class);
  private static final SecurityLink<GenericFuture> RESOLVED_LINK =
      SecurityLink.resolved(SECURITY);

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    GenericFutureOption test = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .underlyingLink(RESOLVABLE_LINK)
        .build();
    assertEquals(test.getProductId(), SYMBOL);
    assertEquals(test.getExpiryMonth(), YM_2015_06);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), 1.51d);
    assertEquals(test.getExpiryDate(), Optional.of(DATE_2015_06));
    assertEquals(test.getTickSize(), 0.0001);
    assertEquals(test.getTickValue(), USD_10);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getUnderlyingQuantity(), 1L);
    assertEquals(test.getUnderlyingLink(), Optional.of(RESOLVABLE_LINK));
    assertThrows(() -> test.getUnderlyingSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getUnderlying(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    GenericFutureOption test = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .underlyingLink(RESOLVED_LINK)
        .build();
    assertEquals(test.getProductId(), SYMBOL);
    assertEquals(test.getExpiryMonth(), YM_2015_06);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), 1.51d);
    assertEquals(test.getExpiryDate(), Optional.of(DATE_2015_06));
    assertEquals(test.getTickSize(), 0.0001);
    assertEquals(test.getTickValue(), USD_10);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getUnderlyingQuantity(), 1L);
    assertEquals(test.getUnderlyingLink(), Optional.of(RESOLVED_LINK));
    assertEquals(test.getUnderlyingSecurity(), Optional.of(SECURITY));
    assertEquals(test.getUnderlying(), Optional.of(PRODUCT));
  }

  public void test_builder_noUnderlying() {
    GenericFutureOption test = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .build();
    assertEquals(test.getProductId(), SYMBOL);
    assertEquals(test.getExpiryMonth(), YM_2015_06);
    assertEquals(test.getPutCall(), CALL);
    assertEquals(test.getStrikePrice(), 1.51d);
    assertEquals(test.getExpiryDate(), Optional.of(DATE_2015_06));
    assertEquals(test.getTickSize(), 0.0001);
    assertEquals(test.getTickValue(), USD_10);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getUnderlyingQuantity(), 1L);
    assertEquals(test.getUnderlyingLink(), Optional.empty());
    assertEquals(test.getUnderlyingSecurity(), Optional.empty());
    assertEquals(test.getUnderlying(), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    GenericFutureOption test = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .underlyingLink(RESOLVABLE_LINK)
        .build();
    GenericFutureOption expected = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .underlyingLink(RESOLVED_LINK)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        assertEquals(identifier, RESOLVABLE_LINK.getStandardId());
        return (T) SECURITY;
      }
    };
    assertEquals(test.resolveLinks(resolver), expected);
  }

  public void test_resolveLinks_resolved() {
    GenericFutureOption test = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .underlyingLink(RESOLVED_LINK)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        fail();  // not invoked because link is already resolved
        return null;
      }
    };
    assertSame(test.resolveLinks(resolver), test);
  }

  public void test_resolveLinks_noUnderlying() {
    GenericFutureOption test = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        fail();  // not invoked because link is already resolved
        return null;
      }
    };
    assertSame(test.resolveLinks(resolver), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    GenericFutureOption test = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .underlyingQuantity(1)
        .underlyingLink(RESOLVED_LINK)
        .build();
    coverImmutableBean(test);
    GenericFutureOption test2 = GenericFutureOption.builder()
        .productId(SYMBOL2)
        .putCall(PUT)
        .strikePrice(1.52)
        .expiryMonth(YM_2015_09)
        .expiryDate(DATE_2015_09)
        .tickSize(0.0002)
        .tickValue(GBP_20)
        .underlyingQuantity(20)
        .underlyingLink(RESOLVABLE_LINK)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    GenericFutureOption test = GenericFutureOption.builder()
        .productId(SYMBOL)
        .putCall(CALL)
        .strikePrice(1.51)
        .expiryMonth(YM_2015_06)
        .expiryDate(DATE_2015_06)
        .tickSize(0.0001)
        .tickValue(USD_10)
        .underlyingLink(RESOLVED_LINK)
        .build();
    assertSerialization(test);
  }

}
