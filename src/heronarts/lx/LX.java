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

import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXPalette;
import heronarts.lx.model.GridModel;
import heronarts.lx.model.LXModel;
import heronarts.lx.output.LXOutput;
import heronarts.lx.pattern.IteratorTestPattern;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * Core controller for a LX instance. Each instance drives a grid of nodes with
 * a fixed width and height. The nodes are indexed using typical computer
 * graphics coordinates, with the x-axis going from left to right, y-axis from
 * top to bottom.
 *
 * <pre>
 *    x
 *  y 0 1 2 .
 *    1 . . .
 *    2 . . .
 *    . . . .
 * </pre>
 *
 * Note that the grid layout is just a helper. The node buffer is actually a 1-d
 * array and can be used to represent any type of layout. The library just
 * provides helpful accessors for grid layouts.
 *
 * The instance manages rotation amongst a set of patterns. There may be
 * multiple channels, each with its own list of patterns. These channels are then
 * blended together.
 *
 * The color-space used is HSB, with H ranging from 0-360, S from 0-100, and B
 * from 0-100.
 */
public class LX {

  public static class InitTimer {
    private long lastTime;

    protected void init() {
      this.lastTime = System.nanoTime();
    }

    public void log(String label) {
      long thisTime = System.nanoTime();
      if (LX.LOG_INIT_TIMING) {
        System.out.println(String.format("[LX init: %s: %.2fms]", label, (thisTime - lastTime) / 1000000.));
      }
      this.lastTime = thisTime;
    }
  }

  public static final InitTimer initTimer = new InitTimer();

  private static boolean LOG_INIT_TIMING = false;

  public static void logInitTiming() {
    LX.LOG_INIT_TIMING = true;
  }

  /**
   * Listener for top-level events
   */
  public interface Listener {
    public void modelChanged(LX lx, LXModel model);
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  public interface ProjectListener {
    public void projectChanged(File file);
  }

  private final List<ProjectListener> projectListeners = new ArrayList<ProjectListener>();

  final LXComponent.Registry componentRegistry = new LXComponent.Registry();

  /**
   * The width of the grid, immutable.
   */
  public final int width;

  /**
   * The height of the grid, immutable.
   */
  public final int height;

  /**
   * The midpoint of the x-space.
   */
  public final float cx;

  /**
   * This midpoint of the y-space.
   */
  public final float cy;

  /**
   * The pixel model.
   */
  public final LXModel model;

  /**
   * The total number of pixels in the grid, immutable.
   */
  public final int total;

  /**
   * The default palette.
   */
  public final LXPalette palette;

  /**
   * The animation engine.
   */
  public final LXEngine engine;

  /**
   * The global tempo object.
   */
  public final Tempo tempo;

  /**
   * The global audio input.
   */
  public final LXAudio audio;

  /**
   * The list of globally registered pattern classes
   */
  private final List<Class<LXPattern>> registeredPatterns =
    new ArrayList<Class<LXPattern>>();

  /**
   * The list of globally registered effects
   */
  private final List<Class<LXEffect>> registeredEffects =
    new ArrayList<Class<LXEffect>>();

  /**
   * Creates an LX instance with no nodes.
   */
  public LX() {
    this(null);
  }

  /**
   * Creates an LX instance. This instance will run patterns for a grid of the
   * specified size.
   *
   * @param total Number of nodes
   */
  public LX(int total) {
    this(total, 1);
  }

  /**
   * Creates a LX instance. This instance will run patterns for a grid of the
   * specified size.
   *
   * @param width Width
   * @param height Height
   */
  public LX(int width, int height) {
    this(new GridModel(width, height));
  }

