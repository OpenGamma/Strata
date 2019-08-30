/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityPriceInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link IborFutureOptionSecurity}.
 */
public class IborFutureOptionSecurityTest {

  private static final IborFutureOption OPTION = IborFutureOptionTest.sut();
  private static final IborFutureOption OPTION2 = IborFutureOptionTest.sut2();
  private static final IborFuture FUTURE = OPTION.getUnderlyingFuture();
  private static final IborFuture FUTURE2 = OPTION2.getUnderlyingFuture();
  private static final IborFutureSecurity FUTURE_SECURITY = IborFutureSecurityTest.sut();
  private static final SecurityId FUTURE_ID = FUTURE.getSecurityId();
  private static final SecurityId FUTURE_ID2 = FUTURE2.getSecurityId();
  private static final SecurityPriceInfo PRICE_INFO = SecurityPriceInfo.of(0.1, CurrencyAmount.of(GBP, 25));
  private static final SecurityInfo INFO = SecurityInfo.of(OPTION.getSecurityId(), PRICE_INFO);
  private static final SecurityInfo INFO2 = SecurityInfo.of(OPTION2.getSecurityId(), PRICE_INFO);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    IborFutureOptionSecurity test = sut();
    assertThat(test.getInfo()).isEqualTo(INFO);
    assertThat(test.getSecurityId()).isEqualTo(OPTION.getSecurityId());
    assertThat(test.getCurrency()).isEqualTo(OPTION.getCurrency());
    assertThat(test.getPutCall()).isEqualTo(OPTION.getPutCall());
    assertThat(test.getPremiumStyle()).isEqualTo(OPTION.getPremiumStyle());
    assertThat(test.getUnderlyingFutureId()).isEqualTo(FUTURE_ID);
    assertThat(test.getUnderlyingIds()).containsOnly(FUTURE_ID);
  }

  @Test
  public void test_builder_badPrice() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> sut().toBuilder().strikePrice(2.1).build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createProduct() {
    IborFutureOptionSecurity test = sut();
    ReferenceData refData = ImmutableReferenceData.of(FUTURE_ID, FUTURE_SECURITY);
    assertThat(test.createProduct(refData)).isEqualTo(OPTION);
    TradeInfo tradeInfo = TradeInfo.of(date(2016, 6, 30));
    IborFutureOptionTrade expectedTrade = IborFutureOptionTrade.builder()
        .info(tradeInfo)
        .product(OPTION)
        .quantity(100)
        .price(123.50)
        .build();
    assertThat(test.createTrade(tradeInfo, 100, 123.50, refData)).isEqualTo(expectedTrade);

    PositionInfo positionInfo = PositionInfo.empty();
    IborFutureOptionPosition expectedPosition1 = IborFutureOptionPosition.builder()
        .info(positionInfo)
        .product(OPTION)
        .longQuantity(100)
        .build();
    TestHelper.assertEqualsBean(test.createPosition(positionInfo, 100, refData), expectedPosition1);
    IborFutureOptionPosition expectedPosition2 = IborFutureOptionPosition.builder()
        .info(positionInfo)
        .product(OPTION)
        .longQuantity(100)
        .shortQuantity(50)
        .build();
    assertThat(test.createPosition(positionInfo, 100, 50, refData)).isEqualTo(expectedPosition2);
  }

  @Test
  public void test_createProduct_wrongType() {
    IborFutureOptionSecurity test = sut();
    IborFuture future = OPTION.getUnderlyingFuture();
    SecurityId secId = future.getSecurityId();
    GenericSecurity sec = GenericSecurity.of(INFO);
    ReferenceData refData = ImmutableReferenceData.of(secId, sec);
    assertThatExceptionOfType(ClassCastException.class)
        .isThrownBy(() -> test.createProduct(refData));
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
  static IborFutureOptionSecurity sut() {
    return IborFutureOptionSecurity.builder()
        .info(INFO)
        .currency(OPTION.getCurrency())
        .putCall(OPTION.getPutCall())
        .strikePrice(OPTION.getStrikePrice())
        .expiryDate(OPTION.getExpiryDate())
        .expiryTime(OPTION.getExpiryTime())
        .expiryZone(OPTION.getExpiryZone())
        .premiumStyle(OPTION.getPremiumStyle())
        .rounding(OPTION.getRounding())
        .underlyingFutureId(FUTURE_ID)
        .build();
  }

  static IborFutureOptionSecurity sut2() {
    return IborFutureOptionSecurity.builder()
        .info(INFO2)
        .currency(OPTION2.getCurrency())
        .putCall(OPTION2.getPutCall())
        .strikePrice(OPTION2.getStrikePrice())
        .expiryDate(OPTION2.getExpiryDate())
        .expiryTime(OPTION2.getExpiryTime())
        .expiryZone(OPTION2.getExpiryZone())
        .premiumStyle(OPTION2.getPremiumStyle())
        .rounding(OPTION2.getRounding())
        .underlyingFutureId(FUTURE_ID2)
        .build();
  }

}
