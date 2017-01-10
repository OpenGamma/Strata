/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.rootfinding.VectorRootFinder;

/**
 * Base implementation for all Newton-Raphson style multi-dimensional root finding (i.e. using the Jacobian matrix as a basis for some iterative process)
 */
public class NewtonVectorRootFinder extends VectorRootFinder {

  private static final Logger log = LoggerFactory.getLogger(NewtonVectorRootFinder.class);
  private static final double ALPHA = 1e-4;
  private static final double BETA = 1.5;
  private static final int FULL_RECALC_FREQ = 20;
  private final double _absoluteTol, _relativeTol;
  private final int _maxSteps;
  private final NewtonRootFinderDirectionFunction _directionFunction;
  private final NewtonRootFinderMatrixInitializationFunction _initializationFunction;
  private final NewtonRootFinderMatrixUpdateFunction _updateFunction;
  private final MatrixAlgebra _algebra = new OGMatrixAlgebra();

  public NewtonVectorRootFinder(
      double absoluteTol,
      double relativeTol,
      int maxSteps,
      NewtonRootFinderDirectionFunction directionFunction,
      NewtonRootFinderMatrixInitializationFunction initializationFunction,
      NewtonRootFinderMatrixUpdateFunction updateFunction) {

    ArgChecker.notNegative(absoluteTol, "absolute tolerance");
    ArgChecker.notNegative(relativeTol, "relative tolerance");
    ArgChecker.notNegative(maxSteps, "maxSteps");
    _absoluteTol = absoluteTol;
    _relativeTol = relativeTol;
    _maxSteps = maxSteps;
    _directionFunction = directionFunction;
    _initializationFunction = initializationFunction;
    _updateFunction = updateFunction;
  }

  @Override
  public DoubleArray getRoot(Function<DoubleArray, DoubleArray> function, DoubleArray startPosition) {
    VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return getRoot(function, jac.differentiate(function), startPosition);
  }

  /**
   *@param function a vector function (i.e. vector to vector) 
   *@param jacobianFunction calculates the Jacobian
  * @param startPosition where to start the root finder for.
  *  Note if multiple roots exist which one if found (if at all) will depend on startPosition 
  * @return the vector root of the collection of functions 
   */

  @SuppressWarnings("synthetic-access")
  public DoubleArray getRoot(Function<DoubleArray, DoubleArray> function,
      Function<DoubleArray, DoubleMatrix> jacobianFunction, DoubleArray startPosition) {
    checkInputs(function, startPosition);

    DataBundle data = new DataBundle();
    DoubleArray y = function.apply(startPosition);
    data.setX(startPosition);
    data.setY(y);
    data.setG0(_algebra.getInnerProduct(y, y));
    DoubleMatrix estimate = _initializationFunction.getInitializedMatrix(jacobianFunction, startPosition);

    if (!getNextPosition(function, estimate, data)) {
      if (isConverged(data)) {
        return data.getX(); // this can happen if the starting position is the root
      }
      throw new MathException("Cannot work with this starting position. Please choose another point");
    }

    int count = 0;
    int jacReconCount = 1;
    while (!isConverged(data)) {
      // Want to reset the Jacobian every so often even if backtracking is working
      if ((jacReconCount) % FULL_RECALC_FREQ == 0) {
        estimate = _initializationFunction.getInitializedMatrix(jacobianFunction, data.getX());
        jacReconCount = 1;
      } else {
        estimate = _updateFunction.getUpdatedMatrix(
            jacobianFunction, data.getX(), data.getDeltaX(), data.getDeltaY(), estimate);
        jacReconCount++;
      }
      // if backtracking fails, could be that Jacobian estimate has drifted too far
      if (!getNextPosition(function, estimate, data)) {
        estimate = _initializationFunction.getInitializedMatrix(jacobianFunction, data.getX());
        jacReconCount = 1;
        if (!getNextPosition(function, estimate, data)) {
          if (isConverged(data)) {
            // non-standard exit. Cannot find an improvement from this position,
            // so provided we are close enough to the root, exit.
            return data.getX();
          }
          String msg = "Failed to converge in backtracking, even after a Jacobian recalculation." +
              getErrorMessage(data, jacobianFunction);
          log.info(msg);
          throw new MathException(msg);
        }
      }
      count++;
      if (count > _maxSteps) {
        throw new MathException("Failed to converge - maximum iterations of " + _maxSteps + " reached." +
            getErrorMessage(data, jacobianFunction));
      }
    }
    return data.getX();
  }

  private String getErrorMessage(DataBundle data, Function<DoubleArray, DoubleMatrix> jacobianFunction) {
    return "Final position:" + data.getX() + "\nlast deltaX:" + data.getDeltaX() + "\n function value:" +
        data.getY() + "\nJacobian: \n" + jacobianFunction.apply(data.getX());
  }

  private boolean getNextPosition(
      Function<DoubleArray, DoubleArray> function,
      DoubleMatrix estimate,
      DataBundle data) {

    DoubleArray p = _directionFunction.getDirection(estimate, data.getY());
    if (data.getLambda0() < 1.0) {
      data.setLambda0(1.0);
    } else {
      data.setLambda0(data.getLambda0() * BETA);
    }
    updatePosition(p, function, data);
    double g1 = data.getG1();
    if (!Doubles.isFinite(g1)) {
      bisectBacktrack(p, function, data);
    }
    if (data.getG1() > data.getG0() / (1 + ALPHA * data.getLambda0())) {
      quadraticBacktrack(p, function, data);
      int count = 0;
      while (data.getG1() > data.getG0() / (1 + ALPHA * data.getLambda0())) {
        if (count > 5) {
          return false;
        }
        cubicBacktrack(p, function, data);
        count++;
      }
    }
    DoubleArray deltaX = data.getDeltaX();
    DoubleArray deltaY = data.getDeltaY();
    data.setG0(data.getG1());
    data.setX((DoubleArray) _algebra.add(data.getX(), deltaX));
    data.setY((DoubleArray) _algebra.add(data.getY(), deltaY));
    return true;
  }

