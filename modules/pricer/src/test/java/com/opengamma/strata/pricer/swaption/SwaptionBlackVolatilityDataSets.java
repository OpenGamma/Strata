/*
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
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.IborRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.ImmutableFixedIborSwapConvention;

/**
 * Black volatility data sets for testing.
 */
public class SwaptionBlackVolatilityDataSets {

  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);

  //     =====     Standard figures for testing     =====
  private static final DoubleArray TIMES =
      DoubleArray.of(0.5, 0.5, 0.5, 0.5, 0.5, 1, 1, 1, 1, 1, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10);
  private static final DoubleArray TENOR =
      DoubleArray.of(1, 2, 5, 10, 30, 1, 2, 5, 10, 30, 1, 2, 5, 10, 30, 1, 2, 5, 10, 30);
  private static final DoubleArray BLACK_VOL =
      DoubleArray.of(
          0.45, 0.425, 0.4, 0.375, 0.35,
          0.425, 0.4, 0.375, 0.35, 0.325,
          0.4, 0.375, 0.35, 0.325, 0.3,
          0.375, 0.35, 0.325, 0.3, 0.275);

  private static final BusinessDayAdjustment MOD_FOL_US = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, USNY);
  private static final FixedRateSwapLegConvention USD_FIXED_1Y_30U360 =
      FixedRateSwapLegConvention.of(USD, THIRTY_U_360, Frequency.P6M, MOD_FOL_US);
  private static final IborRateSwapLegConvention USD_IBOR_LIBOR3M =
      IborRateSwapLegConvention.of(USD_LIBOR_3M);
  public static final FixedIborSwapConvention USD_1Y_LIBOR3M =
      ImmutableFixedIborSwapConvention.of("USD-Swap", USD_FIXED_1Y_30U360, USD_IBOR_LIBOR3M);
  private static final SurfaceMetadata METADATA_STD =
      Surfaces.blackVolatilityByExpiryTenor("Black Vol", ACT_365F);
  private static final Surface SURFACE_STD =
      InterpolatedNodalSurface.of(METADATA_STD, TIMES, TENOR, BLACK_VOL, INTERPOLATOR_2D);

  private static final LocalDate VAL_DATE_STD = LocalDate.of(2015, 8, 7);
  private static final LocalTime VAL_TIME_STD = LocalTime.of(13, 45);
  private static final ZoneId VAL_ZONE_STD = ZoneId.of("Europe/London");
  /** Black volatility provider */
  public static final BlackSwaptionExpiryTenorVolatilities BLACK_SWAPTION_VOLS_USD_STD =
      BlackSwaptionExpiryTenorVolatilities.of(
          USD_1Y_LIBOR3M, VAL_DATE_STD.atTime(VAL_TIME_STD).atZone(VAL_ZONE_STD), SURFACE_STD);

  /** constant volatility */
  public static final double VOLATILITY = 0.20;
  /** metadata for constant surface */
  public static final SurfaceMetadata META_DATA =
      Surfaces.blackVolatilityByExpiryTenor("Constant Surface", ACT_365F);
  private static final Surface CST_SURFACE = ConstantSurface.of(META_DATA, VOLATILITY);
  /** flat Black volatility provider */
  public static final BlackSwaptionExpiryTenorVolatilities BLACK_SWAPTION_VOLS_CST_USD =
      BlackSwaptionExpiryTenorVolatilities.of(
          USD_FIXED_6M_LIBOR_3M, VAL_DATE_STD.atTime(VAL_TIME_STD).atZone(VAL_ZONE_STD), CST_SURFACE);

}
