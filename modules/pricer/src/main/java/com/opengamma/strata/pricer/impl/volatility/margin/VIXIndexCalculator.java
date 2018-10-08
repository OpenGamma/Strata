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

public class VIXIndexCalculator {
  final static LocalTime MIDNIGHT = LocalTime.MIDNIGHT;
  
  private static double timeToMaturity(double numDays, LocalTime calcTime){
    double hour = calcTime.getHour();
    double minute = calcTime.getMinute();
    double t = hour + minute / 60;
    return ((24 - t) * 60 + 510 + 60 * 24 * numDays) / 525600;
  }
  
  private static double determineF(CallsAndPutsPricesHolder callsAndPuts, double rate, double expiry){
    DoubleArray priceDifferences = DoubleArray.of(IntStream.range(0, callsAndPuts.getCalls().length)
                                                           .mapToDouble(i -> Math.abs(callsAndPuts.getCalls()[i] - callsAndPuts.getPuts()[i])));
    int location = priceDifferences.indexOf(priceDifferences.min());
    return callsAndPuts.getStrikes()[location] + priceDifferences.get(location) * Math.exp(rate * expiry);
  }
  
  private static double determineKZero(double[] strikes, double forwardIndex){
    DoubleArray differences = DoubleArray.of(IntStream.range(0, strikes.length)
                                          .mapToDouble(i -> forwardIndex - strikes[i])
                                          .filter( i -> i > 0));
    int location = differences.indexOf(differences.min());
    return strikes[location];
  }
  
  private static double[][] filterPrices(CallsAndPutsPricesHolder callsAndPuts, double kZero){
    double[] filteredPuts = IntStream.range(0, callsAndPuts.getStrikes().length)
                                     .filter( i-> callsAndPuts.getStrikes()[i] <= kZero)
                                     .mapToDouble(i -> callsAndPuts.getPuts()[i])
                                     .toArray();
    double[] filteredCalls =  IntStream.range(0, callsAndPuts.getStrikes().length)
                                       .filter(i-> callsAndPuts.getStrikes()[i] >= kZero)
                                       .mapToDouble(i -> callsAndPuts.getCalls()[i])                                       
                                       .toArray();
    double[] concatenatedPut = IntStream.range(0, filteredPuts.length)
                                        .filter(i -> i < filteredPuts.length - 1)
                                        .mapToDouble(i ->filteredPuts[i])                                        
                                        .toArray();
    filteredCalls[0] = 0.5 * (filteredCalls[0] + filteredPuts[filteredPuts.length - 1]);
    double[][] filteredPrices = {callsAndPuts.getStrikes(), DoubleStream.concat(Arrays.stream(concatenatedPut),
                                                                                Arrays.stream(filteredCalls))
                                                                        .toArray() };
  
    return filteredPrices;
  }
  
  private static double subVariance(double[][] filteredPrices, double forward, double kZero, double riskFreeRate, double expiry){
    double[] strikes = filteredPrices[0];
    double[] prices = filteredPrices[1];
    double[] strikeDifferences = IntStream.range(0, strikes.length)
        .mapToDouble(i -> {
          if(i == 0)
            return strikes[1]-strikes[0];
          if(i == strikes.length-1)
            return strikes[strikes.length-1] - strikes[strikes.length-2];            
          return 0.5 * (strikes[i + 1] - strikes[i - 1]);})
        .toArray();
    double vixContributionsSummed = IntStream.range(0, strikes.length)
                                             .mapToDouble(i -> (2 * Math.exp(riskFreeRate * expiry) * prices[i] * strikeDifferences[i]) / Math.pow(strikes[i], 2) / expiry)
                                             .sum();
    return vixContributionsSummed - Math.pow(forward / kZero - 1, 2) / expiry;
  }
  
  public static double vix(CallsAndPutsPricesHolder nearestOptions, CallsAndPutsPricesHolder furthestOptions, LocalTime calcTime, double rate){
    double expiryTimeOne = timeToMaturity(nearestOptions.daysToExpiry(), calcTime);
    double forwardOne = determineF(nearestOptions, rate, expiryTimeOne);
    double kZeroOne = determineKZero(nearestOptions.getStrikes(), forwardOne);
    double[][] testPricesOne = filterPrices(nearestOptions, kZeroOne);
    double varOne = subVariance(testPricesOne, forwardOne, kZeroOne, rate, expiryTimeOne);
  
    double expiryTimeTwo = timeToMaturity(furthestOptions.daysToExpiry(), calcTime);
    double forwardTwo = determineF(furthestOptions, rate, expiryTimeTwo);
    double kZeroTwo = determineKZero(furthestOptions.getStrikes(), forwardTwo);
    double[][] testPricesTwo = filterPrices(furthestOptions, kZeroTwo);
    double varTwo = subVariance(testPricesTwo, forwardTwo, kZeroTwo, rate, expiryTimeTwo);
  
    double hour = calcTime.getHour();
    double minute = calcTime.getMinute();
    double t = hour + minute / 60;
    double nTOne = ((24 - t) * 60 + 510 + 60 * 24 * nearestOptions.daysToExpiry());
    double nTTwo = ((24 - t) * 60 + 510 + 60 * 24 * furthestOptions.daysToExpiry());
    double tOne = nTOne / 525600;
    double tTwo = nTTwo / 525600;
    double factorOne = tOne * varOne * (nTTwo - 43200) / (nTTwo - nTOne);
    double factorTwo = tTwo * varTwo * (43200 - nTOne) / (nTTwo - nTOne);
    return Math.sqrt( (factorOne + factorTwo) * 365 /30) * 100;
  }
  
  public static void main(String args[]){
    double[][] nearestOptions = {
        {775, 800, 825, 850, 875, 900, 925, 950, 975, 1000, 1025},
        {125.48, 100.79, 76.7, 45.01, 34.05, 18.41, 8.07, 2.68, 0.62, 0.09, 0.01},
        {0.11, 0.41, 1.30, 3.60, 8.64, 17.98, 32.63, 52.23, 75.16, 99.61, 124.52}};
    double[][] farthestOptions = {
        {775, 800, 825, 850, 875, 900, 925, 950, 975, 1000, 1025},
        {128.78, 105.85, 84.14, 64.13, 46.38, 31.40, 19.57, 11., 5.43, 2.28, 0.78},
        {2.72, 4.76, 8.01, 12.97, 20.18, 30.17, 43.31, 59.70, 79.10, 100.981, 4.38}};
    double rate = 0.01162;
    
    CallsAndPutsPricesHolder nearOptionsHolder = new CallsAndPutsPricesHolder(nearestOptions, 14);
    CallsAndPutsPricesHolder farOptionsHolder = new CallsAndPutsPricesHolder(farthestOptions, 42);
    double vixCalc = vix(nearOptionsHolder, farOptionsHolder, LocalTime.of(8, 30), rate);
   
    //Replication of VIX index Calculation from CBOE (2003)
    System.out.println();
    System.out.println("VIX Replication Against CBOE (2003 Paper). Expected: 25.361" + ", Actual: " + vixCalc);    
  }
}
