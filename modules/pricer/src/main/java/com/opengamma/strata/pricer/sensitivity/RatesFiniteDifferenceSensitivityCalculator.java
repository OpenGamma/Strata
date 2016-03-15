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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.view.DiscountFactors;
import com.opengamma.strata.market.view.ForwardPriceIndexValues;
import com.opengamma.strata.market.view.PriceIndexValues;
import com.opengamma.strata.market.view.SimpleDiscountFactors;
import com.opengamma.strata.market.view.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;

/**
 * Computes the curve parameter sensitivity by finite difference.
 * <p>
 * This is based on an {@link ImmutableRatesProvider} or {@link LegalEntityDiscountingProvider}, 
 * and calculates the sensitivity by finite difference.
 * The curves underlying the rates provider must be of type {@link NodalCurve}.
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
   * The curves underlying the rates provider must be convertible to a {@link NodalCurve}.
   * The finite difference is computed by forward type. 
   * The function should return a value in the same currency for any rate provider.
   * 
   * @param provider  the rates provider
   * @param valueFn  the function from a rate provider to a currency amount for which the sensitivity should be computed
   * @return the curve sensitivity
   */
  public CurveCurrencyParameterSensitivities sensitivity(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn) {

    CurrencyAmount valueInit = valueFn.apply(provider);
    CurveCurrencyParameterSensitivities discounting = sensitivity(
        provider,
        provider.getDiscountCurves(),
        (base, bumped) -> base.toBuilder().discountCurves(bumped).build(),
        valueFn,
        valueInit);
    CurveCurrencyParameterSensitivities forward = sensitivity(
        provider,
        provider.getIndexCurves(),
        (base, bumped) -> base.toBuilder().indexCurves(bumped).build(),
        valueFn,
        valueInit);
    CurveCurrencyParameterSensitivities priceIndex = sensitivityPriceIndex(
        provider,
        provider.getPriceIndexValues(),
        (base, bumped) -> base.toBuilder().priceIndexValues(bumped).build(),
        valueFn,
        valueInit);
    return discounting.combinedWith(forward).combinedWith(priceIndex);
  }

  // computes the sensitivity with respect to the curves
  private <T> CurveCurrencyParameterSensitivities sensitivity(
      ImmutableRatesProvider provider,
      Map<T, Curve> baseCurves,
      BiFunction<ImmutableRatesProvider, Map<T, Curve>, ImmutableRatesProvider> storeBumpedFn,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      CurrencyAmount valueInit) {

    CurveCurrencyParameterSensitivities result = CurveCurrencyParameterSensitivities.empty();
    for (Entry<T, Curve> entry : baseCurves.entrySet()) {
      NodalCurve curveInt = entry.getValue().toNodalCurve();
      int nbNodePoint = curveInt.getXValues().size();
      DoubleArray sensitivity = DoubleArray.of(nbNodePoint, i -> {
        Curve dscBumped = bumpedCurve(curveInt, i);
        Map<T, Curve> mapBumped = new HashMap<>(baseCurves);
        mapBumped.put(entry.getKey(), dscBumped);
        ImmutableRatesProvider providerDscBumped = storeBumpedFn.apply(provider, mapBumped);
        return (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      });
      CurveMetadata metadata = entry.getValue().getMetadata();
      result = result.combinedWith(CurveCurrencyParameterSensitivity.of(metadata, valueInit.getCurrency(), sensitivity));
    }
    return result;
  }

  // computes the sensitivity with respect to the price index curves
  private <T> CurveCurrencyParameterSensitivities sensitivityPriceIndex(
      ImmutableRatesProvider provider,
      Map<PriceIndex, PriceIndexValues> indexValues,
      BiFunction<ImmutableRatesProvider, Map<PriceIndex, PriceIndexValues>, ImmutableRatesProvider> storeBumpedFn,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      CurrencyAmount valueInit) {

    CurveCurrencyParameterSensitivities result = CurveCurrencyParameterSensitivities.empty();
    for (Entry<PriceIndex, PriceIndexValues> entry : indexValues.entrySet()) {
      ForwardPriceIndexValues indexValue = ((ForwardPriceIndexValues) entry.getValue());
      NodalCurve curveInt = indexValue.getCurve().toNodalCurve();
      int nbNodePoint = curveInt.getXValues().size();
      DoubleArray sensitivity = DoubleArray.of(nbNodePoint, i -> {
        Curve dscBumped = bumpedCurve(curveInt, i);
        Map<PriceIndex, PriceIndexValues> mapBumped = new HashMap<>(indexValues);
        mapBumped.put(entry.getKey(), indexValue.withCurve((InterpolatedNodalCurve) dscBumped));
        ImmutableRatesProvider providerDscBumped = storeBumpedFn.apply(provider, mapBumped);
        return (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      });
      CurveMetadata metadata = indexValue.getCurve().getMetadata();
      result = result.combinedWith(CurveCurrencyParameterSensitivity.of(metadata, valueInit.getCurrency(), sensitivity));
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the first order sensitivities of a function of a LegalEntityDiscountingProvider to a double by finite difference.
   * <p>
   * The curves underlying the rates provider must be of type {@link NodalCurve}.
   * The finite difference is computed by forward type. 
   * The function should return a value in the same currency for any rates provider of LegalEntityDiscountingProvider.
   * 
   * @param provider  the rates provider
   * @param valueFn  the function from a rate provider to a currency amount for which the sensitivity should be computed
   * @return the curve sensitivity
   */
  public CurveCurrencyParameterSensitivities sensitivity(
      LegalEntityDiscountingProvider provider,
      Function<LegalEntityDiscountingProvider, CurrencyAmount> valueFn) {

    CurrencyAmount valueInit = valueFn.apply(provider);
    CurveCurrencyParameterSensitivities discounting = sensitivity(
        provider, valueFn, LegalEntityDiscountingProvider.meta().repoCurves(), valueInit);
    CurveCurrencyParameterSensitivities forward = sensitivity(
        provider, valueFn, LegalEntityDiscountingProvider.meta().issuerCurves(), valueInit);
    return discounting.combinedWith(forward);
  }

  private <T> CurveCurrencyParameterSensitivities sensitivity(
      LegalEntityDiscountingProvider provider,
      Function<LegalEntityDiscountingProvider, CurrencyAmount> valueFn,
      MetaProperty<ImmutableMap<Pair<T, Currency>, DiscountFactors>> metaProperty,
      CurrencyAmount valueInit) {
    ImmutableMap<Pair<T, Currency>, DiscountFactors> baseCurves = metaProperty.get(provider);
    CurveCurrencyParameterSensitivities result = CurveCurrencyParameterSensitivities.empty();
    for (Pair<T, Currency> key : baseCurves.keySet()) {
      DiscountFactors discountFactors = baseCurves.get(key);
      Curve curve = checkDiscountFactors(discountFactors);
      NodalCurve curveInt = checkNodal(curve);
      int nbNodePoint = curveInt.getXValues().size();
      double[] sensitivity = new double[nbNodePoint];
      for (int i = 0; i < nbNodePoint; i++) {
        Curve dscBumped = bumpedCurve(curveInt, i);
        Map<Pair<T, Currency>, DiscountFactors> mapBumped = new HashMap<>(baseCurves);
        mapBumped.put(key, createDiscountFactors(discountFactors, dscBumped));
        LegalEntityDiscountingProvider providerDscBumped = provider.toBuilder().set(metaProperty, mapBumped).build();
        sensitivity[i] = (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      }
      CurveMetadata metadata = curveInt.getMetadata();
      result = result.combinedWith(
          CurveCurrencyParameterSensitivity.of(metadata, valueInit.getCurrency(), DoubleArray.copyOf(sensitivity)));
    }
    return result;
  }

  //-------------------------------------------------------------------------
  // check that the curve is a NodalCurve
  private NodalCurve checkNodal(Curve curve) {
    ArgChecker.isTrue(curve instanceof NodalCurve, "Curve must be a NodalCurve");
    return (NodalCurve) curve;
  }

  // create new curve by bumping the existing curve at a given parameter
  private NodalCurve bumpedCurve(NodalCurve curveInt, int loopnode) {
    DoubleArray yValues = curveInt.getYValues();
    return curveInt.withYValues(yValues.with(loopnode, yValues.get(loopnode) + shift));
  }

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
