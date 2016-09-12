/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda.e2e;

import java.util.Arrays;
import java.util.BitSet;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;

/**
 * 
 */
public class IntrinsicIndexDataBundle {

  private static final double TOL = 1e-12;

  private final int _indexSize;
  private final int _nDefaults;

  private final double _indexFactor;
  private final double[] _weights;
  private final double[] _lgd;
  private final IsdaCompliantCreditCurve[] _creditCurves;
  private final BitSet _defaulted;

  public IntrinsicIndexDataBundle(IsdaCompliantCreditCurve[] creditCurves, double[] recoveryRates) {
    ArgChecker.noNulls(creditCurves, "creditCurves");
    ArgChecker.notEmpty(recoveryRates, "recoveryRates");
    _indexSize = creditCurves.length;
    ArgChecker.isTrue(
        _indexSize == recoveryRates.length,
        "Length of recoveryRates ({}) does not match index size ({})", recoveryRates.length, _indexSize);
    _nDefaults = 0;

    _lgd = new double[_indexSize];

    for (int i = 0; i < _indexSize; i++) {
      double lgd = 1 - recoveryRates[i];
      if (lgd < 0.0 || lgd > 1.0) {
        throw new IllegalArgumentException(
            "recovery rate must be between 0 and 1.Value of " + recoveryRates[i] + " given at index " + i);
      }
      _lgd[i] = lgd;
    }

    _weights = new double[_indexSize];
    Arrays.fill(_weights, 1.0 / _indexSize);
    _creditCurves = creditCurves;
    _defaulted = new BitSet(_indexSize);
    _indexFactor = 1.0;
  }

  public IntrinsicIndexDataBundle(IsdaCompliantCreditCurve[] creditCurves, double[] recoveryRates, double[] weights) {
    ArgChecker.noNulls(creditCurves, "creditCurves");
    ArgChecker.notEmpty(recoveryRates, "recoveryRates");
    ArgChecker.notEmpty(weights, "weights");
    _indexSize = creditCurves.length;
    ArgChecker.isTrue(
        _indexSize == recoveryRates.length,
        "Length of recoveryRates ({}) does not match index size ({})", recoveryRates.length, _indexSize);
    ArgChecker.isTrue(_indexSize == weights.length,
        "Length of weights ({}) does not match index size ({})", weights.length, _indexSize);

    _nDefaults = 0;

    _lgd = new double[_indexSize];
    double sum = 0.0;
    for (int i = 0; i < _indexSize; i++) {
      if (weights[i] <= 0.0) {
        throw new IllegalArgumentException("weights must be positive. Value of " + weights[i] + " given at index " + i);
      }
      sum += weights[i];
      double lgd = 1 - recoveryRates[i];
      if (lgd < 0.0 || lgd > 1.0) {
        throw new IllegalArgumentException(
            "recovery rate must be between 0 and 1.Value of " + recoveryRates[i] + " given at index " + i);
      }
      _lgd[i] = lgd;
    }
    if (Math.abs(sum - 1.0) > TOL) {
      throw new IllegalArgumentException("weights do not sum to 1.0, but " + sum);
    }

    _weights = new double[_indexSize];
    System.arraycopy(weights, 0, _weights, 0, _indexSize);
    _creditCurves = creditCurves;
    _defaulted = new BitSet(_indexSize);
    _indexFactor = 1.0;
  }

