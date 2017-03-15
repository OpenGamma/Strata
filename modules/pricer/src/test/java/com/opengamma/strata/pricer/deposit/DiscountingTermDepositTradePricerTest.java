/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.deposit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.ResolvedTermDeposit;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;

/**
 * Tests {@link DiscountingTermDepositTradePricer}.
 */
@Test
public class DiscountingTermDepositTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2014, 1, 22);

  private static final LocalDate START_DATE = date(2014, 1, 24);
  private static final LocalDate END_DATE = date(2014, 7, 24);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0750;
  private static final double INTEREST = NOTIONAL * RATE * ACT_360.yearFraction(START_DATE, END_DATE);
  private static final BusinessDayAdjustment BD_ADJ = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final TermDeposit DEPOSIT_PRODUCT = TermDeposit.builder()
      .buySell(BuySell.BUY)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .businessDayAdjustment(BD_ADJ)
      .dayCount(ACT_360)
      .notional(NOTIONAL)
      .currency(EUR)
      .rate(RATE)
      .build();
  private static final ResolvedTermDeposit RDEPOSIT_PRODUCT = DEPOSIT_PRODUCT.resolve(REF_DATA);
  private static final TermDepositTrade DEPOSIT_TRADE =
      TermDepositTrade.builder().product(DEPOSIT_PRODUCT).info(TradeInfo.empty()).build();
  private static final ResolvedTermDepositTrade RDEPOSIT_TRADE = DEPOSIT_TRADE.resolve(REF_DATA);

  private static final Curve CURVE;;
  private static final ImmutableRatesProvider IMM_PROV;
  static {
    CurveInterpolator interp = CurveInterpolators.DOUBLE_QUADRATIC;
    DoubleArray time_eur = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0);
    DoubleArray rate_eur = DoubleArray.of(0.0160, 0.0135, 0.0160, 0.0185, 0.0185, 0.0195, 0.0200, 0.0210);
    CURVE = InterpolatedNodalCurve.of(Curves.zeroRates("EUR-Discount", ACT_360), time_eur, rate_eur, interp);
    IMM_PROV = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(EUR, CURVE)
        .build();
  }
  double DF_END = 0.94;
  
  private static final DiscountingTermDepositProductPricer PRICER_PRODUCT =
      DiscountingTermDepositProductPricer.DEFAULT;
  private static final DiscountingTermDepositTradePricer PRICER_TRADE =
      DiscountingTermDepositTradePricer.DEFAULT;


  private static final double TOLERANCE_PV = 1E-2;
  private static final double TOLERANCE_PV_DELTA = 1E-2;
  private static final double TOLERANCE_RATE = 1E-8;
  
  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount pvTrade = PRICER_TRADE.presentValue(RDEPOSIT_TRADE, IMM_PROV);
    CurrencyAmount pvProduct = PRICER_PRODUCT.presentValue(RDEPOSIT_PRODUCT, IMM_PROV);
    assertEquals(pvTrade.getCurrency(), pvProduct.getCurrency());
    assertEquals(pvTrade.getAmount(), pvProduct.getAmount(), TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivities ptsTrade = PRICER_TRADE.presentValueSensitivity(RDEPOSIT_TRADE, IMM_PROV);
    PointSensitivities ptsProduct = PRICER_PRODUCT.presentValueSensitivity(RDEPOSIT_PRODUCT, IMM_PROV);
    assertTrue(ptsTrade.equalWithTolerance(ptsProduct, TOLERANCE_PV_DELTA));
  }

  //-------------------------------------------------------------------------
  public void test_parRate() {
    double psTrade = PRICER_TRADE.parRate(RDEPOSIT_TRADE, IMM_PROV);
    double psProduct = PRICER_PRODUCT.parRate(RDEPOSIT_PRODUCT, IMM_PROV);
    assertEquals(psTrade, psProduct, TOLERANCE_RATE);

  }

  //-------------------------------------------------------------------------
  public void test_parRateSensitivity() {
    PointSensitivities ptsTrade = PRICER_TRADE.parRateSensitivity(RDEPOSIT_TRADE, IMM_PROV);
    PointSensitivities ptsProduct = PRICER_PRODUCT.parRateSensitivity(RDEPOSIT_PRODUCT, IMM_PROV);
    assertTrue(ptsTrade.equalWithTolerance(ptsProduct, TOLERANCE_PV_DELTA));
  }

  //-------------------------------------------------------------------------
  public void test_parSpread() {
    double psTrade = PRICER_TRADE.parSpread(RDEPOSIT_TRADE, IMM_PROV);
    double psProduct = PRICER_PRODUCT.parSpread(RDEPOSIT_PRODUCT, IMM_PROV);
    assertEquals(psTrade, psProduct, TOLERANCE_RATE);
    
  }

  //-------------------------------------------------------------------------
  public void test_parSpreadSensitivity() {
    PointSensitivities ptsTrade = PRICER_TRADE.parSpreadSensitivity(RDEPOSIT_TRADE, IMM_PROV);
    PointSensitivities ptsProduct = PRICER_PRODUCT.parSpreadSensitivity(RDEPOSIT_PRODUCT, IMM_PROV);
    assertTrue(ptsTrade.equalWithTolerance(ptsProduct, TOLERANCE_PV_DELTA));    
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    assertEquals(
        PRICER_TRADE.currencyExposure(RDEPOSIT_TRADE, IMM_PROV),
        MultiCurrencyAmount.of(PRICER_TRADE.presentValue(RDEPOSIT_TRADE, IMM_PROV)));
  }

  public void test_currentCash_onStartDate() {
    RatesProvider prov = ImmutableRatesProvider.builder(RDEPOSIT_TRADE.getProduct().getStartDate())
        .discountCurve(EUR, CURVE)
        .build();
    assertEquals(PRICER_TRADE.currentCash(RDEPOSIT_TRADE, prov), CurrencyAmount.of(EUR, -NOTIONAL));
  }

  public void test_currentCash_onEndDate() {
    RatesProvider prov = ImmutableRatesProvider.builder(RDEPOSIT_TRADE.getProduct().getEndDate())
        .discountCurve(EUR, CURVE)
        .build();
    assertEquals(PRICER_TRADE.currentCash(RDEPOSIT_TRADE, prov), CurrencyAmount.of(EUR, NOTIONAL + INTEREST));
  }

  public void test_currentCash_otherDate() {
    assertEquals(PRICER_TRADE.currentCash(RDEPOSIT_TRADE, IMM_PROV), CurrencyAmount.zero(EUR));
  }

}
