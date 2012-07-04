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

package com.google.collide.shared.metrics;

/**
 * An Action is something that is explicitly performed by the user. This is
 * usually application specific. For example, when a user creates a domain
 * object such as a "project", the corresponding action may be "CREATE_PROJECT".
 *
 *  This is the base interface, which all Action interfaces and enums should
 * implement.
 *
 * Currently, this interface is empty, but is used for type safety in the
 * Metrics instrumentation class.
 *
 *
 */
public interface Action {

}