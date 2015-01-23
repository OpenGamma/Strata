/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */

/**
 * Basic financial tools for working with date-based schedules.
 * <p>
 * The {@link com.opengamma.basics.schedule.PeriodicSchedule PeriodicSchedule} class is
 * used to define a schedule in high level terms based on a
 * {@linkplain com.opengamma.basics.schedule.Frequency frequency},
 * {@linkplain com.opengamma.basics.schedule.StubConvention stub convention},
 * {@linkplain com.opengamma.basics.schedule.RollConvention roll convention}.
 * This is then used to generate a {@link com.opengamma.basics.schedule.Schedule Schedule}
 * which is formed from one or more {@linkplain com.opengamma.basics.schedule.SchedulePeriod periods}.
 */
@javax.annotation.ParametersAreNonnullByDefault
package com.opengamma.basics.schedule;

