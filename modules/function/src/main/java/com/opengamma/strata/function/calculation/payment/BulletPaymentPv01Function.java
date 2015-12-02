/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.payment;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toMultiCurrencyArray;

import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.DefaultSingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * Calculates PV01, the present value sensitivity of a {@code BulletPaymentTrade}.
 * This operates by algorithmic differentiation (AD).
 */
public class BulletPaymentPv01Function
    extends AbstractBulletPaymentFunction<MultiCurrencyAmount> {

  @Override
  protected MultiCurrencyAmount execute(Payment product, RatesProvider provider) {
    PointSensitivities pointSensitivity = pricer().presentValueSensitivity(product, provider).build();
    return provider.curveParameterSensitivity(pointSensitivity).total();
  }

  @Override
  public MultiCurrencyValuesArray execute(BulletPaymentTrade trade, CalculationMarketData marketData) {
    Payment product = trade.getProduct().expandToPayment();
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(MarketDataRatesProvider::new)
        .map(provider -> execute(product, provider))
        .collect(toMultiCurrencyArray());
  }
}
