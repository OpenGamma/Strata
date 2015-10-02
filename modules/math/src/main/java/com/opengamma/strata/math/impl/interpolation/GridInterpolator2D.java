/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * 
 */
public class GridInterpolator2D extends Interpolator2D {
  //TODO this is really inefficient - needs to be changed in a similar way to 1D interpolation
  /** The x interpolator */
  private final Interpolator1D _xInterpolator;
  /** The y interpolator */
  private final Interpolator1D _yInterpolator;

  /**
   * @param xInterpolator The x interpolator, not null
   * @param yInterpolator The y interpolator, not null
   */
  public GridInterpolator2D(final Interpolator1D xInterpolator, final Interpolator1D yInterpolator) {
    ArgChecker.notNull(xInterpolator, "x interpolator");
    ArgChecker.notNull(yInterpolator, "y interpolator");
    _xInterpolator = xInterpolator;
    _yInterpolator = yInterpolator;
  }

  /**
   * @param xInterpolator The x interpolator, not null
   * @param yInterpolator The y interpolator, not null
   * @param xExtrapolator The x extrapolator, not null
   * @param yExtrapolator The y extrapolator, not null
   */
  public GridInterpolator2D(
      final Interpolator1D xInterpolator,
      final Interpolator1D yInterpolator,
      final Extrapolator1D xExtrapolator,
      final Extrapolator1D yExtrapolator) {

    ArgChecker.notNull(xInterpolator, "x interpolator");
    ArgChecker.notNull(yInterpolator, "y interpolator");
    ArgChecker.notNull(xExtrapolator, "x extrapolator");
    ArgChecker.notNull(yExtrapolator, "y extrapolator");
    _xInterpolator = new CombinedInterpolatorExtrapolator(xInterpolator, xExtrapolator);
    _yInterpolator = new CombinedInterpolatorExtrapolator(yInterpolator, yExtrapolator);
  }

  public Map<Double, Interpolator1DDataBundle> getDataBundle(final Map<DoublesPair, Double> data) {
    ArgChecker.notNull(data, "data");
    return testData(data);
  }

  @Override
  public Double interpolate(final Map<Double, Interpolator1DDataBundle> dataBundle, final DoublesPair value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(dataBundle, "data bundle");
    final Map<Double, Double> xData = new HashMap<>();
    for (final Map.Entry<Double, Interpolator1DDataBundle> entry : dataBundle.entrySet()) {
      xData.put(entry.getKey(), _yInterpolator.interpolate(entry.getValue(), value.getSecond()));
    }
    return _xInterpolator.interpolate(_xInterpolator.getDataBundle(xData), value.getFirst());
  }

  @Override
  public Map<DoublesPair, Double> getNodeSensitivitiesForValue(final Map<Double, Interpolator1DDataBundle> dataBundle, final DoublesPair value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(dataBundle, "data bundle");
    final Map<Double, Double> xData = new HashMap<>();
    final double[][] temp = new double[dataBundle.size()][];
    int i = 0;
    for (final Map.Entry<Double, Interpolator1DDataBundle> entry : dataBundle.entrySet()) {
      //this is the sensitivity of the point projected onto a column of y-points to those points
      temp[i++] = _yInterpolator.getNodeSensitivitiesForValue(entry.getValue(), value.getSecond());
      xData.put(entry.getKey(), _yInterpolator.interpolate(entry.getValue(), value.getSecond()));
    }
    //this is the sensitivity of the point to the points projected onto y columns
    final double[] xSense = _xInterpolator.getNodeSensitivitiesForValue(_xInterpolator.getDataBundle(xData), value.getFirst());
    ArgChecker.isTrue(xSense.length == dataBundle.size(), "Number of x sensitivities {} must be equal to the data bundle size {}", xSense.length, dataBundle.size());
    final Map<DoublesPair, Double> res = new HashMap<>();

    double sense;
    i = 0;
    int j = 0;
    for (final Map.Entry<Double, Interpolator1DDataBundle> entry : dataBundle.entrySet()) {
      final double[] yValues = entry.getValue().getKeys();
      for (j = 0; j < yValues.length; j++) {
        sense = xSense[i] * temp[i][j];
        res.put(DoublesPair.of(entry.getKey(), yValues[j]), sense);
      }
      i++;
    }

    return res;
  }

  private Map<Double, Interpolator1DDataBundle> testData(final Map<DoublesPair, Double> data) {
    final Map<Double, Interpolator1DDataBundle> result = new TreeMap<>();
    final TreeMap<DoublesPair, Double> sorted = new TreeMap<>();
    sorted.putAll(data);
    final Iterator<Map.Entry<DoublesPair, Double>> iterator = sorted.entrySet().iterator();
    final Map.Entry<DoublesPair, Double> firstEntry = iterator.next();
    double x = firstEntry.getKey().getFirst();
    Map<Double, Double> yzValues = new TreeMap<>();
    yzValues.put(firstEntry.getKey().getSecond(), firstEntry.getValue());
    while (iterator.hasNext()) {
      final Map.Entry<DoublesPair, Double> nextEntry = iterator.next();
      final double newX = nextEntry.getKey().getFirst();
      if (Double.doubleToLongBits(newX) != Double.doubleToLongBits(x)) {
        final Interpolator1DDataBundle interpolatorData = _yInterpolator.getDataBundle(yzValues);
        result.put(x, interpolatorData);
        yzValues = new TreeMap<>();
        yzValues.put(nextEntry.getKey().getSecond(), nextEntry.getValue());
        x = newX;
      } else {
        yzValues.put(nextEntry.getKey().getSecond(), nextEntry.getValue());
      }
      if (!iterator.hasNext()) {
        yzValues.put(nextEntry.getKey().getSecond(), nextEntry.getValue());
        final Interpolator1DDataBundle interpolatorData = _yInterpolator.getDataBundle(yzValues);
        result.put(x, interpolatorData);
      }
    }
    return result;
  }

  public Interpolator1D getXInterpolator() {
    return _xInterpolator;
  }

  public Interpolator1D getYInterpolator() {
    return _yInterpolator;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof GridInterpolator2D)) {
      return false;
    }
    final GridInterpolator2D other = (GridInterpolator2D) o;
    return getXInterpolator().equals(other.getXInterpolator()) && getYInterpolator().equals(other.getYInterpolator());
  }

  @Override
  public int hashCode() {
    int hc = 1;
    hc = (hc * 31) + getXInterpolator().hashCode();
    hc = (hc * 31) + getYInterpolator().hashCode();
    return hc;
  }

}
