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

package com.google.collide.client.workspace;

import com.google.collide.client.AppContext;
import com.google.collide.client.code.FileSelectionController.FileOpenedEvent;
import com.google.collide.client.communication.ResourceUriUtils;
import com.google.collide.client.history.Place;
import com.google.collide.client.plugin.ClientPlugin;
import com.google.collide.client.plugin.ClientPluginService;
import com.google.collide.client.plugin.RunConfiguration;
import com.google.collide.client.search.FileNameSearch;
import com.google.collide.client.ui.menu.AutoHideComponent.AutoHideHandler;
import com.google.collide.client.ui.menu.PositionController.HorizontalAlign;
import com.google.collide.client.ui.menu.PositionController.Positioner;
import com.google.collide.client.ui.menu.PositionController.VerticalAlign;
import com.google.collide.client.ui.tooltip.Tooltip;
import com.google.collide.client.ui.tooltip.Tooltip.TooltipRenderer;
import com.google.collide.client.util.Elements;
import com.google.collide.client.util.PathUtil;
import com.google.collide.client.workspace.RunButtonTargetPopup.RunTargetType;
import com.google.collide.dto.RunTarget;
import com.google.collide.dto.UpdateWorkspaceRunTargets;
import com.google.collide.dto.client.DtoClientImpls.RunTargetImpl;
import com.google.collide.dto.client.DtoClientImpls.UpdateWorkspaceRunTargetsImpl;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.dom.client.Element;

import elemental.css.CSSStyleDeclaration;
import elemental.html.DivElement;
import elemental.html.SpanElement;
import elemental.util.Collections;
import elemental.util.MapFromStringTo;

/**
 * Controller for the behavior of the run button on the workspace header.
 */
public class RunButtonController {

  public static RunButtonController create(AppContext context,
      Element buttonElem,
      Element dropdownElem,
      Place currentPlace,
      FileNameSearch fileNameSearch,
      FileTreeModel fileTreeModel) {

    //initialize our run configs with any items added by plugins
    MapFromStringTo<RunConfiguration> runConfigs = Collections.mapFromStringTo();
    for (ClientPlugin<?> p : ClientPluginService.getPlugins()) {
      RunConfiguration runConfig = p.getRunConfig();
      if (runConfig != null) {
        runConfigs.put(runConfig.getId(), runConfig);
      }
    }


    RunButtonTargetPopup targetPopup =
        RunButtonTargetPopup.create(context, buttonElem, dropdownElem, fileNameSearch, runConfigs);

    elemental.dom.Element target = Elements.asJsElement(buttonElem);
    Positioner positioner = new Tooltip.TooltipPositionerBuilder().setVerticalAlign(
        VerticalAlign.BOTTOM)
        .setHorizontalAlign(HorizontalAlign.RIGHT).buildAnchorPositioner(target);
    Tooltip noFileSelectedTooltip = new Tooltip.Builder(
        context.getResources(), target, positioner).setShouldListenToHover(false)
        .setTooltipRenderer(new TooltipRenderer() {
          @Override
          public elemental.dom.Element renderDom() {
            DivElement container = Elements.createDivElement();

            DivElement header = Elements.createDivElement();
            header.setTextContent("No File Selected");
            header.getStyle().setFontWeight(CSSStyleDeclaration.FontWeight.BOLDER);
            header.getStyle().setMarginBottom(5, CSSStyleDeclaration.Unit.PX);
            container.appendChild(header);

            DivElement text = Elements.createDivElement();
            text.setTextContent(
                "Choose a file from the tree to preview it, or select a custom run target.");
            container.appendChild(text);

            return container;
          }
        }).build();

    Tooltip yamlAddedTooltip = new Tooltip.Builder(
        context.getResources(), target, positioner).setShouldListenToHover(false)
        .setTooltipRenderer(new TooltipRenderer() {
          @Override
          public elemental.dom.Element renderDom() {
            DivElement container = Elements.createDivElement();

            SpanElement text = Elements.createSpanElement();
            text.setTextContent(
                "The run target has been set to your newly created app.yaml file. ");
            container.appendChild(text);

            // TODO: We'd like to offer an option to undo the
            // automatic setting of the run target, but I don't have time
            // to write a coach tips class right now and tool tips can't be
            // clicked.
            return container;
          }
        }).build();

    Tooltip targetResetTooltip = new Tooltip.Builder(
        context.getResources(), target, positioner).setShouldListenToHover(false)
        .setTooltipRenderer(new TooltipRenderer() {
          @Override
          public elemental.dom.Element renderDom() {
            DivElement container = Elements.createDivElement();

            SpanElement text = Elements.createSpanElement();
            text.setTextContent(
                "You deleted your run target, the run button has been reset to preview "
                + "the active file.");
            container.appendChild(text);

            // TODO: We'd like to offer undo, but I don't have time
            // to write a coach tips class right now and tool tips can't be
            // clicked.
            return container;
          }
        }).build();
    return new RunButtonController(context,
        dropdownElem,
        currentPlace,
        fileNameSearch,
        targetPopup,
        runConfigs,
        fileTreeModel,
        noFileSelectedTooltip,
        yamlAddedTooltip,
        targetResetTooltip);
  }

