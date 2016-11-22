/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.local;

import java.util.function.Function;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.DeformedSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.ScalarSecondOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.VectorFieldSecondOrderDifferentiator;

/**
 * Local volatility computation based on the exact formula.
 * <p>
 * Bruno Dupire, "Pricing with a Smile", Risk (1994).
 */
public class DupireLocalVolatilityCalculator implements LocalVolatilityCalculator {

  private static final double SMALL = 1.0e-10;
  private static final ScalarFirstOrderDifferentiator FIRST_DERIV = new ScalarFirstOrderDifferentiator();
  private static final ScalarSecondOrderDifferentiator SECOND_DERIV = new ScalarSecondOrderDifferentiator();
  private static final VectorFieldFirstOrderDifferentiator FIRST_DERIV_SENSI = new VectorFieldFirstOrderDifferentiator();
  private static final VectorFieldSecondOrderDifferentiator SECOND_DERIV_SENSI = new VectorFieldSecondOrderDifferentiator();

  @Override
  public DeformedSurface localVolatilityFromImpliedVolatility(
      Surface impliedVolatilitySurface,
      double spot,
      Function<Double, Double> interestRate,
      Function<Double, Double> dividendRate) {

    Function<DoublesPair, ValueDerivatives> func = new Function<DoublesPair, ValueDerivatives>() {
      @Override
      public ValueDerivatives apply(DoublesPair x) {
        double t = x.getFirst();
        double k = x.getSecond();
        double r = interestRate.apply(t);
        double q = dividendRate.apply(t);
        double vol = impliedVolatilitySurface.zValue(t, k);
        DoubleArray volSensi = impliedVolatilitySurface.zValueParameterSensitivity(t, k).getSensitivity();
        double divT = FIRST_DERIV.differentiate(u -> impliedVolatilitySurface.zValue(u, k)).apply(t);
        DoubleArray divTSensi = FIRST_DERIV_SENSI.differentiate(
            u -> impliedVolatilitySurface.zValueParameterSensitivity(u.get(0), k).getSensitivity())
            .apply(DoubleArray.of(t)).column(0);
        double localVol;
        DoubleArray localVolSensi = DoubleArray.of();
        if (k < SMALL) {
          localVol = Math.sqrt(vol * vol + 2 * vol * t * (divT));
          localVolSensi =
              volSensi.multipliedBy((vol + t * divT) / localVol).plus(divTSensi.multipliedBy(vol * t / localVol));
        } else {
          double divK = FIRST_DERIV.differentiate(l -> impliedVolatilitySurface.zValue(t, l)).apply(k);
          DoubleArray divKSensi = FIRST_DERIV_SENSI.differentiate(
              l -> impliedVolatilitySurface.zValueParameterSensitivity(t, l.get(0)).getSensitivity())
              .apply(DoubleArray.of(k)).column(0);
          double divK2 = SECOND_DERIV.differentiate(l -> impliedVolatilitySurface.zValue(t, l)).apply(k);
          DoubleArray divK2Sensi = SECOND_DERIV_SENSI.differentiateNoCross(
              l -> impliedVolatilitySurface.zValueParameterSensitivity(t, l.get(0)).getSensitivity())
              .apply(DoubleArray.of(k)).column(0);
          double rq = r - q;
          double h1 = (Math.log(spot / k) + (rq + 0.5 * vol * vol) * t) / vol;
          double h2 = h1 - vol * t;
          double den = 1d + 2d * h1 * k * divK + k * k * (h1 * h2 * divK * divK + t * vol * divK2);
          double var = (vol * vol + 2d * vol * t * (divT + k * rq * divK)) / den;
          if (var < 0d) {
            throw new IllegalArgumentException("Negative variance");
          }
          localVol = Math.sqrt(var);
          localVolSensi = volSensi.multipliedBy(localVol * k * h2 * divK * (1d + 0.5 * k * h2 * divK) / vol / den +
              0.5 * localVol * Math.pow(k * h1 * divK, 2) / vol / den + (vol + divT * t + rq * t * k * divK) / (localVol * den) -
              0.5 * divK2 * localVol * k * k * t / den)
              .plus(divKSensi.multipliedBy((vol * t * rq * k / localVol - localVol * k * h1 * (1d + k * h2 * divK)) / den))
              .plus(divTSensi.multipliedBy(vol * t / (localVol * den)))
              .plus(divK2Sensi.multipliedBy(-0.5 * vol * localVol * k * k * t / den));
        }
        return ValueDerivatives.of(localVol, localVolSensi);
      }
    };
    SurfaceMetadata metadata = DefaultSurfaceMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.LOCAL_VOLATILITY)
        .surfaceName(SurfaceName.of("localVol_" + impliedVolatilitySurface.getName()))
        .build();
    return DeformedSurface.of(metadata, impliedVolatilitySurface, func);
  }

  @Override
  public DeformedSurface localVolatilityFromPrice(
      Surface callPriceSurface,
      double spot,
      Function<Double, Double> interestRate,
      Function<Double, Double> dividendRate) {

    Function<DoublesPair, ValueDerivatives> func = new Function<DoublesPair, ValueDerivatives>() {
      @Override
      public ValueDerivatives apply(DoublesPair x) {
        double t = x.getFirst();
        double k = x.getSecond();
        double r = interestRate.apply(t);
        double q = dividendRate.apply(t);
        double price = callPriceSurface.zValue(t, k);
        DoubleArray priceSensi = callPriceSurface.zValueParameterSensitivity(t, k).getSensitivity();
        double divT = FIRST_DERIV.differentiate(u -> callPriceSurface.zValue(u, k)).apply(t);
        DoubleArray divTSensi = FIRST_DERIV_SENSI.differentiate(
            u -> callPriceSurface.zValueParameterSensitivity(u.get(0), k).getSensitivity())
            .apply(DoubleArray.of(t)).column(0);
        double divK = FIRST_DERIV.differentiate(l -> callPriceSurface.zValue(t, l)).apply(k);
        DoubleArray divKSensi = FIRST_DERIV_SENSI.differentiate(
            l -> callPriceSurface.zValueParameterSensitivity(t, l.get(0)).getSensitivity())
            .apply(DoubleArray.of(k)).column(0);
        double divK2 = SECOND_DERIV.differentiate(l -> callPriceSurface.zValue(t, l)).apply(k);
        DoubleArray divK2Sensi = SECOND_DERIV_SENSI.differentiateNoCross(
            l -> callPriceSurface.zValueParameterSensitivity(t, l.get(0)).getSensitivity())
            .apply(DoubleArray.of(k)).column(0);
        double var = 2d * (divT + q * price + (r - q) * k * divK) / (k * k * divK2);
        if (var < 0d) {
          throw new IllegalArgumentException("Negative variance");
        }
        double localVol = Math.sqrt(var);
        double factor = 1d / (localVol * k * k * divK2);
        DoubleArray localVolSensi = divTSensi.multipliedBy(factor)
            .plus(divKSensi.multipliedBy((r - q) * k * factor))
            .plus(priceSensi.multipliedBy(q * factor))
            .plus(divK2Sensi.multipliedBy(-0.5 * localVol / divK2));
        return ValueDerivatives.of(localVol, localVolSensi);
      }
    };
    SurfaceMetadata metadata = DefaultSurfaceMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.LOCAL_VOLATILITY)
        .surfaceName(SurfaceName.of("localVol_" + callPriceSurface.getName()))
        .build();
    return DeformedSurface.of(metadata, callPriceSurface, func);
  }

}
