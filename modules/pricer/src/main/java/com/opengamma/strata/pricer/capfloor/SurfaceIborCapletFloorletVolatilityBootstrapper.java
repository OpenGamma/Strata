/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;

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
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.pricer.impl.option.GenericImpliedVolatiltySolver;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Caplet volatilities calibration to cap volatilities based on interpolated surface.
 * <p>
 * The caplet volatilities are computed by bootstrapping along the expiry time dimension. 
 * The result is an interpolated surface spanned by expiry and strike.  
 * The position of the node points on the resultant surface corresponds to last expiry date of market caps. 
 * The nodes should be interpolated by a local interpolation scheme along the time direction.  
 * See {@link SurfaceIborCapletFloorletBootstrapDefinition} for detail.
 * <p>
 * If the shift curve is not present in {@code SurfaceIborCapletFloorletBootstrapVolatilityDefinition}, 
 * the resultant volatility type is the same as the input volatility type, i.e.,
 * Black caplet volatilities are returned if Balck cap volatilities are plugged in, and normal caplet volatilities are
 * returned otherwise. 
 * On the other hand, if the shift curve is present in {@code SurfaceIborCapletFloorletBootstrapVolatilityDefinition}, 
 * Black caplet volatilities are returned for any input volatility type. 
 */
public class SurfaceIborCapletFloorletVolatilityBootstrapper extends IborCapletFloorletVolatilityCalibrator {

