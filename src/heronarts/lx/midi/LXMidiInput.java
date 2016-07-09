/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.midi;

import heronarts.lx.LX;
import heronarts.lx.parameter.BooleanParameter;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

public class LXMidiInput {

  protected final Transmitter transmitter;
  private final String name;

  private final LXMidiEngine midiEngine;

  private final List<LXMidiListener> listeners = new ArrayList<LXMidiListener>();

  private boolean isEngineInput = false;

  public final BooleanParameter enabled = new BooleanParameter("Midi Input Enabled", true);

  public LXMidiInput(LX lx, MidiDevice device) throws MidiUnavailableException {
    this(lx.engine.midiEngine, device);
  }

  public LXMidiInput(LX lx, Transmitter transmitter, String name) {
    this(lx.engine.midiEngine, transmitter, name);
  }

  public LXMidiInput(LXMidiEngine midiEngine, MidiDevice device)
      throws MidiUnavailableException {
    this(midiEngine, device, null);
  }

  public LXMidiInput(LXMidiEngine midiEngine, MidiDevice device, LXMidiListener listener) throws MidiUnavailableException {
    this.midiEngine = midiEngine;
    this.transmitter = device.getTransmitter();
    this.name = device.getDeviceInfo().getName();
    if (listener != null) {
      addListener(listener);
    }
    device.open();
    transmitter.setReceiver(new Receiver());
  }

  public LXMidiInput(LXMidiEngine midiEngine, Transmitter transmitter, String name) {
    this(midiEngine, transmitter, name, null);
  }

  public LXMidiInput(LXMidiEngine midiEngine, Transmitter transmitter, String name, LXMidiListener listener) {
    this.midiEngine = midiEngine;
    this.transmitter = transmitter;
    this.name = name;
    if (listener != null) {
      addListener(listener);
    }
    transmitter.setReceiver(new Receiver());
  }

  public String getName() {
    return this.name;
  }

  public LXMidiInput addListener(LXMidiListener listener) {
    this.listeners.add(listener);
    return this;
  }

  public LXMidiInput removeListener(LXMidiListener listener) {
    this.listeners.remove(listener);
    return this;
  }

  LXMidiInput setEngineInput(boolean isEngineInput) {
    this.isEngineInput = isEngineInput;
    return this;
  }

  /**
   * This receiver is called by a MIDI thread, it just puts messages
   * into a queue that can then be called by the engine thread.
   */
  private class Receiver implements javax.sound.midi.Receiver {
    @Override
    public void close() {
      listeners.clear();
    }

    @Override
    public void send(MidiMessage midiMessage, long timeStamp) {
      if (midiMessage instanceof ShortMessage) {
        ShortMessage sm = (ShortMessage) midiMessage;
        LXShortMessage message = null;
        switch (sm.getCommand()) {
        case ShortMessage.NOTE_ON:
          message = new LXMidiNoteOn(sm);
          break;
        case ShortMessage.NOTE_OFF:
          message = new LXMidiNoteOff(sm);
          break;
        case ShortMessage.CONTROL_CHANGE:
          message = new LXMidiControlChange(sm);
          break;
        case ShortMessage.PROGRAM_CHANGE:
          message = new LXMidiProgramChange(sm);
          break;
        case ShortMessage.PITCH_BEND:
          message = new LXMidiPitchBend(sm);
          break;
        case ShortMessage.CHANNEL_PRESSURE:
          message = new LXMidiAftertouch(sm);
          break;
        }
        if (message != null) {
          midiEngine.queueMessage(message.setInput(LXMidiInput.this));
        }
      }
    }
  }

  /**
   * This method is invoked on the main thread to process the MIDI message.
   *
   * @param message Midi message
   */
  public void dispatch(LXShortMessage message) {
    if (!enabled.isOn()) {
      return;
    }
    for (LXMidiListener listener : this.listeners) {
      this.midiEngine.dispatch(message, listener);
    }
    if (this.isEngineInput) {
      this.midiEngine.dispatch(message);
    }
  }
}
