/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;

import java.util.stream.IntStream;

import com.opengamma.strata.collect.array.DoubleArray;
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
  
   //This will become a portfolio of options 
   public static WorseCaseScenario determineWorseMarketLevelScenario(Option option, double spot, double vol, double rate){
    //Bump and grind
    double originalPrice = option.calculate(spot, rate, vol);
    final DoubleArray bumps = DoubleArray.of(-0.15, -0.1, -0.05, 0.05, 0.1);
    DoubleArray newSpots = DoubleArray.of(bumps.stream().map(i -> spot*(1. + i)));
    DoubleArray moneyNess = DoubleArray.of(newSpots.stream().map(i -> i / option.strike()));
    DoubleArray volFactors = DoubleArray.of(IntStream.range(0, bumps.size())
                                        .mapToDouble( i -> ALPHA * BOUND_2D_INTERPOLATOR_ONE.interpolate(option.expiry(), moneyNess.get(i))));
    DoubleArray newVols = DoubleArray.of(volFactors.stream().map(i -> vol*(1. + i)));
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
    DoubleArray volFactors = DoubleArray.of(alphaOne.stream().map(x -> { double factor = worstScenario.worstVolFactor + cOneVolFactor * x;
                                                                         if(factor > 1.) 
                                                                           return 1.;
                                                                         if(factor < -0.7) 
                                                                           return -0.7;
                                                                         return factor; }));
    DoubleArray newVols = DoubleArray.of( volFactors.stream().map( x -> vol * (1 + x)));
    DoubleArray newPrices = DoubleArray.of(newVols.stream().map(x -> option.calculate( worstScenario.worstSpot, x, rate)));
    DoubleArray profitAndLoss = DoubleArray.of(newPrices.stream().map(i -> option.quantity() *(i - worstScenario.originalPrice)));
    return option.notional() * Math.abs(profitAndLoss.min());    
  }
  
  public static double determineConcentrationRequirement(Option option, double originalPrice, double spot, double vol, double rate){
    final double[] stockStresses = {0.50, 0., -0.50};
    final double[][] volStresses = {{-0.50, -0.40, 0.}, {-0.50, 0.40, 0.}, {0., 0.50}};
    double worsePNL = 0.;
      for(int i = 0; i < stockStresses.length; ++i){
      double stockStress = stockStresses[i];
      double[] volStressForCurrentStockStress = volStresses[i];
      for(int j = 0; j < volStressForCurrentStockStress.length; ++j){
        double newPNL = option.quantity() * (option.calculate( spot * (1 + stockStress), rate, vol*(1 + volStressForCurrentStockStress[j])) - originalPrice);
        if(newPNL < worsePNL)
          worsePNL = newPNL;
      }
    }
    return option.notional() * Math.abs(worsePNL);
  }
  
  public static void main(String[] args){
    double strike = 290.;
    double expiry =  87./360.;
    double dividend = 0.0175;
    double spot = 284.64;
    double rate = 0.03;
    double vol = 0.0967;
    
    Option singleOption = new AmericanOptionWithContinuousYield(-96., 100., strike, expiry, PutCall.CALL, dividend);
    System.out.println();
    WorseCaseScenario wmls = determineWorseMarketLevelScenario(singleOption, spot, vol, rate);
    System.out.println("Worst Market Level Scenario has Alpha: " + ALPHA + ", Delta Spot: " + wmls.worstSpotBump + " and Delta Vol: " + wmls.worstVolFactor);
    double ss = determineSystematicStress(wmls, singleOption, vol, rate);
    System.out.println("Systematic Stress: " + ss);
    double cr = determineConcentrationRequirement(singleOption, wmls.originalPrice, spot, vol, rate);
    System.out.println("Concentration Requirement: " + cr);
  }
}
