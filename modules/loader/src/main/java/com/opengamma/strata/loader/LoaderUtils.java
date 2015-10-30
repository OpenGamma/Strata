/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader;

import org.joda.beans.JodaBeanUtils;

import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.math.impl.interpolation.DoubleQuadraticInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.ExponentialExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.ExponentialInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.FlatExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.LinearExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.LogLinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.NaturalCubicSplineInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.QuadraticPolynomialLeftExtrapolator;
import com.opengamma.strata.math.impl.interpolation.TimeSquareInterpolator1D;

/**
 * Contains utilities for loading market data from input files.
 */
public final class LoaderUtils {
  
  static {
    JodaBeanUtils.registerMetaBean(DoubleQuadraticInterpolator1D.meta());
    JodaBeanUtils.registerMetaBean(ExponentialInterpolator1D.meta());
    JodaBeanUtils.registerMetaBean(LinearInterpolator1D.meta());
    JodaBeanUtils.registerMetaBean(LogLinearInterpolator1D.meta());
    JodaBeanUtils.registerMetaBean(NaturalCubicSplineInterpolator1D.meta());
    JodaBeanUtils.registerMetaBean(TimeSquareInterpolator1D.meta());

    JodaBeanUtils.registerMetaBean(ExponentialExtrapolator1D.meta());
    JodaBeanUtils.registerMetaBean(FlatExtrapolator1D.meta());
    JodaBeanUtils.registerMetaBean(LinearExtrapolator1D.meta());
    JodaBeanUtils.registerMetaBean(QuadraticPolynomialLeftExtrapolator.meta());
  }

  //-------------------------------------------------------------------------
  /**
   * Attempts to locate a rate index by reference name.
   * <p>
   * This utility searches {@link IborIndex}, {@link OvernightIndex}, {@link FxIndex}
   * and {@link PriceIndex}.
   * 
   * @param reference  the reference name
   * @return the resolved rate index
   */
  public static Index findIndex(String reference) {
    if (IborIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return IborIndex.of(reference);

    } else if (OvernightIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return OvernightIndex.of(reference);

    } else if (PriceIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return PriceIndex.of(reference);

    } else if (FxIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return FxIndex.of(reference);

    } else {
      throw new IllegalArgumentException(Messages.format("No index found for reference: {}", reference));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private LoaderUtils() {
  }

}
