/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * Test {@link BondFutureOption}.
 */
public class BondFutureOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // future
  private static final BondFuture FUTURE = BondFutureTest.sut();
  private static final BondFuture FUTURE2 = BondFutureTest.sut2();
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "BondFutureOption");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "BondFutureOption2");
  // future option
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(3);
  private static final LocalDate EXPIRY_DATE = date(2011, 9, 20);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final double STRIKE_PRICE = 1.15;

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    BondFutureOption test = sut();
    assertThat(test.getPutCall()).isEqualTo(CALL);
    assertThat(test.getStrikePrice()).isEqualTo(STRIKE_PRICE);
    assertThat(test.getExpiryDate()).isEqualTo(EXPIRY_DATE);
    assertThat(test.getExpiryTime()).isEqualTo(EXPIRY_TIME);
    assertThat(test.getExpiryZone()).isEqualTo(EXPIRY_ZONE);
    assertThat(test.getExpiry()).isEqualTo(ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertThat(test.getRounding()).isEqualTo(Rounding.none());
    assertThat(test.getUnderlyingFuture()).isEqualTo(FUTURE);
    assertThat(test.getCurrency()).isEqualTo(FUTURE.getCurrency());
  }

  @Test
  public void test_builder_expiryNotAfterTradeDate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BondFutureOption.builder()
            .putCall(CALL)
            .expiryDate(FUTURE.getLastTradeDate())
            .expiryTime(EXPIRY_TIME)
            .expiryZone(EXPIRY_ZONE)
            .strikePrice(STRIKE_PRICE)
            .underlyingFuture(FUTURE)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    BondFutureOption test = sut();
    ResolvedBondFutureOption expected = ResolvedBondFutureOption.builder()
        .securityId(SECURITY_ID)
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiry(EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE))
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingFuture(FUTURE.resolve(REF_DATA))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
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
  static BondFutureOption sut() {
    return BondFutureOption.builder()
        .securityId(SECURITY_ID)
        .putCall(CALL)
        .strikePrice(STRIKE_PRICE)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
        .underlyingFuture(FUTURE)
        .build();
  }

  static BondFutureOption sut2() {
    return BondFutureOption.builder()
        .securityId(SECURITY_ID2)
        .putCall(PUT)
        .strikePrice(1.075)
        .expiryDate(date(2011, 9, 21))
        .expiryTime(LocalTime.of(12, 0))
        .expiryZone(ZoneId.of("Europe/Paris"))
        .premiumStyle(FutureOptionPremiumStyle.UPFRONT_PREMIUM)
        .rounding(ROUNDING)
        .underlyingFuture(FUTURE2)
        .build();
  }

}
