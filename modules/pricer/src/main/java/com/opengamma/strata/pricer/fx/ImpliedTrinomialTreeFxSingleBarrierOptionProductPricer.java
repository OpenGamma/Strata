/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.view.DiscountFactors;
import com.opengamma.strata.pricer.impl.tree.ConstantContinuousSingleBarrierKnockoutFunction;
import com.opengamma.strata.pricer.impl.tree.EuropeanVanillaOptionFunction;
import com.opengamma.strata.pricer.impl.tree.RecombiningTrinomialTreeData;
import com.opengamma.strata.pricer.impl.tree.TrinomialTree;
import com.opengamma.strata.pricer.impl.volatility.local.ImpliedTrinomialTreeLocalVolatilityCalculator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fx.ResolvedFxVanillaOption;
import com.opengamma.strata.product.fx.SimpleConstantContinuousBarrier;

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
   * Number of time steps.
   */
  private final int nSteps;

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
    ArgChecker.isTrue(nSteps > 1, "the number of steps should be greater than 1");
    this.nSteps = nSteps;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the FX barrier option product.
   * <p>
   * The price of the product is the value on the valuation date for one unit of the base currency 
   * and is expressed in the counter currency. The price does not take into account the long/short flag. 
   * See {@linkplain #presentValue(ResolvedFxSingleBarrierOption, RatesProvider, BlackVolatilityFxProvider) presentValue} 
   * for scaling and currency.
   * <p>
   * The trinomial tree is first calibrated to Black volatilities, 
   * then the price is computed based on the calibrated tree.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the price of the product
   */
  public double price(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    RecombiningTrinomialTreeData treeData = calibrateTrinomialTree(option, ratesProvider, volatilityProvider);
    return price(option, ratesProvider, volatilityProvider, treeData);
  }

  /**
   * Calculates the price of the FX barrier option product.
   * <p>
   * The price of the product is the value on the valuation date for one unit of the base currency 
   * and is expressed in the counter currency. The price does not take into account the long/short flag. 
   * See {@linkplain #presentValue(ResolvedFxSingleBarrierOption, RatesProvider, BlackVolatilityFxProvider, RecombiningTrinomialTreeData) presnetValue} 
   * for scaling and currency.
   * <p>
   * This assumes the tree is already calibrated and the tree data is stored as {@code RecombiningTrinomialTreeData}.
   * The tree data should be consistent with the pricer and other inputs, see {@link #validateData}.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @param treeData  the trinomial tree data
   * @return the price of the product
   */
  public double price(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      RecombiningTrinomialTreeData treeData) {

    return priceDerivatives(option, ratesProvider, volatilityProvider, treeData).getValue();
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
   * @param volatilityProvider  the Black volatility provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    RecombiningTrinomialTreeData data = calibrateTrinomialTree(option, ratesProvider, volatilityProvider);
    return presentValue(option, ratesProvider, volatilityProvider, data);
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
   * @param volatilityProvider  the Black volatility provider
   * @param treeData  the trinomial tree data
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      RecombiningTrinomialTreeData treeData) {

    double price = price(option, ratesProvider, volatilityProvider, treeData);
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
   * @param volatilityProvider  the Black volatility provider
   * @return the present value of the product
   */
  public CurveCurrencyParameterSensitivities presentValueCurveParameterSensitivity(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    RecombiningTrinomialTreeData baseData = calibrateTrinomialTree(option, ratesProvider, volatilityProvider);
    return presentValueCurveParameterSensitivity(option, ratesProvider, volatilityProvider, baseData);
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
   * @param volatilityProvider  the Black volatility provider
   * @param baseTreeData  the trinomial tree data
   * @return the present value of the product
   */
  public CurveCurrencyParameterSensitivities presentValueCurveParameterSensitivity(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      RecombiningTrinomialTreeData baseTreeData) {

    double shift = 1.0e-5;
    CurrencyAmount pvBase = presentValue(option, ratesProvider, volatilityProvider, baseTreeData);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    ImmutableRatesProvider immRatesProvider = (ImmutableRatesProvider) ratesProvider;
    ImmutableMap<Currency, Curve> baseCurves = immRatesProvider.getDiscountCurves();
    CurveCurrencyParameterSensitivities result = CurveCurrencyParameterSensitivities.empty();

    for (Entry<Currency, Curve> entry : baseCurves.entrySet()) {
      if (currencyPair.contains(entry.getKey())) {
        NodalCurve nodalCurve = entry.getValue().toNodalCurve();
        int nParams = nodalCurve.getXValues().size();
        DoubleArray sensitivity = DoubleArray.of(nParams, i -> {
          Curve dscBumped = bumpedCurve(nodalCurve, shift, i);
          Map<Currency, Curve> mapBumped = new HashMap<>(baseCurves);
          mapBumped.put(entry.getKey(), dscBumped);
          ImmutableRatesProvider providerDscBumped = immRatesProvider.toBuilder().discountCurves(mapBumped).build();
          double pvBumped = presentValue(option, providerDscBumped, volatilityProvider).getAmount();
          return (pvBumped - pvBase.getAmount()) / shift;
        });
        CurveMetadata metadata = entry.getValue().getMetadata();
        result = result.combinedWith(CurveCurrencyParameterSensitivity.of(metadata, pvBase.getCurrency(), sensitivity));
      }
    }
    return result;
  }

  private NodalCurve bumpedCurve(NodalCurve curveInt, double shift, int loopnode) {
    DoubleArray yValues = curveInt.getYValues();
    return curveInt.withYValues(yValues.with(loopnode, yValues.get(loopnode) + shift));
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
   * @param volatilityProvider  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    RecombiningTrinomialTreeData data = calibrateTrinomialTree(option, ratesProvider, volatilityProvider);
    return currencyExposure(option, ratesProvider, volatilityProvider, data);
  }

  /**
   * Calculates the currency exposure of the FX barrier option product.
   * <p>
   * This assumes the tree is already calibrated and the tree data is stored as {@code RecombiningTrinomialTreeData}.
   * The tree data should be consistent with the pricer and other inputs, see {@link #validateData}.
   * 
   * @param option  the option product
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @param treeData  the trinomial tree data
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      RecombiningTrinomialTreeData treeData) {

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    ValueDerivatives priceDerivatives = priceDerivatives(option, ratesProvider, volatilityProvider, treeData);
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
  /**
   * Calibrate trinomial tree to Black volatilities. 
   * <p>
   * The calibration procedure is in principle independent of the option to price. 
   * However, {@code ResolvedFxSingleBarrierOption} is plugged in to ensure that the grid points properly cover 
   * the lifetime of the target option. 
   * 
   * @param option  the option
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the Black volatility provider
   * @return the trinomial tree data
   */
  public RecombiningTrinomialTreeData calibrateTrinomialTree(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    validate(option, ratesProvider, volatilityProvider);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    if (volatilityProvider.relativeTime(underlyingOption.getExpiry()) <= 0d) {
      throw new IllegalArgumentException("option expired");
    }
    double timeToExpiry = volatilityProvider.relativeTime(underlyingOption.getExpiry());
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    Currency ccyBase = underlyingFx.getBaseCurrencyPayment().getCurrency();
    Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
    double todayFx = ratesProvider.fxRate(currencyPair);
    DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);

    Function<Double, Double> interestRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double t) {
        return counterDiscountFactors.zeroRate(t);
      }
    };
    Function<Double, Double> dividendRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double t) {
        return baseDiscountFactors.zeroRate(t);
      }
    };
    Function<DoublesPair, Double> impliedVolSurface = new Function<DoublesPair, Double>() {
      @Override
      public Double apply(DoublesPair tk) {
        double dfBase = baseDiscountFactors.discountFactor(tk.getFirst());
        double dfCounter = counterDiscountFactors.discountFactor(tk.getFirst());
        double forward = todayFx * dfBase / dfCounter;
        return volatilityProvider.getVolatility(currencyPair, tk.getFirst(), tk.getSecond(), forward);
      }
    };
    ImpliedTrinomialTreeLocalVolatilityCalculator localVol =
        new ImpliedTrinomialTreeLocalVolatilityCalculator(nSteps, timeToExpiry);
    return localVol.calibrateImpliedVolatility(impliedVolSurface, todayFx, interestRate, dividendRate);
  }

  //-------------------------------------------------------------------------
  private ValueDerivatives priceDerivatives(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      RecombiningTrinomialTreeData data) {

    validate(option, ratesProvider, volatilityProvider);
    validateData(option, ratesProvider, volatilityProvider, data);
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
      if (barrier.getKnockType().isKnockIn()) {
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
    if (barrier.getKnockType().isKnockIn()) {
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
      BlackVolatilityFxProvider volatilityProvider,
      RecombiningTrinomialTreeData data) {
    ArgChecker.isTrue(data.getNumberOfSteps() == nSteps,
        "the number of steps mismatch between pricer and trinomial tree data");
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    ArgChecker.isTrue(DoubleMath.fuzzyEquals(data.getTime(nSteps),
            volatilityProvider.relativeTime(underlyingOption.getExpiry()), SMALL),
        "time to expiry mismatch between pricing option and trinomial tree data");
    ArgChecker.isTrue(DoubleMath.fuzzyEquals(data.getSpot(),
        ratesProvider.fxRate(underlyingOption.getUnderlying().getCurrencyPair()), SMALL),
        "today's FX rate mismatch between rates provider and trinomial tree data");
  }

  private void validate(ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    ArgChecker.isTrue(option.getBarrier() instanceof SimpleConstantContinuousBarrier,
        "barrier should be SimpleConstantContinuousBarrier");
    ArgChecker.isTrue(
        ratesProvider.getValuationDate().isEqual(volatilityProvider.getValuationDateTime().toLocalDate()),
        "Volatility and rate data must be for the same date");
  }

  // signed notional amount to computed present value and value Greeks
  private double signedNotional(ResolvedFxVanillaOption option) {
    return (option.getLongShort().isLong() ? 1d : -1d) *
        Math.abs(option.getUnderlying().getBaseCurrencyPayment().getAmount());
  }

}
