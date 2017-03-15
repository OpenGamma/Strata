/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * The eigenvalues of a matrix $\mathbf{A}$ are the roots of the characteristic
 * polynomial $P(x) = \mathrm{det}[\mathbf{A} - x\mathbb{1}]$. For a 
 * polynomial 
 * $$
 * \begin{align*}
 * P(x) = \sum_{i=0}^n a_i x^i
 * \end{align*} 
 * $$
 * an equivalent polynomial can be constructed from the characteristic polynomial of the matrix
 * $$
 * \begin{align*}
 * A = 
 * \begin{pmatrix}
 * -\frac{a_{m-1}}{a_m}  & -\frac{a_{m-2}}{a_m} & \cdots & -\frac{a_{1}}{a_m} & -\frac{a_{0}}{a_m} \\
 * 1                      & 0                     & \cdots & 0                   & 0                   \\
 * 0                      & 1                     & \cdots & 0                   & 0                   \\
 * \vdots                &                       & \cdots &                     & \vdots             \\
 * 0                      & 0                     & \cdots & 1                   & 0                   
 * \end{pmatrix}
 * \end{align*}
 * $$
 * and so the roots are found by calculating the eigenvalues of this matrix.
 */
public class EigenvaluePolynomialRootFinder implements Polynomial1DRootFinder<Double> {

  @Override
  public Double[] getRoots(RealPolynomialFunction1D function) {
    ArgChecker.notNull(function, "function");
    double[] coeffs = function.getCoefficients();
    int l = coeffs.length - 1;
    double[][] hessianDeref = new double[l][l];
    for (int i = 0; i < l; i++) {
      hessianDeref[0][i] = -coeffs[l - i - 1] / coeffs[l];
      for (int j = 1; j < l; j++) {
        hessianDeref[j][i] = 0;
        if (i != l - 1) {
          hessianDeref[i + 1][i] = 1;
        }
      }
    }
    RealMatrix hessian = new Array2DRowRealMatrix(hessianDeref);
    double[] d = new EigenDecomposition(hessian).getRealEigenvalues();
    Double[] result = new Double[d.length];
    for (int i = 0; i < d.length; i++) {
      result[i] = d[i];
    }
    return result;
  }

}
