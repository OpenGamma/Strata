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
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.credit.CreditDiscountFactors;
import com.opengamma.strata.pricer.credit.CreditRatesProvider;
import com.opengamma.strata.pricer.credit.ImmutableCreditRatesProvider;
import com.opengamma.strata.pricer.credit.IsdaCompliantZeroRateDiscountFactors;
import com.opengamma.strata.pricer.credit.LegalEntitySurvivalProbabilities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Computes the curve parameter sensitivity by finite difference.
 * <p>
 * This is based on an {@link ImmutableRatesProvider}, {@link LegalEntityDiscountingProvider} or {@link CreditRatesProvider}.
 * The sensitivities are calculated by finite difference.
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
      RatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn) {

    ImmutableRatesProvider immProv = provider.toImmutableRatesProvider();
    CurrencyAmount valueInit = valueFn.apply(immProv);
    CurrencyParameterSensitivities discounting = sensitivity(
        immProv,
        immProv.getDiscountCurves(),
        (base, bumped) -> base.toBuilder().discountCurves(bumped).build(),
        valueFn,
        valueInit);
    CurrencyParameterSensitivities forward = sensitivity(
        immProv,
        immProv.getIndexCurves(),
        (base, bumped) -> base.toBuilder().indexCurves(bumped).build(),
        valueFn,
        valueInit);
    return discounting.combinedWith(forward);
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
  /**
   * Computes the first order sensitivities of a function of a {@code CreditRatesProvider} to a double by finite difference.
   * <p>
   * The finite difference is computed by forward type.
   * The function should return a value in the same currency for any rates provider of {@code CreditRatesProvider}.
   * 
   * @param provider  the rates provider
   * @param valueFn  the function from a rate provider to a currency amount for which the sensitivity should be computed
   * @return the curve sensitivity
   */
  public CurrencyParameterSensitivities sensitivity(
      CreditRatesProvider provider,
      Function<ImmutableCreditRatesProvider, CurrencyAmount> valueFn) {

    ImmutableCreditRatesProvider immutableProvider = provider.toImmutableCreditRatesProvider();
    CurrencyAmount valueInit = valueFn.apply(immutableProvider);
    CurrencyParameterSensitivities discounting = sensitivityDiscountCurve(
        immutableProvider, valueFn, ImmutableCreditRatesProvider.meta().discountCurves(), valueInit);
    CurrencyParameterSensitivities credit = sensitivityCreidtCurve(
        immutableProvider, valueFn, ImmutableCreditRatesProvider.meta().creditCurves(), valueInit);
    return discounting.combinedWith(credit);
  }

  private <T> CurrencyParameterSensitivities sensitivityDiscountCurve(
      ImmutableCreditRatesProvider provider,
      Function<ImmutableCreditRatesProvider, CurrencyAmount> valueFn,
      MetaProperty<ImmutableMap<T, CreditDiscountFactors>> metaProperty,
      CurrencyAmount valueInit) {

    ImmutableMap<T, CreditDiscountFactors> baseCurves = metaProperty.get(provider);
    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (T key : baseCurves.keySet()) {
      CreditDiscountFactors creditDiscountFactors = baseCurves.get(key);
      DiscountFactors discountFactors = creditDiscountFactors.toDiscountFactors();
      Curve curve = checkDiscountFactors(discountFactors);
      int paramCount = curve.getParameterCount();
      double[] sensitivity = new double[paramCount];
      for (int i = 0; i < paramCount; i++) {
        Curve dscBumped = curve.withParameter(i, curve.getParameter(i) + shift);
        Map<T, CreditDiscountFactors> mapBumped = new HashMap<>(baseCurves);
        mapBumped.put(key, createCreditDiscountFactors(creditDiscountFactors, dscBumped));
        ImmutableCreditRatesProvider providerDscBumped = provider.toBuilder().set(metaProperty, mapBumped).build();
        sensitivity[i] = (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      }
      result = result.combinedWith(
          curve.createParameterSensitivity(valueInit.getCurrency(), DoubleArray.copyOf(sensitivity)));
    }
    return result;
  }

  private <T> CurrencyParameterSensitivities sensitivityCreidtCurve(
      ImmutableCreditRatesProvider provider,
      Function<ImmutableCreditRatesProvider, CurrencyAmount> valueFn,
      MetaProperty<ImmutableMap<T, LegalEntitySurvivalProbabilities>> metaProperty,
      CurrencyAmount valueInit) {

    ImmutableMap<T, LegalEntitySurvivalProbabilities> baseCurves = metaProperty.get(provider);
    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (T key : baseCurves.keySet()) {
      LegalEntitySurvivalProbabilities credit = baseCurves.get(key);
      CreditDiscountFactors creditDiscountFactors = credit.getSurvivalProbabilities();
      DiscountFactors discountFactors = creditDiscountFactors.toDiscountFactors();
      Curve curve = checkDiscountFactors(discountFactors);
      int paramCount = curve.getParameterCount();
      double[] sensitivity = new double[paramCount];
      for (int i = 0; i < paramCount; i++) {
        Curve dscBumped = curve.withParameter(i, curve.getParameter(i) + shift);
        Map<T, LegalEntitySurvivalProbabilities> mapBumped = new HashMap<>(baseCurves);
        mapBumped.put(key, LegalEntitySurvivalProbabilities.of(
            credit.getLegalEntityId(), createCreditDiscountFactors(creditDiscountFactors, dscBumped)));
        ImmutableCreditRatesProvider providerDscBumped = provider.toBuilder().set(metaProperty, mapBumped).build();
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

  // return correct instance of CreditDiscountFactors
  private CreditDiscountFactors createCreditDiscountFactors(CreditDiscountFactors originalDsc, Curve bumpedCurve) {
    if (originalDsc instanceof IsdaCompliantZeroRateDiscountFactors && bumpedCurve instanceof NodalCurve) {
      IsdaCompliantZeroRateDiscountFactors isdaDsc = (IsdaCompliantZeroRateDiscountFactors) originalDsc;
      return isdaDsc.withCurve((NodalCurve) bumpedCurve);
    }
    throw new IllegalArgumentException("Not supported");
  }

}
