/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */

package com.opengamma.strata.calc.marketdata;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ComparisonChain;

/**
 * Identifies a single scenario in the margin calculations.
 * <p>
 * Each scenario is associated with a date and can be a historical or stress scenario.
 */

/*@BeanDefinition(builderScope = "private", style = "minimal")
public final class ScenarioId implements Comparable<ScenarioId>, ImmutableBean {

  *//** The index of the scenario. *//*
  @PropertyDefinition(validate = "notNull")
  private final int index;

  *//**
   * Returns a scenario ID for the specified type and date.
   *
   * @param date the date of the scenario
   * @return a scenario ID for the specified type and date
   *//*
  public static ScenarioId of(LocalDate date) {
    return new ScenarioId(index);
  }

  @Override
  public int compareTo(ScenarioId other) {
    return ComparisonChain.start().compare(index, other.index).result();
  }
}*/


