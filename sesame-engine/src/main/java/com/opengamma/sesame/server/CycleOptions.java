/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.server;

/**
 * This is a marker interface for the cycle options to be run.
 */
 // todo - we could just use Iterable<IndividualCycleOptions> rather than this interface
public interface CycleOptions extends Iterable<IndividualCycleOptions> {

}
