/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * Test {@link FxSwapTemplate}.
 */
public class FxSwapTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyPair EUR_USD = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, EUTA_USNY);
  private static final ImmutableFxSwapConvention CONVENTION = ImmutableFxSwapConvention.of(EUR_USD, PLUS_TWO_DAYS);
  private static final ImmutableFxSwapConvention CONVENTION2 = ImmutableFxSwapConvention.of(EUR_USD, PLUS_ONE_DAY);
  private static final Period NEAR_PERIOD = Period.ofMonths(3);
  private static final Period FAR_PERIOD = Period.ofMonths(6);
  private static final FxSwapTemplate NEAR_FAR_TEMPLATE = FxSwapTemplate.of(NEAR_PERIOD, FAR_PERIOD, CONVENTION);
  private static final LocalDate TRADE_DATE = LocalDate.of(2015, 10, 29);
  private static final LocalDate SPOT_DATE = PLUS_TWO_DAYS.adjust(TRADE_DATE, REF_DATA);
  private static final LocalDate NEAR_DATE = SPOT_DATE.plus(NEAR_PERIOD);
  private static final LocalDate FAR_DATE = SPOT_DATE.plus(FAR_PERIOD);
  private static final BusinessDayAdjustment BDA = CONVENTION.getBusinessDayAdjustment();

  private static final double NOTIONAL_EUR = 2_000_000d;
  private static final double NOTIONAL_USD = 3_900_000d;
  private static final double FX_RATE_NEAR = 1.30d;
  private static final double FX_RATE_PTS = 0.0050d;

  @Test
  public void test_of_far() {
    FxSwapTemplate test = FxSwapTemplate.of(FAR_PERIOD, CONVENTION);
    assertThat(test.getPeriodToNear()).isEqualTo(Period.ZERO);
    assertThat(test.getPeriodToFar()).isEqualTo(FAR_PERIOD);
    assertThat(test.getConvention()).isEqualTo(CONVENTION);
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_USD);
  }

  @Test
  public void test_of_near_far() {
    assertThat(NEAR_FAR_TEMPLATE.getPeriodToNear()).isEqualTo(NEAR_PERIOD);
    assertThat(NEAR_FAR_TEMPLATE.getPeriodToFar()).isEqualTo(FAR_PERIOD);
    assertThat(NEAR_FAR_TEMPLATE.getConvention()).isEqualTo(CONVENTION);
    assertThat(NEAR_FAR_TEMPLATE.getCurrencyPair()).isEqualTo(EUR_USD);
  }

  @Test
  public void test_builder_insufficientInfo() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSwapTemplate.builder().convention(CONVENTION).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSwapTemplate.builder().periodToNear(NEAR_PERIOD).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSwapTemplate.builder().periodToFar(FAR_PERIOD).build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTradeBuyImpliedCurrency() {
    FxSwapTrade test = NEAR_FAR_TEMPLATE.createTrade(TRADE_DATE, BUY, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS, REF_DATA);
    FxSwap expected = FxSwap.of(
        FxSingle.of(CurrencyAmount.of(EUR, NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR), NEAR_DATE, BDA),
        FxSingle.of(CurrencyAmount.of(EUR, -NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR + FX_RATE_PTS), FAR_DATE, BDA));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(TRADE_DATE));
    assertThat(test.getInfo().getAttribute(AttributeType.BUY_SELL)).isEqualTo(BUY);
    assertThat(test.getProduct().getNearLeg().getBaseCurrencyAmount().getCurrency()).isEqualTo(EUR);
    assertThat(test.getProduct().getNearLeg().getBaseCurrencyAmount().getAmount()).isPositive();
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_createTradeSellImpliedCurrency() {
    FxSwapTrade test = NEAR_FAR_TEMPLATE.createTrade(TRADE_DATE, SELL, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS, REF_DATA);
    FxSwap expected = FxSwap.of(
        FxSingle.of(CurrencyAmount.of(EUR, -NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR), NEAR_DATE, BDA),
        FxSingle.of(CurrencyAmount.of(EUR, NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR + FX_RATE_PTS), FAR_DATE, BDA));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(TRADE_DATE));
    assertThat(test.getInfo().getAttribute(AttributeType.BUY_SELL)).isEqualTo(SELL);
    assertThat(test.getProduct().getNearLeg().getBaseCurrencyAmount().getCurrency()).isEqualTo(EUR);
    assertThat(test.getProduct().getNearLeg().getBaseCurrencyAmount().getAmount()).isNegative();
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_createTradeBuySpecifiedCurrencyBase() {
    FxSwapTrade test = NEAR_FAR_TEMPLATE.createTrade(TRADE_DATE, BUY, EUR, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS, REF_DATA);
    FxSwap expected = FxSwap.of(
        FxSingle.of(CurrencyAmount.of(EUR, NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR), NEAR_DATE, BDA),
        FxSingle.of(CurrencyAmount.of(EUR, -NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR + FX_RATE_PTS), FAR_DATE, BDA));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(TRADE_DATE));
    assertThat(test.getInfo().getAttribute(AttributeType.BUY_SELL)).isEqualTo(BUY);
    assertThat(test.getProduct().getNearLeg().getBaseCurrencyAmount().getCurrency()).isEqualTo(EUR);
    assertThat(test.getProduct().getNearLeg().getBaseCurrencyAmount().getAmount()).isPositive();
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_createTradeBuySpecifiedCurrencyCounter() {
    FxSwapTrade test = NEAR_FAR_TEMPLATE.createTrade(TRADE_DATE, BUY, USD, NOTIONAL_USD, FX_RATE_NEAR, FX_RATE_PTS, REF_DATA);
    FxSwap expected = FxSwap.of(
        FxSingle.of(CurrencyAmount.of(USD, NOTIONAL_USD), FxRate.of(EUR, USD, FX_RATE_NEAR), NEAR_DATE, BDA),
        FxSingle.of(CurrencyAmount.of(USD, -NOTIONAL_USD), FxRate.of(EUR, USD, FX_RATE_NEAR + FX_RATE_PTS), FAR_DATE, BDA));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(TRADE_DATE));
    assertThat(test.getInfo().getAttribute(AttributeType.BUY_SELL)).isEqualTo(BUY);
    assertThat(test.getProduct().getNearLeg().getCounterCurrencyAmount().getCurrency()).isEqualTo(USD);
    assertThat(test.getProduct().getNearLeg().getCounterCurrencyAmount().getAmount()).isPositive();
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_createTradeSellSpecifiedCurrencyBase() {
    FxSwapTrade test = NEAR_FAR_TEMPLATE.createTrade(TRADE_DATE, SELL, EUR, NOTIONAL_EUR, FX_RATE_NEAR, FX_RATE_PTS, REF_DATA);
    FxSwap expected = FxSwap.of(
        FxSingle.of(CurrencyAmount.of(EUR, -NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR), NEAR_DATE, BDA),
        FxSingle.of(CurrencyAmount.of(EUR, NOTIONAL_EUR), FxRate.of(EUR, USD, FX_RATE_NEAR + FX_RATE_PTS), FAR_DATE, BDA));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(TRADE_DATE));
    assertThat(test.getInfo().getAttribute(AttributeType.BUY_SELL)).isEqualTo(SELL);
    assertThat(test.getProduct().getNearLeg().getBaseCurrencyAmount().getCurrency()).isEqualTo(EUR);
    assertThat(test.getProduct().getNearLeg().getBaseCurrencyAmount().getAmount()).isNegative();
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_createTradeSellSpecifiedCurrencyCounter() {
    FxSwapTrade test = NEAR_FAR_TEMPLATE.createTrade(TRADE_DATE, SELL, USD, NOTIONAL_USD, FX_RATE_NEAR, FX_RATE_PTS, REF_DATA);
    FxSwap expected = FxSwap.of(
        FxSingle.of(CurrencyAmount.of(USD, -NOTIONAL_USD), FxRate.of(EUR, USD, FX_RATE_NEAR), NEAR_DATE, BDA),
        FxSingle.of(CurrencyAmount.of(USD, NOTIONAL_USD), FxRate.of(EUR, USD, FX_RATE_NEAR + FX_RATE_PTS), FAR_DATE, BDA));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(TRADE_DATE));
    assertThat(test.getInfo().getAttribute(AttributeType.BUY_SELL)).isEqualTo(SELL);
    assertThat(test.getProduct().getNearLeg().getCounterCurrencyAmount().getCurrency()).isEqualTo(USD);
    assertThat(test.getProduct().getNearLeg().getCounterCurrencyAmount().getAmount()).isNegative();
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxSwapTemplate test = NEAR_FAR_TEMPLATE;
    coverImmutableBean(test);
    FxSwapTemplate test2 = FxSwapTemplate.of(Period.ofMonths(4), Period.ofMonths(7), CONVENTION2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FxSwapTemplate test = NEAR_FAR_TEMPLATE;
    assertSerialization(test);
  }

}