  /**
   * Constructs an LX instance with the given pixel model
   *
   * @param model Pixel model
   */
  public LX(LXModel model) {
    LX.initTimer.init();
    this.model = model;
    if (model == null) {
      this.total = this.width = this.height = 0;
      this.cx = this.cy = 0;
    } else {
      this.total = model.points.size();
      this.cx = model.cx;
      this.cy = model.cy;
      if (model instanceof GridModel) {
        GridModel grid = (GridModel) model;
        this.width = grid.width;
        this.height = grid.height;
      } else {
        this.width = this.height = 0;
      }
    }
    LX.initTimer.log("Model");

    // Color palette
    this.palette = new LXPalette(this);
    LX.initTimer.log("Palette");

    // Construct the engine
    this.engine = new LXEngine(this);
    LX.initTimer.log("Engine");

    // Tempo
    this.tempo = new Tempo(this);
    LX.initTimer.log("Tempo");

    // Audio
    this.audio = new LXAudio(this);
    LX.initTimer.log("Audio");

    // Add a default channel
    this.engine.addChannel(new LXPattern[] { new IteratorTestPattern(this) }).fader.setValue(1);
    LX.initTimer.log("Default Channel");

  }

  public LX addListener(Listener listener) {
    this.listeners.add(listener);
    return this;
  }

  public LX removeListener(Listener listener) {
    this.listeners.add(listener);
    return this;
  }

  public LX addProjectListener(ProjectListener listener) {
    this.projectListeners.add(listener);
    return this;
  }

  public LX removeProjectListener(ProjectListener listener) {
    this.projectListeners.remove(listener);
    return this;
  }

  public LXComponent getComponent(int id) {
    return this.componentRegistry.get(id);
  }

  /**
   * Shut down resources of the LX instance.
   */
  public void dispose() {
    this.audio.dispose();
  }

  /**
   * Utility function to return the row of a given index
   *
   * @param i Index into colors array
   * @return Which row this index is in
   */
  public int row(int i) {
    return (this.width == 0) ? 0 : (i / this.width);
  }

  /**
   * Utility function to return the column of a given index
   *
   * @param i Index into colors array
   * @return Which column this index is in
   */
  public int column(int i) {
    return (this.width == 0) ? 0 : (i % this.width);
  }

  /**
   * Utility function to get the x-coordinate of a pixel
   *
   * @param i Node index
   * @return x coordinate
   */
  public int x(int i) {
    return (this.width == 0) ? 0 : (i % this.width);
  }

  /**
   * Utility function to return the position of an index in x coordinate space
   * normalized from 0 to 1.
   *
   * @param i Node index
   * @return Position of this node in x space, from 0 to 1
   */
  public double xn(int i) {
    return (this.width == 0) ? 0 : ((i % this.width) / (double) (this.width - 1));
  }

  /**
   * Utility function to return the position of an index in x coordinate space
   * normalized from 0 to 1, as a floating point.
   *
   * @param i Node index
   * @return Position of this node in x space, from 0 to 1
   */
  public float xnf(int i) {
    return (float) this.xn(i);
  }

  /**
   * Utility function to get the y-coordinate of a pixel
   *
   * @param i Node index
   * @return y coordinate
   */
  public int y(int i) {
    return (this.width == 0) ? 0 : (i / this.width);
  }

  /**
   * Utility function to return the position of an index in y coordinate space
   * normalized from 0 to 1.
   *
   * @param i Node index
   * @return Position of this node in y space, from 0 to 1
   */
  public double yn(int i) {
    return (this.width == 0) ? 0 : ((i / this.width) / (double) (this.height - 1));
  }

  /**
   * Utility function to return the position of an index in y coordinate space
   * normalized from 0 to 1, as a floating point.
   *
   * @param i Node index
   * @return Position of this node in y space, from 0 to 1
   */
  public float ynf(int i) {
    return (float) this.yn(i);
  }

  /**
   * Shorthand for LXColor.hsb()
   *
   * @param h Hue 0-360
   * @param s Saturation 0-100
   * @param b Brightness 0-100
   * @return Color
   */
  public static int hsb(float h, float s, float b) {
    return LXColor.hsb(h, s, b);
  }

