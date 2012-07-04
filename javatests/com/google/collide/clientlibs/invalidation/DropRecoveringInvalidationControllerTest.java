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

package com.google.collide.clientlibs.invalidation;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;

import com.google.collide.clientlibs.invalidation.InvalidationManager.Recoverer;
import com.google.collide.clientlibs.invalidation.InvalidationManager.Recoverer.Callback;
import com.google.collide.clientlibs.invalidation.InvalidationRegistrar.Listener;
import com.google.collide.clientlibs.invalidation.InvalidationRegistrar.Listener.AsyncProcessingHandle;
import com.google.collide.dto.RecoverFromDroppedTangoInvalidationResponse.RecoveredPayload;
import com.google.collide.json.shared.JsonArray;
import com.google.collide.shared.invalidations.InvalidationObjectId;
import com.google.collide.shared.invalidations.InvalidationObjectId.VersioningRequirement;
import com.google.collide.shared.invalidations.InvalidationUtils.InvalidationObjectPrefix;
import com.google.collide.shared.util.JsonCollections;
import com.google.collide.shared.util.MockTimer;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;

import java.util.List;

/**
 * Tests for {@link DropRecoveringInvalidationController}.
 */
public class DropRecoveringInvalidationControllerTest extends TestCase {
  
  private static class StubRecoveredPayload implements RecoveredPayload {
    private final long version;
    private final String payload;

    public StubRecoveredPayload(long version, String payload) {
      this.version = version;
      this.payload = payload;
    }

    @Override
    public String getPayload() {
      return payload;
    }

    @Override
    public int getPayloadVersion() {
      return (int) version;
    }
  }

  private static final InvalidationObjectId<?> obj = new InvalidationObjectId<Void>(
      InvalidationObjectPrefix.FILE_TREE_MUTATION, "12345", VersioningRequirement.PAYLOADS);
  
  /**
   * The index corresponds to the version. Since there was never an invalidation to get to version
   * 0, all invalidations will start at 1 for consistency.
   */
  private static final List<String> PAYLOADS = Lists.newArrayList("a", "b", "c", "d", "e", "f", "g");

  private IMocksControl strictControl;
  private Listener listener;
  private Recoverer recoverer;
  private MockTimer.Factory timerFactory;
  private DropRecoveringInvalidationControllerFactory controllerFactory;

  public void testNormal() {
    makeListenerExpectInvalidations(1, PAYLOADS.size());
    strictControl.replay();

    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);
    controller.setNextExpectedVersion(1);

