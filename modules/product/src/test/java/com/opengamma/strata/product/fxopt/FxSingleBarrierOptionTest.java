/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link FxSingleBarrierOption}.
 */
public class FxSingleBarrierOptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2015, 2, 14);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(12, 15);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * 1.35);
  private static final FxSingle FX = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);
  private static final FxVanillaOption VANILLA_OPTION = FxVanillaOption.builder()
      .longShort(LONG)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(EXPIRY_ZONE)
      .underlying(FX)
      .build();
  private static final SimpleConstantContinuousBarrier BARRIER =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 1.2);
  private static final CurrencyAmount REBATE = CurrencyAmount.of(USD, 5.0e4);

  @Test
  public void test_of() {
    FxSingleBarrierOption test = FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
    assertThat(test.getBarrier()).isEqualTo(BARRIER);
    assertThat(test.getRebate().get()).isEqualTo(REBATE);
    assertThat(test.getUnderlyingOption()).isEqualTo(VANILLA_OPTION);
    assertThat(test.getCurrencyPair()).isEqualTo(VANILLA_OPTION.getCurrencyPair());
    assertThat(test.isCrossCurrency()).isTrue();
    assertThat(test.allPaymentCurrencies()).containsOnly(EUR, USD);
    assertThat(test.allCurrencies()).containsOnly(EUR, USD);
  }

  @Test
  public void test_builder() {
    FxSingleBarrierOption test = FxSingleBarrierOption.builder()
        .underlyingOption(VANILLA_OPTION)
        .barrier(BARRIER)
        .rebate(REBATE)
        .build();
    assertThat(test.getBarrier()).isEqualTo(BARRIER);
    assertThat(test.getRebate().get()).isEqualTo(REBATE);
    assertThat(test.getUnderlyingOption()).isEqualTo(VANILLA_OPTION);
    assertThat(test.getCurrencyPair()).isEqualTo(VANILLA_OPTION.getCurrencyPair());
  }

  @Test
  public void test_of_noRebate() {
    FxSingleBarrierOption test = FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER);
    assertThat(test.getBarrier()).isEqualTo(BARRIER);
    assertThat(test.getRebate().isPresent()).isFalse();
    assertThat(test.getUnderlyingOption()).isEqualTo(VANILLA_OPTION);
  }

  @Test
  public void test_of_fail() {
    CurrencyAmount negative = CurrencyAmount.of(USD, -5.0e4);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, negative));
    CurrencyAmount other = CurrencyAmount.of(GBP, 5.0e4);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, other));
  }

  @Test
  public void test_resolve() {
    FxSingleBarrierOption base = FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
    ResolvedFxSingleBarrierOption expected =
        ResolvedFxSingleBarrierOption.of(VANILLA_OPTION.resolve(REF_DATA), BARRIER, REBATE);
    assertThat(base.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_noRebate() {
    FxSingleBarrierOption base = FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER);
    ResolvedFxSingleBarrierOption expected =
        ResolvedFxSingleBarrierOption.of(VANILLA_OPTION.resolve(REF_DATA), BARRIER);
    assertThat(base.resolve(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxSingleBarrierOption test1 = FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
    FxSingleBarrierOption test2 = FxSingleBarrierOption.of(
        FxVanillaOption.builder()
            .longShort(SHORT)
            .expiryDate(EXPIRY_DATE)
            .expiryTime(EXPIRY_TIME)
            .expiryZone(EXPIRY_ZONE)
            .underlying(FX)
            .build(),
        SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, 1.5));
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    FxSingleBarrierOption test = FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
    assertSerialization(test);
  }

}
