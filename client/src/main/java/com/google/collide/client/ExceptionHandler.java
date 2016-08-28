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

package com.google.collide.client;

import com.google.collide.client.communication.FrontendApi;
import com.google.collide.client.communication.FrontendApi.ApiCallback;
import com.google.collide.client.communication.MessageFilter;
import com.google.collide.client.communication.MessageFilter.MessageRecipient;
import com.google.collide.client.history.HistoryUtils;
import com.google.collide.client.history.HistoryUtils.SetHistoryListener;
import com.google.collide.client.history.HistoryUtils.ValueChangeListener;
import com.google.collide.client.status.StatusManager;
import com.google.collide.client.status.StatusMessage;
import com.google.collide.client.status.StatusMessage.MessageType;
import com.google.collide.client.util.ExceptionUtils;
import com.google.collide.client.util.logging.Log;
import com.google.collide.dto.LogFatalRecordResponse;
import com.google.collide.dto.RoutingTypes;
import com.google.collide.dto.ServerError;
import com.google.collide.dto.ServerError.FailureReason;
import com.google.collide.dto.StackTraceElementDto;
import com.google.collide.dto.ThrowableDto;
import com.google.collide.dto.client.DtoClientImpls.LogFatalRecordImpl;
import com.google.collide.dto.client.DtoClientImpls.StackTraceElementDtoImpl;
import com.google.collide.dto.client.DtoClientImpls.ThrowableDtoImpl;
import com.google.collide.json.client.JsoArray;
import com.google.collide.shared.util.StringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;

import elemental.client.Browser;

/**
 * The global {@link UncaughtExceptionHandler} for Collide. In addition to
 * catching uncaught client exceptions, this handler is also responsible for
 * notifying the user of server to client errors.
 */
public class ExceptionHandler implements UncaughtExceptionHandler {

  private static final String FATAL_MESSAGE =
      "Hoist with our own petard. Something broke! We promise that if you reload Collide"
          + " all will be set right :).";

  /**
   * Record recent history change events (up to some finite maximum) to assist
   * failure forensics.
   *
   */
  private static class HistoryListener implements SetHistoryListener, ValueChangeListener {
    private static final int MAX_HISTORY_ENTRIES = 10;
    private final JsoArray<String> historyBuffer = JsoArray.create();

    private void addHistoryString(String historyString) {
      historyBuffer.add(historyString);
      if (historyBuffer.size() > MAX_HISTORY_ENTRIES) {
        historyBuffer.remove(0);
      }
    }

    @Override
    public void onHistorySet(String historyString) {
      addHistoryString(historyString);
    }

    @Override
    public void onValueChanged(String historyString) {
      addHistoryString(historyString);
    }

    /**
     * Retrieve any stored history entries in descending chronological order
     * (most recent first).
     */
    public JsoArray<String> getRecentHistory() {
      JsoArray<String> ret = historyBuffer.copy();
      ret.reverse();
      return ret;
    }
  }

  private final HistoryListener historyListener;
  private final MessageRecipient<ServerError> serverErrorReceiver =
      new MessageRecipient<ServerError>() {
        private StatusMessage serverError = null;

        @Override
        public void onMessageReceived(ServerError message) {
          Log.error(getClass(), "Server Error #" + message.getFailureReason() + ": "
              + message.getDetails());
          if (serverError != null) {
            serverError.cancel();
          }

          // Authorization errors are handled within the app.
          if (message.getFailureReason() != FailureReason.UNAUTHORIZED) {
            serverError =
                new StatusMessage(statusManager, MessageType.ERROR,
                    "The server encountered an error (#" + message.getFailureReason() + ")");
            serverError.setDismissable(true);
            serverError.addAction(StatusMessage.RELOAD_ACTION);
            serverError.fire();
          }
        }
      };
  private final FrontendApi frontendApi;
  private final StatusManager statusManager;