  /**
   * Shorthand for LXColor.hsa()
   *
   * @param h Hue 0-360
   * @param s Saturation 0-100
   * @param a Alpha 0-1
   * @return Color
   */
  public static int hsa(float h, float s, float a) {
    return LXColor.hsba(h, s, 100, a);
  }

  /**
   * Sets the speed of the entire system. Default is 1.0, any modification will
   * mutate deltaMs values system-wide.
   *
   * @param speed Coefficient, 1 is normal speed
   * @return this
   */
  public LX setSpeed(double speed) {
    this.engine.setSpeed(speed);
    return this;
  }

  /**
   * Add multiple effects to the chain
   *
   * @param effects Array of effects
   * @return this
   */
  public LX addEffects(LXEffect[] effects) {
    for (LXEffect effect : effects) {
      addEffect(effect);
    }
    return this;
  }

  /**
   * Add an effect to the FX chain.
   *
   * @param effect Effect
   * @return this
   */
  public LX addEffect(LXEffect effect) {
    this.engine.masterChannel.addEffect(effect);
    return this;
  }

  /**
   * Remove an effect from the chain
   *
   * @param effect Effect
   * @return this
   */
  public LX removeEffect(LXEffect effect) {
    this.engine.masterChannel.removeEffect(effect);
    return this;
  }

  /**
   * Pause the engine from running
   *
   * @param paused Whether to pause the engine to pause
   * @return this
   */
  public LX setPaused(boolean paused) {
    this.engine.setPaused(paused);
    return this;
  }

  /**
   * Whether the engine is currently running.
   *
   * @return State of the engine
   */
  public boolean isPaused() {
    return this.engine.isPaused();
  }

  /**
   * Toggles the running state of the engine.
   *
   * @return this
   */
  public LX togglePaused() {
    return setPaused(!this.engine.isPaused());
  }

  /**
   * Sets the main channel to the previous pattern.
   *
   * @return this
   */
  public LX goPrev() {
    this.engine.goPrev();
    return this;
  }

  /**
   * Sets the main channel to the next pattern.
   *
   * @return this
   */
  public LX goNext() {
    this.engine.goNext();
    return this;
  }

  /**
   * Sets the main channel to a given pattern instance.
   *
   * @param pattern The pattern instance to run
   * @return this
   */
  public LX goPattern(LXPattern pattern) {
    this.engine.goPattern(pattern);
    return this;
  }

  /**
   * Sets the main channel to a pattern of the given index
   *
   * @param i Index of the pattern to run
   * @return this
   */
  public LX goIndex(int i) {
    this.engine.goIndex(i);
    return this;
  }

  /**
   * Stops patterns from automatically rotating
   *
   * @return this
   */
  public LX disableAutoTransition() {
    this.engine.disableAutoTransition();
    return this;
  }

  /**
   * Sets the patterns to rotate automatically
   *
   * @param autoTransitionThreshold Number of milliseconds after which to rotate
   *          pattern
   * @return this
   */
  public LX enableAutoTransition(int autoTransitionThreshold) {
    this.engine.enableAutoTransition(autoTransitionThreshold);
    return this;
  }

  /**
   * Adds an output driver
   *
   * @param output Output
   * @return this
   */
  public LX addOutput(LXOutput output) {
    this.engine.addOutput(output);
    return this;
  }

  /**
   * Specifies the set of patterns to be run.
   *
   * @param patterns Array of patterns
   * @return this
   */
  public LX setPatterns(LXPattern[] patterns) {
    this.engine.setPatterns(patterns);
    return this;
  }

  /**
   * Gets the current set of patterns on the main channel.
   *
   * @return The list of patters
   */
  public List<LXPattern> getPatterns() {
    return this.engine.getPatterns();
  }

