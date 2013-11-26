/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.opengamma.sesame.trace.CallGraph;
import com.opengamma.util.ArgumentChecker;

// TODO is it worth including a lookup by ID instead of row index?
// TODO is it worth including a lookup by column name as well as column index?
// TODO Iterable<Row>?
// TODO column types
public final class Results {

  private final List<String> _columnNames;
  private final List<Row> _rows;

  private Results(List<Row> rows, List<String> columnNames) {
    _rows = rows;
    _columnNames = ImmutableList.copyOf(ArgumentChecker.notNull(columnNames, "columnNames"));
  }

  public Row get(int rowIndex) {
    checkRowIndex(rowIndex);
    return _rows.get(rowIndex);
  }

  private void checkRowIndex(int rowIndex) {
    if (rowIndex < 0 || rowIndex >= _rows.size()) {
      throw new IndexOutOfBoundsException("Index " + rowIndex + " is out of bounds. row count = " + _rows.size());
    }
  }

  public Item get(int rowIndex, int columnIndex) {
    checkRowIndex(rowIndex);
    if (columnIndex < 0 || columnIndex >= _columnNames.size()) {
      throw new IndexOutOfBoundsException("Index " + columnIndex + " is out of bounds. column count = " + _columnNames.size());
    }
    return _rows.get(rowIndex).get(columnIndex);
  }

  public List<String> getColumnNames() {
    return _columnNames;
  }

  @Override
  public String toString() {
    return "Results [_columnNames=" + _columnNames + ", _rows=" + _rows + "]";
  }

  /* package */ static Builder builder(List<String> columnNames) {
    return new Builder(columnNames);
  }

  //Item get(int rowIndex, String columnName);

  public static final class Row {

    private final List<Item> _items;

    private Row(List<Item> items) {
      _items = items;
    }

    public Item get(int columnIndex) {
      if (columnIndex < 0 || columnIndex >= _items.size()) {
        throw new IndexOutOfBoundsException("Index " + columnIndex + " is out of bounds. column count = " + _items.size());
      }
      return _items.get(columnIndex);
    }

    //Item get(String columnName);

    @Override
    public String toString() {
      return "Row [_items=" + _items + "]";
    }
  }

  public final static class Item {

    private final Object _output;
    private final Object _input;
    private final CallGraph _callGraph;

    public Item(Object input, Object output, CallGraph callGraph) {
      _input = ArgumentChecker.notNull(input, "input");
      _output = output;
      _callGraph = callGraph;
    }

    public Object getOutput() {
      return _output;
    }

    public CallGraph getCallGraph() {
      return _callGraph;
    }

    public Object getInput() {
      return _input;
    }

    @Override
    public String toString() {
      return "Item [_result=" + _output + ", _input=" + _input + ", _callGraph=" + _callGraph + "]";
    }
  }

  /* package */ static final class Builder {

    private final Table<Integer, Integer, Item> _table = TreeBasedTable.create();
    private final List<String> _columnNames;

    public Builder(List<String> columnNames) {
      _columnNames = columnNames;
    }

    /* package */ void add(int rowIndex, int columnIndex, Object input, Object result, CallGraph callGraph) {
      _table.put(rowIndex, columnIndex, new Item(input, result, callGraph));
    }

    /* package */ Results build() {
      Map<Integer, Map<Integer, Item>> rowMap = _table.rowMap();
      List<Row> rows = Lists.newArrayListWithCapacity(rowMap.size());
      for (Map<Integer, Item> row : rowMap.values()) {
        rows.add(new Row(Lists.newArrayList(row.values())));
      }
      return new Results(rows, _columnNames);
    }
  }
}
