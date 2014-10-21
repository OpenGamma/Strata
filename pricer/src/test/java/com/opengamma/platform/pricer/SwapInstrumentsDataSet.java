/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.analytics.financial.instrument.NotionalProvider;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.BusinessDayConvention;
import com.opengamma.basics.date.BusinessDayConventions;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.DayCounts;
import com.opengamma.basics.date.HolidayCalendar;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.basics.index.RateIndices;
import com.opengamma.basics.schedule.StubConvention;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.platform.finance.trade.swap.CompoundingMethod;

/**
 * Data set of swaps used in the end-to-end tests.
 */
public class SwapInstrumentsDataSet {

  private SwapInstrumentsDataSet() { /* private constructor */ }

  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 1, 22);
  private static final HolidayCalendar NYC = CalendarUSD.NYC;
  private static final IborIndex USDLIBOR1M = RateIndices.USD_LIBOR_1M;
  private static final IborIndex USDLIBOR3M = RateIndices.USD_LIBOR_3M;
  private static final IborIndex USDLIBOR6M = RateIndices.USD_LIBOR_6M;
  private static final Currency USD = USDLIBOR3M.getCurrency();

  /** Fixing data */
  private static final LocalDateDoubleTimeSeries TS_USDLIBOR1M =
      LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2013, 12, 10), 0.00123)
        .put(LocalDate.of(2013, 12, 12), 0.00123)
        .build();
  public static final LocalDateDoubleTimeSeries TS_USDLIBOR3M =
      LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2013, 12, 10), 0.0024185)
        .put(LocalDate.of(2013, 12, 12), 0.0024285)
        .build();
  public static final LocalDateDoubleTimeSeries TS_USDLIBOR6M = 
      LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2013, 12, 10), 0.0030)
        .put(LocalDate.of(2013, 12, 12), 0.0035)
        .build();
  private static final LocalDateDoubleTimeSeries TS_USDON =
      LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2014, 1, 17), 0.0007)
        .put(LocalDate.of(2014, 1, 21), 0.0007)
        .put(LocalDate.of(2014, 1, 22), 0.0007)
        .build();
  public static final LocalDateDoubleTimeSeries[] TS_ARRAY_USDLIBOR3M = 
      new LocalDateDoubleTimeSeries[] {TS_USDLIBOR3M };
  private static final LocalDateDoubleTimeSeries[] TS_ARRAY_USDLIBOR3M_2X = 
      new LocalDateDoubleTimeSeries[] {TS_USDLIBOR3M, TS_USDLIBOR3M };
  private static final LocalDateDoubleTimeSeries[] TS_ARRAY_USDLIBOR3M_USDLIBOR6M = 
      new LocalDateDoubleTimeSeries[] {TS_USDLIBOR3M, TS_USDLIBOR6M };
  private static final LocalDateDoubleTimeSeries[] TS_ARRAY_USDLIBOR1M_USDLIBOR3M = 
      new LocalDateDoubleTimeSeries[] {TS_USDLIBOR1M, TS_USDLIBOR3M };
  private static final LocalDateDoubleTimeSeries[] TS_ARRAY_USDON =
      new LocalDateDoubleTimeSeries[] {TS_USDON };

  /** Standard market conventions */
  private static final CompoundingMethod CMP_FLAT = CompoundingMethod.FLAT;
  private static final int OFFSET_SPOT = 2;
  private static final int OFFSET_PAYMENT = 0;
  private static final Period P3M = Period.ofMonths(3);
  private static final Period P6M = Period.ofMonths(6);
  private static final DayCount DC_30U_360 = DayCounts.THIRTY_U_360;
  private static final BusinessDayConvention BDC_MODFOL = BusinessDayConventions.MODIFIED_FOLLOWING;
  private static final BusinessDayConvention BDC_FOL = BusinessDayConventions.FOLLOWING;
  private static final StubConvention STUB_SHORT_START = StubConvention.SHORT_INITIAL;
  private static final BusinessDayAdjustment ADJUSTED_DATE_USDLIBOR = BusinessDayAdjustment.of(BDC_MODFOL, NYC);
