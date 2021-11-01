/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.fpml;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.BRL;
import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.INR;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_360_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_E_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.AUSY;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.BRBD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.FRPA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.index.IborIndices.AUD_BBSW_3M;
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
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.joda.beans.Bean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.StandardSchemes;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.AdjustableDates;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
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
import com.opengamma.strata.product.swap.FutureValueNotional;
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
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionExercise;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Test {@link FpmlDocumentParser}.
 */
public class FpmlDocumentParserTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_EUTA = GBLO.combinedWith(EUTA);
  private static final HolidayCalendarId GBLO_USNY_JPTO = GBLO.combinedWith(USNY).combinedWith(JPTO);

  //-------------------------------------------------------------------------
  @Test
  public void bulletPayment() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex28-bullet-payments.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(BulletPaymentTrade.class);
    BulletPaymentTrade bpTrade = (BulletPaymentTrade) trade;
    assertThat(bpTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2001, 4, 29)));
    BulletPayment bp = bpTrade.getProduct();
    assertThat(bp.getPayReceive()).isEqualTo(PAY);
    assertThat(bp.getDate())
        .isEqualTo(AdjustableDate.of(date(2001, 7, 27), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)));
    assertThat(bp.getValue()).isEqualTo(CurrencyAmount.of(USD, 15000));
  }

  @Test
  public void bulletPayment_twoTradesTwoParties() {
    String location = "classpath:com/opengamma/strata/loader/fpml/bullet-payment-weird.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlPartySelector selector = FpmlPartySelector.matchingRegex(Pattern.compile("Party1[ab]"));
    List<Trade> trades = FpmlDocumentParser.of(selector).parseTrades(resource);
    assertThat(trades).hasSize(2);
    Trade trade0 = trades.get(0);
    assertThat(trade0.getClass()).isEqualTo(BulletPaymentTrade.class);
    BulletPaymentTrade bpTrade0 = (BulletPaymentTrade) trade0;
    assertThat(bpTrade0.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2001, 4, 29)));
    assertThat(bpTrade0.getInfo().getId().get().getValue()).isEqualTo("123");
    BulletPayment bp0 = bpTrade0.getProduct();
    assertThat(bp0.getPayReceive()).isEqualTo(PAY);
    assertThat(bp0.getDate())
        .isEqualTo(AdjustableDate.of(date(2001, 7, 27), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)));
    assertThat(bp0.getValue()).isEqualTo(CurrencyAmount.of(USD, 15000));
    Trade trade1 = trades.get(1);
    assertThat(trade1.getClass()).isEqualTo(BulletPaymentTrade.class);
    BulletPaymentTrade bpTrade1 = (BulletPaymentTrade) trade1;
    assertThat(bpTrade1.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2001, 4, 29)));
    assertThat(bpTrade1.getInfo().getId().get().getValue()).isEqualTo("124");
    BulletPayment bp1 = bpTrade1.getProduct();
    assertThat(bp1.getPayReceive()).isEqualTo(RECEIVE);
    assertThat(bp1.getDate())
        .isEqualTo(AdjustableDate.of(date(2001, 8, 27), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO_USNY)));
    assertThat(bp1.getValue()).isEqualTo(CurrencyAmount.of(USD, 15000));
  }

  //-------------------------------------------------------------------------
  @Test
  public void termDeposit() {
    String location = "classpath:com/opengamma/strata/loader/fpml/td-ex01-simple-term-deposit.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(TermDepositTrade.class);
    TermDepositTrade tdTrade = (TermDepositTrade) trade;
    assertThat(tdTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2002, 2, 14)));
    TermDeposit td = tdTrade.getProduct();
    assertThat(td.getBuySell()).isEqualTo(BUY);
    assertThat(td.getStartDate()).isEqualTo(date(2002, 2, 14));
    assertThat(td.getEndDate()).isEqualTo(date(2002, 2, 15));
    assertThat(td.getCurrency()).isEqualTo(CHF);
    assertThat(td.getNotional()).isEqualTo(25000000d);
    assertThat(td.getRate()).isEqualTo(0.04);
    assertThat(td.getDayCount()).isEqualTo(ACT_360);
  }

  //-------------------------------------------------------------------------
  @Test
  public void fxSpot() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex01-fx-spot.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(FxSingleTrade.class);
    FxSingleTrade fxTrade = (FxSingleTrade) trade;
    assertThat(fxTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2001, 10, 23)));
    FxSingle fx = fxTrade.getProduct();
    assertThat(fx.getBaseCurrencyPayment()).isEqualTo(Payment.of(GBP, 10000000, date(2001, 10, 25)));
    assertThat(fx.getCounterCurrencyPayment()).isEqualTo(Payment.of(USD, -14800000, date(2001, 10, 25)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void fxForward() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex03-fx-fwd.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(FxSingleTrade.class);
    FxSingleTrade fxTrade = (FxSingleTrade) trade;
    assertThat(fxTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2001, 11, 19)));
    FxSingle fx = fxTrade.getProduct();
    assertThat(fx.getBaseCurrencyPayment()).isEqualTo(Payment.of(EUR, 10000000, date(2001, 12, 21)));
    assertThat(fx.getCounterCurrencyPayment()).isEqualTo(Payment.of(USD, -9175000, date(2001, 12, 21)));
  }

  @Test
  public void fxForward_splitDate() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex03-fx-fwd-split-date.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(FxSingleTrade.class);
    FxSingleTrade fxTrade = (FxSingleTrade) trade;
    assertThat(fxTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2001, 11, 19)));
    FxSingle fx = fxTrade.getProduct();
    assertThat(fx.getBaseCurrencyPayment()).isEqualTo(Payment.of(EUR, 10000000, date(2001, 12, 21)));
    assertThat(fx.getCounterCurrencyPayment()).isEqualTo(Payment.of(USD, -9175000, date(2001, 12, 22)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void fxNdf() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex07-non-deliverable-forward.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(FxNdfTrade.class);
    FxNdfTrade fxTrade = (FxNdfTrade) trade;
    assertThat(fxTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2002, 1, 9)));
    FxNdf fx = fxTrade.getProduct();
    assertThat(fx.getSettlementCurrencyNotional()).isEqualTo(CurrencyAmount.of(USD, 10000000));
    assertThat(fx.getAgreedFxRate()).isEqualTo(FxRate.of(USD, INR, 43.4));
    assertThat(fx.getIndex()).isEqualTo(ImmutableFxIndex.builder()
        .name("Reuters/RBIB/14:30")
        .currencyPair(CurrencyPair.of(USD, INR))
        .fixingCalendar(USNY)
        .maturityDateOffset(DaysAdjustment.ofCalendarDays(-2))
        .build());
    assertThat(fx.getPaymentDate()).isEqualTo(date(2002, 4, 11));
  }

  //-------------------------------------------------------------------------
  @Test
  public void fxSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/fx-ex08-fx-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(FxSwapTrade.class);
    FxSwapTrade fxTrade = (FxSwapTrade) trade;
    assertThat(fxTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2002, 1, 23)));
    FxSwap fx = fxTrade.getProduct();
    FxSingle nearLeg = fx.getNearLeg();
    assertThat(nearLeg.getBaseCurrencyPayment()).isEqualTo(Payment.of(GBP, 10000000, date(2002, 1, 25)));
    assertThat(nearLeg.getCounterCurrencyPayment()).isEqualTo(Payment.of(USD, -14800000, date(2002, 1, 25)));
    FxSingle farLeg = fx.getFarLeg();
    assertThat(farLeg.getBaseCurrencyPayment()).isEqualTo(Payment.of(GBP, -10000000, date(2002, 2, 25)));
    assertThat(farLeg.getCounterCurrencyPayment()).isEqualTo(Payment.of(USD, 15000000, date(2002, 2, 25)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void swaption() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex10-euro-swaption-relative.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwaptionTrade.class);
    SwaptionTrade swaptionTrade = (SwaptionTrade) trade;
    assertThat(swaptionTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(1992, 8, 30)));
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
        .exerciseInfo(SwaptionExercise.ofEuropean(expiryDate, DaysAdjustment.ofBusinessDays(2, EUTA)))
        .expiryDate(expiryDate)
        .expiryZone(expiryZone)
        .expiryTime(expiryTime)
        .longShort(LongShort.LONG)
        .swaptionSettlement(PhysicalSwaptionSettlement.DEFAULT)
        .underlying(underylingSwap)
        .build();
    assertEqualsBean(swaption, swaptionExpected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void swaption_bermuda() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex14-berm-swaption.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwaptionTrade.class);
    SwaptionTrade swaptionTrade = (SwaptionTrade) trade;
    assertThat(swaptionTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2000, 8, 30)));
    Swaption swaption = swaptionTrade.getProduct();
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, GBLO_EUTA);
    assertThat(swaption.getExerciseInfo()).hasValue(
        SwaptionExercise.ofBermudan(
            AdjustableDates.of(bda, date(2000, 12, 28), date(2001, 4, 28), date(2001, 8, 28)),
            DaysAdjustment.ofBusinessDays(2, GBLO_EUTA)));
    assertThat(swaption.getExpiryDate())
        .isEqualTo(AdjustableDate.of(date(2001, 8, 28), bda));
    assertThat(swaption.getExpiryTime()).isEqualTo(LocalTime.of(11, 0));
  }

  //-------------------------------------------------------------------------
  @Test
  public void swaption_american() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex15-amer-swaption.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwaptionTrade.class);
    SwaptionTrade swaptionTrade = (SwaptionTrade) trade;
    assertThat(swaptionTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2000, 8, 30)));
    Swaption swaption = swaptionTrade.getProduct();
    BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, GBLO_EUTA);
    assertThat(swaption.getExerciseInfo()).hasValue(
        SwaptionExercise.ofAmerican(
            date(2000, 8, 30),
            date(2002, 8, 30),
            bda,
            DaysAdjustment.ofBusinessDays(2, GBLO_EUTA)));
    assertThat(swaption.getExpiryDate())
        .isEqualTo(AdjustableDate.of(date(2002, 8, 30), bda));
    assertThat(swaption.getExpiryTime()).isEqualTo(LocalTime.of(11, 0));
  }

  //-------------------------------------------------------------------------
  @Test
  public void fra() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertFra(trades, false);
  }

  private void assertFra(List<Trade> trades, boolean interpolatedParty1) {
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(FraTrade.class);
    FraTrade fraTrade = (FraTrade) trade;
    assertThat(fraTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(1991, 5, 14)));
    StandardId party1id = StandardId.of("http://www.hsbc.com/swaps/trade-id", "MB87623");
    StandardId party2id = StandardId.of("http://www.abnamro.com/swaps/trade-id", "AA9876");
    assertThat(fraTrade.getInfo().getId()).isEqualTo(Optional.of(interpolatedParty1 ? party1id : party2id));
    Fra fra = fraTrade.getProduct();
    assertThat(fra.getBuySell()).isEqualTo(interpolatedParty1 ? BUY : SELL);
    assertThat(fra.getStartDate()).isEqualTo(date(1991, 7, 17));
    assertThat(fra.getEndDate()).isEqualTo(date(1992, 1, 17));
    assertThat(fra.getBusinessDayAdjustment()).isEqualTo(Optional.empty());
    assertThat(fra.getPaymentDate().getUnadjusted()).isEqualTo(date(1991, 7, 17));
    assertThat(fra.getPaymentDate().getAdjustment()).isEqualTo(BusinessDayAdjustment.of(FOLLOWING, CHZU));
    assertThat(fra.getFixingDateOffset().getDays()).isEqualTo(-2);
    assertThat(fra.getFixingDateOffset().getCalendar()).isEqualTo(GBLO);
    assertThat(fra.getFixingDateOffset().getAdjustment()).isEqualTo(BusinessDayAdjustment.NONE);
    assertThat(fra.getDayCount()).isEqualTo(ACT_360);
    assertThat(fra.getCurrency()).isEqualTo(CHF);
    assertThat(fra.getNotional()).isEqualTo(25000000d);
    assertThat(fra.getFixedRate()).isEqualTo(0.04d);
    assertThat(fra.getIndex()).isEqualTo(interpolatedParty1 ? CHF_LIBOR_3M : CHF_LIBOR_6M);
    assertThat(fra.getIndexInterpolated()).isEqualTo(interpolatedParty1 ? Optional.of(CHF_LIBOR_6M) : Optional.empty());
    assertThat(fra.getDiscounting()).isEqualTo(FraDiscountingMethod.ISDA);
  }

  //-------------------------------------------------------------------------
  @Test
  public void fra_noParty() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.any()).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(FraTrade.class);
    FraTrade fraTrade = (FraTrade) trade;
    assertThat(fraTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(1991, 5, 14)));
    Fra fra = fraTrade.getProduct();
    assertThat(fra.getBuySell()).isEqualTo(BUY);
    assertThat(fra.getStartDate()).isEqualTo(date(1991, 7, 17));
    assertThat(fra.getEndDate()).isEqualTo(date(1992, 1, 17));
    assertThat(fra.getBusinessDayAdjustment()).isEqualTo(Optional.empty());
    assertThat(fra.getPaymentDate().getUnadjusted()).isEqualTo(date(1991, 7, 17));
    assertThat(fra.getPaymentDate().getAdjustment()).isEqualTo(BusinessDayAdjustment.of(FOLLOWING, CHZU));
    assertThat(fra.getFixingDateOffset().getDays()).isEqualTo(-2);
    assertThat(fra.getFixingDateOffset().getCalendar()).isEqualTo(GBLO);
    assertThat(fra.getFixingDateOffset().getAdjustment()).isEqualTo(BusinessDayAdjustment.NONE);
    assertThat(fra.getDayCount()).isEqualTo(ACT_360);
    assertThat(fra.getCurrency()).isEqualTo(CHF);
    assertThat(fra.getNotional()).isEqualTo(25000000d);
    assertThat(fra.getFixedRate()).isEqualTo(0.04d);
    assertThat(fra.getIndex()).isEqualTo(CHF_LIBOR_6M);
    assertThat(fra.getIndexInterpolated()).isEqualTo(Optional.empty());
    assertThat(fra.getDiscounting()).isEqualTo(FraDiscountingMethod.ISDA);
    // check same when using a specific selector instead of FpmlPartySelector.any()
    List<Trade> trades2 = FpmlDocumentParser.of(allParties -> ImmutableList.of()).parseTrades(resource);
    assertThat(trades2).isEqualTo(trades);
  }

  //-------------------------------------------------------------------------
  @Test
  public void fra_interpolated() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-interpolated.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertFra(trades, true);
  }

  //-------------------------------------------------------------------------
  @Test
  public void fra_namespace() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-namespace.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void fra_wrapper1() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-wrapper1.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void fra_wrapper2() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-wrapper2.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void fra_wrapper_clearingStatus() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra-wrapper-clearing-status.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertFra(trades, false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void vanillaSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex01-vanilla-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(1994, 12, 12)));
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
  @Test
  public void stubAmortizedSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex02-stub-amort-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(1994, 12, 12)));
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
            .firstRegularStartDate(date(1995, 6, 14))
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
            .firstRegularStartDate(date(1995, 12, 14))
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

  @Test
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
    assertThat(expandedPayLeg.getPaymentPeriods()).hasSize(10);
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
    assertThat(expandedRecLeg.getPaymentPeriods()).hasSize(5);
    assertFixedPaymentPeriod(expandedRecLeg, 0, "1995-12-14", "1995-01-16", "1995-12-14", 50000000d, 0.06d);
    assertFixedPaymentPeriod(expandedRecLeg, 1, "1996-12-16", "1995-12-14", "1996-12-16", 40000000d, 0.06d);
    assertFixedPaymentPeriod(expandedRecLeg, 2, "1997-12-15", "1996-12-16", "1997-12-15", 30000000d, 0.06d);
    assertFixedPaymentPeriod(expandedRecLeg, 3, "1998-12-14", "1997-12-15", "1998-12-14", 20000000d, 0.06d);
    assertFixedPaymentPeriod(expandedRecLeg, 4, "1999-12-14", "1998-12-14", "1999-12-14", 10000000d, 0.06d);
  }

  @Test
  public void stubAmortizedSwap2() {
    // example where notionalStepParameters are used instead of explicit steps
    // fixed and float legs express notionalStepParameters in two different ways, but they resolve to same object model
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex02-stub-amort-swap2.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(1994, 12, 12)));
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
            .firstRegularStartDate(date(1995, 6, 14))
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
            .firstRegularStartDate(date(1995, 12, 14))
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
  @Test
  public void compoundSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex03-compound-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2000, 4, 25)));
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

  @Test
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
    assertThat(expandedRecLeg.getPaymentPeriods()).hasSize(4);
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
    assertThat(expandedPayLeg.getPaymentPeriods()).hasSize(4);
    assertFixedPaymentPeriod(expandedPayLeg, 0, "2000-11-03", "2000-04-27", "2000-10-27", 100000000d, 0.0585d);
    assertFixedPaymentPeriod(expandedPayLeg, 1, "2001-05-04", "2000-10-27", "2001-04-27", 100000000d, 0.0585d);
    assertFixedPaymentPeriod(expandedPayLeg, 2, "2001-11-05", "2001-04-27", "2001-10-29", 100000000d, 0.0585d);
    assertFixedPaymentPeriod(expandedPayLeg, 3, "2002-05-06", "2001-10-29", "2002-04-29", 100000000d, 0.0585d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void dualSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex05-long-stub-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2000, 4, 3)));
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
            .firstRegularStartDate(date(2000, 10, 5))
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
            .firstRegularStartDate(date(2000, 10, 5))
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
  @Test
  public void oisSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex07-ois-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2001, 1, 25)));
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
  @Test
  public void compoundAverageSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex30-swap-comp-avg-relative-date.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2005, 7, 31)));
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
  @Test
  public void zeroCouponSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex32-zero-coupon-swap.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2005, 2, 20)));
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
  @Test
  public void brlCdiSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/brl-future-value-notional.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2018, 11, 12)));
    Swap swap = swapTrade.getProduct();

    NotionalSchedule notional = NotionalSchedule.of(BRL, 10000000d);
    RateCalculationSwapLeg recLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2018, 11, 14))
            .endDate(date(2020, 11, 14))
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .endDateBusinessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, BRBD))
            .frequency(Frequency.TERM)
            .rollConvention(RollConventions.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(1, USNY, BusinessDayAdjustment.NONE))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .rate(ValueSchedule.of(0.1))
            .dayCount(DayCount.ofBus252(BRBD))
            .futureValueNotional(FutureValueNotional.of(12345670))
            .build())
        .build();
    assertEqualsBean((Bean) swap.getLegs().get(0), recLeg);
  }

  //-------------------------------------------------------------------------
  @Test
  public void inverseFloaterSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex35-inverse-floater-inverse-vs-floating.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2009, 4, 29)));
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
  @Test
  public void iborFloatingLegNoResetDates() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ibor-no-reset-dates.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    Swap swap = swapTrade.getProduct();

    List<SwapLeg> floatLegs = swap.getLegs(SwapLegType.IBOR);
    assertThat(floatLegs).hasSize(1);
    SwapLeg floatLeg = Iterables.getOnlyElement(floatLegs);

    RateCalculationSwapLeg expectedFloatLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(date(2018, 9, 28))
            .endDate(date(2019, 9, 29))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, AUSY))
            .frequency(Frequency.P3M)
            .rollConvention(RollConvention.ofDayOfMonth(29))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, AUSY)))
            .build())
        .notionalSchedule(NotionalSchedule.of(AUD, 500000000))
        .calculation(IborRateCalculation.builder()
            .index(AUD_BBSW_3M)
            .dayCount(ACT_365F)
            .fixingDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, AUSY)))
            .build())
        .build();
    assertEqualsBean((Bean) floatLeg, expectedFloatLeg);
  }

  @Test
  public void oisFloatingLegNoResetDates() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ois-no-reset-dates.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    Swap swap = swapTrade.getProduct();

    List<SwapLeg> oisLegs = swap.getLegs(SwapLegType.OVERNIGHT);
    assertThat(oisLegs).hasSize(1);
    SwapLeg oisLeg = Iterables.getOnlyElement(oisLegs);

    RateCalculationSwapLeg expectedOisLeg = RateCalculationSwapLeg.builder()
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
        .notionalSchedule(NotionalSchedule.of(EUR, 100000000))
        .calculation(OvernightRateCalculation.builder()
            .dayCount(ACT_360)
            .index(EUR_EONIA)
            .build())
        .build();
    assertEqualsBean((Bean) oisLeg, expectedOisLeg);
  }

  //-------------------------------------------------------------------------
  @Test
  public void inflationSwap() {
    String location = "classpath:com/opengamma/strata/loader/fpml/inflation-swap-ex01-yoy.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    Trade trade = trades.get(0);
    assertThat(trade.getClass()).isEqualTo(SwapTrade.class);
    SwapTrade swapTrade = (SwapTrade) trade;
    assertThat(swapTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2003, 11, 15)));
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
            .firstRegularStartDate(date(2004, 11, 20))
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
            .firstRegularStartDate(date(2004, 11, 20))
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
  @Test
  public void cds01() {
    String location = "classpath:com/opengamma/strata/loader/fpml/cd-ex01-long-asia-corp-fixreg.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2"));
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
    CdsTrade cdsTrade = (CdsTrade) trades.get(0);
    assertThat(cdsTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2002, 12, 4)));

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
    assertThat(cdsTrade.getUpfrontFee()).isNotPresent();
  }

  //-------------------------------------------------------------------------
  @Test
  public void cds02() {
    String location = "classpath:com/opengamma/strata/loader/fpml/cd-ex02-2003-short-asia-corp-fixreg.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    CdsTrade cdsTrade = (CdsTrade) trades.get(0);
    assertThat(cdsTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2002, 12, 4)));

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
    assertThat(cdsTrade.getUpfrontFee()).isNotPresent();
  }

  //-------------------------------------------------------------------------
  @Test
  public void cdsIndex01() {
    String location = "classpath:com/opengamma/strata/loader/fpml/cdindex-ex01-cdx.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    CdsIndexTrade cdsTrade = (CdsIndexTrade) trades.get(0);
    assertThat(cdsTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2005, 1, 24)));

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
    assertThat(cdsTrade.getUpfrontFee().get()).isEqualTo(AdjustablePayment.of(USD, 16000, AdjustableDate.of(date(2004, 3, 23))));
  }

  //-------------------------------------------------------------------------
  @Test
  public void cdsIndex02() {
    String location = "classpath:com/opengamma/strata/loader/fpml/cdindex-ex02-indexId.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertThat(trades).hasSize(1);
    CdsIndexTrade cdsTrade = (CdsIndexTrade) trades.get(0);
    assertThat(cdsTrade.getInfo().getTradeDate()).isEqualTo(Optional.of(date(2020, 1, 24)));

    CdsIndex expected = CdsIndex.builder()
        .buySell(BUY)
        .cdsIndexId(StandardId.of(StandardSchemes.RED9_SCHEME, "2I65BYCM5"))
        .currency(USD)
        .notional(25000000d)
        .paymentSchedule(PeriodicSchedule.builder()
            .startDate(date(2020, 3, 23))
            .endDate(date(2021, 6, 20))
            .startDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .endDateBusinessDayAdjustment(BusinessDayAdjustment.NONE)
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .frequency(Frequency.P3M)
            .build())
        .fixedRate(0.0060)
        .dayCount(ACT_360)
        .build();
    assertEqualsBean(cdsTrade.getProduct(), expected);
    assertThat(cdsTrade.getUpfrontFee()).hasValue(AdjustablePayment.of(USD, 16000, AdjustableDate.of(date(2020, 3, 23))));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_parse() {
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

  @ParameterizedTest
  @MethodSource("data_parse")
  public void parse(String location) {
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    List<Trade> trades = FpmlDocumentParser.of(FpmlPartySelector.matching("Party2")).parseTrades(resource);
    assertThat(trades).hasSize(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void noTrades() {
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of());
    List<Trade> trades =
        FpmlDocumentParser.of(FpmlPartySelector.any()).parseTrades(rootEl, ImmutableMap.of());
    assertThat(trades).hasSize(0);
  }

  @Test
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
    assertThatExceptionOfType(DateTimeParseException.class)
        .isThrownBy(() -> parser.parseTrades(rootEl, ImmutableMap.of()))
        .withMessageMatching(".*2000/06/30.*");
  }

  @Test
  public void unknownProduct() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000-06-30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement unknownEl = XmlElement.ofChildren("unknown", ImmutableList.of());
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableList.of(tradeHeaderEl, unknownEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.any());
    assertThatExceptionOfType(FpmlParseException.class)
        .isThrownBy(() -> parser.parseTrades(rootEl, ImmutableMap.of()))
        .withMessageMatching(".*unknown.*");
  }

  @Test
  public void badSelector() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex08-fra.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(allParties -> ImmutableList.of("rubbish"));
    assertThatExceptionOfType(FpmlParseException.class)
        .isThrownBy(() -> parser.parseTrades(resource))
        .withMessageStartingWith("Selector returned an ID ");
  }

  @Test
  public void notFpml() {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" +
        "<root fpm=\"\" fp=\"\" f=\"\">\r\n" +
        "</root>";
    ByteSource resource = CharSource.wrap(xml).asByteSource(StandardCharsets.UTF_8);
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.any());
    assertThat(parser.isKnownFormat(resource)).isFalse();
    assertThatExceptionOfType(FpmlParseException.class)
        .isThrownBy(() -> parser.parseTrades(resource))
        .withMessageStartingWith("Unable to find FpML root element");
  }

  @Test
  public void isKnownFpmlUtf16() {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-16le\"?>\r\n" +
        "<root fpml=\"\">\r\n" +
        "</root>";
    ByteSource resource = CharSource.wrap(xml).asByteSource(StandardCharsets.UTF_16LE);
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.any());
    assertThat(parser.isKnownFormat(resource)).isTrue();
  }

  @Test
  public void notFpmlUtf16() {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-16le\"?>\r\n" +
        "<root fpm=\"\" fp=\"\" f=\"\">\r\n" +
        "</root>";
    ByteSource resource = CharSource.wrap(xml).asByteSource(StandardCharsets.UTF_16LE);
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.any());
    assertThat(parser.isKnownFormat(resource)).isFalse();
  }

  @Test
  public void notFound() {
    String location = "not-found.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.any());
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> parser.isKnownFormat(resource));
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> parser.parseTrades(resource));
  }

  @Test
  public void unsupportedElementInLenientMode() {
    String location = "classpath:com/opengamma/strata/loader/fpml/ird-ex01-vanilla-swap-with-unsupported-element.xml";
    ByteSource resource = ResourceLocator.of(location).getByteSource();
    FpmlDocumentParser parser = FpmlDocumentParser.of(FpmlPartySelector.matching("Party1")).withLenientMode();
    assertThat(parser.isKnownFormat(resource)).isTrue();
    List<Trade> trades = parser.parseTrades(resource);
    assertThat(trades).hasSize(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void document() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000-06-30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableMap.of("href", "foo"), ImmutableList.of(tradeHeaderEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlDocument test =
        new FpmlDocument(rootEl, ImmutableMap.of(), FpmlPartySelector.any(), FpmlTradeInfoParserPlugin.standard(), REF_DATA);
    assertThat(test.getFpmlRoot()).isEqualTo(rootEl);
    assertThat(test.getParties()).isEqualTo(ImmutableListMultimap.of());
    assertThat(test.getReferences()).isEmpty();
    assertThat(test.getOurPartyHrefIds()).isEmpty();
    assertThatExceptionOfType(FpmlParseException.class)
        .isThrownBy(() -> test.lookupReference(tradeEl))
        .withMessageMatching(".*reference not found.*");
    assertThatExceptionOfType(FpmlParseException.class)
        .isThrownBy(() -> test.validateNotPresent(tradeEl, "tradeHeader"))
        .withMessageMatching(".*tradeHeader.*");
  }

  @Test
  public void documentFrequency() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000-06-30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableMap.of("href", "foo"), ImmutableList.of(tradeHeaderEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlDocument test =
        new FpmlDocument(rootEl, ImmutableMap.of(), FpmlPartySelector.any(), FpmlTradeInfoParserPlugin.standard(), REF_DATA);
    assertThat(test.convertFrequency("1", "M")).isEqualTo(Frequency.P1M);
    assertThat(test.convertFrequency("12", "M")).isEqualTo(Frequency.P12M);
    assertThat(test.convertFrequency("1", "Y")).isEqualTo(Frequency.P12M);
    assertThat(test.convertFrequency("13", "Y")).isEqualTo(Frequency.of(Period.ofYears(13)));
  }

  @Test
  public void documentTenor() {
    XmlElement tradeDateEl = XmlElement.ofContent("tradeDate", "2000-06-30");
    XmlElement tradeHeaderEl = XmlElement.ofChildren("tradeHeader", ImmutableList.of(tradeDateEl));
    XmlElement tradeEl = XmlElement.ofChildren("trade", ImmutableMap.of("href", "foo"), ImmutableList.of(tradeHeaderEl));
    XmlElement rootEl = XmlElement.ofChildren("dataDocument", ImmutableList.of(tradeEl));
    FpmlDocument test =
        new FpmlDocument(rootEl, ImmutableMap.of(), FpmlPartySelector.any(), FpmlTradeInfoParserPlugin.standard(), REF_DATA);
    assertThat(test.convertIndexTenor("1", "M")).isEqualTo(Tenor.TENOR_1M);
    assertThat(test.convertIndexTenor("12", "M")).isEqualTo(Tenor.TENOR_12M);
    assertThat(test.convertIndexTenor("1", "Y")).isEqualTo(Tenor.TENOR_12M);
    assertThat(test.convertIndexTenor("13", "Y")).isEqualTo(Tenor.of(Period.ofYears(13)));
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
    assertThat(pp.getPaymentDate().toString()).isEqualTo(paymentDateStr);
    assertThat(Math.abs(pp.getNotional())).isEqualTo(notional);
    assertThat(pp.getAccrualPeriods()).hasSize(1);
    RateAccrualPeriod ap = pp.getAccrualPeriods().get(0);
    assertThat(ap.getStartDate().toString()).isEqualTo(startDateStr);
    assertThat(ap.getEndDate().toString()).isEqualTo(endDateStr);
    if (ap.getRateComputation() instanceof IborInterpolatedRateComputation) {
      assertThat(((IborInterpolatedRateComputation) ap.getRateComputation()).getFixingDate().toString()).isEqualTo(fixingDateStr);
    } else if (ap.getRateComputation() instanceof IborRateComputation) {
      assertThat(((IborRateComputation) ap.getRateComputation()).getFixingDate().toString()).isEqualTo(fixingDateStr);
    } else {
      fail("Unknown type");
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
    assertThat(pp.getPaymentDate().toString()).isEqualTo(paymentDateStr);
    assertThat(Math.abs(pp.getNotional())).isEqualTo(notional);
    assertThat(pp.getAccrualPeriods()).hasSize(2);
    RateAccrualPeriod ap = pp.getAccrualPeriods().get(accrualIndex);
    assertThat(ap.getStartDate().toString()).isEqualTo(startDateStr);
    assertThat(ap.getEndDate().toString()).isEqualTo(endDateStr);
    if (ap.getRateComputation() instanceof IborInterpolatedRateComputation) {
      assertThat(((IborInterpolatedRateComputation) ap.getRateComputation()).getFixingDate().toString()).isEqualTo(fixingDateStr);
    } else if (ap.getRateComputation() instanceof IborRateComputation) {
      assertThat(((IborRateComputation) ap.getRateComputation()).getFixingDate().toString()).isEqualTo(fixingDateStr);
    } else {
      fail("Unknown type");
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
    assertThat(pp.getPaymentDate().toString()).isEqualTo(paymentDateStr);
    assertThat(Math.abs(pp.getNotional())).isEqualTo(notional);
    assertThat(pp.getAccrualPeriods()).hasSize(1);
    RateAccrualPeriod ap = pp.getAccrualPeriods().get(0);
    assertThat(ap.getStartDate().toString()).isEqualTo(startDateStr);
    assertThat(ap.getEndDate().toString()).isEqualTo(endDateStr);
    if (ap.getRateComputation() instanceof FixedRateComputation) {
      assertThat(((FixedRateComputation) ap.getRateComputation()).getRate()).isEqualTo(rate);
    } else {
      fail("Unknown type");
    }
  }

}
