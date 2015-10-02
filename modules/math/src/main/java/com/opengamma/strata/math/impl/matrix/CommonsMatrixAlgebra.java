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
import com.opengamma.strata.math.impl.util.CommonsMathWrapper;

/**
 * Provides matrix algebra by using the <a href = "http://commons.apache.org/math/api-2.1/index.html">Commons library</a>. 
 */
public class CommonsMatrixAlgebra extends MatrixAlgebra {

  /**
   * {@inheritDoc}
   */
  @Override
  public double getCondition(final Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix2D) {
      final RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
      final SingularValueDecomposition svd = new SingularValueDecomposition(temp);
      return svd.getConditionNumber();
    }
    throw new IllegalArgumentException("Can only find condition number of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getDeterminant(final Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix2D) {
      final RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
      final LUDecomposition lud = new LUDecomposition(temp);
      return lud.getDeterminant();
    }
    throw new IllegalArgumentException("Can only find determinant of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getInnerProduct(final Matrix<?> m1, final Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleMatrix1D && m2 instanceof DoubleMatrix1D) {
      final RealVector t1 = CommonsMathWrapper.wrap((DoubleMatrix1D) m1);
      final RealVector t2 = CommonsMathWrapper.wrap((DoubleMatrix1D) m2);
      return t1.dotProduct(t2);
    }
    throw new IllegalArgumentException("Can only find inner product of DoubleMatrix1D; have " + m1.getClass() + " and " + m2.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getInverse(final Matrix<?> m) {
    ArgChecker.notNull(m, "matrix was null");
    if (m instanceof DoubleMatrix2D) {
      final RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
      final SingularValueDecomposition sv = new SingularValueDecomposition(temp);
      final RealMatrix inv = sv.getSolver().getInverse();
      return CommonsMathWrapper.unwrap(inv);
    }
    throw new IllegalArgumentException("Can only find inverse of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getNorm1(final Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix1D) {
      final RealVector temp = CommonsMathWrapper.wrap((DoubleMatrix1D) m);
      return temp.getL1Norm();
    } else if (m instanceof DoubleMatrix2D) {
      final RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
      // TODO find if commons implements this anywhere, so we are not doing it
      // by hand
      double max = 0.0;
      for (int col = temp.getColumnDimension() - 1; col >= 0; col--) {
        max = Math.max(max, temp.getColumnVector(col).getL1Norm());
      }
      return max;

    }
    throw new IllegalArgumentException("Can only find norm1 of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getNorm2(final Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix1D) {
      final RealVector temp = CommonsMathWrapper.wrap((DoubleMatrix1D) m);
      return temp.getNorm();
    } else if (m instanceof DoubleMatrix2D) {
      final RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
      final SingularValueDecomposition svd = new SingularValueDecomposition(temp);
      return svd.getNorm();
    }
    throw new IllegalArgumentException("Can only find norm2 of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getNormInfinity(final Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix1D) {
      final RealVector temp = CommonsMathWrapper.wrap((DoubleMatrix1D) m);
      return temp.getLInfNorm();
    } else if (m instanceof DoubleMatrix2D) {
      final RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
      //REVIEW Commons getNorm() is wrong - it returns the column norm
      // TODO find if commons implements this anywhere, so we are not doing it
      // by hand
      double max = 0.0;
      for (int row = temp.getRowDimension() - 1; row >= 0; row--) {
        max = Math.max(max, temp.getRowVector(row).getL1Norm());
      }
      return max;
    }
    throw new IllegalArgumentException("Can only find normInfinity of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getOuterProduct(final Matrix<?> m1, final Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleMatrix1D && m2 instanceof DoubleMatrix1D) {
      final RealVector t1 = CommonsMathWrapper.wrap((DoubleMatrix1D) m1);
      final RealVector t2 = CommonsMathWrapper.wrap((DoubleMatrix1D) m2);
      return CommonsMathWrapper.unwrap(t1.outerProduct(t2));
    }
    throw new IllegalArgumentException("Can only find outer product of DoubleMatrix1D; have " + m1.getClass() + " and " + m2.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getPower(final Matrix<?> m, final int p) {
    ArgChecker.notNull(m, "m");
    RealMatrix temp;
    if (m instanceof DoubleMatrix2D) {
      temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
    } else {
      throw new IllegalArgumentException("Can only find powers of DoubleMatrix2D; have " + m.getClass());
    }
    return CommonsMathWrapper.unwrap(temp.power(p));
  }

  /**
   * Returns a real matrix raised to some real power 
   * Currently this method is limited to symmetric matrices only as Commons Math does not support the diagonalization of asymmetric matrices  
   * @param m The <strong>symmetric</strong> matrix to take the power of. 
   * @param p The power to raise to matrix to
   * @return The result
   */
  @Override
  public DoubleMatrix2D getPower(final Matrix<?> m, final double p) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTrace(final Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix2D) {
      final RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
      return temp.getTrace();
    }
    throw new IllegalArgumentException("Can only find trace of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getTranspose(final Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix2D) {
      final RealMatrix temp = CommonsMathWrapper.wrap((DoubleMatrix2D) m);
      return CommonsMathWrapper.unwrap(temp.transpose());
    }
    throw new IllegalArgumentException("Can only find transpose of DoubleMatrix2D; have " + m.getClass());
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
  public Matrix<?> multiply(final Matrix<?> m1, final Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    ArgChecker.isTrue(!(m1 instanceof DoubleMatrix1D), "Cannot have 1D matrix as first argument");
    if (m1 instanceof DoubleMatrix2D) {
      final RealMatrix t1 = CommonsMathWrapper.wrap((DoubleMatrix2D) m1);
      RealMatrix t2;
      if (m2 instanceof DoubleMatrix1D) {
        t2 = CommonsMathWrapper.wrapAsMatrix((DoubleMatrix1D) m2);
      } else if (m2 instanceof DoubleMatrix2D) {
        t2 = CommonsMathWrapper.wrap((DoubleMatrix2D) m2);
      } else {
        throw new IllegalArgumentException("Can only have 1D or 2D matrix as second argument");
      }
      return CommonsMathWrapper.unwrap(t1.multiply(t2));
    }
    throw new IllegalArgumentException("Can only multiply 2D and 1D matrices");
  }

}
