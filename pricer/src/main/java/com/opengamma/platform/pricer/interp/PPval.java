/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.interp;

/**
 * PPval polynomial evaluation
 */
public class PPval {

	/**
	 * Does table lookups.
	 * 
	 * @param breaks
	 *            the x values (required: sorted ascending).
	 * @param point
	 *            the point to lookup in x
	 * @return the indexes of the first element in \a breaks which is less than
	 *         \a points. If a member of \a points is less than the minimum
	 *         value in \a breaks the index is set to 0, likewise if greater
	 *         than the maxium value in breaks the index is set to \a npieces-1.
	 */
	public static int lookup_ppval(double[] breaks, double point) {
		int npieces = breaks.length;
		double searchingFor;
		int lower;
		int upper, mid;
		// find the first one
		lower = 0;
		searchingFor = point;
		upper = npieces;
		// fast escape if point off LHS
		if(point <= breaks[0])
		{
			return 0;
		}
		// fast escape if point off RHS
		if(point >= breaks[breaks.length - 1])
		{
			return npieces - 2;
		}		
		while (upper - lower > 1) {
			mid = (upper + lower) / 2;
			if (searchingFor >= breaks[mid])
				lower = mid;
			else
				upper = mid;
		}

		return lower;
	}

	/**
	 * Does table lookups.
	 * 
	 * @param breaks
	 *            the x values (required: sorted ascending).
	 * @param points
	 *            the points to lookup in x (required: sorted ascending).
	 * @param npieces
	 *            the number of values in \a breaks.
	 * @param npos
	 *            the number of values in \a points.
	 * @return the array of indexes of the first element in \a breaks which is
	 *         less than \a points. If a member of \a points is less than the
	 *         minimum value in \a breaks the index is set to 0, likewise if
	 *         greater than the maxium value in breaks the index is set to \a
	 *         npieces-1.
	 */
	public static int[] lookup_ppval(double[] breaks, double[] points) {
		int[] lookedup = new int[points.length];
		lookup_usebuffers(breaks, points, lookedup);
		return lookedup;
	}

	/**
	 * Does table lookups.
	 * 
	 * @param breaks
	 *            the x values (required: sorted ascending).
	 * @param points
	 *            the points to lookup in x (required: sorted ascending).
	 * @param npieces
	 *            the number of values in \a breaks.
	 * @param npos
	 *            the number of values in \a points.
	 * @param lookup
	 *            the return array of indexes of the first element in \a breaks
	 *            which is less than \a points. If a member of \a points is less
	 *            than the minimum value in \a breaks the index is set to 0,
	 *            likewise if greater than the maxium value in breaks the index
	 *            is set to \a npieces-1. Vector must have length \a npos
	 *            minimum
	 * @return void
	 */
	public static void lookup_usebuffers(double[] breaks, double[] points,
			int[] lookup) {
		int npieces = breaks.length;
		int npos = points.length;
		double searchingFor;
		int lower;
		int upper, mid;
		// find the first one
		lookup[0] = lookup_ppval(breaks, points[0]);
		// shrinking bisection, there's probably a quicker way to do this,
		// i.e. search forwards, then backwards, also including the shrink
		for (int i = 1; i < npos; i++) {
			searchingFor = points[i];
			lower = lookup[i - 1];
			upper = npieces;
			// fast escape if point off LHS
			if(points[i] <= breaks[0])
			{
				lookup[i] = 0;
				continue;
			}
			// fast escape if point off RHS
			if(points[i] >= breaks[breaks.length - 1])
			{
				lookup[i] = npieces - 2;
				continue;
			}	
			while (upper - lower > 1) {
				mid = (upper + lower) / 2;
				if (searchingFor >= breaks[mid])
					lower = mid;
				else
					upper = mid;
			}
			lookup[i] = lower;
		}

	};

	/**
	 * Evaluates a polynomial at given positions.
	 *
	 * @param pp
	 *            a pointer to a the polynomial to evaluate expressed as a \a
	 *            pp_t.
	 * @param positions
	 *            a pointer to the vector of positions the positions at which
	 *            the polynomial shall be evaluated.
	 * @return a pointer to a npos length vector of the values of the polynomial
	 *         evaluated at the positions \a positions.
	 */
	public static double[] ppval(PP_t pp, double[] positions) {
		double[] ppval_vector = new double[positions.length];
		int[] lookupbuffer = new int[positions.length];
		ppval_usebuffers(pp, positions, ppval_vector, lookupbuffer);
		return ppval_vector;
	}

