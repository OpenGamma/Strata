/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.fx.FxSingle;

/**
 * Test {@link FxVanillaOption}.
 */
public class FxVanillaOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2015, 2, 14);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(12, 15);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final LongShort LONG = LongShort.LONG;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * 1.35);
  private static final FxSingle FX = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    FxVanillaOption test = sut();
    assertThat(test.getExpiryDate()).isEqualTo(EXPIRY_DATE);
    assertThat(test.getExpiry()).isEqualTo(ZonedDateTime.of(EXPIRY_DATE, EXPIRY_TIME, EXPIRY_ZONE));
    assertThat(test.getExpiryZone()).isEqualTo(EXPIRY_ZONE);
    assertThat(test.getExpiryTime()).isEqualTo(EXPIRY_TIME);
    assertThat(test.getLongShort()).isEqualTo(LONG);
    assertThat(test.getUnderlying()).isEqualTo(FX);
    assertThat(test.getCurrencyPair()).isEqualTo(FX.getCurrencyPair());
    assertThat(test.isCrossCurrency()).isTrue();
    assertThat(test.allPaymentCurrencies()).containsOnly(EUR, USD);
    assertThat(test.allCurrencies()).containsOnly(EUR, USD);
  }

  @Test
  public void test_of_constructor() {
    // long call
    FxVanillaOption test = sut();
    CurrencyAmount baseCcyAmt = test.getUnderlying().getBaseCurrencyAmount();
    CurrencyAmount counterCcyAmt = test.getUnderlying().getCounterCurrencyAmount();
    FxVanillaOption testEquivalent = FxVanillaOption.of(
        test.getLongShort(),
        test.getExpiry(),
        test.getCurrencyPair(),
        baseCcyAmt.isPositive() ? PutCall.CALL : PutCall.PUT,
        Math.abs(counterCcyAmt.getAmount() / baseCcyAmt.getAmount()),
        baseCcyAmt.positive().getAmount(),
        test.getUnderlying().getPaymentDate());
    assertThat(test).isEqualTo(testEquivalent);

    // short put
    FxVanillaOption test2 = sut2();
    CurrencyAmount baseCcyAmt2 = test2.getUnderlying().getBaseCurrencyAmount();
    CurrencyAmount counterCcyAmt2 = test2.getUnderlying().getCounterCurrencyAmount();
    FxVanillaOption testEquivalent2 = FxVanillaOption.of(
        test2.getLongShort(),
        test2.getExpiry(),
        test2.getCurrencyPair(),
        baseCcyAmt2.isPositive() ? PutCall.CALL : PutCall.PUT,
        Math.abs(counterCcyAmt2.getAmount() / baseCcyAmt2.getAmount()),
        baseCcyAmt2.positive().getAmount(),
        test.getUnderlying().getPaymentDate());
    assertThat(test2).isEqualTo(testEquivalent2);
  }

  @Test
  public void test_builder_earlyPaymentDate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxVanillaOption.builder()
            .longShort(LONG)
            .expiryDate(LocalDate.of(2015, 2, 21))
            .expiryTime(EXPIRY_TIME)
            .expiryZone(EXPIRY_ZONE)
            .underlying(FX)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    FxVanillaOption base = sut();
    ResolvedFxVanillaOption expected = ResolvedFxVanillaOption.builder()
        .longShort(LONG)
        .expiry(EXPIRY_DATE.atTime(EXPIRY_TIME).atZone(EXPIRY_ZONE))
        .underlying(FX.resolve(REF_DATA))
        .build();
    assertThat(base.resolve(REF_DATA)).isEqualTo(expected);
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
  static FxVanillaOption sut() {
    return FxVanillaOption.builder()
        .longShort(LONG)
        .expiryDate(EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(EXPIRY_ZONE)
        .underlying(FX)
        .build();
  }

  static FxVanillaOption sut2() {
    FxSingle fxProduct =
        FxSingle.of(CurrencyAmount.of(EUR, -NOTIONAL), CurrencyAmount.of(GBP, NOTIONAL * 0.9), PAYMENT_DATE);
    return FxVanillaOption.builder()
        .longShort(LongShort.SHORT)
        .expiryDate(LocalDate.of(2015, 2, 15))
        .expiryTime(LocalTime.of(12, 45))
        .expiryZone(ZoneId.of("GMT"))
        .underlying(fxProduct)
        .build();
  }

}
