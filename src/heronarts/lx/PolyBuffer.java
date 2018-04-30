package heronarts.lx;

import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXColor16;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages a set of color buffers of various color spaces, converting color values
 * between them automatically as needed.  Clients should call markModified()
 * after writing into any buffer; then getBuffer() will convert the data when necessary.
 * Buffers are allocated on demand; if only one is used, no memory is wasted on any others.
 */
public class PolyBuffer {
  public enum Space {RGB8, RGB16};

  private LX lx = null;
  private Map<Space, Buffer> buffers = new EnumMap<>(Space.class);
  private Set<Space> freshSpaces = EnumSet.noneOf(Space.class);

  public PolyBuffer(LX lx) {
    this.lx = lx;
  }

  public Buffer getBuffer(Space space) {
    updateBuffer(space);
    return buffers.get(space);
  }

  public Object getArray(Space space) {
    return getBuffer(space).getArray();
  }

  public void markModified(Space space) {
    assert buffers.get(space) != null;
    freshSpaces = EnumSet.of(space);
  }

  public Space getFreshSpace() {
    for (Space space : freshSpaces) {
      return space;
    }
    return DEFAULT_SPACE;
  }

  public boolean isFresh(Space space) {
    return freshSpaces.contains(space);
  }

  protected Buffer createBuffer(Space space) {
    switch (space) {
      case RGB8:
        return new ModelBuffer(lx);
      case RGB16:
        return new ModelLongBuffer(lx);
      default:
        return null;
    }
  }

  protected void updateBuffer(Space space) {
    if (!isFresh(space)) {
      if (buffers.get(space) == null) {
        buffers.put(space, createBuffer(space));
      }
      Buffer buffer = buffers.get(space);
      switch (space) {
        case RGB8:
          if (isFresh(Space.RGB16)) {
            LXColor16.longsToInts((long[]) getArray(Space.RGB16),
                (int[]) buffer.getArray());
          }
          break;
        case RGB16:
          if (isFresh(Space.RGB8)) {
            LXColor.intsToLongs((int[]) getArray(Space.RGB8),
                (long[]) buffer.getArray());
          }
          break;
      }
      freshSpaces.add(space);
    }
  }

  // The methods below provide support for old-style use of the PolyBuffer
  // as if it were only an RGB8 buffer.

  @Deprecated
  private static final Space DEFAULT_SPACE = Space.RGB8;

  @Deprecated
  public Buffer getBuffer() {
    return getBuffer(DEFAULT_SPACE);
  }

  @Deprecated
  public int[] getArray() {
    return (int[]) getBuffer(DEFAULT_SPACE).getArray();
  }

  @Deprecated
  public void setBuffer(Buffer buffer) {
    buffers.clear();
    buffers.put(DEFAULT_SPACE, buffer);
    if (buffer != null) {
      markModified(DEFAULT_SPACE);
    }
  }

  @Deprecated
  public void markModified() {
    markModified(DEFAULT_SPACE);
  }

  @Deprecated
  public void sync() {
    for (Space space : Space.values()) {
      if (buffers.get(space) != null && !isFresh(space)) {
        updateBuffer(space);
      }
    }
  }
}
