/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.payment;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.function.calculation.RatesMarketData;
import com.opengamma.strata.function.calculation.RatesScenarioMarketData;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

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
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    Payment payment = trade.getProduct().getPayment();
    return CurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePresentValue(payment, marketData.scenario(i)));
  }

  // present value for one scenario
  private static CurrencyAmount calculatePresentValue(Payment payment, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    return PRICER.presentValue(payment, provider);
  }

  //-------------------------------------------------------------------------
  // calculates PV01 for all scenarios
  static MultiCurrencyValuesArray pv01(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    Payment payment = trade.getProduct().getPayment();
    return MultiCurrencyValuesArray.of(
        marketData.getScenarioCount(),
        i -> calculatePv01(payment, marketData.scenario(i)));
  }

  // PV01 for one scenario
  private static MultiCurrencyAmount calculatePv01(Payment payment, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(payment, provider).build();
    return provider.parameterSensitivity(pointSensitivity).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates bucketed PV01 for all scenarios
  static ScenarioArray<CurrencyParameterSensitivities> bucketedPv01(
      ResolvedBulletPaymentTrade trade,
      RatesScenarioMarketData marketData) {

    Payment payment = trade.getProduct().getPayment();
    return ScenarioArray.of(
        marketData.getScenarioCount(),
        i -> calculateBucketedPv01(payment, marketData.scenario(i)));
  }

  // bucketed PV01 for one scenario
  private static CurrencyParameterSensitivities calculateBucketedPv01(Payment payment, RatesMarketData marketData) {
    RatesProvider provider = marketData.ratesProvider();
    PointSensitivities pointSensitivity = PRICER.presentValueSensitivity(payment, provider).build();
    return provider.parameterSensitivity(pointSensitivity).multipliedBy(ONE_BASIS_POINT);
  }

}