  /**
   * Default implementation.
   */
  public static final SurfaceIborCapletFloorletVolatilityBootstrapper DEFAULT = of(
      VolatilityIborCapFloorLegPricer.DEFAULT, ReferenceData.standard());

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
      IborCapletFloorletVolatilityDefinition definition,
      ZonedDateTime calibrationDateTime,
      RawOptionData capFloorData,
      RatesProvider ratesProvider) {

    ArgChecker.isTrue(ratesProvider.getValuationDate().equals(calibrationDateTime.toLocalDate()),
        "valuationDate of ratesProvider should be coherent to calibrationDateTime");
    ArgChecker.isTrue(definition instanceof SurfaceIborCapletFloorletBootstrapVolatilityDefinition,
        "definition should be SurfaceIborCapletFloorletBootstrapVolatilityDefinition");
    SurfaceIborCapletFloorletBootstrapVolatilityDefinition bsDefinition =
        (SurfaceIborCapletFloorletBootstrapVolatilityDefinition) definition;
    IborIndex index = bsDefinition.getIndex();
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate baseDate = index.getEffectiveDateOffset().adjust(calibrationDate, referenceData);
    LocalDate startDate = baseDate.plus(index.getTenor());
    Function<Surface, IborCapletFloorletVolatilities> volatilitiesFunction = volatilitiesFunction(
        bsDefinition, calibrationDateTime, capFloorData);
    SurfaceMetadata metadata = bsDefinition.createMetadata(capFloorData);
    List<Period> expiries = capFloorData.getExpiries();
    int nExpiries = expiries.size();
    DoubleArray strikes = capFloorData.getStrikes();
    DoubleMatrix errorsMatrix = capFloorData.getError().orElse(DoubleMatrix.filled(nExpiries, strikes.size(), 1d));
    List<Double> timeList = new ArrayList<>();
    List<Double> strikeList = new ArrayList<>();
    List<Double> volList = new ArrayList<>();
    List<ResolvedIborCapFloorLeg> capList = new ArrayList<>();
    List<Double> priceList = new ArrayList<>();
    List<Double> errorList = new ArrayList<>();
    int[] startIndex = new int[nExpiries + 1];
    for (int i = 0; i < nExpiries; ++i) {
      LocalDate endDate = baseDate.plus(expiries.get(i));
      DoubleArray volatilityData = capFloorData.getData().row(i);
      DoubleArray errors = errorsMatrix.row(i);
      reduceRawData(bsDefinition, ratesProvider, strikes, volatilityData, errors, startDate, endDate, metadata,
          volatilitiesFunction, timeList, strikeList, volList, capList, priceList, errorList);
      startIndex[i + 1] = volList.size();
      ArgChecker.isTrue(startIndex[i + 1] > startIndex[i], "no valid option data for {}", expiries.get(i));
    }
    int nTotal = startIndex[nExpiries];
    IborCapletFloorletVolatilities vols;
    int start;
    ZonedDateTime prevExpiry;
    DoubleArray initialVol = DoubleArray.copyOf(volList);
    if (bsDefinition.getShiftCurve().isPresent()) {
      Curve shiftCurve = bsDefinition.getShiftCurve().get();
      DoubleArray strikeShifted = DoubleArray.of(nTotal, n -> strikeList.get(n) + shiftCurve.yValue(timeList.get(n)));
      if (capFloorData.getDataType().equals(NORMAL_VOLATILITY)) { // correct initial surface
        metadata = Surfaces.blackVolatilityByExpiryStrike(bsDefinition.getName().getName(), bsDefinition.getDayCount())
          .withParameterMetadata(metadata.getParameterMetadata().get());
        initialVol = DoubleArray.of(nTotal, n -> volList.get(n) /
                (ratesProvider.iborIndexRates(index).rate(capList.get(n).getFinalPeriod().getIborRate().getObservation()) +
                shiftCurve.yValue(timeList.get(n))));
      }
      InterpolatedNodalSurface surface = InterpolatedNodalSurface.of(
          metadata, DoubleArray.copyOf(timeList), strikeShifted, initialVol, bsDefinition.getInterpolator());
      vols = ShiftedBlackIborCapletFloorletExpiryStrikeVolatilities.of(
          index, calibrationDateTime, surface, bsDefinition.getShiftCurve().get());
      start = 0;
      prevExpiry = calibrationDateTime.minusDays(1L); // included if calibrationDateTime == fixingDateTime
    } else {
      InterpolatedNodalSurface surface = InterpolatedNodalSurface.of(
          metadata, DoubleArray.copyOf(timeList), DoubleArray.copyOf(strikeList), initialVol, bsDefinition.getInterpolator());
      vols = volatilitiesFunction.apply(surface);
      start = 1;
      prevExpiry = capList.get(startIndex[1] - 1).getFinalFixingDateTime();
    }
    for (int i = start; i < nExpiries; ++i) {
      for (int j = startIndex[i]; j < startIndex[i + 1]; ++j) {
        Function<Double, double[]> func = getValueVegaFunction(capList.get(j), ratesProvider, vols, prevExpiry, j);
        GenericImpliedVolatiltySolver solver = new GenericImpliedVolatiltySolver(func);
        double priceFixed = i == 0 ? 0d : priceFixed(capList.get(j), ratesProvider, vols, prevExpiry);
        double capletVol = solver.impliedVolatility(priceList.get(j) - priceFixed, initialVol.get(j));
        vols = vols.withParameter(j, capletVol);
      }
      prevExpiry = capList.get(startIndex[i + 1] - 1).getFinalFixingDateTime();
    }
    return IborCapletFloorletVolatilityCalibrationResult.ofRootFind(vols);
  }

  //-------------------------------------------------------------------------
  // price and vega function
  private Function<Double, double[]> getValueVegaFunction(
      ResolvedIborCapFloorLeg cap,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities vols,
      ZonedDateTime prevExpiry,
      int nodeIndex) {

    VolatilityIborCapletFloorletPeriodPricer periodPricer = pricer.getPeriodPricer();
    Function<Double, double[]> priceAndVegaFunction = new Function<Double, double[]>() {
      @Override
      public double[] apply(Double x) {
        IborCapletFloorletVolatilities newVols = vols.withParameter(nodeIndex, x);
        double price = cap.getCapletFloorletPeriods().stream()
            .filter(p -> p.getFixingDateTime().isAfter(prevExpiry))
            .mapToDouble(p -> periodPricer.presentValue(p, ratesProvider, newVols).getAmount())
            .sum();
        PointSensitivities point = cap.getCapletFloorletPeriods().stream()
            .filter(p -> p.getFixingDateTime().isAfter(prevExpiry))
            .map(p -> periodPricer.presentValueSensitivityModelParamsVolatility(p, ratesProvider, newVols))
            .reduce((c1, c2) -> c1.combinedWith(c2))
            .get()
            .build();
        CurrencyParameterSensitivities sensi = newVols.parameterSensitivity(point);
        double vega = sensi.getSensitivities().get(0).getSensitivity().get(nodeIndex);
        return new double[] {price, vega};
      }
    };
    return priceAndVegaFunction;
  }

  // sum of caplet prices which are already fixed
  private double priceFixed(
      ResolvedIborCapFloorLeg cap,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities vols,
      ZonedDateTime prevExpiry) {

    VolatilityIborCapletFloorletPeriodPricer periodPricer = pricer.getPeriodPricer();
    return cap.getCapletFloorletPeriods().stream()
        .filter(p -> !p.getFixingDateTime().isAfter(prevExpiry))
        .mapToDouble(p -> periodPricer.presentValue(p, ratesProvider, vols).getAmount())
        .sum();
  }

}
