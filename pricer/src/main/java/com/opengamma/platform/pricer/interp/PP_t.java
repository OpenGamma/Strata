/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.interp;

public class PP_t {

	// typedef struct {
	// double *X; // The points
	// double *P; // The coeffs, rows contain the coeffs ordered from highest to
	// lowest
	// int N; // Number of polynomial pieces
	// int K; // Polynomial order + 1
	// } pp_t;

	private double[] _X;
	private double[][] _P;
	private int _N;
	private int _K;

	/**
	 * Wire through ctor
	 * @param X
	 * @param P
	 * @param N
	 * @param K
	 */
	public PP_t(double[] X, double[][] P, int N, int K) {
		_X = X;
		_P = P;
		_N = N;
		_K = K;
	}

	public double[] get_X() {
		return _X;
	}

	public double[][] get_P() {
		return _P;
	}

	public int get_N() {
		return _N;
	}

	public int get_K() {
		return _K;
	};

	public void print_pp() {
		int PP_ORDER = this._K - 1;
		System.out.format("__PP STRUCT__\n\n");
		System.out.format("X =\n ");
		for (int i = 0; i <= this._N; i++)
			System.out.format("%16.16f ", this._X[i]);
		System.out.format("\n\n");
		System.out.format("P =\n ");
		for (int i = 0; i < this._N; i++) {
			for (int j = 0; j < PP_ORDER; j++)
				System.out.format("%16.16f ", this._P[i][j]);
			System.out.format("\n ");
		}
		System.out.format("\n");
		System.out.format("N = %d\n", this._N);
		System.out.format("K = %d\n", this._K);
	}

}
