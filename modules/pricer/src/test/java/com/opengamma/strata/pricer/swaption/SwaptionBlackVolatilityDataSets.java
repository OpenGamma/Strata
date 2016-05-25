/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.IborRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.ImmutableFixedIborSwapConvention;

/**
 * Black volatility data sets for testing.
 */
public class SwaptionBlackVolatilityDataSets {

  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR.getName(), CurveExtrapolators.FLAT.getName(), CurveExtrapolators.FLAT.getName());
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);

  //     =====     Standard figures for testing     =====
  private static final DoubleArray TIMES =
      DoubleArray.of(0.50, 1.00, 5.00, 10.0, 0.50, 1.00, 5.00, 10.0,
          0.50, 1.00, 5.00, 10.0, 0.50, 1.00, 5.00, 10.0, 0.50, 1.00, 5.00, 10.0);
  private static final DoubleArray TENOR =
      DoubleArray.of(1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 2.0,
          5.0, 5.0, 5.0, 5.0, 10.0, 10.0, 10.0, 10.0, 30.0, 30.0, 30.0, 30.0);
  private static final DoubleArray BLACK_VOL =
      DoubleArray.of(0.45, 0.425, 0.4, 0.375, 0.425, 0.4, 0.375, 0.35, 0.4, 0.375, 0.35, 0.325, 0.375, 0.35, 0.325, 0.3,
          0.35, 0.325, 0.3, 0.275);

  private static final BusinessDayAdjustment MOD_FOL_US = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY);
  private static final FixedRateSwapLegConvention USD_FIXED_1Y_30U360 =
      FixedRateSwapLegConvention.of(USD, THIRTY_U_360, Frequency.P6M, MOD_FOL_US);
  private static final IborRateSwapLegConvention USD_IBOR_LIBOR3M =
      IborRateSwapLegConvention.of(USD_LIBOR_3M);
  public static final FixedIborSwapConvention USD_1Y_LIBOR3M =
      ImmutableFixedIborSwapConvention.of("USD-Swap", USD_FIXED_1Y_30U360, USD_IBOR_LIBOR3M);
  private static final SurfaceMetadata METADATA_STD =
      Surfaces.swaptionBlackExpiryTenor("Black Vol", ACT_365F, USD_1Y_LIBOR3M);
  private static final Surface SURFACE_STD =
      InterpolatedNodalSurface.of(METADATA_STD, TIMES, TENOR, BLACK_VOL, INTERPOLATOR_2D);

  private static final LocalDate VAL_DATE_STD = LocalDate.of(2015, 8, 7);
  private static final LocalTime VAL_TIME_STD = LocalTime.of(13, 45);
  private static final ZoneId VAL_ZONE_STD = ZoneId.of("Europe/London");
  /** Black volatility provider */
  public static final BlackSwaptionExpiryTenorVolatilities BLACK_VOL_SWAPTION_PROVIDER_USD_STD =
      BlackSwaptionExpiryTenorVolatilities.of(SURFACE_STD, VAL_DATE_STD, VAL_TIME_STD, VAL_ZONE_STD);

  /** constant volatility */
  public static final double VOLATILITY = 0.20;
  /** metadata for constant surface */
  public static final SurfaceMetadata META_DATA =
      Surfaces.swaptionBlackExpiryTenor("Constant Surface", ACT_365F, USD_FIXED_6M_LIBOR_3M);
  private static final Surface CST_SURFACE = ConstantSurface.of(META_DATA, VOLATILITY);
  /** flat Black volatility provider */
  public static final BlackSwaptionExpiryTenorVolatilities BLACK_VOL_CST_SWAPTION_PROVIDER_USD =
      BlackSwaptionExpiryTenorVolatilities.of(CST_SURFACE, VAL_DATE_STD, VAL_TIME_STD, VAL_ZONE_STD);

}
