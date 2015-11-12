/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.payment;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.calculation.AbstractCalculationFunction;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.key.DiscountFactorsKey;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * Perform calculations on a single {@code BulletPaymentTrade} for each of a set of scenarios.
 * 
 * @param <T>  the return type
 */
public abstract class AbstractBulletPaymentFunction<T>
    extends AbstractCalculationFunction<BulletPaymentTrade, ScenarioResult<T>> {

  /**
   * Creates a new instance which will return results from the {@code execute} method that support automatic
   * currency conversion if the underlying results support it.
   */
  protected AbstractBulletPaymentFunction() {
    super();
  }

  /**
   * Creates a new instance.
   *
   * @param convertCurrencies if this is true the value returned by the {@code execute} method will support
   *   automatic currency conversion if the underlying results support it
   */
  protected AbstractBulletPaymentFunction(boolean convertCurrencies) {
    super(convertCurrencies);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the pricer.
   * 
   * @return the pricer
   */
  protected DiscountingPaymentPricer pricer() {
    return DiscountingPaymentPricer.DEFAULT;
  }

  @Override
  public FunctionRequirements requirements(BulletPaymentTrade trade) {
    Currency currency = trade.getProduct().getCurrency();
    Set<DiscountFactorsKey> discountCurveKeys =
        ImmutableSet.of(DiscountFactorsKey.of(currency));
    return FunctionRequirements.builder()
        .singleValueRequirements(discountCurveKeys)
        .timeSeriesRequirements()
        .outputCurrencies(currency)
        .build();
  }

  @Override
  public ScenarioResult<T> execute(BulletPaymentTrade trade, CalculationMarketData marketData) {
    Payment product = trade.getProduct().expandToPayment();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toScenarioResult(isConvertCurrencies()));
  }

  @Override
  public Optional<Currency> defaultReportingCurrency(BulletPaymentTrade target) {
    return Optional.of(target.getProduct().getCurrency());
  }

  // execute for a single trade
  protected abstract T execute(Payment product, RatesProvider provider);

}
