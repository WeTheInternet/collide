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

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.json.client.Jso;
import com.google.collide.json.client.JsoArray;
import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.regexp.shared.RegExp;

import elemental.js.util.JsMapFromStringToBoolean;

/**
 * Based on the complete property list for CSS2, this partial parser parses
 * already existing values and proposes autocompletions for the slot where the
 * cursor currently is.
 *
 */
public class CssPartialParser {

  /**
   * Singleton instance.
   */
  private static CssPartialParser instance;

  // TODO: What about "-.5"?
  private static final String NUMBER_PATTERN = "(-|\\+)?\\d+(\\.\\d+)?";
  private static final String PERCENTAGE_PATTERN = "(\\d|[1-9]\\d|100)%";
  private static final String BYTE_PATTERN = "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])";
  private static final String LENGTH_PATTERN = NUMBER_PATTERN + "(em|ex|in|cm|mm|pt|pc|px)";

  private static final RegExp REGEXP_INTEGER = compileRegExp("(-|\\+)?\\d+");
  private static final RegExp REGEXP_PERCENTAGE = compileRegExp(PERCENTAGE_PATTERN);
  private static final RegExp REGEXP_3_HEX = compileRegExp("#[0-9a-fA-F]{3}");
  private static final RegExp REGEXP_6_HEX = compileRegExp("#[0-9a-fA-F]{6}");
  private static final RegExp REGEXP_RGB_BYTES = compileRegExp(
      "rgb\\(" + BYTE_PATTERN + "\\,\\s*" + BYTE_PATTERN + "\\,\\s*" + BYTE_PATTERN + "\\)");
  private static final RegExp REGEXP_RGB_PERCENTS = compileRegExp("rgb\\(" + PERCENTAGE_PATTERN
      + "\\,\\s*" + PERCENTAGE_PATTERN + "\\,\\s*" + PERCENTAGE_PATTERN + "\\)");
  private static final RegExp REGEXP_ANGLE = compileRegExp(NUMBER_PATTERN + "(deg|grad|rad)");
  private static final RegExp REGEXP_NUMBER = compileRegExp(NUMBER_PATTERN);
  private static final RegExp REGEXP_FREQUENCY = compileRegExp(NUMBER_PATTERN + "k?Hz");
  private static final RegExp REGEXP_LENGTH = compileRegExp(LENGTH_PATTERN);
  private static final RegExp REGEXP_RECT = compileRegExp("rect\\(\\s*("
      + LENGTH_PATTERN + "|auto)\\s*\\,\\s*(" + LENGTH_PATTERN + "|auto)\\s*\\,\\s*("
      + LENGTH_PATTERN + "|auto)\\s*\\,\\s*(" + LENGTH_PATTERN + "|auto)\\s*\\)");

  private static final String ANGLE = "<angle>";
  private static final String NUMBER = "<number>";
  private static final String INTEGER = "<integer>";
  private static final String URI = "<uri>";
  private static final String PERCENTAGE = "<percentage>";
  private static final String STRING = "<string>";
  private static final String COUNTER = "<counter>";
  private static final String IDENTIFIER = "<identifier>";
  private static final String FREQUENCY = "<frequency>";
  private static final String COLOR = "<color>";
  private static final String LENGTH = "<length>";
  private static final String SHAPE = "<shape>";

  private final JavaScriptObject allProperties;
  private final JavaScriptObject specialValueProposals;
  private final JsMapFromStringToBoolean repeatingProperties = JsMapFromStringToBoolean.create();

  public static CssPartialParser getInstance() {
    if (instance == null) {
      instance = new CssPartialParser();
    }
    return instance;
  }

  /**
   * Creates {@link RegExp} instance by given pattern that matches the
   * whole string.
   */
  private static RegExp compileRegExp(String pattern) {
    return RegExp.compile("^" + pattern + "$");
  }

  private CssPartialParser() {
    allProperties = setupAllProperties();
    specialValueProposals = setupSpecialValueProposals();
    setupRepeatingProperties(repeatingProperties);
  }

  private boolean checkIfAngle(String maybeSpecialValue) {
    return REGEXP_ANGLE.test(maybeSpecialValue);
  }

  private boolean checkIfNumber(String maybeSpecialValue) {
    return REGEXP_NUMBER.test(maybeSpecialValue);
  }

  private boolean checkIfInteger(String maybeSpecialValue) {
    return REGEXP_INTEGER.test(maybeSpecialValue);
  }

  private boolean checkIfUri(String maybeSpecialValue) {
    // TODO: This is an oversimplification.
    return (maybeSpecialValue.startsWith("http://") || maybeSpecialValue.startsWith("https://"));
  }

  private boolean checkIfPercentage(String maybeSpecialValue) {
    return REGEXP_PERCENTAGE.test(maybeSpecialValue);
  }

  private boolean checkIfString(String maybeSpecialValue) {
    return (((maybeSpecialValue.charAt(0) == '"')
        && (maybeSpecialValue.charAt(maybeSpecialValue.length() - 1) == '"'))
        || ((maybeSpecialValue.charAt(0) == '\'')
            && (maybeSpecialValue.charAt(maybeSpecialValue.length() - 1) == '\'')));
  }

  private boolean checkIfCounter(String maybeSpecialValue) {
    // TODO: This is a simplification.
    return maybeSpecialValue.startsWith("counter(");
  }

  private boolean checkIfIdentifier(String maybeSpecialValue) {
    // TODO: Implement this.
    return false;
  }

  private boolean checkIfFrequency(String maybeSpecialValue) {
    return REGEXP_FREQUENCY.test(maybeSpecialValue);
  }

