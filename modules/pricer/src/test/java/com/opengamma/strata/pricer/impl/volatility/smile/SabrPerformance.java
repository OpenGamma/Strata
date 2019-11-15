/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import java.util.stream.IntStream;

import com.opengamma.strata.basics.value.ValueDerivatives;

/**
 * Test performance.
 */
public class SabrPerformance {

  public static void main(String[] args) {
    double[] betas = new double[] {0.5, 0.75, 1.0, 1.25, 1.5};
    for (int i = 0; i < betas.length; i++) {
      double beta = betas[i];
      System.out.println("Beta = " + beta);
      for (int j = 0; j < 5; j++) {
        double time = sabrVolatility(beta);
        System.out.println(time + "ms");
      }
      double hot1 = IntStream.range(0, 1000)
          .mapToDouble(index -> sabrVolatility(beta))
          .average()
          .getAsDouble();
      System.out.println(hot1 + "ms (hot)");
      System.out.println();
    }

    for (int i = 0; i < betas.length; i++) {
      double beta = betas[i];
      System.out.println("Beta = " + beta);
      for (int j = 0; j < 5; j++) {
        double time = sabrAdjoint(beta);
        System.out.println(time + "ms");
      }
      double hot1 = IntStream.range(0, 1000)
          .mapToDouble(index -> sabrAdjoint(beta))
          .average()
          .getAsDouble();
      System.out.println(hot1 + "ms (hot)");
      System.out.println();
    }
  }

  private static double sabrVolatility(double beta) {
    SabrHaganVolatilityFunctionProvider provider = SabrHaganVolatilityFunctionProvider.DEFAULT;
    Data cp = new Data();
    double total = 0;
    long start = System.nanoTime();
    for (int i = 0; i < cp.strikes.length; i++) {
      double strike = cp.strikes[i];
      double eval = provider.volatility(cp.forward, strike, cp.tau, cp.alpha, beta, cp.rho, cp.nu);
      total += eval;
    }
    long end = System.nanoTime();
    if (total == 0) {
      return -1;
    }
    return (end - start) / 1_000_000d;
  }

  private static double sabrAdjoint(double beta) {
    SabrHaganVolatilityFunctionProvider provider = SabrHaganVolatilityFunctionProvider.DEFAULT;
    Data cp = new Data();
    double total = 0;
    long start = System.nanoTime();
    for (int i = 0; i < cp.strikes.length; i++) {
      double strike = cp.strikes[i];
      ValueDerivatives eval = provider.volatilityAdjoint(cp.forward, strike, cp.tau, cp.alpha, beta, cp.rho, cp.nu);
      total += eval.getValue();
    }
    long end = System.nanoTime();
    if (total == 0) {
      return -1;
    }
    return (end - start) / 1_000_000d;
  }

  static class Data {
    private int cnt = 10;
    private double forward = 100.4456433578360;
    private double tau = 0.01917808219;
    private double strikeLb = 99.1;
    private double strikeUb = 101.7;
    private double[] strikes = createStrikes(strikeLb, strikeUb, 1 << cnt);
    private double alpha = 0.11535269852484416;
    private double nu = 4.249484906629612;
    private double rho = -0.08280305920343885;

    double[] createStrikes(double lb, double ub, int cnt) {
      double incr = (ub - lb) / cnt;
      double[] res = new double[cnt];
      int i = 0;
      while (i < cnt) {
        res[i] = lb + incr * i;
        i += 1;
      }
      return res;
    }
  }
}
