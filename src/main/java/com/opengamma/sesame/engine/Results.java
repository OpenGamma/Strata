/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Map;

import com.google.common.collect.Maps;
import com.opengamma.id.ObjectId;

/**
 * TODO use linked maps so the column and rows orders reflect the view def
 */
public class Results {

  private final Map<ObjectId, Map<String, Object>> _results;

  /* package */ Results(Map<ObjectId, Map<String, Object>> results) {
    _results = results;
  }

  @Override
  public String toString() {
    return _results.toString();
  }

  // TODO this is most definitely *not* thread safe yet
  /* package */ static class Builder {

    private final Map<ObjectId, Map<String, Object>> _results = Maps.newHashMap();

    /* package */ void add(String columnName, ObjectId targetId, Object result) {
      Map<String, Object> targetResults;
      if (_results.containsKey(targetId)) {
        targetResults = _results.get(targetId);
      } else {
        targetResults = Maps.newHashMap();
      }
      targetResults.put(columnName, result);
    }

    /* package */ Results build() {
      return new Results(_results);
    }
  }
}