  private boolean checkIfColor(String maybeSpecialValue) {
    return maybeSpecialValue.equals("aqua") || maybeSpecialValue.equals("black")
        || maybeSpecialValue.equals("blue") || maybeSpecialValue.equals("fuchsia")
        || maybeSpecialValue.equals("gray") || maybeSpecialValue.equals("green")
        || maybeSpecialValue.equals("lime") || maybeSpecialValue.equals("maroon")
        || maybeSpecialValue.equals("navy") || maybeSpecialValue.equals("olive")
        || maybeSpecialValue.equals("orange") || maybeSpecialValue.equals("purple")
        || maybeSpecialValue.equals("red") || maybeSpecialValue.equals("silver")
        || maybeSpecialValue.equals("teal") || maybeSpecialValue.equals("white")
        || maybeSpecialValue.equals("yellow")
        || REGEXP_3_HEX.test(maybeSpecialValue)
        || REGEXP_6_HEX.test(maybeSpecialValue)
        || REGEXP_RGB_BYTES.test(maybeSpecialValue)
        || REGEXP_RGB_PERCENTS.test(maybeSpecialValue);
  }

  private boolean checkIfLength(String maybeSpecialValue) {
    return REGEXP_LENGTH.test(maybeSpecialValue);
  }

  private boolean checkIfShape(String maybeSpecialValue) {
    // Note: the syntax for shape is rect(<top>, <right>, <bottom>, <left>)
    return REGEXP_RECT.test(maybeSpecialValue);
  }

  /**
   * Checks whether the passed-in string matches the format of the special value
   * type in question. If it does, it returns a list of proposals that should be
   * shown to the user for this query.
   *
   * @param maybeSpecialValue
   * @param specialValueType a special value type, e.g., <integer>. This method
   *        can be called with a specialValueType value that is not one of those
   *        special values, in which case the method returns an empty array.
   * @return an array of strings corresponding to the proposals that should be
   *         shown for the special value type
   */
  @VisibleForTesting
  public JsArrayString checkIfSpecialValueAndGetSpecialValueProposals(
      String maybeSpecialValue, String specialValueType) {

    JsArrayString specialValues = getSpecialValues(maybeSpecialValue);
    for (int i = 0; i < specialValues.length(); i++) {
      if (specialValues.get(i).equals(specialValueType)) {
        return specialValueProposals.<Jso>cast().getJsObjectField(specialValueType).cast();
      }
    }
    return JavaScriptObject.createArray().cast();
  }

  public JsoArray<AutocompleteProposal> getAutocompletions(
      String property, JsArrayString valuesBefore, String incomplete, JsArrayString valuesAfter) {
    incomplete = incomplete.toLowerCase();
    boolean isRepeatingProperty = repeatingProperties.hasKey(property);
    JsArrayString valuesAndSpecialValuesAfter = getValuesAndSpecialValues(valuesAfter);
    JsArrayString valuesAndSpecialValuesBefore = getValuesAndSpecialValues(valuesBefore);

    JsoArray<AutocompleteProposal> proposals = JsoArray.create();
    JsArray<JavaScriptObject> valuesForAllSlots = getPropertyValues(property);
    if (valuesForAllSlots == null) {
      return proposals;
    }
    int numSlots = valuesForAllSlots.length();

    if (numSlots == 0) {
      return proposals;
    }
    int slot = valuesBefore.length(); // slots use 0-based counting

    if (slot >= numSlots) {
      // before giving up, see if the last entry is +, in which case we just
      // adjust the slot number
      JavaScriptObject lastSlotValues = valuesForAllSlots.get(numSlots - 1);
      JsArrayString keySet = getKeySet(lastSlotValues);
      if ((keySet.length() == 1) && (keySet.get(0).equals("+"))) {
        slot = numSlots - 1;
      } else {
        return proposals;
      }
    }
    JavaScriptObject valuesForSlotInQuestion = valuesForAllSlots.get(slot);

    JsArrayString keySet = getKeySet(valuesForSlotInQuestion);
    if ((keySet.length() == 1) && (keySet.get(0).equals("+"))) {
      valuesForSlotInQuestion = valuesForAllSlots.get(slot - 1);
      keySet = getKeySet(valuesForSlotInQuestion);
    }

    if (valuesForSlotInQuestion != null) {
      if (keySet.length() == 0) {
        return proposals;
      }

      for (int keyCt = 0; keyCt < keySet.length(); keyCt++) {
        String currentValue = keySet.get(keyCt);
        Boolean shouldBeIncluded = false;
        // TODO: Avoid using untyped native collections.
        JavaScriptObject triggers =
            valuesForSlotInQuestion.<Jso>cast().getJsObjectField(currentValue).cast();
        JsArrayString keyTriggerSet = getKeySet(triggers);
        if (keyTriggerSet.length() == 0) {
          if (currentValue.charAt(0) == '<') {
            JsArrayString valueProposals =
                specialValueProposals.<Jso>cast().getJsObjectField(currentValue).cast();
            if (valueProposals != null && valueProposals.length() != 0) {
              shouldBeIncluded = false;
              for (int i = 0; i < valueProposals.length(); i++) {
                if (valueProposals.get(i).startsWith(incomplete)
                    && (isRepeatingProperty || !inExistingValues(
                        currentValue, valuesAndSpecialValuesBefore, valuesAndSpecialValuesAfter))) {
                  proposals.add(new AutocompleteProposal(valueProposals.get(i)));
                }
              }
            }
          } else {
            shouldBeIncluded = true;
          }
        } else {
          for (int keyTriggerCt = 0; keyTriggerCt < keyTriggerSet.length(); keyTriggerCt++) {
            String triggerValue = keyTriggerSet.get(keyTriggerCt);
            int triggerSlot = triggers.<Jso>cast().getIntField(triggerValue);
            if (triggerValue.charAt(0) == '<') {
              JsArrayString specialValueProposalsAfterCheck =
                  checkIfSpecialValueAndGetSpecialValueProposals(
                      valuesBefore.get(triggerSlot), triggerValue);
              if (specialValueProposalsAfterCheck.length() != 0) {
                shouldBeIncluded = false;
                for (int i = 0; i < specialValueProposalsAfterCheck.length(); i++) {
                  if (specialValueProposalsAfterCheck.get(i).startsWith(incomplete)
                      && (isRepeatingProperty
                          || !inExistingValues(triggerValue, valuesAndSpecialValuesBefore,
                              valuesAndSpecialValuesAfter))) {
                    proposals.add(new AutocompleteProposal(specialValueProposalsAfterCheck.get(i)));
                  }
                }
              }
            } else if (valuesBefore.get(triggerSlot).compareTo(triggerValue) == 0) {
              shouldBeIncluded = true;
            }
          }
        }
        if (shouldBeIncluded) {
          if (currentValue.startsWith(incomplete) && (isRepeatingProperty || !inExistingValues(
              currentValue, valuesAndSpecialValuesBefore, valuesAndSpecialValuesAfter))) {
            proposals.add(new AutocompleteProposal(currentValue));
          }
        }
      }
    }
    return proposals;
  }

