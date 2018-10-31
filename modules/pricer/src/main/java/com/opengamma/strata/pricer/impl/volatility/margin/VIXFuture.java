/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.IntStream;

import com.google.common.io.Files;
import com.opengamma.strata.collect.io.CsvFile;

public class VIXFuture {
  
  private CsvFile historicFiles;
  
  VIXFuture(CsvFile historicFiles){
    this.historicFiles = historicFiles;
  }
  
  private static double median(double[] values){
    Arrays.sort(values);
    double median = 0;
    double pos1 = Math.floor((values.length - 1.0) / 2.0);
    double pos2 = Math.ceil((values.length - 1.0) / 2.0);
    if (pos1 == pos2 ) {
      median = values[(int) pos1];
    } else {
      median = (values[(int) pos1] + values[(int) pos2]) / 2.0 ;
    }
    return median;
  }
  
  private static double determineVIXEMA(double[] values, double currentValue){
    return 0.5 * currentValue + IntStream.range(0, values.length).mapToDouble(x -> Math.pow(0.5, x + 2) * values[x]).sum();
  }
  
  private static double determineVIXRef(double currentValue, double emaValue, double daysToExpiry){
    return currentValue - (currentValue - emaValue) * Math.log(daysToExpiry + 1) / 4.5;
  }
  
  public double calculate(double currentValue, double daysToExpiry){
    double[] historicValues = historicFiles.rows()
        .reverse()
        .stream()
        .mapToDouble(x -> Double.parseDouble(x.getValue("Close")))
        .toArray();
    double vMedian = median(historicValues);
    double vEMA = determineVIXEMA(historicValues, currentValue);
    double vRef = determineVIXRef(currentValue, vEMA, daysToExpiry);
    return vRef + (1 - currentValue / vMedian) * Math.log(1 + daysToExpiry) + 0.21 * Math.sqrt(daysToExpiry);
  }
  
  
  public static void main(String[] args){
    //Test the median
    System.out.println(median(new double[]{1, 2, 3}));
    System.out.println(median(new double[]{2, 1, 6, 3, 10, 5, 8}));
    System.out.println(median(new double[]{1, 4, 2, 3}));
    
    //Test exponential weighted average
    System.out.println(determineVIXEMA(new double[]{2, 3, 4}, 1));
    System.out.println(0.5 * 1 + 0.5 * 0.5 * 2 + 0.5 * 0.5 * 0.5 * 3 + 0.5 * 0.5 * 0.5 * 0.5 * 4);//1.625
    
    //Test VIX Ref
    double ema = determineVIXEMA(new double[]{2, 3, 4}, 1);
    double ref = determineVIXRef(1, ema, 10);
    System.out.println(ref);
    System.out.println(1 - ((1 - 1.625) * Math.log( 11))/4.5);
  
    //Actual Historic Values
    CsvFile historicCSVFile = CsvFile.of(Files.asCharSource(new File("/Users/richardweeks/Downloads/vix1.csv"), StandardCharsets.UTF_8), true);
    
    //Current Value of VIX as of 3/10/2018 11.61
    //Prediction of VIX for 100 days expiration
    VIXFuture vixFuture = new VIXFuture(historicCSVFile);
    System.out.println(vixFuture.calculate(13.61, 30));
  }
}