  protected void updatePosition(DoubleArray p, Function<DoubleArray, DoubleArray> function, DataBundle data) {
    double lambda0 = data.getLambda0();
    DoubleArray deltaX = (DoubleArray) _algebra.scale(p, -lambda0);
    DoubleArray xNew = (DoubleArray) _algebra.add(data.getX(), deltaX);
    DoubleArray yNew = function.apply(xNew);
    data.setDeltaX(deltaX);
    data.setDeltaY((DoubleArray) _algebra.subtract(yNew, data.getY()));
    data.setG2(data.getG1());
    data.setG1(_algebra.getInnerProduct(yNew, yNew));
  }

  private void bisectBacktrack(DoubleArray p, Function<DoubleArray, DoubleArray> function, DataBundle data) {
    do {
      data.setLambda0(data.getLambda0() * 0.1);
      updatePosition(p, function, data);

      if (data.getLambda0() == 0.0) {
        throw new MathException("Failed to converge");
      }
    } while (Double.isNaN(data.getG1()) || Double.isInfinite(data.getG1()) ||
        Double.isNaN(data.getG2()) || Double.isInfinite(data.getG2()));

  }

  private void quadraticBacktrack(
      DoubleArray p,
      Function<DoubleArray, DoubleArray> function,
      DataBundle data) {

    double lambda0 = data.getLambda0();
    double g0 = data.getG0();
    double lambda = Math.max(0.01 * lambda0, g0 * lambda0 * lambda0 / (data.getG1() + g0 * (2 * lambda0 - 1)));
    data.swapLambdaAndReplace(lambda);
    updatePosition(p, function, data);
  }

  private void cubicBacktrack(DoubleArray p, Function<DoubleArray, DoubleArray> function, DataBundle data) {
    double temp1, temp2, temp3, temp4, temp5;
    double lambda0 = data.getLambda0();
    double lambda1 = data.getLambda1();
    double g0 = data.getG0();
    temp1 = 1.0 / lambda0 / lambda0;
    temp2 = 1.0 / lambda1 / lambda1;
    temp3 = data.getG1() + g0 * (2 * lambda0 - 1.0);
    temp4 = data.getG2() + g0 * (2 * lambda1 - 1.0);
    temp5 = 1.0 / (lambda0 - lambda1);
    double a = temp5 * (temp1 * temp3 - temp2 * temp4);
    double b = temp5 * (-lambda1 * temp1 * temp3 + lambda0 * temp2 * temp4);
    double lambda = (-b + Math.sqrt(b * b + 6 * a * g0)) / 3 / a;
    lambda = Math.min(Math.max(lambda, 0.01 * lambda0), 0.75 * lambda1); // make sure new lambda is between 1% & 75% of old value
    data.swapLambdaAndReplace(lambda);
    updatePosition(p, function, data);
  }

  private boolean isConverged(DataBundle data) {
    DoubleArray deltaX = data.getDeltaX();
    DoubleArray x = data.getX();
    int n = deltaX.size();
    double diff, scale;
    for (int i = 0; i < n; i++) {
      diff = Math.abs(deltaX.get(i));
      scale = Math.abs(x.get(i));
      if (diff > _absoluteTol + scale * _relativeTol) {
        return false;
      }
    }
    return (Math.sqrt(data.getG0()) < _absoluteTol);
  }

  private static class DataBundle {
    private double _g0;
    private double _g1;
    private double _g2;
    private double _lambda0;
    private double _lambda1;
    private DoubleArray _deltaY;
    private DoubleArray _y;
    private DoubleArray _deltaX;
    private DoubleArray _x;

    public double getG0() {
      return _g0;
    }

    public double getG1() {
      return _g1;
    }

    public double getG2() {
      return _g2;
    }

    public double getLambda0() {
      return _lambda0;
    }

    public double getLambda1() {
      return _lambda1;
    }

    public DoubleArray getDeltaY() {
      return _deltaY;
    }

    public DoubleArray getY() {
      return _y;
    }

    public DoubleArray getDeltaX() {
      return _deltaX;
    }

    public DoubleArray getX() {
      return _x;
    }

    public void setG0(double g0) {
      _g0 = g0;
    }

    public void setG1(double g1) {
      _g1 = g1;
    }

    public void setG2(double g2) {
      _g2 = g2;
    }

    public void setLambda0(double lambda0) {
      _lambda0 = lambda0;
    }

    public void setDeltaY(DoubleArray deltaY) {
      _deltaY = deltaY;
    }

    public void setY(DoubleArray y) {
      _y = y;
    }

    public void setDeltaX(DoubleArray deltaX) {
      _deltaX = deltaX;
    }

    public void setX(DoubleArray x) {
      _x = x;
    }

    public void swapLambdaAndReplace(double lambda0) {
      _lambda1 = _lambda0;
      _lambda0 = lambda0;
    }
  }

}
