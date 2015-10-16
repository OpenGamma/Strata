/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.UnitSecurity;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFuture;
import com.opengamma.strata.finance.rate.future.DeliverableSwapFutureTrade;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;

/**
 * Test {@link DiscountingDeliverableSwapFutureTradePricer}.
 */
@Test
public class DiscountingDeliverableSwapFutureTradePricerTest {
  // curves
  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;
  private static final LocalDate VALUATION = LocalDate.of(2013, 3, 28);
  private static final DoubleArray USD_DSC_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray USD_DSC_RATE = DoubleArray.of(0.0100, 0.0120, 0.0120, 0.0140, 0.0140, 0.0140);
  private static final CurveName USD_DSC_NAME = CurveName.of("USD Dsc");
  private static final CurveMetadata USD_DSC_METADATA = Curves.zeroRates(USD_DSC_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve USD_DSC =
      InterpolatedNodalCurve.of(USD_DSC_METADATA, USD_DSC_TIME, USD_DSC_RATE, INTERPOLATOR);
  private static final DoubleArray USD_FWD3_TIME = DoubleArray.of(0.0, 0.5, 1.0, 2.0, 5.0, 10.0);
  private static final DoubleArray USD_FWD3_RATE = DoubleArray.of(0.0150, 0.0125, 0.0150, 0.0175, 0.0150, 0.0150);
  private static final CurveName USD_FWD3_NAME = CurveName.of("USD LIBOR 3M");
  private static final CurveMetadata USD_FWD3_METADATA = Curves.zeroRates(USD_FWD3_NAME, ACT_ACT_ISDA);
  private static final InterpolatedNodalCurve USD_FWD3 =
      InterpolatedNodalCurve.of(USD_FWD3_METADATA, USD_FWD3_TIME, USD_FWD3_RATE, INTERPOLATOR);
  private static final ImmutableRatesProvider PROVIDER = ImmutableRatesProvider.builder()
      .valuationDate(VALUATION)
      .fxMatrix(FxMatrix.empty())
      .discountCurves(ImmutableMap.of(USD, USD_DSC))
      .indexCurves(ImmutableMap.of(USD_LIBOR_3M, USD_FWD3))
      .build();
  // underlying swap
  private static final NotionalSchedule UNIT_NOTIONAL = NotionalSchedule.of(USD, 1d);
  private static final HolidayCalendar CALENDAR = HolidayCalendars.SAT_SUN;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CALENDAR);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, CALENDAR);
  private static final LocalDate START = LocalDate.of(2013, 6, 19);
  private static final LocalDate END = START.plusYears(10);
  private static final double RATE = 0.0175;
  private static final SwapLeg FIXED_LEG = RateCalculationSwapLeg.builder()
      .payReceive(RECEIVE)
      .accrualSchedule(PeriodicSchedule.builder()
          .startDate(START)
          .endDate(END)
          .frequency(P6M)
          .businessDayAdjustment(BDA_MF)
          .stubConvention(StubConvention.SHORT_FINAL)
          .build())
      .paymentSchedule(PaymentSchedule.builder()
          .paymentFrequency(P6M)
          .paymentDateOffset(DaysAdjustment.NONE)
          .build())
      .notionalSchedule(UNIT_NOTIONAL)
      .calculation(FixedRateCalculation.builder()
          .dayCount(THIRTY_U_360)
          .rate(ValueSchedule.of(RATE))
          .build())
      .build();
  private static final SwapLeg IBOR_LEG = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(PeriodicSchedule.builder()
          .startDate(START)
          .endDate(END)
          .frequency(P3M)
          .businessDayAdjustment(BDA_MF)
          .stubConvention(StubConvention.SHORT_FINAL)
          .build())
      .paymentSchedule(PaymentSchedule.builder()
          .paymentFrequency(P3M)
          .paymentDateOffset(DaysAdjustment.NONE)
          .build())
      .notionalSchedule(UNIT_NOTIONAL)
      .calculation(IborRateCalculation.builder()
          .index(USD_LIBOR_3M)
          .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, CALENDAR, BDA_P))
          .build())
      .build();
  private static final Swap SWAP = Swap.of(FIXED_LEG, IBOR_LEG);
  // deliverable swap future
  private static final LocalDate LAST_TRADE = LocalDate.of(2013, 6, 17);
  private static final LocalDate DELIVERY = LocalDate.of(2013, 6, 19);
  private static final double NOTIONAL = 100000;
  private static final DeliverableSwapFuture FUTURE = DeliverableSwapFuture.builder()
      .deliveryDate(DELIVERY)
      .lastTradeDate(LAST_TRADE)
      .notional(NOTIONAL)
      .underlyingSwap(SWAP)
      .build();
  private static final StandardId DSF_ID = StandardId.of("OG-Ticker", "DSF1");
  private static final Security<DeliverableSwapFuture> DSF_SECURITY = UnitSecurity
      .builder(FUTURE)
      .standardId(DSF_ID)
      .build();
  private static final SecurityLink<DeliverableSwapFuture> DSF_SECURITY_LINK = SecurityLink.resolved(DSF_SECURITY);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VALUATION).build();
  private static final double TRADE_PRICE = 0.98 + 31.0 / 32.0 / 100.0; // price quoted in 32nd of 1%
  private static final long QUANTITY = 1234L;
  private static final DeliverableSwapFutureTrade FUTURE_TRADE = DeliverableSwapFutureTrade.builder()
      .quantity(QUANTITY)
      .securityLink(DSF_SECURITY_LINK)
      .tradeInfo(TRADE_INFO)
      .tradePrice(TRADE_PRICE)
      .build();
  private static final double LASTMARG_PRICE = 0.99 + 8.0 / 32.0 / 100.0;
  // calculators
  private static final double TOL = 1.0e-13;
  private static final double EPS = 1.0e-6;
  private static final DiscountingDeliverableSwapFutureProductPricer PRODUCT_PRICER =
      DiscountingDeliverableSwapFutureProductPricer.DEFAULT;
  private static final DiscountingDeliverableSwapFutureTradePricer TRADE_PRICER =
      DiscountingDeliverableSwapFutureTradePricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);

  public void test_price() {
    double computed = TRADE_PRICER.price(FUTURE_TRADE, PROVIDER);
    double expected = PRODUCT_PRICER.price(FUTURE, PROVIDER);
    assertEquals(computed, expected, TOL);
  }

  public void test_presentValue() {
    CurrencyAmount computed = TRADE_PRICER.presentValue(FUTURE_TRADE, PROVIDER, LASTMARG_PRICE);
    double expected = QUANTITY * NOTIONAL * (PRODUCT_PRICER.price(FUTURE, PROVIDER) - LASTMARG_PRICE);
    assertEquals(computed.getCurrency(), USD);
    assertEquals(computed.getAmount(), expected, QUANTITY * NOTIONAL * TOL);
  }

  public void test_presentValueSensitivity() {
    PointSensitivities point = TRADE_PRICER.presentValueSensitivity(FUTURE_TRADE, PROVIDER);
    CurveCurrencyParameterSensitivities computed = PROVIDER.curveParameterSensitivity(point);
    CurveCurrencyParameterSensitivities expected = FD_CAL.sensitivity(
        PROVIDER, (p) -> TRADE_PRICER.presentValue(FUTURE_TRADE, (p), LASTMARG_PRICE));
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * QUANTITY * EPS * 10d));
  }

  public void test_currencyExposure() {
    CurrencyAmount pv = TRADE_PRICER.presentValue(FUTURE_TRADE, PROVIDER, LASTMARG_PRICE);
    PointSensitivities point = TRADE_PRICER.presentValueSensitivity(FUTURE_TRADE, PROVIDER);
    MultiCurrencyAmount expected = PROVIDER.currencyExposure(point).plus(pv);
    MultiCurrencyAmount computed = TRADE_PRICER.currencyExposure(FUTURE_TRADE, PROVIDER, LASTMARG_PRICE);
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  // regression to 2.x
  public void regression() {
    CurrencyAmount pv = TRADE_PRICER.presentValue(FUTURE_TRADE, PROVIDER, TRADE_PRICE);
    assertEquals(pv.getAmount(), 4022633.290539182, NOTIONAL * QUANTITY * TOL);
    DoubleArray dscExp = DoubleArray.of(
        347963.1427498563, 240275.26230191416, 123908.37739051704,
        -1302968.1341957184, -8402797.591029292, -9024590.733895564);
    DoubleArray fwdExp =DoubleArray.of(
        1.5288758221797276E7, 1.2510651813905597E7, -1535786.53682933,
        -9496881.09854053, -3.583343769759877E7, -1.1342379328462188E9);
    PointSensitivities point = TRADE_PRICER.presentValueSensitivity(FUTURE_TRADE, PROVIDER);
    CurveCurrencyParameterSensitivities sensi = PROVIDER.curveParameterSensitivity(point);
    double tolerance = NOTIONAL * QUANTITY * EPS;
    assertTrue(sensi.getSensitivity(USD_DSC_NAME, USD).getSensitivity().equalWithTolerance(dscExp, tolerance));
    assertTrue(sensi.getSensitivity(USD_FWD3_NAME, USD).getSensitivity().equalWithTolerance(fwdExp, tolerance));
  }

}