  /**
   * A listener which handles events in the file tree which may affect the current RunTarget.
   */
  public class FileTreeChangeListener extends FileTreeModel.AbstractTreeModelChangeListener {
    @Override
    public void onNodeAdded(PathUtil parentDirPath, FileTreeNode newNode) {
      RunTarget currentTarget = targetPopup.getRunTarget();
      if ("ALWAYS_RUN".equals(currentTarget.getRunMode())
          && currentTarget.getAlwaysRunFilename().contains("app.yaml")) {
        return;
      }

      // did the user add an app.yaml?
      if (newNode.isFile() && newNode.getName().equals("app.yaml")) {
        // lets set the run target to run the app.yaml
        RunTarget target = RunTargetImpl.make().setRunMode("ALWAYS_RUN")
            .setAlwaysRunFilename(newNode.getNodePath().getPathString()).setAlwaysRunUrlOrQuery("");

        updateRunTarget(target);

        targetPopup.setRunTarget(target);

        yamlSelectedFileTooltip.show(TOOLTIP_DISPLAY_TIMEOUT_MS);
      }
    }

    @Override
    public void onNodeMoved(
        PathUtil oldPath, FileTreeNode node, PathUtil newPath, FileTreeNode newNode) {
      RunTarget currentTarget = targetPopup.getRunTarget();
      if (currentTarget == null || currentTarget.getAlwaysRunFilename() == null) {
        return;
      }

      PathUtil currentAlwaysRunPath = new PathUtil(currentTarget.getAlwaysRunFilename());
      PathUtil relativePath = currentAlwaysRunPath.makeRelativeToParent(oldPath);
      if (relativePath != null) {
        // Either this node, or some ancestor node got renamed.
        PathUtil newFilePath = PathUtil.concatenate(newPath, relativePath);
        RunTargetImpl impl = (RunTargetImpl) currentTarget;
        impl.setAlwaysRunFilename(newFilePath.getPathString());
        targetPopup.setRunTarget(impl);
      }
    }

    @Override
    public void onNodesRemoved(JsonArray<FileTreeNode> oldNodes) {
      RunTarget target = targetPopup.getRunTarget();
      // we should clear the current file if the user deleted that one
      // we should revert the run target to preview if the user deleted the
      // "always run target".

      // If this gets more complicated, make it a visitor type thing
      boolean isAlwaysRun = "ALWAYS_RUN".equals(target.getRunMode());
      boolean hasClearedCurrentFile = currentFilePath == null;
      boolean hasClearedAlwaysRun = !isAlwaysRun;
      PathUtil alwaysRunPath = isAlwaysRun ? new PathUtil(target.getAlwaysRunFilename()) : null;

      for (int i = 0; (!hasClearedCurrentFile || !hasClearedAlwaysRun) && i < oldNodes.size();
          i++) {
        FileTreeNode node = oldNodes.get(i);
        if (!hasClearedCurrentFile) {
          if (node.getNodePath().containsPath(currentFilePath)) {
            updateCurrentFile(null);
            hasClearedCurrentFile = true;
          }
        }
        if (!hasClearedAlwaysRun) {
          if (node.getNodePath().containsPath(alwaysRunPath)) {
            RunTarget newTarget = RunTargetImpl.make().setRunMode("PREVIEW_CURRENT_FILE");
            updateRunTarget(newTarget);
            targetResetTooltip.show(TOOLTIP_DISPLAY_TIMEOUT_MS);

            hasClearedAlwaysRun = true;
          }
        }
      }
    }
  }

  /**
   * Timeout for tooltip messages to the user.
   */
  private static final int TOOLTIP_DISPLAY_TIMEOUT_MS = 5000;

  private final Place currentPlace;
  private final AppContext appContext;
  private final RunButtonTargetPopup targetPopup;
  private final Element dropdownElem;
  private final Tooltip noFileSelectedTooltip;
  private final Tooltip yamlSelectedFileTooltip;
  private final Tooltip targetResetTooltip;
  private final MapFromStringTo<RunConfiguration> runConfigs;

  private PathUtil currentFilePath;

