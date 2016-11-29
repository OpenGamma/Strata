/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.pricer.impl.option.GenericImpliedVolatiltySolver;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Caplet volatilities calibration to cap volatilities based on interpolated surface.
 * <p>
 * The caplet volatilities are computed by bootstrapping along the expiry time dimension. 
 * The result is an interpolated surface spanned by expiry and strike.  
 * The position of the node points on the resultant surface corresponds to market caps. 
 * The node should be interpolated by a local interpolation scheme along the time direction.  
 * See {@link SurfaceIborCapletFloorletBootstrapDefinition} for detail.
 */
public class SurfaceIborCapletFloorletVolatilityBootstrapper extends IborCapletFloorletVolatilityCalibrator {

  /**
   * Default implementation.
   */
  public static final SurfaceIborCapletFloorletVolatilityBootstrapper DEFAULT =
      new SurfaceIborCapletFloorletVolatilityBootstrapper(VolatilityIborCapFloorLegPricer.DEFAULT, ReferenceData.standard());

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param pricer  the cap pricer
   * @param referenceData  the reference data
   * @return the instance
   */
  public static SurfaceIborCapletFloorletVolatilityBootstrapper of(
      VolatilityIborCapFloorLegPricer pricer,
      ReferenceData referenceData) {

    return new SurfaceIborCapletFloorletVolatilityBootstrapper(pricer, referenceData);
  }

  // private constructor
  private SurfaceIborCapletFloorletVolatilityBootstrapper(VolatilityIborCapFloorLegPricer pricer, ReferenceData referenceData) {
    super(pricer, referenceData);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborCapletFloorletVolatilityCalibrationResult calibrate(
      IborCapletFloorletDefinition definition,
      ZonedDateTime calibrationDateTime,
      RawOptionData capFloorData,
      RatesProvider ratesProvider) {

    ArgChecker.isTrue(ratesProvider.getValuationDate().equals(calibrationDateTime.toLocalDate()),
        "valuationDate of ratesProvider should be coherent to calibrationDateTime");
    ArgChecker.isTrue(definition instanceof SurfaceIborCapletFloorletBootstrapDefinition);
    SurfaceIborCapletFloorletBootstrapDefinition bsDefinition = (SurfaceIborCapletFloorletBootstrapDefinition) definition;
    IborIndex index = bsDefinition.getIndex();
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate baseDate = index.getEffectiveDateOffset().adjust(calibrationDate, referenceData);
    LocalDate startDate = baseDate.plus(index.getTenor());
    Function<Surface, IborCapletFloorletVolatilities> volatilitiesFunction = volatilitiesFunction(
        bsDefinition, calibrationDateTime, capFloorData);
    SurfaceMetadata metadata = bsDefinition.createMetadata(capFloorData);
    List<Period> expiries = capFloorData.getExpiries();
    int nExpiries = expiries.size();
    List<Double> timeList = new ArrayList<>();
    List<Double> strikeList = new ArrayList<>();
    List<Double> volList = new ArrayList<>();
    List<ResolvedIborCapFloorLeg> capList = new ArrayList<>();
    List<Double> priceList = new ArrayList<>();
    int[] startIndex = new int[nExpiries + 1];
    for (int i = 0; i < nExpiries; ++i) {
      LocalDate endDate = baseDate.plus(expiries.get(i));
      DoubleArray volatilityData = capFloorData.getData().row(i);
      reduceRawData(bsDefinition, ratesProvider, capFloorData.getStrikes(), volatilityData, startDate, endDate, metadata,
          volatilitiesFunction, timeList, strikeList, volList, capList, priceList);
      startIndex[i + 1] = volList.size();
    }
    IborCapletFloorletVolatilities vols;
    int start;
    if (bsDefinition.getShiftCurve().isPresent()) {
      Curve shiftCurve = bsDefinition.getShiftCurve().get();
      DoubleArray timeShifted = DoubleArray.of(timeList.size(), n -> strikeList.get(n) + shiftCurve.yValue(timeList.get(n)));
      InterpolatedNodalSurface surface = InterpolatedNodalSurface.of(
          metadata, DoubleArray.copyOf(timeList), timeShifted, DoubleArray.copyOf(volList), bsDefinition.getInterpolator());
      vols = ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
          index, calibrationDateTime, surface, bsDefinition.getShiftCurve().get());
      start = 0;
    } else {
      InterpolatedNodalSurface surface = InterpolatedNodalSurface.of(
          metadata, DoubleArray.copyOf(timeList), DoubleArray.copyOf(strikeList), DoubleArray.copyOf(volList),
          bsDefinition.getInterpolator());
      vols = volatilitiesFunction.apply(surface);
      start = 1;
    }
    for (int i = start; i < nExpiries; ++i) {
      for (int j = startIndex[i]; j < startIndex[i + 1]; ++j) {
        Function<Double, double[]> func = getValueVegaFunction(capList.get(j), ratesProvider, vols, j);
        GenericImpliedVolatiltySolver solver = new GenericImpliedVolatiltySolver(func);
        double capletVol = solver.impliedVolatility(priceList.get(j), volList.get(j));
        vols = vols.withParameter(j, capletVol);
      }
    }
    return IborCapletFloorletVolatilityCalibrationResult.ofRootFind(vols);
  }

  //-------------------------------------------------------------------------
  // price and vega function
  private Function<Double, double[]> getValueVegaFunction(
      ResolvedIborCapFloorLeg leg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities vols,
      int nodeIndex) {

    Function<Double, double[]> priceAndVegaFunction = new Function<Double, double[]>() {
      @Override
      public double[] apply(Double x) {
        IborCapletFloorletVolatilities newVols = vols.withParameter(nodeIndex, x);
        double price = pricer.presentValue(leg, ratesProvider, newVols).getAmount();
        PointSensitivities point = pricer.presentValueSensitivityModelParamsVolatility(leg, ratesProvider, newVols).build();
        CurrencyParameterSensitivities sensi = vols.parameterSensitivity(point);
        double vega = sensi.getSensitivities().get(0).getSensitivity().get(nodeIndex);
        return new double[] {price, vega};
      }
    };
    return priceAndVegaFunction;
  }

}
