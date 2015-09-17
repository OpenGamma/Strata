/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import static com.opengamma.strata.engine.calculation.function.FunctionUtils.toScenarioResult;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.engine.calculation.DefaultSingleCalculationMarketData;
import com.opengamma.strata.engine.calculation.function.result.ScenarioResult;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;
import com.opengamma.strata.finance.credit.Cds;
import com.opengamma.strata.finance.credit.CdsTrade;
import com.opengamma.strata.finance.credit.ExpandedCds;
import com.opengamma.strata.finance.credit.IndexReferenceInformation;
import com.opengamma.strata.finance.credit.ReferenceInformation;
import com.opengamma.strata.finance.credit.ReferenceInformationType;
import com.opengamma.strata.finance.credit.SingleNameReferenceInformation;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.market.curve.IsdaCreditCurveParRates;
import com.opengamma.strata.market.curve.IsdaYieldCurveParRates;
import com.opengamma.strata.market.key.IsdaIndexCreditCurveParRatesKey;
import com.opengamma.strata.market.key.IsdaIndexRecoveryRateKey;
import com.opengamma.strata.market.key.IsdaSingleNameCreditCurveParRatesKey;
import com.opengamma.strata.market.key.IsdaSingleNameRecoveryRateKey;
import com.opengamma.strata.market.key.IsdaYieldCurveParRatesKey;
import com.opengamma.strata.market.value.CdsRecoveryRate;
import com.opengamma.strata.pricer.credit.IsdaCdsPricer;

/**
 * Perform calculations on a single {@code CdsTrade} for each of a set of scenarios.
 * <p>
 * The default reporting currency is determined to be the currency of the fee leg.
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

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected IsdaCdsPricer pricer() {
    return IsdaCdsPricer.DEFAULT;
  }

  @Override
  public ScenarioResult<T> execute(CdsTrade trade, CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(provider -> execute(trade, provider))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public FunctionRequirements requirements(CdsTrade trade) {
    Cds cds = trade.getProduct();

    Currency notionalCurrency = cds.getFeeLeg().getPeriodicPayments().getNotional().getCurrency();
    Currency feeCurrency = cds.getFeeLeg().getUpfrontFee().getFixedAmount().getCurrency();

    Set<MarketDataKey<?>> rateCurveKeys = ImmutableSet.of(
        IsdaYieldCurveParRatesKey.of(notionalCurrency),
        IsdaYieldCurveParRatesKey.of(feeCurrency));

    ReferenceInformation referenceInformation = cds.getReferenceInformation();
    ReferenceInformationType cdsType = referenceInformation.getType();
    // TODO the only real difference between single name and index trades is how the credit curves are keyed and the
    // TODO application of an index factor. We have two switch statements currently to handle this
    // TODO we should be able to handle that a bit more gracefully, but there seems little point in duplicating
    // TODO all of the calculation functions and the entire trade model when the vast majority of behavior is common
    Set<MarketDataKey<?>> spreadCurveKey;
    switch (cdsType) {
      case SINGLE_NAME:
        SingleNameReferenceInformation singleNameReferenceInformation = (SingleNameReferenceInformation) referenceInformation;
        spreadCurveKey = ImmutableSet.of(IsdaSingleNameCreditCurveParRatesKey.of(singleNameReferenceInformation));
        break;
      case INDEX:
        IndexReferenceInformation indexReferenceInformation = (IndexReferenceInformation) referenceInformation;
        spreadCurveKey = ImmutableSet.of(IsdaIndexCreditCurveParRatesKey.of(indexReferenceInformation));
        break;
      default:
        throw new IllegalStateException("unknown reference information type: " + cdsType);
    }
    // TODO index factor as static data behind a resolvable link
    return FunctionRequirements.builder()
        .singleValueRequirements(Sets.union(rateCurveKeys, spreadCurveKey))
        .outputCurrencies(ImmutableSet.of(notionalCurrency, feeCurrency))
        .build();
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(CdsTrade target) {
    return Optional.of(target.getProduct().getFeeLeg().getPeriodicPayments().getNotional().getCurrency());
  }

  // execute for a single product
  protected T execute(CdsTrade trade, DefaultSingleCalculationMarketData provider) {

    IsdaYieldCurveParRatesKey yieldCurveParRatesKey = IsdaYieldCurveParRatesKey.of(
        trade.getProduct().getFeeLeg().getPeriodicPayments().getNotional().getCurrency());
    IsdaYieldCurveParRates yieldCurveParRates = provider.getValue(yieldCurveParRatesKey);

    ReferenceInformation referenceInformation = trade.getProduct().getReferenceInformation();
    ReferenceInformationType cdsType = referenceInformation.getType();
    // TODO see comment above on the other switch statement
    IsdaCreditCurveParRates creditCurveParRates;
    CdsRecoveryRate cdsRecoveryRate;
    switch (cdsType) {
      case SINGLE_NAME:
        SingleNameReferenceInformation singleNameReferenceInformation = (SingleNameReferenceInformation) referenceInformation;
        IsdaSingleNameCreditCurveParRatesKey singleNameCreditCurveParRatesKey =
            IsdaSingleNameCreditCurveParRatesKey.of(singleNameReferenceInformation);
        creditCurveParRates = provider.getValue(singleNameCreditCurveParRatesKey);
        IsdaSingleNameRecoveryRateKey singleNameRecoveryRateKey =
            IsdaSingleNameRecoveryRateKey.of(singleNameReferenceInformation);
        cdsRecoveryRate = provider.getValue(singleNameRecoveryRateKey);
        break;
      case INDEX:
        IndexReferenceInformation indexReferenceInformation = (IndexReferenceInformation) referenceInformation;
        IsdaIndexCreditCurveParRatesKey indexCreditCurveParRatesKey =
            IsdaIndexCreditCurveParRatesKey.of(indexReferenceInformation);
        creditCurveParRates = provider.getValue(indexCreditCurveParRatesKey);
        IsdaIndexRecoveryRateKey indexRecoveryRateKey =
            IsdaIndexRecoveryRateKey.of(indexReferenceInformation);
        cdsRecoveryRate = provider.getValue(indexRecoveryRateKey);
        break;
      default:
        throw new IllegalStateException("unknown reference information type: " + cdsType);
    }
    double recoveryRate = cdsRecoveryRate.getRecoveryRate();
    double scalingFactor = creditCurveParRates.getScalingFactor();
    return execute(
        trade.getProduct().expand(),
        yieldCurveParRates,
        creditCurveParRates,
        provider.getValuationDate(),
        recoveryRate,
        scalingFactor);
  }

  // execute for a single product
  protected abstract T execute(
      ExpandedCds product,
      IsdaYieldCurveParRates yieldCurveParRates,
      IsdaCreditCurveParRates creditCurveParRates,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor);

}
