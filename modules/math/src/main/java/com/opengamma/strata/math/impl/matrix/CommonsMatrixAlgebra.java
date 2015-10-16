/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.array.Matrix;
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * Provides matrix algebra by using the <a href = "http://commons.apache.org/math/api-2.1/index.html">Commons library</a>. 
 */
public class CommonsMatrixAlgebra extends MatrixAlgebra {

  @Override
  public double getCondition(Matrix m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix) {
      RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
      SingularValueDecomposition svd = new SingularValueDecomposition(temp);
      return svd.getConditionNumber();
    }
    throw new IllegalArgumentException("Can only find condition number of DoubleMatrix; have " + m.getClass());
  }

  @Override
  public double getDeterminant(Matrix m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix) {
      RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
      LUDecomposition lud = new LUDecomposition(temp);
      return lud.getDeterminant();
    }
    throw new IllegalArgumentException("Can only find determinant of DoubleMatrix; have " + m.getClass());
  }

  @Override
  public double getInnerProduct(Matrix m1, Matrix m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleArray && m2 instanceof DoubleArray) {
      RealVector t1 = CommonsMathWrapper.wrap((DoubleArray) m1);
      RealVector t2 = CommonsMathWrapper.wrap((DoubleArray) m2);
      return t1.dotProduct(t2);
    }
    throw new IllegalArgumentException("Can only find inner product of DoubleArray; have " +
        m1.getClass() + " and " + m2.getClass());
  }

  @Override
  public DoubleMatrix getInverse(Matrix m) {
    ArgChecker.notNull(m, "matrix was null");
    if (m instanceof DoubleMatrix) {
      RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
      SingularValueDecomposition sv = new SingularValueDecomposition(temp);
      RealMatrix inv = sv.getSolver().getInverse();
      return CommonsMathWrapper.unwrap(inv);
    }
    throw new IllegalArgumentException("Can only find inverse of DoubleMatrix; have " + m.getClass());
  }

  @Override
  public double getNorm1(Matrix m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleArray) {
      RealVector temp = CommonsMathWrapper.wrap((DoubleArray) m);
      return temp.getL1Norm();
    } else if (m instanceof DoubleMatrix) {
      RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
      // TODO find if commons implements this anywhere, so we are not doing it
      // by hand
      double max = 0.0;
      for (int col = temp.getColumnDimension() - 1; col >= 0; col--) {
        max = Math.max(max, temp.getColumnVector(col).getL1Norm());
      }
      return max;

    }
    throw new IllegalArgumentException("Can only find norm1 of DoubleMatrix; have " + m.getClass());
  }

  @Override
  public double getNorm2(Matrix m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleArray) {
      RealVector temp = CommonsMathWrapper.wrap((DoubleArray) m);
      return temp.getNorm();
    } else if (m instanceof DoubleMatrix) {
      RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
      SingularValueDecomposition svd = new SingularValueDecomposition(temp);
      return svd.getNorm();
    }
    throw new IllegalArgumentException("Can only find norm2 of DoubleMatrix; have " + m.getClass());
  }

  @Override
  public double getNormInfinity(Matrix m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleArray) {
      RealVector temp = CommonsMathWrapper.wrap((DoubleArray) m);
      return temp.getLInfNorm();
    } else if (m instanceof DoubleMatrix) {
      RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
      //REVIEW Commons getNorm() is wrong - it returns the column norm
      // TODO find if commons implements this anywhere, so we are not doing it
      // by hand
      double max = 0.0;
      for (int row = temp.getRowDimension() - 1; row >= 0; row--) {
        max = Math.max(max, temp.getRowVector(row).getL1Norm());
      }
      return max;
    }
    throw new IllegalArgumentException("Can only find normInfinity of DoubleMatrix; have " + m.getClass());
  }

  @Override
  public DoubleMatrix getOuterProduct(Matrix m1, Matrix m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleArray && m2 instanceof DoubleArray) {
      RealVector t1 = CommonsMathWrapper.wrap((DoubleArray) m1);
      RealVector t2 = CommonsMathWrapper.wrap((DoubleArray) m2);
      return CommonsMathWrapper.unwrap(t1.outerProduct(t2));
    }
    throw new IllegalArgumentException("Can only find outer product of DoubleArray; have " +
        m1.getClass() + " and " + m2.getClass());
  }

  @Override
  public DoubleMatrix getPower(Matrix m, int p) {
    ArgChecker.notNull(m, "m");
    RealMatrix temp;
    if (m instanceof DoubleMatrix) {
      temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
    } else {
      throw new IllegalArgumentException("Can only find powers of DoubleMatrix; have " + m.getClass());
    }
    return CommonsMathWrapper.unwrap(temp.power(p));
  }

  /**
   * Returns a real matrix raised to some real power 
   * Currently this method is limited to symmetric matrices only as Commons Math does not
   * support the diagonalization of asymmetric matrices.
   * 
   * @param m The <strong>symmetric</strong> matrix to take the power of. 
   * @param p The power to raise to matrix to
   * @return The result
   */
  @Override
  public DoubleMatrix getPower(Matrix m, double p) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getTrace(Matrix m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix) {
      RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
      return temp.getTrace();
    }
    throw new IllegalArgumentException("Can only find trace of DoubleMatrix; have " + m.getClass());
  }

  @Override
  public DoubleMatrix getTranspose(Matrix m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix) {
      RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix) m);
      return CommonsMathWrapper.unwrap(temp.transpose());
    }
    throw new IllegalArgumentException("Can only find transpose of DoubleMatrix; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   * The following combinations of input matrices m1 and m2 are allowed:
   * <ul>
   * <li> m1 = 2-D matrix, m2 = 2-D matrix, returns $\mathbf{C} = \mathbf{AB}$
   * <li> m1 = 2-D matrix, m2 = 1-D matrix, returns $\mathbf{C} = \mathbf{A}b$
   * </ul>
   */
  @Override
  public Matrix multiply(Matrix m1, Matrix m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    ArgChecker.isTrue(!(m1 instanceof DoubleArray), "Cannot have 1D matrix as first argument");
    if (m1 instanceof DoubleMatrix) {
      RealMatrix t1 = CommonsMathWrapper.wrap((DoubleMatrix) m1);
      RealMatrix t2;
      if (m2 instanceof DoubleArray) {
        t2 = CommonsMathWrapper.wrapAsMatrix((DoubleArray) m2);
      } else if (m2 instanceof DoubleMatrix) {
        t2 = CommonsMathWrapper.wrap((DoubleMatrix) m2);
      } else {
        throw new IllegalArgumentException("Can only have 1D or 2D matrix as second argument");
      }
      return CommonsMathWrapper.unwrap(t1.multiply(t2));
    }
    throw new IllegalArgumentException("Can only multiply 2D and 1D matrices");
  }

}
