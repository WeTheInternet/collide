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

package com.google.collide.client.code.autocomplete.html;

import com.google.collide.client.code.autocomplete.AutocompleteProposal;
import com.google.collide.client.util.collections.SkipListStringBag;
import com.google.collide.client.util.collections.StringMultiset;
import com.google.collide.json.client.Jso;
import com.google.collide.json.client.JsoArray;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.json.shared.JsonStringMap;
import com.google.collide.json.shared.JsonStringSet;
import com.google.collide.shared.util.JsonCollections;

// TODO: Implement and use unmodifiable-sored-string-list.
// TODO: Implement type-specific attributes filtering.
// TODO: Implement attribute-specific autocompletion.
/**
 * This class holds map of HTML5 tags with corresponding attributes.
 *
 */
public class HtmlTagsAndAttributes {

  private static HtmlTagsAndAttributes instance;

  private final JsonStringSet selfClosedTags;
  private final SkipListStringBag tags;
  private final JsonStringMap<SkipListStringBag> attributes = JsonCollections.createMap();

  public static HtmlTagsAndAttributes getInstance() {
    if (instance == null) {
      instance = new HtmlTagsAndAttributes();
    }
    return instance;
  }

  private HtmlTagsAndAttributes() {
    selfClosedTags = JsonCollections.createStringSet(
        makeSelfClosedTagsArray().asIterable().iterator());
    tags = new SkipListStringBag();

    Jso jso = makeNestedAttributesMap();
    for (String tag : jso.getKeys().asIterable()) {
      tags.add(tag);

      SkipListStringBag attributesSet = new SkipListStringBag();
      flattenAttributes(jso.getArrayField(tag), attributesSet);
      attributes.put(tag, attributesSet);
    }
  }

  public boolean isSelfClosedTag(String tag) {
    return selfClosedTags.contains(tag.toLowerCase());
  }

  public JsonArray<AutocompleteProposal> searchTags(String prefix) {
    JsonArray<AutocompleteProposal> result = JsonCollections.createArray();
    for (String tag : tags.search(prefix)) {
      if (!tag.startsWith(prefix)) {
        break;
      }
      // TODO: Do we need to cache that?
      result.add(new AutocompleteProposal(tag));
    }
    return result;
  }

  /**
   * Gets autocompletions for a specific element, narrowed down by the already
   * present attributes and the incomplete attribute string.
   *
   * @param tag the enclosing HTML tag
   * @param alreadyPresentAttributes the attributes that are already present in
   *        the editor
   * @param incomplete the incomplete attribute that we find autocomplete
   *        proposals for
   * @return all matching autocompletion proposals
   */
  public JsonArray<AutocompleteProposal> searchAttributes(
      String tag, StringMultiset alreadyPresentAttributes, String incomplete) {
    JsonArray<AutocompleteProposal> result = JsonCollections.createArray();
    tag = tag.toLowerCase();
    incomplete = incomplete.toLowerCase();

    SkipListStringBag tagAttributes = attributes.get(tag);

    if (tagAttributes == null) {
      return result;
    }

    for (String attribute : tagAttributes.search(incomplete)) {
      if (!attribute.startsWith(incomplete)) {
        break;
      }
      if (alreadyPresentAttributes.contains(attribute)) {
        continue;
      }
      // TODO: Do we need to cache that?
      result.add(new AutocompleteProposal(attribute));
    }

    return result;
  }

  private static native String devmodeWorkaround(Object f) /*-{
    return f;
  }-*/;

  private void flattenAttributes(JsoArray input, SkipListStringBag output) {
    if (input == null) {
      return;
    }

    for (Object jso : input.asIterable()) {
      if (jso instanceof String) {
        output.add(devmodeWorkaround(jso));
      } else {
        flattenAttributes((JsoArray) jso, output);
      }
    }
  }

