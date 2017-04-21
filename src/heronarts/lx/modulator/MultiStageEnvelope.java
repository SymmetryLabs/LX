/**
 * Copyright 2017- Mark C. Slee, Heron Arts LLC
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package heronarts.lx.modulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import heronarts.lx.LX;
import heronarts.lx.LXSerializable;
import heronarts.lx.LXUtils;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.FixedParameter;
import heronarts.lx.parameter.LXParameter;

public class MultiStageEnvelope extends LXRangeModulator implements LXWaveshape {

  public class Stage implements LXSerializable {
    private double basis;
    private double value;
    private double shape;
    private Stage previous = null;
    private Stage next = null;

    public final boolean initial;
    public final boolean last;

    private Stage(double basis, double value) {
      this(basis, value, 1, false, false);
    }

    private Stage(JsonObject obj) {
      this(
        obj.get(KEY_BASIS).getAsDouble(),
        obj.get(KEY_VALUE).getAsDouble(),
        obj.get(KEY_SHAPE).getAsDouble(),
        false,
        false
      );
    }

    private Stage(double basis, double value, double shape, boolean initial, boolean last) {
      this.basis = basis;
      this.value = value;
      this.shape = shape;
      this.initial = initial;
      this.last = last;
    }

    public void setPosition(double basis, double value) {
      if (!this.initial && !this.last) {
        this.basis = LXUtils.constrain(basis, this.previous.basis, this.next.basis);
      }
      this.value = value;
      monitor.toggle();
    }

    public void setShape(double shape) {
      this.shape = shape;
      monitor.toggle();
    }

    public double getBasis() {
      return this.basis;
    }

    public double getValue() {
      return this.value;
    }

    public double getShape() {
      return this.shape;
    }

    @Override
    public String toString() {
      return String.format("Basis: %.2f Value: %.2f", this.basis, this.value);
    }

    private static final String KEY_BASIS = "basis";
    private static final String KEY_VALUE = "value";
    private static final String KEY_SHAPE = "shape";

    @Override
    public void save(LX lx, JsonObject object) {
      object.addProperty(KEY_BASIS, this.basis);
      object.addProperty(KEY_VALUE, this.value);
      object.addProperty(KEY_SHAPE, this.shape);
    }

    @Override
    public void load(LX lx, JsonObject object) {
      if (object.has(KEY_BASIS)) {
        this.basis = object.get(KEY_BASIS).getAsDouble();
      }
      if (object.has(KEY_VALUE)) {
        this.value= object.get(KEY_VALUE).getAsDouble();
      }
      if (object.has(KEY_SHAPE)) {
        this.shape = object.get(KEY_SHAPE).getAsDouble();
      }
    }
  }

  public final CompoundParameter period = (CompoundParameter)
    new CompoundParameter("Period", 1000, 100, 10000)
    .setDescription("Sets the period of the Envelope in secs")
    .setExponent(2)
    .setUnits(LXParameter.Units.MILLISECONDS);

  private final List<Stage> internalStages = new ArrayList<Stage>();

  public final List<Stage> stages = Collections.unmodifiableList(internalStages);

  public final BooleanParameter monitor = new BooleanParameter("Monitor");

  public MultiStageEnvelope() {
    this("Env");
  }

  public MultiStageEnvelope(String label) {
    super(label, new FixedParameter(0), new FixedParameter(1), new FixedParameter(1000));
    setPeriod(period);
    setLooping(false);
    addParameter("period", period);
    internalStages.add(new Stage(0, 0, 1, true, false));
    internalStages.add(new Stage(1, 1, 1, false, true));
    updateStages();
  }

  private void updateStages() {
    Stage previous = null;
    for (Stage stage : this.internalStages) {
      stage.previous = previous;
      stage.next = null;
      if (previous != null) {
        previous.next = stage;
      }
      previous = stage;
    }
  }

  public MultiStageEnvelope removeStage(Stage stage) {
    if (!stage.initial && !stage.last) {
      this.internalStages.remove(stage);
      updateStages();
      this.monitor.toggle();
    }
    return this;
  }

  public Stage addStage(Stage stage) {
    for (int i = 1; i < this.internalStages.size(); ++i) {
      if (stage.basis <= this.internalStages.get(i).basis) {
        this.internalStages.add(i, stage);
        updateStages();
        this.monitor.toggle();
        break;
      }
    }
    return stage;
  }

  public Stage addStage(double basis, double value) {
    basis = LXUtils.constrain(basis, 0, 1);
    value = LXUtils.constrain(value, 0, 1);
    return addStage(new Stage(basis, value));
  }

  @Override
  protected double computeNormalizedValue(double deltaMs, double basis) {
    return compute(basis);
  }

  @Override
  protected double computeNormalizedBasis(double basis, double normalizedValue) {
    throw new UnsupportedOperationException("Cannot invert MultiStageEnvelope");
  }

  @Override
  public double compute(double basis) {
    double prevValue = 0;
    double prevBasis = 0;
    for (Stage stage : this.internalStages) {
      if (basis < stage.basis) {
        double relativeBasis = (basis - prevBasis) / (stage.basis - prevBasis);
        return LXUtils.lerp(prevValue, stage.value, Math.pow(relativeBasis, stage.shape));
      } else if (basis == stage.basis) {
        return stage.value;
      }
      prevBasis = stage.basis;
      prevValue = stage.value;
    }
    return 0;
  }

  @Override
  public double invert(double value, double basisHint) {
    throw new UnsupportedOperationException("Custom staged envelopes are not invertable");
  }

  private static final String KEY_STAGES = "stages";

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);
    obj.add(KEY_STAGES, LXSerializable.Utils.toArray(lx, this.internalStages));
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    super.load(lx, obj);
    if (obj.has(KEY_STAGES)) {
      JsonArray stageArr = obj.getAsJsonArray(KEY_STAGES);
      int index = 0;
      for (JsonElement stageElem : stageArr) {
        JsonObject stageObj = stageElem.getAsJsonObject();
        if (index == 0) {
          this.internalStages.get(0).load(lx, stageObj);
        } else if (index == stageArr.size() - 1) {
          this.internalStages.get(this.internalStages.size()-1).load(lx, stageObj);
        } else {
          addStage(new Stage(stageObj));
        }
        ++index;
      }
    }
    this.monitor.toggle();
  }

}
