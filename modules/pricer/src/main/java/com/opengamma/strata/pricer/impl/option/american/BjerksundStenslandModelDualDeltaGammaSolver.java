/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option.american;

import com.opengamma.strata.math.impl.statistics.distribution.BivariateNormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

/**
 * Class computing dual-Delta and dual-Gamma of American option based on an analytical approximation by Bjerksund and Stensland (2002).  
 *
 * Delta and Gamma of puts are computed using the Bjerksund-Stensland put-call transformation
 * $p(S, K, T, r, b, \sigma) = c(K, S, T, r - b, -b, \sigma)$.
 *
 */

public class BjerksundStenslandModelDualDeltaGammaSolver {
  
  private static final double RHO2 = 0.5 * (Math.sqrt(5) - 1);
  private static final double RHO = Math.sqrt(RHO2);
  private static final double RHO_STAR = Math.sqrt(1 - RHO2);
  private static final ProbabilityDistribution<double[]> BIVARIATE_NORMAL = new BivariateNormalDistribution();
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);
  
  /**
   * Get the price of an American call option by the Bjerksund and Stensland (2002) approximation.
   * @param s0 The spot
   * @param k The strike
   * @param r The risk-free rate
   * @param b The cost-of-carry
   * @param t The time-to-expiry
   * @param sigma The volatility
   * @return American call option price, dual-Delta, dual-Gamma 
   */
  static double[] getCallDualDeltaGamma(final double s0, final double k, final double r, final double b, final double t, final double sigma) {
    
    final double[] res = new double[3];
    //    European option case
    if (b >= r) {
      final double expbt = Math.exp(b * t);
      final double fwd = s0 * expbt;
      final double df = Math.exp(-r * t);
      res[0] = df * BlackFormulaRepository.price(fwd, k, t, sigma, true);
      res[1] = df * BlackFormulaRepository.dualDelta(fwd, k, t, sigma, true);
      res[2] = df * BlackFormulaRepository.dualGamma(fwd, k, t, sigma);
      //      }
      return res;
    }
    
    final double sigmaSq = sigma * sigma;
    final double y = 0.5 - b / sigmaSq;
    final double beta = y + Math.sqrt(y * y + 2. * r / sigmaSq);
    
    final double[] b0 = new double[3];
    b0[0] = Math.max(k, r * k / Math.abs(r - b));
    b0[1] = Math.max(1., r / Math.abs(r - b));
    b0[2] = 0.;
    
    final double[] bInfinity = new double[3];
    bInfinity[0] = beta * k / (beta - 1);
    bInfinity[1] = beta / (beta - 1);
    bInfinity[2] = 0.;
    
    final double[] h2 = getHDualDeltaGamma(b, t, sigma, k, b0, bInfinity);
    final double[] x2 = getXDualDeltaGamma(b0, bInfinity, h2);
    
    //        early exercise
    if (s0 >= x2[0]) {
      res[0] = s0 - k;
      res[1] = -1.0;
      res[2] = 0.0;
      return res;
    }
    
    final double[] kDual = {k, 1., 0. };
    
    final double t1 = RHO2 * t;
    final double[] h1 = getHDualDeltaGamma(b, t1, sigma, k, b0, bInfinity);
    final double[] x1 = getXDualDeltaGamma(b0, bInfinity, h1);
    
    final double[] alpha1 = getAlphaDualDeltaGamma(x1, beta, k);
    final double[] alpha2 = getAlphaDualDeltaGamma(x2, beta, k);
    
    final double[] phi1 = getPhiDualDeltaGamma(s0, t, beta, x2, x2, r, b, sigma);
    final double[] phi2 = getPhiDualDeltaGamma(s0, t, 1.0, x2, x2, r, b, sigma);
    final double[] phi3 = getPhiDualDeltaGamma(s0, t, 1.0, x1, x2, r, b, sigma);
    final double[] phi4 = getPhiDualDeltaGamma(s0, t, 0.0, x2, x2, r, b, sigma);
    final double[] phi5 = getPhiDualDeltaGamma(s0, t, 0.0, x1, x2, r, b, sigma);
    final double[] phi6 = getPhiDualDeltaGamma(s0, t, beta, x1, x2, r, b, sigma);
    
    final double[] psi1 = getPsiDualDeltaGamma(s0, t, beta, x1, x2, x1, r, b, sigma);
    final double[] psi2 = getPsiDualDeltaGamma(s0, t, 1.0, x1, x2, x1, r, b, sigma);
    final double[] psi3 = getPsiDualDeltaGamma(s0, t, 1.0, kDual, x2, x1, r, b, sigma);
    final double[] psi4 = getPsiDualDeltaGamma(s0, t, 0.0, x1, x2, x1, r, b, sigma);
    final double[] psi5 = getPsiDualDeltaGamma(s0, t, 0.0, kDual, x2, x1, r, b, sigma);
    
    final double w1 = alpha2[0] * Math.pow(s0, beta);
    final double w2 = -alpha2[0] * phi1[0];
    final double w3 = +phi2[0];
    final double w4 = -phi3[0];
    final double w5 = -kDual[0] * phi4[0];
    final double w6 = +kDual[0] * phi5[0];
    final double w7 = +alpha1[0] * phi6[0];
    final double w8 = -alpha1[0] * psi1[0];
    final double w9 = psi2[0];
    final double w10 = -psi3[0];
    final double w11 = -kDual[0] * psi4[0];
    final double w12 = kDual[0] * psi5[0];
    final double w13 = w1 + w2 + w3 + w4 + w5 + w6 + w7 + w8 + w9 + w10 + w11 + w12;
    
    final double w1d = alpha2[1] * Math.pow(s0, beta);
    final double w2d = -alpha2[1] * phi1[0] - alpha2[0] * phi1[1];
    final double w3d = +phi2[1];
    final double w4d = -phi3[1];
    final double w5d = -kDual[1] * phi4[0] - kDual[0] * phi4[1];
    final double w6d = +kDual[1] * phi5[0] + kDual[0] * phi5[1];
    final double w7d = +alpha1[1] * phi6[0] + alpha1[0] * phi6[1];
    final double w8d = -alpha1[1] * psi1[0] - alpha1[0] * psi1[1];
    final double w9d = psi2[1];
    final double w10d = -psi3[1];
    final double w11d = -kDual[1] * psi4[0] - kDual[0] * psi4[1];
    final double w12d = kDual[0] * psi5[1] + kDual[1] * psi5[0];
    final double w13d = w1d + w2d + w3d + w4d + w5d + w6d + w7d + w8d + w9d + w10d + w11d + w12d;
    
    final double w1dd = alpha2[2] * Math.pow(s0, beta);
    final double w2dd = -alpha2[2] * phi1[0] - alpha2[0] * phi1[2] - alpha2[1] * phi1[1] - alpha2[1] * phi1[1];
    final double w3dd = +phi2[2];
    final double w4dd = -phi3[2];
    final double w5dd = -kDual[2] * phi4[0] - kDual[0] * phi4[2] - kDual[1] * phi4[1] - kDual[1] * phi4[1];
    final double w6dd = +kDual[2] * phi5[0] + kDual[0] * phi5[2] + kDual[1] * phi5[1] + kDual[1] * phi5[1];
    final double w7dd = +alpha1[2] * phi6[0] + alpha1[0] * phi6[2] + alpha1[1] * phi6[1] + alpha1[1] * phi6[1];
    final double w8dd = -alpha1[2] * psi1[0] - alpha1[0] * psi1[2] - alpha1[1] * psi1[1] - alpha1[1] * psi1[1];
    final double w9dd = psi2[2];
    final double w10dd = -psi3[2];
    final double w11dd = -kDual[2] * psi4[0] - kDual[0] * psi4[2] - kDual[1] * psi4[1] - kDual[1] * psi4[1];
    final double w12dd = kDual[1] * psi5[1] + kDual[1] * psi5[1] + kDual[0] * psi5[2] + kDual[2] * psi5[0];
    final double w13dd = w1dd + w2dd + w3dd + w4dd + w5dd + w6dd + w7dd + w8dd + w9dd + w10dd + w11dd + w12dd;
    
    res[0] = w13;
    res[1] = w13d;
    res[2] = w13dd;
    return res;
  }
  
  static double[] getPhiDualDeltaGamma(final double s, final double t, final double gamma, final double[] h, final double[] x, final double r, final double b,
      final double sigma) {
    
    final double t1 = RHO2 * t;
    final double sigmaSq = sigma * sigma;
    final double sigmaRootT = sigma * Math.sqrt(t1);
    
    final double lambda = -r + gamma * b + 0.5 * gamma * (gamma - 1) * sigmaSq; //lambda
    final double kappa = 2 * b / sigmaSq + 2 * gamma - 1;
    
    final double second = (b + (gamma - 0.5) * sigma * sigma) * t1;
    
    final double w1 = h[0];
    final double w2 = x[0];
    final double w3 = w2 * w2;
    final double w4 = (Math.log(s / w1) + second) / sigmaRootT;
    final double w5 = (Math.log(w3 / s / w1) + second) / sigmaRootT;
    final double w6 = NORMAL.getCDF(-w4);
    final double w7 = NORMAL.getCDF(-w5);
    final double w8 = Math.pow(w2 / s, kappa);
    final double w9 = w7 * w8;
    final double w10 = Math.exp(lambda * t1) * Math.pow(s, gamma) * (w6 - w9);
    
    final double w1d = h[1];
    final double w2d = x[1];
    final double w3d = 2. * w2 * w2d;
    final double w4d = -1. * w1d / w1 / sigmaRootT;
    final double w5d = (w3d / w3 - w1d / w1) / sigmaRootT;
    final double w6d = NORMAL.getPDF(-w4) * (-w4d);
    final double w7d = NORMAL.getPDF(-w5) * (-w5d);
    final double w8d = kappa * Math.pow(w2 / s, kappa - 1.) / s * w2d;
    final double w9d = w7d * w8 + w7 * w8d;
    final double w10d = Math.exp(lambda * t1) * Math.pow(s, gamma) * (w6d - w9d);
    
    final double w1dd = h[2];
    final double w2dd = x[2];
    final double w3dd = 2. * w2d * w2d + 2. * w2 * w2dd;
    final double w4dd = -1. * w1dd / w1 / sigmaRootT + 1. * w1d * w1d / w1 / w1 / sigmaRootT;
    final double w5dd = (w3dd / w3 - w1dd / w1) / sigmaRootT - (w3d * w3d / w3 / w3 - w1d * w1d / w1 / w1) / sigmaRootT;
    final double w6dd = NORMAL.getPDF(-w4) * (-w4dd) + NORMAL.getPDF(-w4) * (w4) * (-w4d) * (-w4d);
    final double w7dd = NORMAL.getPDF(-w5) * (-w5dd) + NORMAL.getPDF(-w5) * (w5) * (-w5d) * (-w5d);
    final double w8dd = kappa * Math.pow(w2 / s, kappa - 1.) / s * w2dd + kappa * (kappa - 1) * Math.pow(w2 / s, kappa - 2.) / s / s * w2d * w2d;
    final double w9dd = w7dd * w8 + w7 * w8dd + 2. * w7d * w8d;
    final double w10dd = Math.exp(lambda * t1) * Math.pow(s, gamma) * (w6dd - w9dd);
    
    final double[] res = new double[3];
    res[0] = w10;
    res[1] = w10d;
    res[2] = w10dd;
    
    return res;
  }
  
  static double[] getPsiDualDeltaGamma(final double s, final double t, final double gamma, final double[] h, final double[] x2, final double[] x1,
      final double r, final double b, final double sigma) {
    
    final double rootT = Math.sqrt(t);
    final double sigmarootT = sigma * rootT;
    final double t1 = RHO2 * t;
    final double rootT1 = RHO * rootT;
    final double sigmarootT1 = sigma * rootT1;
    final double sigmaSq = sigma * sigma;
    
    final double lambda = -r + gamma * b + 0.5 * gamma * (gamma - 1) * sigmaSq; //lambda
    final double kappa = 2 * b / sigmaSq + 2 * gamma - 1;
    
    final double second = (b + (gamma - 0.5) * sigma * sigma) * t;
    final double second1 = (b + (gamma - 0.5) * sigma * sigma) * t1;
    
    final double w1 = x1[0];
    final double w2 = x2[0];
    final double w3 = h[0];
    final double w4 = (Math.log(s / w1) + second1) / sigmarootT1;  //e1
    final double w5 = (Math.log(w2 * w2 / s / w1) + second1) / sigmarootT1;  //e2
    final double w6 = (Math.log(s / w1) - second1) / sigmarootT1;  //e3
    final double w7 = (Math.log(w2 * w2 / s / w1) - second1) / sigmarootT1;  //e4
    final double w8 = (Math.log(s / w3) + second) / sigmarootT;  //f1
    final double w9 = (Math.log(w2 * w2 / s / w3) + second) / sigmarootT;  //f2
    final double w10 = (Math.log(w1 * w1 / s / w3) + second) / sigmarootT;  //f3
    final double w11 = (Math.log(w1 * w1 * s / w3 / w2 / w2) + second) / sigmarootT;  //f4
    final double w12 = BIVARIATE_NORMAL.getCDF(new double[] {-w4, -w8, RHO });
    final double w13 = BIVARIATE_NORMAL.getCDF(new double[] {-w5, -w9, RHO });
    final double w14 = BIVARIATE_NORMAL.getCDF(new double[] {-w6, -w10, -RHO });
    final double w15 = BIVARIATE_NORMAL.getCDF(new double[] {-w7, -w11, -RHO });
    final double w16 = Math.pow(w1 / s, kappa);
    final double w17 = Math.pow(w2 / s, kappa);
    final double w18 = Math.pow(w1 / w2, kappa);
    final double w19 = Math.exp(lambda * t) * Math.pow(s, gamma) * (w12 - w17 * w13 - w16 * w14 + w18 * w15);
    
    final double w1d = x1[1];
    final double w2d = x2[1];
    final double w3d = h[1];
    final double w4d = -w1d / w1 / sigmarootT1;  //e1
    final double w5d = (2. * w2d / w2 - w1d / w1) / sigmarootT1;  //e2
    final double w6d = (-w1d / w1) / sigmarootT1;  //e3
    final double w7d = (2. * w2d / w2 - w1d / w1) / sigmarootT1;  //e4
    final double w8d = (-w3d / w3) / sigmarootT;  //f1
    final double w9d = (2. * w2d / w2 - w3d / w3) / sigmarootT;  //f2
    final double w10d = (2. * w1d / w1 - w3d / w3) / sigmarootT;  //f3
    final double w11d = (2. * w1d / w1 - w3d / w3 - 2. * w2d / w2) / sigmarootT;  //f4
    final double w12d = -NORMAL.getPDF(-w4) * NORMAL.getCDF(-(w8 - RHO * w4) / RHO_STAR) * w4d - NORMAL.getPDF(-w8) * NORMAL.getCDF(-(w4 - RHO * w8) / RHO_STAR) * w8d;
    final double w13d = -NORMAL.getPDF(-w5) * NORMAL.getCDF(-(w9 - RHO * w5) / RHO_STAR) * w5d - NORMAL.getPDF(-w9) * NORMAL.getCDF(-(w5 - RHO * w9) / RHO_STAR) * w9d;
    final double w14d = -NORMAL.getPDF(-w6) * NORMAL.getCDF(-(w10 + RHO * w6) / RHO_STAR) * w6d - NORMAL.getPDF(-w10) * NORMAL.getCDF(-(w6 + RHO * w10) / RHO_STAR) * w10d;
    final double w15d = -NORMAL.getPDF(-w7) * NORMAL.getCDF(-(w11 + RHO * w7) / RHO_STAR) * w7d - NORMAL.getPDF(-w11) * NORMAL.getCDF(-(w7 + RHO * w11) / RHO_STAR) * w11d;
    final double w16d = Math.pow(w1 / s, kappa - 1.) * kappa * w1d / s;
    final double w17d = Math.pow(w2 / s, kappa - 1.) * kappa * w2d / s;
    final double w18d = Math.pow(w1 / w2, kappa - 1.) * kappa * (w1d / w2 - w1 * w2d / w2 / w2);
    final double w19d = Math.exp(lambda * t) * Math.pow(s, gamma) * (w12d - w17d * w13 - w17 * w13d - w16d * w14 - w16 * w14d + w18d * w15 + w18 * w15d);
    
    final double w1dd = x1[2];
    final double w2dd = x2[2];
    final double w3dd = h[2];
    final double w4dd = -w1dd / w1 / sigmarootT1 + w1d * w1d / w1 / w1 / sigmarootT1;  //e1
    final double w5dd = (2. * w2dd / w2 - w1dd / w1 - 2. * w2d * w2d / w2 / w2 + w1d * w1d / w1 / w1) / sigmarootT1;  //e2
    final double w6dd = (-w1dd / w1 + w1d * w1d / w1 / w1) / sigmarootT1;  //e3
    final double w7dd = (2. * w2dd / w2 - w1dd / w1 - 2. * w2d * w2d / w2 / w2 + w1d * w1d / w1 / w1) / sigmarootT1;  //e4
    final double w8dd = (-w3dd / w3 + w3d * w3d / w3 / w3) / sigmarootT;  //f1
    final double w9dd = (2. * w2dd / w2 - w3dd / w3 - 2. * w2d * w2d / w2 / w2 + w3d * w3d / w3 / w3) / sigmarootT;  //f2
    final double w10dd = (2. * w1dd / w1 - w3dd / w3 - 2. * w1d * w1d / w1 / w1 + w3d * w3d / w3 / w3) / sigmarootT;  //f3
    final double w11dd = (2. * w1dd / w1 - w3dd / w3 - 2. * w2dd / w2 - 2. * w1d * w1d / w1 / w1 + w3d * w3d / w3 / w3 + 2. * w2d * w2d / w2 / w2) / sigmarootT;  //f4
    final double w12dd = getMdd(w4, w4d, w4dd, w8, w8d, w8dd, RHO);
    final double w13dd = getMdd(w5, w5d, w5dd, w9, w9d, w9dd, RHO);
    final double w14dd = getMdd(w6, w6d, w6dd, w10, w10d, w10dd, -RHO);
    final double w15dd = getMdd(w7, w7d, w7dd, w11, w11d, w11dd, -RHO);
    final double w16dd = Math.pow(w1 / s, kappa - 1.) * kappa * w1dd / s + Math.pow(w1 / s, kappa - 2.) * kappa * (kappa - 1.) * w1d / s * w1d / s;
    final double w17dd = Math.pow(w2 / s, kappa - 1.) * kappa * w2dd / s + Math.pow(w2 / s, kappa - 2.) * kappa * (kappa - 1.) * w2d / s * w2d / s;
    final double w18dd = Math.pow(w1 / w2, kappa - 1.) * kappa * (w1dd / w2 - w1 * w2dd / w2 / w2 - w1d * w2d / w2 / w2 - w1d * w2d / w2 / w2 + 2. * w1 * w2d * w2d / w2 / w2 / w2) +
        Math.pow(w1 / w2, kappa - 2.) * kappa * (kappa - 1.) *
            (w1d / w2 - w1 * w2d / w2 / w2) * (w1d / w2 - w1 * w2d / w2 / w2);
    final double w19dd = Math.exp(lambda * t) * Math.pow(s, gamma) *
        (w12dd - w17dd * w13 - w17 * w13dd - 2. * w17d * w13d - w16dd * w14 - w16 * w14dd - 2. * w16d * w14d + w18dd * w15 + w18 * w15dd + 2. * w18d * w15d);
    
    final double[] res = new double[3];
    res[0] = w19;
    res[1] = w19d;
    res[2] = w19dd;
    
    return res;
  }
  
  static double getMdd(final double eF, final double eFd, final double eFdd, final double fF, final double fFd, final double fFdd, final double rho) {
    
    return -NORMAL.getPDF(-eF) * NORMAL.getCDF(-(fF - rho * eF) / RHO_STAR) * eFdd - NORMAL.getPDF(-fF) * NORMAL.getCDF(-(eF - rho * fF) / RHO_STAR) * fFdd
        - NORMAL.getPDF(-eF) * NORMAL.getPDF(-(fF - rho * eF) / RHO_STAR) * eFd * (-(fFd - rho * eFd) / RHO_STAR) - NORMAL.getPDF(-fF) *
        NORMAL.getPDF(-(eF - rho * fF) / RHO_STAR) * fFd * (-(eFd - rho * fFd) / RHO_STAR)
        - NORMAL.getPDF(-eF) * NORMAL.getCDF(-(fF - rho * eF) / RHO_STAR) * eFd * (-eF * eFd) - NORMAL.getPDF(-fF) * NORMAL.getCDF(-(eF - rho * fF) / RHO_STAR) *
        fFd * (-fF * fFd);
  }
  
  static double[] getHDualDeltaGamma(final double b, final double t, final double sigma, final double k, final double[] b0, final double[] bInfinity) {
    
    final double w1 = k;
    final double w2 = w1 * w1;
    final double w3 = b0[0];
    final double w4 = bInfinity[0];
    final double w5 = w4 - w3;
    final double w6 = w5 * w3;
    final double w7 = w2 / w6;
    
    final double w2d = 2. * w1;
    final double w2dd = 2.;
    final double w3d = b0[1];
    final double w3dd = b0[2];
    final double w4d = bInfinity[1];
    final double w4dd = bInfinity[2];
    final double w5d = w4d - w3d;
    final double w5dd = w4dd - w3dd;
    final double w6d = w5d * w3 + w5 * w3d;
    final double w6dd = w5dd * w3 + w5 * w3dd + 2. * w5d * w3d;
    final double w7d = w2d / w6 - w2 / w6 / w6 * w6d;
    final double w7dd = w2dd / w6 + 2. * w2 / w6 / w6 / w6 * w6d * w6d - 2. * w2d / w6 / w6 * w6d - w2 / w6 / w6 * w6dd;
    
    final double[] res = new double[3];
    res[0] = -(b * t + 2 * sigma * Math.sqrt(t)) * w7;
    res[1] = -(b * t + 2 * sigma * Math.sqrt(t)) * w7d;
    res[2] = -(b * t + 2 * sigma * Math.sqrt(t)) * w7dd;
    
    return res;
  }
  
  static double[] getXDualDeltaGamma(final double[] b0, final double[] bInfinity, final double[] h) {
    
    final double w1 = h[0];
    final double w2 = b0[0];
    final double w3 = bInfinity[0];
    final double w4 = 1. - Math.exp(w1);
    final double w5 = w3 - w2;
    final double w6 = w4 * w5;
    final double w7 = w2 + w6;
    
    final double w1d = h[1];
    final double w1dd = h[2];
    final double w2d = b0[1];
    final double w2dd = b0[2];
    final double w3d = bInfinity[1];
    final double w3dd = bInfinity[2];
    final double w4d = -Math.exp(w1) * w1d;
    final double w4dd = -Math.exp(w1) * w1dd - Math.exp(w1) * w1d * w1d;
    final double w5d = w3d - w2d;
    final double w5dd = w3dd - w2dd;
    final double w6d = w4d * w5 + w4 * w5d;
    final double w6dd = w4dd * w5 + 2. * w4d * w5d + w4 * w5dd;
    final double w7d = w2d + w6d;
    final double w7dd = w2dd + w6dd;
    
    final double[] res = new double[3];
    res[0] = w7;
    res[1] = w7d;
    res[2] = w7dd;
    
    return res;
  }
  
  static double[] getAlphaDualDeltaGamma(final double[] x, final double beta, final double k) {
    
    final double w1 = k;
    final double w2 = x[0];
    final double w3 = w2 - w1;
    final double w4 = Math.pow(w2, -beta);
    final double w5 = w3 * w4;
    
    final double w2d = x[1];
    final double w3d = w2d - 1.;
    final double w4d = -beta * Math.pow(w2, -beta - 1) * w2d;
    final double w5d = w3d * w4 + w3 * w4d;
    
    final double w2dd = x[2];
    final double w3dd = w2dd;
    final double w4dd = (-beta) * (-beta - 1) * Math.pow(w2, -beta - 2) * w2d * w2d + (-beta) * Math.pow(w2, -beta - 1) * w2dd;
    final double w5dd = w3dd * w4 + w3 * w4dd + 2. * w3d * w4d;
    
    final double[] res = new double[3];
    res[0] = w5;
    res[1] = w5d;
    res[2] = w5dd;
    
    return res;
  }
  
}
