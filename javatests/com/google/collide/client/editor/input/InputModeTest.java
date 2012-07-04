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

package com.google.collide.client.editor.input;

import com.google.collide.client.util.input.KeyCodeMap;
import com.google.collide.client.util.input.ModifierKeys;
import com.google.gwt.junit.client.GWTTestCase;

import elemental.events.KeyboardEvent.KeyCode;
import org.waveprotocol.wave.client.common.util.SignalEvent;

/**
 * Test cases for input handling, switching between input modes+schemes, and
 * stream+event shortcut callbacks to keyboard events.
 * 
 *
 */
public class InputModeTest extends GWTTestCase {
  
  private TestScheme inputScheme;

  class TestScheme extends InputScheme {
    private int flag = 0;
    private int flagVal = 1;
    
    public int getFlag() {
      return flag;
    }
    
    /**
     * The value to set flag to when setFlag() is called
     */
    public void initFlag(int val, int i) {
      flagVal = i;
      flag = val;
    }
    
    public void initFlag(int i) {
      flagVal = i;
    }
    
    // should only be called by test subclasses
    protected void setFlag() {
      flag = flagVal;
    }
    
    protected void setFlag(int i) {
      flag = i;
    }
    
    public void setup() {}
    public void teardown() {}
  }
  
  /**
   * Used to record internal function calls
   */
  public abstract class TestMode extends InputMode {
    private int flag = 0;
    private int flagVal = 1;
    
    public int getFlag() {
      return flag;
    }
    
    /**
     * The value to set flag to when setFlag() is called
     */
    public void initFlag(int val, int i) {
      flagVal = i;
      flag = val;
    }
    
    public void initFlag(int i) {
      flagVal = i;
    }
    
    // should only be called by test subclasses
    protected void setFlag() {
      flag = flagVal;
    }
    
    protected void setFlag(int i) {
      flag = i;
    }
  }
  
  /**
   * Test mode 1 to install into TestScheme
   * 
   * Sets internal flag to the signal's keycode and returns true for defaultInput
   */
  public class TestMode1 extends TestMode {
    public void setup() {
      setFlag();
    }
    
    public void teardown() {
      setFlag();
    }
    
    public boolean onDefaultInput(SignalEvent e, char text) {
      setFlag(e.getKeyCode());
      return true;
     }
    
    public boolean onDefaultPaste(SignalEvent e, String text) {
      return false;
    }
  }

  /**
   * Test mode 2 to install into TestScheme
   * 
   * Returns false on defaultInput
   */
  public class TestMode2 extends TestMode {
    public void setup() {
      setFlag();
    }
    
    public void teardown() {
      setFlag();
    }
    
    public boolean onDefaultInput(SignalEvent e, char text) {
      return false;
    }
    
    public boolean onDefaultPaste(SignalEvent e, String text) {
      return false;
    }
  }
  
  /**
   * Test mode 3 to install into TestScheme
   * 
   * defaultInput sets flag equal to -1 and returns true
   * defaultInputMulti sets flag equal to length of event string and returns false
   */
  public class TestMode3 extends TestMode {
    public void setup() {
      setFlag();
    }
    
    public void teardown() {
      setFlag();
    }
    
    public boolean onDefaultInput(SignalEvent e, char text) {
      setFlag(-1);
      return true;
    }
    
    public boolean onDefaultPaste(SignalEvent e, String text) {
      setFlag(text.length());
      return false;
    }
  }
  
  /* (non-Javadoc)
   * @see com.google.gwt.junit.client.GWTTestCase#getModuleName()
   */
  @Override
  public String getModuleName() {
    return "com.google.collide.client.TestCode";
  }
  
  /**
   * Setup client environment
   */
  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();

