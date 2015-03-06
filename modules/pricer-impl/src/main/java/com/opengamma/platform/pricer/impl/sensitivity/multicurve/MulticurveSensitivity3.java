/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.sensitivity.multicurve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MulticurveSensitivity3 {
  
  private List<ZeroRateSensitivity> zeroRateSensitivities;
  private List<ForwardRateSensitivity> forwardRateSensitivities;

  public MulticurveSensitivity3() {
    zeroRateSensitivities = new ArrayList<ZeroRateSensitivity>();
    forwardRateSensitivities = new ArrayList<ForwardRateSensitivity>();
  }
  
  public MulticurveSensitivity3(
      List<ZeroRateSensitivity> zeroRateSensitivities, 
      List<ForwardRateSensitivity> forwardRateSensitivities) {
    this.zeroRateSensitivities = zeroRateSensitivities;
    this.forwardRateSensitivities = forwardRateSensitivities;
  }
  
  public static MulticurveSensitivity3 ofZeroRate(List<ZeroRateSensitivity> zeroRateSensitivities) {
    return new MulticurveSensitivity3(zeroRateSensitivities, new ArrayList<ForwardRateSensitivity>());
  }
  
  public static MulticurveSensitivity3 ofForwardRate(List<ForwardRateSensitivity> forwardRateSensitivities) {
    return new MulticurveSensitivity3(new ArrayList<ZeroRateSensitivity>(), forwardRateSensitivities);
  }
  
  public void add(MulticurveSensitivity3 sensitivity) {
    zeroRateSensitivities.addAll(sensitivity.zeroRateSensitivities);
    forwardRateSensitivities.addAll(sensitivity.forwardRateSensitivities);
  }
  
  // TODO: do we need a "plus"?
  
  public MulticurveSensitivity3 multipliedBy(double factor) {
    List<ZeroRateSensitivity> zeroRateSensitivitiesMultiplied = new ArrayList<ZeroRateSensitivity>();
    for(ZeroRateSensitivity zr: zeroRateSensitivities) {
      zeroRateSensitivitiesMultiplied.add(new ZeroRateSensitivity(zr.getCurveName(), zr.getTime(), 
          zr.getValue() * factor, zr.getCurrency()));
    }
    List<ForwardRateSensitivity> forwardRateSensitivitiesMultiplied = new ArrayList<ForwardRateSensitivity>();
    for(ForwardRateSensitivity fr: forwardRateSensitivities) {
      forwardRateSensitivitiesMultiplied.add(new ForwardRateSensitivity(fr.getCurveName(), fr.getFixingTime(),
          fr.getStartTime(), fr.getEndTime(), fr.getAccrualFactor(), fr.getValue() * factor, fr.getCurrency()));
    }
    return new MulticurveSensitivity3(zeroRateSensitivitiesMultiplied, forwardRateSensitivitiesMultiplied);
  }
  
  public List<ZeroRateSensitivity> getZeroRateSensitivities() {
    return zeroRateSensitivities;
  }

  public List<ForwardRateSensitivity> getForwardRateSensitivities() {
    return forwardRateSensitivities;
  }
  
  public MulticurveSensitivity3 cleaned() {
    Collections.sort(zeroRateSensitivities);
    List<ZeroRateSensitivity> zeroRateSensitivityCleaned;
    if(zeroRateSensitivities.size() == 0) {
      zeroRateSensitivityCleaned = zeroRateSensitivities;
    } else {
      zeroRateSensitivityCleaned = new ArrayList<ZeroRateSensitivity>();
      ZeroRateSensitivity zrLast =  zeroRateSensitivities.get(0);
      zeroRateSensitivityCleaned.add(zrLast);
      int loopzrc = 0;
      for(int loopzr=1; loopzr<zeroRateSensitivities.size(); loopzr++)  {
        if(zeroRateSensitivities.get(loopzr).compareTo(zrLast) == 0) {
          zeroRateSensitivityCleaned.set(loopzrc, new ZeroRateSensitivity(zrLast.getCurveName(), zrLast.getTime(), 
              zrLast.getValue() + zeroRateSensitivities.get(loopzr).getValue(), zrLast.getCurrency()));
        } else {
          zeroRateSensitivityCleaned.add(zeroRateSensitivities.get(loopzr));
          zrLast = zeroRateSensitivities.get(loopzr);
          loopzrc++;
        }
      }
    }
    Collections.sort(forwardRateSensitivities);
    List<ForwardRateSensitivity> forwardRateSensitivityCleaned;
    if(zeroRateSensitivities.size() == 0) {
      forwardRateSensitivityCleaned = forwardRateSensitivities;
    } else {
      forwardRateSensitivityCleaned = new ArrayList<ForwardRateSensitivity>();
      ForwardRateSensitivity frLast =  forwardRateSensitivities.get(0);
      forwardRateSensitivityCleaned.add(frLast);
      int loopzrc = 0;
      for(int loopfr=1; loopfr<forwardRateSensitivities.size(); loopfr++)  {
        if(forwardRateSensitivities.get(loopfr).compareTo(frLast) == 0) {
          forwardRateSensitivityCleaned.set(loopzrc, new ForwardRateSensitivity(frLast.getCurveName(), 
              frLast.getFixingTime(), frLast.getStartTime(), frLast.getEndTime(), frLast.getAccrualFactor(),
              frLast.getValue() + forwardRateSensitivities.get(loopfr).getValue(), frLast.getCurrency()));
        } else {
          forwardRateSensitivityCleaned.add(forwardRateSensitivities.get(loopfr));
          frLast = forwardRateSensitivities.get(loopfr);
          loopzrc++;
        }
      }
    }
    return new MulticurveSensitivity3(zeroRateSensitivityCleaned, forwardRateSensitivityCleaned);
//    zeroRateSensitivities = zeroRateSensitivityCleaned;
//    forwardRateSensitivities = forwardRateSensitivityCleaned;
  }

  @Override
  public String toString() {
    String msg = "MultiCurveSensitivity: \n";
    msg = msg + zeroRateSensitivities.toString() + "\n";
    msg = msg + forwardRateSensitivities.toString();
    return msg;
  }
  
  // Hash - equal?

}
