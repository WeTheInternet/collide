package com.google.collide.client.editor;

import com.google.collide.client.editor.renderer.LineNumberRenderer;
import com.google.collide.client.editor.search.SearchMatchRenderer;
import com.google.collide.client.editor.selection.CursorView;
import com.google.collide.client.editor.selection.SelectionLineRenderer;
import com.google.collide.client.util.UserActivityManager;

public interface EditorContext <R extends 
  Editor.Resources &
  SearchMatchRenderer.Resources & 
  SelectionLineRenderer.Resources & 
  LineNumberRenderer.Resources &
  CursorView.Resources &
  Buffer.Resources> {

  UserActivityManager getUserActivityManager();

  R getResources();

}
