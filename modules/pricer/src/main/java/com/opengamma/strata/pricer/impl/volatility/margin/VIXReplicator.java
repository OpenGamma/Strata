/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import com.opengamma.strata.collect.array.DoubleArray;

public class VIXReplicator {
  public static final double[] STRIKES = {775, 800, 825, 850, 875, 900, 925, 950, 975, 1000, 1025};
  public static final double[][] NEAREST = {{125.48, 100.79, 76.7, 45.01, 34.05, 18.41, 8.07, 2.68, 0.62, 0.09, 0.01},
                                            {0.11, 0.41, 1.30, 3.60, 8.64, 17.98, 32.63, 52.23, 75.16, 99.61, 124.52}};
  public static final double[][] NEXT_NEAREST = {{128.78, 105.85, 84.14, 64.13, 46.38, 31.40, 19.57, 11., 5.43, 2.28, 0.78},
                                                 {2.72, 4.76, 8.01, 12.97, 20.18, 30.17, 43.31, 59.70, 79.10, 100.981, 4.38}};
  public static final double RISK_FREE_RATE = 0.01162;
  final static LocalTime MIDNIGHT = LocalTime.MIDNIGHT;
  
  private static double[] computePriceDifference( double[][] callsAndPuts ){
    double[] calls = callsAndPuts[0];
    double[] puts = callsAndPuts[1];
    return IntStream.range(0, calls.length)
                    .mapToDouble(i -> Math.abs(calls[i] - puts[i]))
                    .toArray();    
  }
  
  private static double timeToMaturity(double numDays, LocalTime calcTime){
    double hour = calcTime.getHour();
    double minute = calcTime.getMinute();
    double t = hour + minute / 60;
    return ((24 - t) * 60 + 510 + 60 * 24 * numDays) / 525600;
  }
  
  private static double determineF(double[] strikes, double[][] callsAndPuts, double rate, double expiry){
    DoubleArray priceDifferences = DoubleArray.ofUnsafe(computePriceDifference(callsAndPuts));    
    int location = priceDifferences.indexOf(priceDifferences.min());
    return strikes[location] + priceDifferences.get(location) * Math.exp(rate * expiry);
    
  }
  
  private static double determineKZero(double[] strikes, double forwardIndex){
    DoubleArray differences = DoubleArray.of(IntStream.range(0, strikes.length)
                                          .mapToDouble(i -> forwardIndex - strikes[i])
                                          .filter( i-> i > 0));
    int location = differences.indexOf(differences.min());
    return strikes[location];
  }
  
  private static double[] filterPrices(double[] strikes, double[][] callsAndPuts, double kZero){
    double[] filteredPuts = IntStream.range(0, strikes.length)
                                     .filter( i-> strikes[i] <= kZero)
                                     .mapToDouble(i -> callsAndPuts[1][i])
                                     .toArray();
    double[] filteredCalls =  IntStream.range(0, strikes.length)
                                       .filter(i-> strikes[i] >= kZero)
                                       .mapToDouble(i -> callsAndPuts[0][i])                                       
                                       .toArray();
    double[] concatenatedPut = IntStream.range(0, filteredPuts.length)
                                        .filter(i -> i < filteredPuts.length - 1)
                                        .mapToDouble(i ->filteredPuts[i])                                        
                                        .toArray();
    filteredCalls[0] = 0.5 * (filteredCalls[0] + filteredPuts[filteredPuts.length - 1]);  
  
    return DoubleStream.concat(Arrays.stream(concatenatedPut), Arrays.stream(filteredCalls))
                       .toArray();
  }
  
  private static double subVariance(double[] strikes, double[] filteredPrices, double forward, double kZero, double riskFreeRate, double expiry){
    double[] strikeDifferences = IntStream.range(0, strikes.length)
        .mapToDouble(i -> {
          if(i == 0)
            return strikes[1]-strikes[0];
          if(i == strikes.length-1)
            return strikes[strikes.length-1] - strikes[strikes.length-2];            
          return 0.5 * (strikes[i + 1] - strikes[i - 1]);})
        .toArray();
    double vixContributionsSummed = IntStream.range(0, strikes.length)
                                             .mapToDouble(i -> (2 * Math.exp(riskFreeRate * expiry) * filteredPrices[i] * strikeDifferences[i]) / Math.pow(strikes[i], 2) / expiry)
                                             .sum();
    return vixContributionsSummed - Math.pow( forward / kZero - 1, 2) / expiry;
  }
  
  private static  double vix(double varOne, double varTwo, LocalTime calcTime, double nDaysOne, double nDaysTwo){
    double hour = calcTime.getHour();
    double minute = calcTime.getMinute();
    double t = hour + minute / 60;
    double nTOne = ((24 - t) * 60 + 510 + 60 * 24 * nDaysOne);
    double nTTwo = ((24 - t) * 60 + 510 + 60 * 24 * nDaysTwo);
    double tOne = nTOne / 525600;
    double tTwo = nTTwo / 525600;
    double factorOne = tOne * varOne * (nTTwo - 43200) / (nTTwo - nTOne);
    double factorTwo = tTwo * varTwo * (43200 - nTOne) / (nTTwo - nTOne);
    return Math.sqrt( (factorOne + factorTwo) * 365 /30) * 100;
  }
  
  public static void main(String args[]){
    double expiryTimeOne = timeToMaturity(14, LocalTime.of(8, 30));
    double forwardOne = determineF(STRIKES, NEAREST, RISK_FREE_RATE, expiryTimeOne);
    double kZeroOne = determineKZero(STRIKES, forwardOne);
    double[] testPricesOne = filterPrices(STRIKES, NEAREST, kZeroOne);
    double varOne = subVariance(STRIKES, testPricesOne, forwardOne, kZeroOne, RISK_FREE_RATE, expiryTimeOne);
  
    double expiryTimeTwo = timeToMaturity(42, LocalTime.of(8, 30));
    double forwardTwo = determineF(STRIKES, NEXT_NEAREST, RISK_FREE_RATE, expiryTimeTwo);
    double kZeroTwo = determineKZero(STRIKES, forwardTwo);
    double[] testPricesTwo = filterPrices(STRIKES, NEXT_NEAREST, kZeroTwo);
    double varTwo = subVariance(STRIKES, testPricesTwo, forwardTwo, kZeroTwo, RISK_FREE_RATE, expiryTimeTwo);
   
    double vixCalc = vix(varOne, varTwo, LocalTime.of(8, 30), 14, 42);
    //Replication of VIX index Calculation from CBOE (2003)
    System.out.println("VIX Replication:" + vixCalc);
  }
}
