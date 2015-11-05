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

import heronarts.lx.effect.LXEffect;
import heronarts.lx.midi.LXMidiEngine;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.output.LXOutput;
import heronarts.lx.parameter.BasicParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameterized;
import heronarts.lx.pattern.IteratorTestPattern;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.pattern.SolidColorPattern;
import heronarts.lx.transition.LXTransition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;

/**
 * The engine is the core class that runs the internal animations. An engine is
 * comprised of top-level modulators, then a number of channels, each of which
 * has a set of patterns that it may transition between. These channels are
 * blended together, and effects are then applied.
 *
 * <pre>
 *   -----------------------------
 *  | Engine                      |
 *  |  -------------------------  |
 *  | | Modulators              | |
 *  |  -------------------------  |
 *  |  --------    --------       |
 *  | | Chnl 0 |  | Chnl 1 |  ... |
 *  |  --------    --------       |
 *  |  -------------------------  |
 *  | | Effects                 | |
 *  |  -------------------------  |
 *   -----------------------------
 * </pre>
 *
 * The result of all this generates a display buffer of node values.
 */
public class LXEngine extends LXParameterized {

  private final LX lx;

  public final LXMidiEngine midiEngine;

  private Dispatch inputDispatch = null;

  private final List<LXLoopTask> loopTasks = new ArrayList<LXLoopTask>();
  private final List<LXChannel> channels = new ArrayList<LXChannel>();
  private final List<LXEffect> effects = new ArrayList<LXEffect>();
  private final List<LXOutput> outputs = new ArrayList<LXOutput>();
  private final List<Listener> listeners = new ArrayList<Listener>();
  private final List<MessageListener> messageListeners = new ArrayList<MessageListener>();

  private final List<LXChannel> unmodifiableChannels = Collections.unmodifiableList(this.channels);
  private final List<LXEffect> unmodifiableEffects = Collections.unmodifiableList(this.effects);

  public final DiscreteParameter focusedChannel = new DiscreteParameter("CHANNEL", 1);

  public final BasicParameter framesPerSecond = new BasicParameter("FPS", 60, 0, 300);

  private float frameRate = 0;

  private final ReadWriteUpdateLock engineModificationLock = new ReentrantReadWriteUpdateLock();

  public interface Dispatch {
    public void dispatch();
  }

  public interface Listener {
    public void channelAdded(LXEngine engine, LXChannel channel);
    public void channelRemoved(LXEngine engine, LXChannel channel);
  }

  public interface MessageListener {
    public void onMessage(LXEngine engine, String message);
  }

