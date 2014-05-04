/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 * All Rights Reserved
 *
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package heronarts.lx.modulator;

import heronarts.lx.LXLayerComponent;
import heronarts.lx.LXChannel;
import heronarts.lx.LXEngine;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.pattern.LXPattern;

import java.util.ArrayList;
import java.util.List;

public class LXAutomationRecorder extends LXModulator implements
    LXParameterListener {

  public final BooleanParameter armRecord = new BooleanParameter("ARM", false);
  public final BooleanParameter looping = new BooleanParameter("LOOP", false);

  private final List<LXChannel> channels = new ArrayList<LXChannel>();

  private int cursor = 0;
  private double elapsedMillis = 0;

  private abstract class LXAutomationEvent {

    final double millis;

    private LXAutomationEvent() {
      this.millis = elapsedMillis;
    }

    abstract void play();
  }

  private class PatternAutomationEvent extends LXAutomationEvent {
    final LXChannel channel;
    final LXPattern pattern;

    private PatternAutomationEvent(LXChannel channel, LXPattern pattern) {
      this.channel = channel;
      this.pattern = pattern;
    }

    @Override
    void play() {
      this.channel.goPattern(this.pattern);
    }
  }

  private class ParameterAutomationEvent extends LXAutomationEvent {

    final LXParameter parameter;
    final double value;

    private ParameterAutomationEvent(LXParameter parameter) {
      this.parameter = parameter;
      this.value = parameter.getValue();
    }

    @Override
    void play() {
      this.parameter.setValue(this.value);
    }
  }

  private class FinishAutomationEvent extends LXAutomationEvent {
    @Override
    void play() {
      if (looping.isOn()) {
        elapsedMillis = 0;
        cursor = 0;
      } else {
        reset();
      }
    }
  }

  private final List<LXAutomationEvent> events = new ArrayList<LXAutomationEvent>();

  public LXAutomationRecorder(LXEngine engine) {
    super("AUTOMATION");
    for (LXChannel channel : engine.getChannels()) {
      registerChannel(channel);
    }
    for (LXEffect effect : engine.getEffects()) {
      registerComponent(effect);
    }
  }

  public LXAutomationRecorder registerChannel(LXChannel channel) {
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
    registerParameter(channel.getFader());
    registerComponent(channel.getFaderTransition());
    for (LXPattern pattern : channel.getPatterns()) {
      registerComponent(pattern);
    }
    return this;
  }

  public LXAutomationRecorder registerComponent(LXLayerComponent component) {
    for (LXParameter parameter : component.getParameters()) {
      if (parameter instanceof LXListenableParameter) {
        registerParameter((LXListenableParameter) parameter);
      }
    }
    return this;
  }

  public LXAutomationRecorder registerParameter(LXListenableParameter parameter) {
    addParameter(parameter);
    return this;
  }

  @Override
  public void onStart() {
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
  public void onStop() {
    if (this.armRecord.isOn()) {
      this.events.add(new FinishAutomationEvent());
      this.armRecord.setValue(false);
    }
  }

  @Override
  public void onReset() {
    this.cursor = 0;
  }

  @Override
  public double computeValue(double deltaMs) {
    this.elapsedMillis += deltaMs;
    if (!this.armRecord.isOn()) {
      while (isRunning() && (this.cursor < this.events.size())) {
        LXAutomationEvent event = this.events.get(this.cursor);
        if (this.elapsedMillis < event.millis) {
          return 0;
        }
        ++this.cursor;
        event.play();
      }
    }
    return 0;
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    if (this.armRecord.isOn()) {
      events.add(new ParameterAutomationEvent(parameter));
    }
  }
}
