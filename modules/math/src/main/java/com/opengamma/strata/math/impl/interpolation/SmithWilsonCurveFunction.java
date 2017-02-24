/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.FunctionUtils;

/**
 * Smith-Wilson curve function.
 * <p>
 * The curve represents the discount factor values as a function of time. 
 * <p>
 * The curve is controlled by {@code omega}, {@code alpha} and {@code weights}: 
 * {@code omega} is related to the ultimate forward rate (UFR) by {@code omega = log(1 + UFR)}, 
 * the {@code alpha} parameter determines the rate of convergence to the UFR, 
 * and the {@code weights} are the parameters to be calibrated to market data.
 */
public class SmithWilsonCurveFunction {

  /**
   * Default implementation with UFR = 4.2%
   */
  public static final SmithWilsonCurveFunction DEFAULT = SmithWilsonCurveFunction.of(0.042);

  /**
   * The omega parameter.
   */
  private final double omega;

  /**
   * Creates an instance with UFR 
   * 
   * @param ufr  the UFR
   * @return the instance
   */
  public static SmithWilsonCurveFunction of(double ufr) {
    return new SmithWilsonCurveFunction(ufr);
  }

  // private constructor
  private SmithWilsonCurveFunction(double ufr) {
    this.omega = Math.log(1d + ufr);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the gap from the UFR at x value.
   * <p>
   * The {@code nodes} must be sorted in ascending order and coherent to {@code weights}.
   * 
   * @param x  the x value
   * @param alpha  the alpha parameter
   * @param nodes  the nodes
   * @param weights  the weights
   * @return the gap
   */
  public static double gap(double x, double alpha, DoubleArray nodes, DoubleArray weights) {
    int size = nodes.size();
    ArgChecker.isTrue(size == weights.size(), "nodes and weights must be the same size");
    double num = 1d;
    double den = 0d;
    for (int i = 0; i < size; ++i) {
      num += alpha * nodes.get(i) * weights.get(i);
      den += Math.sinh(alpha * nodes.get(i)) * weights.get(i);
    }
    return alpha / Math.abs(1d - num * Math.exp(alpha * x) / den);
  }

  //-------------------------------------------------------------------------
  /**
   * Evaluates the Smith-Wilson curve function at a x value. 
   * <p>
   * The {@code nodes} must be sorted in ascending order and coherent to {@code weights}.
   * 
   * @param x  the x value
   * @param alpha  the alpha parameter
   * @param nodes  the nodes
   * @param weights  the weights
   * @return the value
   */
  public double value(double x, double alpha, DoubleArray nodes, DoubleArray weights) {
    int size = nodes.size();
    ArgChecker.isTrue(size == weights.size(), "nodes and weights must be the same size");
    double res = 1d;
    int bound = x < nodes.get(0) ? 0 : FunctionUtils.getLowerBoundIndex(nodes, x) + 1;
    for (int i = 0; i < bound; ++i) {
      res += weights.get(i) * wilsonFunctionLeft(x, alpha, nodes.get(i));
    }
    for (int i = bound; i < size; ++i) {
      res += weights.get(i) * wilsonFunctionRight(x, alpha, nodes.get(i));
    }
    res *= Math.exp(-omega * x);
    return res;
  }

  /**
   * Computes the gradient of the Smith-Wilson curve function at a x value. 
   * <p>
   * The {@code nodes} must be sorted in ascending order and coherent to {@code weights}.
   * 
   * @param x  the x value
   * @param alpha  the alpha parameter
   * @param nodes  the nodes
   * @param weights  the weights
   * @return the gradient
   */
  public double firstDerivative(double x, double alpha, DoubleArray nodes, DoubleArray weights) {
    int size = nodes.size();
    ArgChecker.isTrue(size == weights.size(), "nodes and weights must be the same size");
    double res = -omega;
    int bound = x < nodes.get(0) ? 0 : FunctionUtils.getLowerBoundIndex(nodes, x) + 1;
    for (int i = 0; i < bound; ++i) {
      res += weights.get(i) * wilsonFunctionLeftDerivative(x, alpha, nodes.get(i));
    }
    for (int i = bound; i < size; ++i) {
      res += weights.get(i) * wilsonFunctionRightDerivative(x, alpha, nodes.get(i));
    }
    res *= Math.exp(-omega * x);
    return res;
  }

  /**
   * Computes the sensitivity of the Smith-Wilson curve function to weights parameters at a x value. 
   * <p>
   * The {@code nodes} must be sorted in ascending order.
   * 
   * @param x  the x value
   * @param alpha  the alpha parameter
   * @param nodes  the nodes
   * @return the value
   */
  public DoubleArray parameterSensitivity(double x, double alpha, DoubleArray nodes) {
    int size = nodes.size();
    double[] res = new double[size];
    double expOmega = Math.exp(-omega * x);
    int bound = x < nodes.get(0) ? 0 : FunctionUtils.getLowerBoundIndex(nodes, x) + 1;
    for (int i = 0; i < bound; ++i) {
      res[i] = expOmega * wilsonFunctionLeft(x, alpha, nodes.get(i));
    }
    for (int i = bound; i < size; ++i) {
      res[i] = expOmega * wilsonFunctionRight(x, alpha, nodes.get(i));
    }
    return DoubleArray.ofUnsafe(res);
  }

  //-------------------------------------------------------------------------
  // x < node
  private double wilsonFunctionRight(double x, double alpha, double node) {
    double alphaX = alpha * x;
    return alphaX - Math.exp(-alpha * node) * Math.sinh(alphaX);
  }

  // x < node, includes derivative of Math.exp(-omega * x)
  private double wilsonFunctionRightDerivative(double x, double alpha, double node) {
    double alphaX = alpha * x;
    double expAlphaNode = Math.exp(-alpha * node);
    return -omega * (alphaX - expAlphaNode * Math.sinh(alphaX)) + alpha * (1d - expAlphaNode * Math.cosh(alphaX));
  }

  // x > node
  private double wilsonFunctionLeft(double x, double alpha, double node) {
    double alphaNode = alpha * node;
    return alphaNode - Math.exp(-alpha * x) * Math.sinh(alphaNode);
  }

  // x > node, includes derivative of Math.exp(-omega * x)
  private double wilsonFunctionLeftDerivative(double x, double alpha, double node) {
    double alphaNode = alpha * node;
    double expAlphaX = Math.exp(-alpha * x);
    return -omega * (alphaNode - expAlphaX * Math.sinh(alphaNode)) + alpha * expAlphaX * Math.sinh(alphaNode);
  }

}
