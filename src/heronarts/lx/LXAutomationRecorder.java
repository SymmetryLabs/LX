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

package heronarts.lx;

import heronarts.lx.midi.LXMidiAftertouch;
import heronarts.lx.midi.LXMidiControlChange;
import heronarts.lx.midi.LXMidiListener;
import heronarts.lx.midi.LXMidiNote;
import heronarts.lx.midi.LXMidiNoteOn;
import heronarts.lx.midi.LXMidiPitchBend;
import heronarts.lx.midi.LXMidiProgramChange;
import heronarts.lx.midi.LXShortMessage;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableParameter;
import heronarts.lx.parameter.LXParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * An automation recorder contains meta-data about all the controls on the
 * patterns and effects in the system, which can be recorded or played back.
 */
public class LXAutomationRecorder extends LXRunnable implements LXEngine.MessageListener, LXMidiListener {

  private final static String EVENT_PATTERN = "PATTERN";
  private final static String EVENT_PARAMETER = "PARAMETER";
  private final static String EVENT_MESSAGE = "MESSAGE";
  private final static String EVENT_MIDI = "MIDI";
  private final static String EVENT_FINISH = "FINISH";

  private final static String KEY_EVENT = "event";
  private final static String KEY_MILLIS = "millis";
  private final static String KEY_CHANNEL = "channel";
  private final static String KEY_PATTERN = "pattern";
  private final static String KEY_PARAMETER = "parameter";
  private final static String KEY_VALUE = "value";
  private final static String KEY_COMMAND = "command";
  private final static String KEY_DATA_1 = "data1";
  private final static String KEY_DATA_2 = "data2";
  private final static String KEY_MESSAGE = "message";

  private final LXEngine engine;

  /**
   * Whether the recorder is armed. If true, then playback records new
   * events.
   */
  public final BooleanParameter armRecord = new BooleanParameter("ARM", false);

  /**
   * Whether the automation should loop upon completion.
   */
  public final BooleanParameter looping = new BooleanParameter("LOOP", false);

  private final List<LXChannel> channels = new ArrayList<LXChannel>();

  private final List<LXAutomationEvent> events = new ArrayList<LXAutomationEvent>();

  private final Map<String, LXParameter> pathToParameter = new HashMap<String, LXParameter>();

  private final Map<LXParameter, String> parameterToPath = new HashMap<LXParameter, String>();

  private int cursor = 0;

  private double elapsedMillis = 0;

  private abstract class LXAutomationEvent {

    private final String eventType;
    private double millis;

    private LXAutomationEvent(String eventType) {
      this.eventType = eventType;
      this.millis = elapsedMillis;
    }

    public final JsonObject toJson() {
      JsonObject jsonObj = new JsonObject();
      jsonObj.addProperty(KEY_EVENT, this.eventType);
      jsonObj.addProperty(KEY_MILLIS, (float) this.millis);
      toJson(jsonObj);
      return jsonObj;
    }

    protected abstract void toJson(JsonObject jsonObj);

    abstract void play();
  }

  private class PatternAutomationEvent extends LXAutomationEvent {

    final LXChannel channel;
    final LXPattern pattern;

    private PatternAutomationEvent(LXChannel channel, LXPattern pattern) {
      super(EVENT_PATTERN);
      this.channel = channel;
      this.pattern = pattern;
    }

    @Override
    void play() {
      this.channel.goPattern(this.pattern);
    }

    @Override
    protected void toJson(JsonObject jsonObj) {
      jsonObj.addProperty(KEY_CHANNEL, this.channel.getIndex());
      jsonObj.addProperty(KEY_PATTERN, this.pattern.getClass().getName());
    }
  }

  private class ParameterAutomationEvent extends LXAutomationEvent {

    final LXParameter parameter;
    final double value;

    private ParameterAutomationEvent(LXParameter parameter) {
      this(parameter, parameter.getValue());
    }

    private ParameterAutomationEvent(LXParameter parameter, double value) {
      super(EVENT_PARAMETER);
      this.parameter = parameter;
      this.value = value;
    }

    @Override
    void play() {
      this.parameter.setValue(this.value);
    }

    @Override
    protected void toJson(JsonObject jsonObj) {
      jsonObj.addProperty(KEY_PARAMETER, parameterToPath.get(this.parameter));
      jsonObj.addProperty(KEY_VALUE, (float) this.value);
    }
  }