//  private static final OffsetAdjustedDateParameters OFFSET_FIXING_USDLIBOR = 
//      new OffsetAdjustedDateParameters(-OFFSET_SPOT, OffsetType.BUSINESS, NYC, BDC_FOL);
//  private static final GeneratorLegIbor LEG_USDLIBOR3M =  GeneratorLegIborMaster.getInstance().getGenerator("USDLIBOR3M", NYC);
//  private static final GeneratorLegIbor LEG_USDLIBOR6M =  GeneratorLegIborMaster.getInstance().getGenerator("USDLIBOR6M", NYC);
//  private static final GeneratorLegONArithmeticAverage LEG_USDFEDFUNDAA3M = 
//      GeneratorLegOnAaMaster.getInstance().getGenerator("USDFEDFUNDAA3M", NYC);
//  private static final GeneratorLegONCompounded LEG_USDFEDFUNDCMP1Y =
//      new GeneratorLegONCompounded("LEG_USDFEDFUNDCMP1Y", USD, LEG_USDFEDFUNDAA3M.getIndexON(), Period.ofMonths(12), 
//          2, 2, LEG_USDFEDFUNDAA3M.getBusinessDayConvention(), true, STUB_SHORT_START, false, NYC, NYC);
//  private static final GeneratorLegIborCompounding LEG_USDLIBOR1MCMP3M = 
//      new GeneratorLegIborCompounding("LEG_USDLIBOR1MCMP3M", USD, USDLIBOR1M, 
//      P3M, CMP_FLAT, OFFSET_SPOT, OFFSET_PAYMENT, BusinessDayConventions.MODIFIED_FOLLOWING, true, 
//      STUB_SHORT_START, false, NYC, NYC);
//  private static final GeneratorLegFixed LEG_USDFixed1Y = new GeneratorLegFixed("LEG_USDFixed1Y", USD, OFFSET_SPOT, 
//      P6M, DC_30U_360, BDC_MODFOL, OFFSET_PAYMENT, true, STUB_SHORT_START, false, NYC);
//  private static final GeneratorSwapSingleCurrency USDFFAA3MLIBOR3M = new GeneratorSwapSingleCurrency("USDFEDFUNDAA3MLIBOR3M",
//      LEG_USDFEDFUNDAA3M, LEG_USDLIBOR3M);
//  private static final GeneratorSwapFixedIborMaster GENERATOR_SWAP_FIXED_IBOR_MASTER = GeneratorSwapFixedIborMaster.getInstance();
//  private static final GeneratorSwapFixedONMaster GENERATOR_SWAP_FIXED_ONCMP_MASTER = GeneratorSwapFixedONMaster.getInstance();
//  private static final GeneratorSwapFixedIbor USD6MLIBOR1M = GENERATOR_SWAP_FIXED_IBOR_MASTER.getGenerator("USD6MLIBOR1M", NYC);
//  private static final GeneratorSwapFixedIbor USD6MLIBOR3M = GENERATOR_SWAP_FIXED_IBOR_MASTER.getGenerator("USD6MLIBOR3M", NYC);
//  private static final GeneratorSwapFixedON USD1YFEDFUND = GENERATOR_SWAP_FIXED_ONCMP_MASTER.getGenerator("USD1YFEDFUND", NYC);

  /** Instruments descriptions */
  private static final double NOTIONAL = 100_000_000;
  static NotionalProvider NOTIONAL_PROVIDER = new NotionalProvider() {
    @Override
    public double getAmount(org.threeten.bp.LocalDate date) {
      return NOTIONAL;
    }
  };
  
//  // Instrument description: Swap Fixed vs ON Cmp (X)
//  private static final LocalDate TRADE_DATE_ON = LocalDate.of(2014, 2, 3);
//  private static final Period TENOR_SWAP_ON = Period.ofMonths(2);
//  private static final double FIXED_RATE_ON = 0.00123;
//  private static final GeneratorAttributeIR ATTRIBUTE_ON = new GeneratorAttributeIR(TENOR_SWAP_ON);
//  private static final SwapDefinition SWAP_FIXED_ON_DEFINITION = 
//      USD1YFEDFUND.generateInstrument(TRADE_DATE_ON, FIXED_RATE_ON, NOTIONAL, ATTRIBUTE_ON);
//  public static final Swap<? extends Payment, ? extends Payment> SWAP_FIXED_ON =
//      SWAP_FIXED_ON_DEFINITION.toDerivative(VALUATION_DATE);
//
//
//  // Instrument description: Swap Fixed vs ON Cmp Already started (with fixing) (X)
//  private static final LocalDate TRADE_DATE_ON_S = LocalDate.of(2014, 1, 15);
//  private static final Period TENOR_SWAP_ON_S = Period.ofMonths(2);
//  private static final double FIXED_RATE_ON_S = 0.00123;
//  private static final GeneratorAttributeIR ATTRIBUTE_ON_S = new GeneratorAttributeIR(TENOR_SWAP_ON_S);
//  private static final SwapDefinition SWAP_FIXED_ON_DEFINITION_S =
//      USD1YFEDFUND.generateInstrument(TRADE_DATE_ON_S, FIXED_RATE_ON_S, NOTIONAL, ATTRIBUTE_ON_S);
//  public static final Swap<? extends Payment, ? extends Payment> SWAP_FIXED_ON_S =
//      SWAP_FIXED_ON_DEFINITION_S.toDerivative(VALUATION_DATE, TS_ARRAY_USDON);
//
//  // Instrument description: Swap ON AA + Spread vs Libor 3M (X)
//  private static final LocalDate TRADE_DATE_FF = LocalDate.of(2014, 9, 10);
//  private static final Period TENOR_SWAP_FF = Period.ofYears(6);
//  private static final double SPREAD_FF = 0.0025;
//  private static final GeneratorAttributeIR ATTRIBUTE_FF = new GeneratorAttributeIR(TENOR_SWAP_FF);
//  private static final SwapDefinition SWAP_FF_3M_0_DEFINITION = 
//      USDFFAA3MLIBOR3M.generateInstrument(TRADE_DATE_FF, 0.0, NOTIONAL, ATTRIBUTE_FF);
//  public static final Swap<? extends Payment, ? extends Payment> SWAP_FF_3M_0 = SWAP_FF_3M_0_DEFINITION.toDerivative(VALUATION_DATE);
//  private static final SwapDefinition SWAP_FF_3M_DEFINITION = 
//      USDFFAA3MLIBOR3M.generateInstrument(TRADE_DATE_FF, SPREAD_FF, NOTIONAL, ATTRIBUTE_FF);
//  public static final Swap<? extends Payment, ? extends Payment> SWAP_FF_3M = SWAP_FF_3M_DEFINITION.toDerivative(VALUATION_DATE);

