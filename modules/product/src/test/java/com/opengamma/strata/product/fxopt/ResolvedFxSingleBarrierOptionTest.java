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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link ResolvedFxSingleBarrierOption}.
 */
public class ResolvedFxSingleBarrierOptionTest {

  private static final ZonedDateTime EXPIRY_DATE_TIME = ZonedDateTime.of(2015, 2, 14, 12, 15, 0, 0, ZoneOffset.UTC);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final double STRIKE = 1.35;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * STRIKE);
  private static final ResolvedFxSingle FX = ResolvedFxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);
  private static final ResolvedFxVanillaOption VANILLA_OPTION = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY_DATE_TIME)
      .underlying(FX)
      .build();
  private static final SimpleConstantContinuousBarrier BARRIER =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 1.2);
  private static final CurrencyAmount REBATE = CurrencyAmount.of(USD, 5.0e4);

  @Test
  public void test_of() {
    ResolvedFxSingleBarrierOption test = ResolvedFxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
    assertThat(test.getBarrier()).isEqualTo(BARRIER);
    assertThat(test.getRebate().get()).isEqualTo(REBATE);
    assertThat(test.getUnderlyingOption()).isEqualTo(VANILLA_OPTION);
    assertThat(test.getCurrencyPair()).isEqualTo(VANILLA_OPTION.getCurrencyPair());
  }

  @Test
  public void test_of_noRebate() {
    ResolvedFxSingleBarrierOption test = ResolvedFxSingleBarrierOption.of(VANILLA_OPTION, BARRIER);
    assertThat(test.getBarrier()).isEqualTo(BARRIER);
    assertThat(test.getRebate().isPresent()).isFalse();
    assertThat(test.getUnderlyingOption()).isEqualTo(VANILLA_OPTION);
  }

  @Test
  public void test_of_fail() {
    CurrencyAmount negative = CurrencyAmount.of(USD, -5.0e4);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, negative));
    CurrencyAmount other = CurrencyAmount.of(GBP, 5.0e4);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, other));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ResolvedFxSingleBarrierOption test1 = ResolvedFxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
    ResolvedFxSingleBarrierOption test2 = ResolvedFxSingleBarrierOption.of(
        ResolvedFxVanillaOption.builder()
            .longShort(SHORT)
            .expiry(EXPIRY_DATE_TIME)
            .underlying(FX)
            .build(),
        SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, 1.5));
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    ResolvedFxSingleBarrierOption test = ResolvedFxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
    assertSerialization(test);
  }

}
