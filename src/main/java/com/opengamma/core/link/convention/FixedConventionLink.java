/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.core.link.convention;

import com.opengamma.core.convention.Convention;
import com.opengamma.service.ServiceContext;

/**
 * This link is designed to hold an actual instance of the underlying object.  This is useful for unit testing or scripting.
 * This should not be used in production engine code because it won't track version/correction changes.
 * @param <T> the type of the underlying convention
 */
class FixedConventionLink<T extends Convention> extends ConventionLink<T> {
  private T _convention;
    
  protected FixedConventionLink(T convention) {
    super(convention.getExternalIdBundle());
    _convention = convention;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ConventionLink<T> with(ServiceContext resolver) {
    return new CustomResolverConventionLink<T>(_convention.getExternalIdBundle(), resolver);
  }

  @Override
  public T getConvention() {
    return _convention;
  }

}
