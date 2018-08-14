/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
/*
 * This code is copied from the original library from the `cern.jet.random` package.
 * Changes:
 * - package name
 * - missing Javadoc param tags
 * - reformat
 * - make package scoped
 */
/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose
is hereby granted without fee, provided that the above copyright notice appear in all copies and
that both that copyright notice and this permission notice appear in supporting documentation.
CERN makes no representations about the suitability of this software for any purpose.
It is provided "as is" without expressed or implied warranty.
*/
package com.opengamma.strata.math.impl.cern;

//CSOFF: ALL
/**
 * Abstract base class for all continous distributions.
 *
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 */
abstract class AbstractContinousDistribution extends AbstractDistribution {

  private static final long serialVersionUID = 1L;

  /**
  * Makes this class non instantiable, but still let's others inherit from it.
  */
  protected AbstractContinousDistribution() {
  }
}
