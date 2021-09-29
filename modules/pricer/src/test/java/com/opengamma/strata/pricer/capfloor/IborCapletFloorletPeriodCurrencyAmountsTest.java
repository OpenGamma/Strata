/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link IborCapletFloorletPeriodCurrencyAmounts}.
 */
class IborCapletFloorletPeriodCurrencyAmountsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING = LocalDate.of(2011, 1, 3);
  private static final double NOTIONAL = 1_000_000;
  private static final double STRIKE = 0.01;
  private static final IborRateComputation RATE_COMP = IborRateComputation.of(EUR_EURIBOR_3M, FIXING, REF_DATA);
  private static final IborCapletFloorletPeriod CAPLET_LONG = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .notional(NOTIONAL)
      .iborRate(RATE_COMP)
      .build();
  private static final IborCapletFloorletPeriod CAPLET_SHORT = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .notional(NOTIONAL * -1)
      .iborRate(RATE_COMP)
      .build();
  private static final CurrencyAmount CURRENCY_AMOUNT = CurrencyAmount.of(Currency.USD, 1d);
  private static final Map<IborCapletFloorletPeriod, CurrencyAmount> CAPLET_CURRENCY_AMOUNT_MAP = ImmutableMap.of(
      CAPLET_LONG, CURRENCY_AMOUNT);

  @Test
  void test_of() {
    IborCapletFloorletPeriodCurrencyAmounts test =
        IborCapletFloorletPeriodCurrencyAmounts.of(CAPLET_CURRENCY_AMOUNT_MAP);
    assertThat(test.getAmounts()).isEqualTo(CAPLET_CURRENCY_AMOUNT_MAP);
  }

  @Test
  void test_findAmount() {
    IborCapletFloorletPeriodCurrencyAmounts test =
        IborCapletFloorletPeriodCurrencyAmounts.of(CAPLET_CURRENCY_AMOUNT_MAP);
    assertThat(test.findAmount(CAPLET_LONG)).contains(CURRENCY_AMOUNT);
  }

  @Test
  void test_findAmountEmpty() {
    IborCapletFloorletPeriodCurrencyAmounts test =
        IborCapletFloorletPeriodCurrencyAmounts.of(CAPLET_CURRENCY_AMOUNT_MAP);
    assertThat(test.findAmount(CAPLET_SHORT)).isEmpty();
  }

  @Test
  void test_getAmount() {
    IborCapletFloorletPeriodCurrencyAmounts test =
        IborCapletFloorletPeriodCurrencyAmounts.of(CAPLET_CURRENCY_AMOUNT_MAP);
    assertThat(test.getAmount(CAPLET_LONG)).isEqualTo(CURRENCY_AMOUNT);
  }

  @Test
  void test_getAmountThrows() {
    IborCapletFloorletPeriodCurrencyAmounts test =
        IborCapletFloorletPeriodCurrencyAmounts.of(CAPLET_CURRENCY_AMOUNT_MAP);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAmount(CAPLET_SHORT))
        .withMessage("Could not find currency amount for " + CAPLET_SHORT);
  }

  @Test
  void coverage() {
    IborCapletFloorletPeriodCurrencyAmounts test =
        IborCapletFloorletPeriodCurrencyAmounts.of(CAPLET_CURRENCY_AMOUNT_MAP);
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    IborCapletFloorletPeriodCurrencyAmounts test =
        IborCapletFloorletPeriodCurrencyAmounts.of(CAPLET_CURRENCY_AMOUNT_MAP);
    assertSerialization(test);
  }

}
