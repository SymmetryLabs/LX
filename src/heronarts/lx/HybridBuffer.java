package heronarts.lx;

import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXColor16;

/**
 * Manages a pair of color buffers (one in 8-bit color and one in 16-bit color),
 * converting data between them automatically as needed.  Clients should call
 * markBufferModified() or markBuffer16Modified() after writing into either buffer;
 * then getBuffer() and getBuffer16() will convert data when necessary.  The buffers
 * are allocated on demand; if only one is used, no memory is wasted on the other.
 */
public class HybridBuffer {
    private LX lx = null;
    private LXBuffer buffer = null;
    private LXBuffer16 buffer16 = null;

    enum State {BUFFER_IS_NEWER, BUFFER16_IS_NEWER, IN_SYNC}
    private State state = State.IN_SYNC;

    public HybridBuffer(LX lx) {
        this.lx = lx;
    }

    /** Gets the 8-bit buffer, converting from the 16-bit buffer if needed. */
    public LXBuffer getBuffer() {
        if (buffer == null) {
            buffer = new ModelBuffer(lx);
            if (buffer16 != null) {
                state = State.BUFFER16_IS_NEWER;
            }
        }
        if (state == State.BUFFER16_IS_NEWER) {
            sync();
        }
        return buffer;
    }

    /** Gets the 16-bit buffer, converting from the 8-bit buffer if needed. */
    public LXBuffer16 getBuffer16() {
        if (buffer16 == null) {
            buffer16 = new ModelBuffer16(lx);
            if (buffer != null) {
                state = State.BUFFER_IS_NEWER;
            }
        }
        if (state == State.BUFFER_IS_NEWER) {
            sync();
        }
        return buffer16;
    }

    /** Sets the 8-bit buffer where data will be read from and written to. */
    public void setBuffer(LXBuffer buffer) {
        this.buffer = buffer;
        if (buffer != null) {
            markBufferModified();
        }
    }

    /** Sets the 16-bit buffer where data will be read from and written to. */
    public void setBuffer16(LXBuffer16 buffer16) {
        this.buffer16 = buffer16;
        if (buffer16 != null) {
            markBuffer16Modified();
        }
    }

    /** Marks the 8-bit buffer as containing newer data than the 16-bit buffer. */
    public void markBufferModified() {
        state = State.BUFFER_IS_NEWER;
    }

    /** Marks the 16-bit buffer as containing newer data than the 8-bit buffer. */
    public void markBuffer16Modified() {
        state = State.BUFFER16_IS_NEWER;
    }

    /**
     * If both the 8-bit and 16-buffers exist, ensures that they have matching data,
     * performing a conversion if necessary that gives priority to whichever has newer data.
     */
    public void sync() {
        if (buffer != null && buffer16 != null) {
            switch (state) {
                case BUFFER_IS_NEWER:
                    LXColor.intsToLongs(buffer.getArray(), buffer16.getArray16());
                    break;
                case BUFFER16_IS_NEWER:
                    LXColor16.longsToInts(buffer16.getArray16(), buffer.getArray());
                    break;
            }
            state = State.IN_SYNC;
        }
    }
}