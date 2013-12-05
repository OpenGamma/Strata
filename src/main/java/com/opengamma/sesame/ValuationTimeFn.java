/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import javax.inject.Provider;

import org.threeten.bp.ZonedDateTime;

public interface ValuationTimeFn extends Provider<ZonedDateTime> {
}
