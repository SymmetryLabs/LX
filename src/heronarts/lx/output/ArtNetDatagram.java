/**
 * Copyright 2016- Mark C. Slee, Heron Arts LLC
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

package heronarts.lx.output;

import heronarts.lx.model.LXFixture;

public class ArtNetDatagram extends LXDatagram {

  private final static int DEFAULT_UNIVERSE = 0;
  private final static int ARTNET_HEADER_LENGTH = 18;
  private final static int ARTNET_PORT = 6454;
  private final static int SEQUENCE_INDEX = 12;

  private final int[] pointIndices;

  private boolean sequenceEnabled = false;

  private byte sequence = 1;

  public ArtNetDatagram(LXFixture fixture) {
    this(fixture, DEFAULT_UNIVERSE);
  }

  public ArtNetDatagram(int[] indices) {
    this(indices, DEFAULT_UNIVERSE);
  }

  public ArtNetDatagram(LXFixture fixture, int universeNumber) {
    this(LXOutput.fixtureToIndices(fixture), universeNumber);
  }

  public ArtNetDatagram(int[] indices, int universeNumber) {
    this(indices, 3*indices.length, universeNumber);
  }

  public ArtNetDatagram(LXFixture fixture, int dataLength, int universeNumber) {
    this(LXOutput.fixtureToIndices(fixture), dataLength, universeNumber);
  }

  public ArtNetDatagram(int[] indices, int dataLength, int universeNumber) {
    super(ARTNET_HEADER_LENGTH + dataLength + (dataLength % 2));

    this.pointIndices = indices;
    setPort(ARTNET_PORT);

    this.buffer[0] = 'A';
    this.buffer[1] = 'r';
    this.buffer[2] = 't';
    this.buffer[3] = '-';
    this.buffer[4] = 'N';
    this.buffer[5] = 'e';
    this.buffer[6] = 't';
    this.buffer[7] = 0;
    this.buffer[8] = 0x00; // ArtDMX opcode
    this.buffer[9] = 0x50; // ArtDMX opcode
    this.buffer[10] = 0; // Protcol version
    this.buffer[11] = 14; // Protcol version
    this.buffer[12] = 0; // Sequence
    this.buffer[13] = 0; // Physical
    this.buffer[14] = (byte) (universeNumber & 0xff); // Universe LSB
    this.buffer[15] = (byte) ((universeNumber >>> 8) & 0xff); // Universe MSB
    this.buffer[16] = (byte) ((dataLength >>> 8) & 0xff);
    this.buffer[17] = (byte) (dataLength & 0xff);

    // Ensure zero rest of buffer
    for (int i = ARTNET_HEADER_LENGTH; i < this.buffer.length; ++i) {
     this.buffer[i] = 0;
    }
  }

  /**
   * Set whether to increment and send sequence numbers
   *
   * @param sequenceEnabled true if sequence should be incremented and transmitted
   * @return this
   */
  public ArtNetDatagram setSequenceEnabled(boolean sequenceEnabled) {
    this.sequenceEnabled = sequenceEnabled;
    return this;
  }

  @Override
  public void onSend(int[] colors) {
    copyPoints(colors, this.pointIndices, ARTNET_HEADER_LENGTH);

    if (this.sequenceEnabled) {
      if (++this.sequence == 0) {
        ++this.sequence;
      }
      this.buffer[SEQUENCE_INDEX] = this.sequence;
    }
  }
}
