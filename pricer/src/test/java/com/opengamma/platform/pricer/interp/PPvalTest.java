/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.platform.pricer.interp;

import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.maths.helpers.FuzzyEquals;
import com.opengamma.platform.pricer.interp.methods.Linear;

@Test
public class PPvalTest {

	double x[] = new double[] { 1.1988e0, 1.5321e0, 2.8306e0, 3.5228e0,
			5.2120e0, 5.5397e0, 6.5623e0, 7.5492e0, 8.4498e0, 9.4649e0 };
	double[] y = new double[] { 4.628112e0, 4.674382e0, 5.267772e0, 7.311721e0,
			2.221348e0, 1.117535e0, 1.499433e0, 1.782051e0, 7.596913e0,
			4.750974e0 };
	double[] xx = new double[] { 0.1984020000000000e0, 0.8419940000000000e0,
			1.2041960000000000e0, 2.4419930000000001e0, 3.0759629999999998e0,
			4.6108060000000002e0, 5.3652119999999996e0, 5.6010280000000003e0,
			7.5732670000000004e0, 8.9191389999999995e0 };
	double[] expected = new double[] { 4.4892328657065708e0,
			4.5785788058205821e0, 4.6288610936693670e0, 5.0901859332075468e0,
			5.9922872535206579e0, 4.0330350147774086e0, 1.7052741600366192e0,
			1.1404384231801292e0, 1.9374432759871230e0, 6.2810720342616504e0 };

	int lexpected_c = 15;
	int lbase_c = 10;
	double[] lx = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
	double[] lxx = new double[] { 0.5, 1, 1.5, 2, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5,
			8.5, 9, 9.5, 10, 10.5 };
	int[] lexpected = new int[] { 0, 0, 0, 1, 1, 2, 3, 4, 5, 6, 7, 8, 8, 8, 8 };

	@Test
	public void lookup_ppval_Test() {
		int[] lookedup_vals = PPval.lookup_ppval(lx, lxx);
		for (int k = 0; k < lexpected_c; k++) {
			assertTrue(lexpected[k] == lookedup_vals[k]);
			assertTrue(lookedup_vals[k] == PPval.lookup_ppval(lx, lxx[k]));
		}
	}

	@Test
	public void lookup_ppval_check_rh_infinite_inverval_Test() {
		int lookedup_val = PPval.lookup_ppval(lx, 10);
		assertTrue(lookedup_val == lx.length - 2);
	}

	@Test
	public void look_up_ppval_test_hithigh_point0_branch_Test() {
		double[] xval = { 11 };
		int[] lookedup_vals = PPval.lookup_ppval(lx, xval);
		assertTrue(lookedup_vals[0] == 8);
		assertTrue(lookedup_vals[0] == PPval.lookup_ppval(lx, xval[0]));
	}

	@Test
	public void look_up_ppval_usebuffers_Test() {
		int[] lookedup_vals = new int[lexpected_c];
		PPval.lookup_usebuffers(lx, lxx, lookedup_vals);

		for (int k = 0; k < lexpected_c; k++) {
			assertTrue(lexpected[k] == lookedup_vals[k]);
		}

	}

	@Test
	public void look_up_ppval_usebufferstest_hithigh_point0_branch_Test() {
		int[] lookedup_vals = new int[1];
		double[] xval = { 11 };
		PPval.lookup_usebuffers(lx, xval, lookedup_vals);
		assertTrue(lookedup_vals[0] == 8);
	}

	@Test
	public void VectorTest() {
		PP_t pp = new Linear().createpp(x, y);
		double[] yy = PPval.ppval(pp, xx);
		assertTrue(FuzzyEquals.ArrayFuzzyEquals(expected, yy));
		// check single dispatch works
		for (int i = 0; i < xx.length; i++) {
			assertTrue(FuzzyEquals.SingleValueFuzzyEquals(expected[i],
					PPval.ppval(pp, xx[i])));
		}
	}

	@Test
	public void PreemptiveVectorTest() {
		PP_t pp = new Linear().createpp(x, y);
		int[] lookedup_vals = PPval.lookup_ppval(pp.get_X(), xx);
		double[] yy = PPval.ppval_preempt_lookup(pp, xx, lookedup_vals);
		assertTrue(FuzzyEquals.ArrayFuzzyEquals(expected, yy));
		// check single dispatch works
		for (int i = 0; i < xx.length; i++) {
			assertTrue(FuzzyEquals.SingleValueFuzzyEquals(expected[i],
					PPval.ppval_preempt_lookup(pp, xx[i], lookedup_vals[i])));
		}
	}

	@Test
	public void PreemptiveBufferedVectorTest() {
		PP_t pp = new Linear().createpp(x, y);
		int[] lookedup_vals = new int[xx.length];
		double[] yy = new double[xx.length];
		PPval.ppval_preempt_lookup_usebuffers(pp, xx, lookedup_vals, yy);
		assertTrue(FuzzyEquals.ArrayFuzzyEquals(expected, yy));
	}

}