  private class MidiAutomationEvent extends LXAutomationEvent {

    private final LXShortMessage shortMessage;

    private MidiAutomationEvent(LXShortMessage shortMessage) {
      super(EVENT_MIDI);
      this.shortMessage = shortMessage;
    }

    @Override
    protected void toJson(JsonObject jsonObj) {
      jsonObj.addProperty(KEY_CHANNEL, this.shortMessage.getChannel());
      jsonObj.addProperty(KEY_COMMAND, this.shortMessage.getCommand());
      jsonObj.addProperty(KEY_DATA_1, this.shortMessage.getData1());
      jsonObj.addProperty(KEY_DATA_2, this.shortMessage.getData2());
    }

    @Override
    void play() {
      engine.midi.dispatch(this.shortMessage);
    }
  }

  private class MessageAutomationEvent extends LXAutomationEvent {

    private final String message;

    private MessageAutomationEvent(String message) {
      super(EVENT_MESSAGE);
      this.message = message;
    }

    @Override
    protected void toJson(JsonObject jsonObj) {
      jsonObj.addProperty(KEY_MESSAGE, this.message);
    }

    @Override
    void play() {
      engine.broadcastMessage(this.message);
    }
  }

  private class FinishAutomationEvent extends LXAutomationEvent {

    private FinishAutomationEvent() {
      super(EVENT_FINISH);
    }

    @Override
    void play() {
      if (looping.isOn()) {
        elapsedMillis = 0;
        cursor = 0;
      } else {
        reset();
      }
    }

    @Override
    protected void toJson(JsonObject jsonObj) {
      // Nothing extra needed
    }
  }

  public LXAutomationRecorder(LXEngine engine) {
    this.engine = engine;
    registerEngine();
    for (LXChannel channel : engine.getChannels()) {
      registerChannel(channel);
    }
    for (LXEffect effect : engine.masterChannel.getEffects()) {
      registerComponent("effect/" + effect.getClass().getName(), effect);
    }
    engine.midi.addListener(this);
    engine.addMessageListener(this);
  }

  private LXAutomationRecorder registerEngine() {
    for (LXParameter parameter : this.engine.getParameters()) {
      if (parameter instanceof LXListenableParameter) {
        registerParameter("engine/" + parameter.getLabel(), (LXListenableParameter) parameter);
      }
    }
    return this;
  }

  private LXAutomationRecorder registerChannel(LXChannel channel) {
    String path = "channel/" + channel.getIndex();
    this.channels.add(channel);
    channel.addListener(new LXChannel.AbstractListener() {
      @Override
      public void patternWillChange(LXChannel channel, LXPattern pattern,
          LXPattern nextPattern) {
        if (armRecord.isOn()) {
          events.add(new PatternAutomationEvent(channel, nextPattern));
        }
      }
    });
    registerParameter(path + "/fader", channel.fader);
    registerParameter(path + "/blendMode", channel.blendMode);
    for (LXPattern pattern : channel.getPatterns()) {
      registerComponent(path + "/pattern/" + pattern.getClass().getName(), pattern);
    }
    return this;
  }

  private LXAutomationRecorder registerComponent(String prefix, LXLayeredComponent component) {
    for (LXParameter parameter : component.getParameters()) {
      if (parameter instanceof LXListenableParameter) {
        registerParameter(prefix + "/" + parameter.getLabel(), (LXListenableParameter) parameter);
      }
    }
    return this;
  }

  private LXAutomationRecorder registerParameter(String path, LXListenableParameter parameter) {
    this.pathToParameter.put(path, parameter);
    this.parameterToPath.put(parameter, path);
    addParameter(parameter);
    return this;
  }

  @Override
  protected void onStart() {
    this.elapsedMillis = 0;
    if (this.armRecord.isOn()) {
      this.events.clear();
      for (LXParameter parameter : getParameters()) {
        this.events.add(new ParameterAutomationEvent(parameter));
      }
      for (LXChannel channel : this.channels) {
        this.events.add(new PatternAutomationEvent(channel, channel.getActivePattern()));
      }
    }
  }

  @Override
  protected void onStop() {
    if (this.armRecord.isOn()) {
      this.events.add(new FinishAutomationEvent());
      this.armRecord.setValue(false);
    }
  }

