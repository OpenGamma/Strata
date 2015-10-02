/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.linearalgebra.TridiagonalMatrix;

/**
 * A minimal implementation of matrix algebra.
 * <p>
 * This includes only some of the multiplications.
 * For more advanced operations, such as calculating the inverse, use {@link CommonsMatrixAlgebra}.
 */
public class OGMatrixAlgebra extends MatrixAlgebra {

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public double getCondition(Matrix<?> m) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public double getDeterminant(Matrix<?> m) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getInnerProduct(Matrix<?> m1, Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleMatrix1D && m2 instanceof DoubleMatrix1D) {
      double[] a = ((DoubleMatrix1D) m1).getData();
      double[] b = ((DoubleMatrix1D) m2).getData();
      int l = a.length;
      ArgChecker.isTrue(l == b.length, "Matrix size mismacth");
      double sum = 0.0;
      for (int i = 0; i < l; i++) {
        sum += a[i] * b[i];
      }
      return sum;
    }
    throw new IllegalArgumentException("Can only find inner product of DoubleMatrix1D; have " + m1.getClass() +
        " and " + m2.getClass());
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public DoubleMatrix2D getInverse(Matrix<?> m) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public double getNorm1(Matrix<?> m) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc} This is only implemented for {@link DoubleMatrix1D}.
   * @throws IllegalArgumentException If the matrix is not a {@link DoubleMatrix1D}
   */
  @Override
  public double getNorm2(Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix1D) {
      double[] a = ((DoubleMatrix1D) m).getData();
      int l = a.length;
      double sum = 0.0;
      for (int i = 0; i < l; i++) {
        sum += a[i] * a[i];
      }
      return Math.sqrt(sum);
    } else if (m instanceof DoubleMatrix2D) {
      throw new UnsupportedOperationException();
    }
    throw new IllegalArgumentException("Can only find norm2 of a DoubleMatrix1D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public double getNormInfinity(Matrix<?> m) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getOuterProduct(Matrix<?> m1, Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleMatrix1D && m2 instanceof DoubleMatrix1D) {
      double[] a = ((DoubleMatrix1D) m1).getData();
      double[] b = ((DoubleMatrix1D) m2).getData();
      int m = a.length;
      int n = b.length;
      double[][] res = new double[m][n];
      int i, j;
      for (i = 0; i < m; i++) {
        for (j = 0; j < n; j++) {
          res[i][j] = a[i] * b[j];
        }
      }
      return new DoubleMatrix2D(res);
    }
    throw new IllegalArgumentException("Can only find outer product of DoubleMatrix1D; have " + m1.getClass() +
        " and " + m2.getClass());
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public DoubleMatrix2D getPower(Matrix<?> m, int p) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public double getTrace(Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix2D) {
      double[][] data = ((DoubleMatrix2D) m).getData();
      int rows = data.length;
      ArgChecker.isTrue(rows == data[0].length, "Matrix not square");
      double sum = 0.0;
      for (int i = 0; i < rows; i++) {
        sum += data[i][i];
      }
      return sum;
    }
    throw new IllegalArgumentException("Can only take the trace of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DoubleMatrix2D getTranspose(Matrix<?> m) {
    ArgChecker.notNull(m, "m");
    if (m instanceof IdentityMatrix) {
      return (IdentityMatrix) m;
    }
    if (m instanceof DoubleMatrix2D) {
      double[][] data = ((DoubleMatrix2D) m).getData();
      int rows = data.length;
      int cols = data[0].length;
      double[][] res = new double[cols][rows];
      int i, j;
      for (i = 0; i < cols; i++) {
        for (j = 0; j < rows; j++) {
          res[i][j] = data[j][i];
        }
      }
      return new DoubleMatrix2D(res);
    }
    throw new IllegalArgumentException("Can only take transpose of DoubleMatrix2D; have " + m.getClass());
  }

  /**
   * {@inheritDoc} The following combinations of input matrices m1 and m2 are allowed:
   * <ul>
   * <li>m1 = 2-D matrix, m2 = 2-D matrix, returns $\mathbf{C} = \mathbf{AB}$
   * <li>m1 = 2-D matrix, m2 = 1-D matrix, returns $\mathbf{C} = \mathbf{A}b$
   * <li>m1 = 1-D matrix, m2 = 2-D matrix, returns $\mathbf{C} = a^T\mathbf{B}$
   * </ul>
   */
  @Override
  public Matrix<?> multiply(Matrix<?> m1, Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof IdentityMatrix) {
      if (m2 instanceof IdentityMatrix) {
        return multiply((IdentityMatrix) m1, (IdentityMatrix) m2);
      } else if (m2 instanceof DoubleMatrix1D) {
        return multiply((IdentityMatrix) m1, (DoubleMatrix1D) m2);
      } else if (m2 instanceof DoubleMatrix2D) {
        return multiply((IdentityMatrix) m1, (DoubleMatrix2D) m2);
      }
      throw new IllegalArgumentException("can only handle IdentityMatrix by DoubleMatrix2D or DoubleMatrix1D, have " +
          m1.getClass() + " and " + m2.getClass());
    }
    if (m2 instanceof IdentityMatrix) {
      if (m1 instanceof DoubleMatrix1D) {
        return multiply((DoubleMatrix1D) m1, (IdentityMatrix) m2);
      } else if (m1 instanceof DoubleMatrix2D) {
        return multiply((DoubleMatrix2D) m1, (IdentityMatrix) m2);
      }
      throw new IllegalArgumentException("can only handle  DoubleMatrix2D or DoubleMatrix1D by IdentityMatrix, have " +
          m1.getClass() + " and " + m2.getClass());
    }
    if (m1 instanceof TridiagonalMatrix && m2 instanceof DoubleMatrix1D) {
      return multiply((TridiagonalMatrix) m1, (DoubleMatrix1D) m2);
    } else if (m1 instanceof DoubleMatrix1D && m2 instanceof TridiagonalMatrix) {
      return multiply((DoubleMatrix1D) m1, (TridiagonalMatrix) m2);
    } else if (m1 instanceof DoubleMatrix2D && m2 instanceof DoubleMatrix2D) {
      return multiply((DoubleMatrix2D) m1, (DoubleMatrix2D) m2);
    } else if (m1 instanceof DoubleMatrix2D && m2 instanceof DoubleMatrix1D) {
      return multiply((DoubleMatrix2D) m1, (DoubleMatrix1D) m2);
    } else if (m1 instanceof DoubleMatrix1D && m2 instanceof DoubleMatrix2D) {
      return multiply((DoubleMatrix1D) m1, (DoubleMatrix2D) m2);
    }
    throw new IllegalArgumentException(
        "Can only multiply two DoubleMatrix2D; a DoubleMatrix2D and a DoubleMatrix1D; " +
            "or a DoubleMatrix1D and a DoubleMatrix2D. have " + m1.getClass() + " and " + m2.getClass());
  }

  /**
   * {@inheritDoc}
   * @throws UnsupportedOperationException always
   */
  @Override
  public DoubleMatrix2D getPower(Matrix<?> m, double p) {
    throw new UnsupportedOperationException();
  }

  private DoubleMatrix2D multiply(IdentityMatrix idet, DoubleMatrix2D m) {
    ArgChecker.isTrue(idet.getSize() == m.getNumberOfRows(),
        "size of identity matrix ({}) does not match number or rows of m ({})", idet.getSize(), m.getNumberOfRows());
    return m;
  }

  private DoubleMatrix2D multiply(DoubleMatrix2D m, IdentityMatrix idet) {
    ArgChecker.isTrue(idet.getSize() == m.getNumberOfColumns(),
        "size of identity matrix ({}) does not match number or columns of m ({})", idet.getSize(),
        m.getNumberOfColumns());
    return m;
  }

  private IdentityMatrix multiply(IdentityMatrix i1, IdentityMatrix i2) {
    ArgChecker.isTrue(i1.getSize() == i2.getSize(),
        "size of identity matrix 1 ({}) does not match size of identity matrix 2 ({})", i1.getSize(), i2.getSize());
    return i1;
  }

  private DoubleMatrix2D multiply(DoubleMatrix2D m1, DoubleMatrix2D m2) {
    double[][] a = m1.getData();
    double[][] b = m2.getData();
    int p = b.length;
    ArgChecker.isTrue(
        a[0].length == p,
        "Matrix size mismatch. m1 is " + m1.getNumberOfRows() + " by " + m1.getNumberOfColumns() + ", but m2 is " +
            m2.getNumberOfRows() + " by " + m2.getNumberOfColumns());
    int m = a.length;
    int n = b[0].length;
    double sum;
    double[][] res = new double[m][n];
    int i, j, k;
    for (i = 0; i < m; i++) {
      for (j = 0; j < n; j++) {
        sum = 0.0;
        for (k = 0; k < p; k++) {
          sum += a[i][k] * b[k][j];
        }
        res[i][j] = sum;
      }
    }
    return new DoubleMatrix2D(res);
  }

  private DoubleMatrix1D multiply(IdentityMatrix matrix, DoubleMatrix1D vector) {
    ArgChecker.isTrue(matrix.getSize() == vector.getNumberOfElements(),
        "size of identity matrix ({}) does not match size of vector ({})", matrix.getSize(),
        vector.getNumberOfElements());
    return vector;
  }

  private DoubleMatrix1D multiply(DoubleMatrix1D vector, IdentityMatrix matrix) {
    ArgChecker.isTrue(matrix.getSize() == vector.getNumberOfElements(),
        "size of identity matrix ({}) does not match size of vector ({})", matrix.getSize(),
        vector.getNumberOfElements());
    return vector;
  }

  private DoubleMatrix1D multiply(DoubleMatrix2D matrix, DoubleMatrix1D vector) {
    double[][] a = matrix.getData();
    double[] b = vector.getData();
    int n = b.length;
    ArgChecker.isTrue(a[0].length == n, "Matrix/vector size mismatch");
    int m = a.length;
    double[] res = new double[m];
    int i, j;
    double sum;
    for (i = 0; i < m; i++) {
      sum = 0.0;
      for (j = 0; j < n; j++) {
        sum += a[i][j] * b[j];
      }
      res[i] = sum;
    }
    return new DoubleMatrix1D(res);
  }

  private DoubleMatrix1D multiply(TridiagonalMatrix matrix, DoubleMatrix1D vector) {
    double[] a = matrix.getLowerSubDiagonalData();
    double[] b = matrix.getDiagonalData();
    double[] c = matrix.getUpperSubDiagonalData();
    double[] x = vector.getData();
    int n = x.length;
    ArgChecker.isTrue(b.length == n, "Matrix/vector size mismatch");
    double[] res = new double[n];
    int i;
    res[0] = b[0] * x[0] + c[0] * x[1];
    res[n - 1] = b[n - 1] * x[n - 1] + a[n - 2] * x[n - 2];
    for (i = 1; i < n - 1; i++) {
      res[i] = a[i - 1] * x[i - 1] + b[i] * x[i] + c[i] * x[i + 1];
    }
    return new DoubleMatrix1D(res);
  }

  private DoubleMatrix1D multiply(DoubleMatrix1D vector, DoubleMatrix2D matrix) {
    double[] a = vector.getData();
    double[][] b = matrix.getData();
    int n = a.length;
    ArgChecker.isTrue(b.length == n, "Matrix/vector size mismatch");
    int m = b[0].length;
    double[] res = new double[m];
    int i, j;
    double sum;
    for (i = 0; i < m; i++) {
      sum = 0.0;
      for (j = 0; j < n; j++) {
        sum += a[j] * b[j][i];
      }
      res[i] = sum;
    }
    return new DoubleMatrix1D(res);
  }

  private DoubleMatrix1D multiply(DoubleMatrix1D vector, TridiagonalMatrix matrix) {
    double[] a = matrix.getLowerSubDiagonalData();
    double[] b = matrix.getDiagonalData();
    double[] c = matrix.getUpperSubDiagonalData();
    double[] x = vector.getData();
    int n = x.length;
    ArgChecker.isTrue(b.length == n, "Matrix/vector size mismatch");
    double[] res = new double[n];
    int i;
    res[0] = b[0] * x[0] + a[0] * x[1];
    res[n - 1] = b[n - 1] * x[n - 1] + c[n - 2] * x[n - 2];
    for (i = 1; i < n - 1; i++) {
      res[i] = a[i] * x[i + 1] + b[i] * x[i] + c[i - 1] * x[i - 1];
    }
    return new DoubleMatrix1D(res);
  }

}