  private RunButtonController(final AppContext appContext,
      Element dropdownElem,
      Place currentPlace,
      FileNameSearch fileNameSearch,
      final RunButtonTargetPopup targetPopup,
      final MapFromStringTo<RunConfiguration> runConfigs,
      FileTreeModel fileTreeModel,
      Tooltip noFileSelectedTooltip,
      Tooltip autoSelectedFileTooltip,
      Tooltip targetResetTooltip) {

    this.appContext = appContext;
    this.dropdownElem = dropdownElem;
    this.currentPlace = currentPlace;
    this.targetPopup = targetPopup;
    this.noFileSelectedTooltip = noFileSelectedTooltip;
    this.yamlSelectedFileTooltip = autoSelectedFileTooltip;
    this.targetResetTooltip = targetResetTooltip;
    this.runConfigs = runConfigs;

    //put in our hardcoded default configs
    if (!runConfigs.hasKey("PREVIEW_CURRENT_FILE"))
    runConfigs.put("PREVIEW_CURRENT_FILE", new RunConfiguration() {
      @Override
      public String getId() {
        return "PREVIEW_CURRENT_FILE";
      }
      @Override
      public String getLabel() {
        return "View On Server";
      }
      @Override
      public void run(AppContext appContext, PathUtil file) {
        launchFile(currentFilePath == null ? null : currentFilePath.getPathString(), "");
      }
      @Override
      public elemental.dom.Element getForm() {
        return null;
      }
    });


    // Setup handler to keep track of current file
    currentPlace.registerSimpleEventHandler(FileOpenedEvent.TYPE, new FileOpenedEvent.Handler() {
      @Override
      public void onFileOpened(boolean isEditable, PathUtil filePath) {
        updateCurrentFile(filePath);
      }
    });

    // Listen to the file Tree
    fileTreeModel.addModelChangeListener(new FileTreeChangeListener());

    this.targetPopup.setAutoHideHandler(new AutoHideHandler() {
      @Override
      public void onHide() {
        toggleDropdownStyle(false);

        // Update the runTarget for this workspace
        // TODO: Consider checking to see if there has actually
        // been a change
        RunTarget runTarget = targetPopup.getRunTarget();
        updateRunTarget(runTarget);
      }

      @Override
      public void onShow() {
        // do nothing
      }
    });
  }

  public void toggleDropdownStyle(boolean active) {
    if (active) {
      dropdownElem.addClassName(appContext.getResources().baseCss().buttonActive());
      dropdownElem.addClassName(appContext.getResources().runButtonTargetPopupCss().stayActive());
    } else {
      dropdownElem.removeClassName(appContext.getResources().baseCss().buttonActive());
      dropdownElem.removeClassName(
          appContext.getResources().runButtonTargetPopupCss().stayActive());
    }
  }

  public void onRunButtonClicked() {
    if (targetPopup.getRunTarget() != null) {
      launchRunTarget(targetPopup.getRunTarget());
    } else {
      launchFile(currentFilePath == null ? null : currentFilePath.getPathString(), "");
    }
  }

  private void launchRunTarget(RunTarget target) {
    RunConfiguration runner = runConfigs.get(target.getRunMode());
    if (runner == null) {
      launchFile(target.getAlwaysRunFilename(), target.getAlwaysRunUrlOrQuery());
    }else {
      runner.run(appContext, currentFilePath);
    }
  }

  /**
   * Launches a file using the appropriate method
   */
  private void launchFile(String file, String urlOrQuery) {
    if (file == null) {
      displayNoFileSelectedTooltip();
      return;
    }

    RunTargetType appType = RunButtonTargetPopup.RunTargetType.parseTargetType(file);
    launchPreview(file, StringUtils.nullToEmpty(urlOrQuery));
  }

  public void onRunButtonDropdownClicked() {
    if (targetPopup.isShowing()) {
      targetPopup.forceHide();
    } else {
      targetPopup.show();
      // only needed on show since hiding is caught by the auto hide handler
      toggleDropdownStyle(true);
    }
  }

  private void updateRunTarget(RunTarget newTarget) {
    UpdateWorkspaceRunTargets update =
        UpdateWorkspaceRunTargetsImpl.make().setRunTarget(newTarget);
    appContext.getFrontendApi().UPDATE_WORKSPACE_RUN_TARGETS.send(update);
    targetPopup.setRunTarget(newTarget);
  }

  /**
   * Launches a file given the base file and a query string.
   */
  public void launchPreview(String baseUrl, String query) {
    StringBuilder builder = new StringBuilder(ResourceUriUtils.getAbsoluteResourceBaseUri());
    builder.append(StringUtils.ensureStartsWith(baseUrl, "/"));

    if (!StringUtils.isNullOrEmpty(query)) {
      builder.append(StringUtils.ensureStartsWith(query, "?"));
    }
    currentPlace.fireEvent(new RunApplicationEvent(builder.toString()));
  }

  private void updateCurrentFile(PathUtil filePath) {
    currentFilePath = filePath;
    targetPopup.updateCurrentFile(filePath == null ? null : filePath.getPathString());
  }

  private void displayNoFileSelectedTooltip() {
    noFileSelectedTooltip.show(TOOLTIP_DISPLAY_TIMEOUT_MS);
  }
}
