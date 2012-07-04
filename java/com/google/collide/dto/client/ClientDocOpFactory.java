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

package com.google.collide.dto.client;

import static com.google.collide.dto.DocOpComponent.Type.*;

import com.google.collide.dto.DocOp;
import com.google.collide.dto.DocOpComponent;
import com.google.collide.dto.DocOpComponent.Delete;
import com.google.collide.dto.DocOpComponent.Insert;
import com.google.collide.dto.DocOpComponent.Retain;
import com.google.collide.dto.DocOpComponent.RetainLine;
import com.google.collide.dto.client.DtoClientImpls.DeleteImpl;
import com.google.collide.dto.client.DtoClientImpls.DocOpImpl;
import com.google.collide.dto.client.DtoClientImpls.InsertImpl;
import com.google.collide.dto.client.DtoClientImpls.RetainImpl;
import com.google.collide.dto.client.DtoClientImpls.RetainLineImpl;
import com.google.collide.dto.shared.DocOpFactory;
import com.google.collide.json.client.JsoArray;

// TODO: These should be moved to an Editor2-specific package
/**
 */
public final class ClientDocOpFactory implements DocOpFactory {

  public static final ClientDocOpFactory INSTANCE = new ClientDocOpFactory();

  private ClientDocOpFactory() {
  }

  @Override
  public Delete createDelete(String text) {
    return (Delete) DeleteImpl.make().setText(text).setType(DELETE);
  }

  @Override
  public DocOp createDocOp() {
    return DocOpImpl.make().setComponents(JsoArray.<DocOpComponent>create());
  }

  @Override
  public Insert createInsert(String text) {
    return (Insert) InsertImpl.make().setText(text).setType(INSERT);
  }

  @Override
  public Retain createRetain(int count, boolean hasTrailingNewline) {
    return (Retain) RetainImpl.make().setCount(count).setHasTrailingNewline(hasTrailingNewline)
        .setType(RETAIN);
  }

  @Override
  public RetainLine createRetainLine(int lineCount) {
    return (RetainLine) RetainLineImpl.make().setLineCount(lineCount).setType(RETAIN_LINE);
  }
}
