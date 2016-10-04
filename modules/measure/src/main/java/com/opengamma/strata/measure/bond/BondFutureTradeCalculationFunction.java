/**
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
import com.opengamma.strata.product.bond.BondFutureTrade;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.ResolvedBondFutureTrade;

/**
 * Perform calculations on a single {@code BondFutureTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * An instance of {@link RatesMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#UNIT_PRICE Unit price}
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link FixedCouponBond}. The bond futures delivery is a bond
 * for an amount computed from the bond future price, a conversion factor and the accrued interest.
 */
public class BondFutureTradeCalculationFunction
    implements CalculationFunction<BondFutureTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, BondFutureMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, BondFutureMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, BondFutureMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.UNIT_PRICE, BondFutureMeasureCalculations.DEFAULT::unitPrice)
          .put(Measures.PAR_SPREAD, BondFutureMeasureCalculations.DEFAULT::parSpread)
          .put(Measures.CURRENCY_EXPOSURE, BondFutureMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.RESOLVED_TARGET, (rt, smd) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public BondFutureTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<BondFutureTrade> targetType() {
    return BondFutureTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(BondFutureTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(BondFutureTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      BondFutureTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    BondFuture product = trade.getProduct();
    QuoteId quoteId = QuoteId.of(product.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
    Currency currency = product.getCurrency();

    // use lookup to build requirements
    FunctionRequirements freqs = FunctionRequirements.builder()
        .valueRequirements(quoteId)
        .outputCurrencies(currency)
        .build();
    LegalEntityDiscountingMarketDataLookup ledLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    for (FixedCouponBond bond : product.getDeliveryBasket()) {
      freqs = freqs.combinedWith(ledLookup.requirements(bond.getSecurityId(), bond.getLegalEntityId(), bond.getCurrency()));
    }
    return freqs;
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      BondFutureTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedBondFutureTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    LegalEntityDiscountingMarketDataLookup ledLookup = parameters.getParameter(LegalEntityDiscountingMarketDataLookup.class);
    LegalEntityDiscountingScenarioMarketData marketData = ledLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, marketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingScenarioMarketData marketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for BondFutureTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, marketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedBondFutureTrade trade,
        LegalEntityDiscountingScenarioMarketData marketData);
  }

}
