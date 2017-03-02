/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.deposit;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDeposit;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDepositTrade;

/**
 * Tests {@link DiscountingIborFixingDepositTradePricer}.
 */
@Test
public class DiscountingIborFixingDepositTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 16);

  private static final LocalDate START_DATE = LocalDate.of(2014, 1, 24);
  private static final LocalDate END_DATE = LocalDate.of(2014, 7, 24);
  private static final double NOTIONAL = 100_000_000d;
  private static final double RATE = 0.0150;
  private static final BusinessDayAdjustment BD_ADJ = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA);
  private static final IborFixingDeposit DEPOSIT_PRODUCT = IborFixingDeposit.builder()
      .buySell(BuySell.BUY)
      .notional(NOTIONAL)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .businessDayAdjustment(BD_ADJ)
      .index(EUR_EURIBOR_6M)
      .fixedRate(RATE)
      .build();
  private static final ResolvedIborFixingDeposit RDEPOSIT_PRODUCT = DEPOSIT_PRODUCT.resolve(REF_DATA);
  private static final IborFixingDepositTrade DEPOSIT_TRADE =
      IborFixingDepositTrade.builder().product(DEPOSIT_PRODUCT).info(TradeInfo.empty()).build();
  private static final ResolvedIborFixingDepositTrade RDEPOSIT_TRADE = DEPOSIT_TRADE.resolve(REF_DATA);

  private static final ImmutableRatesProvider IMM_PROV;
  static {
    CurveInterpolator interp = CurveInterpolators.DOUBLE_QUADRATIC;
    DoubleArray time_eur = DoubleArray.of(0.0, 0.1, 0.25, 0.5, 0.75, 1.0, 2.0);
    DoubleArray rate_eur = DoubleArray.of(0.0160, 0.0165, 0.0155, 0.0155, 0.0155, 0.0150, 0.014);
    InterpolatedNodalCurve dscCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("EUR-Discount", ACT_ACT_ISDA), time_eur, rate_eur, interp);
    DoubleArray time_index = DoubleArray.of(0.0, 0.25, 0.5, 1.0);
    DoubleArray rate_index = DoubleArray.of(0.0180, 0.0180, 0.0175, 0.0165);
    InterpolatedNodalCurve indexCurve =
        InterpolatedNodalCurve.of(Curves.zeroRates("EUR-EURIBOR6M", ACT_ACT_ISDA), time_index, rate_index, interp);
    IMM_PROV = ImmutableRatesProvider.builder(VAL_DATE)
        .discountCurve(EUR, dscCurve)
        .iborIndexCurve(EUR_EURIBOR_6M, indexCurve)
        .build();
  }
  
  private static final DiscountingIborFixingDepositProductPricer PRICER_PRODUCT =
      DiscountingIborFixingDepositProductPricer.DEFAULT;
  private static final DiscountingIborFixingDepositTradePricer PRICER_TRADE =
      DiscountingIborFixingDepositTradePricer.DEFAULT;


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

}
