/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.google.common.io.Files;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.market.surface.interpolator.BoundSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.product.common.PutCall;

public class VolatilityArbitrage {
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIMES = DoubleArray.of(1./12., 1./12., 1./12., 1./12., 1./12., 1./6., 1./6., 1./6., 1./6., 1./6., 1./3., 1./3., 1./3., 1./3., 1./3., 1., 1., 1., 1., 1.);
  private static final DoubleArray MONEYNESS = DoubleArray.of(0.5, 0.75, 1., 1.25, 1.5, 0.5, 0.75, 1., 1.25, 1.5, 0.5, 0.75, 1., 1.25, 1.5, 0.5, 0.75, 1., 1.25, 1.5);
  private static final DoubleArray FACTORS_ONE = DoubleArray.of(0.32, 0.40, 0.66, 0.37, 0.25, 0.29, 0.35, 0.54, 0.38, 0.26, 0.25, 0.30, 0.43, 0.37, 0.25, 0.18, 0.21, 0.27, 0.30, 0.22);
  private static final DoubleArray FACTORS_TWO = DoubleArray.of(-0.15, -0.1, -0.07, 0.18, 0.19, -0.1, -0.07, -0.05, 0.13, 0.16, -0.07, -0.04, -0.04, 0.07, 0.12, -0.03, -0.02, -0.02, 0., 0.04);
  
  private static final BoundSurfaceInterpolator BOUND_2D_INTERPOLATOR_ONE = INTERPOLATOR_2D.bind(TIMES, MONEYNESS, FACTORS_ONE);
  private static final BoundSurfaceInterpolator BOUND_2D_INTERPOLATOR_TWO = INTERPOLATOR_2D.bind(TIMES, MONEYNESS, FACTORS_TWO);
  private static final double ALPHA = 1.0;
  
  public static class WorseCaseScenario{
    private double worstSpot;
    private double worstMoneyNess;
    private double worstSpotBump;
    private double worstVolFactor;
    private double originalPrice;
    
    WorseCaseScenario(
        double worstSpot,
        double worstMoneyNess,
        double worstSpotBump,
        double worstVolFactor,
        double originalPrice){
      this.worstSpot = worstSpot;
      this.worstMoneyNess = worstMoneyNess;
      this.worstSpotBump = worstSpotBump;
      this.worstVolFactor = worstVolFactor;
      this.originalPrice = originalPrice;
    }
  }
  
   public static WorseCaseScenario determineWorseMarketLevelScenario(Option option, double spot, double vol, double rate){
    //Bump and grind
    double originalPrice = option.calculate(spot, rate, vol);
    final DoubleArray bumps = DoubleArray.of(-0.15, -0.1, -0.05, 0.05, 0.1);
    DoubleArray newSpots = DoubleArray.of(bumps.stream().map(i -> spot * (1. + i)));
    DoubleArray moneyNess = DoubleArray.of(newSpots.stream().map(i -> i / option.strike()));
    DoubleArray volFactors = DoubleArray.of(IntStream.range(0, bumps.size())
                                        .mapToDouble( i -> ALPHA * BOUND_2D_INTERPOLATOR_ONE.interpolate(option.expiry(), moneyNess.get(i))));
    DoubleArray newVols = DoubleArray.of(volFactors.stream().map(i -> vol * (1. + i)));
    DoubleArray newPrices = DoubleArray.of(IntStream.range(0, bumps.size())
                                       .mapToDouble( i -> option.calculate( newSpots.get(i), newVols.get(i), rate)));
    DoubleArray profitAndLoss = DoubleArray.of(newPrices.stream().map(i -> option.quantity() * (i - originalPrice)));
    
    int worstIndex = profitAndLoss.indexOf(profitAndLoss.min());
    WorseCaseScenario wcs = new WorseCaseScenario(newSpots.get(worstIndex), moneyNess.get(worstIndex), bumps.get(worstIndex), volFactors.get(worstIndex), originalPrice);
    return wcs;
  }
  //This will become a portfolio of options, etc.
  public static double determineSystematicStress(WorseCaseScenario worstScenario, Option option, double vol, double rate){
    double cOneVolFactor = BOUND_2D_INTERPOLATOR_TWO.interpolate(option.expiry(), worstScenario.worstMoneyNess);
    DoubleArray alphaOne = DoubleArray.of(IntStream.range(-10, 11).mapToDouble(x -> x/10.));
    DoubleArray volFactors = DoubleArray.of(alphaOne.stream().map(x -> {double factor = worstScenario.worstVolFactor + cOneVolFactor * x;
                                                                          if(factor > 1.)
                                                                            return 1.;
                                                                          if(factor < -0.7)
                                                                            return -0.7;
                                                                          return factor;}));
    DoubleArray newVols = DoubleArray.of(volFactors.stream().map(x -> vol * (1 + x)));
    DoubleArray newPrices = DoubleArray.of(newVols.stream().map(x -> option.calculate( worstScenario.worstSpot, x, rate)));
    DoubleArray profitAndLoss = DoubleArray.of(newPrices.stream().map(i -> option.quantity() * (i - worstScenario.originalPrice)));
    return option.multiplier() * Math.abs(profitAndLoss.min());
  }
  
