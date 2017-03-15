/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackBarrierPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackOneTouchAssetPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackOneTouchCashPriceFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Pricer for FX barrier option products in Black-Scholes world.
 * <p>
 * This function provides the ability to price an {@link ResolvedFxSingleBarrierOption}.
 * <p>
 * All of the computation is be based on the counter currency of the underlying FX transaction.
 * For example, price, PV and risk measures of the product will be expressed in USD for an option on EUR/USD.
 */
public class BlackFxSingleBarrierOptionProductPricer {

  /**
   * Default implementation.
   */
  public static final BlackFxSingleBarrierOptionProductPricer DEFAULT = new BlackFxSingleBarrierOptionProductPricer();

  /**
   * Pricer for barrier option without rebate.
   */
  private static final BlackBarrierPriceFormulaRepository BARRIER_PRICER = new BlackBarrierPriceFormulaRepository();
  /**
   * Pricer for rebate.
   */
  private static final BlackOneTouchAssetPriceFormulaRepository ASSET_REBATE_PRICER =
      new BlackOneTouchAssetPriceFormulaRepository();
  /**
   * Pricer for rebate.
   */
  private static final BlackOneTouchCashPriceFormulaRepository CASH_REBATE_PRICER =
      new BlackOneTouchCashPriceFormulaRepository();