  public IntrinsicIndexDataBundle(IsdaCompliantCreditCurve[] creditCurves, double[] recoveryRates, BitSet defaulted) {
    ArgChecker.notNull(creditCurves, "creditCurves"); //we do allow null entries if listed as defaulted
    ArgChecker.notEmpty(recoveryRates, "recoveryRates");
    ArgChecker.notNull(defaulted, "defaulted");

    _indexSize = creditCurves.length;
    ArgChecker.isTrue(_indexSize == recoveryRates.length,
        "Length of recoveryRates ({}) does not match index size ({})", recoveryRates.length, _indexSize);
    // Correction made  PLAT-6323 
    ArgChecker.isTrue(_indexSize >= defaulted.length(),
        "Length of defaulted ({}) is greater than index size ({})", defaulted.length(), _indexSize);

    _nDefaults = defaulted.cardinality();

    _lgd = new double[_indexSize];

    for (int i = 0; i < _indexSize; i++) {
      if (creditCurves[i] == null && !defaulted.get(i)) {
        throw new IllegalArgumentException("Null credit curve, but not set as defaulted in alive list. Index is " + i);
      }
      double lgd = 1 - recoveryRates[i];
      if (lgd < 0.0 || lgd > 1.0) {
        throw new IllegalArgumentException(
            "recovery rate must be between 0 and 1.Value of " + recoveryRates[i] + " given at index " + i);
      }
      _lgd[i] = lgd;
    }

    _weights = new double[_indexSize];
    Arrays.fill(_weights, 1.0 / _indexSize);
    _creditCurves = creditCurves;
    _defaulted = defaulted;
    // Correction made PLAT-6328
    _indexFactor = (((double) _indexSize) - _nDefaults) * _weights[0];
  }

  public IntrinsicIndexDataBundle(
      IsdaCompliantCreditCurve[] creditCurves,
      double[] recoveryRates,
      double[] weights,
      BitSet defaulted) {

    ArgChecker.notNull(creditCurves, "creditCurves"); //we do allow null entries if listed as defaulted
    ArgChecker.notEmpty(recoveryRates, "recoveryRates");
    ArgChecker.notEmpty(weights, "weights");
    ArgChecker.notNull(defaulted, "defaulted");

    _indexSize = creditCurves.length;
    ArgChecker.isTrue(_indexSize == recoveryRates.length,
        "Length of recoveryRates ({}) does not match index size ({})", recoveryRates.length, _indexSize);
    ArgChecker.isTrue(_indexSize == weights.length,
        "Length of weights ({}) does not match index size ({})", weights.length, _indexSize);
    // Correction made  PLAT-6323 
    ArgChecker.isTrue(_indexSize >= defaulted.length(),
        "Length of defaulted ({}) is greater than index size ({})", defaulted.length(), _indexSize);

    _nDefaults = defaulted.cardinality();

    _lgd = new double[_indexSize];
    double sum = 0.0;
    for (int i = 0; i < _indexSize; i++) {
      if (creditCurves[i] == null && !defaulted.get(i)) {
        throw new IllegalArgumentException("Null credit curve, but not set as defaulted in alive list. Index is " + i);
      }
      if (weights[i] <= 0.0) {
        throw new IllegalArgumentException("weights must be positive. Value of " + weights[i] + " given at index " + i);
      }
      sum += weights[i];
      double lgd = 1 - recoveryRates[i];
      if (lgd < 0.0 || lgd > 1.0) {
        throw new IllegalArgumentException(
            "recovery rate must be between 0 and 1.Value of " + recoveryRates[i] + " given at index " + i);
      }
      _lgd[i] = lgd;
    }

    double f = 1.0;
    if (_nDefaults > 0) {
      for (int i = defaulted.nextSetBit(0); i >= 0; i = defaulted.nextSetBit(i + 1)) {
        f -= weights[i];
      }
    }
    _indexFactor = f;

    if (Math.abs(sum - 1.0) > TOL) {
      throw new IllegalArgumentException("weights do not sum to 1.0, but " + sum);
    }

    _weights = new double[_indexSize];
    System.arraycopy(weights, 0, _weights, 0, _indexSize);
    _creditCurves = creditCurves;
    _defaulted = defaulted;
  }

  private IntrinsicIndexDataBundle(
      int indexSize, int nDefaults,
      double indexFactor,
      double[] weights,
      double[] lgd,
      IsdaCompliantCreditCurve[] creditCurves,
      BitSet defaulted) {

    _indexSize = indexSize;
    _nDefaults = nDefaults;
    _indexFactor = indexFactor;
    _weights = weights;
    _lgd = lgd;
    _creditCurves = creditCurves;
    _defaulted = defaulted;
  }

  /**
   * Gets the (initial) index size 
   * @return the index size
   */
  public int getIndexSize() {
    return _indexSize;
  }

  /**
   * Gets the number of defaults the index has suffered
   * @return the number of defaults 
   */
  public int getNumOfDefaults() {
    return _nDefaults;
  }

