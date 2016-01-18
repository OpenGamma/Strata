/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.payment;

import static com.opengamma.strata.calc.runner.function.FunctionUtils.toCurrencyValuesArray;
import static com.opengamma.strata.calc.runner.function.FunctionUtils.toMultiCurrencyValuesArray;
import static com.opengamma.strata.calc.runner.function.FunctionUtils.toScenarioResult;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.runner.SingleCalculationMarketData;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.function.marketdata.MarketDataRatesProvider;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.payment.BulletPaymentTrade;

/**
 * Multi-scenario measure calculations for Bullet Payment trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
class BulletPaymentMeasureCalculations {

  /**
   * The pricer to use.
   */
  private static final DiscountingPaymentPricer PRICER = DiscountingPaymentPricer.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  // restricted constructor
  private BulletPaymentMeasureCalculations() {
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  static CurrencyValuesArray presentValue(
      BulletPaymentTrade trade,
      Payment payment,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> PRICER.presentValue(payment, provider))
        .collect(toCurrencyValuesArray());
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      BulletPaymentTrade trade,
      Payment payment,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> calculatePv01(payment, provider))
        .collect(toMultiCurrencyValuesArray());
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(
      Payment payment,
      RatesProvider provider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(payment, provider).build();
    return provider.curveParameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioResult<CurveCurrencyParameterSensitivities> bucketedPv01(
      BulletPaymentTrade trade,
      Payment payment,
      CalculationMarketData marketData) {

    return ratesProviderStream(marketData)
        .map(provider -> calculateBucketedPv01(payment, provider))
        .collect(toScenarioResult());
  }

  // bucketed PV01 for one scenario
  private static CurveCurrencyParameterSensitivities calculateBucketedPv01(
      Payment payment,
      RatesProvider provider) {

    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(payment, provider).build();
    return provider.curveParameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // common code, creating a stream of RatesProvider from CalculationMarketData
  private static Stream<RatesProvider> ratesProviderStream(CalculationMarketData marketData) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new SingleCalculationMarketData(marketData, index))
        .map(market -> new MarketDataRatesProvider(market));
  }

}
