/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.deposit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.finance.rate.deposit.ExpandedIborFixingDeposit;
import com.opengamma.strata.finance.rate.deposit.IborFixingDeposit;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.datasets.ImmutableRatesProviderSimpleData;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link DiscountingIborFixingDepositProductPricer}.
 */
@Test
public class DiscountingIborFixingDepositProductPricerTest {

  private static final LocalDate VAL_DATE = ImmutableRatesProviderSimpleData.VAL_DATE;
  private static final LocalDate START_DATE = EUR_EURIBOR_6M.calculateEffectiveFromFixing(VAL_DATE);
  private static final LocalDate END_DATE = EUR_EURIBOR_6M.calculateMaturityFromEffective(START_DATE);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0150;
  private static final BusinessDayAdjustment BD_ADJ = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final IborFixingDeposit DEPOSIT = IborFixingDeposit.builder()
      .buySell(BuySell.BUY)
      .notional(NOTIONAL)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .businessDayAdjustment(BD_ADJ)
      .index(EUR_EURIBOR_6M)
      .fixedRate(RATE)
      .build();
  private static final double TOLERANCE_PV = 1E-2;
  private static final double TOLERANCE_PV_DELTA = 1E-2;
  private static final double TOLERANCE_RATE = 1E-8;
  private static final double TOLERANCE_RATE_DELTA = 1E-6;

  private static final double EPS_FD = 1E-7;
  private static final RatesFiniteDifferenceSensitivityCalculator CAL_FD =
      new RatesFiniteDifferenceSensitivityCalculator(EPS_FD);
  private static final ImmutableRatesProvider IMM_PROV_NOFIX = ImmutableRatesProviderSimpleData.IMM_PROV_EUR_NOFIX;
  private static final ImmutableRatesProvider IMM_PROV_FIX = ImmutableRatesProviderSimpleData.IMM_PROV_EUR_FIX;

  private static final DiscountingIborFixingDepositProductPricer PRICER = 
      DiscountingIborFixingDepositProductPricer.DEFAULT;
  
  //-------------------------------------------------------------------------
  public void present_value_no_fixing() {
    ExpandedIborFixingDeposit deposit = DEPOSIT.expand();
    double discountFactor = IMM_PROV_NOFIX.discountFactor(EUR, END_DATE);
    double forwardRate = IMM_PROV_NOFIX.iborIndexRates(EUR_EURIBOR_6M).rate(deposit.getFloatingRate().getFixingDate());
    CurrencyAmount computed = PRICER.presentValue(DEPOSIT, IMM_PROV_NOFIX);
    double expected = NOTIONAL * discountFactor * (RATE - forwardRate) * deposit.getYearFraction();
    assertEquals(computed.getCurrency(), EUR);
    assertEquals(computed.getAmount(), expected, TOLERANCE_PV);
  }
  
  public void present_value_fixing() {
    CurrencyAmount computedNoFix = PRICER.presentValue(DEPOSIT, IMM_PROV_NOFIX);
    CurrencyAmount computedFix = PRICER.presentValue(DEPOSIT, IMM_PROV_FIX); // Fixing should not be taken into account
    assertEquals(computedFix.getCurrency(), EUR);
    assertEquals(computedFix.getAmount(), computedNoFix.getAmount(), TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void present_value_sensitivity_no_fixing() {
    PointSensitivities computed = PRICER.presentValueSensitivity(DEPOSIT, IMM_PROV_NOFIX);
    CurveCurrencyParameterSensitivities sensiComputed = IMM_PROV_NOFIX.curveParameterSensitivity(computed);
    CurveCurrencyParameterSensitivities sensiExpected = 
        CAL_FD.sensitivity(IMM_PROV_NOFIX, (p) -> PRICER.presentValue(DEPOSIT, (p)));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * EPS_FD));
  }

  //-------------------------------------------------------------------------
  public void present_value_sensitivity_fixing() {
    PointSensitivities computedNoFix = PRICER.presentValueSensitivity(DEPOSIT, IMM_PROV_NOFIX);
    CurveCurrencyParameterSensitivities sensiComputedNoFix = IMM_PROV_NOFIX.curveParameterSensitivity(computedNoFix);
    PointSensitivities computedFix = PRICER.presentValueSensitivity(DEPOSIT, IMM_PROV_FIX);
    CurveCurrencyParameterSensitivities sensiComputedFix = IMM_PROV_NOFIX.curveParameterSensitivity(computedFix);
    assertTrue(sensiComputedNoFix.equalWithTolerance(sensiComputedFix, TOLERANCE_PV_DELTA));
  }

  //-------------------------------------------------------------------------
  public void par_rate() {
    double parRate = PRICER.parRate(DEPOSIT, IMM_PROV_NOFIX);
    IborFixingDeposit deposit0 = DEPOSIT.toBuilder().fixedRate(parRate).build();
    CurrencyAmount pv0 = PRICER.presentValue(deposit0, IMM_PROV_NOFIX);
    assertEquals(pv0.getAmount(), 0, TOLERANCE_RATE);
    double parRate2 = PRICER.parRate(DEPOSIT, IMM_PROV_NOFIX);
    assertEquals(parRate, parRate2, TOLERANCE_RATE);
  }

  //-------------------------------------------------------------------------
  public void par_spread() {
    double parSpread = PRICER.parSpread(DEPOSIT, IMM_PROV_NOFIX);
    IborFixingDeposit deposit0 = DEPOSIT.toBuilder().fixedRate(RATE + parSpread).build();
    CurrencyAmount pv0 = PRICER.presentValue(deposit0, IMM_PROV_NOFIX);
    assertEquals(pv0.getAmount(), 0, TOLERANCE_RATE);
    double parSpread2 = PRICER.parSpread(DEPOSIT, IMM_PROV_NOFIX);
    assertEquals(parSpread, parSpread2, TOLERANCE_RATE);
  }

  //-------------------------------------------------------------------------
  public void par_spread_sensitivity() {
    PointSensitivities computedNoFix = PRICER.parSpreadSensitivity(DEPOSIT, IMM_PROV_NOFIX);
    CurveCurrencyParameterSensitivities sensiComputedNoFix = IMM_PROV_NOFIX.curveParameterSensitivity(computedNoFix);
    CurveCurrencyParameterSensitivities sensiExpected =
        CAL_FD.sensitivity(IMM_PROV_NOFIX, (p) -> CurrencyAmount.of(EUR, PRICER.parSpread(DEPOSIT, (p))));
    assertTrue(sensiComputedNoFix.equalWithTolerance(sensiExpected, TOLERANCE_RATE_DELTA));
    PointSensitivities computedFix = PRICER.parSpreadSensitivity(DEPOSIT, IMM_PROV_FIX);
    CurveCurrencyParameterSensitivities sensiComputedFix = IMM_PROV_NOFIX.curveParameterSensitivity(computedFix);
    assertTrue(sensiComputedFix.equalWithTolerance(sensiExpected, TOLERANCE_RATE_DELTA));
  }

}
