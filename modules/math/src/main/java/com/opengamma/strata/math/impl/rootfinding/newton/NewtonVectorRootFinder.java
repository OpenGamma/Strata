/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.impl.rootfinding.VectorRootFinder;

/**
 * Base implementation for all Newton-Raphson style multi-dimensional root finding (i.e. using the Jacobian matrix as a basis for some iterative process)
 */
public class NewtonVectorRootFinder extends VectorRootFinder {
  private static final Logger s_logger = LoggerFactory.getLogger(NewtonVectorRootFinder.class);
  private static final double ALPHA = 1e-4;
  private static final double BETA = 1.5;
  private static final int FULL_RECALC_FREQ = 20;
  private final double _absoluteTol, _relativeTol;
  private final int _maxSteps;
  private final NewtonRootFinderDirectionFunction _directionFunction;
  private final NewtonRootFinderMatrixInitializationFunction _initializationFunction;
  private final NewtonRootFinderMatrixUpdateFunction _updateFunction;
  private final MatrixAlgebra _algebra = new OGMatrixAlgebra();

  public NewtonVectorRootFinder(final double absoluteTol, final double relativeTol, final int maxSteps, final NewtonRootFinderDirectionFunction directionFunction,
      final NewtonRootFinderMatrixInitializationFunction initializationFunction, final NewtonRootFinderMatrixUpdateFunction updateFunction) {
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
  public DoubleMatrix1D getRoot(final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DoubleMatrix1D startPosition) {
    final VectorFieldFirstOrderDifferentiator jac = new VectorFieldFirstOrderDifferentiator();
    return getRoot(function, jac.differentiate(function), startPosition);
  }

  /**
   *@param function a vector function (i.e. vector to vector) 
   *@param jacobianFunction calculates the Jacobian
  * @param startPosition where to start the root finder for. Note if multiple roots exist which one if found (if at all) will depend on startPosition 
  * @return the vector root of the collection of functions 
   */

  @SuppressWarnings("synthetic-access")
  public DoubleMatrix1D getRoot(final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final Function1D<DoubleMatrix1D, DoubleMatrix2D> jacobianFunction, final DoubleMatrix1D startPosition) {
    checkInputs(function, startPosition);

    final DataBundle data = new DataBundle();
    final DoubleMatrix1D y = function.evaluate(startPosition);
    data.setX(startPosition);
    data.setY(y);
    data.setG0(_algebra.getInnerProduct(y, y));
    DoubleMatrix2D estimate = _initializationFunction.getInitializedMatrix(jacobianFunction, startPosition);

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
        estimate = _updateFunction.getUpdatedMatrix(jacobianFunction, data.getX(), data.getDeltaX(), data.getDeltaY(), estimate);
        jacReconCount++;
      }
      // if backtracking fails, could be that Jacobian estimate has drifted too far
      if (!getNextPosition(function, estimate, data)) {
        estimate = _initializationFunction.getInitializedMatrix(jacobianFunction, data.getX());
        jacReconCount = 1;
        if (!getNextPosition(function, estimate, data)) {
          if (isConverged(data)) {
            return data.getX(); //non-standard exit. Cannot find an improvement from this position, so provided we are close enough to the root, exit.
          }
          String msg = "Failed to converge in backtracking, even after a Jacobian recalculation." + getErrorMessage(data, jacobianFunction);
          s_logger.info(msg);
          throw new MathException(msg);
        }
      }
      count++;
      if (count > _maxSteps) {
        throw new MathException("Failed to converge - maximum iterations of " + _maxSteps + " reached." + getErrorMessage(data, jacobianFunction));
      }
    }
    return data.getX();
  }

  private String getErrorMessage(final DataBundle data, final Function1D<DoubleMatrix1D, DoubleMatrix2D> jacobianFunction) {
    return "Final position:" + data.getX() + "\nlast deltaX:" + data.getDeltaX() + "\n function value:" + data.getY() + "\nJacobian: \n"
        + jacobianFunction.evaluate(data.getX());
  }

  private boolean getNextPosition(final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DoubleMatrix2D estimate, final DataBundle data) {
    final DoubleMatrix1D p = _directionFunction.getDirection(estimate, data.getY());
    if (data.getLambda0() < 1.0) {
      data.setLambda0(1.0);
    } else {
      data.setLambda0(data.getLambda0() * BETA);
    }
    updatePosition(p, function, data);
    final double g1 = data.getG1();
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
    final DoubleMatrix1D deltaX = data.getDeltaX();
    final DoubleMatrix1D deltaY = data.getDeltaY();
    data.setG0(data.getG1());
    data.setX((DoubleMatrix1D) _algebra.add(data.getX(), deltaX));
    data.setY((DoubleMatrix1D) _algebra.add(data.getY(), deltaY));
    return true;
  }

  protected void updatePosition(final DoubleMatrix1D p, final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DataBundle data) {
    final double lambda0 = data.getLambda0();
    final DoubleMatrix1D deltaX = (DoubleMatrix1D) _algebra.scale(p, -lambda0);
    final DoubleMatrix1D xNew = (DoubleMatrix1D) _algebra.add(data.getX(), deltaX);
    final DoubleMatrix1D yNew = function.evaluate(xNew);
    data.setDeltaX(deltaX);
    data.setDeltaY((DoubleMatrix1D) _algebra.subtract(yNew, data.getY()));
    data.setG2(data.getG1());
    data.setG1(_algebra.getInnerProduct(yNew, yNew));
  }

  private void bisectBacktrack(final DoubleMatrix1D p, final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DataBundle data) {
    do {
      data.setLambda0(data.getLambda0() * 0.1);
      updatePosition(p, function, data);

      if (data.getLambda0() == 0.0) {
        throw new MathException("Failed to converge");
      }
    } while (Double.isNaN(data.getG1()) || Double.isInfinite(data.getG1()) || Double.isNaN(data.getG2()) || Double.isInfinite(data.getG2()));

  }

  private void quadraticBacktrack(final DoubleMatrix1D p, final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DataBundle data) {
    final double lambda0 = data.getLambda0();
    final double g0 = data.getG0();
    final double lambda = Math.max(0.01 * lambda0, g0 * lambda0 * lambda0 / (data.getG1() + g0 * (2 * lambda0 - 1)));
    data.swapLambdaAndReplace(lambda);
    updatePosition(p, function, data);
  }

  private void cubicBacktrack(final DoubleMatrix1D p, final Function1D<DoubleMatrix1D, DoubleMatrix1D> function, final DataBundle data) {
    double temp1, temp2, temp3, temp4, temp5;
    final double lambda0 = data.getLambda0();
    final double lambda1 = data.getLambda1();
    final double g0 = data.getG0();
    temp1 = 1.0 / lambda0 / lambda0;
    temp2 = 1.0 / lambda1 / lambda1;
    temp3 = data.getG1() + g0 * (2 * lambda0 - 1.0);
    temp4 = data.getG2() + g0 * (2 * lambda1 - 1.0);
    temp5 = 1.0 / (lambda0 - lambda1);
    final double a = temp5 * (temp1 * temp3 - temp2 * temp4);
    final double b = temp5 * (-lambda1 * temp1 * temp3 + lambda0 * temp2 * temp4);
    double lambda = (-b + Math.sqrt(b * b + 6 * a * g0)) / 3 / a;
    lambda = Math.min(Math.max(lambda, 0.01 * lambda0), 0.75 * lambda1); // make sure new lambda is between 1% & 75% of old value
    data.swapLambdaAndReplace(lambda);
    updatePosition(p, function, data);
  }

  private boolean isConverged(final DataBundle data) {
    final DoubleMatrix1D deltaX = data.getDeltaX();
    final DoubleMatrix1D x = data.getX();
    final int n = deltaX.getNumberOfElements();
    double diff, scale;
    for (int i = 0; i < n; i++) {
      diff = Math.abs(deltaX.getEntry(i));
      scale = Math.abs(x.getEntry(i));
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
    private DoubleMatrix1D _deltaY;
    private DoubleMatrix1D _y;
    private DoubleMatrix1D _deltaX;
    private DoubleMatrix1D _x;

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

    public DoubleMatrix1D getDeltaY() {
      return _deltaY;
    }

    public DoubleMatrix1D getY() {
      return _y;
    }

    public DoubleMatrix1D getDeltaX() {
      return _deltaX;
    }

    public DoubleMatrix1D getX() {
      return _x;
    }

    public void setG0(final double g0) {
      _g0 = g0;
    }

    public void setG1(final double g1) {
      _g1 = g1;
    }

    public void setG2(final double g2) {
      _g2 = g2;
    }

    public void setLambda0(final double lambda0) {
      _lambda0 = lambda0;
    }

    public void setDeltaY(final DoubleMatrix1D deltaY) {
      _deltaY = deltaY;
    }

    public void setY(final DoubleMatrix1D y) {
      _y = y;
    }

    public void setDeltaX(final DoubleMatrix1D deltaX) {
      _deltaX = deltaX;
    }

    public void setX(final DoubleMatrix1D x) {
      _x = x;
    }

    public void swapLambdaAndReplace(final double lambda0) {
      _lambda1 = _lambda0;
      _lambda0 = lambda0;
    }
  }
}
