/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.credit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.key.IsdaIndexCreditCurveInputsKey;
import com.opengamma.strata.market.key.IsdaIndexRecoveryRateKey;
import com.opengamma.strata.market.key.IsdaSingleNameCreditCurveInputsKey;
import com.opengamma.strata.market.key.IsdaSingleNameRecoveryRateKey;
import com.opengamma.strata.market.key.IsdaYieldCurveInputsKey;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.ExpandedCds;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.ReferenceInformation;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;

/**
 * Perform calculations on a single {@code CdsTrade} for each of a set of scenarios.
 * <p>
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measure#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measure#PAR_RATE Par rate}
 *   <li>{@linkplain Measure#IR01_PARALLEL_ZERO Scalar IR01, based on zero rates}
 *   <li>{@linkplain Measure#IR01_BUCKETED_ZERO Vector curve node IR01, based on zero rates}
 *   <li>{@linkplain Measure#IR01_PARALLEL_PAR Scalar IR01, based on par interest rates}
 *   <li>{@linkplain Measure#IR01_BUCKETED_PAR Vector curve node IR01, based on par interest rates}
 *   <li>{@linkplain Measure#CS01_PARALLEL_PAR Scalar CS01, based on credit par rates}
 *   <li>{@linkplain Measure#CS01_BUCKETED_PAR Vector curve node CS01, based on credit par rates}
 *   <li>{@linkplain Measure#CS01_PARALLEL_HAZARD Scalar CS01, based on hazard rates}
 *   <li>{@linkplain Measure#CS01_BUCKETED_HAZARD Vector curve node CS01, based on hazard rates}
 *   <li>{@linkplain Measure#RECOVERY01 Recovery01}
 *   <li>{@linkplain Measure#JUMP_TO_DEFAULT Jump to Default}
 * </ul>
 * <p>
 * The default reporting currency is determined to be the currency of the fee leg.
 */
public class CdsCalculationFunction
    implements CalculationFunction<CdsTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measure.PAR_RATE, CdsMeasureCalculations::parRate)
          .put(Measure.PRESENT_VALUE, CdsMeasureCalculations::presentValue)
          .put(Measure.IR01_PARALLEL_ZERO, CdsMeasureCalculations::ir01ParallelZero)
          .put(Measure.IR01_BUCKETED_ZERO, CdsMeasureCalculations::ir01BucketedZero)
          .put(Measure.IR01_PARALLEL_PAR, CdsMeasureCalculations::ir01ParallelPar)
          .put(Measure.IR01_BUCKETED_PAR, CdsMeasureCalculations::ir01BucketedPar)
          .put(Measure.CS01_PARALLEL_PAR, CdsMeasureCalculations::cs01ParallelPar)
          .put(Measure.CS01_BUCKETED_PAR, CdsMeasureCalculations::cs01BucketedPar)
          .put(Measure.CS01_PARALLEL_HAZARD, CdsMeasureCalculations::cs01ParallelHazard)
          .put(Measure.CS01_BUCKETED_HAZARD, CdsMeasureCalculations::cs01BucketedHazard)
          .put(Measure.RECOVERY01, CdsMeasureCalculations::recovery01)
          .put(Measure.JUMP_TO_DEFAULT, CdsMeasureCalculations::jumpToDefault)
          .build();

  /**
   * Creates an instance.
   */
  public CdsCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Set<Measure> supportedMeasures() {
    return CALCULATORS.keySet();
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(CdsTrade target) {
    return Optional.of(target.getProduct().getFeeLeg().getPeriodicPayments().getNotional().getCurrency());
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(CdsTrade trade, Set<Measure> measures) {
    Cds cds = trade.getProduct();

    Currency notionalCurrency = cds.getFeeLeg().getPeriodicPayments().getNotional().getCurrency();
    Currency feeCurrency = cds.getFeeLeg().getUpfrontFee().getCurrency();

    Set<MarketDataKey<?>> rateCurveKeys = ImmutableSet.of(
        IsdaYieldCurveInputsKey.of(notionalCurrency),
        IsdaYieldCurveInputsKey.of(feeCurrency));

    Set<Currency> currencies = ImmutableSet.of(notionalCurrency, feeCurrency);
    ReferenceInformation refInfo = cds.getReferenceInformation();
    if (refInfo instanceof SingleNameReferenceInformation) {
      SingleNameReferenceInformation singleNameRefInfo = (SingleNameReferenceInformation) refInfo;
      Set<MarketDataKey<?>> keys = ImmutableSet.of(
          IsdaSingleNameCreditCurveInputsKey.of(singleNameRefInfo),
          IsdaSingleNameRecoveryRateKey.of(singleNameRefInfo));
      return FunctionRequirements.builder()
          .singleValueRequirements(Sets.union(rateCurveKeys, keys))
          .outputCurrencies(currencies)
          .build();

    } else if (refInfo instanceof IndexReferenceInformation) {
      IndexReferenceInformation indexRefInfo = (IndexReferenceInformation) refInfo;
      Set<MarketDataKey<?>> keys = ImmutableSet.of(
          IsdaIndexCreditCurveInputsKey.of(indexRefInfo),
          IsdaIndexRecoveryRateKey.of(indexRefInfo));
      return FunctionRequirements.builder()
          .singleValueRequirements(Sets.union(rateCurveKeys, keys))
          .outputCurrencies(currencies)
          .build();

    } else {
      throw new IllegalStateException("Unknown reference information type: " + refInfo.getType());
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      CdsTrade trade,
      Set<Measure> measures,
      CalculationMarketData scenarioMarketData) {

    // expand the trade once for all measures and all scenarios
    ExpandedCds product = trade.getProduct().expand();

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, trade, product, scenarioMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      CdsTrade trade,
      ExpandedCds product,
      CalculationMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, product, scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        CdsTrade trade,
        ExpandedCds product,
        CalculationMarketData marketData);
  }

}
