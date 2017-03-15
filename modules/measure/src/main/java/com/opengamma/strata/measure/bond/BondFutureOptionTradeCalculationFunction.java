/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.BondFutureOption;
import com.opengamma.strata.product.bond.BondFutureOptionTrade;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.ResolvedBondFutureOptionTrade;

/**
 * Perform calculations on a single {@code BondFutureOptionTrade} for each of a set of scenarios.
 * <p>
 * This uses Black pricing.
 * An instance of {@link RatesMarketDataLookup} and {@link BondFutureOptionMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#UNIT_PRICE Unit price}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures options in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link BondFuture}.
 */
public class BondFutureOptionTradeCalculationFunction
    implements CalculationFunction<BondFutureOptionTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, BondFutureOptionMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, BondFutureOptionMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, BondFutureOptionMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.UNIT_PRICE, BondFutureOptionMeasureCalculations.DEFAULT::unitPrice)
          .put(Measures.CURRENCY_EXPOSURE, BondFutureOptionMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.RESOLVED_TARGET, (rt, smd, m) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public BondFutureOptionTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<BondFutureOptionTrade> targetType() {
    return BondFutureOptionTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(BondFutureOptionTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(BondFutureOptionTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      BondFutureOptionTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    BondFutureOption option = trade.getProduct();
    BondFuture future = option.getUnderlyingFuture();

    // use lookup to build requirements
    QuoteId optionQuoteId = QuoteId.of(option.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
    FunctionRequirements freqs = FunctionRequirements.builder()
        .valueRequirements(optionQuoteId)
        .outputCurrencies(future.getCurrency(), option.getCurrency())
        .build();
    LegalEntityDiscountingMarketDataLookup ledLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    for (FixedCouponBond bond : future.getDeliveryBasket()) {
      freqs = freqs.combinedWith(ledLookup.requirements(bond.getSecurityId(), bond.getLegalEntityId(), bond.getCurrency()));
    }
    BondFutureOptionMarketDataLookup optionLookup = parameters.getParameter(BondFutureOptionMarketDataLookup.class);
    FunctionRequirements optionReqs = optionLookup.requirements(future.getSecurityId());
    return freqs.combinedWith(optionReqs);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      BondFutureOptionTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedBondFutureOptionTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    LegalEntityDiscountingMarketDataLookup ledLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    LegalEntityDiscountingScenarioMarketData ledMarketData = ledLookup.marketDataView(scenarioMarketData);
    BondFutureOptionMarketDataLookup optionLookup = parameters.getParameter(BondFutureOptionMarketDataLookup.class);
    BondFutureOptionScenarioMarketData optionMarketData = optionLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, ledMarketData, optionMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedBondFutureOptionTrade trade,
      LegalEntityDiscountingScenarioMarketData ratesMarketData,
      BondFutureOptionScenarioMarketData optionMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for BondFutureOptionTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, ratesMarketData, optionMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedBondFutureOptionTrade trade,
        LegalEntityDiscountingScenarioMarketData ratesMarketData,
        BondFutureOptionScenarioMarketData optionMarketData);
  }

}
