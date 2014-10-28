/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.platform.pricer.interp.methods;

import com.opengamma.platform.pricer.interp.PP_t;

public class Linear implements Interp1DMethodBacking {

	private static int ORDER = 1;
	
	public static Linear s_instance = new Linear();

	@Override
	public PP_t assembler(double[] x, double[] y) {
		int nx = x.length;

		// np is the number of pieces
		int np = nx - 1;

		int k = ORDER + 1;

		double[][] P = new double[np][k];

		// Compute and set the coefficients in the pp struct
		for (int i = 0; i < np; ++i) {
			double dx = x[i + 1] - x[i];
			double dy = y[i + 1] - y[i];
			P[i][0] = dy / dx;
			P[i][1] = y[i];
		}
		return new PP_t(x, P, np, k);
	}
}
