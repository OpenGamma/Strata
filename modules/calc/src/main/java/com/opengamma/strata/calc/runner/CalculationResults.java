/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.List;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;

/**
 * A set of related calculation results for a single calculation target.
 * <p>
 * This contains a list of {@link CalculationResult}, produced by a single {@link CalculationTask}.
 * Each individual result relates to a single cell in the output grid.
 */
@BeanDefinition(style = "light")
public final class CalculationResults implements ImmutableBean {

  /**
   * The target of the calculation, often a trade.
   */
  @PropertyDefinition(validate = "notNull")
  private final CalculationTarget target;
  /**
   * The calculated cells.
   * Each entry contains a calculation result for a single cell.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<CalculationResult> cells;

  //-------------------------------------------------------------------------
  /**
   * Obtains a calculation result from individual calculations.
   * 
   * @param target  the calculation target, such as a trade
   * @param results  the results of the calculation
   * @return the calculation result
   */
  public static CalculationResults of(CalculationTarget target, List<CalculationResult> results) {
    return new CalculationResults(target, results);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CalculationResults}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(CalculationResults.class);

  /**
   * The meta-bean for {@code CalculationResults}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  private CalculationResults(
      CalculationTarget target,
      List<CalculationResult> cells) {
    JodaBeanUtils.notNull(target, "target");
    JodaBeanUtils.notNull(cells, "cells");
    this.target = target;
    this.cells = ImmutableList.copyOf(cells);
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the target of the calculation, often a trade.
   * @return the value of the property, not null
   */
  public CalculationTarget getTarget() {
    return target;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calculated cells.
   * Each entry contains a calculation result for a single cell.
   * @return the value of the property, not null
   */
  public ImmutableList<CalculationResult> getCells() {
    return cells;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CalculationResults other = (CalculationResults) obj;
      return JodaBeanUtils.equal(target, other.target) &&
          JodaBeanUtils.equal(cells, other.cells);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(target);
    hash = hash * 31 + JodaBeanUtils.hashCode(cells);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("CalculationResults{");
    buf.append("target").append('=').append(target).append(',').append(' ');
    buf.append("cells").append('=').append(JodaBeanUtils.toString(cells));
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