  private static native JsoArray<String> makeSelfClosedTagsArray() /*-{
    return ['area', 'base', 'br', 'col', 'command', 'embed', 'hr', 'img', 'input', 'keygen', 'link',
      'meta', 'param', 'source', 'track', 'wbr'];
  }-*/;

  private static native Jso makeNestedAttributesMap() /*-{
    var commonAttrsCore = ['accesskey', 'class', 'contenteditable', 'contextmenu', 'dir',
      'draggable', 'hidden', 'id', 'lang', 'spellcheck', 'style', 'tabindex', 'title'];

    var commonAttrsEventHandler = ['onabort', 'onblur', 'oncanplay', 'oncanplaythrough', 'onchange',
      'onclick', 'oncontextmenu', 'ondblclick', 'ondrag', 'ondragend', 'ondragenter', 'ondragleave',
      'ondragover', 'ondragstart', 'ondrop', 'ondurationchange', 'onemptied', 'onended', 'onerror',
      'onfocus', 'onformchange', 'onforminput', 'oninput', 'oninvalid', 'onkeydown', 'onkeypress',
      'onkeyup', 'onload', 'onloadeddata', 'onloadedmetadata', 'onloadstart', 'onmousedown',
      'onmousemove', 'onmouseout', 'onmouseover', 'onmouseup', 'onmousewheel', 'onpause', 'onplay',
      'onplaying', 'onprogress', 'onratechange', 'onreadystatechange', 'onscroll', 'onseeked',
      'onseeking', 'onselect', 'onshow', 'onstalled', 'onsubmit', 'onsuspend', 'ontimeupdate',
      'onvolumechange', 'onwaiting'];

    var commonAttrsXml = ['xml:lang', 'xml:space', 'xml:base'];

    // This is non-normative yet, but widely used by search engines already.
    var commonAttrsMicrodata = ['itemid', 'itemprop', 'itemref', 'itemscope', 'itemtype'];

    var commonAttrs = [commonAttrsCore, commonAttrsEventHandler, commonAttrsXml,
      commonAttrsMicrodata];

    var commonAttrsAriaExpanded = ['aria-expanded'];

    var commonAttrsAriaActiveDescendant = ['aria-activedescendant'];
    
    var commonAttrsAriaImplicitListItem = ['aria-posinset', 'aria-setsize'];
    
    var commonAttrsAriaImplicitTh = ['aria-sort', 'aria-level', commonAttrsAriaExpanded,
      'aria-readonly', 'aria-selected'];

    var commonAttrsAria = [commonAttrsAriaActiveDescendant, 'aria-atomic', 'aria-autocomplete',
      'aria-busy', 'aria-checked', 'aria-controls', 'aria-describedby', 'aria-disabled',
      'aria-dropeffect', 'aria-flowto', 'aria-grabbed', 'aria-haspopup', 'aria-hidden',
      'aria-invalid', 'aria-label', 'aria-labelledby', 'aria-live', 'aria-multiline',
      'aria-multiselectable', 'aria-owns', 'aria-pressed', commonAttrsAriaImplicitTh,
      'aria-relevant', 'aria-required', commonAttrsAriaImplicitListItem, 'aria-valuemax',
      'aria-valuemin', 'aria-valuenow', 'aria-valuetext'];

    var commonAttrsAriaImplicitGroup = [commonAttrsAriaExpanded, commonAttrsAriaActiveDescendant];

    return {
      'a' : [commonAttrs, 'name', 'href', 'target', 'rel', 'hreflang', 'media', 'type', 'ping',
        commonAttrsAria],
      'abbr' : [commonAttrs, commonAttrsAria],
      'address' : [commonAttrs, commonAttrsAriaExpanded],
      'area' : [commonAttrs, 'alt','href', 'target', 'ping', 'rel', 'media', 'hreflang', 'type',
        'shape', 'coords'],
      'article' : [commonAttrs, 'pubdate', commonAttrsAriaExpanded],
      'aside' : [commonAttrs, commonAttrsAriaExpanded],
      'audio' : [commonAttrs, 'autoplay', 'autobuffer', 'controls', 'loop', 'src'],
      'b' : [commonAttrs, commonAttrsAria],
      'base' : [commonAttrs, 'href', 'target'],
      'bdo' : [commonAttrs],
      'blockquote' : [commonAttrs, commonAttrsAria, 'cite'],
      'body' : [commonAttrs, commonAttrsAriaExpanded, 'onafterprint', 'onbeforeprint',
        'onbeforeunload', 'onhashchange', 'onmessage', 'onoffline', 'ononline', 'onpopstate',
        'onredo', 'onresize', 'onstorage', 'onundo', 'onunload'],
      'br' : [commonAttrs],
      'button' : [commonAttrs, commonAttrsAria, 'name', 'disabled', 'form', 'type', 'value',
        'formaction', 'autofocus', 'formenctype', 'formmethod', 'formtarget', 'formnovalidate'],
      'canvas' : [commonAttrs, commonAttrsAria, 'height', 'width'],
      'caption' : [commonAttrs, commonAttrsAriaExpanded],
      'cite' : [commonAttrs, commonAttrsAria],
      'code' : [commonAttrs, commonAttrsAria],
      'col' : [commonAttrs, 'span'],
      'colgroup' : [commonAttrs, 'span'],
      'command' : [commonAttrs, 'type', 'label', 'icon', 'disabled', 'radiogroup', 'checked'],
      'datalist' : [commonAttrs],
      'dd' : [commonAttrs, commonAttrsAria],
      'del' : [commonAttrs, 'cite', 'datetime'],
      'details' : [commonAttrs, commonAttrsAriaExpanded, 'open'],
      'dfn' : [commonAttrs, commonAttrsAria],
      'dialog' : [commonAttrs, commonAttrsAriaExpanded],
      'div' : [commonAttrs, commonAttrsAria],
      'dl' : [commonAttrs, commonAttrsAria],
      'dt' : [commonAttrs, commonAttrsAria],
      'em' : [commonAttrs, commonAttrsAria],
      'embed' : [commonAttrs, 'src', 'type', 'height', 'width'],
      'fieldset' : [commonAttrs, commonAttrsAriaImplicitGroup, 'name', 'disabled', 'form'],
      'figure' : [commonAttrs],
      'footer' : [commonAttrs, commonAttrsAriaExpanded],
      'form' : [commonAttrs, commonAttrsAriaExpanded, 'action', 'method', 'enctype', 'name',
        'accept-charset', 'novalidate', 'target', 'autocomplete'],
      'h1' : [commonAttrs],
      'h2' : [commonAttrs],
      'h3' : [commonAttrs],
      'h4' : [commonAttrs],
      'h5' : [commonAttrs],
      'h6' : [commonAttrs],
      'head' : [commonAttrs],
      'header' : [commonAttrs, commonAttrsAriaExpanded],
      'hgroup' : [commonAttrs],
      'hr' : [commonAttrs],
      'html' : [commonAttrs, 'manifest'],
      'i' : [commonAttrs, commonAttrsAria],
      'iframe' : [commonAttrs, commonAttrsAria, 'src', 'name', 'width', 'height', 'sandbox',
        'seamless'],
      'img' : [commonAttrs, commonAttrsAria, 'src', 'alt', 'height', 'width', 'usemap', 'ismap',
        'border'],
      'input' : [commonAttrs, commonAttrsAria, 'disabled', 'form', 'type', 'maxlength', 'readonly',
        'size', 'value', 'autocomplete', 'autofocus', 'list', 'pattern', 'required', 'placeholder',
        'checked', 'formaction', 'autofocus', 'formenctype', 'formmethod', 'formtarget',
        'formnovalidate', 'multiple', 'accept', 'alt', 'src', 'height', 'width',  'list', 'min',
        'max', 'step', 'readonly'],
      'ins' : [commonAttrs, 'cite', 'datetime'],
      'kbd' : [commonAttrs, commonAttrsAria],
      'keygen' : [commonAttrs, 'challenge', 'keytype', 'autofocus', 'name', 'disabled', 'form'],
      'label' : [commonAttrs, commonAttrsAriaExpanded, 'for', 'form'],
      'legend' : [commonAttrs, commonAttrsAriaExpanded],
      'li' : [commonAttrs, commonAttrsAria, 'value'],
      'link' : [commonAttrs,  'href', 'rel', 'hreflang', 'media', 'type', 'sizes'],
      'map' : [commonAttrs, 'name'],
      'mark' : [commonAttrs],
      'menu' : [commonAttrs, 'type', 'label'],
      'meta' : [commonAttrs, 'name', 'content', 'http-equiv', 'content', 'charset'],
      'meter' : [commonAttrs, 'value', 'min', 'low', 'high', 'max', 'optimum'],
      'nav' : [commonAttrs, commonAttrsAriaExpanded],
      'noscript' : [commonAttrs],
      'object' : [commonAttrs, commonAttrsAria, 'data', 'type', 'height', 'width', 'usemap', 'name',
        'form'],
      'ol' : [commonAttrs, commonAttrsAria, 'start', 'reversed'],
      'optgroup' : [commonAttrs,  'label', 'disabled'],
      'option' : [commonAttrs, 'disabled', 'selected', 'label', 'value'],
      'output' : [commonAttrs, commonAttrsAriaExpanded, 'name', 'form', 'for'],
      'p' : [commonAttrs, commonAttrsAria],
      'param' : [commonAttrs, 'name', 'value'],
      'pre' : [commonAttrs, commonAttrsAria],
      'progress' : [commonAttrs, 'value', 'max'],
      'q' : [commonAttrs, commonAttrsAria, 'cite'],
      'rp' : [commonAttrs, commonAttrsAria],
      'rt' : [commonAttrs, commonAttrsAria],
      'ruby' : [commonAttrs, commonAttrsAria],
      'samp' : [commonAttrs, commonAttrsAria],
      'script' : [commonAttrs, 'src', 'defer', 'async', 'type', 'charset', 'language'],
      'section' : [commonAttrs, commonAttrsAriaExpanded],
      'select' : [commonAttrs, 'name', 'disabled', 'form', 'size', 'multiple'],
      'small' : [commonAttrs, commonAttrsAria],
      'source' : [commonAttrs, 'src', 'type', 'media'],
      'span' : [commonAttrs, commonAttrsAria],
      'strong' : [commonAttrs, commonAttrsAria],
      'style' : [commonAttrs, 'type', 'media', 'scoped'],
      'sub' : [commonAttrs],
      'sup' : [commonAttrs],
      'table' : [commonAttrs, commonAttrsAria, 'summary'],
      'tbody' : [commonAttrs],
      'td' : [commonAttrs, commonAttrsAria, 'colspan', 'rowspan', 'headers'],
      'textarea' : [commonAttrs, ' name', 'disabled', 'form', 'readonly', 'maxlength', 'autofocus',
        'required', 'placeholder', 'rows', 'wrap', 'cols'],
      'tfoot' : [commonAttrs],
      'th' : [commonAttrs, commonAttrsAriaImplicitTh, 'scope', 'colspan', 'rowspan', 'headers'],
      'thead' : [commonAttrs],
      'time' : [commonAttrs, 'datetime'],
      'title' : [commonAttrs],
      'tr' : [commonAttrs, commonAttrsAria],
      'track' : [commonAttrs, 'default', 'kind', 'label', 'src', 'srclang'],
      'ul' : [commonAttrs, commonAttrsAria],
      'var' : [commonAttrs, commonAttrsAria],
      'video' : [commonAttrs, 'autoplay', 'autobuffer', 'controls', 'loop', 'poster', 'height',
        'width', 'src'],
      'wbr' : [commonAttrs]
    };
  }-*/;
}
