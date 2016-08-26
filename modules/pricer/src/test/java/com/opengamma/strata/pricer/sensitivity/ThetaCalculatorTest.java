/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions.GBP_LIBOR_3M_USD_LIBOR_3M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.RatesProviderDecoratedForward;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

/**
 * Tests {@link ThetaCalculator}.
 */
@Test
public class ThetaCalculatorTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_FWD = LocalDate.of(2015, 6, 5);
  private static final RatesProvider PROVIDER_START = RatesProviderDataSets.multiGbpUsd(DATE_VAL).toBuilder()
      .timeSeries(USD_LIBOR_3M,
          LocalDateDoubleTimeSeries.builder()
              .put(LocalDate.of(2015, 3, 2), 0.00234).put(LocalDate.of(2015, 6, 2), 0.00235).build())
      .timeSeries(GBP_LIBOR_3M,
          LocalDateDoubleTimeSeries.builder()
              .put(LocalDate.of(2015, 3, 4), 0.00432).put(LocalDate.of(2015, 6, 4), 0.00532).build())
      .build();
  private static final RatesProvider PROVIDER_FWD = RatesProviderDecoratedForward.of(PROVIDER_START, DATE_FWD);
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final double FIXED_RATE = 0.05; // Large coupon to have a large theta
  private static final double NOTIONAL = 100_000_000;
  private static final LocalDate START_FWD = LocalDate.of(2015, 6, 10);
  private static final LocalDate MATURITY_FWD = LocalDate.of(2017, 6, 10);
  private static final ResolvedSwapTrade SWAP_USD_FWD = USD_FIXED_6M_LIBOR_3M
      .toTrade(START_FWD, START_FWD, MATURITY_FWD, BuySell.BUY, NOTIONAL, FIXED_RATE).resolve(REF_DATA);
  private static final ResolvedSwapTrade SWAP_XCCY_FWD = GBP_LIBOR_3M_USD_LIBOR_3M
      .toTrade(DATE_VAL, START_FWD, MATURITY_FWD, BuySell.BUY, NOTIONAL * 1.55, NOTIONAL, FIXED_RATE).resolve(REF_DATA);
  private static final LocalDate START_CF = LocalDate.of(2015, 3, 4);
  private static final LocalDate MATURITY_CF = LocalDate.of(2017, 3, 4);
  private static final ResolvedSwapTrade SWAP_USD_CF = USD_FIXED_6M_LIBOR_3M  // Swap with a cf on DATE_VAL
      .toTrade(START_CF, START_CF, MATURITY_CF, BuySell.BUY, NOTIONAL, FIXED_RATE).resolve(REF_DATA);
  private static final ResolvedSwapTrade SWAP_XCCY_CF = GBP_LIBOR_3M_USD_LIBOR_3M
      .toTrade(START_CF, START_CF, MATURITY_CF, BuySell.BUY, NOTIONAL * 1.55, NOTIONAL, FIXED_RATE).resolve(REF_DATA);

  private static final DiscountingSwapTradePricer SWAP_PRICER = DiscountingSwapTradePricer.DEFAULT;
  private static final ThetaCalculator THETA_CAL = ThetaCalculator.DEFAULT;

  private static final double TOLERANCE = 1.0E-8;
  
  public void swap_fwd_singlecurrency_theta() {
    Function<RatesProvider, MultiCurrencyAmount> valueFunction =
        r -> SWAP_PRICER.presentValue(SWAP_USD_FWD, r);
    Function<RatesProvider, MultiCurrencyAmount> adjustmentFunction =
        r -> SWAP_PRICER.currentCash(SWAP_USD_FWD, r);
    MultiCurrencyAmount thetaComputed = THETA_CAL.theta(PROVIDER_START, DATE_FWD, valueFunction, adjustmentFunction);    
    MultiCurrencyAmount pv0 = SWAP_PRICER.presentValue(SWAP_USD_FWD, PROVIDER_START);
    MultiCurrencyAmount pv1 = SWAP_PRICER.presentValue(SWAP_USD_FWD, PROVIDER_FWD);
    MultiCurrencyAmount thetaExpected = pv1.minus(pv0);    
    assertTrue(thetaComputed.contains(USD));
    assertFalse(thetaComputed.contains(GBP));
    assertEquals(thetaComputed.getAmount(USD).getAmount(), thetaExpected.getAmount(USD).getAmount(), TOLERANCE);
  }
  
  public void swap_cf_singlecurrency_theta() {
    Function<RatesProvider, MultiCurrencyAmount> valueFunction =
        r -> SWAP_PRICER.presentValue(SWAP_USD_CF, r);
    Function<RatesProvider, MultiCurrencyAmount> adjustmentFunction =
        r -> SWAP_PRICER.currentCash(SWAP_USD_CF, r);
    MultiCurrencyAmount thetaComputed = THETA_CAL.theta(PROVIDER_START, DATE_FWD, valueFunction, adjustmentFunction);    
    MultiCurrencyAmount pv0 = SWAP_PRICER.presentValue(SWAP_USD_CF, PROVIDER_START);   
    MultiCurrencyAmount cc0 = SWAP_PRICER.currentCash(SWAP_USD_CF, PROVIDER_START);
    MultiCurrencyAmount pv1 = SWAP_PRICER.presentValue(SWAP_USD_CF, PROVIDER_FWD);
    MultiCurrencyAmount thetaExpected = pv1.minus(pv0.minus(cc0));    
    assertTrue(thetaComputed.contains(USD));
    assertFalse(thetaComputed.contains(GBP));
    assertEquals(thetaComputed.getAmount(USD).getAmount(), thetaExpected.getAmount(USD).getAmount(), TOLERANCE);
  }
  
  public void swap_fwd_xccy_theta() {
    Function<RatesProvider, MultiCurrencyAmount> valueFunction =
        r -> SWAP_PRICER.presentValue(SWAP_XCCY_FWD, r);
    Function<RatesProvider, MultiCurrencyAmount> adjustmentFunction =
        r -> SWAP_PRICER.currentCash(SWAP_XCCY_FWD, r);
    MultiCurrencyAmount thetaComputed = THETA_CAL.theta(PROVIDER_START, DATE_FWD, valueFunction, adjustmentFunction);    
    MultiCurrencyAmount pv0 = SWAP_PRICER.presentValue(SWAP_XCCY_FWD, PROVIDER_START);
    MultiCurrencyAmount pv1 = SWAP_PRICER.presentValue(SWAP_XCCY_FWD, PROVIDER_FWD);
    MultiCurrencyAmount thetaExpected = pv1.minus(pv0);    
    assertTrue(thetaComputed.contains(USD));
    assertTrue(thetaComputed.contains(GBP));
    assertEquals(thetaComputed.getAmount(USD).getAmount(), thetaExpected.getAmount(USD).getAmount(), TOLERANCE);
    assertEquals(thetaComputed.getAmount(GBP).getAmount(), thetaExpected.getAmount(GBP).getAmount(), TOLERANCE);
  }
  
  public void swap_cf_xccy_theta() {
    Function<RatesProvider, MultiCurrencyAmount> valueFunction =
        r -> SWAP_PRICER.presentValue(SWAP_XCCY_CF, r);
    Function<RatesProvider, MultiCurrencyAmount> adjustmentFunction =
        r -> SWAP_PRICER.currentCash(SWAP_XCCY_CF, r);
    MultiCurrencyAmount thetaComputed = THETA_CAL.theta(PROVIDER_START, DATE_FWD, valueFunction, adjustmentFunction);    
    MultiCurrencyAmount pv0 = SWAP_PRICER.presentValue(SWAP_XCCY_CF, PROVIDER_START);   
    MultiCurrencyAmount cc0 = SWAP_PRICER.currentCash(SWAP_XCCY_CF, PROVIDER_START);
    MultiCurrencyAmount pv1 = SWAP_PRICER.presentValue(SWAP_XCCY_CF, PROVIDER_FWD);
    MultiCurrencyAmount thetaExpected = pv1.minus(pv0.minus(cc0));    
    assertTrue(thetaComputed.contains(USD));
    assertTrue(thetaComputed.contains(GBP));
    assertEquals(thetaComputed.getAmount(USD).getAmount(), thetaExpected.getAmount(USD).getAmount(), TOLERANCE);
    assertEquals(thetaComputed.getAmount(GBP).getAmount(), thetaExpected.getAmount(GBP).getAmount(), TOLERANCE);
  }

  @SuppressWarnings("unused")
  @Test(enabled = false) // To estimate the speed of the computation. Not an actual unit test. 
  public void performance() {
    int nbTest = 10000;
    int nbRep = 10;
    long start, end;

    for (int looprep = 0; looprep < nbRep; looprep++) {
      System.out.println("Computation time - repetition " + looprep);
      start = System.currentTimeMillis();
      for (int i = 0; i < nbTest; i++) {
        MultiCurrencyAmount pv = SWAP_PRICER.presentValue(SWAP_USD_CF, PROVIDER_START);
      }
      end = System.currentTimeMillis();
      System.out.println("  |--> " + nbTest + " pv: " + (end - start) + " ms."); // 25 ms for 10,000

      start = System.currentTimeMillis();
      for (int i = 0; i < nbTest; i++) {
        Function<RatesProvider, MultiCurrencyAmount> valueFunction =
            r -> SWAP_PRICER.presentValue(SWAP_USD_CF, r);
        Function<RatesProvider, MultiCurrencyAmount> adjustmentFunction =
            r -> SWAP_PRICER.currentCash(SWAP_USD_CF, r);
        MultiCurrencyAmount theta = THETA_CAL.theta(PROVIDER_START, DATE_FWD, valueFunction, adjustmentFunction);

      }
      end = System.currentTimeMillis();
      System.out.println("  |--> " + nbTest + " theta: " + (end - start) + " ms."); // 70 ms for 10,000
    }

  }

}
