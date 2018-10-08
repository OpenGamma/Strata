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
import java.util.stream.Collectors;

import com.google.common.io.Files;
import com.opengamma.strata.collect.io.CsvFile;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.pricer.impl.option.BlackScholesFormulaRepository;

public class VIXIndex {
      
  private CsvFile nearOptionsCSV;
  private CsvFile farOptionsCSV;
  
  VIXIndex(String nearOptionsFilename,
           String farOptionsFilename){
    this.nearOptionsCSV = CsvFile.of(Files.asCharSource(new File(nearOptionsFilename), StandardCharsets.UTF_8), true);
    this.farOptionsCSV =  CsvFile.of(Files.asCharSource(new File(farOptionsFilename), StandardCharsets.UTF_8), true);
  }
  
  private CallsAndPutsPricesHolder formCallAndPutHolder(CsvFile csv, double spot, double rate){
    List<CsvRow> calls =  csv.rows().stream().filter(x->x.getValue("Type").equals("Call")).collect(Collectors.toList());
    List<CsvRow> puts =  csv.rows().stream().filter(x->x.getValue("Type").equals("Put")).collect(Collectors.toList());
    assert(calls.size() == puts.size());
    
    double daysToExpiry = Double.parseDouble(calls.get(0).getValue("DTE"));
    
    double[] strikes = calls.stream().mapToDouble(x->Double.parseDouble(x.getValue("Strike"))).toArray();
    double[] callPrices = calls.stream()
        .mapToDouble(x -> BlackScholesFormulaRepository.price(
            spot,
            Double.parseDouble(x.getValue("Strike")),
            Double.parseDouble(x.getValue("DTE")) / 360.,
            Double.parseDouble(x.getValue("IV")),
            rate,
            rate,
            true)).toArray();
    double[] putPrices = puts.stream()
        .mapToDouble(x -> BlackScholesFormulaRepository.price(
            spot,
            Double.parseDouble(x.getValue("Strike")),
            Double.parseDouble(x.getValue("DTE")) / 360.,
            Double.parseDouble(x.getValue("IV")),
            rate,
            rate,
            false)).toArray();
    double[][] callsAndPuts = {strikes, callPrices, putPrices};
    
    return new CallsAndPutsPricesHolder(callsAndPuts, daysToExpiry);
  }
  
  public double calculate(double spot, double rate, LocalTime calcTime){
    CallsAndPutsPricesHolder nearHolder = formCallAndPutHolder(nearOptionsCSV, spot, rate);
    CallsAndPutsPricesHolder farHolder = formCallAndPutHolder(farOptionsCSV, spot, rate);
    return VIXIndexCalculator.vix(nearHolder, farHolder, calcTime, rate);
  }
  
  
  
  public static void main(String[] args){ 
  
    VIXIndex vix = new VIXIndex("/Users/richardweeks/Downloads/$spx-options-exp-2018-10-29-show-all-stacked-10-04-2018.csv",
                                "/Users/richardweeks/Downloads/$spx-options-exp-2018-11-07-show-all-stacked-10-04-2018.csv");
    double rate  = 0.03;
    double spot = 2900.83;
    System.out.println(vix.calculate(spot, rate, LocalTime.of(8, 30)));
  }
}
