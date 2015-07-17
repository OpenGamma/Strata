/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

/**
 * CDS can be quoted in one of three ways.
 * <p>
 * The pre-April 2009 Par Spreads; and the post April 2009 'Big Bang' points up-front (PUF) and Quoted Spreads.
 * 
 * @see CdsParSpread
 * @see CdsQuotedSpread
 * @see PointsUpFront
 */
public interface CdsQuoteConvention {

  double getCoupon();

}
