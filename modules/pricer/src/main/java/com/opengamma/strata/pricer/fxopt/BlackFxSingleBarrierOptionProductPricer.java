/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackBarrierPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
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
 * All the computation is based on the counter currency of the underlying FX transaction.
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
   * Pricer for underlying vanilla option.
   */
  private static final BlackFxVanillaOptionProductPricer VANILLA_OPTION_PRICER =
      BlackFxVanillaOptionProductPricer.DEFAULT;

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
    double timeToExpiry = volatilities.relativeTime(underlyingOption.getExpiry());
    if (timeToExpiry < 0d) {
      return 0d;
    }
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    Currency ccyBase = underlyingFx.getBaseCurrencyPayment().getCurrency();
    Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
    if (alreadyTouched(todayFx, barrier)) {
      if (barrier.getKnockType().isKnockIn()) {
        return VANILLA_OPTION_PRICER.price(underlyingOption, ratesProvider, volatilities);
      } else if (option.getRebate().isPresent()) {
        CurrencyAmount rebate = option.getRebate().get();
        DaysAdjustment spotLag = spotAdjustment(currencyPair);
        LocalDate paymentDate = spotLag.adjust(ratesProvider.getValuationDate(), ReferenceData.standard());
        double rebatePrice = rebate.getAmount() * ratesProvider.discountFactor(rebate.getCurrency(), paymentDate) /
            Math.abs(underlyingFx.getBaseCurrencyPayment().getAmount());
        return rebate.getCurrency().equals(ccyCounter) ? rebatePrice : todayFx * rebatePrice;
      }
      return 0d;
    }
    DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);
    double rateBase = baseDiscountFactors.zeroRate(underlyingFx.getPaymentDate());
    double rateCounter = counterDiscountFactors.zeroRate(underlyingFx.getPaymentDate());
    double costOfCarry = rateCounter - rateBase;
    double dfBase = baseDiscountFactors.discountFactor(underlyingFx.getPaymentDate());
    double dfCounter = counterDiscountFactors.discountFactor(underlyingFx.getPaymentDate());
    double strike = underlyingOption.getStrike();
    double forward = todayFx * dfBase / dfCounter;
    double volatility = volatilities.volatility(currencyPair, underlyingOption.getExpiry(), strike, forward);
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
    double timeToExpiry = volatilities.relativeTime(underlyingOption.getExpiry());
    if (timeToExpiry <= 0d) {
      return PointSensitivityBuilder.none();
    }
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    if (alreadyTouched(todayFx, barrier)) {
      if (barrier.getKnockType().isKnockIn()) {
        PointSensitivities underlyingOptionSensitivity = VANILLA_OPTION_PRICER.presentValueSensitivityRatesStickyStrike(
            option.getUnderlyingOption(), ratesProvider, volatilities);
        return PointSensitivityBuilder.of(underlyingOptionSensitivity.getSensitivities());
      } else if (option.getRebate().isPresent()) {
        CurrencyAmount rebate = option.getRebate().get();
        DaysAdjustment spotLag = spotAdjustment(currencyPair);
        LocalDate paymentDate = spotLag.adjust(ratesProvider.getValuationDate(), ReferenceData.standard());
        ZeroRateSensitivity rebaseSensitivity = ratesProvider.discountFactors(rebate.getCurrency())
            .zeroRatePointSensitivity(paymentDate);
        Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
        return rebate.getCurrency().equals(ccyCounter) ?
            rebaseSensitivity.multipliedBy(rebate.getAmount()) :
            rebaseSensitivity.multipliedBy(rebate.getAmount() * todayFx).withCurrency(ccyCounter);
      }
      return PointSensitivityBuilder.none();
    }
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
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

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    if (volatilities.relativeTime(underlyingOption.getExpiry()) < 0d) {
      return 0d;
    }
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    if (alreadyTouched(todayFx, barrier)) {
      if (barrier.getKnockType().isKnockIn()) {
        return VANILLA_OPTION_PRICER.delta(underlyingOption, ratesProvider, volatilities);
      } else if (option.getRebate().isPresent()) {
        Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
        CurrencyAmount rebate = option.getRebate().get();
        if (!rebate.getCurrency().equals(ccyCounter)) {
          DaysAdjustment spotLag = spotAdjustment(currencyPair);
          LocalDate paymentDate = spotLag.adjust(ratesProvider.getValuationDate(), ReferenceData.standard());
          return rebate.getAmount() * ratesProvider.discountFactor(rebate.getCurrency(), paymentDate) /
              Math.abs(underlyingFx.getBaseCurrencyPayment().getAmount());
        }
      }
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

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    if (volatilities.relativeTime(underlyingOption.getExpiry()) <= 0d) {
      return 0d;
    }
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    if (alreadyTouched(todayFx, barrier)) {
      if (barrier.getKnockType().isKnockIn()) {
        return VANILLA_OPTION_PRICER.gamma(underlyingOption, ratesProvider, volatilities);
      }
      return 0d;
    }
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
    double timeToExpiry = volatilities.relativeTime(underlyingOption.getExpiry());
    if (timeToExpiry <= 0d) {
      return PointSensitivityBuilder.none();
    }
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    if (alreadyTouched(todayFx, barrier)) {
      if (barrier.getKnockType().isKnockIn()) {
        return VANILLA_OPTION_PRICER.presentValueSensitivityModelParamsVolatility(
            option.getUnderlyingOption(), ratesProvider, volatilities);
      }
      return PointSensitivityBuilder.none();
    }
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    Currency ccyBase = currencyPair.getBase();
    Currency ccyCounter = currencyPair.getCounter();
    double dfBase = ratesProvider.discountFactor(ccyBase, underlyingFx.getPaymentDate());
    double dfCounter = ratesProvider.discountFactor(ccyCounter, underlyingFx.getPaymentDate());
    double forward = todayFx * dfBase / dfCounter;
    return FxOptionSensitivity.of(
        volatilities.getName(),
        currencyPair,
        timeToExpiry,
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

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    if (volatilities.relativeTime(underlyingOption.getExpiry()) <= 0d) {
      return 0d;
    }
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    if (alreadyTouched(todayFx, barrier)) {
      if (barrier.getKnockType().isKnockIn()) {
        return VANILLA_OPTION_PRICER.vega(underlyingOption, ratesProvider, volatilities);
      }
      return 0d;
    }
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

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    double timeToExpiry = volatilities.relativeTime(underlyingOption.getExpiry());
    if (timeToExpiry <= 0d) {
      return 0d;
    }
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    if (alreadyTouched(todayFx, barrier)) {
      if (barrier.getKnockType().isKnockIn()) {
        Currency ccyBase = underlyingFx.getBaseCurrencyPayment().getCurrency();
        Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
        DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
        DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);
        double rateBase = baseDiscountFactors.zeroRate(underlyingFx.getPaymentDate());
        double rateCounter = counterDiscountFactors.zeroRate(underlyingFx.getPaymentDate());
        double costOfCarry = rateCounter - rateBase;
        double dfBase = baseDiscountFactors.discountFactor(underlyingFx.getPaymentDate());
        double dfCounter = counterDiscountFactors.discountFactor(underlyingFx.getPaymentDate());
        double strike = underlyingOption.getStrike();
        double forward = todayFx * dfBase / dfCounter;
        double volatility = volatilities.volatility(currencyPair, timeToExpiry, strike, forward);
        boolean isCall = underlyingOption.getPutCall().isCall();
        double fwdPrice = BlackFormulaRepository.price(forward, strike, timeToExpiry, volatility, isCall);
        double fwdTheta = BlackFormulaRepository.driftlessTheta(forward, strike, timeToExpiry, volatility);
        double fwdDelta = BlackFormulaRepository.delta(forward, strike, timeToExpiry, volatility, isCall);
        return dfCounter * (fwdTheta + rateCounter * fwdPrice - costOfCarry * forward * fwdDelta);
      } else if (option.getRebate().isPresent()) {
        CurrencyAmount rebate = option.getRebate().get();
        Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
        DaysAdjustment spotLag = spotAdjustment(currencyPair);
        LocalDate paymentDate = spotLag.adjust(ratesProvider.getValuationDate(), ReferenceData.standard());
        DiscountFactors discountFactors = ratesProvider.discountFactors(rebate.getCurrency());
        double rebateTheta = discountFactors.zeroRate(paymentDate) * rebate.getAmount() *
            discountFactors.discountFactor(paymentDate) / Math.abs(underlyingFx.getBaseCurrencyPayment().getAmount());
        return rebate.getCurrency().equals(ccyCounter) ? rebateTheta : todayFx * rebateTheta;
      }
      return 0d;
    }
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    return -priceDerivatives.getDerivative(5);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward exchange rate.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @return the forward rate
   */
  public FxRate forwardFxRate(ResolvedFxSingleBarrierOption option, RatesProvider ratesProvider) {
    CurrencyPair strikePair = option.getCurrencyPair();
    LocalDate paymentDate = option.getUnderlyingOption().getUnderlying().getPaymentDate();
    double forwardRate = ratesProvider.fxForwardRates(strikePair).rate(strikePair.getBase(), paymentDate);
    return FxRate.of(strikePair, forwardRate);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the implied Black volatility of the FX barrier option product.
   *
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the implied volatility of the product
   * @throws IllegalArgumentException if the option has expired
   */
  public double impliedVolatility(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ZonedDateTime expiry = option.getUnderlyingOption().getExpiry();
    double timeToExpiry = volatilities.relativeTime(expiry);
    if (timeToExpiry <= 0d) {
      throw new IllegalArgumentException("valuation is after option's expiry.");
    }
    FxRate forward = forwardFxRate(option, ratesProvider);
    CurrencyPair strikePair = option.getCurrencyPair();
    double strike = option.getUnderlyingOption().getStrike();
    return volatilities.volatility(strikePair, expiry, strike, forward.fxRate(strikePair));
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
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    double todayFx = ratesProvider.fxRate(currencyPair);
    if (alreadyTouched(todayFx, barrier)) {
      if (barrier.getKnockType().isKnockIn()) {
        return VANILLA_OPTION_PRICER.currencyExposure(underlyingOption, ratesProvider, volatilities);
      } else if (option.getRebate().isPresent()) {
        CurrencyAmount rebate = option.getRebate().get();
        DaysAdjustment spotLag = spotAdjustment(currencyPair);
        LocalDate paymentDate = spotLag.adjust(ratesProvider.getValuationDate(), ReferenceData.standard());
        double pv = (option.getUnderlyingOption().getLongShort().isLong() ? 1d : -1d) * rebate.getAmount() *
            ratesProvider.discountFactor(rebate.getCurrency(), paymentDate);
        Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
        double ceAmount = rebate.getCurrency().equals(ccyCounter) ? pv : todayFx * pv;
        return MultiCurrencyAmount.of(ccyCounter, ceAmount);
      }
      return MultiCurrencyAmount.empty();
    }
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities);
    double price = priceDerivatives.getValue();
    double delta = priceDerivatives.getDerivative(0);
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
  // calculate spot lag for rebate
  private DaysAdjustment spotAdjustment(CurrencyPair currencyPair) {
    return FxIndex.extendedEnum().lookupAll().values().stream()
        .filter(index -> index.getCurrencyPair().equals(currencyPair))
        .findFirst()
        .map(FxIndex::getFixingDateOffset)
        .map(adjustment -> adjustment.toBuilder().days(-adjustment.getDays()).build())
        .orElseGet(() -> DaysAdjustment.ofBusinessDays(2, calendarForPair(currencyPair)));
  }

  private HolidayCalendarId calendarForPair(CurrencyPair pair) {
    return pair.toSet().stream()
        .map(currency -> defaultByCurrencyOrNoHolidays(currency))
        .reduce(HolidayCalendarIds.NO_HOLIDAYS, HolidayCalendarId::combinedWith);
  }

  private HolidayCalendarId defaultByCurrencyOrNoHolidays(Currency currency) {
    try {
      return HolidayCalendarId.defaultByCurrency(currency);
    } catch (IllegalArgumentException e) {
      return HolidayCalendarIds.NO_HOLIDAYS;
    }
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

  private boolean alreadyTouched(double fxRate, SimpleConstantContinuousBarrier barrier) {
    if (barrier.getBarrierType().isDown()) {
      return fxRate <= barrier.getBarrierLevel();
    }
    return fxRate >= barrier.getBarrierLevel();
  }

}
