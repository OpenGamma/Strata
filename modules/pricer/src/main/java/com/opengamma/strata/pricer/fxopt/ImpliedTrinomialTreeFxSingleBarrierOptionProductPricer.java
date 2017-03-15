/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.impl.tree.ConstantContinuousSingleBarrierKnockoutFunction;
import com.opengamma.strata.pricer.impl.tree.EuropeanVanillaOptionFunction;
import com.opengamma.strata.pricer.impl.tree.TrinomialTree;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Pricer for FX barrier option products under implied trinomial tree.
 * <p>
 * This function provides the ability to price an {@link ResolvedFxSingleBarrierOption}.
 * <p>
 * All of the computation is be based on the counter currency of the underlying FX transaction.
 * For example, price, PV and risk measures of the product will be expressed in USD for an option on EUR/USD.
 */
public class ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer {

  /**
   * The trinomial tree.
   */
  private static final TrinomialTree TREE = new TrinomialTree();
  /**
   * Small parameter.
   */
  private static final double SMALL = 1.0e-12;
  /**
   * Default number of time steps.
   */
  private static final int NUM_STEPS_DEFAULT = 51;

  /**
   * Default implementation.
   */
  public static final ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer DEFAULT =
      new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(NUM_STEPS_DEFAULT);

  /**
   * Number of time steps.
   */
  private final ImpliedTrinomialTreeFxOptionCalibrator calibrator;

  /**
   * Pricer with the default number of time steps.
   */
  public ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer() {
    this(NUM_STEPS_DEFAULT);
  }