  public ExceptionHandler(
      MessageFilter messageFilter, FrontendApi frontendApi, StatusManager statusManager) {
    this.frontendApi = frontendApi;
    this.statusManager = statusManager;
    messageFilter.registerMessageRecipient(RoutingTypes.SERVERERROR, serverErrorReceiver);
    this.historyListener = new HistoryListener();
    HistoryUtils.addSetHistoryListener(historyListener);
    HistoryUtils.addValueChangeListener(historyListener);
  }

  @Override
  public void onUncaughtException(Throwable e) {
    Log.error(getClass(), e.toString(), e);
    final Throwable exception = e;
    final JsoArray<String> recentHistory = historyListener.getRecentHistory();
    final String currentWindowLocation = Browser.getWindow().getLocation().getHref();

    ThrowableDtoImpl throwableDto = getThrowableAsDto(e);
    LogFatalRecordImpl logRecord = LogFatalRecordImpl
        .make()
        .setMessage("Client exception at: " + Browser.getWindow().getLocation().getHref())
        .setThrowable(throwableDto)
        .setRecentHistory(recentHistory)
        .setPermutationStrongName(GWT.getPermutationStrongName());

    frontendApi.LOG_REMOTE.send(logRecord, new ApiCallback<LogFatalRecordResponse>() {

      @Override
      public void onMessageReceived(final LogFatalRecordResponse message) {
        StatusMessage msg = new StatusMessage(statusManager, MessageType.FATAL, FATAL_MESSAGE);

        msg.addAction(StatusMessage.FEEDBACK_ACTION);
        msg.addAction(StatusMessage.RELOAD_ACTION);

        String stackTrace;
        if (!StringUtils.isNullOrEmpty(message.getStackTrace())) {
          stackTrace = message.getStackTrace();
        } else {
          stackTrace = ExceptionUtils.getStackTraceAsString(exception);
        }

        msg.setLongText(calculateLongText(stackTrace));
        msg.fire();
      }

      private String calculateLongText(String stackTrace) {
        return "Client exception at " + currentWindowLocation + "\n\nRecent history:\n\t"
            + recentHistory.join("\n\t") + "\n\n" + stackTrace;
      }

      @Override
      public void onFail(FailureReason reason) {
        StatusMessage msg = new StatusMessage(statusManager, MessageType.FATAL, FATAL_MESSAGE);
        msg.addAction(StatusMessage.FEEDBACK_ACTION);
        msg.addAction(StatusMessage.RELOAD_ACTION);
        msg.setLongText(calculateLongText(ExceptionUtils.getStackTraceAsString(exception)));
        msg.fire();
      }
    });
  }

  /**
   * Serialize a {@link Throwable} as a {@link ThrowableDto}.
   */
  private static ThrowableDtoImpl getThrowableAsDto(Throwable e) {
    ThrowableDtoImpl ret = ThrowableDtoImpl.make();
    ThrowableDtoImpl currentDto = ret;
    Throwable currentCause = e;

    for (int causeCounter = 0; causeCounter < ExceptionUtils.MAX_CAUSE && currentCause != null;
        causeCounter++) {
      currentDto.setClassName(currentCause.getClass().getName());
      currentDto.setMessage(currentCause.getMessage());

      JsoArray<StackTraceElementDto> currentStackTrace = JsoArray.create();
      StackTraceElement[] stackElems = currentCause.getStackTrace();
      if (stackElems != null) {
        for (int i = 0; i < stackElems.length; ++i) {
          StackTraceElement stackElem = stackElems[i];
          currentStackTrace.add(StackTraceElementDtoImpl
              .make()
              .setClassName(stackElem.getClassName())
              .setFileName(stackElem.getFileName())
              .setMethodName(stackElem.getMethodName())
              .setLineNumber(stackElem.getLineNumber()));
        }
        currentDto.setStackTrace(currentStackTrace);
      }

      currentCause = currentCause.getCause();
      if (currentCause != null) {
        ThrowableDtoImpl nextDto = ThrowableDtoImpl.make();
        currentDto.setCause(nextDto);
        currentDto = nextDto;
      }
    }

    return ret;
  }

}
