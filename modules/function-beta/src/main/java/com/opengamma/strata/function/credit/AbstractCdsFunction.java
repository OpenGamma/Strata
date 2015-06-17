/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.credit;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.engine.calculations.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.finance.credit.Cds;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.market.key.MarketDataKeys;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static com.opengamma.strata.engine.calculations.function.FunctionUtils.toScenarioResult;

/**
 * Calculates a result of a {@code CdsTrade} for each of a set of scenarios.
 *
 * @param <T>  the return type
 */
public abstract class AbstractCdsFunction<T>
    extends AbstractCalculationFunction<CdsTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractCdsFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractCdsFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  /**
   * Returns the Swap pricer.
   *
   * @return the pricer
   */
  protected CdsPricer pricer() {
    return CdsPricer.DEFAULT;
  }

  //-------------------------------------------------------------------------
  @Override
  public ScenarioResult<T> execute(CdsTrade trade, CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(provider -> execute(trade.getProduct().expand(), provider))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public CalculationRequirements requirements(CdsTrade trade) {
    Cds cds = trade.getProduct();

    Currency notionalCurrency = cds.getFeeLeg().getPeriodicPayments().getNotional().getCurrency();
    Currency feeCurrency = cds.getFeeLeg().getUpfrontFee().getFixedAmount().getCurrency();
//    Set<ObservableKey> indexRateKeys =
//        indices.stream()
//            .map(IndexRateKey::of)
//            .collect(toImmutableSet());
//
//    Set<MarketDataKey<?>> indexCurveKeys =
//        indices.stream()
//            .map(MarketDataKeys::indexCurve)
//            .collect(toImmutableSet());
//
//    Set<DiscountFactorsKey> discountCurveKeys =
//        swap.getLegs().stream()
//            .map(SwapLeg::getCurrency)
//            .map(DiscountFactorsKey::of)
//            .collect(toImmutableSet());

    Set<DiscountFactorsKey> rateCurveKey = ImmutableSet.of(MarketDataKeys.discountFactors(notionalCurrency), MarketDataKeys.discountFactors(feeCurrency));

    Set<MarketDataKey<?>> spreadCurveKey = ImmutableSet.of();

    // TODO recovery rate and index factor
    return CalculationRequirements.builder()
        .singleValueRequirements(Sets.union(rateCurveKey, spreadCurveKey))
        .outputCurrencies(ImmutableSet.of(notionalCurrency, feeCurrency))
        .build();

//    ReferenceInformationType cdsType = cds.getReferenceInformation().getType();
//    switch (cdsType) {
//      case SINGLE_NAME:
//        break;
//      case INDEX:
//        // Index Factor?
//        break;
//      default:
//        throw new IllegalStateException("unknown reference information type: " + cdsType);
//    }
//
//    return CalculationRequirements.builder()
//        .singleValueRequirements(Sets.newHashSet())
//        .timeSeriesRequirements()
//        .outputCurrencies(ImmutableSet.of(cds.getFeeLeg().getPeriodicPayments().getNotional().getCurrency()))
//        .build();
  }

  /**
   * Returns the currency of the trade.
   *
   * @param target  the swap that is the target of the calculation
   * @return the currency of the cds
   */
  @Override
  public Optional<Currency> defaultReportingCurrency(CdsTrade target) {
    return Optional.of(target.getProduct().getFeeLeg().getPeriodicPayments().getNotional().getCurrency());
  }

  // execute for a single product
  protected abstract T execute(ExpandedCds product, DefaultSingleCalculationMarketData provider);

}
