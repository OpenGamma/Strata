package com.opengamma.strata.pricer.impl.finitedifference;


public interface ConvectionDiffusionPde1dCoefficients {

  abstract double getA(double t, double x);

  abstract double getB(double t, double x);

  abstract double getC(double t, double x);

}
