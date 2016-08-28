// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.collide.client.code.autocomplete.css;

import static com.google.collide.client.code.autocomplete.css.CompletionType.NONE;

import com.google.collide.json.client.JsoArray;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;

/**
 * A completion query for a CSS autocompletion. Processes the relevant context
 * in the document.
 *
 */
public class CssCompletionQuery {

  private static final RegExp REGEXP_SPACES = RegExp.compile("\\s+");
  private static final RegExp REGEXP_COLON = RegExp.compile(":");
  private static final RegExp REGEXP_SEMICOLON = RegExp.compile(";");

  /*
   * Depending on the query, we will either have an incomplete property or an
   * incomplete value. If there is an incomplete value, the property (name) is
   * assumed completed (or incorrect).
   */
  private String property = ""; // Current property

  /*
   * The following two fields are used for filtering existing
   * properties/attributes from the list of proposals.
   */
  private final JsoArray<String> completedProperties = JsoArray.create();
  private final JsArrayString valuesAfter = JsArrayString.createArray().cast();
  private final JsArrayString valuesBefore = JsArrayString.createArray().cast();

  private String value = "";
  private CompletionType completionType;

  /**
   * Constructs a completion query based on an incomplete string, which is
   * everything from the caret back to the beginning of the open CSS declaration
   * block.
   *
   *
   * @param textBefore the string to be completed
   */
  public CssCompletionQuery(String textBefore, String textAfter) {
    completionType = NONE;
    parseContext(textBefore, textAfter);
  }

  public String getValue() {
    return value;
  }

  public JsArrayString getValuesAfter() {
    return valuesAfter;
  }

  public JsArrayString getValuesBefore() {
    return valuesBefore;
  }

  public JsoArray<String> getCompletedProperties() {
    return completedProperties;
  }

  public CompletionType getCompletionType() {
    return completionType;
  }

  public String getProperty() {
    return property;
  }

  private void parseCurrentPropertyAndValues(String incompletePropertyAndValues) {
    incompletePropertyAndValues =
        incompletePropertyAndValues.substring(incompletePropertyAndValues.indexOf('{') + 1);
    SplitResult subParts = REGEXP_COLON.split(incompletePropertyAndValues);
    // subParts must have at least one element
    property = subParts.get(0).trim();
    if (subParts.length() > 1) {
      SplitResult valueParts = REGEXP_SPACES.split(subParts.get(1));

      if (subParts.get(1).endsWith(" ")) {
        for (int i = 0; i < valueParts.length(); i++) {
          String trimmed = valueParts.get(i).trim();
          if (!trimmed.isEmpty()) {
            valuesBefore.push(trimmed);
          }
        }
      } else {
        if (valueParts.length() == 1) {
          value = subParts.get(1).trim();
        } else {
          value = valueParts.get(valueParts.length() - 1).trim();
          for (int i = 0; i < valueParts.length() - 1; i++) {
            String trimmed = valueParts.get(i).trim();
            if (!trimmed.isEmpty()) {
              valuesBefore.push(trimmed);
            }
          }
        }
      }
    } else if (incompletePropertyAndValues.endsWith(":")) {
      value = "";
    }
  }

  // TODO: Do something useful with textAfter
  private void parseContext(String textBefore, String textAfter) {
    if (textBefore.isEmpty()) {
      completionType = CompletionType.PROPERTY;
      return;
    } else if (textBefore.endsWith("{")) {
      completionType = CompletionType.CLASS;
      return;
    }

    textBefore = textBefore.replaceAll("^\\s+", "");

    // Split first on ';'. The last one is the incomplete one.
    SplitResult parts = REGEXP_SEMICOLON.split(textBefore);

    if ((textBefore.endsWith(";")) || (!parts.get(parts.length() - 1).contains(":"))) {
      completionType = CompletionType.PROPERTY;
    } else {
      completionType = CompletionType.VALUE;
    }

    int highestCompleteIndex = parts.length() - 2;
    if (textBefore.endsWith(";")) {
      highestCompleteIndex = parts.length() - 1;
    } else {
      parseCurrentPropertyAndValues(parts.get(parts.length() - 1));
    }
    if (parts.length() > 1) {
      // Parse the completed properties, which we use for filtering.
      for (int i = 0; i <= highestCompleteIndex; i++) {
        String completePropertyAndValues = parts.get(i);
        SplitResult subParts = REGEXP_COLON.split(completePropertyAndValues);
        completedProperties.add(subParts.get(0).trim().toLowerCase());
      }
    }

    // Interpret textAfter
    // Everything up to the first ; will be interpreted as being part of the
    // current property, so it'll be complete values
    parts = REGEXP_SEMICOLON.split(textAfter);
    if (parts.length() > 0) {

      // We assume that the property+values we are currently working on is not
      // completed but can be assumed to end with a newline.
      int newlineIndex = parts.get(0).indexOf('\n');
      if (newlineIndex != -1) {
        String currentValues = parts.get(0).substring(0, newlineIndex);
        addToValuesAfter(currentValues);
        addToCompletedProperties(parts.get(0).substring(newlineIndex + 1));
      } else {
        addToValuesAfter(parts.get(0));
      }
      for (int i = 1; i < parts.length(); i++) {
        addToCompletedProperties(parts.get(i));
      }
    }
  }

  private void addToCompletedProperties(String completedProps) {
    SplitResult completed = REGEXP_SEMICOLON.split(completedProps);
    for (int i = 0; i < completed.length(); i++) {
      int colonIndex = completed.get(i).indexOf(":");
      String trimmed;
      if (colonIndex != -1) {
        trimmed = completed.get(i).substring(0, colonIndex).trim();
      } else {
        trimmed = completed.get(i).trim();
      }
      if (!trimmed.isEmpty()) {
        completedProperties.add(trimmed.toLowerCase());
      }
    }
  }

  private void addToValuesAfter(String completedVals) {
    SplitResult completed = REGEXP_SPACES.split(completedVals);
    for (int i = 0; i < completed.length(); i++) {
      String trimmed = completed.get(i).trim();
      if (!trimmed.isEmpty()) {
        valuesAfter.push(trimmed);
      }
    }
  }

  @VisibleForTesting
  public String getTriggeringString() {
    switch (completionType) {
      case CLASS:
        return "";
      case PROPERTY:
        return getProperty();
      case VALUE:
        return getValue();
      default:
        return null;
    }
  }

  public void setCompletionType(CompletionType type) {
    this.completionType = type;
  }
}