//  // Instrument description: Swap Fixed vs Libor3M (X)
//  private static final LocalDate TRADE_DATE_3M = LocalDate.of(2014, 9, 10);
//  private static final Period TENOR_SWAP_3M = Period.ofYears(7);
//  private static final double FIXED_RATE_3M = 0.0150;
//  private static final GeneratorAttributeIR ATTRIBUTE_3M = new GeneratorAttributeIR(TENOR_SWAP_3M);
//  private static final SwapDefinition SWAP_FIXED_3M_DEFINITION = 
//      USD6MLIBOR3M.generateInstrument(TRADE_DATE_3M, FIXED_RATE_3M, NOTIONAL, ATTRIBUTE_3M);
//  public static final Swap<? extends Payment, ? extends Payment> SWAP_FIXED_3M =
//      SWAP_FIXED_3M_DEFINITION.toDerivative(VALUATION_DATE);
//
  
  
  
//  // Instrument description: Swap Fixed vs Libor1M
//  private static final LocalDate TRADE_DATE_1M = LocalDate.of(2014, 9, 10);
//  private static final Period TENOR_SWAP_1M = Period.ofYears(2);
//  private static final double FIXED_RATE_1M = 0.0125;
//  private static final GeneratorAttributeIR ATTRIBUTE_1M = new GeneratorAttributeIR(TENOR_SWAP_1M);
//  private static final SwapDefinition SWAP_FIXED_1M_DEFINITION = 
//      USD6MLIBOR1M.generateInstrument(TRADE_DATE_1M, FIXED_RATE_1M, NOTIONAL, ATTRIBUTE_1M);
//  public static final Swap<? extends Payment, ? extends Payment> SWAP_FIXED_1M = SWAP_FIXED_1M_DEFINITION.toDerivative(VALUATION_DATE);
//
//  // Instrument description: Swap Fixed vs Libor3M Already started (with fixing) (X)
//  private static final LocalDate TRADE_DATE_3M_S = LocalDate.of(2013, 9, 10);
//  private static final Period TENOR_SWAP_3M_S = Period.ofYears(7);
//  private static final double FIXED_RATE_3M_S = 0.0150;
//  private static final GeneratorAttributeIR ATTRIBUTE_3M_S = new GeneratorAttributeIR(TENOR_SWAP_3M_S);
//  private static final SwapFixedIborDefinition SWAP_FIXED_3M_S_DEFINITION = 
//      USD6MLIBOR3M.generateInstrument(TRADE_DATE_3M_S, FIXED_RATE_3M_S, NOTIONAL, ATTRIBUTE_3M_S);
//  public static final Swap<? extends Payment, ? extends Payment> SWAP_FIXED_3M_S = 
//      SWAP_FIXED_3M_S_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M);
//
//  // Instrument description: Swap Libor3M+S vs Libor6M (X)
//  private static final LocalDate TRADE_DATE_3M_6M_BS = LocalDate.of(2014, 8, 27);
//  private static final Period TENOR_SWAP_3M_6M_BS = Period.ofYears(10);
//  private static final double SPREAD_3M_6M_BS = 0.0010;
//  private static final GeneratorAttributeIR ATTRIBUTE_3M_6M_BS = new GeneratorAttributeIR(TENOR_SWAP_3M_6M_BS);
//  private static final AnnuityDefinition<?> LEG_3M_S = 
//      LEG_USDLIBOR3M.generateInstrument(TRADE_DATE_3M_6M_BS, SPREAD_3M_6M_BS, NOTIONAL, ATTRIBUTE_3M_6M_BS); // Receiver
//  private static final AnnuityDefinition<?> LEG_6M = 
//      LEG_USDLIBOR6M.generateInstrument(TRADE_DATE_3M_6M_BS, 0.0, -NOTIONAL, ATTRIBUTE_3M_6M_BS); // Payer
//  private static final SwapDefinition BS_3M_S_6M_DEFINITION = new SwapDefinition(LEG_3M_S, LEG_6M);
//  public static final Swap<? extends Payment, ? extends Payment> BS_3M_S_6M = 
//      BS_3M_S_6M_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M_USDLIBOR6M);
//
//  // Instrument description: Swap ON Comp+spread v ON AA - Such swap would not be traded, used only for testing
//  private static final LocalDate TRADE_DATE_ONCMP_AA = LocalDate.of(2014, 8, 27);
//  private static final Period TENOR_SWAP_ONCMP_AA = Period.ofYears(5);
//  private static final double SPREAD_ONCMP_AA = 0.0010;
//  private static final GeneratorAttributeIR ATTRIBUTE_ONCMP_AA = new GeneratorAttributeIR(TENOR_SWAP_ONCMP_AA);
//  private static final AnnuityDefinition<?> LEG_ONCMP_S = 
//      LEG_USDFEDFUNDCMP1Y.generateInstrument(TRADE_DATE_ONCMP_AA, SPREAD_ONCMP_AA, -NOTIONAL, ATTRIBUTE_ONCMP_AA); // Payer
//  private static final AnnuityDefinition<?> LEG_ONAA = 
//      LEG_USDFEDFUNDAA3M.generateInstrument(TRADE_DATE_ONCMP_AA, 0.0, NOTIONAL, ATTRIBUTE_ONCMP_AA); // Receiver
//  private static final SwapDefinition BS_ONCMP_S_ONAA_DEFINITION = new SwapDefinition(LEG_ONCMP_S, LEG_ONAA);
//  public static final Swap<? extends Payment, ? extends Payment> BS_ONCMP_S_ONAA = 
//      BS_ONCMP_S_ONAA_DEFINITION.toDerivative(VALUATION_DATE);
//  
//  // Instrument description: Swap LIBOR1M Compounding 3M + Spread v LIBOR3M
//  private static final LocalDate TRADE_DATE_1MCMP_S_3M_BS = LocalDate.of(2014, 8, 27);
//  private static final Period TENOR_SWAP_1MCMP_S_3M_BS = Period.ofYears(5);
//  private static final double SPREAD_1MCMP_S_3M_BS = 0.0010;
//  private static final GeneratorAttributeIR ATTRIBUTE_1MCMP_S_3M_BS = new GeneratorAttributeIR(TENOR_SWAP_1MCMP_S_3M_BS);
//  private static final AnnuityDefinition<?> LEG_1MCMP_S = 
//      LEG_USDLIBOR1MCMP3M.generateInstrument(TRADE_DATE_1MCMP_S_3M_BS, SPREAD_1MCMP_S_3M_BS, NOTIONAL, ATTRIBUTE_1MCMP_S_3M_BS); // Receiver
//  private static final AnnuityDefinition<?> LEG_3MCMP = 
//      LEG_USDLIBOR3M.generateInstrument(TRADE_DATE_1MCMP_S_3M_BS, 0.0, -NOTIONAL, ATTRIBUTE_1MCMP_S_3M_BS); // Payer
//  private static final SwapDefinition BS_1MCMP_S_3M_DEFINITION = new SwapDefinition(LEG_1MCMP_S, LEG_3MCMP);
//  public static final Swap<? extends Payment, ? extends Payment> BS_1MCMP_S_3M = 
//      BS_1MCMP_S_3M_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR1M_USDLIBOR3M);
//  
//  // Instrument description: Swap LIBOR1M Compounding 3M v LIBOR3M
//  private static final LocalDate TRADE_DATE_1MCMP_3M_BS = LocalDate.of(2014, 8, 27);
//  private static final Period TENOR_SWAP_1MCMP_3M_BS = Period.ofYears(5);
//  private static final double SPREAD_1MCMP_3M_BS = 0.00;
//  private static final GeneratorAttributeIR ATTRIBUTE_1MCMP_3M_BS = new GeneratorAttributeIR(TENOR_SWAP_1MCMP_3M_BS);
//  private static final AnnuityDefinition<?> LEG_1MCMP = 
//      LEG_USDLIBOR1MCMP3M.generateInstrument(TRADE_DATE_1MCMP_3M_BS, SPREAD_1MCMP_3M_BS, NOTIONAL, ATTRIBUTE_1MCMP_3M_BS); // Receiver
//  private static final AnnuityDefinition<?> LEG_3MCMP_2 = 
//      LEG_USDLIBOR3M.generateInstrument(TRADE_DATE_1MCMP_3M_BS, 0.0, -NOTIONAL, ATTRIBUTE_1MCMP_3M_BS); // Payer
//  private static final SwapDefinition BS_1MCMP_3M_DEFINITION = new SwapDefinition(LEG_1MCMP, LEG_3MCMP_2);
//  public static final Swap<? extends Payment, ? extends Payment> BS_1MCMP_3M = 
//      BS_1MCMP_3M_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR1M_USDLIBOR3M);
//  
//  // Instrument description: Swap Fixed vs Libor3M - Stub 3M
//  private static final LocalDate TRADE_DATE_3M_STUB1 = LocalDate.of(2014, 9, 10);
//  private static final Period TENOR_SWAP_3M_STUB1 = Period.ofMonths(21);
//  private static final double FIXED_RATE_3M_STUB1 = 0.0100;
//  private static final GeneratorAttributeIR ATTRIBUTE_3M_STUB1 = new GeneratorAttributeIR(TENOR_SWAP_3M_STUB1);
//  private static final PaymentDefinition[] PAYMENT_FIXED_STUB1 = LEG_USDFixed1Y.generateInstrument(TRADE_DATE_3M_STUB1, 
//      FIXED_RATE_3M_STUB1, NOTIONAL, ATTRIBUTE_3M_STUB1).getPayments();
//  private static final CouponFixedDefinition[] CPN_FIXED_STUB1_DEFINITION = new CouponFixedDefinition[PAYMENT_FIXED_STUB1.length];
//  static {
//    for (int loopcpn = 0; loopcpn < PAYMENT_FIXED_STUB1.length; loopcpn++) {
//      CPN_FIXED_STUB1_DEFINITION[loopcpn] = (CouponFixedDefinition) PAYMENT_FIXED_STUB1[loopcpn];
//    }
//  }
//  private static final AnnuityCouponFixedDefinition LEG_FIXED_STUB1 = 
//      new AnnuityCouponFixedDefinition(CPN_FIXED_STUB1_DEFINITION, NYC);
//  private static final AnnuityDefinition<? extends CouponDefinition> LEG_IBOR_STUB1 = 
//      (AnnuityDefinition<? extends CouponDefinition>) 
//      LEG_USDLIBOR3M.generateInstrument(TRADE_DATE_3M_STUB1, 0.0, -NOTIONAL, ATTRIBUTE_3M_STUB1);
//  private static final SwapCouponFixedCouponDefinition IRS_STUB1_DEFINITION = 
//      new SwapCouponFixedCouponDefinition(LEG_FIXED_STUB1, LEG_IBOR_STUB1);
//  public static final Swap<? extends Payment, ? extends Payment> IRS_STUB1 = 
//      IRS_STUB1_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M_2X);
//  
//  // Instrument description: IRS Fixed vs Libor3M - Stub 1M: Accrual period is 1M / fixing rate is based on 3M
//  private static final LocalDate TRADE_DATE_3M_STUB2 = LocalDate.of(2014, 9, 10);
//  private static final Period TENOR_SWAP_3M_STUB2 = Period.ofMonths(22);
//  private static final double FIXED_RATE_3M_STUB2 = 0.0100;
//  private static final GeneratorAttributeIR ATTRIBUTE_3M_STUB2 = new GeneratorAttributeIR(TENOR_SWAP_3M_STUB2);
//  private static final PaymentDefinition[] PAYMENT_FIXED_STUB2 = LEG_USDFixed1Y.generateInstrument(TRADE_DATE_3M_STUB2, 
//      FIXED_RATE_3M_STUB2, NOTIONAL, ATTRIBUTE_3M_STUB2).getPayments();
//  private static final CouponFixedDefinition[] CPN_FIXED_STUB2_DEFINITION = new CouponFixedDefinition[PAYMENT_FIXED_STUB2.length];
//  static {
//    for (int loopcpn = 0; loopcpn < PAYMENT_FIXED_STUB2.length; loopcpn++) {
//      CPN_FIXED_STUB2_DEFINITION[loopcpn] = (CouponFixedDefinition) PAYMENT_FIXED_STUB2[loopcpn];
//    }
//  }
//  private static final AnnuityCouponFixedDefinition LEG_FIXED_STUB2 = 
//      new AnnuityCouponFixedDefinition(CPN_FIXED_STUB2_DEFINITION, NYC);
//  private static final AnnuityDefinition<? extends CouponDefinition> LEG_IBOR_STUB2 = 
//      (AnnuityDefinition<? extends CouponDefinition>) 
//      LEG_USDLIBOR3M.generateInstrument(TRADE_DATE_3M_STUB2, 0.0, -NOTIONAL, ATTRIBUTE_3M_STUB2);
//  private static final SwapCouponFixedCouponDefinition IRS_STUB2_DEFINITION = 
//      new SwapCouponFixedCouponDefinition(LEG_FIXED_STUB2, LEG_IBOR_STUB2);
//  public static final Swap<? extends Payment, ? extends Payment> IRS_STUB2 = 
//      IRS_STUB2_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M_2X);
//  
//  // Instrument description: IRS Fixed vs Libor6M - Stub 3M: Accrual period is 3M / fixing rate is based on 3M
//  private static final LocalDate TRADE_DATE_3M_STUB3 = LocalDate.of(2014, 9, 10);
//  private static final LocalDate SPOT_DATE_STUB3 = 
//      ScheduleCalculator.getAdjustedDate(TRADE_DATE_3M_STUB3, OFFSET_SPOT, NYC);
//  private static final Period TENOR_SWAP_3M_STUB3 = Period.ofMonths(21);
//  private static final LocalDate END_DATE_STUB3 = SPOT_DATE_STUB3.plus(TENOR_SWAP_3M_STUB3);
//  private static final double FIXED_RATE_3M_STUB3 = 0.0100;
//  private static final GeneratorAttributeIR ATTRIBUTE_3M_STUB3 = new GeneratorAttributeIR(TENOR_SWAP_3M_STUB3);
//  private static final PaymentDefinition[] PAYMENT_FIXED_STUB3 = LEG_USDFixed1Y.generateInstrument(TRADE_DATE_3M_STUB3, 
//      FIXED_RATE_3M_STUB3, NOTIONAL, ATTRIBUTE_3M_STUB3).getPayments();
//  private static final CouponFixedDefinition[] CPN_FIXED_STUB3_DEFINITION = new CouponFixedDefinition[PAYMENT_FIXED_STUB3.length];
//  static {
//    for (int loopcpn = 0; loopcpn < PAYMENT_FIXED_STUB3.length; loopcpn++) {
//      CPN_FIXED_STUB3_DEFINITION[loopcpn] = (CouponFixedDefinition) PAYMENT_FIXED_STUB3[loopcpn];
//    }
//  }
//  private static final AnnuityCouponFixedDefinition LEG_FIXED_STUB3 = 
//      new AnnuityCouponFixedDefinition(CPN_FIXED_STUB3_DEFINITION, NYC);
//  private static final CouponStub CPN_STUB3 = new CouponStub(STUB_SHORT_START, USDLIBOR3M, USDLIBOR6M);
//  private static final AnnuityDefinition<? extends CouponDefinition> LEG_IBOR_STUB3 =
//      (AnnuityDefinition<? extends CouponDefinition>) 
//      new FloatingAnnuityDefinitionBuilder().payer(true).notional(NOTIONAL_PROVIDER).
//      startDate(SPOT_DATE_STUB3.toLocalDate()).endDate(END_DATE_STUB3.toLocalDate()).index(USDLIBOR6M).
//      accrualPeriodFrequency(P6M).rollDateAdjuster(RollConvention.NONE.getRollDateAdjuster(0)).
//      resetDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR).accrualPeriodParameters(ADJUSTED_DATE_USDLIBOR).
//      dayCount(USDLIBOR6M.getDayCount()).fixingDateAdjustmentParameters(OFFSET_FIXING_USDLIBOR).
//      currency(USD).spread(0.0).startStub(CPN_STUB3).build();
//  private static final SwapCouponFixedCouponDefinition IRS_STUB3_DEFINITION = 
//      new SwapCouponFixedCouponDefinition(LEG_FIXED_STUB3, LEG_IBOR_STUB3);
//  public static final Swap<? extends Payment, ? extends Payment> IRS_STUB3 = 
//      IRS_STUB3_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M_2X);
//  
//  // Instrument description: IRS Fixed vs Libor6M - Stub 4M: Accrual period is 4M / fixing rate average 3M and 6M
//  private static final LocalDate TRADE_DATE_3M_STUB4 = LocalDate.of(2014, 9, 10);
//  private static final LocalDate SPOT_DATE_STUB4 = 
//      ScheduleCalculator.getAdjustedDate(TRADE_DATE_3M_STUB4, OFFSET_SPOT, NYC);
//  private static final Period TENOR_SWAP_3M_STUB4 = Period.ofMonths(22);
//  private static final LocalDate END_DATE_STUB4 = SPOT_DATE_STUB4.plus(TENOR_SWAP_3M_STUB4);
//  private static final double FIXED_RATE_3M_STUB4 = 0.0100;
//  private static final GeneratorAttributeIR ATTRIBUTE_3M_STUB4 = new GeneratorAttributeIR(TENOR_SWAP_3M_STUB4);
//  private static final PaymentDefinition[] PAYMENT_FIXED_STUB4 = LEG_USDFixed1Y.generateInstrument(TRADE_DATE_3M_STUB4, 
//      FIXED_RATE_3M_STUB4, NOTIONAL, ATTRIBUTE_3M_STUB4).getPayments();
//  private static final CouponFixedDefinition[] CPN_FIXED_STUB4_DEFINITION = new CouponFixedDefinition[PAYMENT_FIXED_STUB4.length];
//  static {
//    for (int loopcpn = 0; loopcpn < PAYMENT_FIXED_STUB4.length; loopcpn++) {
//      CPN_FIXED_STUB4_DEFINITION[loopcpn] = (CouponFixedDefinition) PAYMENT_FIXED_STUB4[loopcpn];
//    }
//  }
//  private static final AnnuityCouponFixedDefinition LEG_FIXED_STUB4 = 
//      new AnnuityCouponFixedDefinition(CPN_FIXED_STUB4_DEFINITION, NYC);
//  private static final CouponStub CPN_STUB4 = new CouponStub(STUB_SHORT_START, USDLIBOR3M, USDLIBOR6M);
//  private static final AnnuityDefinition<? extends CouponDefinition> LEG_IBOR_STUB4 =
//      (AnnuityDefinition<? extends CouponDefinition>) 
//      new FloatingAnnuityDefinitionBuilder().payer(true).notional(NOTIONAL_PROVIDER).
//      startDate(SPOT_DATE_STUB4.toLocalDate()).endDate(END_DATE_STUB4.toLocalDate()).index(USDLIBOR6M).
//      accrualPeriodFrequency(P6M).rollDateAdjuster(RollConvention.NONE.getRollDateAdjuster(0)).
//      resetDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR).accrualPeriodParameters(ADJUSTED_DATE_USDLIBOR).
//      dayCount(USDLIBOR6M.getDayCount()).fixingDateAdjustmentParameters(OFFSET_FIXING_USDLIBOR).
//      currency(USD).spread(0.0).startStub(CPN_STUB4).build();
//  private static final SwapCouponFixedCouponDefinition IRS_STUB4_DEFINITION = 
//      new SwapCouponFixedCouponDefinition(LEG_FIXED_STUB4, LEG_IBOR_STUB4);
//  public static final Swap<? extends Payment, ? extends Payment> IRS_STUB4 = 
//      IRS_STUB4_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M_2X);
//  
//  // Instrument description: IRS Fixed vs Libor3M - Stub Long Start 6M: Accrual period is 5M30D / fixing rate 6M
//  private static final LocalDate SPOT_DATE_STUB5 = LocalDate.of(2014, 3, 12);
//  private static final LocalDate END_DATE_STUB5 = LocalDate.of(2021, 9, 11);
//  private static final double FIXED_RATE_3M_STUB5 = 0.0150;  
//  private static final CouponStub CPN_FIXED_STUB5 = new CouponStub(StubType.LONG_START);
//  private static final PaymentDefinition[] PAYMENT_FIXED_STUB5 = new FixedAnnuityDefinitionBuilder().
//      payer(false).currency(USD6MLIBOR3M.getCurrency()).notional(NOTIONAL_PROVIDER).startDate(SPOT_DATE_STUB5.toLocalDate()).
//      endDate(END_DATE_STUB5.toLocalDate()).dayCount(USD6MLIBOR3M.getFixedLegDayCount()).
//      accrualPeriodFrequency(USD6MLIBOR3M.getFixedLegPeriod()).rate(FIXED_RATE_3M_STUB5).
//      accrualPeriodParameters(ADJUSTED_DATE_USDLIBOR).startStub(CPN_FIXED_STUB5).
//      build().getPayments();
//  private static final CouponFixedDefinition[] CPN_FIXED_STUB5_DEFINITION = new CouponFixedDefinition[PAYMENT_FIXED_STUB5.length];
//  static {
//    for (int loopcpn = 0; loopcpn < PAYMENT_FIXED_STUB5.length; loopcpn++) {
//      CPN_FIXED_STUB5_DEFINITION[loopcpn] = (CouponFixedDefinition) PAYMENT_FIXED_STUB5[loopcpn];
//    }
//  }
//  private static final CouponStub CPN_IBOR_STUB5 = new CouponStub(StubType.LONG_START, USDLIBOR6M, USDLIBOR6M);
//  private static final AnnuityCouponFixedDefinition LEG_FIXED_STUB5 = 
//      new AnnuityCouponFixedDefinition(CPN_FIXED_STUB5_DEFINITION, NYC);
//  private static final AnnuityDefinition<? extends CouponDefinition> LEG_IBOR_STUB5 =
//      (AnnuityDefinition<? extends CouponDefinition>) 
//      new FloatingAnnuityDefinitionBuilder().payer(true).notional(NOTIONAL_PROVIDER).
//      startDate(SPOT_DATE_STUB5.toLocalDate()).endDate(END_DATE_STUB5.toLocalDate()).index(USDLIBOR3M).
//      accrualPeriodFrequency(P3M).rollDateAdjuster(RollConvention.NONE.getRollDateAdjuster(0)).
//      resetDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR).accrualPeriodParameters(ADJUSTED_DATE_USDLIBOR).
//      dayCount(USDLIBOR3M.getDayCount()).fixingDateAdjustmentParameters(OFFSET_FIXING_USDLIBOR).
//      currency(USD).spread(0.0).startStub(CPN_IBOR_STUB5).build();
//  private static final SwapCouponFixedCouponDefinition IRS_STUB5_DEFINITION = 
//      new SwapCouponFixedCouponDefinition(LEG_FIXED_STUB5, LEG_IBOR_STUB5);
//  public static final Swap<? extends Payment, ? extends Payment> IRS_STUB5 = 
//      IRS_STUB5_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M_2X);
//  
//  // Instrument description: IRS Fixed vs Libor3M - Short end Stub 2M: Accrual period is 2M / fixing rate average 1M and 3M
//private static final LocalDate SPOT_DATE_STUB6 = LocalDate.of(2014, 3, 12);
//private static final LocalDate END_DATE_STUB6 = LocalDate.of(2021, 11, 12);
//private static final double FIXED_RATE_3M_STUB6 = 0.0150;  
//private static final CouponStub CPN_FIXED_STUB6 = new CouponStub(StubType.SHORT_END);
//private static final PaymentDefinition[] PAYMENT_FIXED_STUB6 = new FixedAnnuityDefinitionBuilder().
//    payer(false).currency(USD6MLIBOR3M.getCurrency()).notional(NOTIONAL_PROVIDER).startDate(SPOT_DATE_STUB6.toLocalDate()).
//    endDate(END_DATE_STUB6.toLocalDate()).dayCount(USD6MLIBOR3M.getFixedLegDayCount()).
//    accrualPeriodFrequency(USD6MLIBOR3M.getFixedLegPeriod()).rate(FIXED_RATE_3M_STUB6).
//    accrualPeriodParameters(ADJUSTED_DATE_USDLIBOR).startStub(CPN_FIXED_STUB6).
//    build().getPayments();
//private static final CouponFixedDefinition[] CPN_FIXED_STUB6_DEFINITION = new CouponFixedDefinition[PAYMENT_FIXED_STUB6.length];
//static {
//  for (int loopcpn = 0; loopcpn < PAYMENT_FIXED_STUB6.length; loopcpn++) {
//    CPN_FIXED_STUB6_DEFINITION[loopcpn] = (CouponFixedDefinition) PAYMENT_FIXED_STUB6[loopcpn];
//  }
//}
//private static final CouponStub CPN_IBOR_STUB6 = new CouponStub(StubType.SHORT_START, USDLIBOR1M, USDLIBOR3M);
//private static final AnnuityCouponFixedDefinition LEG_FIXED_STUB6 = 
//    new AnnuityCouponFixedDefinition(CPN_FIXED_STUB6_DEFINITION, NYC);
//private static final AnnuityDefinition<? extends CouponDefinition> LEG_IBOR_STUB6 =
//    (AnnuityDefinition<? extends CouponDefinition>) 
//    new FloatingAnnuityDefinitionBuilder().payer(true).notional(NOTIONAL_PROVIDER).
//    startDate(SPOT_DATE_STUB6.toLocalDate()).endDate(END_DATE_STUB6.toLocalDate()).index(USDLIBOR6M).
//    accrualPeriodFrequency(P6M).rollDateAdjuster(RollConvention.NONE.getRollDateAdjuster(0)).
//    resetDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR).accrualPeriodParameters(ADJUSTED_DATE_USDLIBOR).
//    dayCount(USDLIBOR6M.getDayCount()).fixingDateAdjustmentParameters(OFFSET_FIXING_USDLIBOR).
//    currency(USD).spread(0.0).startStub(CPN_IBOR_STUB6).build();
//private static final SwapCouponFixedCouponDefinition IRS_STUB6_DEFINITION = 
//    new SwapCouponFixedCouponDefinition(LEG_FIXED_STUB6, LEG_IBOR_STUB6);
//public static final Swap<? extends Payment, ? extends Payment> IRS_STUB6 = 
//    IRS_STUB6_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M_2X);
//
//  // Instrument description: Zero Coupon IRS Fixed vs Libor3M
//  private static final LocalDate SPOT_DATE_ZC = LocalDate.of(2014, 9, 12);
//  private static final LocalDate END_DATE_ZC = LocalDate.of(2021, 9, 12);
//  private static final BusinessDayAdjustment ADJUSTED_DATE_USDLIBOR_ZC =
//      BusinessDayAdjustment.of(BDC_MODFOL, NYC);
//  private static final OffsetAdjustedDateParameters OFFSET_FIXING_USDLIBOR_ZC =
//      new OffsetAdjustedDateParameters(-OFFSET_SPOT, OffsetType.BUSINESS, NYC, BDC_MODFOL);
//  private static final OffsetAdjustedDateParameters OFFSET_PAYMENT_USDLIBOR_ZC =
//      new OffsetAdjustedDateParameters(0, OffsetType.BUSINESS, NYC, BDC_MODFOL);
//  private static final Period ZERO_PERIOD = Period.ZERO;
//  private static final double FIXED_RATE_ZC = 0.0150;
//  private static final RollDateAdjuster ROLL_DATE_ADJUSTER_ZC = RollConvention.NONE.getRollDateAdjuster(0);
//  private static final AnnuityDefinition<?> LEG_FIXED = new FixedAnnuityDefinitionBuilder().
//      payer(true).currency(USD).notional(NOTIONAL_PROVIDER).startDate(SPOT_DATE_ZC).endDate(END_DATE_ZC).
//      endDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR_ZC).startDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR_ZC).
//      dayCount(DC_30U_360).accrualPeriodFrequency(ZERO_PERIOD).accrualPeriodParameters(ADJUSTED_DATE_USDLIBOR_ZC).
//      paymentDateAdjustmentParameters(OFFSET_PAYMENT_USDLIBOR_ZC).rate(FIXED_RATE_ZC).build();
//  private static final AnnuityDefinition<?> LEG_IBOR_3M = new FloatingAnnuityDefinitionBuilder().
//      payer(false).currency(USD).notional(NOTIONAL_PROVIDER).startDate(SPOT_DATE_ZC).endDate(END_DATE_ZC).
//      endDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR_ZC).startDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR_ZC).
//      dayCount(USDLIBOR3M.getDayCount()).accrualPeriodFrequency(ZERO_PERIOD)
//      .accrualPeriodParameters(ADJUSTED_DATE_USDLIBOR_ZC).paymentDateAdjustmentParameters(OFFSET_PAYMENT_USDLIBOR_ZC).
//      index(USDLIBOR3M).resetDateAdjustmentParameters(ADJUSTED_DATE_USDLIBOR_ZC).
//      fixingDateAdjustmentParameters(OFFSET_FIXING_USDLIBOR_ZC).compoundingMethod(CompoundingMethod.STRAIGHT)
//      .rollDateAdjuster(ROLL_DATE_ADJUSTER_ZC).build();
//  private static final SwapDefinition IRS_ZERO_CPN_DEFINITION = new SwapDefinition(LEG_FIXED, LEG_IBOR_3M);
//  public static final Swap<? extends Payment, ? extends Payment> IRS_ZERO_CPN =
//      IRS_ZERO_CPN_DEFINITION.toDerivative(VALUATION_DATE, TS_ARRAY_USDLIBOR3M_2X);
}
