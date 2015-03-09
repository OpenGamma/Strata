/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity.multicurve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MulticurveSensitivity3LD {
  
  private List<ZeroRateSensitivityLD> zeroRateSensitivities;
  private final List<ForwardRateSensitivityLD> forwardRateSensitivities;

  public MulticurveSensitivity3LD() {
    zeroRateSensitivities = new ArrayList<ZeroRateSensitivityLD>();
    forwardRateSensitivities = new ArrayList<ForwardRateSensitivityLD>();
  }
  
  public MulticurveSensitivity3LD(
      List<ZeroRateSensitivityLD> zeroRateSensitivities, 
      List<ForwardRateSensitivityLD> forwardRateSensitivities) {
    this.zeroRateSensitivities = zeroRateSensitivities;
    this.forwardRateSensitivities = forwardRateSensitivities;
  }
  
  public static MulticurveSensitivity3LD ofZeroRate(List<ZeroRateSensitivityLD> zeroRateSensitivities) {
    return new MulticurveSensitivity3LD(zeroRateSensitivities, new ArrayList<ForwardRateSensitivityLD>());
  }
  
  public static MulticurveSensitivity3LD ofForwardRate(List<ForwardRateSensitivityLD> forwardRateSensitivities) {
    return new MulticurveSensitivity3LD(new ArrayList<ZeroRateSensitivityLD>(), forwardRateSensitivities);
  }
  
  public void add(MulticurveSensitivity3LD sensitivity) {
    zeroRateSensitivities.addAll(sensitivity.zeroRateSensitivities);
    forwardRateSensitivities.addAll(sensitivity.forwardRateSensitivities);
  }
  
  // TODO: do we need a "plus"?
  
  public MulticurveSensitivity3LD multipliedBy(double factor) {
    List<ZeroRateSensitivityLD> zeroRateSensitivitiesMultiplied = new ArrayList<ZeroRateSensitivityLD>();
    for(ZeroRateSensitivityLD zr: zeroRateSensitivities) {
      zeroRateSensitivitiesMultiplied.add(new ZeroRateSensitivityLD(zr.getCurrencyDiscount(), zr.getDate(), 
          zr.getValue() * factor, zr.getCurrencySensitivity()));
    }
    List<ForwardRateSensitivityLD> forwardRateSensitivitiesMultiplied = new ArrayList<ForwardRateSensitivityLD>();
    for(ForwardRateSensitivityLD fr: forwardRateSensitivities) {
      forwardRateSensitivitiesMultiplied.add(new ForwardRateSensitivityLD(fr.getIndex(), fr.getFixingDate(), 
          fr.getValue() * factor, fr.getCurrency()));
    }
    return new MulticurveSensitivity3LD(zeroRateSensitivitiesMultiplied, forwardRateSensitivitiesMultiplied);
  }
  
  public List<ZeroRateSensitivityLD> getZeroRateSensitivities() {
    return zeroRateSensitivities;
  }

  public List<ForwardRateSensitivityLD> getForwardRateSensitivities() {
    return forwardRateSensitivities;
  }
  
  public void cleaned() {
    Collections.sort(zeroRateSensitivities);
    List<ZeroRateSensitivityLD> zeroRateSensitivityCleaned;
    if(zeroRateSensitivities.size() == 0) {
      zeroRateSensitivityCleaned = zeroRateSensitivities;
    } else {
      zeroRateSensitivityCleaned = new ArrayList<ZeroRateSensitivityLD>();
      ZeroRateSensitivityLD zrLast =  zeroRateSensitivities.get(0);
      zeroRateSensitivityCleaned.add(zrLast);
      int loopzrc = 0;
      for(int loopzr=1; loopzr<zeroRateSensitivities.size(); loopzr++)  {
        if(zeroRateSensitivities.get(loopzr).compareTo(zrLast) == 0) {
          zeroRateSensitivityCleaned.set(loopzrc, new ZeroRateSensitivityLD(zrLast.getCurrencyDiscount(), zrLast.getDate(), 
              zrLast.getValue() + zeroRateSensitivities.get(loopzr).getValue(), zrLast.getCurrencySensitivity()));
        } else {
          zeroRateSensitivityCleaned.add(zeroRateSensitivities.get(loopzr));
          zrLast = zeroRateSensitivities.get(loopzr);
          loopzrc++;
        }
      }
    }
    zeroRateSensitivities = zeroRateSensitivityCleaned;
    Collections.sort(forwardRateSensitivities);
    List<ForwardRateSensitivityLD> forwardRateSensitivityCleaned;
    if(zeroRateSensitivities.size() == 0) {
      forwardRateSensitivityCleaned = forwardRateSensitivities;
    } else {
      forwardRateSensitivityCleaned = new ArrayList<ForwardRateSensitivityLD>();
      ForwardRateSensitivityLD frLast =  forwardRateSensitivities.get(0);
      forwardRateSensitivityCleaned.add(frLast);
      int loopzrc = 0;
      for(int loopfr=1; loopfr<forwardRateSensitivities.size(); loopfr++)  {
        if(forwardRateSensitivities.get(loopfr).compareTo(frLast) == 0) {
          forwardRateSensitivityCleaned.set(loopzrc, new ForwardRateSensitivityLD(frLast.getIndex(), 
              frLast.getFixingDate(),
              frLast.getValue() + forwardRateSensitivities.get(loopfr).getValue(), frLast.getCurrency()));
        } else {
          forwardRateSensitivityCleaned.add(forwardRateSensitivities.get(loopfr));
          frLast = forwardRateSensitivities.get(loopfr);
          loopzrc++;
        }
      }
    }
    zeroRateSensitivities = zeroRateSensitivityCleaned;
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
