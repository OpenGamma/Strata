/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioArray;

/**
 * Calculation results of performing calculations for a set of targets and columns.
 * <p>
 * This defines a grid of results where the grid contains a row for each target and a column for each measure.
 * Each result may be a single value or a multi-scenario value.
 * A multi-scenario value will implement {@link ScenarioArray} unless it has been aggregated.
 */
@BeanDefinition(builderScope = "private")
public final class Results implements ImmutableBean {

  /**
   * The column headers.
   * <p>
   * Each column in the results is defined by a header consisting of the name and measure.
   * The size of this list defines the number of columns, which is needed to interpret the list of cells.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<ColumnHeader> columns;
  /**
   * The grid of results, stored as a flat list.
   * <p>
   * This list contains the calculated result for each cell in the grid.
   * The cells are grouped by target, then column.
   * Thus, the index of a given cell is {@code (targetRowIndex * columnCount) + columnIndex}.
   * <p>
   * For example, given a set of results with two targets, t1 and t2,
   * and three columns c1, c2, and c3, the results will be:
   * <pre>
   *   [t1c1, t1c2, t1c3, t2c1, t2c2, t2c3]
   * </pre>
   */
  @PropertyDefinition(validate = "notNull", builderType = "List<? extends Result<?>>")
  private final ImmutableList<Result<?>> cells;
  /**
   * The number of rows.
   */
  private final transient int rowCount;  // derived, not a property
  /**
   * The number of columns.
   */
  private final transient int columnCount;  // derived, not a property

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance containing the results of the calculation for each cell.
   * <p>
   * The number of cells must be exactly divisible by the number of columns.
   *
   * @param columns  the names of each column
   * @param cells  the calculated results, one for each cell
   * @return a set of results for the calculations
   */
  public static Results of(List<ColumnHeader> columns, List<? extends Result<?>> cells) {
    return new Results(columns, cells);
  }

