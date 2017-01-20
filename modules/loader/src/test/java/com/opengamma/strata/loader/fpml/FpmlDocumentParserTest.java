/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.INR;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_360_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_E_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.FRPA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_LIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.collect.TestHelper.assertEqualsBean;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.joda.beans.Bean;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.ImmutableFxIndex;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.basics.value.ValueStepSequence;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsIndex;
import com.opengamma.strata.product.credit.CdsIndexTrade;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraDiscountingMethod;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fx.FxNdf;
import com.opengamma.strata.product.fx.FxNdfTrade;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.IborInterpolatedRateComputation;
import com.opengamma.strata.product.rate.IborRateComputation;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.IborRateResetMethod;
import com.opengamma.strata.product.swap.IborRateStubCalculation;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResetSchedule;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Test {@link FpmlDocumentParser}.
 */
@Test
public class FpmlDocumentParserTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_EUTA = GBLO.combinedWith(EUTA);
  private static final HolidayCalendarId GBLO_USNY_JPTO = GBLO.combinedWith(USNY).combinedWith(JPTO);

  //-------------------------------------------------------------------------
  public void bulletPayment() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex28-bullet-payments.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), BulletPaymentTrade.class);
    BulletPaymentTrade bpTrade = (BulletPaymentTrade) trade;
    assertEquals(bpTrade.getInfo().getTradeDate(), Optional.of(date(2001, 4, 29)));
    BulletPayment bp = bpTrade.getProduct();
    assertEquals(bp.getPayReceive(), PAY);
    assertEquals(bp.getDate(), AdjustableDate.of(date(2001, 7, 27), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)));
    assertEquals(bp.getValue(), CurrencyAmount.of(USD, 15000));
  }

  //-------------------------------------------------------------------------
  public void termDeposit() {
    String location = "classpath:com/opengamma/strata/loader/fpml/td-ex01-simple-term-deposit.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), TermDepositTrade.class);
    TermDepositTrade tdTrade = (TermDepositTrade) trade;
    assertEquals(tdTrade.getInfo().getTradeDate(), Optional.of(date(2002, 2, 14)));
    TermDeposit td = tdTrade.getProduct();
    assertEquals(td.getBuySell(), BUY);
    assertEquals(td.getStartDate(), date(2002, 2, 14));
    assertEquals(td.getEndDate(), date(2002, 2, 15));
    assertEquals(td.getCurrency(), CHF);
    assertEquals(td.getNotional(), 25000000d);
    assertEquals(td.getRate(), 0.04);
    assertEquals(td.getDayCount(), ACT_360);
  }

  //-------------------------------------------------------------------------
  public void fxSpot() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex01-fx-spot.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), FxSingleTrade.class);
    FxSingleTrade fxTrade = (FxSingleTrade) trade;
    assertEquals(fxTrade.getInfo().getTradeDate(), Optional.of(date(2001, 10, 23)));
    FxSingle fx = fxTrade.getProduct();
    assertEquals(fx.getBaseCurrencyAmount(), CurrencyAmount.of(GBP, 10000000));
    assertEquals(fx.getCounterCurrencyAmount(), CurrencyAmount.of(USD, -14800000));
    assertEquals(fx.getPaymentDate(), date(2001, 10, 25));
  }

  //-------------------------------------------------------------------------
  public void fxForward() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex03-fx-fwd.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), FxSingleTrade.class);
    FxSingleTrade fxTrade = (FxSingleTrade) trade;
    assertEquals(fxTrade.getInfo().getTradeDate(), Optional.of(date(2001, 11, 19)));
    FxSingle fx = fxTrade.getProduct();
    assertEquals(fx.getBaseCurrencyAmount(), CurrencyAmount.of(EUR, 10000000));
    assertEquals(fx.getCounterCurrencyAmount(), CurrencyAmount.of(USD, -9175000));
    assertEquals(fx.getPaymentDate(), date(2001, 12, 21));
  }

  //-------------------------------------------------------------------------
  public void fxNdf() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex07-non-deliverable-forward.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), FxNdfTrade.class);
    FxNdfTrade fxTrade = (FxNdfTrade) trade;
    assertEquals(fxTrade.getInfo().getTradeDate(), Optional.of(date(2002, 1, 9)));
    FxNdf fx = fxTrade.getProduct();
    assertEquals(fx.getSettlementCurrencyNotional(), CurrencyAmount.of(USD, 10000000));
    assertEquals(fx.getAgreedFxRate(), FxRate.of(USD, INR, 43.4));
    assertEquals(fx.getIndex(), ImmutableFxIndex.builder()
        .name("Reuters/RBIB/14:30")
        .currencyPair(CurrencyPair.of(USD, INR))
        .fixingCalendar(USNY)
        .maturityDateOffset(DaysAdjustment.ofCalendarDays(-2))
        .build());
    assertEquals(fx.getPaymentDate(), date(2002, 4, 11));
  }

  //-------------------------------------------------------------------------
  public void fxSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex08-fx-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), FxSwapTrade.class);
    FxSwapTrade fxTrade = (FxSwapTrade) trade;
    assertEquals(fxTrade.getInfo().getTradeDate(), Optional.of(date(2002, 1, 23)));
    FxSwap fx = fxTrade.getProduct();
    FxSingle nearLeg = fx.getNearLeg();
    assertEquals(nearLeg.getBaseCurrencyAmount(), CurrencyAmount.of(GBP, 10000000));
    assertEquals(nearLeg.getCounterCurrencyAmount(), CurrencyAmount.of(USD, -14800000));
    assertEquals(nearLeg.getPaymentDate(), date(2002, 1, 25));
    FxSingle farLeg = fx.getFarLeg();
    assertEquals(farLeg.getBaseCurrencyAmount(), CurrencyAmount.of(GBP, -10000000));
    assertEquals(farLeg.getCounterCurrencyAmount(), CurrencyAmount.of(USD, 15000000));
    assertEquals(farLeg.getPaymentDate(), date(2002, 2, 25));
  }

  public void swaption() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex10-euro-swaption-relative.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwaptionTrade.class);
    SwaptionTrade swaptionTrade = (SwaptionTrade) trade;
    assertEquals(swaptionTrade.getInfo().getTradeDate(), Optional.of(date(1992, 8, 30)));
    Swaption swaption = swaptionTrade.getProduct();

    //Test the parsing of the underlying swap
    Swap swap = swaption.getUnderlying();
    NotionalSchedule notional = NotionalSchedule.of(EUR, 50000000d);
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1994, 12, 14))
            .endDate(date(1999, 12, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA)))
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(EUR_LIBOR_6M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1994, 12, 14))
            .endDate(date(1999, 12, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA))
            .frequency(Frequency.P12M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P12M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_E_360)
            .rate(ValueSchedule.of(0.06))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);

    //Test the parsing of the option part of the swaption
    Swap underylingSwap = Swap.of(payLeg, recLeg);
    AdjustableDate expiryDate = AdjustableDate.of(
        LocalDate.of(1993, 8, 28),
        BusinessDayAdjustment.of(FOLLOWING, GBLO_EUTA));
    LocalTime expiryTime = LocalTime.of(11, 0, 0);
    ZoneId expiryZone = ZoneId.of("Europe/Brussels");
    Swaption swaptionExpected = Swaption.builder()
        .expiryDate(expiryDate)
        .expiryZone(expiryZone)
        .expiryTime(expiryTime)
        .longShort(LongShort.LONG)
        .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
        .underlying(underylingSwap)
        .build();
    assertEqualsBean((Bean) swaption, swaptionExpected);
  }

  //-------------------------------------------------------------------------
  public void fra() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  private void assertFra(List<Trade> trades, boolean interpolatedParty1) {
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), FraTrade.class);
    FraTrade fraTrade = (FraTrade) trade;
    assertEquals(fraTrade.getInfo().getTradeDate(), Optional.of(date(1991, 5, 14)));
    StandardId party1id = StandardId.of("http://www.hsbc.com/swaps/trade-id", "MB87623");
    StandardId party2id = StandardId.of("http://www.abnamro.com/swaps/trade-id", "AA9876");
    assertEquals(fraTrade.getInfo().getId(), Optional.of(interpolatedParty1 ? party1id : party2id));
    Fra fra = fraTrade.getProduct();
    assertEquals(fra.getBuySell(), interpolatedParty1 ? BUY : SELL);
    assertEquals(fra.getStartDate(), date(1991, 7, 17));
    assertEquals(fra.getEndDate(), date(1992, 1, 17));
    assertEquals(fra.getBusinessDayAdjustment(), Optional.empty());
    assertEquals(fra.getPaymentDate().getUnadjusted(), date(1991, 7, 17));
    assertEquals(fra.getPaymentDate().getAdjustment(), BusinessDayAdjustment.of(FOLLOWING, CHZU));
    assertEquals(fra.getFixingDateOffset().getDays(), -2);
    assertEquals(fra.getFixingDateOffset().getCalendar(), GBLO);
    assertEquals(fra.getFixingDateOffset().getAdjustment(), BusinessDayAdjustment.NONE);
    assertEquals(fra.getDayCount(), ACT_360);
    assertEquals(fra.getCurrency(), CHF);
    assertEquals(fra.getNotional(), 25000000d);
    assertEquals(fra.getFixedRate(), 0.04d);
    assertEquals(fra.getIndex(), interpolatedParty1 ? CHF_LIBOR_3M : CHF_LIBOR_6M);
    assertEquals(fra.getIndexInterpolated(), interpolatedParty1 ? Optional.of(CHF_LIBOR_6M) : Optional.empty());
    assertEquals(fra.getDiscounting(), FraDiscountingMethod.ISDA);
  }

  //-------------------------------------------------------------------------
  public void fra_noParty() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.any()).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), FraTrade.class);
    FraTrade fraTrade = (FraTrade) trade;
    assertEquals(fraTrade.getInfo().getTradeDate(), Optional.of(date(1991, 5, 14)));
    Fra fra = fraTrade.getProduct();
    assertEquals(fra.getBuySell(), BUY);
    assertEquals(fra.getStartDate(), date(1991, 7, 17));
    assertEquals(fra.getEndDate(), date(1992, 1, 17));
    assertEquals(fra.getBusinessDayAdjustment(), Optional.empty());
    assertEquals(fra.getPaymentDate().getUnadjusted(), date(1991, 7, 17));
    assertEquals(fra.getPaymentDate().getAdjustment(), BusinessDayAdjustment.of(FOLLOWING, CHZU));
    assertEquals(fra.getFixingDateOffset().getDays(), -2);
    assertEquals(fra.getFixingDateOffset().getCalendar(), GBLO);
    assertEquals(fra.getFixingDateOffset().getAdjustment(), BusinessDayAdjustment.NONE);
    assertEquals(fra.getDayCount(), ACT_360);
    assertEquals(fra.getCurrency(), CHF);
    assertEquals(fra.getNotional(), 25000000d);
    assertEquals(fra.getFixedRate(), 0.04d);
    assertEquals(fra.getIndex(), CHF_LIBOR_6M);
    assertEquals(fra.getIndexInterpolated(), Optional.empty());
    assertEquals(fra.getDiscounting(), FraDiscountingMethod.ISDA);
    // check same when using a specific selector instead of FpmlPartySelector.auto()
    List<Trade> trades2 = FpmlDocumentParser.of(allParties -> Optional.empty()).parseTrades(resource);
    assertEquals(trades2, trades);
  }

  //-------------------------------------------------------------------------
  public void fra_interpolated() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-interpolated.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertFra(trades, true);
  }

  //-------------------------------------------------------------------------
  public void fra_namespace() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-namespace.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  //-------------------------------------------------------------------------
  public void fra_wrapper1() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-wrapper1.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  //-------------------------------------------------------------------------
  public void fra_wrapper2() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-wrapper2.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  //-------------------------------------------------------------------------
  public void fra_wrapper_clearingStatus() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-wrapper-clearing-status.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  //-------------------------------------------------------------------------
  public void vanillaSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex01-vanilla-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(1994, 12, 12)));
    Swap swap = swapTrade.getProduct();
    NotionalSchedule notional = NotionalSchedule.of(EUR, 50000000d);
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1994, 12, 14))
            .endDate(date(1999, 12, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA)))
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(EUR_LIBOR_6M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1994, 12, 14))
            .endDate(date(1999, 12, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA))
            .frequency(Frequency.P12M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P12M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, FRPA)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_E_360)
            .rate(ValueSchedule.of(0.06))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  //-------------------------------------------------------------------------
  public void stubAmortizedSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex02-stub-amort-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(1994, 12, 12)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.builder()
        .currency(EUR)
        .amount(ValueSchedule.builder()
            .initialValue(50000000d)
            .steps(
                ValueStep.of(date(1995, 12, 14), ValueAdjustment.ofReplace(40000000d)),
                ValueStep.of(date(1996, 12, 14), ValueAdjustment.ofReplace(30000000d)),
                ValueStep.of(date(1997, 12, 14), ValueAdjustment.ofReplace(20000000d)),
                ValueStep.of(date(1998, 12, 14), ValueAdjustment.ofReplace(10000000d)))
            .build())
        .build();
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1995, 1, 16))
            .endDate(date(1999, 12, 14))
            .firstRegularStartDate(date(1995, 6, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(EUR_LIBOR_6M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .initialStub(IborRateStubCalculation.ofIborInterpolatedRate(EUR_LIBOR_3M, EUR_LIBOR_6M))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1995, 1, 16))
            .endDate(date(1999, 12, 14))
            .firstRegularStartDate(date(1995, 12, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA))
            .frequency(Frequency.P12M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P12M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_E_360)
            .rate(ValueSchedule.of(0.06))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  public void stubAmortizedSwap_cashflows() {
    // cashflows from ird-ex02-stub-amort-swap.xml with Sat/Sun holidays only
    NotionalSchedule notional = NotionalSchedule.builder()
        .currency(EUR)
        .amount(ValueSchedule.builder()
            .initialValue(50000000d)
            .steps(
                ValueStep.of(date(1995, 12, 14), ValueAdjustment.ofReplace(40000000d)),
                ValueStep.of(date(1996, 12, 14), ValueAdjustment.ofReplace(30000000d)),
                ValueStep.of(date(1997, 12, 14), ValueAdjustment.ofReplace(20000000d)),
                ValueStep.of(date(1998, 12, 14), ValueAdjustment.ofReplace(10000000d)))
            .build())
        .build();
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1995, 1, 16))
            .endDate(date(1999, 12, 14))
            .firstRegularStartDate(date(1995, 6, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN)))
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(EUR_LIBOR_6M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, SAT_SUN))
            .initialStub(IborRateStubCalculation.ofIborInterpolatedRate(EUR_LIBOR_3M, EUR_LIBOR_6M))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1995, 1, 16))
            .endDate(date(1999, 12, 14))
            .firstRegularStartDate(date(1995, 12, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN))
            .frequency(Frequency.P12M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P12M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_E_360)
            .rate(ValueSchedule.of(0.06))
            .build())
        .build();
    ImmutableReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(
        HolidayCalendarIds.GBLO, HolidayCalendars.SAT_SUN,
        HolidayCalendarIds.EUTA, HolidayCalendars.SAT_SUN,
        HolidayCalendarIds.SAT_SUN, HolidayCalendars.SAT_SUN,
        HolidayCalendarIds.NO_HOLIDAYS, HolidayCalendars.NO_HOLIDAYS));
    ResolvedSwapLeg expandedPayLeg = payLeg.resolve(refData);
    assertEquals(expandedPayLeg.getPaymentPeriods().size(), 10);
    assertIborPaymentPeriod(expandedPayLeg, 0, "1995-06-14", "1995-01-16", "1995-06-14", 50000000d, "1995-01-12");
    assertIborPaymentPeriod(expandedPayLeg, 1, "1995-12-14", "1995-06-14", "1995-12-14", 50000000d, "1995-06-12");
    assertIborPaymentPeriod(expandedPayLeg, 2, "1996-06-14", "1995-12-14", "1996-06-14", 40000000d, "1995-12-12");
    assertIborPaymentPeriod(expandedPayLeg, 3, "1996-12-16", "1996-06-14", "1996-12-16", 40000000d, "1996-06-12");
    assertIborPaymentPeriod(expandedPayLeg, 4, "1997-06-16", "1996-12-16", "1997-06-16", 30000000d, "1996-12-12");
    assertIborPaymentPeriod(expandedPayLeg, 5, "1997-12-15", "1997-06-16", "1997-12-15", 30000000d, "1997-06-12");
    assertIborPaymentPeriod(expandedPayLeg, 6, "1998-06-15", "1997-12-15", "1998-06-15", 20000000d, "1997-12-11");
    assertIborPaymentPeriod(expandedPayLeg, 7, "1998-12-14", "1998-06-15", "1998-12-14", 20000000d, "1998-06-11");
    assertIborPaymentPeriod(expandedPayLeg, 8, "1999-06-14", "1998-12-14", "1999-06-14", 10000000d, "1998-12-10");
    assertIborPaymentPeriod(expandedPayLeg, 9, "1999-12-14", "1999-06-14", "1999-12-14", 10000000d, "1999-06-10");
    ResolvedSwapLeg expandedRecLeg = recLeg.resolve(refData);
    assertEquals(expandedRecLeg.getPaymentPeriods().size(), 5);
    assertFixedPaymentPeriod(expandedRecLeg, 0, "1995-12-14", "1995-01-16", "1995-12-14", 50000000d, 0.06d);
    assertFixedPaymentPeriod(expandedRecLeg, 1, "1996-12-16", "1995-12-14", "1996-12-16", 40000000d, 0.06d);
    assertFixedPaymentPeriod(expandedRecLeg, 2, "1997-12-15", "1996-12-16", "1997-12-15", 30000000d, 0.06d);
    assertFixedPaymentPeriod(expandedRecLeg, 3, "1998-12-14", "1997-12-15", "1998-12-14", 20000000d, 0.06d);
    assertFixedPaymentPeriod(expandedRecLeg, 4, "1999-12-14", "1998-12-14", "1999-12-14", 10000000d, 0.06d);
  }

  public void stubAmortizedSwap2() {
    // example where notionalStepParameters are used instead of explicit steps
    // fixed and float legs express notionalStepParameters in two different ways, but they resolve to same object model
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex02-stub-amort-swap2.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(1994, 12, 12)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notionalFloat = NotionalSchedule.builder()
        .currency(EUR)
        .amount(ValueSchedule.builder()
            .initialValue(50000000d)
            .stepSequence(ValueStepSequence.of(
                date(1995, 12, 14), date(1998, 12, 14), Frequency.P12M, ValueAdjustment.ofDeltaAmount(-10000000d)))
            .build())
        .build();
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1995, 1, 16))
            .endDate(date(1999, 12, 14))
            .firstRegularStartDate(date(1995, 6, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .build())
        .notionalSchedule(notionalFloat)
        .calculation(IborRateCalculation.builder()
            .index(EUR_LIBOR_6M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .initialStub(IborRateStubCalculation.ofIborInterpolatedRate(EUR_LIBOR_3M, EUR_LIBOR_6M))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(1995, 1, 16))
            .endDate(date(1999, 12, 14))
            .firstRegularStartDate(date(1995, 12, 14))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA))
            .frequency(Frequency.P12M)
            .rollConvention(RollConvention.ofDayOfMonth(14))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P12M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .build())
        .notionalSchedule(notionalFloat)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_E_360)
            .rate(ValueSchedule.of(0.06))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  //-------------------------------------------------------------------------
  public void compoundSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex03-compound-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(2000, 4, 25)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.of(USD, 100000000d);
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2000, 4, 27))
            .endDate(date(2002, 4, 27))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
            .frequency(Frequency.P3M)
            .rollConvention(RollConvention.ofDayOfMonth(27))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(5, GBLO_USNY))
            .compoundingMethod(CompoundingMethod.FLAT)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build();
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2000, 4, 27))
            .endDate(date(2002, 4, 27))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(27))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(5, GBLO_USNY))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_360_ISDA)
            .rate(ValueSchedule.of(0.0585))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), recLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), payLeg);
  }

  public void compoundSwap_cashFlows() {
    // cashflows from ird-ex02-stub-amort-swap.xml with Sat/Sun holidays only
    NotionalSchedule notional = NotionalSchedule.of(USD, 100000000d);
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2000, 4, 27))
            .endDate(date(2002, 4, 27))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN))
            .frequency(Frequency.P3M)
            .rollConvention(RollConvention.ofDayOfMonth(27))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(5, SAT_SUN))
            .compoundingMethod(CompoundingMethod.FLAT)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build();
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2000, 4, 27))
            .endDate(date(2002, 4, 27))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(27))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(5, SAT_SUN))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_360_ISDA)
            .rate(ValueSchedule.of(0.0585))
            .build())
        .build();
    ImmutableReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(
        HolidayCalendarIds.GBLO, HolidayCalendars.SAT_SUN,
        HolidayCalendarIds.EUTA, HolidayCalendars.SAT_SUN,
        HolidayCalendarIds.USNY, HolidayCalendars.SAT_SUN,
        HolidayCalendarIds.SAT_SUN, HolidayCalendars.SAT_SUN,
        HolidayCalendarIds.NO_HOLIDAYS, HolidayCalendars.NO_HOLIDAYS));
    ResolvedSwapLeg expandedRecLeg = recLeg.resolve(refData);
    assertEquals(expandedRecLeg.getPaymentPeriods().size(), 4);
    assertIborPaymentPeriodCpd(expandedRecLeg, 0, 0, "2000-11-03", "2000-04-27", "2000-07-27", 100000000d, "2000-04-25");
    assertIborPaymentPeriodCpd(expandedRecLeg, 0, 1, "2000-11-03", "2000-07-27", "2000-10-27", 100000000d, "2000-07-25");
    assertIborPaymentPeriodCpd(expandedRecLeg, 1, 0, "2001-05-04", "2000-10-27", "2001-01-29", 100000000d, "2000-10-25");
    assertIborPaymentPeriodCpd(expandedRecLeg, 1, 1, "2001-05-04", "2001-01-29", "2001-04-27", 100000000d, "2001-01-25");
    assertIborPaymentPeriodCpd(expandedRecLeg, 2, 0, "2001-11-05", "2001-04-27", "2001-07-27", 100000000d, "2001-04-25");
    assertIborPaymentPeriodCpd(expandedRecLeg, 2, 1, "2001-11-05", "2001-07-27", "2001-10-29", 100000000d, "2001-07-25");
    // final cashflow dates do not match with GBLO, USNY, GBLO+USNY or SAT_SUN
