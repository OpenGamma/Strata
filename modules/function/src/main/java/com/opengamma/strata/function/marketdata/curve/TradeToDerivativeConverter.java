/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata.curve;

import java.time.LocalDate;
import java.time.ZoneOffset;

import com.opengamma.analytics.financial.instrument.fra.ForwardRateAgreementDefinition;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.util.timeseries.zdt.ImmutableZonedDateTimeDoubleTimeSeries;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.FraTrade;
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

    return analyticFraDefn.toDerivative(
        valuationDate.atStartOfDay(ZoneOffset.UTC),
        ImmutableZonedDateTimeDoubleTimeSeries.ofEmptyUTC());
  }
}