    for (int i = 1; i < PAYLOADS.size(); i++) {
      controller.handleInvalidated(PAYLOADS.get(i), i, false);
    }
    
    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }
  
  public void testNullPayloadNotCountedAsDropped() {
    listener.onInvalidated(
        eq(obj.getName()), eq(1L), eq((String) null), anyObject(AsyncProcessingHandle.class));
    strictControl.replay();

    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);
    controller.setNextExpectedVersion(1);

   controller.handleInvalidated(null, 1, true);

    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }

  /**
   * Test the object-bootstrap sequence: we subscribe to Tango and then do an XHR to fetch the full
   * version of the object. Before we get the XHR response, we could have gotten invalidations.
   */
  public void testBootstrapInOrder() {
    strictControl.replay();

    // Client instantiated/registered Tango listener and performs XHR
    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);

    // While XHR is on the wire, we get a Tango invalidation
    triggerInvalidated(controller, 1, 2, false);

    // Ensure those invalidations didn't reach the listener
    strictControl.verify();
    strictControl.reset();
    
    makeListenerExpectInvalidations(2, PAYLOADS.size());
    strictControl.replay();
    
    // We got XHR response, its payload start at version 2
    controller.setNextExpectedVersion(2);

    for (int i = 2; i < PAYLOADS.size(); i++) {
      controller.handleInvalidated(PAYLOADS.get(i), i, false);
    }

    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }

  /**
   * Like {@link #testBootstrapInOrder()} except the there is overlap between the received payloads
   * before we got the XHR response and the payloads in the XHR response.
   */
  public void testBootstrapOverlap() {
    strictControl.replay();

    // Client instantiated/registered Tango listener and performs XHR
    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);

    // While XHR is on the wire, we get a Tango invalidation
    triggerInvalidated(controller, 1, 2, false);

    // Ensure those invalidations didn't reach the listener
    strictControl.verify();
    strictControl.reset();
    
    makeListenerExpectInvalidations(1, PAYLOADS.size());
    strictControl.replay();
    
    // We got XHR response, its payloads start at version 0
    controller.setNextExpectedVersion(1);

    for (int i = 2; i < PAYLOADS.size(); i++) {
      controller.handleInvalidated(PAYLOADS.get(i), i, false);
    }

    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }
  
  public void testSquelchedImmediatelyAfterBootstrap() {
    makeRecovererExpectRecover(1, 3);
    makeListenerExpectInvalidations(1, PAYLOADS.size());
    strictControl.replay();
    
    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);
    controller.setNextExpectedVersion(1);
    
    // We were expecting v1 but got v2, recovery should kick off and get through v3 (defined above)
    triggerInvalidated(controller, 2, PAYLOADS.size(), false);
    
    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }


  public void testSquelchedDuringOperation() {
    makeListenerExpectInvalidations(1, 3);
    makeRecovererExpectRecover(3, 3);
    makeListenerExpectInvalidations(3, PAYLOADS.size());
    strictControl.replay();
    
    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);
    controller.setNextExpectedVersion(1);
    
    // Invalidate v1 and v2, then v4 - end
    triggerInvalidated(controller, 1, 3, false);
    triggerInvalidated(controller, 4, PAYLOADS.size(), false);
    
    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }

  public void testDroppedPayloadsImmediatelyAfterBootstrap() {
    makeRecovererExpectRecover(1, 1);
    makeListenerExpectInvalidations(1, 2);
    makeRecovererExpectRecover(2, 1);
    makeListenerExpectInvalidations(2, PAYLOADS.size());
    strictControl.replay();
    
    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);
    controller.setNextExpectedVersion(1);
    
    triggerInvalidated(controller, 1, 3, true);
    triggerInvalidated(controller, 3, PAYLOADS.size(), false);
    
    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }

  public void testDroppedPayloadsDuringOperation() {
    makeListenerExpectInvalidations(1, 4);
    makeRecovererExpectRecover(4, 1);
    makeListenerExpectInvalidations(4, 5);
    makeRecovererExpectRecover(5, 1);
    makeListenerExpectInvalidations(5, PAYLOADS.size());
    strictControl.replay();
    
    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);
    controller.setNextExpectedVersion(1);
    
    triggerInvalidated(controller, 1, 4, false);
    triggerInvalidated(controller, 4, 6, true);
    triggerInvalidated(controller, 6, PAYLOADS.size(), false);
    
    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }

  public void testOutageOfAllDroppedPayloads() {
    for (int i = 1; i < PAYLOADS.size(); i++) {
      makeRecovererExpectRecover(i, 1);
      makeListenerExpectInvalidations(i, i + 1);
    }
    strictControl.replay();
    
    DropRecoveringInvalidationController controller =
        controllerFactory.create(obj, listener, recoverer);
    controller.setNextExpectedVersion(1);
    
    triggerInvalidated(controller, 1, PAYLOADS.size(), true);
    
    timerFactory.tickToFireAllTimers();
    strictControl.verify();
  }
  
  @Override
  protected void setUp() throws Exception {
    strictControl = EasyMock.createStrictControl();
    recoverer = strictControl.createMock(Recoverer.class);
    listener = strictControl.createMock(Listener.class);
    
    timerFactory = new MockTimer.Factory();
    controllerFactory = new DropRecoveringInvalidationControllerFactory(
        new InvalidationLogger(false, false), timerFactory);
  }

  /**
   * @param end exclusive  
   */
  private void makeListenerExpectInvalidations(int begin, int end) {
    for (int i = begin; i < end; i++) {
      listener.onInvalidated(eq(obj.getName()), eq((long) i), eq(PAYLOADS.get(i)),
          anyObject(AsyncProcessingHandle.class));
    }
  }
  
  /**
   * @param end exclusive
   * @param dropPayloads TODO:
   */
  private void triggerInvalidated(
      DropRecoveringInvalidationController controller, int begin, int end, boolean dropPayloads) {
    for (int i = begin; i < end; i++) {
      controller.handleInvalidated(dropPayloads ? null : PAYLOADS.get(i), i, !dropPayloads);
    }
  }
  
  private void makeRecovererExpectRecover(final int nextExpectedVersion, final int payloadCount) {
    recoverer.recoverPayloads(
        EasyMock.eq(obj), EasyMock.eq(nextExpectedVersion - 1), EasyMock.anyObject(Callback.class));
    EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
      @Override
      public Void answer() throws Throwable {
        Callback callback = (Callback) EasyMock.getCurrentArguments()[2];

        JsonArray<RecoveredPayload> recoveredPayloads = JsonCollections.createArray();
        for (int i = nextExpectedVersion; i < nextExpectedVersion + payloadCount; i++) {
          recoveredPayloads.add(new StubRecoveredPayload(i, PAYLOADS.get(i)));
        }
        
        callback.onPayloadsRecovered(recoveredPayloads, nextExpectedVersion);
        return null;
      }
    });
  }
}
