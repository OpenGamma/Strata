/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.scenario.MarketDataBox;

/**
 * Test {@link FxRateShifts}.
 */
@Test
public class FxRateShiftsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final DoubleArray SHIFT_AMOUNT_1 = DoubleArray.of(0.95d, 0.2d, -0.5d);
  private static final DoubleArray SHIFT_AMOUNT_2 = DoubleArray.of(0.85d, 1.2d, 1.5d);
  private static final CurrencyPair EURUSD = CurrencyPair.of(EUR, USD);
  private static final CurrencyPair USDEUR = CurrencyPair.of(USD, EUR);
  private static final double BASE_RATE = 1.2d;
  private static final FxRate FX_RATE = FxRate.of(EURUSD, BASE_RATE);

  public void test_of() {
    FxRateShifts test = FxRateShifts.of(ShiftType.SCALED, SHIFT_AMOUNT_2, EURUSD);
    assertEquals(test.getCurrencyPair(), EURUSD);
    assertEquals(test.getScenarioCount(), 3);
    assertEquals(test.getShiftAmount(), SHIFT_AMOUNT_2);
    assertEquals(test.getShiftType(), ShiftType.SCALED);
  }

  public void test_applyTo() {
    MarketDataBox<FxRate> marketData = MarketDataBox.ofSingleValue(FX_RATE);
    FxRateShifts testScaled = FxRateShifts.of(ShiftType.SCALED, SHIFT_AMOUNT_2, EURUSD);
    MarketDataBox<FxRate> computedScaled = testScaled.applyTo(marketData, REF_DATA);
    MarketDataBox<FxRate> expectedScaled = MarketDataBox.ofScenarioValues(ImmutableList.of(
        FxRate.of(EURUSD, BASE_RATE * SHIFT_AMOUNT_2.get(0)),
        FxRate.of(EURUSD, BASE_RATE * SHIFT_AMOUNT_2.get(1)),
        FxRate.of(EURUSD, BASE_RATE * SHIFT_AMOUNT_2.get(2))));
    assertEquals(computedScaled, expectedScaled);
    FxRateShifts testScaledInv = FxRateShifts.of(ShiftType.SCALED, SHIFT_AMOUNT_2, USDEUR);
    MarketDataBox<FxRate> computedScaledInv = testScaledInv.applyTo(marketData, REF_DATA);
    MarketDataBox<FxRate> expectedScaledInv = MarketDataBox.ofScenarioValues(ImmutableList.of(
        FxRate.of(USDEUR, 1d / BASE_RATE * SHIFT_AMOUNT_2.get(0)),
        FxRate.of(USDEUR, 1d / BASE_RATE * SHIFT_AMOUNT_2.get(1)),
        FxRate.of(USDEUR, 1d / BASE_RATE * SHIFT_AMOUNT_2.get(2))));
    assertEquals(computedScaledInv, expectedScaledInv);
    FxRateShifts testAbsolute = FxRateShifts.of(ShiftType.ABSOLUTE, SHIFT_AMOUNT_1, EURUSD);
    MarketDataBox<FxRate> computedAbsolute = testAbsolute.applyTo(marketData, REF_DATA);
    MarketDataBox<FxRate> expectedAbsolute = MarketDataBox.ofScenarioValues(ImmutableList.of(
        FxRate.of(EURUSD, BASE_RATE + SHIFT_AMOUNT_1.get(0)),
        FxRate.of(EURUSD, BASE_RATE + SHIFT_AMOUNT_1.get(1)),
        FxRate.of(EURUSD, BASE_RATE + SHIFT_AMOUNT_1.get(2))));
    assertEquals(computedAbsolute, expectedAbsolute);
    FxRateShifts testRelative = FxRateShifts.of(ShiftType.RELATIVE, SHIFT_AMOUNT_1, EURUSD);
    MarketDataBox<FxRate> computedRelative = testRelative.applyTo(marketData, REF_DATA);
    MarketDataBox<FxRate> expectedRelative = MarketDataBox.ofScenarioValues(ImmutableList.of(
        FxRate.of(EURUSD, BASE_RATE * (1d + SHIFT_AMOUNT_1.get(0))),
        FxRate.of(EURUSD, BASE_RATE * (1d + SHIFT_AMOUNT_1.get(1))),
        FxRate.of(EURUSD, BASE_RATE * (1d + SHIFT_AMOUNT_1.get(2)))));
    assertEquals(computedRelative, expectedRelative);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxRateShifts test1 = FxRateShifts.of(ShiftType.SCALED, SHIFT_AMOUNT_2, EURUSD);
    coverImmutableBean(test1);
    FxRateShifts test2 = FxRateShifts.of(ShiftType.ABSOLUTE, SHIFT_AMOUNT_1, USDEUR);
    coverBeanEquals(test1, test2);
  }

}
