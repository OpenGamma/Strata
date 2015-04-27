/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.rate.fra;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.instrument.fra.ForwardRateAgreementDefinition;
import com.opengamma.analytics.financial.interestrate.fra.derivative.ForwardRateAgreement;
import com.opengamma.analytics.financial.interestrate.fra.provider.ForwardRateAgreementDiscountingMethod;
import com.opengamma.analytics.financial.interestrate.payments.derivative.Payment;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.util.timeseries.zdt.ImmutableZonedDateTimeDoubleTimeSeries;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.finance.rate.IborRateObservation;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;
import com.opengamma.strata.finance.rate.fra.FraDiscountingMethod;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.marketdata.key.IndexCurveKey;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Calculates the par rate of a {@code FraTrade} for each of a set of scenarios.
 */
public class FraParRateAnalyticsFunction
    extends AbstractFraFunction<Double> {
  // Note that this does not handle interpolated index Fra, or non-ISDA discounting

  @Override
  public List<Double> execute(FraTrade trade, CalculationMarketData marketData) {
    if (trade.getProduct().getIndexInterpolated().isPresent()) {
      throw new UnsupportedOperationException("Fra with interpolated index not supported");
    }
    if (trade.getProduct().getDiscounting() != FraDiscountingMethod.ISDA) {
      throw new UnsupportedOperationException("Non-ISDA Fra discounting not supported");
    }
    ExpandedFra product = trade.getProduct().expand();
    
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(md -> createMulticurve(product, md))
        .map(pair -> execute(product, pair.getFirst(), pair.getSecond()))
        .collect(toList());
  }

  // create the multicurve
  private Pair<LocalDate, MulticurveProviderDiscount> createMulticurve(
      ExpandedFra product,
      DefaultSingleCalculationMarketData marketData) {

    Currency currency = product.getCurrency();
    Map<Currency, YieldAndDiscountCurve> discountCurves =
        ImmutableMap.of(currency, marketData.getValue(DiscountingCurveKey.of(currency)));

    IborRateObservation observation = (IborRateObservation) product.getFloatingRate();
    IborIndex index = observation.getIndex();
    Map<com.opengamma.analytics.financial.instrument.index.IborIndex, YieldAndDiscountCurve> iborCurves =
        ImmutableMap.of(Legacy.iborIndex(index), marketData.getValue(IndexCurveKey.of(index)));

    MulticurveProviderDiscount multicurve = new MulticurveProviderDiscount(
        discountCurves, iborCurves, ImmutableMap.of(), FxMatrix.empty());
    return Pair.of(marketData.getValuationDate(), multicurve);
  }

  // execute for a single trade
  private double execute(ExpandedFra product, LocalDate valuationDate, MulticurveProviderDiscount multicurve) {
    // have to convert each time because valuation date may differ
    IborRateObservation observation = (IborRateObservation) product.getFloatingRate();
    IborIndex index = observation.getIndex();
    LocalDate fixingDate = observation.getFixingDate();
    LocalDate effectiveDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate);
    
    ForwardRateAgreementDefinition analyticFraDefn = new ForwardRateAgreementDefinition(
        product.getCurrency(),
        product.getPaymentDate().atStartOfDay(ZoneOffset.UTC),
        product.getStartDate().atStartOfDay(ZoneOffset.UTC),
        product.getEndDate().atStartOfDay(ZoneOffset.UTC),
        product.getYearFraction(),
        product.getNotional(),
        fixingDate.atStartOfDay(ZoneOffset.UTC),
        effectiveDate.atStartOfDay(ZoneOffset.UTC),
        maturityDate.atStartOfDay(ZoneOffset.UTC),
        Legacy.iborIndex(index),
        product.getFixedRate(),
        index.getFixingCalendar());
    Payment analyticFra = analyticFraDefn.toDerivative(
        valuationDate.atStartOfDay(ZoneOffset.UTC),
        ImmutableZonedDateTimeDoubleTimeSeries.ofEmptyUTC());  // TODO: time-series

    if (analyticFra instanceof ForwardRateAgreement) {
      return ForwardRateAgreementDiscountingMethod.getInstance().parRate((ForwardRateAgreement) analyticFra, multicurve);
    } else {
      return 0d;  // TODO: correct answer
    }
  }

  @Override
  protected Double execute(ExpandedFra product, RatesProvider provider) {
    throw new IllegalStateException("Never invoked");
  }

}