	/**
	 * Evaluates a polynomial at given position.
	 *
	 * @param pp
	 *            a pointer to a the polynomial to evaluate expressed as a \a
	 *            pp_t.
	 * @param position
	 *            the location at which the evaluation shall take place
	 * @return a pointer to a npos length vector of the values of the polynomial
	 *         evaluated at the positions \a positions.
	 */
	public static double ppval(PP_t pp, double position) {
		return evals(pp, position, lookup_ppval(pp.get_X(), position));
	}

	/**
	 * Evaluates a polynomial at given positions using predefined buffers.
	 *
	 * @param pp
	 *            a pointer to the polynomial to evaluate expressed as a \a
	 *            pp_t.
	 * @param positions
	 *            a pointer to the vector of positions the positions at which
	 *            the polynomial shall be evaluated.
	 * @param ppval_vector
	 *            a vector of the values of the polynomial evaluated at the
	 *            positions \a positions must have length of at least \a npos.
	 * @param lookupbuffer
	 *            a buffer, must have length of at least \a npos.
	 * @return void.
	 */
	public static void ppval_usebuffers(PP_t pp, double[] positions,
			double[] ppval_vector, int[] lookupbuffer) {
		lookup_usebuffers(pp.get_X(), positions, lookupbuffer);
		evalv(pp, positions, lookupbuffer, ppval_vector);
	}

	/**
	 * Evaluates a polynomial at given positions with pre-emptive lookup.
	 *
	 * @param pp
	 *            a pointer to a the polynomial to evaluate expressed as a \a
	 *            pp_t.
	 * @param positions
	 *            a pointer to the vector of positions the positions at which
	 *            the polynomial shall be evaluated.
	 * @param preemptlookup
	 *            the location in the vector at which the lookup would resolve
	 *            were it to be computed.
	 * @return a pointer to a npos length vector of the values of the polynomial
	 *         evaluated at the positions \a positions.
	 */
	public static double[] ppval_preempt_lookup(PP_t pp, double[] positions,
			int[] preemptlookup) {
		double[] ppval_vector = new double[positions.length];
		evalv(pp, positions, preemptlookup, ppval_vector);
		return ppval_vector;
	}

	/**
	 * Evaluates a piecewise polynomial a given position
	 * 
	 * @param pp
	 *            the piecewise polynomial
	 * @param position
	 *            the location at which the evaluation shall take place
	 * @param preemptlookup
	 *            the location in the vector at which the lookup would resolve
	 *            were it to be computed.
	 * @return the value of the piecewise polynomial evaluated at \a position
	 */
	public static double ppval_preempt_lookup(PP_t pp, double position,
			int preemptlookup) {
		return evals(pp, position, preemptlookup);
	}

	/**
	 * Evaluates a polynomial at given positions with pre-emptive lookup using
	 * predefined buffers.
	 *
	 * @param pp
	 *            a pointer to a the polynomial to evaluate expressed as a \a
	 *            pp_t.
	 * @param positions
	 *            a pointer to the vector of positions the positions at which
	 *            the polynomial shall be evaluated.
	 * @param preemptlookup
	 *            the location in the vector at which the lookup would resolve
	 *            were it to be computed.
	 * @param ppval_vector
	 *            a vector of the values of the polynomial evaluated at the
	 *            positions \a positions must have length of at least \a npos.
	 * @return void.
	 */
	public static void ppval_preempt_lookup_usebuffers(PP_t pp,
			double[] positions, int[] preemptlookup, double[] ppval_vector) {
		lookup_usebuffers(pp.get_X(), positions, preemptlookup);
		evalv(pp, positions, preemptlookup, ppval_vector);
	}

	// the methods that do the work

	// does scalar eval
	private static double evals(PP_t pp, double position, int lookedup_val) {
		return inlinable_horner(position, lookedup_val, pp.get_X(), pp.get_P()[lookedup_val], pp.get_K());
	};

	// does vector eval
	private static void evalv(PP_t pp, double[] positions, int[] lookedup_vals,
			double[] ppval_vector) {
		int npos = positions.length;
		for (int i = 0; i < npos; i++) {
			ppval_vector[i] = inlinable_horner(positions[i], lookedup_vals[i], pp.get_X(), pp.get_P()[lookedup_vals[i]], pp.get_K());
		}

	};

	// horner method
	private static double inlinable_horner(double position, int local_index,
			double[] X, double[] Prow, int nCoefs) {
		double xe = position;
		double xleft = X[local_index];
		final double xdiff = xe - xleft;
		double res = Prow[0];
		for (int j = 1; j < nCoefs; j++) {
			res *= xdiff;
			res += Prow[j];
		}
		return res;
	}

}