  private boolean inExistingValues(String currentValue, JsArrayString valuesAndSpecialValuesAfter,
      JsArrayString valuesAndSpecialValuesBefore) {
    for (int i = 0; i < valuesAndSpecialValuesAfter.length(); i++) {
      if (valuesAndSpecialValuesAfter.get(i).equals(currentValue)) {
        return true;
      }
    }
    for (int i = 0; i < valuesAndSpecialValuesBefore.length(); i++) {
      if (valuesAndSpecialValuesBefore.get(i).equals(currentValue)) {
        return true;
      }
    }
    return false;
  }

  private JsArrayString getValuesAndSpecialValues(JsArrayString existingValues) {
    JsArrayString valuesAndSpecialValuesAfter = JavaScriptObject.createArray().cast();
    for (int i = 0; i < existingValues.length(); i++) {
      String value = existingValues.get(i);
      JsArrayString specialValues = getSpecialValues(value);
      if (specialValues.length() == 0) {
        valuesAndSpecialValuesAfter.push(value);
      }
      for (int j = 0; j < specialValues.length(); j++) {
        valuesAndSpecialValuesAfter.push(specialValues.get(j));
      }
    }
    return valuesAndSpecialValuesAfter;
  }

  private JsArrayString getSpecialValues(String value) {
    JsArrayString specialValues = JavaScriptObject.createArray().cast();
    if (value.isEmpty()) {
      return specialValues;
    }
    if (checkIfAngle(value)) {
      specialValues.push(ANGLE);
    }
    if (checkIfInteger(value)) {
      specialValues.push(INTEGER);
    }
    if (checkIfNumber(value)) {
      specialValues.push(NUMBER);
    }
    if (checkIfUri(value)) {
      specialValues.push(URI);
    }
    if (checkIfPercentage(value)) {
      specialValues.push(PERCENTAGE);
    }
    if (checkIfString(value)) {
      specialValues.push(STRING);
    }
    if (checkIfCounter(value)) {
      specialValues.push(COUNTER);
    }
    if (checkIfIdentifier(value)) {
      specialValues.push(IDENTIFIER);
    }
    if (checkIfFrequency(value)) {
      specialValues.push(FREQUENCY);
    }
    if (checkIfColor(value)) {
      specialValues.push(COLOR);
    }
    if (checkIfLength(value)) {
      specialValues.push(LENGTH);
    }
    if (checkIfShape(value)) {
      specialValues.push(SHAPE);
    }
    return specialValues;
  }

  @VisibleForTesting
  public JsArray<JavaScriptObject> getPropertyValues(String property) {
    return allProperties.<Jso>cast().getJsObjectField(property).cast();
  }

  private native JsArrayString getKeySet(JavaScriptObject jso) /*-{
    var accumulator = [];
    for ( var propertyName in jso) {
      accumulator.push(propertyName);
    }
    return accumulator;
  }-*/;

