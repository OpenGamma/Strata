package com.opengamma.strata.pricer.impl.finitedifference;

public interface ConvectionDiffusionPde1dCoefficients {

  public double getA(double t, double x);

  public double getB(double t, double x);

  public double getC(double t, double x);

}
