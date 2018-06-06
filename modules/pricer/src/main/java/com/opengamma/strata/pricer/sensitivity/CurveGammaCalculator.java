/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.LegalEntityGroup;
import com.opengamma.strata.market.curve.ParallelShiftedCurve;
import com.opengamma.strata.market.curve.RepoGroup;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivities;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivity;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.SimpleDiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.ZeroRatePeriodicDiscountFactors;
import com.opengamma.strata.pricer.bond.ImmutableLegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Computes the gamma-related values for the rates curve parameters.
 * <p>
 * By default the gamma is computed using a one basis-point shift and a forward finite difference.
 * The results themselves are not scaled (they represent the second order derivative).
 * <p>
 * Reference: Interest Rate Cross-gamma for Single and Multiple Curves. OpenGamma quantitative research 15, July 14
 */
public final class CurveGammaCalculator {

  /**
   * Default implementation. Finite difference is forward and the shift is one basis point (0.0001).
   */
  public static final CurveGammaCalculator DEFAULT = new CurveGammaCalculator(FiniteDifferenceType.FORWARD, 1e-4);

  /**
   * The first order finite difference calculator.
   */
  private final VectorFieldFirstOrderDifferentiator fd;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the finite difference calculator using forward differencing.
   * 
   * @param shift  the shift to be applied to the curves
   * @return the calculator
   */
  public static CurveGammaCalculator ofForwardDifference(double shift) {
    return new CurveGammaCalculator(FiniteDifferenceType.FORWARD, shift);
  }

  /**
   * Obtains an instance of the finite difference calculator using central differencing.
   * 
   * @param shift  the shift to be applied to the curves
   * @return the calculator
   */
  public static CurveGammaCalculator ofCentralDifference(double shift) {
    return new CurveGammaCalculator(FiniteDifferenceType.CENTRAL, shift);
  }

  /**
   * Obtains an instance of the finite difference calculator using backward differencing.
   * 
   * @param shift  the shift to be applied to the curves
   * @return the calculator
   */
  public static CurveGammaCalculator ofBackwardDifference(double shift) {
    return new CurveGammaCalculator(FiniteDifferenceType.BACKWARD, shift);
  }