  public static double determineConcentrationRequirement(Option option, double originalPrice, double spot, double vol, double rate){
    final double[] stockStresses = {0.50, 0., -0.50};
    final double[][] volStresses = {{-0.50, -0.40, 0.}, {-0.50, 0.40, 0.}, {0., 0.50}};
    double worsePNL = 0.;
      for(int i = 0; i < stockStresses.length; ++i){
      double stockStress = stockStresses[i];
      double[] volStressForCurrentStockStress = volStresses[i];
      for(int j = 0; j < volStressForCurrentStockStress.length; ++j){
        double newPNL = option.quantity() * (option.calculate( spot * (1 + stockStress), rate, vol * (1 + volStressForCurrentStockStress[j])) - originalPrice);
        if(newPNL < worsePNL)
          worsePNL = newPNL;
      }
    }
    return option.multiplier() * Math.abs(worsePNL);
  }
  
  public static void main(String[] args){
    //Look at a portfolio
    LocalTime calcTime = LocalTime.of(8, 30);
    double dividend = 0.0175;
    double rate = 0.03;
    double spot = 2900.83;
    VIXIndex vix = new VIXIndex("/Users/richardweeks/Downloads/$spx-options-exp-2018-10-29-show-all-stacked-10-04-2018_equalVolsCallsAndPuts.csv",
                                "/Users/richardweeks/Downloads/$spx-options-exp-2018-11-07-show-all-stacked-10-04-2018_equalCallsAndPutsVols.csv");
    double spotVix = vix.calculate(spot, rate, calcTime);
    
    
    List<Option> options = new ArrayList<>();
    List<VIXFuture> futures = new ArrayList<>();
    List<Double> vols = new ArrayList<>();
    double initialPortfolioValue = 0.;
    CsvFile historicCSVFile = CsvFile.of(Files.asCharSource(new File("/Users/richardweeks/Downloads/vix1.csv"), StandardCharsets.UTF_8), true);
    CsvFile portfolioCSV = CsvFile.of(Files.asCharSource(new File("/Users/richardweeks/Downloads/PortfolioForIM.csv"), StandardCharsets.UTF_8), true);
    //Construct initial portfolio
    for(int i = 0; i < portfolioCSV.rowCount(); ++i) {
      String type = portfolioCSV.row(i).getValue("Type");
      if(type.equals("European")) {
        String payout = portfolioCSV.row(i).getValue("Payout");
        PutCall oType = payout.equals("Call") ? PutCall.CALL : PutCall.PUT;
        EuropeanOption eo =  new EuropeanOptionWithContinuousYield(
            Double.parseDouble(portfolioCSV.row(i).getValue("Quantity")),
            Double.parseDouble(portfolioCSV.row(i).getValue("Multiplier")),
            Double.parseDouble(portfolioCSV.row(i).getValue("Strike")),
            Double.parseDouble(portfolioCSV.row(i).getValue("Expiry")) /365.,
            oType,
            dividend);
        double vol = Double.parseDouble(portfolioCSV.row(i).getValue("Volatility"));
        options.add(eo);
        vols.add(vol);
        initialPortfolioValue += eo.calculate(spot, rate, vol) * eo.multiplier() * eo.quantity();
      } else {
        VIXFuture vf = new VIXFuture(
            historicCSVFile,
            Double.parseDouble(portfolioCSV.row(i).getValue("Expiry")),
            Double.parseDouble(portfolioCSV.row(i).getValue("Quantity")),
            Double.parseDouble(portfolioCSV.row(i).getValue("Multiplier")));
        futures.add(vf);
        //initialPortfolioValue += vf.calculate(spotVix) * vf.quantity() * vf.multiplier();
      }
    }
    System.out.println("Initial Portfolio Value: " + initialPortfolioValue);
    
    //Bump and grind
    List<Double> scenarioValues = new ArrayList<>();
    DoubleArray bumps = DoubleArray.of(-0.15, -0.1, -0.05, 0.05, 0.1);
    for(int i = 0; i < bumps.size(); ++i) {
      double bumpScenario = 0.;
      double bumpedSpot = spot * (1 + bumps.get(i));
      for(int j = 0; j < options.size(); ++j) {
        Option o = options.get(j);
        double moneyness = bumpedSpot / o.strike();
        double volFactor = ALPHA * BOUND_2D_INTERPOLATOR_ONE.interpolate(o.expiry(), moneyness);
        double newVol = vols.get(j) * (1. + volFactor);
        bumpScenario += o.calculate( bumpedSpot, newVol, rate) * o.multiplier() * o.quantity();
      }
      for(int k = 0; k < futures.size(); ++k) {
        double bumpedVix = vix.calculateScenarioBump(bumpedSpot, rate, calcTime, BOUND_2D_INTERPOLATOR_ONE, ALPHA, Optional.empty(), Optional.empty());
        VIXFuture vf = futures.get(k);
        bumpScenario += vf.calculate(bumpedVix) * vf.multiplier() * vf.quantity() ;
      }
      scenarioValues.add(i, bumpScenario);
    }
    //Calculate profit and loss for worst case market scenario
    List<Double> pNL = new ArrayList<>();;
    for(int i = 0; i < scenarioValues.size(); ++i) {
      pNL.add(i, scenarioValues.get(i) - initialPortfolioValue);
    }
    
    //Determine worst bump
    int worstCase = pNL.indexOf(pNL.stream().min(Comparator.comparing(Double::valueOf)).get());
    double worstSpotMove = spot * (1 + bumps.get(worstCase));
    System.out.println("Worst Loss: " + pNL.get(worstCase));
    System.out.println("Current Spot: " + spot);
    System.out.println("Worst Spot Move: " + worstSpotMove);
    
    //Systematic Stress
    List<Double> systematicStressScenarios = new ArrayList<>();
    DoubleArray alphaTwos = DoubleArray.of(IntStream.range(-10, 11).mapToDouble(x -> x/10.));
    for(int i = 0; i < alphaTwos.size(); ++i) {
      double bumpScenario = 0.;
      for(int j = 0; j < options.size(); ++j) {
        Option o = options.get(j);
        double moneyness = worstSpotMove / o.strike();
        double cOneVolFactor = BOUND_2D_INTERPOLATOR_TWO.interpolate(o.expiry(), moneyness);
        double newVolFactor = ALPHA * BOUND_2D_INTERPOLATOR_ONE.interpolate(o.expiry(), moneyness) + alphaTwos.get(i) * cOneVolFactor;
          if(newVolFactor > 1.)
            newVolFactor =  1.;
          if(newVolFactor < -0.7)
            newVolFactor = -0.7;
        bumpScenario += o.calculate(worstSpotMove, newVolFactor, rate) * o.quantity() * o.multiplier();
      }
      for(int k = 0; k < futures.size(); ++k) {
        double bumpedVix = vix.calculateScenarioBump(
            worstSpotMove,
            rate,
            calcTime,
            BOUND_2D_INTERPOLATOR_ONE,
            ALPHA,
            Optional.of(BOUND_2D_INTERPOLATOR_TWO),
            Optional.of(alphaTwos.get(i)));
        VIXFuture vf = futures.get(k);
        bumpScenario += vf.calculate(bumpedVix) * vf.multiplier() * vf.quantity() ;
      }
      systematicStressScenarios.add(i, bumpScenario);
    }
  
    //Calculate profit and loss for worst case market scenario
    List<Double> systematicStress = new ArrayList<>();;
    for(int i = 0; i < scenarioValues.size(); ++i) {
      systematicStress.add(i, systematicStressScenarios.get(i) - initialPortfolioValue);
    }
  }
}
