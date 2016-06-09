/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.joda.beans.MetaProperty;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.PriceIndexValues;
import com.opengamma.strata.pricer.rate.SimplePriceIndexValues;

/**
 * Computes the curve parameter sensitivity by finite difference.
 * <p>
 * This is based on an {@link ImmutableRatesProvider} or {@link LegalEntityDiscountingProvider}, 
 * and calculates the sensitivity by finite difference.
 */
public class RatesFiniteDifferenceSensitivityCalculator {

  /**
   * Default implementation. The shift is one basis point (0.0001).
   */
  public static final RatesFiniteDifferenceSensitivityCalculator DEFAULT =
      new RatesFiniteDifferenceSensitivityCalculator(1.0E-4);

  /**
   * The shift used for finite difference.
   */
  private final double shift;

  /**
   * Create an instance of the finite difference calculator.
   * 
   * @param shift  the shift used in the finite difference computation
   */
  public RatesFiniteDifferenceSensitivityCalculator(double shift) {
    this.shift = shift;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the first order sensitivities of a function of a RatesProvider to a double by finite difference.
   * <p>
   * The finite difference is computed by forward type. 
   * The function should return a value in the same currency for any rate provider.
   * 
   * @param provider  the rates provider
   * @param valueFn  the function from a rate provider to a currency amount for which the sensitivity should be computed
   * @return the curve sensitivity
   */
  public CurrencyParameterSensitivities sensitivity(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn) {

    CurrencyAmount valueInit = valueFn.apply(provider);
    CurrencyParameterSensitivities discounting = sensitivity(
        provider,
        provider.getDiscountCurves(),
        (base, bumped) -> base.toBuilder().discountCurves(bumped).build(),
        valueFn,
        valueInit);
    CurrencyParameterSensitivities forward = sensitivity(
        provider,
        provider.getIndexCurves(),
        (base, bumped) -> base.toBuilder().indexCurves(bumped).build(),
        valueFn,
        valueInit);
    CurrencyParameterSensitivities priceIndex = sensitivityPriceIndex(
        provider,
        provider.getPriceIndexValues(),
        (base, bumped) -> base.toBuilder().priceIndexValues(bumped).build(),
        valueFn,
        valueInit);
    return discounting.combinedWith(forward).combinedWith(priceIndex);
  }

  // computes the sensitivity with respect to the curves
  private <T> CurrencyParameterSensitivities sensitivity(
      ImmutableRatesProvider provider,
      Map<T, Curve> baseCurves,
      BiFunction<ImmutableRatesProvider, Map<T, Curve>, ImmutableRatesProvider> storeBumpedFn,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      CurrencyAmount valueInit) {

    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (Entry<T, Curve> entry : baseCurves.entrySet()) {
      Curve curve = entry.getValue();
      DoubleArray sensitivity = DoubleArray.of(curve.getParameterCount(), i -> {
        Curve dscBumped = curve.withParameter(i, curve.getParameter(i) + shift);
        Map<T, Curve> mapBumped = new HashMap<>(baseCurves);
        mapBumped.put(entry.getKey(), dscBumped);
        ImmutableRatesProvider providerDscBumped = storeBumpedFn.apply(provider, mapBumped);
        return (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      });
      result = result.combinedWith(curve.createParameterSensitivity(valueInit.getCurrency(), sensitivity));
    }
    return result;
  }

  // computes the sensitivity with respect to the price index curves
  private <T> CurrencyParameterSensitivities sensitivityPriceIndex(
      ImmutableRatesProvider provider,
      Map<PriceIndex, PriceIndexValues> indexValues,
      BiFunction<ImmutableRatesProvider, Map<PriceIndex, PriceIndexValues>, ImmutableRatesProvider> storeBumpedFn,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      CurrencyAmount valueInit) {

    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (Entry<PriceIndex, PriceIndexValues> entry : indexValues.entrySet()) {
      SimplePriceIndexValues indexValue = ((SimplePriceIndexValues) entry.getValue());
      Curve curve = indexValue.getCurve();
      DoubleArray sensitivity = DoubleArray.of(curve.getParameterCount(), i -> {
        Curve dscBumped = curve.withParameter(i, curve.getParameter(i) + shift);
        Map<PriceIndex, PriceIndexValues> mapBumped = new HashMap<>(indexValues);
        mapBumped.put(entry.getKey(), indexValue.withCurve((InterpolatedNodalCurve) dscBumped));
        ImmutableRatesProvider providerDscBumped = storeBumpedFn.apply(provider, mapBumped);
        return (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      });
      result = result.combinedWith(curve.createParameterSensitivity(valueInit.getCurrency(), sensitivity));
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the first order sensitivities of a function of a LegalEntityDiscountingProvider to a double by finite difference.
   * <p>
   * The finite difference is computed by forward type. 
   * The function should return a value in the same currency for any rates provider of LegalEntityDiscountingProvider.
   * 
   * @param provider  the rates provider
   * @param valueFn  the function from a rate provider to a currency amount for which the sensitivity should be computed
   * @return the curve sensitivity
   */
  public CurrencyParameterSensitivities sensitivity(
      LegalEntityDiscountingProvider provider,
      Function<LegalEntityDiscountingProvider, CurrencyAmount> valueFn) {

    CurrencyAmount valueInit = valueFn.apply(provider);
    CurrencyParameterSensitivities discounting = sensitivity(
        provider, valueFn, LegalEntityDiscountingProvider.meta().repoCurves(), valueInit);
    CurrencyParameterSensitivities forward = sensitivity(
        provider, valueFn, LegalEntityDiscountingProvider.meta().issuerCurves(), valueInit);
    return discounting.combinedWith(forward);
  }

  private <T> CurrencyParameterSensitivities sensitivity(
      LegalEntityDiscountingProvider provider,
      Function<LegalEntityDiscountingProvider, CurrencyAmount> valueFn,
      MetaProperty<ImmutableMap<Pair<T, Currency>, DiscountFactors>> metaProperty,
      CurrencyAmount valueInit) {

    ImmutableMap<Pair<T, Currency>, DiscountFactors> baseCurves = metaProperty.get(provider);
    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (Pair<T, Currency> key : baseCurves.keySet()) {
      DiscountFactors discountFactors = baseCurves.get(key);
      Curve curve = checkDiscountFactors(discountFactors);
      int paramCount = curve.getParameterCount();
      double[] sensitivity = new double[paramCount];
      for (int i = 0; i < paramCount; i++) {
        Curve dscBumped = curve.withParameter(i, curve.getParameter(i) + shift);
        Map<Pair<T, Currency>, DiscountFactors> mapBumped = new HashMap<>(baseCurves);
        mapBumped.put(key, createDiscountFactors(discountFactors, dscBumped));
        LegalEntityDiscountingProvider providerDscBumped = provider.toBuilder().set(metaProperty, mapBumped).build();
        sensitivity[i] = (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      }
      result = result.combinedWith(
          curve.createParameterSensitivity(valueInit.getCurrency(), DoubleArray.copyOf(sensitivity)));
    }
    return result;
  }

  //-------------------------------------------------------------------------
  // check that the discountFactors is ZeroRateDiscountFactors or SimpleDiscountFactors
  private Curve checkDiscountFactors(DiscountFactors discountFactors) {
    if (discountFactors instanceof ZeroRateDiscountFactors) {
      return ((ZeroRateDiscountFactors) discountFactors).getCurve();
    } else if (discountFactors instanceof SimpleDiscountFactors) {
      return ((SimpleDiscountFactors) discountFactors).getCurve();
    }
    throw new IllegalArgumentException("Not supported");
  }

  // return correct instance of DiscountFactors
  private DiscountFactors createDiscountFactors(DiscountFactors originalDsc, Curve bumpedCurve) {
    if (originalDsc instanceof ZeroRateDiscountFactors) {
      return ZeroRateDiscountFactors.of(originalDsc.getCurrency(), originalDsc.getValuationDate(), bumpedCurve);
    } else if (originalDsc instanceof SimpleDiscountFactors) {
      return SimpleDiscountFactors.of(originalDsc.getCurrency(), originalDsc.getValuationDate(), bumpedCurve);
    }
    throw new IllegalArgumentException("Not supported");
  }

}
