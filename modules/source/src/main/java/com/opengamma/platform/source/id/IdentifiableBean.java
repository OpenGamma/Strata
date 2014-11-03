/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import org.joda.beans.Bean;

/**
 * Marker interface which allows identifies objects that can be retrieved
 * from a source.
 */
public interface IdentifiableBean extends StandardIdentifiable, Bean {
}
