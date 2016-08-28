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

package com.google.collide.client.code.autocomplete.codegraph;

import static com.google.collide.client.code.autocomplete.AutocompleteResult.PopupAction.CLOSE;

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.code.autocomplete.AutocompleteResult;
import com.google.collide.client.code.autocomplete.DefaultAutocompleteResult;
import com.google.collide.shared.util.StringUtils;

/**
 * Proposal that contains template and knows how to process it.
 *
 * <p>Template is a string which may contain the following wildcard symbols:<ul>
 *   <li>%n - new line character with indentation to the level of
 *            the inserting place;
 *   <li>%i - additional indentation;
 *   <li>%c - a point to place the cursor to after inserting.
 * </ul>
 */
public class TemplateProposal extends AutocompleteProposal {

  private final String template;

  public TemplateProposal(String name, String template) {
    super(name);
    this.template = template;
  }

  /**
   * Translates template to {@link AutocompleteResult}.
   */
  public AutocompleteResult buildResult(String triggeringString, int indent) {
    String lineStart = "\n" + StringUtils.getSpaces(indent);
    String replaced = template
        .replace("%n", lineStart)
        .replace("%i", indent(lineStart))
        .replace("%d", dedent(lineStart));
    int pos = replaced.indexOf("%c");
    pos = (pos == -1) ? replaced.length() : pos;
    String completion = replaced.replace("%c", "");
    return new DefaultAutocompleteResult(completion, pos, 0, 0, 0, CLOSE, triggeringString);
  }

  /**
   * Adds an indentation to a given string.
   *
   * <p>We suppose extra indention to be double-space.
   */
  private static String indent(String s) {
    return s + "  ";
  }

  /**
   * Removes extra indention.
   *
   * <p>We suppose extra indention to be double-space.
   */
  private static String dedent(String s) {
    if (s.endsWith("  ")) {
      return s.substring(0, s.length() - 2);
    }
    return s;
  }
}