  /**
   * Register a pattern class with the engine
   *
   * @param pattern
   * @return this
   */
  public LX registerPattern(Class<LXPattern> pattern) {
    this.registeredPatterns.add(pattern);
    return this;
  }

  /**
   * Register a pattern class with the engine
   *
   * @param pattern
   * @return this
   */
  public LX registerPatterns(Class<LXPattern>[] patterns) {
    for (Class<LXPattern> pattern : patterns) {
      registerPattern(pattern);
    }
    return this;
  }

  /**
   * Gets the list of registered pattern classes
   *
   * @return Pattern classes
   */
  public List<Class<LXPattern>> getRegisteredPatterns() {
    return this.registeredPatterns;
  }

  /**
   * Register an effect class with the engine
   *
   * @param effect
   * @return this
   */
  public LX registerEffect(Class<LXEffect> effect) {
    this.registeredEffects.add(effect);
    return this;
  }

  /**
   * Register an effect class with the engine
   *
   * @param effects
   * @return this
   */
  public LX registerEffects(Class<LXEffect>[] effects) {
    for (Class<LXEffect> effect : effects) {
      registerEffect(effect);
    }
    return this;
  }

  /**
   * Gets the list of registered effect classes
   *
   * @return Effect classes
   */
  public List<Class<LXEffect>> getRegisteredEffects() {
    return this.registeredEffects;
  }

  private final static String KEY_VERSION = "version";
  private final static String KEY_TIMESTAMP = "timestamp";
  private final static String KEY_ENGINE = "engine";

  private File file;

  public File getProject() {
    return this.file;
  }

  public void saveProject() {
    if (this.file != null) {
      saveProject(this.file);
    }
  }

  public void saveProject(File file) {
    JsonObject obj = new JsonObject();
    obj.addProperty(KEY_VERSION, "0.1");
    obj.addProperty(KEY_TIMESTAMP, System.currentTimeMillis());
    JsonObject engine = new JsonObject();
    obj.add(KEY_ENGINE, engine);
    this.engine.save(engine);
    try {
      JsonWriter writer = new JsonWriter(new FileWriter(file));
      new GsonBuilder().setPrettyPrinting().create().toJson(obj, writer);
      writer.close();
      System.out.println("Project saved successfully to " + file.toString());
      this.file = file;
      for (ProjectListener projectListener : this.projectListeners) {
        projectListener.projectChanged(file);
      }
    } catch (IOException iox) {
      System.err.println(iox.getLocalizedMessage());
    }
  }

  public void loadProject(File file) {
    try {
      FileReader fr = null;
      try {
        fr = new FileReader(file);
        JsonObject obj = new Gson().fromJson(fr, JsonObject.class);
        this.engine.load(obj.getAsJsonObject(KEY_ENGINE));
        System.out.println("Project loaded successfully from " + file.toString());
        this.file = file;
        for (ProjectListener projectListener : this.projectListeners) {
          projectListener.projectChanged(file);
        }
      } catch (IOException iox) {
        System.err.println(iox.getLocalizedMessage());
      } finally {
        if (fr != null) {
          try {
            fr.close();
          } catch (IOException ignored) {}
        }
      }
    } catch (Exception x) {
      System.err.println(x.getLocalizedMessage());
      x.printStackTrace(System.err);
    }
  }

  protected LXEffect instantiateEffect(String className) {
    try {
      Class<? extends LXEffect> cls = Class.forName(className).asSubclass(LXEffect.class);
      return cls.getConstructor(LX.class).newInstance(this);
    } catch (Exception x) {
      System.err.println(x.getLocalizedMessage());
    }
    return null;
  }

  protected LXPattern instantiatePattern(String className) {
    try {
      Class<? extends LXPattern> cls = Class.forName(className).asSubclass(LXPattern.class);
      return cls.getConstructor(LX.class).newInstance(this);
    } catch (Exception x) {
      System.err.println(x.getLocalizedMessage());
    }
    return null;
  }

}