  public final LXEngine addListener(Listener listener) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.listeners.add(listener);
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXEngine removeListener(Listener listener) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.listeners.remove(listener);
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXEngine addMessageListener(MessageListener listener) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.messageListeners.add(listener);
    } finally {
      l.unlock();
    }
    return this;
  }

  public final LXEngine removeMessageListener(MessageListener listener) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.messageListeners.remove(listener);
    } finally {
      l.unlock();
    }
    return this;
  }

  public LXEngine broadcastMessage(String message) {
    Lock l = this.engineModificationLock.updateLock();
    l.lock();
    try {
      for (MessageListener listener : this.messageListeners) {
        listener.onMessage(this, message);
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public class Timer {
    public long runNanos = 0;
    public long channelNanos = 0;
    public long copyNanos = 0;
    public long fxNanos = 0;
    public long inputNanos = 0;
    public long midiNanos = 0;
    public long outputNanos = 0;
  }

  public final Timer timer = new Timer();

  /**
   * Utility class for a threaded engine to do buffer-flipping. One buffer is
   * actively being worked on, while another copy is held for shuffling off to
   * another thread.
   */
  private class DoubleBuffer implements LXBuffer {

    // Concrete buffer instances, these are real memory
    private final int[] buffer1;
    private final int[] buffer2;

    // References, these flip between pointing to buffer1 and buffer 2
    private int[] render;
    private int[] copy;

    private DoubleBuffer() {
      this.render = this.buffer1 = new int[lx.total];
      this.copy = this.buffer2 = new int[lx.total];
      for (int i = 0; i < lx.total; ++i) {
        this.buffer1[i] = this.buffer2[i] = 0xff000000;
      }
    }

    private void flip() {
      int[] tmp = this.render;
      this.render = this.copy;
      this.copy = tmp;
    }

    @Override
    public int[] getArray() {
      return this.render;
    }
  }

  private final DoubleBuffer buffer;

  private final int[] black;

  long nowMillis = 0;
  private long lastMillis;

  private boolean isThreaded = false;
  private Thread engineThread = null;

  public final BasicParameter speed = new BasicParameter("SPEED", 1, 0, 2);

  private boolean paused = false;

  LXEngine(LX lx) {
    this.lx = lx;
    this.black = new int[lx.total];
    for (int i = 0; i < black.length; ++i) {
      this.black[i] = 0xff000000;
    }
    // Master double-buffer
    this.buffer = new DoubleBuffer();

    // Midi engine
    this.midiEngine = new LXMidiEngine(lx);

    // Default color palette
    addComponent(lx.palette);

    // Add a default channel
    addChannel(new LXPattern[] { new IteratorTestPattern(lx) });
    this.channels.get(0).getFader().setValue(1);

    // Initialize timer
    this.lastMillis = System.currentTimeMillis();
  }

  public LXEngine setInputDispatch(Dispatch inputDispatch) {
    this.inputDispatch = inputDispatch;
    return this;
  }

  /**
   * Gets the active frame rate of the engine when in threaded mode
   *
   * @return How many FPS the engine is running
   */
  public float frameRate() {
    return this.isThreaded ? this.frameRate : 0;
  }

  /**
   * Whether the engine is threaded. Should only be called from Processing
   * animation thread.
   *
   * @return Whether the engine is threaded
   */
  public boolean isThreaded() {
    Lock l = this.acquireReadLock();
    try {
      return this.isThreaded;
    } finally {
      unlock(l);
    }
  }

  /**
   * Starts the engine thread.
   */
  public void start() {
    setThreaded(true);
  }

  /**
   * Stops the engine thread.
   */
  public void stop() {
    setThreaded(false);
  }

  /**
   * Sets the engine to threaded or non-threaded mode. Should only be called
   * from the Processing animation thread.
   *
   * @param threaded Whether engine should run on its own thread
   */
  public LXEngine setThreaded(boolean threaded) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      if (threaded == this.isThreaded) {
        return this;
      }
      if (!threaded) {
        // Set interrupt flag on the engine thread
        this.engineThread.interrupt();
        if (Thread.currentThread() != this.engineThread) {
          // Called from another thread? If so, wait for engine thread to finish
          try {
            this.engineThread.join();
          } catch (InterruptedException ix) {
            throw new IllegalThreadStateException("Interrupted waiting to join LXEngine thread");
          }
        }
      } else {
        this.isThreaded = true;
        // Copy the current frame to avoid a black frame
        for (int i = 0; i < this.buffer.render.length; ++i) {
          this.buffer.copy[i] = this.buffer.render[i];
        }
        this.engineThread = new Thread("LX Engine Thread") {
          @Override
          public void run() {
            System.out.println("LX Engine Thread started.");
            while (!isInterrupted()) {
              long frameStart = System.currentTimeMillis();
              LXEngine.this.run();
              synchronized (buffer) {
                buffer.flip();
              }
              long frameMillis = System.currentTimeMillis() - frameStart;
              frameRate = 1000.f / frameMillis;
              float targetFPS = framesPerSecond.getValuef();
              if (targetFPS > 0) {
                long minMillisPerFrame = (long) (1000. / targetFPS);
                if (frameMillis < minMillisPerFrame) {
                  frameRate = targetFPS;
                  try {
                    sleep(minMillisPerFrame - frameMillis);
                  } catch (InterruptedException ix) {
                    // We're done!
                    break;
                  }
                }
              }
            }

            Lock l = LXEngine.this.engineModificationLock.writeLock();
            l.lock();
            try {
              // We are done threading
              LXEngine.this.engineThread = null;
              LXEngine.this.isThreaded = false;
            } finally {
              l.unlock();
            }
            System.out.println("LX Engine Thread finished.");
          }
        };
        this.engineThread.start();
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public LXEngine setSpeed(double speed) {
    this.speed.setValue(speed);
    return this;
  }

  /**
   * Pause the engine from running
   *
   * @param paused Whether to pause the engine to pause
   */
  public LXEngine setPaused(boolean paused) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.paused = paused;
    } finally {
      l.unlock();
    }
    return this;
  }

  public boolean isPaused() {
    Lock l = this.acquireReadLock();
    try {
      return this.paused;
    } finally {
      unlock(l);
    }
  }

  public LXEngine addComponent(LXComponent component) {
    return addLoopTask(component);
  }

  public LXEngine removeComponent(LXComponent component) {
    return removeLoopTask(component);
  }

  public LXEngine addModulator(LXModulator modulator) {
    return addLoopTask(modulator);
  }

  public LXEngine removeModulator(LXModulator modulator) {
    return removeLoopTask(modulator);
  }

  public LXEngine addLoopTask(LXLoopTask loopTask) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      if (!this.loopTasks.contains(loopTask)) {
        this.loopTasks.add(loopTask);
      }
    } finally {
      l.unlock();
    }
    return this;
  }

  public LXEngine removeLoopTask(LXLoopTask loopTask) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.loopTasks.remove(loopTask);
    } finally {
      l.unlock();
    }
    return this;
  }

  public List<LXEffect> getEffects() {
    return this.unmodifiableEffects;
  }

  public LXEngine addEffect(LXEffect fx) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.effects.add(fx);
    } finally {
      l.unlock();
    }
    return this;
  }

  public LXEngine removeEffect(LXEffect fx) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.effects.remove(fx);
    } finally {
      l.unlock();
    }
    return this;
  }

  /**
   * Adds an output driver
   *
   * @param output
   * @return this
   */
  public LXEngine addOutput(LXOutput output) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.outputs.add(output);
    } finally {
      l.unlock();
    }
    return this;
  }

  /**
   * Removes an output driver
   *
   * @param output
   * @return this
   */
  public LXEngine removeOutput(LXOutput output) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      this.outputs.remove(output);
    } finally {
      l.unlock();
    }
    return this;
  }

  public List<LXChannel> getChannels() {
    return this.unmodifiableChannels;
  }

  public LXChannel getDefaultChannel() {
    return this.getChannel(0);
  }

  public LXChannel getChannel(int channelIndex) {
    Lock l = this.acquireReadLock();
    try {
      return this.channels.get(channelIndex);
    } finally {
      unlock(l);
    }
  }

  public LXChannel getFocusedChannel() {
    return getChannel(focusedChannel.getValuei());
  }

  public LXChannel addChannel() {
    return addChannel(new LXPattern[] { new SolidColorPattern(lx, 0xff000000) });
  }

  public LXChannel addChannel(LXPattern[] patterns) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      LXChannel channel = new LXChannel(lx, this.channels.size(), patterns);
      this.channels.add(channel);
      this.focusedChannel.setRange(this.channels.size());
      for (Listener listener : this.listeners) {
        listener.channelAdded(this, channel);
      }
      return channel;
    } finally {
      l.unlock();
    }
  }

  public void removeChannel(LXChannel channel) {
    Lock l = this.engineModificationLock.writeLock();
    l.lock();
    try {
      if (this.channels.remove(channel)) {
        int i = 0;
        for (LXChannel c : this.channels) {
          c.setIndex(i++);
        }
        this.focusedChannel.setRange(this.channels.size());
        for (Listener listener : this.listeners) {
          listener.channelRemoved(this, channel);
        }
      }
    } finally {
      l.unlock();
    }
  }

  public void setPatterns(LXPattern[] patterns) {
    this.getDefaultChannel().setPatterns(patterns);
  }

  public List<LXPattern> getPatterns() {
    return this.getDefaultChannel().getPatterns();
  }

  protected LXPattern getActivePattern() {
    return this.getDefaultChannel().getActivePattern();
  }

  protected LXPattern getNextPattern() {
    return this.getDefaultChannel().getNextPattern();
  }

  protected LXTransition getActiveTransition() {
    return this.getDefaultChannel().getActiveTransition();
  }

  public void goPrev() {
    this.getDefaultChannel().goPrev();
  }

  public final void goNext() {
    this.getDefaultChannel().goNext();
  }

  public void goPattern(LXPattern pattern) {
    this.getDefaultChannel().goPattern(pattern);
  }

  public void goIndex(int index) {
    this.getDefaultChannel().goIndex(index);
  }

  protected void disableAutoTransition() {
    getDefaultChannel().disableAutoTransition();
  }

  protected void enableAutoTransition(int autoTransitionThreshold) {
    getDefaultChannel().enableAutoTransition(autoTransitionThreshold);
  }

  protected boolean isAutoTransitionEnabled() {
    return getDefaultChannel().isAutoTransitionEnabled();
  }

  public void run() {
    Lock l = this.engineModificationLock.updateLock();
    l.lock();
    try {
      long runStart = System.nanoTime();

      // Compute elapsed time
      this.nowMillis = System.currentTimeMillis();
      double deltaMs = this.nowMillis - this.lastMillis;
      this.lastMillis = this.nowMillis;

      if (this.paused) {
        this.timer.channelNanos = 0;
        this.timer.copyNanos = 0;
        this.timer.fxNanos = 0;
        this.timer.runNanos = System.nanoTime() - runStart;
        return;
      }

      // Run tempo, always using real-time
      this.lx.tempo.loop(deltaMs);

      // Run top-level loop tasks
      for (LXLoopTask loopTask : this.loopTasks) {
        loopTask.loop(deltaMs);
      }

      // Mutate by speed for channels and effects
      deltaMs *= this.speed.getValue();

      // Run and blend all of our channels
      long channelStart = System.nanoTime();
      int[] bufferColors = this.black;
      for (LXChannel channel : this.channels) {
        if (channel.enabled.isOn()) {
          channel.loop(deltaMs);
          channel.getFaderTransition().timer.blendNanos = 0;

          // This optimization assumed that all transitions do
          // nothing at 0 and completely take over at 1. That's
          // not always the case. Leaving this here for reference.

          // if (channel.getFader().getValue() == 0) {
          // // No blending on this channel, leave colors as they were
          // } else if (channel.getFader().getValue() >= 1) {
          // // Fully faded in, just use this channel
          // bufferColors = channel.getColors();
          // } else {

          // Apply the fader to this channel
          channel.getFaderTransition().loop(deltaMs);
          channel.getFaderTransition().blend(
            bufferColors,
            channel.getColors(),
            channel.getFader().getValue()
          );
          bufferColors = channel.getFaderTransition().getColors();
        }
      }

      this.timer.channelNanos = System.nanoTime() - channelStart;

      // Copy colors into our own rendering buffer
      long copyStart = System.nanoTime();
      for (int i = 0; i < bufferColors.length; ++i) {
        this.buffer.render[i] = bufferColors[i];
      }
      this.timer.copyNanos = System.nanoTime() - copyStart;

      // Apply effects in our rendering buffer
      long fxStart = System.nanoTime();
      for (LXEffect fx : this.effects) {
        ((LXLayeredComponent) fx).setBuffer(this.buffer);
        fx.loop(deltaMs);
      }

      this.timer.fxNanos = System.nanoTime() - fxStart;

      // Process UI input events
      if (this.inputDispatch == null) {
        this.timer.inputNanos = 0;
      } else {
        long inputStart = System.nanoTime();
        this.inputDispatch.dispatch();
        this.timer.inputNanos = System.nanoTime() - inputStart;
      }

      // Process MIDI events
      long midiStart = System.nanoTime();
      this.midiEngine.dispatch();
      this.timer.midiNanos = System.nanoTime() - midiStart;

      // Send to outputs
      long outputStart = System.nanoTime();
      for (LXOutput output : this.outputs) {
        output.send(this.buffer.render);
      }

      this.timer.outputNanos = System.nanoTime() - outputStart;

      this.timer.runNanos = System.nanoTime() - runStart;
    } finally {
      l.unlock();
    }
  }

  private Lock acquireReadLock() {
    Lock l = this.engineModificationLock.readLock();
    try {
      l.lock();
    } catch (IllegalStateException e) {
      // Current thread already holding update lock.
      // Safe to skip getting read lock
      return null;
    }
    return l;
  }

  private void unlock(Lock l) {
    if (l != null) {
      l.unlock();
    }
  }

  /**
   * This should be used when in threaded mode. It synchronizes on the
   * double-buffer and duplicates the internal copy buffer into the provided
   * buffer.
   *
   * @param copy Buffer to copy into
   */
  public void copyBuffer(int[] copy) {
    synchronized (this.buffer) {
      for (int i = 0; i < this.buffer.copy.length; ++i) {
        copy[i] = this.buffer.copy[i];
      }
    }
  }

  /**
   * This is used when not in threaded mode. It provides direct access to the
   * engine's render buffer.
   *
   * @return The internal render buffer
   */
  public int[] renderBuffer() {
    return this.buffer.render;
  }
}