  /**
   * Pricer with the specified number of time steps.
   * 
   * @param nSteps  number of time steps
   */
  public ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(int nSteps) {
    this.calibrator = new ImpliedTrinomialTreeFxOptionCalibrator(nSteps);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the calibrator.
   * 
   * @return the calibrator
   */
  public ImpliedTrinomialTreeFxOptionCalibrator getCalibrator() {
    return calibrator;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the FX barrier option product.
   * <p>
   * The price of the product is the value on the valuation date for one unit of the base currency 
   * and is expressed in the counter currency. The price does not take into account the long/short flag.
   * See {@linkplain #presentValue(ResolvedFxSingleBarrierOption, RatesProvider, BlackFxOptionVolatilities) presentValue} 
   * for scaling and currency.
   * <p>
   * The trinomial tree is first calibrated to Black volatilities, 
   * then the price is computed based on the calibrated tree.
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

    RecombiningTrinomialTreeData treeData =
        calibrator.calibrateTrinomialTree(option.getUnderlyingOption(), ratesProvider, volatilities);
    return price(option, ratesProvider, volatilities, treeData);
  }

  /**
   * Calculates the price of the FX barrier option product.
   * <p>
   * The price of the product is the value on the valuation date for one unit of the base currency 
   * and is expressed in the counter currency. The price does not take into account the long/short flag.
   * See {@linkplain #presentValue(ResolvedFxSingleBarrierOption, RatesProvider, BlackFxOptionVolatilities, RecombiningTrinomialTreeData) presnetValue} 
   * for scaling and currency.
   * <p>
   * This assumes the tree is already calibrated and the tree data is stored as {@code RecombiningTrinomialTreeData}.
   * The tree data should be consistent with the pricer and other inputs, see {@link #validateData}.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @param treeData  the trinomial tree data
   * @return the price of the product
   */
  public double price(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData treeData) {

    return priceDerivatives(option, ratesProvider, volatilities, treeData).getValue();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FX barrier option product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * It is expressed in the counter currency.
   * <p>
   * The trinomial tree is first calibrated to Black volatilities, 
   * then the price is computed based on the calibrated tree.
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

    RecombiningTrinomialTreeData treeData =
        calibrator.calibrateTrinomialTree(option.getUnderlyingOption(), ratesProvider, volatilities);
    return presentValue(option, ratesProvider, volatilities, treeData);
  }

  /**
   * Calculates the present value of the FX barrier option product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * It is expressed in the counter currency.
   * <p>
   * This assumes the tree is already calibrated and the tree data is stored as {@code RecombiningTrinomialTreeData}.
   * The tree data should be consistent with the pricer and other inputs, see {@link #validateData}.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @param treeData  the trinomial tree data
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData treeData) {

    double price = price(option, ratesProvider, volatilities, treeData);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    return CurrencyAmount.of(underlyingOption.getCounterCurrency(), signedNotional(underlyingOption) * price);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the FX barrier option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curve parameters.
   * <p>
   * The sensitivity is computed by bump and re-price.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the product
   * @deprecated Use presentValueSensitivityRates
   */
  @Deprecated
  public CurrencyParameterSensitivities presentValueRatesSensitivity(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    return presentValueSensitivityRates(option, ratesProvider, volatilities);
  }

  /**
   * Calculates the present value sensitivity of the FX barrier option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curve parameters.
   * <p>
   * The sensitivity is computed by bump and re-price.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @param baseTreeData  the trinomial tree data
   * @return the present value of the product
   * @deprecated Use presentValueSensitivityRates
   */
  @Deprecated
  public CurrencyParameterSensitivities presentValueRatesSensitivity(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData baseTreeData) {

    return presentValueSensitivityRates(option, ratesProvider, volatilities, baseTreeData);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the FX barrier option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curve parameters.
   * <p>
   * The sensitivity is computed by bump and re-price.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the product
   */
  public CurrencyParameterSensitivities presentValueSensitivityRates(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    RecombiningTrinomialTreeData baseTreeData =
        calibrator.calibrateTrinomialTree(option.getUnderlyingOption(), ratesProvider, volatilities);
    return presentValueSensitivityRates(option, ratesProvider, volatilities, baseTreeData);
  }

  /**
   * Calculates the present value sensitivity of the FX barrier option product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of {@link #presentValue} to
   * the underlying curve parameters.
   * <p>
   * The sensitivity is computed by bump and re-price.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @param baseTreeData  the trinomial tree data
   * @return the present value of the product
   */
  public CurrencyParameterSensitivities presentValueSensitivityRates(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData baseTreeData) {

    ArgChecker.isTrue(baseTreeData.getNumberOfSteps() == calibrator.getNumberOfSteps(),
        "the number of steps mismatch between pricer and trinomial tree data");
    double shift = 1.0e-5;
    CurrencyAmount pvBase = presentValue(option, ratesProvider, volatilities, baseTreeData);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    ImmutableRatesProvider immRatesProvider = ratesProvider.toImmutableRatesProvider();
    ImmutableMap<Currency, Curve> baseCurves = immRatesProvider.getDiscountCurves();
    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();

    for (Entry<Currency, Curve> entry : baseCurves.entrySet()) {
      if (currencyPair.contains(entry.getKey())) {
        Curve curve = entry.getValue();
        int nParams = curve.getParameterCount();
        DoubleArray sensitivity = DoubleArray.of(nParams, i -> {
          Curve dscBumped = curve.withParameter(i, curve.getParameter(i) + shift);
          Map<Currency, Curve> mapBumped = new HashMap<>(baseCurves);
          mapBumped.put(entry.getKey(), dscBumped);
          ImmutableRatesProvider providerDscBumped = immRatesProvider.toBuilder().discountCurves(mapBumped).build();
          double pvBumped = presentValue(option, providerDscBumped, volatilities).getAmount();
          return (pvBumped - pvBase.getAmount()) / shift;
        });
        result = result.combinedWith(curve.createParameterSensitivity(pvBase.getCurrency(), sensitivity));
      }
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the FX barrier option product.
   * <p>
   * The trinomial tree is first calibrated to Black volatilities, 
   * then the price is computed based on the calibrated tree.
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

    RecombiningTrinomialTreeData treeData =
        calibrator.calibrateTrinomialTree(option.getUnderlyingOption(), ratesProvider, volatilities);
    return currencyExposure(option, ratesProvider, volatilities, treeData);
  }

  /**
   * Calculates the currency exposure of the FX barrier option product.
   * <p>
   * This assumes the tree is already calibrated and the tree data is stored as {@code RecombiningTrinomialTreeData}.
   * The tree data should be consistent with the pricer and other inputs, see {@link #validateData}.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @param treeData  the trinomial tree data
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData treeData) {

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilities, treeData);
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
  private ValueDerivatives priceDerivatives(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData data) {

    validate(option, ratesProvider, volatilities);
    validateData(option, ratesProvider, volatilities, data);
    int nSteps = data.getNumberOfSteps();
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    double timeToExpiry = data.getTime(nSteps);
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    Currency ccyBase = underlyingFx.getCounterCurrencyPayment().getCurrency();
    Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
    DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);
    double rebateAtExpiry = 0d; // used to price knock-in option
    double rebateAtExpiryDerivative = 0d; // used to price knock-in option
    double notional = Math.abs(underlyingFx.getBaseCurrencyPayment().getAmount());
    double[] rebateArray = new double[nSteps + 1];
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    if (option.getRebate().isPresent()) {
      CurrencyAmount rebateCurrencyAmount = option.getRebate().get();
      double rebatePerUnit = rebateCurrencyAmount.getAmount() / notional;
      boolean isCounter = rebateCurrencyAmount.getCurrency().equals(ccyCounter);
      double rebate = isCounter ? rebatePerUnit : rebatePerUnit * barrier.getBarrierLevel();
      if (barrier.getKnockType().isKnockIn()) { // use in-out parity
        double dfCounterAtExpiry = counterDiscountFactors.discountFactor(timeToExpiry);
        double dfBaseAtExpiry = baseDiscountFactors.discountFactor(timeToExpiry);
        for (int i = 0; i < nSteps + 1; ++i) {
          rebateArray[i] = isCounter ?
              rebate * dfCounterAtExpiry / counterDiscountFactors.discountFactor(data.getTime(i)) :
              rebate * dfBaseAtExpiry / baseDiscountFactors.discountFactor(data.getTime(i));
        }
        if (isCounter) {
          rebateAtExpiry = rebatePerUnit * dfCounterAtExpiry;
        } else {
          rebateAtExpiry = rebatePerUnit * data.getSpot() * dfBaseAtExpiry;
          rebateAtExpiryDerivative = rebatePerUnit * dfBaseAtExpiry;
        }
      } else {
        Arrays.fill(rebateArray, rebate);
      }
    }
    ConstantContinuousSingleBarrierKnockoutFunction barrierFunction = ConstantContinuousSingleBarrierKnockoutFunction.of(
        underlyingOption.getStrike(),
        timeToExpiry,
        underlyingOption.getPutCall(),
        nSteps,
        barrier.getBarrierType(),
        barrier.getBarrierLevel(),
        DoubleArray.ofUnsafe(rebateArray));
    ValueDerivatives barrierPrice = TREE.optionPriceAdjoint(barrierFunction, data);
    if (barrier.getKnockType().isKnockIn()) {  // use in-out parity
      EuropeanVanillaOptionFunction vanillaFunction = EuropeanVanillaOptionFunction.of(
          underlyingOption.getStrike(), timeToExpiry, underlyingOption.getPutCall(), nSteps);
      ValueDerivatives vanillaPrice = TREE.optionPriceAdjoint(vanillaFunction, data);
      return ValueDerivatives.of(vanillaPrice.getValue() + rebateAtExpiry - barrierPrice.getValue(),
          DoubleArray.of(vanillaPrice.getDerivative(0) + rebateAtExpiryDerivative - barrierPrice.getDerivative(0)));
    }
    return barrierPrice;
  }

  //-------------------------------------------------------------------------
  private void validateData(ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData data) {

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    ArgChecker.isTrue(DoubleMath.fuzzyEquals(data.getTime(data.getNumberOfSteps()),
        volatilities.relativeTime(underlyingOption.getExpiry()), SMALL),
        "time to expiry mismatch between pricing option and trinomial tree data");
    ArgChecker.isTrue(DoubleMath.fuzzyEquals(data.getSpot(),
        ratesProvider.fxRate(underlyingOption.getUnderlying().getCurrencyPair()), SMALL),
        "today's FX rate mismatch between rates provider and trinomial tree data");
  }

  private void validate(ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ArgChecker.isTrue(option.getBarrier() instanceof SimpleConstantContinuousBarrier,
        "barrier should be SimpleConstantContinuousBarrier");
    ArgChecker.isTrue(
        ratesProvider.getValuationDate().isEqual(volatilities.getValuationDateTime().toLocalDate()),
        "Volatility and rate data must be for the same date");
  }

  // signed notional amount to computed present value and value Greeks
  private double signedNotional(ResolvedFxVanillaOption option) {
    return (option.getLongShort().isLong() ? 1d : -1d) *
        Math.abs(option.getUnderlying().getBaseCurrencyPayment().getAmount());
  }

}