    inputScheme = new TestScheme();
    
  }
  
  /**
   * Tests a scheme with no modes installed
   */
  public void testNoModes() {
    SignalEvent sig = new TestSignalEvent('a'); // user typed an "a" character
    assertFalse(sendSignal(sig)); // returns false when no modes are installed
  }
  
  /**
   * Test mode switching
   */
  public void testModeSwitch() {
    TestMode mode1 = new TestMode1();
    mode1.initFlag(2);
    inputScheme.addMode(1, mode1);
    assertEquals(inputScheme.getMode(), null);
    
    inputScheme.switchMode(1);
    // mode1.setup() should have been called
    assertEquals(mode1.getFlag(), 2);
    
    TestMode mode2 = new TestMode2();
    mode1.initFlag(3);
    mode2.initFlag(4);
    inputScheme.addMode(2, mode2);
    // should still be in mode1
    assertEquals(inputScheme.getMode(), mode1);
    
    inputScheme.switchMode(2);
    // see if mode1.teardown() and mode2.setup() were called
    assertEquals(inputScheme.getMode(), mode2);
    assertEquals(mode1.getFlag(), 3);
    assertEquals(mode2.getFlag(), 4);
  }
  
  /**
   * Test signal handling
   */
  public void testSignalHandle() {
    TestMode mode1 = new TestMode1();
    inputScheme.addMode(1, mode1);
    inputScheme.switchMode(1);
    TestSignalEvent sig = new TestSignalEvent('a');
    assertTrue(sendSignal(sig)); // mode1.defaultInput() should return true
    assertEquals(mode1.getFlag(), 'a'); // flag should be set to the keyCode
  }
  
  /**
   * Test shortcuts
   * 
   * All TestSignalEvents should be intialized with uppercase letters or constants
   * from KeyCode.* to simulate keyboard input
   */
  public void testShortcuts() {
    TestMode mode2 = new TestMode2();

    inputScheme.addMode(2, mode2);
    inputScheme.switchMode(2);
    // CMD+ALT+s
    mode2.addShortcut(new EventShortcut(ModifierKeys.ACTION | ModifierKeys.ALT, 's') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          // callback in here
          ((TestScheme) scheme).setFlag();
          return true;
        }
      }
    );
    
    // SHIFT+TAB
    mode2.addShortcut(new EventShortcut(ModifierKeys.SHIFT, KeyCodeMap.TAB) {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          // callback in here
          ((TestScheme) scheme).setFlag();
          return true;
        }
      }
    );
    
    // CMD+\
    mode2.addShortcut(new EventShortcut(ModifierKeys.ACTION, '\\') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          // callback in here
          ((TestScheme) scheme).setFlag();
          return true;
        }
      }
    );
    
    // ;
    mode2.addShortcut(new EventShortcut(ModifierKeys.NONE, ';') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          // callback in here
          ((TestScheme) scheme).setFlag();
          return true;
        }
      }
    );
    
    // .
    mode2.addShortcut(new EventShortcut(ModifierKeys.ALT, '.') {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          // callback in here
          ((TestScheme) scheme).setFlag();
          return true;
        }
      }
    );
    
    // PAGE_UP
    mode2.addShortcut(new EventShortcut(ModifierKeys.NONE, KeyCodeMap.PAGE_UP) {
        @Override
        public boolean event(InputScheme scheme, SignalEvent event) {
          // callback in here
          ((TestScheme) scheme).setFlag();
          return true;
        }
      }
    );
    
    // feed in javascript keycode representations
    TestSignalEvent evt1 = new TestSignalEvent('A');
    TestSignalEvent evt2 = new TestSignalEvent('A', ModifierKeys.ACTION, ModifierKeys.ALT);
    TestSignalEvent evt3 = new TestSignalEvent('S');
    TestSignalEvent evt4 = new TestSignalEvent('S', ModifierKeys.ACTION, ModifierKeys.ALT);
    TestSignalEvent evt5 = new TestSignalEvent(KeyCode.TAB, ModifierKeys.SHIFT);
    TestSignalEvent evt6 = new TestSignalEvent(KeyCode.BACKSLASH, ModifierKeys.ACTION);
    TestSignalEvent evt7 = new TestSignalEvent(KeyCode.SEMICOLON);
    TestSignalEvent evt8 = new TestSignalEvent(KeyCode.PERIOD, ModifierKeys.ALT);
    TestSignalEvent evt9 = new TestSignalEvent(KeyCode.PAGE_UP);
    
    // evt1..3 shouldn't fire the callback, only evt4..6 should
    inputScheme.initFlag(0, 1);
    assertFalse(sendSignal(evt1));
    assertEquals(inputScheme.getFlag(), 0);
    assertFalse(sendSignal(evt2));
    assertEquals(inputScheme.getFlag(), 0);
    assertFalse(sendSignal(evt3));
    assertEquals(inputScheme.getFlag(), 0);
    assertTrue(sendSignal(evt4));
    assertEquals(inputScheme.getFlag(), 1);
    inputScheme.initFlag(0, 2);
    assertTrue(sendSignal(evt5));
    assertEquals(inputScheme.getFlag(), 2);
    inputScheme.initFlag(0, 3);
    assertTrue(sendSignal(evt6));
    assertEquals(inputScheme.getFlag(), 3);
    inputScheme.initFlag(0, 4);
    assertTrue(sendSignal(evt7));
    assertEquals(inputScheme.getFlag(), 4);
    inputScheme.initFlag(0, 5);
    assertTrue(sendSignal(evt8));
    assertEquals(inputScheme.getFlag(), 5);
    inputScheme.initFlag(0, 6);
    assertTrue(sendSignal(evt9));
    assertEquals(inputScheme.getFlag(), 6);
    
    // now try out StreamShortcuts, such as vi quit: ":q!"
    
    mode2.addShortcut(new StreamShortcut(":q!") {
      @Override
      public boolean event(InputScheme scheme, SignalEvent event) {
        // callback in here
        ((TestScheme) scheme).setFlag();
        return true;
      }
    });
    
    TestSignalEvent x = new TestSignalEvent('X');
    TestSignalEvent excl = new TestSignalEvent('1', ModifierKeys.SHIFT); // !
    TestSignalEvent q = new TestSignalEvent('Q');
    TestSignalEvent upperQ = new TestSignalEvent('Q', ModifierKeys.SHIFT);
    TestSignalEvent colon = new TestSignalEvent(KeyCode.SEMICOLON, null, ModifierKeys.SHIFT);

    inputScheme.initFlag(0, 2);
    assertFalse(sendSignal(x)); // random entry
    assertEquals(inputScheme.getFlag(), 0);
    assertTrue(sendSignal(colon)); // beginning of command
    assertEquals(inputScheme.getFlag(), 0);
    assertFalse(sendSignal(upperQ)); // test uppercase vs lowercase
    assertEquals(inputScheme.getFlag(), 0);
    assertFalse(sendSignal(excl));
    assertEquals(inputScheme.getFlag(), 0);
    
    // do the combo :q!
    assertTrue(sendSignal(colon));
    assertTrue(sendSignal(q));
    assertTrue(sendSignal(excl));
    assertEquals(inputScheme.getFlag(), 2); // triggers callback
    assertFalse(sendSignal(x)); // should reset
  }
  
  /**
   * Test defaultMultiInput for longer strings
   * 
   * All strings > 1 char should skip shortcut system entirely
   */
  /*public void testMultiInput() {
    TestMode mode3 = new TestMode3();

    inputScheme.addMode(3, mode3);
    inputScheme.switchMode(3);
    
    TestSignalEvent paste = new TestSignalEvent('p', ModifierKeys.CMD);
    String text = "Test multi char paste";
    assertFalse(sendStringEvent(paste, text));
    assertEquals(inputScheme.getFlag(), text.length());
    String oneletter = "A";
    assertTrue(sendStringEvent(paste, oneletter));
    assertEquals(inputScheme.getFlag(), -1);
  }*/
  
  /**
   * Sends the signal to the current installed InputScheme
   */
  private boolean sendSignal(SignalEvent sig) {
    return inputScheme.handleEvent(sig, "");
  }
  
  private boolean sendStringEvent(SignalEvent sig, String text) {
    return inputScheme.handleEvent(sig, text);
  }
}