  //-------------------------------------------------------------------------
  /**
   * Create an instance of the finite difference calculator.
   * 
   * @param fdType  the finite difference type
   * @param shift  the shift to be applied to the curves
   */
  private CurveGammaCalculator(FiniteDifferenceType fdType, double shift) {
    this.fd = new VectorFieldFirstOrderDifferentiator(fdType, shift);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes intra-curve cross gamma by applying finite difference method to curve delta.
   * <p>
   * This computes the intra-curve cross gamma, i.e., the second order sensitivities to individual curves. 
   * Thus the sensitivity of a curve delta to another curve is not produced.
   * <p>
   * The sensitivities are computed for discount curves, and forward curves for {@code RateIndex} and {@code PriceIndex}. 
   * This implementation works only for single currency trades. 
   * 
   * @param ratesProvider  the rates provider
   * @param sensitivitiesFn  the sensitivity function
   * @return the cross gamma
   */
  public CrossGammaParameterSensitivities calculateCrossGammaIntraCurve(
      RatesProvider ratesProvider,
      Function<ImmutableRatesProvider, CurrencyParameterSensitivities> sensitivitiesFn) {

    ImmutableRatesProvider immProv = ratesProvider.toImmutableRatesProvider();
    CurrencyParameterSensitivities baseDelta = sensitivitiesFn.apply(immProv); // used to check target sensitivity exits
    CrossGammaParameterSensitivities result = CrossGammaParameterSensitivities.empty();
    // discount curve
    for (Entry<Currency, Curve> entry : immProv.getDiscountCurves().entrySet()) {
      Currency currency = entry.getKey();
      Curve curve = entry.getValue();
      if (baseDelta.findSensitivity(curve.getName(), currency).isPresent()) {
        CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
            curve, currency, c -> immProv.toBuilder().discountCurve(currency, c).build(), sensitivitiesFn);
        result = result.combinedWith(gammaSingle);
      } else if (curve.split().size() > 1) {
        ImmutableList<Curve> curves = curve.split();
        int nCurves = curves.size();
        for (int i = 0; i < nCurves; ++i) {
          int currentIndex = i;
          Curve underlyingCurve = curves.get(currentIndex);
          if (baseDelta.findSensitivity(underlyingCurve.getName(), currency).isPresent()) {
            CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
                underlyingCurve,
                currency,
                c -> immProv.toBuilder().discountCurve(currency, curve.withUnderlyingCurve(currentIndex, c)).build(),
                sensitivitiesFn);
            result = result.combinedWith(gammaSingle);
          }
        }
      }
    }
    // forward curve
    for (Entry<Index, Curve> entry : immProv.getIndexCurves().entrySet()) {
      Index index = entry.getKey();
      if (index instanceof RateIndex || index instanceof PriceIndex) {
        Currency currency = getCurrency(index);
        Curve curve = entry.getValue();
        if (baseDelta.findSensitivity(curve.getName(), currency).isPresent()) {
          CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
              curve, currency, c -> immProv.toBuilder().indexCurve(index, c).build(), sensitivitiesFn);
          result = result.combinedWith(gammaSingle);
        } else if (curve.split().size() > 1) {
          ImmutableList<Curve> curves = curve.split();
          int nCurves = curves.size();
          for (int i = 0; i < nCurves; ++i) {
            int currentIndex = i;
            Curve underlyingCurve = curves.get(currentIndex);
            if (baseDelta.findSensitivity(underlyingCurve.getName(), currency).isPresent()) {
              CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
                  underlyingCurve,
                  currency,
                  c -> immProv.toBuilder().indexCurve(index, curve.withUnderlyingCurve(currentIndex, c)).build(),
                  sensitivitiesFn);
              result = result.combinedWith(gammaSingle);
            }
          }
        }
      }
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes intra-curve cross gamma for bond curves by applying finite difference method to curve delta.
   * <p>
   * This computes the intra-curve cross gamma, i.e., the second order sensitivities to individual curves. 
   * Thus the sensitivity of a curve delta to another curve is not produced.
   * <p>
   * The underlying instruments must be single-currency, i.e., the curve currency must be the same as the sensitivity currency.
   * 
   * @param ratesProvider  the rates provider
   * @param sensitivitiesFn  the sensitivity function
   * @return the cross gamma
   */
  public CrossGammaParameterSensitivities calculateCrossGammaIntraCurve(
      LegalEntityDiscountingProvider ratesProvider,
      Function<ImmutableLegalEntityDiscountingProvider, CurrencyParameterSensitivities> sensitivitiesFn) {

    LocalDate valuationDate = ratesProvider.getValuationDate();
    ImmutableLegalEntityDiscountingProvider immProv = ratesProvider.toImmutableLegalEntityDiscountingProvider();
    CurrencyParameterSensitivities baseDelta = sensitivitiesFn.apply(immProv); // used to check target sensitivity exits
    CrossGammaParameterSensitivities result = CrossGammaParameterSensitivities.empty();
    // issuer curve
    for (Entry<Pair<LegalEntityGroup, Currency>, DiscountFactors> entry : immProv.getIssuerCurves().entrySet()) {
      Pair<LegalEntityGroup, Currency> legCcy = entry.getKey();
      Currency currency = legCcy.getSecond();
      Curve curve = getCurve(entry.getValue());
      CurveName curveName = curve.getName();
      if (baseDelta.findSensitivity(curveName, currency).isPresent()) {
        CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
            curveName,
            curve,
            currency,
            c -> replaceIssuerCurve(immProv, legCcy, DiscountFactors.of(currency, valuationDate, c)),
            sensitivitiesFn);
        result = result.combinedWith(gammaSingle);
      } else {
        ImmutableList<Curve> curves = curve.split();
        int nCurves = curves.size();
        if (nCurves > 1) {
          for (int i = 0; i < nCurves; ++i) {
            int currentIndex = i;
            Curve underlyingCurve = curves.get(currentIndex);
            CurveName underlyingCurveName = underlyingCurve.getName();
            if (baseDelta.findSensitivity(underlyingCurveName, currency).isPresent()) {
              CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
                  underlyingCurveName,
                  underlyingCurve,
                  currency,
                  c -> replaceIssuerCurve(
                      immProv, legCcy, DiscountFactors.of(currency, valuationDate, curve.withUnderlyingCurve(currentIndex, c))),
                  sensitivitiesFn);
              result = result.combinedWith(gammaSingle);
            }
          }
        }
      }
    }
    // repo curve
    for (Entry<Pair<RepoGroup, Currency>, DiscountFactors> entry : immProv.getRepoCurves().entrySet()) {
      Pair<RepoGroup, Currency> rgCcy = entry.getKey();
      Currency currency = rgCcy.getSecond();
      Curve curve = getCurve(entry.getValue());
      CurveName curveName = curve.getName();
      if (baseDelta.findSensitivity(curveName, currency).isPresent()) {
        CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
            curveName,
            curve,
            currency,
            c -> replaceRepoCurve(immProv, rgCcy, DiscountFactors.of(currency, valuationDate, c)),
            sensitivitiesFn);
        result = result.combinedWith(gammaSingle);
      } else {
        ImmutableList<Curve> curves = curve.split();
        int nCurves = curves.size();
        if (nCurves > 1) {
          for (int i = 0; i < nCurves; ++i) {
            int currentIndex = i;
            Curve underlyingCurve = curves.get(currentIndex);
            CurveName underlyingCurveName = underlyingCurve.getName();
            if (baseDelta.findSensitivity(underlyingCurveName, rgCcy.getSecond()).isPresent()) {
              CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
                  underlyingCurveName,
                  underlyingCurve,
                  currency,
                  c -> replaceRepoCurve(
                      immProv, rgCcy, DiscountFactors.of(currency, valuationDate, curve.withUnderlyingCurve(currentIndex, c))),
                  sensitivitiesFn);
              result = result.combinedWith(gammaSingle);
            }
          }
        }
      }
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes cross-curve gamma by applying finite difference method to curve delta.
   * <p>
   * This computes the cross-curve gamma, i.e., the second order sensitivities to full curves. 
   * Thus the sensitivities of curve delta to other curves are produced.
   * <p>
   * The sensitivities are computed for discount curves, and forward curves for {@code RateIndex} and {@code PriceIndex}. 
   * This implementation works only for single currency trades. 
   * 
   * @param ratesProvider  the rates provider
   * @param sensitivitiesFn  the sensitivity function
   * @return the cross gamma
   */
  public CrossGammaParameterSensitivities calculateCrossGammaCrossCurve(
      RatesProvider ratesProvider,
      Function<ImmutableRatesProvider, CurrencyParameterSensitivities> sensitivitiesFn) {

    ImmutableRatesProvider immProv = ratesProvider.toImmutableRatesProvider();
    CurrencyParameterSensitivities baseDelta = sensitivitiesFn.apply(immProv); // used to check target sensitivity exits.
    CrossGammaParameterSensitivities result = CrossGammaParameterSensitivities.empty();
    for (CurrencyParameterSensitivity baseDeltaSingle : baseDelta.getSensitivities()) {
      CrossGammaParameterSensitivities resultInner = CrossGammaParameterSensitivities.empty();
      // discount curve
      for (Entry<Currency, Curve> entry : immProv.getDiscountCurves().entrySet()) {
        Currency currency = entry.getKey();
        Curve curve = entry.getValue();
        if (baseDelta.findSensitivity(curve.getName(), currency).isPresent()) {
          CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
              baseDeltaSingle, curve, c -> immProv.toBuilder().discountCurve(currency, c).build(), sensitivitiesFn);
          resultInner = resultInner.combinedWith(gammaSingle);
        } else if (curve.split().size() > 1) {
          ImmutableList<Curve> curves = curve.split();
          int nCurves = curves.size();
          for (int i = 0; i < nCurves; ++i) {
            int currentIndex = i;
            Curve underlyingCurve = curves.get(currentIndex);
            if (baseDelta.findSensitivity(underlyingCurve.getName(), currency).isPresent()) {
              CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
                  baseDeltaSingle,
                  underlyingCurve,
                  c -> immProv.toBuilder().discountCurve(currency, curve.withUnderlyingCurve(currentIndex, c)).build(),
                  sensitivitiesFn);
              resultInner = resultInner.combinedWith(gammaSingle);
            }
          }
        }
      }
      // forward curve
      for (Entry<Index, Curve> entry : immProv.getIndexCurves().entrySet()) {
        Index index = entry.getKey();
        if (index instanceof RateIndex || index instanceof PriceIndex) {
          Currency currency = getCurrency(index);
          Curve curve = entry.getValue();
          if (baseDelta.findSensitivity(curve.getName(), currency).isPresent()) {
            CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
                baseDeltaSingle, curve, c -> immProv.toBuilder().indexCurve(index, c).build(), sensitivitiesFn);
            resultInner = resultInner.combinedWith(gammaSingle);
          } else if (curve.split().size() > 1) {
            ImmutableList<Curve> curves = curve.split();
            int nCurves = curves.size();
            for (int i = 0; i < nCurves; ++i) {
              int currentIndex = i;
              Curve underlyingCurve = curves.get(currentIndex);
              if (baseDelta.findSensitivity(underlyingCurve.getName(), currency).isPresent()) {
                CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
                    baseDeltaSingle,
                    underlyingCurve,
                    c -> immProv.toBuilder().indexCurve(index, curve.withUnderlyingCurve(currentIndex, c)).build(),
                    sensitivitiesFn);
                resultInner = resultInner.combinedWith(gammaSingle);
              }
            }
          }
        }
      }
      result = result.combinedWith(combineSensitivities(baseDeltaSingle, resultInner));
    }
    return result;
  }

  //-------------------------------------------------------------------------
  private Currency getCurrency(Index index) {
    if (index instanceof RateIndex) {
      return ((RateIndex) index).getCurrency();
    } else if (index instanceof PriceIndex) {
      return ((PriceIndex) index).getCurrency();
    }
    throw new IllegalArgumentException("unsupported index");
  }

  // compute the second order sensitivity to Curve
  CrossGammaParameterSensitivity computeGammaForCurve(
      Curve curve,
      Currency sensitivityCurrency,
      Function<Curve, ImmutableRatesProvider> ratesProviderFn,
      Function<ImmutableRatesProvider, CurrencyParameterSensitivities> sensitivitiesFn) {

    Function<DoubleArray, DoubleArray> function = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray t) {
        Curve newCurve = replaceParameters(curve, t);
        ImmutableRatesProvider newRates = ratesProviderFn.apply(newCurve);
        CurrencyParameterSensitivities sensiMulti = sensitivitiesFn.apply(newRates);
        return sensiMulti.getSensitivity(newCurve.getName(), sensitivityCurrency).getSensitivity();
      }
    };
    int nParams = curve.getParameterCount();
    DoubleMatrix sensi = fd.differentiate(function).apply(DoubleArray.of(nParams, n -> curve.getParameter(n)));
    List<ParameterMetadata> metadata = IntStream.range(0, nParams)
        .mapToObj(i -> curve.getParameterMetadata(i))
        .collect(toImmutableList());
    return CrossGammaParameterSensitivity.of(curve.getName(), metadata, sensitivityCurrency, sensi);
  }

  // computes the sensitivity of baseDeltaSingle to Curve
  CrossGammaParameterSensitivity computeGammaForCurve(
      CurrencyParameterSensitivity baseDeltaSingle,
      Curve curve,
      Function<Curve, ImmutableRatesProvider> ratesProviderFn,
      Function<ImmutableRatesProvider, CurrencyParameterSensitivities> sensitivitiesFn) {

    Function<DoubleArray, DoubleArray> function = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray t) {
        Curve newCurve = replaceParameters(curve, t);
        ImmutableRatesProvider newRates = ratesProviderFn.apply(newCurve);
        CurrencyParameterSensitivities sensiMulti = sensitivitiesFn.apply(newRates);
        return sensiMulti.getSensitivity(baseDeltaSingle.getMarketDataName(), baseDeltaSingle.getCurrency()).getSensitivity();
      }
    };
    int nParams = curve.getParameterCount();
    DoubleMatrix sensi = fd.differentiate(function).apply(DoubleArray.of(nParams, n -> curve.getParameter(n)));
    List<ParameterMetadata> metadata = IntStream.range(0, nParams)
        .mapToObj(i -> curve.getParameterMetadata(i))
        .collect(toImmutableList());
    return CrossGammaParameterSensitivity.of(
        baseDeltaSingle.getMarketDataName(),
        baseDeltaSingle.getParameterMetadata(),
        curve.getName(),
        metadata,
        baseDeltaSingle.getCurrency(),
        sensi);
  }

  private CrossGammaParameterSensitivity combineSensitivities(
      CurrencyParameterSensitivity baseDeltaSingle,
      CrossGammaParameterSensitivities blockCrossGamma) {

    double[][] valuesTotal = new double[baseDeltaSingle.getParameterCount()][];
    List<Pair<MarketDataName<?>, List<? extends ParameterMetadata>>> order = new ArrayList<>();
    for (int i = 0; i < baseDeltaSingle.getParameterCount(); ++i) {
      ArrayList<Double> innerList = new ArrayList<>();
      for (CrossGammaParameterSensitivity gammaSingle : blockCrossGamma.getSensitivities()) {
        innerList.addAll(gammaSingle.getSensitivity().row(i).toList());
        if (i == 0) {
          order.add(gammaSingle.getOrder().get(0));
        }
      }
      valuesTotal[i] = Doubles.toArray(innerList);
    }
    return CrossGammaParameterSensitivity.of(
        baseDeltaSingle.getMarketDataName(),
        baseDeltaSingle.getParameterMetadata(),
        order,
        baseDeltaSingle.getCurrency(),
        DoubleMatrix.ofUnsafe(valuesTotal));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the "sum-of-column gamma" or "semi-parallel gamma" for a sensitivity function.
   * <p>
   * This implementation supports a single {@link Curve} on the zero-coupon rates.
   * By default the gamma is computed using a one basis-point shift and a forward finite difference.
   * The results themselves are not scaled (they represent the second order derivative).
   * 
   * @param curve  the single curve to be bumped
   * @param curveCurrency  the currency of the curve and resulting sensitivity
   * @param sensitivitiesFn  the function to convert the bumped curve to parameter sensitivities
   * @return the "sum-of-columns" or "semi-parallel" gamma vector
   */
  public CurrencyParameterSensitivity calculateSemiParallelGamma(
      Curve curve,
      Currency curveCurrency,
      Function<Curve, CurrencyParameterSensitivity> sensitivitiesFn) {

    Delta deltaShift = new Delta(curve, sensitivitiesFn);
    Function<DoubleArray, DoubleMatrix> gammaFn = fd.differentiate(deltaShift);
    DoubleArray gamma = gammaFn.apply(DoubleArray.filled(1)).column(0);
    return curve.createParameterSensitivity(curveCurrency, gamma);
  }

  //-------------------------------------------------------------------------
  private Curve replaceParameters(Curve curve, DoubleArray newParameters) {
    return curve.withPerturbation((i, v, m) -> newParameters.get(i));
  }

  //-------------------------------------------------------------------------
  /**
   * Inner class to compute the delta for a given parallel shift of the curve.
   */
  static class Delta implements Function<DoubleArray, DoubleArray> {
    private final Curve curve;
    private final Function<Curve, CurrencyParameterSensitivity> sensitivitiesFn;

    Delta(Curve curve, Function<Curve, CurrencyParameterSensitivity> sensitivitiesFn) {
      this.curve = curve;
      this.sensitivitiesFn = sensitivitiesFn;
    }

    @Override
    public DoubleArray apply(DoubleArray s) {
      double shift = s.get(0);
      Curve curveBumped = ParallelShiftedCurve.absolute(curve, shift);
      CurrencyParameterSensitivity pts = sensitivitiesFn.apply(curveBumped);
      return pts.getSensitivity();
    }
  }

  //-------------------------------------------------------------------------
  private Curve getCurve(DiscountFactors discountFactors) {
    if (discountFactors instanceof SimpleDiscountFactors) {
      return ((SimpleDiscountFactors) discountFactors).getCurve();
    }
    if (discountFactors instanceof ZeroRateDiscountFactors) {
      return ((ZeroRateDiscountFactors) discountFactors).getCurve();
    }
    if (discountFactors instanceof ZeroRatePeriodicDiscountFactors) {
      return ((ZeroRatePeriodicDiscountFactors) discountFactors).getCurve();
    }
    throw new IllegalArgumentException("Unsupported DiscountFactors type");
  }

  private CrossGammaParameterSensitivity computeGammaForCurve(
      CurveName curveName,
      Curve curve,
      Currency sensitivityCurrency,
      Function<Curve, ImmutableLegalEntityDiscountingProvider> ratesProviderFn,
      Function<ImmutableLegalEntityDiscountingProvider, CurrencyParameterSensitivities> sensitivitiesFn) {

    Function<DoubleArray, DoubleArray> function = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray t) {
        Curve newCurve = curve.withPerturbation((i, v, m) -> t.get(i));
        ImmutableLegalEntityDiscountingProvider newRates = ratesProviderFn.apply(newCurve);
        CurrencyParameterSensitivities sensiMulti = sensitivitiesFn.apply(newRates);
        return sensiMulti.getSensitivity(curveName, sensitivityCurrency).getSensitivity();
      }
    };
    int nParams = curve.getParameterCount();
    DoubleMatrix sensi = fd.differentiate(function).apply(DoubleArray.of(nParams, n -> curve.getParameter(n)));
    List<ParameterMetadata> metadata = IntStream.range(0, nParams)
        .mapToObj(i -> curve.getParameterMetadata(i))
        .collect(toImmutableList());
    return CrossGammaParameterSensitivity.of(curveName, metadata, sensitivityCurrency, sensi);
  }

  private ImmutableLegalEntityDiscountingProvider replaceIssuerCurve(
      ImmutableLegalEntityDiscountingProvider ratesProvider,
      Pair<LegalEntityGroup, Currency> legCcy,
      DiscountFactors discountFactors) {

    Map<Pair<LegalEntityGroup, Currency>, DiscountFactors> curves = new HashMap<>();
    curves.putAll(ratesProvider.getIssuerCurves());
    curves.put(legCcy, discountFactors);

    return ratesProvider.toBuilder()
        .issuerCurves(curves)
        .build();
  }

  private ImmutableLegalEntityDiscountingProvider replaceRepoCurve(
      ImmutableLegalEntityDiscountingProvider ratesProvider,
      Pair<RepoGroup, Currency> rgCcy,
      DiscountFactors discountFactors) {

    Map<Pair<RepoGroup, Currency>, DiscountFactors> curves = new HashMap<>();
    curves.putAll(ratesProvider.getRepoCurves());
    curves.put(rgCcy, discountFactors);

    return ratesProvider.toBuilder()
        .repoCurves(curves)
        .build();
  }

}
