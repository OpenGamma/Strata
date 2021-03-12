/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSmileVolatilities;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.product.option.Barrier;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Utility methods used in pricing calculations for FX products.
 */
class FxCalculationUtils {

  private FxCalculationUtils() {
  }

  //-------------------------------------------------------------------------
  // ensures that the volatilities are correct
  static BlackFxOptionVolatilities checkBlackVolatilities(FxOptionVolatilities volatilities) {
    if (volatilities instanceof BlackFxOptionVolatilities) {
      return (BlackFxOptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException("FX option Black pricing requires BlackFxOptionVolatilities");
  }

  // ensures that the volatilities are correct
  static BlackFxOptionSmileVolatilities checkVannaVolgaVolatilities(FxOptionVolatilities volatilities) {
    if (volatilities instanceof BlackFxOptionSmileVolatilities) {
      return (BlackFxOptionSmileVolatilities) volatilities;
    }
    throw new IllegalArgumentException("FX option Vanna Volga pricing requires BlackFxOptionSmileVolatilities");
  }

  // ensures that the volatilities are correct
  static BlackFxOptionVolatilities checkTrinomialTreeVolatilities(FxOptionVolatilities volatilities) {
    if (volatilities instanceof BlackFxOptionVolatilities) {
      return (BlackFxOptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException("FX option Trinomial Tree pricing requires BlackFxOptionVolatilities");
  }

  /**
   * Return true if the passed barrier has been breached.
   * <p>
   * A 'down' barrier has been breached if the current spot rate is below the barrier level.
   * An 'up' barrier has been breached if the current spot rate is above the barrier level.
   *
   * @param barrier the barrier definition
   * @param currencyPair the currency pair which the barrier relates to
   * @param rateProvider the rates provider, used to access the spot FX rate for the pair
   * @return true if the passed barrier has been breached
   */
  static boolean isBarrierBreached(Barrier barrier, CurrencyPair currencyPair, FxRateProvider rateProvider) {

    if (!(barrier instanceof SimpleConstantContinuousBarrier)) {
      throw new IllegalArgumentException(Messages.format(
          "Only SimpleConstantContinuousBarrier barrier is supported for FX Options, found {}",
          barrier.getClass().getSimpleName()));
    }
    SimpleConstantContinuousBarrier constantBarrier = (SimpleConstantContinuousBarrier) barrier;

    BarrierType barrierType = constantBarrier.getBarrierType();
    double barrierLevel = constantBarrier.getBarrierLevel();
    double spot = rateProvider.fxRate(currencyPair);

    // return true if barrier is a down barrier with level below spot or an up barrier with level above spot
    return barrierType == BarrierType.DOWN ?
        spot <= barrierLevel :
        spot >= barrierLevel;
  }
}
