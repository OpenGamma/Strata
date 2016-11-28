/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.market.ValueType.BLACK_VOLATILITY;
import static com.opengamma.strata.market.ValueType.NORMAL_VOLATILITY;
import static com.opengamma.strata.market.ValueType.STRIKE;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Caplet volatilities calibration to cap volatilities.
 */
abstract class IborCapletFloorletVolatilityCalibrator {

  /**
   * The cap/floor pricer. 
   * <p>
   * This pricer is used for converting market cap volatilities to cap prices. 
   */
  protected final VolatilityIborCapFloorLegPricer pricer;
  /**
   * The reference data.
   */
  protected final ReferenceData referenceData;

  /**
   * Constructor with cap pricer and reference data.
   * 
   * @param pricer  the cap pricer
   * @param referenceData  the reference data
   */
  public IborCapletFloorletVolatilityCalibrator(VolatilityIborCapFloorLegPricer pricer, ReferenceData referenceData) {
    this.pricer = ArgChecker.notNull(pricer, "pricer");
    this.referenceData = ArgChecker.notNull(referenceData, "referenceData");
  }

  /**
   * Calibrates caplet volatilities to cap volatilities.
   * 
   * @param definition  the caplet volatility definition
   * @param calibrationDateTime  the calibration time
   * @param capFloorData  the cap data
   * @param ratesProvider  the rates provider
   * @return the calibration result
   */
  public abstract IborCapletFloorletVolatilityCalibrationResult calibrate(
      IborCapletFloorletDefinition definition,
      ZonedDateTime calibrationDateTime,
      RawOptionData capFloorData,
      RatesProvider ratesProvider);

  //-------------------------------------------------------------------------
  // create complete lists of caps, volatilities, strikes, expiries
  protected void reduceRawData(
      IborCapletFloorletDefinition definition,
      RatesProvider ratesProvider,
      DoubleArray strikes,
      DoubleArray volatilityData,
      LocalDate startDate,
      LocalDate endDate,
      SurfaceMetadata metadata,
      Function<Surface, IborCapletFloorletVolatilities> volatilityFunction,
      List<Double> timeList,
      List<Double> strikeList,
      List<Double> volList,
      List<ResolvedIborCapFloorLeg> capList,
      List<Double> priceList) {

    int nStrikes = strikes.size();
    for (int i = 0; i < nStrikes; ++i) {
      if (Double.isFinite(volatilityData.get(i))) {
        ResolvedIborCapFloorLeg capFloor = definition.createCap(startDate, endDate, strikes.get(i)).resolve(referenceData);
        capList.add(capFloor);
        strikeList.add(strikes.get(i));
        volList.add(volatilityData.get(i));
        ConstantSurface constVolSurface = ConstantSurface.of(metadata, volatilityData.get(i));
        IborCapletFloorletVolatilities vols = volatilityFunction.apply(constVolSurface);
        timeList.add(vols.relativeTime(capFloor.getFinalFixingDateTime()));
        priceList.add(pricer.presentValue(capFloor, ratesProvider, vols).getAmount());
      }
    }
  }

  // function creating volatilities object from surface
  protected Function<Surface, IborCapletFloorletVolatilities> volatilitiesFunction(
      IborCapletFloorletDefinition definition,
      ZonedDateTime calibrationDateTime,
      RawOptionData capFloorData) {

    IborIndex index = definition.getIndex();
    if (capFloorData.getStrikeType().equals(STRIKE)) {
      if (capFloorData.getDataType().equals(BLACK_VOLATILITY)) {
        return blackVolatilitiesFunction(index, calibrationDateTime);
      } else if (capFloorData.getDataType().equals(NORMAL_VOLATILITY)) {
        return normalVolatilitiesFunction(index, calibrationDateTime);
      }
      throw new IllegalArgumentException("Data type not supported");
    }
    throw new IllegalArgumentException("strike type must be ValueType.STRIKE");
  }

  private Function<Surface, IborCapletFloorletVolatilities> blackVolatilitiesFunction(
      IborIndex index,
      ZonedDateTime calibrationDateTime) {

    Function<Surface, IborCapletFloorletVolatilities> func = new Function<Surface, IborCapletFloorletVolatilities>() {
      @Override
      public IborCapletFloorletVolatilities apply(Surface s) {
        return BlackIborCapletFloorletExpiryStrikeVolatilities.of(index, calibrationDateTime, s);
      }
    };
    return func;
  }

  private Function<Surface, IborCapletFloorletVolatilities> normalVolatilitiesFunction(
      IborIndex index,
      ZonedDateTime calibrationDateTime) {

    Function<Surface, IborCapletFloorletVolatilities> func = new Function<Surface, IborCapletFloorletVolatilities>() {
      @Override
      public IborCapletFloorletVolatilities apply(Surface s) {
        return NormalIborCapletFloorletExpiryStrikeVolatilities.of(index, calibrationDateTime, s);
      }
    };
    return func;
  }

}