  @Override
  protected void onReset() {
    this.cursor = 0;
  }

  @Override
  protected void run(double deltaMs) {
    this.elapsedMillis += deltaMs;
    if (!this.armRecord.isOn()) {
      while (isRunning() && (this.cursor < this.events.size())) {
        LXAutomationEvent event = this.events.get(this.cursor);
        if (this.elapsedMillis < event.millis) {
          return;
        }
        ++this.cursor;
        event.play();
      }
    }
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (this.armRecord.isOn()) {
      if (this.parameterToPath.containsKey(parameter)) {
        this.events.add(new ParameterAutomationEvent(parameter));
      }
    }
  }

  public final JsonArray toJson() {
    JsonArray jsonArr = new JsonArray();
    for (LXAutomationEvent event : this.events) {
      jsonArr.add(event.toJson());
    }
    return jsonArr;
  }

  public final void loadJson(JsonArray jsonArr) {
    this.events.clear();
    for (JsonElement element : jsonArr) {
      try {
        JsonObject obj = element.getAsJsonObject();
        LXAutomationEvent event = null;
        String eventType = obj.get(KEY_EVENT).getAsString();
        if (eventType.equals(EVENT_PARAMETER)) {
          String parameterPath = obj.get(KEY_PARAMETER).getAsString();
          LXParameter parameter = pathToParameter.get(parameterPath);
          if (parameter != null) {
            event = new ParameterAutomationEvent(parameter, obj.get(KEY_VALUE).getAsFloat());
          } else {
            System.out.println("LXAutomationRecorder::Invalid parameter: " + parameterPath);
          }
        } else if (eventType.equals(EVENT_PATTERN)) {
          int channelIndex = obj.get(KEY_CHANNEL).getAsInt();
          String patternClassName = obj.get(KEY_PATTERN).getAsString();
          LXChannel channel = this.engine.getChannel(channelIndex);
          LXPattern pattern = channel.getPattern(patternClassName);
          event = new PatternAutomationEvent(channel, pattern);
        } else if (eventType.equals(EVENT_MIDI)) {
          int command = obj.get(KEY_COMMAND).getAsInt();
          int channel = obj.get(KEY_CHANNEL).getAsInt();
          int data1 = obj.get(KEY_DATA_1).getAsInt();
          int data2 = obj.get(KEY_DATA_2).getAsInt();
          try {
            ShortMessage sm = new ShortMessage();
            sm.setMessage(command, channel, data1, data2);
            event = new MidiAutomationEvent(LXShortMessage.fromShortMessage(sm));
          } catch (InvalidMidiDataException imdx) {
            System.out.println("LXAutomationRecorder::Invalid midi data: " + imdx.getMessage());
          }
        } else if (eventType.equals(EVENT_MESSAGE)) {
          String message = obj.get(KEY_MESSAGE).getAsString();
          event = new MessageAutomationEvent(message);
        } else if (eventType.equals(EVENT_FINISH)) {
          event = new FinishAutomationEvent();
        }
        if (event != null) {
          event.millis = obj.get(KEY_MILLIS).getAsFloat();
          this.events.add(event);
        }
      } catch (UnsupportedOperationException uox) {
        System.out.println("LXAutomationRecorder::Invalid automation event: " + element);
      }
    }
  }

  private void midiEventReceived(LXShortMessage shortMessage) {
    if (this.armRecord.isOn()) {
      this.events.add(new MidiAutomationEvent(shortMessage));
    }
  }

  @Override
  public void noteOnReceived(LXMidiNoteOn note) {
    midiEventReceived(note);
  }

  @Override
  public void noteOffReceived(LXMidiNote note) {
    midiEventReceived(note);
  }

  @Override
  public void controlChangeReceived(LXMidiControlChange cc) {
    midiEventReceived(cc);
  }

  @Override
  public void programChangeReceived(LXMidiProgramChange pc) {
    midiEventReceived(pc);
  }

  @Override
  public void pitchBendReceived(LXMidiPitchBend pitchBend) {
    midiEventReceived(pitchBend);
  }

  @Override
  public void aftertouchReceived(LXMidiAftertouch aftertouch) {
    midiEventReceived(aftertouch);
  }

  @Override
  public void onMessage(LXEngine engine, String message) {
    if (this.armRecord.isOn()) {
      this.events.add(new MessageAutomationEvent(message));
    }
  }
}
