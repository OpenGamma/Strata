/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.impl.fpml;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FloatingRateName;
import com.opengamma.strata.basics.index.FloatingRateType;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.basics.value.ValueStepSequence;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.XmlElement;
import com.opengamma.strata.loader.fpml.FpmlDocument;
import com.opengamma.strata.loader.fpml.FpmlParseException;
import com.opengamma.strata.loader.fpml.FpmlParserPlugin;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.FixedRateStubCalculation;
import com.opengamma.strata.product.swap.FixingRelativeTo;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.IborRateResetMethod;
import com.opengamma.strata.product.swap.IborRateStubCalculation;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.KnownAmountSwapLeg;
import com.opengamma.strata.product.swap.NegativeRateMethod;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.PaymentRelativeTo;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;
import com.opengamma.strata.product.swap.RateCalculation;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResetSchedule;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * FpML parser for Swaps.
 * <p>
 * This parser handles the subset of FpML necessary to populate the trade model.
 * <p>
 * The following features are not available in the Strata trade model:
 * <ul>
 * <li>initial fixing date
 * <li>first payment date
 * <li>last regular payment date
 * <li>weekly reset frequency
 * <li>spread/gearing in a stub
 * <li>overnight leg first rate is known
 * <li>overnight leg stubs
 * <li>FRA discounting
 * <li>future value notional
 * <li>non-delivered settlement
 * <li>rate treatment
 * <li>FX reset first rate is known
 * </ul>
 */
