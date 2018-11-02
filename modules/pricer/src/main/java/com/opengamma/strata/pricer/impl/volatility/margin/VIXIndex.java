/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.io.Files;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;
import com.opengamma.strata.pricer.impl.option.BlackScholesFormulaRepository;

public class VIXIndex {
  
  class OptionInfoHolder {
    double[] strikes;
    double[] callVols;
    double[] putVols;
    double expiry;
    
    OptionInfoHolder(
        double[] strikes,
        double[] callVols,
        double[] putVols,
        double expiry){
      this.strikes = strikes;
      this.callVols = callVols;
      this.putVols = putVols;
      this.expiry = expiry;
    }
  }
  
  private OptionInfoHolder nearInfoHolder;
  private OptionInfoHolder farInfoHolder;
  
  VIXIndex(String nearOptionsFilename,
      String farOptionsFilename){
    CsvFile nearOptionsCSV = CsvFile.of(Files.asCharSource(new File(nearOptionsFilename), StandardCharsets.UTF_8), true);
    CsvFile farOptionsCSV =  CsvFile.of(Files.asCharSource(new File(farOptionsFilename), StandardCharsets.UTF_8), true);
    nearInfoHolder = formOptionInfoHolder(nearOptionsCSV);
    farInfoHolder = formOptionInfoHolder(farOptionsCSV);
  }
  
  private OptionInfoHolder formOptionInfoHolder(CsvFile csvFile){
    List<CsvRow> calls =  csvFile.rows().stream().filter(x->x.getValue("Type").equals("Call")).collect(Collectors.toList());
    List<CsvRow> puts =  csvFile.rows().stream().filter(x->x.getValue("Type").equals("Put")).collect(Collectors.toList());
    assert(calls.size() == puts.size());
    
    double daysToExpiry = Double.parseDouble(calls.get(0).getValue("DTE"));
    double[] strikes = calls.stream().mapToDouble(x->Double.parseDouble(x.getValue("Strike"))).toArray();
    double[] callVols = calls.stream().mapToDouble(x->Double.parseDouble(x.getValue("IV"))).toArray();
    double[] putVols = puts.stream().mapToDouble(x->Double.parseDouble(x.getValue("IV"))).toArray();
    return new OptionInfoHolder(strikes, callVols, putVols, daysToExpiry);
  }
  
  private CallsAndPutsPricesHolder formCallAndPutHolder(OptionInfoHolder infoHolder, double spot, double rate){
    double daysToExpiry = infoHolder.expiry;
    double[] callPrices = IntStream.range(0, infoHolder.strikes.length)
        .mapToDouble(x -> BlackScholesFormulaRepository.price(
            spot,
            infoHolder.strikes[x],
            daysToExpiry / 360.,
            infoHolder.callVols[x],
            rate,
            rate,
            true)).toArray();
    double[] putPrices = IntStream.range(0, infoHolder.strikes.length)
        .mapToDouble(x -> BlackScholesFormulaRepository.price(
            spot,
            infoHolder.strikes[x],
            daysToExpiry / 360.,
            infoHolder.putVols[x],
            rate,
            rate,
            false)).toArray();
    double[][] callsAndPuts = {infoHolder.strikes, callPrices, putPrices};
    
    return new CallsAndPutsPricesHolder(callsAndPuts, daysToExpiry);
  }
  
  public double calculate(double spot, double rate, LocalTime calcTime){
    CallsAndPutsPricesHolder nearHolder = formCallAndPutHolder(nearInfoHolder, spot, rate);
    CallsAndPutsPricesHolder farHolder = formCallAndPutHolder(farInfoHolder, spot, rate);
    return VIXIndexCalculator.vix(nearHolder, farHolder, calcTime, rate);
  }
  
  private OptionInfoHolder formNewOptionInfoHolder(
      double bumpedSpot,
      OptionInfoHolder originalHolder,
      BoundSurfaceInterpolator BumpingFunc,
      double alphaOne,
      Optional<BoundSurfaceInterpolator> systematicFunc,
      Optional<Double> alphaTwo){
    
    double[] newCallVols = new double[originalHolder.strikes.length];
    double[] newPutVols = new double[originalHolder.strikes.length];
    for (int i = 0; i < originalHolder.strikes.length; ++i) {
      double moneyness = alphaOne * bumpedSpot / originalHolder.strikes[i];
      double volFactor = BumpingFunc.interpolate(originalHolder.expiry / 365., moneyness);
      if(systematicFunc.isPresent() && alphaTwo.isPresent()) {
        double cOneVolFactor = systematicFunc.get().interpolate(originalHolder.expiry /365., moneyness);
        volFactor += alphaTwo.get() * cOneVolFactor;
        if(volFactor > 1.)
          volFactor =  1.;
        if(volFactor < -0.7)
          volFactor = -0.7;
      }
      newCallVols[i] = originalHolder.callVols[i] * (1 + volFactor);
      newPutVols[i] = originalHolder.putVols[i] * (1 + volFactor);
    }
    return new OptionInfoHolder(originalHolder.strikes, newCallVols, newPutVols, originalHolder.expiry) ;
  }
  
  public double calculateScenarioBump(
      double bumpedSpot,
      double rate, LocalTime calcTime,
      BoundSurfaceInterpolator BumpingFunc,
      double alphaOne,
      Optional<BoundSurfaceInterpolator> systematicFunc,
      Optional<Double> alphaTwo){
    OptionInfoHolder newNearInfoHolder = formNewOptionInfoHolder(bumpedSpot, nearInfoHolder, BumpingFunc, alphaOne, systematicFunc, alphaTwo);
    OptionInfoHolder newFarInfoHolder = formNewOptionInfoHolder(bumpedSpot, farInfoHolder, BumpingFunc, alphaOne, systematicFunc, alphaTwo);
    CallsAndPutsPricesHolder newNearPriceHolder = formCallAndPutHolder(newNearInfoHolder, bumpedSpot, rate);
    CallsAndPutsPricesHolder newFarPriceHolder = formCallAndPutHolder(newFarInfoHolder, bumpedSpot, rate);
    return VIXIndexCalculator.vix(newNearPriceHolder, newFarPriceHolder, calcTime, rate);
  }
  
  public static void main(String[] args){
    
    VIXIndex vix = new VIXIndex("/Users/richardweeks/Downloads/$spx-options-exp-2018-10-29-show-all-stacked-10-04-2018_equalVolsCallsAndPuts.csv",
                                "/Users/richardweeks/Downloads/$spx-options-exp-2018-11-07-show-all-stacked-10-04-2018_equalCallsAndPutsVols.csv");
    double rate  = 0.03;
    double spot = 2900.83;
    System.out.println("Initial VIX calculation: " + vix.calculate(spot, rate, LocalTime.of(8, 30)));
  }
}

