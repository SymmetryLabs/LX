/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
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

package heronarts.lx.model;

import heronarts.lx.transform.LXVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model of points in a simple grid.
 */
public class GridModel extends LXModel {

  public static class Metrics {
    public final int width;

    public final int height;

    private float xSpacing = 1;

    private float ySpacing = 1;

    private LXVector origin = new LXVector(0, 0, 0);

    public Metrics(int width, int height) {
      this.width = width;
      this.height = height;
    }

    public Metrics setXSpacing(float xSpacing) {
      this.xSpacing = xSpacing;
      return this;
    }

    public Metrics setYSpacing(float ySpacing) {
      this.ySpacing = ySpacing;
      return this;
    }

    public Metrics setSpacing(float xSpacing, float ySpacing) {
      this.xSpacing = xSpacing;
      this.ySpacing = ySpacing;
      return this;
    }

    public float xSpacing() {
      return this.xSpacing();
    }

    public float ySpacing() {
      return this.ySpacing();
    }

    public Metrics setOrigin(float x, float y, float z) {
      this.origin.set(x, y, z);
      return this;
    }

    public Metrics setOrigin(LXVector v) {
      this.origin.set(v);
      return this;
    }
  }

  /**
   * All the rows in this model
   */
  public final List<LXModel> rows;

  /**
   * All the columns in this model
   */
  public final List<LXModel> columns;

  /**
   * Metrics for the grid
   */
  public final Metrics metrics;

  /**
   * Width of the grid
   */
  public final int width;

  /**
   * Height of the grid
   */
  public final int height;

  /**
   * Spacing on the x-axis
   */
  public final float xSpacing;

  /**
   * Spacing on the y-axis
   */
  public final float ySpacing;

  /**
   * Constructs a grid model with the given metrics
   *
   * @param metrics Metrics
   */
  public GridModel(Metrics metrics) {
    super(new Fixture(metrics));

    this.metrics = metrics;
    this.width = metrics.width;
    this.height = metrics.height;
    this.xSpacing = metrics.xSpacing;
    this.ySpacing = metrics.ySpacing;

    List<LXModel> _rows = new ArrayList<LXModel>();
    for (int y = 0; y < height; ++y) {
      List<LXPoint> row = new ArrayList<LXPoint>();
      for (int x = 0; x < width; ++x) {
        row.add(points.get(x + y * this.width));
      }
      _rows.add(new LXModel(row));
    }
    this.rows = Collections.unmodifiableList(_rows);

    List<LXModel> _columns = new ArrayList<LXModel>();
    for (int x = 0; x < width; ++x) {
      List<LXPoint> column = new ArrayList<LXPoint>();
      for (int y = 0; y < height; ++y) {
        column.add(points.get(x + y * this.width));
      }
      _columns.add(new LXModel(column));
    }
    this.columns = Collections.unmodifiableList(_columns);


  }

  /**
   * Constructs a uniformly spaced grid model of the given size with all pixels
   * apart by a unit of 1.
   *
   * @param width Width in pixels
   * @param height Height in pixels
   */
  public GridModel(int width, int height) {
    this(width, height, 1, 1);
  }

  /**
   * Constructs a grid model with specified x and y spacing
   *
   * @param width Number of nodes in x dimension
   * @param height Number of nodes in y dimension
   * @param xSpacing Spacing of nodes in x dimension
   * @param ySpacing Spacing of nodes in y dimension
   */
  public GridModel(int width, int height, float xSpacing, float ySpacing) {
    this(new Metrics(width, height).setSpacing(xSpacing, ySpacing));
  }

  private static class Fixture extends LXAbstractFixture {
    private Fixture(Metrics metrics) {
      for (int y = 0; y < metrics.height; ++y) {
        for (int x = 0; x < metrics.width; ++x) {
          this.points.add(new LXPoint(
            metrics.origin.x + x * metrics.xSpacing,
            metrics.origin.y + y * metrics.ySpacing,
            metrics.origin.z
          ));
        }
      }
    }
  }

}