  private native JavaScriptObject setupAllProperties() /*-{
    return {
        'azimuth' : [
            {
                'left-side' : {},
                'far-left' : {},
                'center-left' : {},
                'center' : {},
                'center-right' : {},
                'right' : {},
                'far-right' : {},
                'right-side' : {},
                '<angle>' : {},
                'leftwards' : {},
                'rightwards' : {},
                'inherit' : {}
            }, {
              'behind' : {
                  'left-side' : 0,
                  'far-left' : 0,
                  'center-left' : 0,
                  'center' : 0,
                  'center-right' : 0,
                  'right' : 0,
                  'far-right' : 0,
                  'right-side' : 0,
                  '<angle>' : 0
              }
            }
        ],
        'background' : [
            {
                '<color>' : {},
                'transparent' : {},
                'inherit' : {}
            }, {
                '<uri>' : {},
                'none' : {},
                'inherit' : {}
            }, {
                'repeat' : {},
                'repeat-x' : {},
                'repeat-y' : {},
                'no-repeat' : {},
                'inherit' : {}
            }, {
                'scroll' : {},
                'fixed' : {},
                'inherit' : {}
            }, {
                '<percentage>' : {},
                '<length>' : {},
                'left' : {},
                'center' : {},
                'right' : {},
                'inherit' : {}
            }, {
                '<percentage>' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                },
                '<length>' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                },
                'top' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                },
                'center' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                },
                'bottom' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                }
            }
        ],
        'background-attachment' : [
          {
              'scroll' : {},
              'fixed' : {},
              'inherit' : {}
          }
        ],
        'background-color' : [
          {
              '<color>' : {},
              'transparent' : {},
              'inherit' : {}
          }
        ],
        'background-image' : [
          {
              '<uri>' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'background-position' : [
            {
                '<percentage>' : {},
                '<length>' : {},
                'left' : {},
                'center' : {},
                'right' : {},
                'inherit' : {}
            }, {
                '<percentage>' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                },
                '<length>' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                },
                'top' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                },
                'center' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                },
                'bottom' : {
                    '<percentage>' : 0,
                    '<length>' : 0,
                    'left' : 0,
                    'center' : 0,
                    'right' : 0
                }
            }
        ],
        'background-repeat' : [
          {
              'repeat' : {},
              'repeat-x' : {},
              'repeat-y' : {},
              'no-repeat' : {},
              'inherit' : {}
          }
        ],
        'border-collapse' : [
          {
              'collapse' : {},
              'separate' : {},
              'inherit' : {}
          }
        ],
        'border-color' : [
            {
                '<color>' : {},
                'transparent' : {},
                'inherit' : {}
            }, {
                '<color>' : {
                    '<color>' : 0,
                    'transparent' : 0
                },
                'transparent' : {
                    '<color>' : 0,
                    'transparent' : 0
                }
            }, {
                '<color>' : {
                    '<color>' : 1,
                    'transparent' : 1
                },
                'transparent' : {
                    '<color>' : 1,
                    'transparent' : 1
                }
            }, {
                '<color>' : {
                    '<color>' : 2,
                    'transparent' : 2
                },
                'transparent' : {
                    '<color>' : 2,
                    'transparent' : 2
                }
            }
        ],
        'border-spacing' : [
            {
                '<length>' : {},
                'inherit' : {}
            }, {
              '<length>' : {
                '<length>' : 0
              }
            }
        ],
        'border-style' : [
            {
                'none' : {},
                'hidden' : {},
                'dotted' : {},
                'dashed' : {},
                'solid' : {},
                'double' : {},
                'groove' : {},
                'ridge' : {},
                'inset' : {},
                'outset' : {},
                'inherit' : {}
            }, {
                'none' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'hidden' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'dotted' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'dashed' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'solid' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'double' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'groove' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'ridge' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'inset' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                },
                'outset' : {
                    'none' : 0,
                    'hidden' : 0,
                    'dotted' : 0,
                    'dashed' : 0,
                    'solid' : 0,
                    'double' : 0,
                    'groove' : 0,
                    'ridge' : 0,
                    'inset' : 0,
                    'outset' : 0
                }
            }, {
                'none' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'hidden' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'dotted' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'dashed' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'solid' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'double' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'groove' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'ridge' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'inset' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'outset' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                }
            }, {
                'none' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'hidden' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'dotted' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'dashed' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'solid' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'double' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'groove' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'ridge' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'inset' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                },
                'outset' : {
                    'none' : 2,
                    'hidden' : 2,
                    'dotted' : 2,
                    'dashed' : 2,
                    'solid' : 2,
                    'double' : 2,
                    'groove' : 2,
                    'ridge' : 2,
                    'inset' : 2,
                    'outset' : 2
                }
            }
        ],
        'border-top' : [
            {
                'thin' : {},
                'medium' : {},
                'thick' : {},
                '<length>' : {},
                'inherit' : {}
            }, {
                'none' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'hidden' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'dotted' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'dashed' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'solid' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'double' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'groove' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'ridge' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'inset' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'outset' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'inherit' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                }
            }, {
                '<color>' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'transparent' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'inherit' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                }
            }
        ],
        'border-bottom' : [
            {
                'thin' : {},
                'medium' : {},
                'thick' : {},
                '<length>' : {},
                'inherit' : {}
            }, {
                'none' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'hidden' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'dotted' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'dashed' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'solid' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'double' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'groove' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'ridge' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'inset' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'outset' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'inherit' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                }
            }, {
                '<color>' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'transparent' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'inherit' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                }
            }
        ],
        'border-left' : [
            {
                'thin' : {},
                'medium' : {},
                'thick' : {},
                '<length>' : {},
                'inherit' : {}
            }, {
                'none' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'hidden' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'dotted' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'dashed' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'solid' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'double' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'groove' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'ridge' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'inset' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'outset' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'inherit' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                }
            }, {
                '<color>' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'transparent' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'inherit' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                }
            }
        ],
        'border-right' : [
            {
                'thin' : {},
                'medium' : {},
                'thick' : {},
                '<length>' : {},
                'inherit' : {}
            }, {
                'none' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'hidden' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'dotted' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'dashed' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'solid' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'double' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'groove' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'ridge' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'inset' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'outset' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'inherit' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                }
            }, {
                '<color>' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'transparent' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                },
                'inherit' : {
                    'none' : 1,
                    'hidden' : 1,
                    'dotted' : 1,
                    'dashed' : 1,
                    'solid' : 1,
                    'double' : 1,
                    'groove' : 1,
                    'ridge' : 1,
                    'inset' : 1,
                    'outset' : 1
                }
            }
        ],
        'border-top-color' : [
          {
              '<color>' : {},
              'transparent' : {},
              'inherit' : {}
          }
        ],
        'border-bottom-color' : [
          {
              '<color>' : {},
              'transparent' : {},
              'inherit' : {}
          }
        ],
        'border-left-color' : [
          {
              '<color>' : {},
              'transparent' : {},
              'inherit' : {}
          }
        ],
        'border-right-color' : [
          {
              '<color>' : {},
              'transparent' : {},
              'inherit' : {}
          }
        ],
        'border-top-style' : [
          {
              'none' : {},
              'hidden' : {},
              'dotted' : {},
              'dashed' : {},
              'solid' : {},
              'double' : {},
              'groove' : {},
              'ridge' : {},
              'inset' : {},
              'outset' : {},
              'inherit' : {}
          }
        ],
        'border-bottom-style' : [
          {
              'none' : {},
              'hidden' : {},
              'dotted' : {},
              'dashed' : {},
              'solid' : {},
              'double' : {},
              'groove' : {},
              'ridge' : {},
              'inset' : {},
              'outset' : {},
              'inherit' : {}
          }
        ],
        'border-left-style' : [
          {
              'none' : {},
              'hidden' : {},
              'dotted' : {},
              'dashed' : {},
              'solid' : {},
              'double' : {},
              'groove' : {},
              'ridge' : {},
              'inset' : {},
              'outset' : {},
              'inherit' : {}
          }
        ],
        'border-right-style' : [
          {
              'none' : {},
              'hidden' : {},
              'dotted' : {},
              'dashed' : {},
              'solid' : {},
              'double' : {},
              'groove' : {},
              'ridge' : {},
              'inset' : {},
              'outset' : {},
              'inherit' : {}
          }
        ],
        'border-top-width' : [
          {
              'thin' : {},
              'medium' : {},
              'thick' : {},
              '<length>' : {},
              'inherit' : {}
          }
        ],
        'border-bottom-width' : [
          {
              'thin' : {},
              'medium' : {},
              'thick' : {},
              '<length>' : {},
              'inherit' : {}
          }
        ],
        'border-left-width' : [
          {
              'thin' : {},
              'medium' : {},
              'thick' : {},
              '<length>' : {},
              'inherit' : {}
          }
        ],
        'border-right-width' : [
          {
              'thin' : {},
              'medium' : {},
              'thick' : {},
              '<length>' : {},
              'inherit' : {}
          }
        ],
        'border-width' : [
            {
                'thin' : {},
                'medium' : {},
                'thick' : {},
                '<length>' : {},
                'inherit' : {}
            }, {
                'thin' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'medium' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                'thick' : {
                    'thin' : 0,
                    'medium' : 0,
                    'thick' : 0,
                    '<length>' : 0
                },
                '<length>' : {
                    'thin' : 1,
                    'medium' : 1,
                    'thick' : 1,
                    '<length>' : 1
                }
            }
        ],
        'border' : [
            {
                '<length>' : {},
                'inherit' : {},
                'transparent' : {},
                'none' : {},
                'hidden' : {},
                'dotted' : {},
                'dashed' : {},
                'solid' : {},
                'double' : {},
                'groove' : {},
                'ridge' : {},
                'inset' : {},
                'outset' : {},
                'thin' : {},
                'medium' : {},
                'thick' : {},
                '<color>' : {}
            }, {
              '+' : {}
            }
        ],
        'bottom' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'caption-side' : [
          {
              'top' : {},
              'bottom' : {},
              'inherit' : {}
          }
        ],
        'clear' : [
          {
              'none' : {},
              'left' : {},
              'right' : {},
              'both' : {},
              'inherit' : {}
          }
        ],
        'clip' : [
          {
              '<shape>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'color' : [
          {
              '<color>' : {},
              'inherit' : {}
          }
        ],
        'content' : [
            {
                'normal' : {},
                'none' : {},
                '<string>' : {},
                '<uri>' : {},
                '<counter>' : {},
                'attr(<identifier>)' : {},
                'open-quote' : {},
                'close-quote' : {},
                'no-open-quote' : {},
                'no-close-quote' : {},
                'inherit' : {}
            }, {
                '<string>' : {},
                '<uri>' : {},
                '<counter>' : {},
                'attr(<identifier>)' : {},
                'open-quote' : {},
                'close-quote' : {},
                'no-open-quote' : {},
                'no-close-quote' : {}
            }, {
              '+' : {}
            }
        ],
        'counter-increment' : [
            {
                '<identifier>' : {},
                '<identifierWithInteger>' : {},
                'none' : {},
                'inherit' : {}
            }, {
                '<identifier>' : {
                    '<identifier>' : 0,
                    '<identifierWithInteger>' : 0
                },
                '<identifierWithInteger>' : {
                    '<identifier>' : 0,
                    '<identifierWithInteger>' : 0
                }
            }, {
              '+' : {}
            }
        ],
        'counter-reset' : [
            {
                '<identifier>' : {},
                '<identifierWithInteger>' : {},
                'none' : {},
                'inherit' : {}
            }, {
                '<identifier>' : {
                    '<identifier>' : 0,
                    '<identifierWithInteger>' : 0
                },
                '<identifierWithInteger>' : {
                    '<identifier>' : 0,
                    '<identifierWithInteger>' : 0
                }
            }, {
              '+' : {}
            }
        ],
        'cue-after' : [
          {
              '<uri>' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'cue-before' : [
          {
              '<uri>' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'cue' : [
            {
                'cue-before' : {},
                'inherit' : {}
            }, {
              'cue-after' : {
                'cue-before' : 0
              }
            }
        ],
        'cursor' : [
            {
                '<uri> ,' : {},
                'auto' : {},
                'crosshair' : {},
                'default' : {},
                'pointer' : {},
                'move' : {},
                'e-resize' : {},
                'ne-resize' : {},
                'nw-resize' : {},
                'n-resize' : {},
                'se-resize' : {},
                'sw-resize' : {},
                's-resize' : {},
                'w-resize' : {},
                'text' : {},
                'wait' : {},
                'help' : {},
                'progress' : {},
                'inherit' : {}
            }, {
              '<uri> ,' : {
                '<uri>' : 0
              }
            }, {
              '+' : {}
            }
        ],
        'direction' : [
          {
              'ltr' : {},
              'rtl' : {},
              'inherit' : {}
          }
        ],
        'display' : [
          {
              'inline' : {},
              'block' : {},
              'list-item' : {},
              'run-in' : {},
              'inline-block' : {},
              'table' : {},
              'inline-table' : {},
              'table-row-group' : {},
              'table-header-group' : {},
              'table-footer-group' : {},
              'table-row' : {},
              'table-column-group' : {},
              'table-column' : {},
              'table-cell' : {},
              'table-caption' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'elevation' : [
          {
              '<angle>' : {},
              'below' : {},
              'level' : {},
              'above' : {},
              'higher' : {},
              'lower' : {},
              'inherit' : {}
          }
        ],
        'empty-cells' : [
          {
              'show' : {},
              'hide' : {},
              'inherit' : {}
          }
        ],
        'float' : [
          {
              'left' : {},
              'right' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'font-family' : [
            {
                '<family-name>' : {},
                'serif' : {},
                'sans-serif' : {},
                'cursive' : {},
                'fantasy' : {},
                'monospace' : {},
                inherit : {}
            }, {
                ', <family-name>' : {
                    '<family-name>' : 0,
                    'serif' : 0,
                    'sans-serif' : 0,
                    'cursive' : 0,
                    'fantasy' : 0,
                    'monospace' : 0
                },
                ', serif' : {
                    '<family-name>' : 0,
                    'serif' : 0,
                    'sans-serif' : 0,
                    'cursive' : 0,
                    'fantasy' : 0,
                    'monospace' : 0
                },
                ', sans-serif' : {
                    '<family-name>' : 0,
                    'serif' : 0,
                    'sans-serif' : 0,
                    'cursive' : 0,
                    'fantasy' : 0,
                    'monospace' : 0
                },
                ', cursive' : {
                    '<family-name>' : 0,
                    'serif' : 0,
                    'sans-serif' : 0,
                    'cursive' : 0,
                    'fantasy' : 0,
                    'monospace' : 0
                },
                ', fantasy' : {
                    '<family-name>' : 0,
                    'serif' : 0,
                    'sans-serif' : 0,
                    'cursive' : 0,
                    'fantasy' : 0,
                    'monospace' : 0
                },
                ', monospace' : {
                    '<family-name>' : 0,
                    'serif' : 0,
                    'sans-serif' : 0,
                    'cursive' : 0,
                    'fantasy' : 0,
                    'monospace' : 0
                }
            }, {
              '+' : {}
            }
        ],
        'font-size' : [
          {
              '<absolute-size>' : {},
              '<relative-size>' : {},
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'font-style' : [
          {
              'normal' : {},
              'italic' : {},
              'oblique' : {},
              'inherit' : {}
          }
        ],
        'font-variant' : [
          {
              'normal' : {},
              'small-caps' : {},
              'inherit' : {}
          }
        ],
        'font-weight' : [
          {
              'normal' : {},
              'bold' : {},
              'bolder' : {},
              'lighter' : {},
              '100' : {},
              '200' : {},
              '300' : {},
              '400' : {},
              '500' : {},
              '600' : {},
              '700' : {},
              '800' : {},
              '900' : {},
              'inherit' : {}
          }
        ],
        'font' : [
            {
                'serif' : {},
                'sans-serif' : {},
                'cursive' : {},
                'fantasy' : {},
                'monospace' : {},
                '<family-name>' : {},
                'inherit' : {},
                'xx-small' : {},
                'x-small' : {},
                'small' : {},
                'medium' : {},
                'large' : {},
                'x-large' : {},
                'xx-large' : {},
                '<length>' : {},
                'larger' : {},
                'smaller' : {},
                'italic' : {},
                'normal' : {},
                'oblique' : {},
                'small-caps' : {},
                '100' : {},
                '200' : {},
                '300' : {},
                '400' : {},
                '500' : {},
                '600' : {},
                '700' : {},
                '800' : {},
                '900' : {},
                'bold' : {},
                'bolder' : {},
                'lighter' : {},
                '<number>' : {},
                '<percentage>' : {},
                'caption' : {},
                'icon' : {},
                'menu' : {},
                'message-box' : {},
                'small-caption' : {},
                'status-bar' : {}
            }, {
              '+' : {}
            }
        ],
        'height' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'left' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'letter-spacing' : [
          {
              'normal' : {},
              '<length>' : {},
              'inherit' : {}
          }
        ],
        'line-height' : [
          {
              'normal' : {},
              '<number>' : {},
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'list-style-image' : [
          {
              '<uri>' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'list-style-position' : [
          {
              'inside' : {},
              'outside' : {},
              'inherit' : {}
          }
        ],
        'list-style-type' : [
          {
              'disc' : {},
              'circle' : {},
              'square' : {},
              'decimal' : {},
              'decimal-leading-zero' : {},
              'lower-roman' : {},
              'upper-roman' : {},
              'lower-greek' : {},
              'lower-latin' : {},
              'upper-latin' : {},
              'armenian' : {},
              'georgian' : {},
              'lower-alpha' : {},
              'upper-alpha' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'list-style' : [
            {
                'disc' : {},
                'circle' : {},
                'square' : {},
                'decimal' : {},
                'decimal-leading-zero' : {},
                'lower-roman' : {},
                'upper-roman' : {},
                'lower-greek' : {},
                'lower-latin' : {},
                'upper-latin' : {},
                'armenian' : {},
                'georgian' : {},
                'lower-alpha' : {},
                'upper-alpha' : {},
                'none' : {},
                'inherit' : {}
            }, {
                'inside' : {},
                'outside' : {},
                'inherit' : {}
            }, {
                '<uri>' : {},
                'none' : {},
                'inherit' : {}
            }
        ],
        'margin-top' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'margin-bottom' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'margin-left' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'margin-right' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'margin' : [
            {
                '<length>' : {},
                '<percentage>' : {},
                'none' : {},
                'inherit' : {}
            }, {
                '<length>' : {},
                '<percentage>' : {},
                'none' : {},
                'inherit' : {}
            }, {
                '<length>' : {},
                '<percentage>' : {},
                'none' : {},
                'inherit' : {}
            }, {
                '<length>' : {},
                '<percentage>' : {},
                'none' : {},
                'inherit' : {}
            }
        ],
        'max-height' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'max-width' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'min-height' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'min-width' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'orphans' : [
          {
              '<integer>' : {},
              'inherit' : {}
          }
        ],
        'outline-color' : [
          {
              '<color>' : {},
              'invert' : {},
              'inherit' : {}
          }
        ],
        'outline-style' : [
          {
              'none' : {},
              'hidden' : {},
              'dotted' : {},
              'dashed' : {},
              'solid' : {},
              'double' : {},
              'groove' : {},
              'ridge' : {},
              'inset' : {},
              'outset' : {},
              'inherit' : {}
          }
        ],
        'outline-width' : [
          {
              'thin' : {},
              'medium' : {},
              'thick' : {},
              '<length>' : {},
              'inherit' : {}
          }
        ],
        'outline' : [
            {
                '<color>' : {},
                'invert' : {},
                'inherit' : {}
            }, {
                'none' : {},
                'hidden' : {},
                'dotted' : {},
                'dashed' : {},
                'solid' : {},
                'double' : {},
                'groove' : {},
                'ridge' : {},
                'inset' : {},
                'outset' : {},
                'inherit' : {}
            }, {
                'thin' : {},
                'medium' : {},
                'thick' : {},
                '<length>' : {},
                'inherit' : {}
            }
        ],
        'overflow' : [
          {
              'visible' : {},
              'hidden' : {},
              'scroll' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'padding-top' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'padding-bottom' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'padding-left' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'padding-right' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'padding' : [
            {
                '<length>' : {},
                '<percentage>' : {},
                'inherit' : {}
            }, {
                '<length>' : {},
                '<percentage>' : {},
                'inherit' : {}
            }, {
                '<length>' : {},
                '<percentage>' : {},
                'inherit' : {}
            }, {
                '<length>' : {},
                '<percentage>' : {},
                'inherit' : {}
            }
        ],
        'page-break-after' : [
          {
              'auto' : {},
              'always' : {},
              'avoid' : {},
              'left' : {},
              'right' : {},
              'inherit' : {}
          }
        ],
        'page-break-before' : [
          {
              'auto' : {},
              'always' : {},
              'avoid' : {},
              'left' : {},
              'right' : {},
              'inherit' : {}
          }
        ],
        'page-break-inside' : [
          {
              'avoid' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'pause-after' : [
          {
              '<time>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'pause-before' : [
          {
              '<time>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'pause' : [
            {
                '<time>' : {},
                '<percentage>' : {},
                'inherit' : {}
            }, {
                '<time>' : {
                    '<time>' : 0,
                    '<percentage>' : 0
                },
                '<percentage>' : {
                    '<time>' : 0,
                    '<percentage>' : 0
                }
            }
        ],
        'pitch-range' : [
          {
              '<number>' : {},
              'inherit' : {}
          }
        ],
        'pitch' : [
          {
              '<frequency>' : {},
              'x-low' : {},
              'low' : {},
              'medium' : {},
              'high' : {},
              'x-high' : {},
              'inherit' : {}
          }
        ],
        'play-during' : [
            {
                '<uri>' : {},
                'auto' : {},
                'none' : {},
                'inherit' : {}
            }, {
                'mix' : {
                  '<uri>' : 0
                },
                'repeat' : {
                  '<uri>' : 0
                }
            }
        ],
        'position' : [
          {
              'static' : {},
              'relative' : {},
              'absolute' : {},
              'fixed' : {},
              'inherit' : {}
          }
        ],
        'quotes' : [
            {
                '<string>' : {},
                'none' : {},
                'inherit' : {}
            }, {
              '<string>' : {
                '<string>' : 0
              }
            }, {
              '+' : {}
            }
        ],
        'richness' : [
          {
              '<number>' : {},
              'inherit' : {}
          }
        ],
        'right' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'speak-header' : [
          {
              'once' : {},
              'always' : {},
              'inherit' : {}
          }
        ],
        'speak-numeral' : [
          {
              'digits' : {},
              'continuous' : {},
              'inherit' : {}
          }
        ],
        'speak-punctuation' : [
          {
              'code' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'speak' : [
          {
              'normal' : {},
              'none' : {},
              'spell-out' : {},
              'inherit' : {}
          }
        ],
        'speech-rate' : [
          {
              '<number>' : {},
              'x-slow' : {},
              'slow' : {},
              'medium' : {},
              'fast' : {},
              'x-fast' : {},
              'faster' : {},
              'slower' : {},
              'inherit' : {}
          }
        ],
        'stress' : [
          {
              '<number>' : {},
              'inherit' : {}
          }
        ],
        'table-layout' : [
          {
              'auto' : {},
              'fixed' : {},
              'inherit' : {}
          }
        ],
        'text-align' : [
          {
              'left' : {},
              'right' : {},
              'center' : {},
              'justify' : {},
              'inherit' : {}
          }
        ],
        'text-decoration' : [
            {
                'none' : {},
                'underline' : {},
                'inherit' : {}
            }, {
              'overline' : {
                'underline' : 0
              }
            }, {
              'line-through' : {
                'overline' : 1
              }
            }, {
              'blink' : {
                'line-through' : 2
              }
            }
        ],
        'text-indent' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'inherit' : {}
          }
        ],
        'text-transform' : [
          {
              'capitalize' : {},
              'uppercase' : {},
              'lowercase' : {},
              'none' : {},
              'inherit' : {}
          }
        ],
        'top' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'unicode-bidi' : [
          {
              'normal' : {},
              'embed' : {},
              'bidi-override' : {},
              'inherit' : {}
          }
        ],
        'vertical-align' : [
          {
              'baseline' : {},
              'sub' : {},
              'super' : {},
              'top' : {},
              'text-top' : {},
              'middle' : {},
              'bottom' : {},
              'text-bottom' : {},
              '<percentage>' : {},
              '<length>' : {},
              'inherit' : {}
          }
        ],
        'visibility' : [
          {
              'visible' : {},
              'hidden' : {},
              'collapse' : {},
              'inherit' : {}
          }
        ],
        'voice-family' : [
            {
                'comedian' : {},
                'trinoids' : {},
                'carlos' : {},
                'lani' : {},
                'male' : {},
                'female' : {},
                'child' : {},
                'comedian,' : {},
                'trinoids,' : {},
                'carlos,' : {},
                'lani,' : {},
                'male,' : {},
                'female,' : {},
                'child,' : {},
                'inherit' : {}
            }, {
                'comedian' : {
                    'comedian,' : {},
                    'trinoids,' : {},
                    'carlos,' : {},
                    'lani,' : {},
                    'male,' : {},
                    'female,' : {},
                    'child,' : {}
                },
                'trinoids' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'carlos' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'lani' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'male' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'female' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'child' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'comedian,' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'trinoids,' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'carlos,' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'lani,' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'male,' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'female,' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                },
                'child,' : {
                    'comedian,' : 0,
                    'trinoids,' : 0,
                    'carlos,' : 0,
                    'lani,' : 0,
                    'male,' : 0,
                    'female,' : 0,
                    'child,' : 0
                }
            }, {
              '+' : {}
            }
        ],
        'volume' : [
          {
              '<number>' : {},
              '<percentage>' : {},
              'silent' : {},
              'x-soft' : {},
              'soft' : {},
              'medium' : {},
              'loud' : {},
              'x-loud' : {},
              'inherit' : {}
          }
        ],
        'white-space' : [
          {
              'normal' : {},
              'pre' : {},
              'nowrap' : {},
              'pre-wrap' : {},
              'pre-line' : {},
              'inherit' : {}
          }
        ],
        'widows' : [
          {
              '<integer>' : {},
              'inherit' : {}
          }
        ],
        'width' : [
          {
              '<length>' : {},
              '<percentage>' : {},
              'auto' : {},
              'inherit' : {}
          }
        ],
        'word-spacing' : [
          {
              'normal' : {},
              '<length>' : {},
              'inherit' : {}
          }
        ],
        'z-index' : [
          {
              'auto' : {},
              '<integer>' : {},
              'inherit' : {}
          }
        ]
    };
  }-*/;

