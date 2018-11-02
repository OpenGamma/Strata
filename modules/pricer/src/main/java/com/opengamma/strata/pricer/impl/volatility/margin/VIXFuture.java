/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.margin;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.google.common.io.Files;
import com.opengamma.strata.collect.io.CsvFile;

public class VIXFuture implements Product {
  
  private double  median;
  private double expiry;
  private double quantity;
  private double multiplier;
  
  VIXFuture(CsvFile historicFiles,
            double expiry,
            double quantity,
            double multiplier){
    double[] historicValues = historicFiles.rows()
        .reverse()
        .stream()
        .mapToDouble(x -> Double.parseDouble(x.getValue("Close")))
        .toArray();
    this.median = median(historicValues);
    this.expiry = expiry;
    this.quantity = quantity;
    this.multiplier = multiplier;
  }
  
  VIXFuture(double median,
            double expiry,
            double quantity,
            double multiplier){
    this.median = median;
    this.expiry = expiry;
    this.quantity = quantity;
    this.multiplier = multiplier;
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
  
  public double calculate(double currentValue){
    return currentValue + (1 - currentValue / median) * Math.log(1 + expiry) + 0.21 * Math.sqrt(expiry);
  }
  
  public double quantity() {
    return quantity;
  }
  
  public double multiplier() {
    return multiplier;
  }
  
  public static void main(String[] args){
    //Actual Historic Values
    CsvFile historicCSVFile = CsvFile.of(Files.asCharSource(new File("/Users/richardweeks/Downloads/vix1.csv"), StandardCharsets.UTF_8), true);
    
    //Current Value of VIX as of 3/10/2018 11.61
    //Prediction of VIX for 100 days expiration
    VIXFuture vixFuture = new VIXFuture(historicCSVFile, 75, 75, 100);
    System.out.println(vixFuture.calculate(16));
  }
}
