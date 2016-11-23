/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.product.option.BarrierType;

/**
 * Single barrier knock-out option function.
 * <p>
 * Note that there is no option function responsible for knock-in options because a knock-in option is priced via
 * the in-out parity in a tree model.
 */
abstract class SingleBarrierKnockoutFunction implements OptionFunction {

  /**
   * Obtains strike value.
   * 
   * @return strike
   */
  public abstract double getStrike();

  /**
   * Obtains the barrier level for the time layer specified by {@code step}.
   * 
   * @param step  the step
   * @return the barrier level
   */
  public abstract double getBarrierLevel(int step);

  /**
   * Obtains the sign.
   * <p>
   * The sign is +1 for call and -1 for put.
   * 
   * @return the sign
   */
  public abstract double getSign();

  /**
   * Obtains the barrier type.
   * 
   * @return the barrier type
   */
  public abstract BarrierType getBarrierType();

  /**
   * Obtains the rebate for the time layer specified by {@code step}.
   * 
   * @param step  the step
   * @return the rebate
   */
  public abstract double getRebate(int step);

  @Override
  public DoubleArray getPayoffAtExpiryTrinomial(DoubleArray stateValue) {

    int nNodes = stateValue.size();
    double[] values = new double[nNodes];
    double rebate = getRebate(getNumberOfSteps());
    double barrierLevel = getBarrierLevel(getNumberOfSteps());
    boolean isDown = getBarrierType().isDown();
    Arrays.fill(values, rebate);
    int index = getLowerBoundIndex(stateValue, barrierLevel);
    ArgChecker.isTrue(index > -1 && index < nNodes - 1, "barrier is covered by tree");
    int iMin = isDown ? index + 1 : 0;
    int iMmax = !isDown ? index + 1 : nNodes;
    for (int i = iMin; i < iMmax; ++i) {
      values[i] = Math.max(getSign() * (stateValue.get(i) - getStrike()), 0d);
    }
    // modification if barrier lies between two consecutive nodes 
    double bd = barrierLevel - stateValue.get(index);
    double ub = stateValue.get(index + 1) - barrierLevel;
    double ud = stateValue.get(index + 1) - stateValue.get(index);
    if (isDown) {
      values[index + 1] = 0.5 * values[index + 1] + 0.5 * (bd * rebate + ub * values[index + 1]) / ud;
    } else {
      values[index] = barrierLevel == stateValue.get(index) ?
          rebate :
          0.5 * values[index] + 0.5 * (ub * rebate + bd * values[index]) / ud;
    }
    return DoubleArray.ofUnsafe(values);
  }

  @Override
  public DoubleArray getNextOptionValues(
      double discountFactor,
      DoubleMatrix transitionProbability,
      DoubleArray stateValue,
      DoubleArray values,
      int i) {

    int nNodes = 2 * i + 1;
    double[] res = new double[nNodes];
    double barrierLevel = getBarrierLevel(i);
    double rebate = getRebate(i);
    boolean isDown = getBarrierType().isDown();
    for (int j = 0; j < nNodes; ++j) {
      if ((isDown && stateValue.get(j) <= barrierLevel) ||
          (!isDown && stateValue.get(j) >= barrierLevel)) {
        res[j] = rebate;
      } else {
        double upProb = transitionProbability.get(j, 2);
        double middleProb = transitionProbability.get(j, 1);
        double downProb = transitionProbability.get(j, 0);
        res[j] = discountFactor *
            (upProb * values.get(j + 2) + middleProb * values.get(j + 1) + downProb * values.get(j));
      }
    }
    // modification if barrier lies between two consecutive nodes 
    int index = getLowerBoundIndex(stateValue, barrierLevel);
    if (index > -1 && index < nNodes - 1) {
      double bd = barrierLevel - stateValue.get(index);
      double ub = stateValue.get(index + 1) - barrierLevel;
      double ud = stateValue.get(index + 1) - stateValue.get(index);
      if (isDown) {
        res[index + 1] = 0.5 * res[index + 1] + 0.5 * (bd * rebate + ub * res[index + 1]) / ud;
      } else {
        res[index] = 0.5 * res[index] + 0.5 * (ub * rebate + bd * res[index]) / ud;
      }
    }
    return DoubleArray.ofUnsafe(res);
  }

  //-------------------------------------------------------------------------
  private int getLowerBoundIndex(DoubleArray set, double value) {
    int n = set.size();
    if (value < set.get(0)) {
      return -1;
    }
    if (value > set.get(n - 1)) {
      return n - 1;
    }
    int index = Arrays.binarySearch(set.toArrayUnsafe(), value);
    if (index >= 0) {
      // Fast break out if it's an exact match.
      return index;
    }
    if (index < 0) {
      index = -(index + 1);
      index--;
    }
    if (value == -0. && index < n - 1 && set.get(index + 1) == 0.) {
      ++index;
    }
    return index;
  }
}