  /**
   * Gets the weight of a particular name in the index.
   * @param index The index of the constituent name 
   * @return The weight
   */
  public double getWeight(int index) {
    return _weights[index];
  }

  /**
   * Gets the Loss-Given-Default (LGD) for a  particular name,
   * @param index The index of the constituent name 
   * @return The LGD
   */
  public double getLGD(int index) {
    return _lgd[index];
  }

  /**
   * Gets the credit curve for a particular name,
   * * @param index The index of the constituent name 
   * @return a credit curve
   */
  public IsdaCompliantCreditCurve getCreditCurve(int index) {
    return _creditCurves[index];
  }

  public IsdaCompliantCreditCurve[] getCreditCurves() {
    return _creditCurves;
  }

  /**
   * Get whether a particular name has defaulted 
   * @param index The index of the constituent name 
   * @return true if the name has defaulted 
   */
  public boolean isDefaulted(int index) {
    return _defaulted.get(index);
  }

  /**
   * Get the index factor
   * @return the index factor 
   */
  public double getIndexFactor() {
    return _indexFactor;
  }

  /**
   * Replace the credit curves with a new set 
   * @param curves Credit curves. Must be the same length as the index size, and only null for defaulted names 
   * @return new IntrinsicIndexDataBundle with given curves 
   */
  public IntrinsicIndexDataBundle withCreditCurves(IsdaCompliantCreditCurve[] curves) {
    ArgChecker.notNull(curves, "curves");
    //  caught by notNull above
    int n = curves.length;
    ArgChecker.isTrue(n == _indexSize, "wrong number of curves. Require {}, but {} given", _indexSize, n);
    for (int i = 0; i < n; i++) {
      if (curves[i] == null && !_defaulted.get(i)) {
        throw new IllegalArgumentException("null curve at index " + i + ", but this is not listed as defaulted");
      }
    }

    return new IntrinsicIndexDataBundle(_indexSize, _nDefaults, _indexFactor, _weights, _lgd, curves, _defaulted);
  }

  /**
   * Produce a new data bundle with the name at the given index marked as defaulted.
   * The number of defaults {@link #getNumOfDefaults} is incremented and the index factor 
   * {@link #getIndexFactor} adjusted down - everything else remained unchanged.
   *  
   * @param index The index of the name to set as defaulted.
   *  If this name is already marked as defaulted, an exception is thrown 
   * @return  new data bundle with the name at the given index marked as defaulted
   */
  public IntrinsicIndexDataBundle withDefault(int index) {
    ArgChecker.isTrue(index < _indexSize, "index ({}) should be smaller than index size ({})", index, _indexSize);
    if (_defaulted.get(index)) {
      throw new IllegalArgumentException("Index " + index + " is already defaulted");
    }
    BitSet defaulted = (BitSet) _defaulted.clone();
    defaulted.set(index);

    return new IntrinsicIndexDataBundle(
        _indexSize, _nDefaults + 1, _indexFactor - _weights[index], _weights, _lgd, _creditCurves, defaulted);
  }

  /**
   * Produce a new data bundle with the names at the given indices marked as defaulted.
   * The number of defaults {@link #getNumOfDefaults} is incremented and the index factor 
   *{@link #getIndexFactor} adjusted down - everything else remained unchanged.
   *
   * @param index The indices of the names to set as defaulted. If any name is already marked
   *  as defaulted (or the list contains duplicates), an exception is thrown 
   * @return  new data bundle with the names at the given indices marked as defaulted
   */
  public IntrinsicIndexDataBundle withDefault(int... index) {
    ArgChecker.notEmpty(index, "index");
    BitSet defaulted = (BitSet) _defaulted.clone();
    int n = index.length;
    double sum = 0.0;
    for (int i = 0; i < n; i++) {
      int jj = index[i];
      ArgChecker.isTrue(jj < _indexSize, "index ({}) should be smaller than index size ({})", jj, _indexSize);
      if (defaulted.get(jj)) {
        throw new IllegalArgumentException("Index " + jj + " is already defaulted");
      }
      defaulted.set(jj);
      sum += _weights[jj];
    }

    return new IntrinsicIndexDataBundle(
        _indexSize, _nDefaults + n, _indexFactor - sum, _weights, _lgd, _creditCurves, defaulted);
  }

}