  /**
   * Creates an instance.
   */
  public BlackFxSingleBarrierOptionProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FX barrier option product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * It is expressed in the counter currency.
   * <p>
   * The volatility used in this computation is the Black implied volatility at expiry time and strike.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double price = price(option, ratesProvider, volatilities);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    return CurrencyAmount.of(underlyingOption.getCounterCurrency(), signedNotional(underlyingOption) * price);
  }

  /**
   * Calculates the price of the FX barrier option product.
   * <p>
   * The price of the product is the value on the valuation date for one unit of the base currency 
   * and is expressed in the counter currency. The price does not take into account the long/short flag.
   * See {@link #presentValue} for scaling and currency.
   * <p>
   * The volatility used in this computation is the Black implied volatility at expiry time and strike.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the price of the product
   */
  public double price(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    validate(option, ratesProvider, volatilities);
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    if (volatilities.relativeTime(underlyingOption.getExpiry()) < 0d) {
      return 0d;
    }
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    Currency ccyBase = underlyingFx.getBaseCurrencyPayment().getCurrency();
    Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);

    double rateBase = baseDiscountFactors.zeroRate(underlyingFx.getPaymentDate());
    double rateCounter = counterDiscountFactors.zeroRate(underlyingFx.getPaymentDate());
    double costOfCarry = rateCounter - rateBase;
    double dfBase = baseDiscountFactors.discountFactor(underlyingFx.getPaymentDate());
    double dfCounter = counterDiscountFactors.discountFactor(underlyingFx.getPaymentDate());
    double todayFx = ratesProvider.fxRate(currencyPair);
    double strike = underlyingOption.getStrike();
    double forward = todayFx * dfBase / dfCounter;
    double volatility = volatilities.volatility(currencyPair, underlyingOption.getExpiry(), strike, forward);
    double timeToExpiry = volatilities.relativeTime(underlyingOption.getExpiry());
    double price = BARRIER_PRICER.price(
        todayFx, strike, timeToExpiry, costOfCarry, rateCounter, volatility, underlyingOption.getPutCall().isCall(), barrier);
    if (option.getRebate().isPresent()) {
      CurrencyAmount rebate = option.getRebate().get();
      double priceRebate = rebate.getCurrency().equals(ccyCounter) ?
          CASH_REBATE_PRICER.price(todayFx, timeToExpiry, costOfCarry, rateCounter, volatility, barrier.inverseKnockType()) :
          ASSET_REBATE_PRICER.price(todayFx, timeToExpiry, costOfCarry, rateCounter, volatility, barrier.inverseKnockType());
      price += priceRebate * rebate.getAmount() / Math.abs(underlyingFx.getBaseCurrencyPayment().getAmount());
    }
    return price;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the FX barrier option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation, i.e., sticky-strike.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the product
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyStrike(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    if (volatilities.relativeTime(underlyingOption.getExpiry()) <= 0d) {
      return PointSensitivityBuilder.none();
    }
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double signedNotional = signedNotional(underlyingOption);
    double counterYearFraction =
        ratesProvider.discountFactors(currencyPair.getCounter()).relativeYearFraction(underlyingFx.getPaymentDate());
    ZeroRateSensitivity counterSensi = ZeroRateSensitivity.of(
        currencyPair.getCounter(),
        counterYearFraction,
        signedNotional * (priceDerivatives.getDerivative(2) + priceDerivatives.getDerivative(3)));
    double baseYearFraction =
        ratesProvider.discountFactors(currencyPair.getBase()).relativeYearFraction(underlyingFx.getPaymentDate());
    ZeroRateSensitivity baseSensi = ZeroRateSensitivity.of(
        currencyPair.getBase(),
        baseYearFraction,
        currencyPair.getCounter(),
        -priceDerivatives.getDerivative(3) * signedNotional);
    return counterSensi.combinedWith(baseSensi);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the FX barrier option product.
   * <p>
   * The present value delta is the first derivative of {@link #presentValue} with respect to spot.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value delta of the product
   */
  public CurrencyAmount presentValueDelta(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double delta = delta(option, ratesProvider, volatilities);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    return CurrencyAmount.of(underlyingOption.getCounterCurrency(), signedNotional(underlyingOption) * delta);
  }

  /**
   * Calculates the delta of the FX barrier option product.
   * <p>
   * The delta is the first derivative of {@link #price} with respect to spot.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the delta of the product
   */
  public double delta(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    if (volatilities.relativeTime(option.getUnderlyingOption().getExpiry()) < 0d) {
      return 0d;
    }
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    return priceDerivatives.getDerivative(0);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value gamma of the FX barrier option product.
   * <p>
   * The present value gamma is the second derivative of {@link #presentValue} with respect to spot.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value gamma of the product
   */
  public CurrencyAmount presentValueGamma(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double gamma = gamma(option, ratesProvider, volatilities);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    return CurrencyAmount.of(underlyingOption.getCounterCurrency(), signedNotional(underlyingOption) * gamma);
  }

  /**
   * Calculates the gamma of the FX barrier option product.
   * <p>
   * The delta is the second derivative of {@link #price} with respect to spot.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the gamma of the product
   */
  public double gamma(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    return priceDerivatives.getDerivative(6);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used. This is also called Black vega.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsVolatility(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    if (volatilities.relativeTime(underlyingOption.getExpiry()) <= 0d) {
      return PointSensitivityBuilder.none();
    }
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    Currency ccyBase = currencyPair.getBase();
    Currency ccyCounter = currencyPair.getCounter();
    double dfBase = ratesProvider.discountFactor(ccyBase, underlyingFx.getPaymentDate());
    double dfCounter = ratesProvider.discountFactor(ccyCounter, underlyingFx.getPaymentDate());
    double todayFx = ratesProvider.fxRate(currencyPair);
    double forward = todayFx * dfBase / dfCounter;
    return FxOptionSensitivity.of(
        volatilities.getName(),
        currencyPair,
        volatilities.relativeTime(underlyingOption.getExpiry()),
        underlyingOption.getStrike(),
        forward,
        ccyCounter,
        priceDerivatives.getDerivative(4) * signedNotional(underlyingOption));
  }

  /**
   * Calculates the vega of the FX barrier option product.
   * <p>
   * The delta is the first derivative of {@link #price} with respect to Black volatility.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the vega of the product
   */
  public double vega(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    return priceDerivatives.getDerivative(4);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value theta of the FX barrier option product.
   * <p>
   * The present value theta is the negative of the first derivative of {@link #presentValue} with time parameter.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value theta of the product
   */
  public CurrencyAmount presentValueTheta(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double theta = theta(option, ratesProvider, volatilities);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    return CurrencyAmount.of(underlyingOption.getCounterCurrency(), signedNotional(underlyingOption) * theta);
  }

  /**
   * Calculates the theta of the FX barrier option product.
   * <p>
   * The theta is the negative of the first derivative of {@link #price} with respect to time parameter.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the theta of the product
   */
  public double theta(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    return -priceDerivatives.getDerivative(5);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the FX barrier option product.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    if (volatilities.relativeTime(underlyingOption.getExpiry()) < 0d) {
      return MultiCurrencyAmount.empty();
    }
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    double price = priceDerivatives.getValue();
    double delta = priceDerivatives.getDerivative(0);
    CurrencyPair currencyPair = underlyingOption.getUnderlying().getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    double signedNotional = signedNotional(underlyingOption);
    CurrencyAmount domestic = CurrencyAmount.of(currencyPair.getCounter(), (price - delta * todayFx) * signedNotional);
    CurrencyAmount foreign = CurrencyAmount.of(currencyPair.getBase(), delta * signedNotional);
    return MultiCurrencyAmount.of(domestic, foreign);
  }

  //-------------------------------------------------------------------------
  //  The derivatives are [0] spot, [1] strike, [2] rate, [3] cost-of-carry, [4] volatility, [5] timeToExpiry, [6] spot twice
  private ValueDerivatives priceDerivatives(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    validate(option, ratesProvider, volatilities);
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    double[] derivatives = new double[7];
    if (volatilities.relativeTime(underlyingOption.getExpiry()) < 0d) {
      return ValueDerivatives.of(0d, DoubleArray.ofUnsafe(derivatives));
    }
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    Currency ccyBase = currencyPair.getBase();
    Currency ccyCounter = currencyPair.getCounter();
    DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);

    double rateBase = baseDiscountFactors.zeroRate(underlyingFx.getPaymentDate());
    double rateCounter = counterDiscountFactors.zeroRate(underlyingFx.getPaymentDate());
    double costOfCarry = rateCounter - rateBase;
    double dfBase = baseDiscountFactors.discountFactor(underlyingFx.getPaymentDate());
    double dfCounter = counterDiscountFactors.discountFactor(underlyingFx.getPaymentDate());
    double todayFx = ratesProvider.fxRate(currencyPair);
    double strike = underlyingOption.getStrike();
    double forward = todayFx * dfBase / dfCounter;
    double volatility = volatilities.volatility(currencyPair, underlyingOption.getExpiry(), strike, forward);
    double timeToExpiry = volatilities.relativeTime(underlyingOption.getExpiry());
    ValueDerivatives valueDerivatives = BARRIER_PRICER.priceAdjoint(
        todayFx, strike, timeToExpiry, costOfCarry, rateCounter, volatility, underlyingOption.getPutCall().isCall(), barrier);
    if (!option.getRebate().isPresent()) {
      return valueDerivatives;
    }
    CurrencyAmount rebate = option.getRebate().get();
    ValueDerivatives valueDerivativesRebate = rebate.getCurrency().equals(ccyCounter) ?
        CASH_REBATE_PRICER.priceAdjoint(todayFx, timeToExpiry, costOfCarry, rateCounter, volatility, barrier.inverseKnockType()) :
        ASSET_REBATE_PRICER.priceAdjoint(todayFx, timeToExpiry, costOfCarry, rateCounter, volatility, barrier.inverseKnockType());
    double rebateRate = rebate.getAmount() / Math.abs(underlyingFx.getBaseCurrencyPayment().getAmount());
    double price = valueDerivatives.getValue() + rebateRate * valueDerivativesRebate.getValue();
    derivatives[0] = valueDerivatives.getDerivative(0) + rebateRate * valueDerivativesRebate.getDerivative(0);
    derivatives[1] = valueDerivatives.getDerivative(1);
    for (int i = 2; i < 7; ++i) {
      derivatives[i] = valueDerivatives.getDerivative(i) + rebateRate * valueDerivativesRebate.getDerivative(i - 1);
    }
    return ValueDerivatives.of(price, DoubleArray.ofUnsafe(derivatives));
  }

  //-------------------------------------------------------------------------
  private void validate(ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ArgChecker.isTrue(option.getBarrier() instanceof SimpleConstantContinuousBarrier,
        "Barrier should be SimpleConstantContinuousBarrier");
    ArgChecker.isTrue(ratesProvider.getValuationDate().isEqual(volatilities.getValuationDateTime().toLocalDate()),
        "Volatility and rate data must be for the same date");
  }

  // signed notional amount to computed present value and value Greeks
  private double signedNotional(ResolvedFxVanillaOption option) {
    return (option.getLongShort().isLong() ? 1d : -1d) *
        Math.abs(option.getUnderlying().getBaseCurrencyPayment().getAmount());
  }

}
