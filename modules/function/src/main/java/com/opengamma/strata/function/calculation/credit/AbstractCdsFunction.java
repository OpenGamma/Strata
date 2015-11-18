/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.market.curve.IsdaCreditCurveInputs;
import com.opengamma.strata.market.curve.IsdaYieldCurveInputs;
import com.opengamma.strata.market.key.IsdaIndexCreditCurveInputsKey;
import com.opengamma.strata.market.key.IsdaIndexRecoveryRateKey;
import com.opengamma.strata.market.key.IsdaSingleNameCreditCurveInputsKey;
import com.opengamma.strata.market.key.IsdaSingleNameRecoveryRateKey;
import com.opengamma.strata.market.key.IsdaYieldCurveInputsKey;
import com.opengamma.strata.market.value.CdsRecoveryRate;
import com.opengamma.strata.pricer.credit.IsdaCdsPricer;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.ExpandedCds;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.ReferenceInformation;
import com.opengamma.strata.product.credit.ReferenceInformationType;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;

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
    Currency feeCurrency = cds.getFeeLeg().getUpfrontFee().getCurrency();

    Set<MarketDataKey<?>> rateCurveKeys = ImmutableSet.of(
        IsdaYieldCurveInputsKey.of(notionalCurrency),
        IsdaYieldCurveInputsKey.of(feeCurrency));

    Set<Currency> currencies = ImmutableSet.of(notionalCurrency, feeCurrency);
    ReferenceInformation referenceInformation = cds.getReferenceInformation();
    ReferenceInformationType cdsType = referenceInformation.getType();
    // TODO the only real difference between single name and index trades is how the credit curves are keyed and the
    // TODO application of an index factor. We have two switch statements currently to handle this
    // TODO we should be able to handle that a bit more gracefully, but there seems little point in duplicating
    // TODO all of the calculation functions and the entire trade model when the vast majority of behavior is common
    if (cdsType == ReferenceInformationType.SINGLE_NAME) {
      SingleNameReferenceInformation singleNameReferenceInformation = (SingleNameReferenceInformation) referenceInformation;

      Set<MarketDataKey<?>> keys = ImmutableSet.of(
          IsdaSingleNameCreditCurveInputsKey.of(singleNameReferenceInformation),
          IsdaSingleNameRecoveryRateKey.of(singleNameReferenceInformation));

      return FunctionRequirements.builder()
          .singleValueRequirements(Sets.union(rateCurveKeys, keys))
          .outputCurrencies(currencies)
          .build();
    } else if (cdsType == ReferenceInformationType.INDEX) {
      IndexReferenceInformation indexReferenceInformation = (IndexReferenceInformation) referenceInformation;

      Set<MarketDataKey<?>> keys = ImmutableSet.of(
          IsdaIndexCreditCurveInputsKey.of(indexReferenceInformation),
          IsdaIndexRecoveryRateKey.of(indexReferenceInformation));

      return FunctionRequirements.builder()
          .singleValueRequirements(Sets.union(rateCurveKeys, keys))
          .outputCurrencies(currencies)
          .build();
    } else {
      throw new IllegalArgumentException("Unknown ReferenceInformationType " + cdsType);
    }
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(CdsTrade target) {
    return Optional.of(target.getProduct().getFeeLeg().getPeriodicPayments().getNotional().getCurrency());
  }

  // execute for a single product
  protected T execute(CdsTrade trade, DefaultSingleCalculationMarketData provider) {

    IsdaYieldCurveInputsKey yieldCurveInputsKey = IsdaYieldCurveInputsKey.of(
        trade.getProduct().getFeeLeg().getPeriodicPayments().getNotional().getCurrency());
    IsdaYieldCurveInputs yieldCurveInputs = provider.getValue(yieldCurveInputsKey);

    ReferenceInformation referenceInformation = trade.getProduct().getReferenceInformation();
    ReferenceInformationType cdsType = referenceInformation.getType();
    // TODO see comment above on the other switch statement
    IsdaCreditCurveInputs creditCurveInputs;
    CdsRecoveryRate cdsRecoveryRate;
    switch (cdsType) {
      case SINGLE_NAME:
        SingleNameReferenceInformation singleNameReferenceInformation = (SingleNameReferenceInformation) referenceInformation;
        IsdaSingleNameCreditCurveInputsKey singleNameCreditCurveInputsKey =
            IsdaSingleNameCreditCurveInputsKey.of(singleNameReferenceInformation);
        creditCurveInputs = provider.getValue(singleNameCreditCurveInputsKey);
        IsdaSingleNameRecoveryRateKey singleNameRecoveryRateKey =
            IsdaSingleNameRecoveryRateKey.of(singleNameReferenceInformation);
        cdsRecoveryRate = provider.getValue(singleNameRecoveryRateKey);
        break;
      case INDEX:
        IndexReferenceInformation indexReferenceInformation = (IndexReferenceInformation) referenceInformation;
        IsdaIndexCreditCurveInputsKey indexCreditCurveInputsKey =
            IsdaIndexCreditCurveInputsKey.of(indexReferenceInformation);
        creditCurveInputs = provider.getValue(indexCreditCurveInputsKey);
        IsdaIndexRecoveryRateKey indexRecoveryRateKey =
            IsdaIndexRecoveryRateKey.of(indexReferenceInformation);
        cdsRecoveryRate = provider.getValue(indexRecoveryRateKey);
        break;
      default:
        throw new IllegalStateException("unknown reference information type: " + cdsType);
    }
    double recoveryRate = cdsRecoveryRate.getRecoveryRate();
    double scalingFactor = creditCurveInputs.getScalingFactor();
    return execute(
        trade.getProduct().expand(),
        yieldCurveInputs,
        creditCurveInputs,
        provider.getValuationDate(),
        recoveryRate,
        scalingFactor);
  }

  // execute for a single product
  protected abstract T execute(
      ExpandedCds product,
      IsdaYieldCurveInputs yieldCurveInputs,
      IsdaCreditCurveInputs creditCurveInputs,
      LocalDate valuationDate,
      double recoveryRate,
      double scalingFactor);

}
