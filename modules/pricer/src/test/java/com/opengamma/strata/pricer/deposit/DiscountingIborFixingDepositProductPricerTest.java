/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.deposit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.datasets.ImmutableRatesProviderSimpleData;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDeposit;

/**
 * Test {@link DiscountingIborFixingDepositProductPricer}.
 */
public class DiscountingIborFixingDepositProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = ImmutableRatesProviderSimpleData.VAL_DATE;
  private static final LocalDate START_DATE = EUR_EURIBOR_6M.calculateEffectiveFromFixing(VAL_DATE, REF_DATA);
  private static final LocalDate END_DATE = EUR_EURIBOR_6M.calculateMaturityFromEffective(START_DATE, REF_DATA);
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
  private static final ResolvedIborFixingDeposit RDEPOSIT = DEPOSIT.resolve(REF_DATA);
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
  @Test
  public void test_presentValue_noFixing() {
    double discountFactor = IMM_PROV_NOFIX.discountFactor(EUR, END_DATE);
    double forwardRate = IMM_PROV_NOFIX.iborIndexRates(EUR_EURIBOR_6M).rate(RDEPOSIT.getFloatingRate().getObservation());
    CurrencyAmount computed = PRICER.presentValue(RDEPOSIT, IMM_PROV_NOFIX);
    double expected = NOTIONAL * discountFactor * (RATE - forwardRate) * RDEPOSIT.getYearFraction();
    assertThat(computed.getCurrency()).isEqualTo(EUR);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(TOLERANCE_PV));
  }
  
  @Test
  public void test_presentValue_fixing() {
    CurrencyAmount computedNoFix = PRICER.presentValue(RDEPOSIT, IMM_PROV_NOFIX);
    CurrencyAmount computedFix = PRICER.presentValue(RDEPOSIT, IMM_PROV_FIX); // Fixing should not be taken into account
    assertThat(computedFix.getCurrency()).isEqualTo(EUR);
    assertThat(computedFix.getAmount()).isCloseTo(computedNoFix.getAmount(), offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_noFixing() {
    PointSensitivities computed = PRICER.presentValueSensitivity(RDEPOSIT, IMM_PROV_NOFIX);
    CurrencyParameterSensitivities sensiComputed = IMM_PROV_NOFIX.parameterSensitivity(computed);
    CurrencyParameterSensitivities sensiExpected =
        CAL_FD.sensitivity(IMM_PROV_NOFIX, (p) -> PRICER.presentValue(RDEPOSIT, (p)));
    assertThat(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * EPS_FD)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValueSensitivity_fixing() {
    PointSensitivities computedNoFix = PRICER.presentValueSensitivity(RDEPOSIT, IMM_PROV_NOFIX);
    CurrencyParameterSensitivities sensiComputedNoFix = IMM_PROV_NOFIX.parameterSensitivity(computedNoFix);
    PointSensitivities computedFix = PRICER.presentValueSensitivity(RDEPOSIT, IMM_PROV_FIX);
    CurrencyParameterSensitivities sensiComputedFix = IMM_PROV_NOFIX.parameterSensitivity(computedFix);
    assertThat(sensiComputedNoFix.equalWithTolerance(sensiComputedFix, TOLERANCE_PV_DELTA)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parRate() {
    double parRate = PRICER.parRate(RDEPOSIT, IMM_PROV_NOFIX);
    IborFixingDeposit deposit0 = DEPOSIT.toBuilder().fixedRate(parRate).build();
    CurrencyAmount pv0 = PRICER.presentValue(deposit0.resolve(REF_DATA), IMM_PROV_NOFIX);
    assertThat(pv0.getAmount()).isCloseTo(0, offset(TOLERANCE_RATE));
    double parRate2 = PRICER.parRate(RDEPOSIT, IMM_PROV_NOFIX);
    assertThat(parRate).isCloseTo(parRate2, offset(TOLERANCE_RATE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parSpread_noFixing() {
    double parSpread = PRICER.parSpread(RDEPOSIT, IMM_PROV_NOFIX);
    IborFixingDeposit deposit0 = DEPOSIT.toBuilder().fixedRate(RATE + parSpread).build();
    CurrencyAmount pv0 = PRICER.presentValue(deposit0.resolve(REF_DATA), IMM_PROV_NOFIX);
    assertThat(pv0.getAmount()).isCloseTo(0, offset(TOLERANCE_RATE));
    double parSpread2 = PRICER.parSpread(RDEPOSIT, IMM_PROV_NOFIX);
    assertThat(parSpread).isCloseTo(parSpread2, offset(TOLERANCE_RATE));
  }
  
  @Test
  public void test_parSpread_fixing() {
    double parSpread1 = PRICER.parSpread(RDEPOSIT, IMM_PROV_FIX);
    double parSpread2 = PRICER.parSpread(RDEPOSIT, IMM_PROV_NOFIX);
    assertThat(parSpread1).isCloseTo(parSpread2, offset(TOLERANCE_RATE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parSpreadSensitivity_noFixing() {
    PointSensitivities computedNoFix = PRICER.parSpreadSensitivity(RDEPOSIT, IMM_PROV_NOFIX);
    CurrencyParameterSensitivities sensiComputedNoFix = IMM_PROV_NOFIX.parameterSensitivity(computedNoFix);
    CurrencyParameterSensitivities sensiExpected =
        CAL_FD.sensitivity(IMM_PROV_NOFIX, (p) -> CurrencyAmount.of(EUR, PRICER.parSpread(RDEPOSIT, (p))));
    assertThat(sensiComputedNoFix.equalWithTolerance(sensiExpected, TOLERANCE_RATE_DELTA)).isTrue();
    // Par rate and par spread sensitivities are equal
    PointSensitivities computedParRateNoFix = PRICER.parRateSensitivity(RDEPOSIT, IMM_PROV_NOFIX);
    CurrencyParameterSensitivities sensiComputedParRateNoFix = IMM_PROV_NOFIX.parameterSensitivity(computedParRateNoFix);
    assertThat(sensiComputedNoFix.equalWithTolerance(sensiComputedParRateNoFix, TOLERANCE_RATE_DELTA)).isTrue();
    PointSensitivities computedFix = PRICER.parSpreadSensitivity(RDEPOSIT, IMM_PROV_FIX);
    CurrencyParameterSensitivities sensiComputedFix = IMM_PROV_NOFIX.parameterSensitivity(computedFix);
    assertThat(sensiComputedFix.equalWithTolerance(sensiExpected, TOLERANCE_RATE_DELTA)).isTrue();
  }
  
  @Test
  public void test_parSpreadSensitivity_fixing() {
    PointSensitivities computedNoFix = PRICER.parSpreadSensitivity(RDEPOSIT, IMM_PROV_NOFIX);
    PointSensitivities computedFix = PRICER.parSpreadSensitivity(RDEPOSIT, IMM_PROV_FIX);
    assertThat(computedNoFix.equalWithTolerance(computedFix, TOLERANCE_PV_DELTA)).isTrue();
    PointSensitivities computedParRateFix = PRICER.parRateSensitivity(RDEPOSIT, IMM_PROV_FIX);
    assertThat(computedParRateFix.equalWithTolerance(computedFix, TOLERANCE_PV_DELTA)).isTrue();
  }

}