  private native JavaScriptObject setupSpecialValueProposals() /*-{
    return {
        '<angle>' : [
            '<number>deg', '<number>grad', '<number>rad'
        ],
        '<family-name>' : [
          '<family-name>'
        ],
        '<number>' : [
          '<number>'
        ],
        '<integer>' : [
          '<integer>'
        ],
        '<color>' : [
            'black',
            'blue',
            'fuchsia',
            'gray',
            'green',
            'lime',
            'maroon',
            'navy',
            'olive',
            'orange',
            'purple',
            'red',
            'silver',
            'teal',
            'white',
            'yellow',
            'rgb(<number>, <number>, <number>)',
            'rgb(<percentage>, <percentage>, <percentage>',
            '#rgb',
            '#rrggbb'
        ],
        '<uri>' : [
          '<uri>'
        ],
        '<length>' : [
            '<number>em',
            '<number>ex',
            '<number>in',
            '<number>cm',
            '<number>mm',
            '<number>pt',
            '<number>pc',
            '<number>px'
        ],
        '<percentage>' : [
          '<number>%'
        ],
        '<relative-size>' : [
          '<relative-size>'
        ],
        '<absolute-size>' : [
          '<absolute-size>'
        ],
        '<shape>' : [
          '<shape>'
        ],
        '<string>' : [
          '<string>'
        ],
        '<counter>' : [
          '<counter>'
        ],
        '<identifier>' : [
          '<identifer>'
        ],
        '<frequency>' : [
          '<frequency>'
        ]
    };
  }-*/;

  // TODO: Make this a bit smarter by allowing only for repetition up
  // to a certain number.
  private void setupRepeatingProperties(JsMapFromStringToBoolean repeatingProperties) {
    repeatingProperties.put("border-color", true);
    repeatingProperties.put("border-style", true);
    repeatingProperties.put("border-width", true);
    repeatingProperties.put("margin", true);
    repeatingProperties.put("quotes", true);
    repeatingProperties.put("padding", true);
    repeatingProperties.put("pause", true);
  }
}