final class SwapFpmlParserPlugin
    implements FpmlParserPlugin {
  // this class is loaded by ExtendedEnum reflection

  /**
   * The singleton instance of the parser.
   */
  public static final SwapFpmlParserPlugin INSTANCE = new SwapFpmlParserPlugin();

  /**
   * Restricted constructor.
   */
  private SwapFpmlParserPlugin() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Trade parseTrade(FpmlDocument document, XmlElement tradeEl) {
    // supported elements:
    //  'swapStream+'
    //  'swapStream/buyerPartyReference'
    //  'swapStream/sellerPartyReference'
    //  'swapStream/calculationPeriodDates'
    //  'swapStream/paymentDates'
    //  'swapStream/resetDates?'
    //  'swapStream/calculationPeriodAmount'
    //  'swapStream/stubCalculationPeriodAmount?'
    //  'swapStream/principalExchanges?'
    //  'swapStream/calculationPeriodAmount/knownAmountSchedule'
    // ignored elements:
    //  'Product.model?'
    //  'swapStream/cashflows?'
    //  'swapStream/settlementProvision?'
    //  'swapStream/formula?'
    //  'earlyTerminationProvision?'
    //  'cancelableProvision?'
    //  'extendibleProvision?'
    //  'additionalPayment*'
    //  'additionalTerms?'
    // rejected elements:
    //  'swapStream/calculationPeriodAmount/calculation/fxLinkedNotionalSchedule'
    //  'swapStream/calculationPeriodAmount/calculation/futureValueNotional'
    TradeInfoBuilder tradeInfoBuilder = document.parseTradeInfo(tradeEl);
    Swap swap = parseSwap(document, tradeEl, tradeInfoBuilder);
    return SwapTrade.builder()
        .info(tradeInfoBuilder.build())
        .product(swap)
        .build();
  }

  // parses the swap
  Swap parseSwap(FpmlDocument document, XmlElement tradeEl, TradeInfoBuilder tradeInfoBuilder) {
    XmlElement swapEl = tradeEl.getChild("swap");
    ImmutableList<XmlElement> legEls = swapEl.getChildren("swapStream");
    ImmutableList.Builder<SwapLeg> legsBuilder = ImmutableList.builder();
    for (XmlElement legEl : legEls) {
      // calculation
      XmlElement calcPeriodAmountEl = legEl.getChild("calculationPeriodAmount");
      XmlElement calcEl = calcPeriodAmountEl.findChild("calculation")
          .orElse(XmlElement.ofChildren("calculation", ImmutableList.of()));
      PeriodicSchedule accrualSchedule = parseSwapAccrualSchedule(legEl, document);
      PaymentSchedule paymentSchedule = parseSwapPaymentSchedule(legEl, calcEl, document);
      // known amount or rate calculation
      Optional<XmlElement> knownAmountOptEl = calcPeriodAmountEl.findChild("knownAmountSchedule");
      if (knownAmountOptEl.isPresent()) {
        XmlElement knownAmountEl = knownAmountOptEl.get();
        document.validateNotPresent(legEl, "stubCalculationPeriodAmount");
        document.validateNotPresent(legEl, "resetDates");
        // pay/receive and counterparty
        PayReceive payReceive = document.parsePayerReceiver(legEl, tradeInfoBuilder);
        ValueSchedule amountSchedule = parseSchedule(knownAmountEl, document);
        // build
        legsBuilder.add(KnownAmountSwapLeg.builder()
            .payReceive(payReceive)
            .accrualSchedule(accrualSchedule)
            .paymentSchedule(paymentSchedule)
            .amount(amountSchedule)
            .currency(document.parseCurrency(knownAmountEl.getChild("currency")))
            .build());
      } else {
        document.validateNotPresent(calcEl, "fxLinkedNotionalSchedule");
        document.validateNotPresent(calcEl, "futureValueNotional");
        // pay/receive and counterparty
        PayReceive payReceive = document.parsePayerReceiver(legEl, tradeInfoBuilder);
        NotionalSchedule notionalSchedule = parseSwapNotionalSchedule(legEl, calcEl, document);
        RateCalculation calculation = parseSwapCalculation(legEl, calcEl, accrualSchedule, document);
        // build
        legsBuilder.add(RateCalculationSwapLeg.builder()
            .payReceive(payReceive)
            .accrualSchedule(accrualSchedule)
            .paymentSchedule(paymentSchedule)
            .notionalSchedule(notionalSchedule)
            .calculation(calculation)
            .build());
      }
    }
    return Swap.of(legsBuilder.build());
  }

  // parses the accrual schedule
  private PeriodicSchedule parseSwapAccrualSchedule(XmlElement legEl, FpmlDocument document) {
    // supported elements:
    //  'calculationPeriodDates/effectiveDate'
    //  'calculationPeriodDates/relativeEffectiveDate'
    //  'calculationPeriodDates/terminationDate'
    //  'calculationPeriodDates/relativeTerminationDate'
    //  'calculationPeriodDates/calculationPeriodDates'
    //  'calculationPeriodDates/calculationPeriodDatesAdjustments'
    //  'calculationPeriodDates/firstPeriodStartDate?'
    //  'calculationPeriodDates/firstRegularPeriodStartDate?'
    //  'calculationPeriodDates/lastRegularPeriodEndDate?'
    //  'calculationPeriodDates/stubPeriodType?'
    //  'calculationPeriodDates/calculationPeriodFrequency'
    // ignored elements:
    //  'calculationPeriodDates/firstCompoundingPeriodEndDate?'
    PeriodicSchedule.Builder accrualScheduleBuilder = PeriodicSchedule.builder();
    // calculation dates
    XmlElement calcPeriodDatesEl = legEl.getChild("calculationPeriodDates");
    // business day adjustments
    BusinessDayAdjustment bda = document.parseBusinessDayAdjustments(
        calcPeriodDatesEl.getChild("calculationPeriodDatesAdjustments"));
    accrualScheduleBuilder.businessDayAdjustment(bda);
    // start date
    AdjustableDate startDate = calcPeriodDatesEl.findChild("effectiveDate")
        .map(el -> document.parseAdjustableDate(el))
        .orElseGet(() -> document.parseAdjustedRelativeDateOffset(calcPeriodDatesEl.getChild("relativeEffectiveDate")));
    accrualScheduleBuilder.startDate(startDate.getUnadjusted());
    if (!bda.equals(startDate.getAdjustment())) {
      accrualScheduleBuilder.startDateBusinessDayAdjustment(startDate.getAdjustment());
    }
    // end date
    AdjustableDate endDate = calcPeriodDatesEl.findChild("terminationDate")
        .map(el -> document.parseAdjustableDate(el))
        .orElseGet(() -> document.parseAdjustedRelativeDateOffset(calcPeriodDatesEl.getChild("relativeTerminationDate")));
    accrualScheduleBuilder.endDate(endDate.getUnadjusted());
    if (!bda.equals(endDate.getAdjustment())) {
      accrualScheduleBuilder.endDateBusinessDayAdjustment(endDate.getAdjustment());
    }
    // first period start date
    calcPeriodDatesEl.findChild("firstPeriodStartDate").ifPresent(el -> {
      accrualScheduleBuilder.overrideStartDate(document.parseAdjustableDate(el));
    });
    // first regular date
    calcPeriodDatesEl.findChild("firstRegularPeriodStartDate").ifPresent(el -> {
      accrualScheduleBuilder.firstRegularStartDate(document.parseDate(el));
    });
    // last regular date
    calcPeriodDatesEl.findChild("lastRegularPeriodEndDate").ifPresent(el -> {
      accrualScheduleBuilder.lastRegularEndDate(document.parseDate(el));
    });
    // stub type
    calcPeriodDatesEl.findChild("stubPeriodType").ifPresent(el -> {
      accrualScheduleBuilder.stubConvention(parseStubConvention(el, document));
    });
    // frequency
    XmlElement freqEl = calcPeriodDatesEl.getChild("calculationPeriodFrequency");
    Frequency accrualFreq = document.parseFrequency(freqEl);
    accrualScheduleBuilder.frequency(accrualFreq);
    // roll convention
    accrualScheduleBuilder.rollConvention(
        document.convertRollConvention(freqEl.getChild("rollConvention").getContent()));
    return accrualScheduleBuilder.build();
  }

  // parses the payment schedule
  private PaymentSchedule parseSwapPaymentSchedule(XmlElement legEl, XmlElement calcEl, FpmlDocument document) {
    // supported elements:
    //  'paymentDates/paymentFrequency'
    //  'paymentDates/payRelativeTo'
    //  'paymentDates/paymentDaysOffset?'
    //  'paymentDates/paymentDatesAdjustments'
    //  'calculationPeriodAmount/calculation/compoundingMethod'
    // ignored elements:
    //  'paymentDates/calculationPeriodDatesReference'
    //  'paymentDates/resetDatesReference'
    //  'paymentDates/valuationDatesReference'
    //  'paymentDates/firstPaymentDate?'
    //  'paymentDates/lastRegularPaymentDate?'
    PaymentSchedule.Builder paymentScheduleBuilder = PaymentSchedule.builder();
    // payment dates
    XmlElement paymentDatesEl = legEl.getChild("paymentDates");
    // frequency
    paymentScheduleBuilder.paymentFrequency(document.parseFrequency(
        paymentDatesEl.getChild("paymentFrequency")));
    paymentScheduleBuilder.paymentRelativeTo(parsePayRelativeTo(paymentDatesEl.getChild("payRelativeTo")));
    // offset
    Optional<XmlElement> paymentOffsetEl = paymentDatesEl.findChild("paymentDaysOffset");
    BusinessDayAdjustment payAdjustment = document.parseBusinessDayAdjustments(
        paymentDatesEl.getChild("paymentDatesAdjustments"));
    if (paymentOffsetEl.isPresent()) {
      Period period = document.parsePeriod(paymentOffsetEl.get());
      if (period.toTotalMonths() != 0) {
        throw new FpmlParseException("Invalid 'paymentDatesAdjustments' value, expected days-based period: " + period);
      }
      Optional<XmlElement> dayTypeEl = paymentOffsetEl.get().findChild("dayType");
      boolean fixingCalendarDays = period.isZero() ||
          (dayTypeEl.isPresent() && dayTypeEl.get().getContent().equals("Calendar"));
      if (fixingCalendarDays) {
        paymentScheduleBuilder.paymentDateOffset(DaysAdjustment.ofCalendarDays(period.getDays(), payAdjustment));
      } else {
        paymentScheduleBuilder.paymentDateOffset(DaysAdjustment.ofBusinessDays(period.getDays(), payAdjustment.getCalendar()));
      }
    } else {
      paymentScheduleBuilder.paymentDateOffset(DaysAdjustment.ofCalendarDays(0, payAdjustment));
    }
    // compounding
    calcEl.findChild("compoundingMethod").ifPresent(compoundingEl -> {
      paymentScheduleBuilder.compoundingMethod(CompoundingMethod.of(compoundingEl.getContent()));
    });
    return paymentScheduleBuilder.build();
  }

  // parses the notional schedule
  private NotionalSchedule parseSwapNotionalSchedule(XmlElement legEl, XmlElement calcEl, FpmlDocument document) {
    // supported elements:
    //  'principalExchanges/initialExchange'
    //  'principalExchanges/finalExchange'
    //  'principalExchanges/intermediateExchange'
    //  'calculationPeriodAmount/calculation/notionalSchedule/notionalStepSchedule'
    //  'calculationPeriodAmount/calculation/notionalSchedule/notionalStepParameters'
    NotionalSchedule.Builder notionalScheduleBuilder = NotionalSchedule.builder();
    // exchanges
    legEl.findChild("principalExchanges").ifPresent(el -> {
      notionalScheduleBuilder.initialExchange(Boolean.parseBoolean(el.getChild("initialExchange").getContent()));
      notionalScheduleBuilder.intermediateExchange(
          Boolean.parseBoolean(el.getChild("intermediateExchange").getContent()));
      notionalScheduleBuilder.finalExchange(Boolean.parseBoolean(el.getChild("finalExchange").getContent()));
    });
    // notional schedule
    XmlElement notionalEl = calcEl.getChild("notionalSchedule");
    XmlElement stepScheduleEl = notionalEl.getChild("notionalStepSchedule");
    Optional<XmlElement> paramScheduleElOpt = notionalEl.findChild("notionalStepParameters");
    double initialValue = document.parseDecimal(stepScheduleEl.getChild("initialValue"));
    ValueStepSequence seq = paramScheduleElOpt.map(el -> parseAmountSchedule(el, initialValue, document)).orElse(null);
    notionalScheduleBuilder.amount(parseSchedule(stepScheduleEl, initialValue, seq, document));
    notionalScheduleBuilder.currency(document.parseCurrency(stepScheduleEl.getChild("currency")));
    return notionalScheduleBuilder.build();
  }

  // parse swap rate calculation
  private RateCalculation parseSwapCalculation(
      XmlElement legEl,
      XmlElement calcEl,
      PeriodicSchedule accrualSchedule,
      FpmlDocument document) {
    // supported elements:
    //  'calculationPeriodAmount/calculation/fixedRateSchedule'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation'
    //  'calculationPeriodAmount/calculation/inflationRateCalculation'
    Optional<XmlElement> fixedOptEl = calcEl.findChild("fixedRateSchedule");
    Optional<XmlElement> floatingOptEl = calcEl.findChild("floatingRateCalculation");
    Optional<XmlElement> inflationOptEl = calcEl.findChild("inflationRateCalculation");

    if (fixedOptEl.isPresent()) {
      return parseFixed(legEl, calcEl, fixedOptEl.get(), document);

    } else if (floatingOptEl.isPresent()) {
      return parseFloat(legEl, calcEl, floatingOptEl.get(), accrualSchedule, document);

    } else if (inflationOptEl.isPresent()) {
      return parseInflation(legEl, calcEl, inflationOptEl.get(), accrualSchedule, document);

    } else {
      throw new FpmlParseException(
          "Invalid 'calculation' type, not fixedRateSchedule, floatingRateCalculation or inflationRateCalculation");
    }
  }

  // Converts an FpML 'fixedRateSchedule' to a {@code RateCalculation}.
  private RateCalculation parseFixed(XmlElement legEl, XmlElement calcEl, XmlElement fixedEl, FpmlDocument document) {
    // supported elements:
    //  'calculationPeriodAmount/calculation/fixedRateSchedule'
    //  'calculationPeriodAmount/calculation/dayCountFraction'
    //  'stubCalculationPeriodAmount'
    // rejected elements:
    //  'resetDates'
    //  'stubCalculationPeriodAmount/initialStub/floatingRate'
    //  'stubCalculationPeriodAmount/finalStub/floatingRate'
    document.validateNotPresent(legEl, "resetDates");
    FixedRateCalculation.Builder fixedRateBuilder = FixedRateCalculation.builder();
    fixedRateBuilder.rate(parseSchedule(fixedEl, document));
    fixedRateBuilder.dayCount(document.parseDayCountFraction(calcEl.getChild("dayCountFraction")));
    // stub
    legEl.findChild("stubCalculationPeriodAmount").ifPresent(stubsEl -> {
      stubsEl.findChild("initialStub").ifPresent(el -> {
        fixedRateBuilder.initialStub(parseStubCalculationForFixed(el, document));
      });
      stubsEl.findChild("finalStub").ifPresent(el -> {
        fixedRateBuilder.finalStub(parseStubCalculationForFixed(el, document));
      });
    });
    return fixedRateBuilder.build();
  }

  // Converts an FpML 'FloatingRateCalculation' to a {@code RateCalculation}.
  private RateCalculation parseFloat(
      XmlElement legEl,
      XmlElement calcEl,
      XmlElement floatingEl,
      PeriodicSchedule accrualSchedule,
      FpmlDocument document) {
    // supported elements:
    //  'calculationPeriodAmount/calculation/floatingRateCalculation'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/floatingRateIndex'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/indexTenor?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/floatingRateMultiplierSchedule?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/spreadSchedule*'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/initialRate?' (Ibor only)
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/averagingMethod?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/negativeInterestRateTreatment?'
    //  'calculationPeriodAmount/calculation/dayCountFraction'
    //  'resetDates/resetRelativeTo'
    //  'resetDates/fixingDates'
    //  'resetDates/rateCutOffDaysOffset' (OIS only)
    //  'resetDates/resetFrequency'
    //  'resetDates/resetDatesAdjustments'
    //  'stubCalculationPeriodAmount/initalStub' (Ibor only, Overnight must match index)
    //  'stubCalculationPeriodAmount/finalStub' (Ibor only, Overnight must match index)
    // ignored elements:
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/finalRateRounding?'
    //  'calculationPeriodAmount/calculation/discounting?'
    //  'resetDates/calculationPeriodDatesReference'
    // rejected elements:
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/spreadSchedule/type?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/rateTreatment?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/capRateSchedule?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/floorRateSchedule?'
    //  'resetDates/initialFixingDate'
    document.validateNotPresent(floatingEl, "rateTreatment");
    document.validateNotPresent(floatingEl, "capRateSchedule");
    document.validateNotPresent(floatingEl, "floorRateSchedule");
    Index index = document.parseIndex(floatingEl);
    if (index instanceof IborIndex) {
      IborRateCalculation.Builder iborRateBuilder = IborRateCalculation.builder();
      // day count
      iborRateBuilder.dayCount(document.parseDayCountFraction(calcEl.getChild("dayCountFraction")));
      // index
      iborRateBuilder.index((IborIndex) document.parseIndex(floatingEl));
      // gearing
      floatingEl.findChild("floatingRateMultiplierSchedule").ifPresent(el -> {
        iborRateBuilder.gearing(parseSchedule(el, document));
      });
      // spread
      if (floatingEl.getChildren("spreadSchedule").size() > 1) {
        throw new FpmlParseException("Only one 'spreadSchedule' is supported");
      }
      floatingEl.findChild("spreadSchedule").ifPresent(el -> {
        document.validateNotPresent(el, "type");
        iborRateBuilder.spread(parseSchedule(el, document));
      });
      // initial fixed rate
      floatingEl.findChild("initialRate").ifPresent(el -> {
        iborRateBuilder.firstRegularRate(document.parseDecimal(el));
      });
      // negative rates
      floatingEl.findChild("negativeInterestRateTreatment").ifPresent(el -> {
        iborRateBuilder.negativeRateMethod(parseNegativeInterestRateTreatment(el));
      });
      // resets
      XmlElement resetDatesEl = legEl.getChild("resetDates");
      document.validateNotPresent(resetDatesEl, "initialFixingDate");
      document.validateNotPresent(resetDatesEl, "rateCutOffDaysOffset");
      resetDatesEl.findChild("resetRelativeTo").ifPresent(el -> {
        iborRateBuilder.fixingRelativeTo(parseResetRelativeTo(el));
      });
      // fixing date offset
      iborRateBuilder.fixingDateOffset(document.parseRelativeDateOffsetDays(resetDatesEl.getChild("fixingDates")));
      Frequency resetFreq = document.parseFrequency(resetDatesEl.getChild("resetFrequency"));
      if (!accrualSchedule.getFrequency().equals(resetFreq)) {
        ResetSchedule.Builder resetScheduleBuilder = ResetSchedule.builder();
        resetScheduleBuilder.resetFrequency(resetFreq);
        floatingEl.findChild("averagingMethod").ifPresent(el -> {
          resetScheduleBuilder.resetMethod(parseAveragingMethod(el));
        });
        resetScheduleBuilder.businessDayAdjustment(
            document.parseBusinessDayAdjustments(resetDatesEl.getChild("resetDatesAdjustments")));
        iborRateBuilder.resetPeriods(resetScheduleBuilder.build());
      }
      // stubs
      legEl.findChild("stubCalculationPeriodAmount").ifPresent(stubsEl -> {
        stubsEl.findChild("initialStub").ifPresent(el -> {
          iborRateBuilder.initialStub(parseStubCalculation(el, document));
        });
        stubsEl.findChild("finalStub").ifPresent(el -> {
          iborRateBuilder.finalStub(parseStubCalculation(el, document));
        });
      });
      return iborRateBuilder.build();

    } else if (index instanceof OvernightIndex) {
      OvernightRateCalculation.Builder overnightRateBuilder = OvernightRateCalculation.builder();
      document.validateNotPresent(floatingEl, "initialRate");  // TODO: should support this in the model
      // stubs
      legEl.findChild("stubCalculationPeriodAmount").ifPresent(stubsEl -> {
        stubsEl.findChild("initialStub").ifPresent(el -> {
          checkStubForOvernightIndex(el, document, (OvernightIndex) index);
        });
        stubsEl.findChild("finalStub").ifPresent(el -> {
          checkStubForOvernightIndex(el, document, (OvernightIndex) index);
        });
      });
      // day count
      overnightRateBuilder.dayCount(document.parseDayCountFraction(calcEl.getChild("dayCountFraction")));
      // index
      overnightRateBuilder.index((OvernightIndex) document.parseIndex(floatingEl));
      // accrual method
      FloatingRateName idx = FloatingRateName.of(floatingEl.getChild("floatingRateIndex").getContent());
      if (idx.getType() == FloatingRateType.OVERNIGHT_COMPOUNDED) {
        overnightRateBuilder.accrualMethod(OvernightAccrualMethod.COMPOUNDED);
      }
      // gearing
      floatingEl.findChild("floatingRateMultiplierSchedule").ifPresent(el -> {
        overnightRateBuilder.gearing(parseSchedule(el, document));
      });
      // spread
      if (floatingEl.getChildren("spreadSchedule").size() > 1) {
        throw new FpmlParseException("Only one 'spreadSchedule' is supported");
      }
      floatingEl.findChild("spreadSchedule").ifPresent(el -> {
        document.validateNotPresent(el, "type");
        overnightRateBuilder.spread(parseSchedule(el, document));
      });
      // negative rates
      floatingEl.findChild("negativeInterestRateTreatment").ifPresent(el -> {
        overnightRateBuilder.negativeRateMethod(parseNegativeInterestRateTreatment(el));
      });
      // rate cut off
      XmlElement resetDatesEl = legEl.getChild("resetDates");
      document.validateNotPresent(resetDatesEl, "initialFixingDate");
      resetDatesEl.findChild("rateCutOffDaysOffset").ifPresent(el -> {
        Period cutOff = document.parsePeriod(el);
        if (cutOff.toTotalMonths() != 0) {
          throw new FpmlParseException("Invalid 'rateCutOffDaysOffset' value, expected days-based period: " + cutOff);
        }
        overnightRateBuilder.rateCutOffDays(-cutOff.getDays());
      });
      return overnightRateBuilder.build();

    } else {
      throw new FpmlParseException("Invalid 'floatingRateIndex' type, not Ibor or Overnight");
    }
  }

  // Converts an FpML 'InflationRateCalculation' to a {@code RateCalculation}.
  private RateCalculation parseInflation(
      XmlElement legEl,
      XmlElement calcEl,
      XmlElement inflationEl,
      PeriodicSchedule accrualSchedule,
      FpmlDocument document) {
    // supported elements:
    //  'calculationPeriodAmount/calculation/inflationRateCalculation'
    //  'calculationPeriodAmount/calculation/inflationRateCalculation/floatingRateIndex'
    //  'calculationPeriodAmount/calculation/inflationRateCalculation/indexTenor?'
    //  'calculationPeriodAmount/calculation/inflationRateCalculation/floatingRateMultiplierSchedule?'
    //  'calculationPeriodAmount/calculation/inflationRateCalculation/inflationLag'
    //  'calculationPeriodAmount/calculation/inflationRateCalculation/interpolationMethod'
    //  'calculationPeriodAmount/calculation/inflationRateCalculation/initialIndexLevel?'
    //  'calculationPeriodAmount/calculation/dayCountFraction'
    // ignored elements:
    // 'calculationPeriodAmount/calculation/inflationRateCalculation/indexSource'
    // 'calculationPeriodAmount/calculation/inflationRateCalculation/mainPublication'
    // 'calculationPeriodAmount/calculation/inflationRateCalculation/fallbackBondApplicable'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/initialRate?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/finalRateRounding?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/averagingMethod?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/negativeInterestRateTreatment?'
    //  'resetDates'
    // rejected elements:
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/spreadSchedule*'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/rateTreatment?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/capRateSchedule?'
    //  'calculationPeriodAmount/calculation/floatingRateCalculation/floorRateSchedule?'
    //  'stubCalculationPeriodAmount'
    document.validateNotPresent(inflationEl, "spreadSchedule");
    document.validateNotPresent(inflationEl, "rateTreatment");
    document.validateNotPresent(inflationEl, "capRateSchedule");
    document.validateNotPresent(inflationEl, "floorRateSchedule");
    document.validateNotPresent(legEl, "stubCalculationPeriodAmount");  // TODO: parse fixed stub rate
    InflationRateCalculation.Builder builder = InflationRateCalculation.builder();
    // index
    builder.index(document.parsePriceIndex(inflationEl));
    // lag
    builder.lag(document.parsePeriod(inflationEl.getChild("inflationLag")));
    // interpolation
    String interpStr = inflationEl.getChild("interpolationMethod").getContent();
    builder.indexCalculationMethod(interpStr.toLowerCase(Locale.ENGLISH).contains("linear") ?
        PriceIndexCalculationMethod.INTERPOLATED :
        PriceIndexCalculationMethod.MONTHLY);
    // initial index
    inflationEl.findChild("initialIndexLevel").ifPresent(el -> {
      builder.firstIndexValue(document.parseDecimal(el));
    });
    // gearing
    inflationEl.findChild("floatingRateMultiplierSchedule").ifPresent(el -> {
      builder.gearing(parseSchedule(el, document));
    });
    return builder.build();
  }

  //-------------------------------------------------------------------------
  // Converts an FpML 'StubValue' to a {@code FixedRateStubCalculation}.
  private FixedRateStubCalculation parseStubCalculationForFixed(XmlElement baseEl, FpmlDocument document) {
    Optional<XmlElement> rateOptEl = baseEl.findChild("stubRate");
    if (rateOptEl.isPresent()) {
      return FixedRateStubCalculation.ofFixedRate(document.parseDecimal(rateOptEl.get()));
    }
    Optional<XmlElement> amountOptEl = baseEl.findChild("stubAmount");
    if (amountOptEl.isPresent()) {
      return FixedRateStubCalculation.ofKnownAmount(document.parseCurrencyAmount(amountOptEl.get()));
    }
    throw new FpmlParseException("Invalid stub, fixed rate leg cannot have a floating rate stub");
  }

  // Converts an FpML 'StubValue' to a {@code IborRateStubCalculation}.
  private IborRateStubCalculation parseStubCalculation(XmlElement baseEl, FpmlDocument document) {
    Optional<XmlElement> rateOptEl = baseEl.findChild("stubRate");
    if (rateOptEl.isPresent()) {
      return IborRateStubCalculation.ofFixedRate(document.parseDecimal(rateOptEl.get()));
    }
    Optional<XmlElement> amountOptEl = baseEl.findChild("stubAmount");
    if (amountOptEl.isPresent()) {
      return IborRateStubCalculation.ofKnownAmount(document.parseCurrencyAmount(amountOptEl.get()));
    }
    List<XmlElement> indicesEls = baseEl.getChildren("floatingRate");
    if (indicesEls.size() == 1) {
      XmlElement indexEl = indicesEls.get(0);
      document.validateNotPresent(indexEl, "floatingRateMultiplierSchedule");
      document.validateNotPresent(indexEl, "spreadSchedule");
      document.validateNotPresent(indexEl, "rateTreatment");
      document.validateNotPresent(indexEl, "capRateSchedule");
      document.validateNotPresent(indexEl, "floorRateSchedule");
      return IborRateStubCalculation.ofIborRate((IborIndex) document.parseIndex(indexEl));
    } else if (indicesEls.size() == 2) {
      XmlElement index1El = indicesEls.get(0);
      document.validateNotPresent(index1El, "floatingRateMultiplierSchedule");
      document.validateNotPresent(index1El, "spreadSchedule");
      document.validateNotPresent(index1El, "rateTreatment");
      document.validateNotPresent(index1El, "capRateSchedule");
      document.validateNotPresent(index1El, "floorRateSchedule");
      XmlElement index2El = indicesEls.get(1);
      document.validateNotPresent(index2El, "floatingRateMultiplierSchedule");
      document.validateNotPresent(index2El, "spreadSchedule");
      document.validateNotPresent(index2El, "rateTreatment");
      document.validateNotPresent(index2El, "capRateSchedule");
      document.validateNotPresent(index2El, "floorRateSchedule");
      return IborRateStubCalculation.ofIborInterpolatedRate(
          (IborIndex) document.parseIndex(index1El),
          (IborIndex) document.parseIndex(index2El));
    }
    throw new FpmlParseException("Unknown stub structure: " + baseEl);
  }

  // checks that the index on a stub matches the main index (this is handling bad FpML)
  private void checkStubForOvernightIndex(XmlElement baseEl, FpmlDocument document, OvernightIndex index) {
    document.validateNotPresent(baseEl, "stubAmount");
    document.validateNotPresent(baseEl, "stubRate");
    List<XmlElement> indicesEls = baseEl.getChildren("floatingRate");
    if (indicesEls.size() == 1) {
      XmlElement indexEl = indicesEls.get(0);
      document.validateNotPresent(indexEl, "floatingRateMultiplierSchedule");
      document.validateNotPresent(indexEl, "spreadSchedule");
      document.validateNotPresent(indexEl, "rateTreatment");
      document.validateNotPresent(indexEl, "capRateSchedule");
      document.validateNotPresent(indexEl, "floorRateSchedule");
      Index parsed = document.parseIndex(indexEl);
      if (parsed.equals(index)) {
        return;
      }
      throw new FpmlParseException("OvernightIndex swap cannot have a different index in the stub: " + baseEl);
    }
    throw new FpmlParseException("Unknown stub structure: " + baseEl);
  }

  //-------------------------------------------------------------------------
  // Converts an FpML 'StubPeriodTypeEnum' to a {@code StubConvention}.
  private StubConvention parseStubConvention(XmlElement baseEl, FpmlDocument document) {
    String str = baseEl.getContent();
    if (str.equals("ShortInitial")) {
      return StubConvention.SHORT_INITIAL;
    } else if (str.equals("ShortFinal")) {
      return StubConvention.SHORT_FINAL;
    } else if (str.equals("LongInitial")) {
      return StubConvention.LONG_INITIAL;
    } else if (str.equals("LongFinal")) {
      return StubConvention.LONG_FINAL;
    } else {
      throw new FpmlParseException(Messages.format(
          "Unknown 'stubPeriodType' value '{}', expected 'ShortInitial', 'ShortFinal', 'LongInitial' or 'LongFinal'", str));
    }
  }

  // Converts an FpML 'Schedule' to a {@code ValueSchedule}.
  private ValueSchedule parseSchedule(XmlElement scheduleEl, FpmlDocument document) {
    // FpML content: ('initialValue', 'step*')
    // FpML 'step' content: ('stepDate', 'stepValue')
    double initialValue = document.parseDecimal(scheduleEl.getChild("initialValue"));
    return parseSchedule(scheduleEl, initialValue, null, document);
  }

  // Converts an FpML 'Schedule' to a {@code ValueSchedule}.
  private ValueSchedule parseSchedule(XmlElement scheduleEl, double initialValue, ValueStepSequence seq, FpmlDocument document) {
    List<XmlElement> stepEls = scheduleEl.getChildren("step");
    ImmutableList.Builder<ValueStep> stepBuilder = ImmutableList.builder();
    for (XmlElement stepEl : stepEls) {
      LocalDate stepDate = document.parseDate(stepEl.getChild("stepDate"));
      double stepValue = document.parseDecimal(stepEl.getChild("stepValue"));
      stepBuilder.add(ValueStep.of(stepDate, ValueAdjustment.ofReplace(stepValue)));
    }
    return ValueSchedule.builder().initialValue(initialValue).steps(stepBuilder.build()).stepSequence(seq).build();
  }

  // Converts an FpML 'NonNegativeAmountSchedule' to a {@code ValueStepSequence}.
  private ValueStepSequence parseAmountSchedule(XmlElement scheduleEl, double initialValue, FpmlDocument document) {
    Frequency freq = document.parseFrequency(scheduleEl.getChild("stepFrequency"));
    LocalDate start = document.parseDate(scheduleEl.getChild("firstNotionalStepDate"));
    LocalDate end = document.parseDate(scheduleEl.getChild("lastNotionalStepDate"));
    Optional<XmlElement> amountElOpt = scheduleEl.findChild("notionalStepAmount");
    if (amountElOpt.isPresent()) {
      double amount = document.parseDecimal(amountElOpt.get());
      return ValueStepSequence.of(start, end, freq, ValueAdjustment.ofDeltaAmount(amount));
    }
    double rate = document.parseDecimal(scheduleEl.getChild("notionalStepRate"));
    String relativeTo = scheduleEl.findChild("stepRelativeTo").map(el -> el.getContent()).orElse("Previous");
    if (relativeTo.equals("Previous")) {
      return ValueStepSequence.of(start, end, freq, ValueAdjustment.ofDeltaMultiplier(rate));
    } else if (relativeTo.equals("Initial")) {
      // data model does not support 'relative to initial' but can calculate amount here
      double amount = initialValue * rate;
      return ValueStepSequence.of(start, end, freq, ValueAdjustment.ofDeltaAmount(amount));
    } else {
      throw new FpmlParseException(Messages.format(
          "Unknown 'stepRelativeTo' value '{}', expected 'Initial' or 'Previous'", relativeTo));
    }
  }

  //-------------------------------------------------------------------------
  // Converts an FpML 'PayRelativeToEnum' to a {@code PaymentRelativeTo}.
  private PaymentRelativeTo parsePayRelativeTo(XmlElement baseEl) {
    String str = baseEl.getContent();
    if (str.equals("CalculationPeriodStartDate")) {
      return PaymentRelativeTo.PERIOD_START;
    } else if (str.equals("CalculationPeriodEndDate")) {
      return PaymentRelativeTo.PERIOD_END;
    } else {
      throw new FpmlParseException(Messages.format(
          "Unknown 'payRelativeTo' value '{}', expected 'CalculationPeriodStartDate' or 'CalculationPeriodEndDate'", str));
    }
  }

  // Converts and FpML 'NegativeInterestRateTreatmentEnum' to a {@code NegativeRateMethod}.
  private NegativeRateMethod parseNegativeInterestRateTreatment(XmlElement baseEl) {
    String str = baseEl.getContent();
    if (str.equals("NegativeInterestRateMethod")) {
      return NegativeRateMethod.ALLOW_NEGATIVE;
    } else if (str.equals("ZeroInterestRateMethod")) {
      return NegativeRateMethod.NOT_NEGATIVE;
    } else {
      throw new FpmlParseException(Messages.format(
          "Unknown 'negativeInterestRateTreatment' value '{}', " +
              "expected 'NegativeInterestRateMethod' or 'ZeroInterestRateMethod'",
          str));
    }
  }

  // Converts an FpML 'AveragingMethodEnum' to a {@code IborRateResetMethod}.
  private IborRateResetMethod parseAveragingMethod(XmlElement baseEl) {
    String str = baseEl.getContent();
    if (str.equals("Unweighted")) {
      return IborRateResetMethod.UNWEIGHTED;
    } else if (str.equals("Weighted")) {
      return IborRateResetMethod.WEIGHTED;
    } else {
      throw new FpmlParseException(Messages.format(
          "Unknown 'resetMethod' value '{}', expected 'Unweighted' or 'Weighted'", str));
    }
  }

  // Converts an FpML 'ResetRelativeToEnum' to a {@code FixingRelativeTo}.
  private FixingRelativeTo parseResetRelativeTo(XmlElement baseEl) {
    String str = baseEl.getContent();
    if (str.equals("CalculationPeriodStartDate")) {
      return FixingRelativeTo.PERIOD_START;
    } else if (str.equals("CalculationPeriodEndDate")) {
      return FixingRelativeTo.PERIOD_END;
    } else {
      throw new FpmlParseException(Messages.format(
          "Unknown 'resetRelativeTo' value '{}', expected 'CalculationPeriodStartDate' or 'CalculationPeriodEndDate'", str));
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return "swap";
  }

}