//    assertIborPaymentPeriodCpd(expandedRecLeg, 3, 0, "2002-05-06", "2001-10-29", "2002-01-29", 100000000d, "2001-10-25");
//    assertIborPaymentPeriodCpd(expandedRecLeg, 3, 1, "2002-05-06", "2002-01-29", "2002-04-29", 100000000d, "2002-01-25");
    ResolvedSwapLeg expandedPayLeg = payLeg.resolve(refData);
    assertEquals(expandedPayLeg.getPaymentPeriods().size(), 4);
    assertFixedPaymentPeriod(expandedPayLeg, 0, "2000-11-03", "2000-04-27", "2000-10-27", 100000000d, 0.0585d);
    assertFixedPaymentPeriod(expandedPayLeg, 1, "2001-05-04", "2000-10-27", "2001-04-27", 100000000d, 0.0585d);
    assertFixedPaymentPeriod(expandedPayLeg, 2, "2001-11-05", "2001-04-27", "2001-10-29", 100000000d, 0.0585d);
    assertFixedPaymentPeriod(expandedPayLeg, 3, "2002-05-06", "2001-10-29", "2002-04-29", 100000000d, 0.0585d);
  }

  //-------------------------------------------------------------------------
  public void dualSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex05-long-stub-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(2000, 4, 3)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.of(EUR, 75000000d);
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2000, 4, 5))
            .firstRegularStartDate(date(2000, 10, 5))
            .lastRegularEndDate(date(2004, 10, 5))
            .endDate(date(2005, 1, 5))
            .overrideStartDate(AdjustableDate.of(date(2000, 3, 5), BusinessDayAdjustment.NONE))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, EUTA))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(5))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, EUTA)))
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .dayCount(ACT_360)
            .index(EUR_EURIBOR_6M)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, EUTA))
            .spread(ValueSchedule.of(0.001))
            .initialStub(IborRateStubCalculation.ofFixedRate(0.05125))
            .finalStub(IborRateStubCalculation.ofIborRate(EUR_EURIBOR_3M))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2000, 4, 5))
            .firstRegularStartDate(date(2000, 10, 5))
            .lastRegularEndDate(date(2004, 10, 5))
            .endDate(date(2005, 1, 5))
            .overrideStartDate(AdjustableDate.of(date(2000, 3, 5), BusinessDayAdjustment.NONE))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, EUTA))
            .frequency(Frequency.P12M)
            .rollConvention(RollConvention.ofDayOfMonth(5))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P12M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, EUTA)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_360_ISDA)
            .rate(ValueSchedule.of(0.0525))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  //-------------------------------------------------------------------------
  public void oisSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex07-ois-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(2001, 1, 25)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.of(EUR, 100000000d);
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2001, 1, 29))
            .endDate(date(2001, 4, 29))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA))
            .frequency(Frequency.TERM)
            .rollConvention(RollConventions.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(1, EUTA))
            .build())
        .notionalSchedule(notional)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(ACT_360)
            .index(EUR_EONIA)
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2001, 1, 29))
            .endDate(date(2001, 4, 29))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA))
            .frequency(Frequency.TERM)
            .rollConvention(RollConventions.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(ACT_360)
            .rate(ValueSchedule.of(0.051))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  //-------------------------------------------------------------------------
  public void compoundAverageSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex30-swap-comp-avg-relative-date.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(2005, 7, 31)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.of(USD, 100000000d);
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2005, 8, 2))
            .endDate(date(2007, 8, 2))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
            .frequency(Frequency.P6M)
            .rollConvention(RollConventions.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_360_ISDA)
            .rate(ValueSchedule.of(0.0003))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2005, 8, 2))
            .endDate(date(2007, 8, 2))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY))
            .frequency(Frequency.P3M)
            .rollConvention(RollConventions.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)))
            .compoundingMethod(CompoundingMethod.STRAIGHT)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_6M)
            .resetPeriods(ResetSchedule.builder()
                .resetFrequency(Frequency.P1M)
                .resetMethod(IborRateResetMethod.UNWEIGHTED)
                .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
                .build())
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  //-------------------------------------------------------------------------
  public void zeroCouponSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex32-zero-coupon-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(2005, 2, 20)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.of(GBP, 100000d);
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2005, 2, 22))
            .endDate(date(2035, 2, 22))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
            .frequency(Frequency.P12M)
            .rollConvention(RollConventions.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_E_360)
            .rate(ValueSchedule.of(0.03))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2005, 2, 22))
            .endDate(date(2035, 2, 22))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .endDateBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
            .frequency(Frequency.P3M)
            .rollConvention(RollConvention.ofDayOfMonth(22))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)))
            .compoundingMethod(CompoundingMethod.FLAT)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(GBP_LIBOR_6M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  //-------------------------------------------------------------------------
  public void inverseFloaterSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex35-inverse-floater-inverse-vs-floating.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(2009, 4, 29)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.of(USD, 100000000d);
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2009, 8, 30))
            .endDate(date(2011, 8, 30))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY))
            .frequency(Frequency.P3M)
            .rollConvention(RollConvention.ofDayOfMonth(30))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY)))
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_3M)
            .dayCount(ACT_360)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .gearing(ValueSchedule.of(-1))
            .spread(ValueSchedule.of(0.0325))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2009, 8, 30))
            .endDate(date(2011, 8, 30))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY))
            .frequency(Frequency.P6M)
            .rollConvention(RollConvention.ofDayOfMonth(30))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY)))
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .index(USD_LIBOR_6M)
            .dayCount(THIRTY_360_ISDA)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, USNY))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  //-------------------------------------------------------------------------
  public void inflationSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/inflation-swap-ex01-yoy.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    Trade trade = trades.get(0);
    assertEquals(trade.getClass(), SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertEquals(swapTrade.getInfo().getTradeDate(), Optional.of(date(2003, 11, 15)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.of(EUR, 1d);
    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2003, 11, 20))
            .endDate(date(2007, 11, 20))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA))
            .overrideStartDate(AdjustableDate.of(date(2003, 11, 12), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .frequency(Frequency.P12M)
            .rollConvention(RollConventions.DAY_20)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P12M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_360_ISDA)
            .rate(ValueSchedule.of(0.01))
            .build())
        .build();
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2003, 11, 20))
            .endDate(date(2007, 11, 20))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA))
            .overrideStartDate(AdjustableDate.of(date(2003, 11, 12), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .frequency(Frequency.P3M)
            .rollConvention(RollConventions.DAY_20)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P12M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)))
            .compoundingMethod(CompoundingMethod.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(InflationRateCalculation.builder()
            .index(PriceIndices.US_CPI_U)
            .lag(Period.ofMonths(3))
            .indexCalculationMethod(PriceIndexCalculationMethod.INTERPOLATED)
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), payLeg);
    assertEqualsBean((Bean) swap.getLegs().get(1), recLeg);
  }

  //-------------------------------------------------------------------------
  public void cds01() {
    String location = "classpath:com/opengamma/strata/loader/fpml/cd-ex01-long-asia-corp-fixreg.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    CdsTrade cdsTrade = (CdsTrade) trades.get(0);
    assertEquals(cdsTrade.getInfo().getTradeDate(), Optional.of(date(2002, 12, 4)));

    Cds expected = Cds.builder()
        .buySell(BUY)
        .legalEntityId(StandardId.of("http://www.fpml.org/spec/2003/entity-id-RED-1-0", "004CC9"))
        .currency(JPY)
        .notional(500000000d)
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(date(2002, 12, 5))
            .endDate(date(2007, 12, 5))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .endDateBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY_JPTO))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY_JPTO))
            .firstRegularStartDate(date(2003, 3, 5))
            .frequency(Frequency.P3M)
            .rollConvention(RollConventions.DAY_5)
            .build())
        .fixedRate(0.007)
        .dayCount(ACT_360)
        .build();
    assertEqualsBean(cdsTrade.getProduct(), expected);
    assertEquals(cdsTrade.getUpfrontFee().isPresent(), false);
  }

  //-------------------------------------------------------------------------
  public void cds02() {
    String location = "classpath:com/opengamma/strata/loader/fpml/cd-ex02-2003-short-asia-corp-fixreg.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    CdsTrade cdsTrade = (CdsTrade) trades.get(0);
    assertEquals(cdsTrade.getInfo().getTradeDate(), Optional.of(date(2002, 12, 4)));

    Cds expected = Cds.builder()
        .buySell(SELL)
        .legalEntityId(StandardId.of("http://www.fpml.org/coding-scheme/external/entity-id-RED-1-0", "008FAQ"))
        .currency(JPY)
        .notional(500000000d)
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(date(2002, 12, 5))
            .endDate(date(2007, 12, 5))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .firstRegularStartDate(date(2003, 3, 5))
            .frequency(Frequency.P3M)
            .rollConvention(RollConventions.DAY_5)
            .build())
        .fixedRate(0.007)
        .dayCount(ACT_360)
        .build();
    assertEqualsBean(cdsTrade.getProduct(), expected);
    assertEquals(cdsTrade.getUpfrontFee().isPresent(), false);
  }

  //-------------------------------------------------------------------------
  public void cdsIndex01() {
    String location = "classpath:com/opengamma/strata/loader/fpml/cdindex-ex01-cdx.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertEquals(trades.size(), 1);
    CdsIndexTrade cdsTrade = (CdsIndexTrade) trades.get(0);
    assertEquals(cdsTrade.getInfo().getTradeDate(), Optional.of(date(2005, 1, 24)));

    CdsIndex expected = CdsIndex.builder()
        .buySell(BUY)
        .cdsIndexId(StandardId.of("CDX-Name", "Dow Jones CDX NA IG.2"))
        .currency(USD)
        .notional(25000000d)
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(date(2004, 3, 23))
            .endDate(date(2009, 3, 20))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .frequency(Frequency.P3M)
            .build())
        .fixedRate(0.0060)
        .dayCount(ACT_360)
        .build();
    assertEqualsBean(cdsTrade.getProduct(), expected);
    assertEquals(cdsTrade.getUpfrontFee().get(), AdjustablePayment.of(USD, 16000, AdjustableDate.of(date(2004, 3, 23))));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "parse")
  Object[][] data_parse() {
    return new Object[][] {
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex01-long-asia-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex02-2003-short-asia-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex02-short-asia-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex03-long-aussie-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex04-short-aussie-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex05-long-emasia-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex06-long-emeur-sov-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex07-2003-long-euro-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex07-long-euro-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex08-2003-short-euro-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex08-short-euro-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex09-long-euro-sov-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex10-2003-long-us-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex10-long-us-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex11-2003-short-us-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex11-short-us-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex12-long-emasia-sov-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex13-long-asia-sov-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex14-long-emlatin-corp-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex15-long-emlatin-sov-fixreg.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex16-short-us-corp-fixreg-recovery-factor.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex17-short-us-corp-portfolio-compression.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cd-ex18-standard-north-american-corp.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/cdindex-ex01-cdx.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/fx-ex01-fx-spot.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/fx-ex02-spot-cross-w-side-rates.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/fx-ex03-fx-fwd.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/fx-ex04-fx-fwd-w-settlement.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/fx-ex05-fx-fwd-w-ssi.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/fx-ex06-fx-fwd-w-splits.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/fx-ex07-non-deliverable-forward.xml"},
        {"classpath:com/opengamma/strata/loader/fpml/fx-ex08-fx-swap.xml"},
    };
  }

  @Test(dataProvider = "parse")
  public void parse(String location) {
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertEquals(trades.size(), 1);
  }

  //-------------------------------------------------------------------------
  public void noTrades() {
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of());
    List<Trade> trades =
        FpmlDocumentParser.of(FpmlPartySelector.any()).parseTrades(rootEl, ImmutableMap.of());
    assertEquals(trades.size(), 0);
  }

  public void badTradeDate() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000/06/30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement tradeTypeEl = XmlElement.ofContent("foo", "fakeTradeType");
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableList.of(tradeHeaderEl, tradeTypeEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlParserPlugin tradeParser = new FpmlParserPlugin() {
      @Override
      public Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
        document.parseTradeInfo(tradeEl);  // expected to throw an exception
        throw new UnsupportedOperationException();
      }

      @Override
      public String getName() {
        return "foo";
      }
    };
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.any(),
        FpmlTradeInfoParserPlugin.standard(),
        ImmutableMap.of("foo", tradeParser));
    assertThrows(
        () -> parser.parseTrades(rootEl, ImmutableMap.of()),
        DateTimeParseException.class,
        ".*2000/06/30.*");
  }

  public void unknownProduct() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000-06-30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement unknownEl = XmlElement.ofChildren("unknown", ImmutableList.of());
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableList.of(tradeHeaderEl, unknownEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.any());
    assertThrows(
        () -> parser.parseTrades(rootEl, ImmutableMap.of()),
        FpmlParseException.class,
        ".*unknown.*");
  }

  public void badSelector() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(allParties -> Optional.of("rubbish"));
    assertThrows(
        () -> parser.parseTrades(resource),
        FpmlParseException.class,
        "Selector returned an ID .*");
  }

  public void notFpml() {
    String location = "classpath:com/opengamma/strata/loader/fpml/not-fpml.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.any());
    assertThrows(
        () -> parser.parseTrades(resource),
        FpmlParseException.class,
        "Unable to find FpML root element.*");
  }

  //-------------------------------------------------------------------------
  public void document() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000-06-30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableMap.of("href", "foo"), ImmutableList.of(tradeHeaderEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlDocument test =
        new FpmlDocument(rootEl, ImmutableMap.of(), FpmlPartySelector.any(), FpmlTradeInfoParserPlugin.standard(), REF_DATA);
    assertEquals(test.getFpmlRoot(), rootEl);
    assertEquals(test.getParties(), ImmutableListMultimap.of());
    assertEquals(test.getReferences(), ImmutableMap.of());
    assertEquals(test.getOurPartyHrefId(), "");
    assertThrows(() -> test.lookupReference(tradeEl), FpmlParseException.class, ".*reference not found.*");
    assertThrows(() -> test.validateNotPresent(tradeEl, "tradeHeader"), FpmlParseException.class, ".*tradeHeader.*");
  }

  public void documentFrequency() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000-06-30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableMap.of("href", "foo"), ImmutableList.of(tradeHeaderEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlDocument test =
        new FpmlDocument(rootEl, ImmutableMap.of(), FpmlPartySelector.any(), FpmlTradeInfoParserPlugin.standard(), REF_DATA);
    assertEquals(test.convertFrequency("1", "M"), Frequency.P1M);
    assertEquals(test.convertFrequency("12", "M"), Frequency.P12M);
    assertEquals(test.convertFrequency("1", "Y"), Frequency.P12M);
    assertEquals(test.convertFrequency("13", "Y"), Frequency.of(Period.ofYears(13)));
  }

  public void documentTenor() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000-06-30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableMap.of("href", "foo"), ImmutableList.of(tradeHeaderEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlDocument test =
        new FpmlDocument(rootEl, ImmutableMap.of(), FpmlPartySelector.any(), FpmlTradeInfoParserPlugin.standard(), REF_DATA);
    assertEquals(test.convertIndexTenor("1", "M"), Tenor.TENOR_1M);
    assertEquals(test.convertIndexTenor("12", "M"), Tenor.TENOR_12M);
    assertEquals(test.convertIndexTenor("1", "Y"), Tenor.TENOR_12M);
    assertEquals(test.convertIndexTenor("13", "Y"), Tenor.of(Period.ofYears(13)));
  }

  //-------------------------------------------------------------------------
  private void assertIborPaymentPeriod(
      ResolvedSwapLeg expandedPayLeg,
      int index,
      String paymentDateStr,
      String startDateStr,
      String endDateStr,
      double notional,
      String fixingDateStr) {

    RatePaymentPeriod pp = (RatePaymentPeriod) expandedPayLeg.getPaymentPeriods().get(index);
    assertEquals(pp.getPaymentDate().toString(), paymentDateStr);
    assertEquals(Math.abs(pp.getNotional()), notional);
    assertEquals(pp.getAccrualPeriods().size(), 1);
    RateAccrualPeriod ap = pp.getAccrualPeriods().get(0);
    assertEquals(ap.getStartDate().toString(), startDateStr);
    assertEquals(ap.getEndDate().toString(), endDateStr);
    if (ap.getRateComputation() instanceof IborInterpolatedRateComputation) {
      assertEquals(((IborInterpolatedRateComputation) ap.getRateComputation()).getFixingDate().toString(), fixingDateStr);
    } else if (ap.getRateComputation() instanceof IborRateComputation) {
      assertEquals(((IborRateComputation) ap.getRateComputation()).getFixingDate().toString(), fixingDateStr);
    } else {
      fail();
    }
  }

  private void assertIborPaymentPeriodCpd(
      ResolvedSwapLeg expandedPayLeg,
      int paymentIndex,
      int accrualIndex,
      String paymentDateStr,
      String startDateStr,
      String endDateStr,
      double notional,
      String fixingDateStr) {

    RatePaymentPeriod pp = (RatePaymentPeriod) expandedPayLeg.getPaymentPeriods().get(paymentIndex);
    assertEquals(pp.getPaymentDate().toString(), paymentDateStr);
    assertEquals(Math.abs(pp.getNotional()), notional);
    assertEquals(pp.getAccrualPeriods().size(), 2);
    RateAccrualPeriod ap = pp.getAccrualPeriods().get(accrualIndex);
    assertEquals(ap.getStartDate().toString(), startDateStr);
    assertEquals(ap.getEndDate().toString(), endDateStr);
    if (ap.getRateComputation() instanceof IborInterpolatedRateComputation) {
      assertEquals(((IborInterpolatedRateComputation) ap.getRateComputation()).getFixingDate().toString(), fixingDateStr);
    } else if (ap.getRateComputation() instanceof IborRateComputation) {
      assertEquals(((IborRateComputation) ap.getRateComputation()).getFixingDate().toString(), fixingDateStr);
    } else {
      fail();
    }
  }

  private void assertFixedPaymentPeriod(
      ResolvedSwapLeg expandedPayLeg,
      int index,
      String paymentDateStr,
      String startDateStr,
      String endDateStr,
      double notional,
      double rate) {

    RatePaymentPeriod pp = (RatePaymentPeriod) expandedPayLeg.getPaymentPeriods().get(index);
    assertEquals(pp.getPaymentDate().toString(), paymentDateStr);
    assertEquals(Math.abs(pp.getNotional()), notional);
    assertEquals(pp.getAccrualPeriods().size(), 1);
    RateAccrualPeriod ap = pp.getAccrualPeriods().get(0);
    assertEquals(ap.getStartDate().toString(), startDateStr);
    assertEquals(ap.getEndDate().toString(), endDateStr);
    if (ap.getRateComputation() instanceof FixedRateComputation) {
      assertEquals(((FixedRateComputation) ap.getRateComputation()).getRate(), rate);
    } else {
      fail();
    }
  }

}