  @ImmutableConstructor
  private Results(List<ColumnHeader> columns, List<? extends Result<?>> cells) {
    JodaBeanUtils.notNull(columns, "columns");
    JodaBeanUtils.notNull(cells, "cells");
    this.columns = ImmutableList.copyOf(columns);
    this.cells = ImmutableList.copyOf(cells);
    this.columnCount = columns.size();
    this.rowCount = (columnCount == 0 ? 0 : cells.size() / columnCount);

    if (rowCount * columnCount != cells.size()) {
      throw new IllegalArgumentException(
          Messages.format(
              "The number of cells ({}) must equal the number of rows ({}) multiplied by the number of columns ({})",
              this.cells.size(),
              this.rowCount,
              this.columnCount));
    }
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new Results(columns, cells);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of rows in the results.
   * <p>
   * The number of rows equals the number of targets input to the calculation.
   *
   * @return the number of rows
   */
  public int getRowCount() {
    return rowCount;
  }

  /**
   * Gets the number of columns in the results.
   *
   * @return the number of columns
   */
  public int getColumnCount() {
    return columnCount;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the results for a target and column index.
   * <p>
   * The result may be a single value or a multi-scenario value.
   * A multi-scenario value will implement {@link ScenarioArray} unless it has been aggregated.
   * <p>
   * If the calculation did not complete successfully, a failure result will be returned
   * explaining the problem. Callers must check whether the result is a success or failure
   * before examining the result value.
   *
   * @param rowIndex   the index of the row containing the results for a target
   * @param columnIndex  the index of the column
   * @return the result for the specified row and column for a set of scenarios
   * @throws IllegalArgumentException if the row or column index is invalid
   */
  public Result<?> get(int rowIndex, int columnIndex) {
    if (rowIndex < 0 || rowIndex >= rowCount) {
      throw new IllegalArgumentException(invalidRowIndexMessage(rowIndex));
    }
    if (columnIndex < 0 || columnIndex >= columnCount) {
      throw new IllegalArgumentException(invalidColumnIndexMessage(columnIndex));
    }
    int index = (rowIndex * columnCount) + columnIndex;
    return cells.get(index);
  }

  private String invalidRowIndexMessage(int rowIndex) {
    return Messages.format(
        "Row index must be greater than or equal to zero and less than the row count ({}), but it was {}",
        rowIndex);
  }

  private String invalidColumnIndexMessage(int columnIndex) {
    return Messages.format(
        "Column index must be greater than or equal to zero and less than the column count ({}), but it was {}",
        columnIndex);
  }

  /**
   * Returns the results for a target and column index, casting the result to a known type.
   * <p>
   * The result may be a single value or a multi-scenario value.
   * A multi-scenario value will implement {@link ScenarioArray} unless it has been aggregated.
   * <p>
   * If the calculation did not complete successfully, a failure result will be returned
   * explaining the problem. Callers must check whether the result is a success or failure
   * before examining the result value.
   *
   * @param <T>  the result type
   * @param rowIndex   the index of the row containing the results for a target
   * @param columnIndex  the index of the column
   * @param type  the result type
   * @return the result for the specified row and column for a set of scenarios, cast to the specified type
   * @throws IllegalArgumentException if the row or column index is invalid
   * @throws ClassCastException if the result is not of the specified type
   */
  public <T> Result<T> get(int rowIndex, int columnIndex, Class<T> type) {
    return cast(get(rowIndex, columnIndex), type);
  }

  /**
   * Returns the results for a target and column name.
   * <p>
   * The result may be a single value or a multi-scenario value.
   * A multi-scenario value will implement {@link ScenarioArray} unless it has been aggregated.
   * <p>
   * If the calculation did not complete successfully, a failure result will be returned
   * explaining the problem. Callers must check whether the result is a success or failure
   * before examining the result value.
   *
   * @param rowIndex   the index of the row containing the results for a target
   * @param columnName  the name of the column
   * @return the result for the specified row and column for a set of scenarios
   * @throws IllegalArgumentException if the row index or column name is invalid
   */
  public Result<?> get(int rowIndex, ColumnName columnName) {
    for (int i = 0; i < columns.size(); i++) {
      if (columns.get(i).getName().equals(columnName)) {
        return get(rowIndex, i);
      }
    }
    throw new IllegalArgumentException(invalidColumnNameMessage(columnName));
  }

  private String invalidColumnNameMessage(ColumnName columnName) {
    return Messages.format("Column name not found: {}", columnName);
  }

  /**
   * Returns the results for a target and column name, casting the result to a known type.
   * <p>
   * The result may be a single value or a multi-scenario value.
   * A multi-scenario value will implement {@link ScenarioArray} unless it has been aggregated.
   * <p>
   * If the calculation did not complete successfully, a failure result will be returned
   * explaining the problem. Callers must check whether the result is a success or failure
   * before examining the result value.
   *
   * @param <T>  the result type
   * @param rowIndex   the index of the row containing the results for a target
   * @param columnName  the name of the column
   * @param type  the result type
   * @return the result for the specified row and column for a set of scenarios, cast to the specified type
   * @throws IllegalArgumentException if the row index or column name is invalid
   * @throws ClassCastException if the result is not of the specified type
   */
  public <T> Result<T> get(int rowIndex, ColumnName columnName, Class<T> type) {
    return cast(get(rowIndex, columnName), type);
  }

  @SuppressWarnings("unchecked")
  private <T> Result<T> cast(Result<?> result, Class<T> type) {
    // cannot use result.map() as we want the exception to be thrown
    if (result.isFailure() || type.isInstance(result.getValue())) {
      return (Result<T>) result;
    }
    throw new ClassCastException(Messages.format(
        "Result queried with type '{}' but was '{}'", type.getName(), result.getValue().getClass().getName()));
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Results}.
   * @return the meta-bean, not null
   */
  public static Results.Meta meta() {
    return Results.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Results.Meta.INSTANCE);
  }

  @Override
  public Results.Meta metaBean() {
    return Results.Meta.INSTANCE;
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
   * Gets the column headers.
   * <p>
   * Each column in the results is defined by a header consisting of the name and measure.
   * The size of this list defines the number of columns, which is needed to interpret the list of cells.
   * @return the value of the property, not null
   */
  public ImmutableList<ColumnHeader> getColumns() {
    return columns;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the grid of results, stored as a flat list.
   * <p>
   * This list contains the calculated result for each cell in the grid.
   * The cells are grouped by target, then column.
   * Thus, the index of a given cell is {@code (targetRowIndex * columnCount) + columnIndex}.
   * <p>
   * For example, given a set of results with two targets, t1 and t2,
   * and three columns c1, c2, and c3, the results will be:
   * <pre>
   * [t1c1, t1c2, t1c3, t2c1, t2c2, t2c3]
   * </pre>
   * @return the value of the property, not null
   */
  public ImmutableList<Result<?>> getCells() {
    return cells;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Results other = (Results) obj;
      return JodaBeanUtils.equal(columns, other.columns) &&
          JodaBeanUtils.equal(cells, other.cells);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(columns);
    hash = hash * 31 + JodaBeanUtils.hashCode(cells);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("Results{");
    buf.append("columns").append('=').append(columns).append(',').append(' ');
    buf.append("cells").append('=').append(JodaBeanUtils.toString(cells));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Results}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code columns} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ColumnHeader>> columns = DirectMetaProperty.ofImmutable(
        this, "columns", Results.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code cells} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<Result<?>>> cells = DirectMetaProperty.ofImmutable(
        this, "cells", Results.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "columns",
        "cells");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 949721053:  // columns
          return columns;
        case 94544721:  // cells
          return cells;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends Results> builder() {
      return new Results.Builder();
    }

    @Override
    public Class<? extends Results> beanType() {
      return Results.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code columns} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ColumnHeader>> columns() {
      return columns;
    }

    /**
     * The meta-property for the {@code cells} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<Result<?>>> cells() {
      return cells;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 949721053:  // columns
          return ((Results) bean).getColumns();
        case 94544721:  // cells
          return ((Results) bean).getCells();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code Results}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<Results> {

    private List<ColumnHeader> columns = ImmutableList.of();
    private List<? extends Result<?>> cells = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 949721053:  // columns
          return columns;
        case 94544721:  // cells
          return cells;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 949721053:  // columns
          this.columns = (List<ColumnHeader>) newValue;
          break;
        case 94544721:  // cells
          this.cells = (List<? extends Result<?>>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public Results build() {
      return new Results(
          columns,
          cells);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("Results.Builder{");
      buf.append("columns").append('=').append(JodaBeanUtils.toString(columns)).append(',').append(' ');
      buf.append("cells").append('=').append(JodaBeanUtils.toString(cells));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
