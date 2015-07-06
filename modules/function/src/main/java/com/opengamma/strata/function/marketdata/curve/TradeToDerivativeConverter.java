/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Ordering;
import com.opengamma.analytics.convention.daycount.DayCount;
import com.opengamma.analytics.financial.instrument.fra.ForwardRateAgreementDefinition;
import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedIbor;
import com.opengamma.analytics.financial.instrument.swap.SwapFixedIborDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapLegType;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Converts trades into legacy {@link InstrumentDerivative} instances required by the analytics.
 */
class TradeToDerivativeConverter {

  // Private constructor because this class only contains helper methods
  private TradeToDerivativeConverter() {
  }

  /**
   * Converts a trade to a derivative which can be passed to the analytics.
   *
   * @param trade  a trade
   * @param valuationDate  the valuation date
   * @return a derivative created from the trade
   * @throws IllegalArgumentException if the trade type is unknown and cannot be converted
   */
  static InstrumentDerivative convert(Trade trade, LocalDate valuationDate) {
    if (trade instanceof FraTrade) {
      return convertFra((FraTrade) trade, valuationDate);
    } else if (trade instanceof SwapTrade) {
      return convertSwap((SwapTrade) trade, valuationDate);
    } else {
      throw new IllegalArgumentException("Unable to create derivatives for trade type " + trade.getClass().getName());
    }
  }

  private static InstrumentDerivative convertFra(FraTrade trade, LocalDate valuationDate) {
    ExpandedFra expandedFra = trade.getProduct().expand();
    IborRateObservation observation = (IborRateObservation) expandedFra.getFloatingRate();
    IborIndex index = observation.getIndex();
    LocalDate fixingDate = observation.getFixingDate();
    LocalDate effectiveDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate);

    ForwardRateAgreementDefinition analyticFraDefn = new ForwardRateAgreementDefinition(
        expandedFra.getCurrency(),
        expandedFra.getPaymentDate().atStartOfDay(ZoneOffset.UTC),
        expandedFra.getStartDate().atStartOfDay(ZoneOffset.UTC),
        expandedFra.getEndDate().atStartOfDay(ZoneOffset.UTC),
        expandedFra.getYearFraction(),
        expandedFra.getNotional(),
        fixingDate.atStartOfDay(ZoneOffset.UTC),
        effectiveDate.atStartOfDay(ZoneOffset.UTC),
        maturityDate.atStartOfDay(ZoneOffset.UTC),
        Legacy.iborIndex(index),
        expandedFra.getFixedRate(),
        index.getFixingCalendar());

    // no need for time-series
    return analyticFraDefn.toDerivative(valuationDate.atStartOfDay(ZoneOffset.UTC));
  }

  private static InstrumentDerivative convertSwap(SwapTrade trade, LocalDate valuationDate) {
    Swap swap = trade.getProduct();
    List<SwapLeg> legs = swap.getLegs();
    List<SwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    List<SwapLeg> iborLegs = swap.getLegs(SwapLegType.IBOR);

    if (legs.size() != 2) {
      throw new UnsupportedOperationException(
          Messages.format(
              "Swap with {} legs cannot be converted, only swaps with 2 legs are supported. Swap: {}",
              legs.size(),
              swap));
    }
    if (fixedLegs.size() == 1 && iborLegs.size() == 1) {
      return convertFixedIborSwap(fixedLegs.get(0), iborLegs.get(0), valuationDate);
    }
    throw new UnsupportedOperationException("Unsupported swap: " + swap);
  }

  private static InstrumentDerivative convertFixedIborSwap(
      SwapLeg fixedSwapLeg,
      SwapLeg floatingSwapLeg,
      LocalDate valuationDate) {

    RateCalculationSwapLeg floatingLeg = (RateCalculationSwapLeg) floatingSwapLeg;
    RateCalculationSwapLeg fixedLeg = (RateCalculationSwapLeg) fixedSwapLeg;
    FixedRateCalculation fixedCalculation = (FixedRateCalculation) fixedLeg.getCalculation();
    IborRateCalculation iborCalculation = (IborRateCalculation) floatingLeg.getCalculation();
    com.opengamma.analytics.financial.instrument.index.IborIndex iborIndex =
        Legacy.iborIndex(iborCalculation.getIndex());
    PeriodicSchedule fixedAccrualSchedule = fixedLeg.getAccrualSchedule();
    PeriodicSchedule floatingAccrualSchedule = floatingLeg.getAccrualSchedule();
    LocalDate settlement = Ordering.natural().min(
        floatingAccrualSchedule.getAdjustedStartDate(),
        fixedAccrualSchedule.getAdjustedStartDate());

    ZonedDateTime settlementDate = settlement.atStartOfDay(ZoneOffset.UTC);
    Period fixedLegPeriod = fixedLeg.getPaymentSchedule().getPaymentFrequency().getPeriod();
    DayCount fixedLegDayCount = Legacy.dayCount(fixedCalculation.getDayCount());
    boolean eom = fixedAccrualSchedule.getRollConvention().map(RollConventions.EOM::equals).orElse(false);
    double notional = fixedLeg.getNotionalSchedule().getAmount().getInitialValue();
    double fixedLegRate = fixedCalculation.getRate().getInitialValue();
    BusinessDayConvention iborLegBusinessDayConvention = iborIndex.getBusinessDayConvention();
    boolean payer = fixedSwapLeg.getPayReceive().isPay();
    HolidayCalendar calendar = floatingAccrualSchedule.getBusinessDayAdjustment().getCalendar();
    int spotLag = iborIndex.getSpotLag();

    LocalDate floatingStart = floatingAccrualSchedule.getStartDate();
    LocalDate fixedStart = fixedAccrualSchedule.getStartDate();
    LocalDate floatingEnd = floatingAccrualSchedule.getEndDate();
    LocalDate fixedEnd = fixedAccrualSchedule.getEndDate();
    LocalDate start = Ordering.natural().min(floatingStart, fixedStart);
    LocalDate end = Ordering.natural().max(floatingEnd, fixedEnd);

    Period tenor = Period.between(start, end);

    GeneratorSwapFixedIbor generator = new GeneratorSwapFixedIbor(
        "Swap Generator",
        fixedLegPeriod,
        fixedLegDayCount,
        iborIndex,
        iborLegBusinessDayConvention,
        eom,
        spotLag,
        calendar);

    SwapFixedIborDefinition definition = SwapFixedIborDefinition.from(
        settlementDate,
        tenor,
        generator,
        notional,
        fixedLegRate,
        payer);

    return definition.toDerivative(valuationDate.atStartOfDay(ZoneOffset.UTC));
  }
}
