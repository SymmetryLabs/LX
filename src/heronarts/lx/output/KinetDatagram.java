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

package heronarts.lx.output;

/**
 * A datagram implementing the Kinet protocol, used by Color Kinetics devices.
 * These datagrams have a header followed by 512 bytes of color data. A port
 * number on the output device is specified, distinct from the UDP port. For
 * instance, an sPDS-480 has 16 outputs.
 */
public class KinetDatagram extends LXDatagram {

  private final static int DMXOUT_HEADER_LENGTH = 21;
  private final static int HEADER_LENGTH = 24;
  private final static int DATA_LENGTH = 512;
  private final static int PACKET_LENGTH = HEADER_LENGTH + DATA_LENGTH;

  private final static int KINET_PORT = 6038;

  private final int[] pointIndices;

  public enum Version {
    DMXOUT,
    PORTOUT
  };

  private final Version version;

  /**
   * Constructs a datagram that sends on the given kinet supply output port
   *
   * @param kinetPort Number of the output port on the kinet power supply
   * @param indices A list of the point indices that should be sent on this port
   */
  public KinetDatagram(int kinetPort, int[] indices) {
    this(kinetPort, indices, Version.PORTOUT);
  }

  public KinetDatagram(int kinetPort, int[] indices, Version version) {
    super(PACKET_LENGTH);
    setPort(KINET_PORT);

    this.pointIndices = indices;
    this.version = version;

    // Kinet Header
    this.buffer[0] = (byte) 0x04;
    this.buffer[1] = (byte) 0x01;
    this.buffer[2] = (byte) 0xdc;
    this.buffer[3] = (byte) 0x4a;

    switch (this.version) {
    case PORTOUT:
      this.buffer[4] = (byte) 0x01;
      this.buffer[5] = (byte) 0x00;
      this.buffer[6] = (byte) 0x08;
      this.buffer[7] = (byte) 0x01;
      this.buffer[8] = (byte) 0x00;
      this.buffer[9] = (byte) 0x00;
      this.buffer[10] = (byte) 0x00;
      this.buffer[11] = (byte) 0x00;
      this.buffer[12] = (byte) 0xff;
      this.buffer[13] = (byte) 0xff;
      this.buffer[14] = (byte) 0xff;
      this.buffer[15] = (byte) 0xff;

      // Port number
      this.buffer[16] = (byte) kinetPort;

      // Maybe a checksum? 0x00 works fine
      this.buffer[17] = (byte) 0x00;
      this.buffer[18] = (byte) 0x00;
      this.buffer[19] = (byte) 0x00;
      this.buffer[20] = (byte) 0x00;

      // Total # of ports on controller? (irrelevant)
      this.buffer[21] = (byte) 0x02;

      // Unused
      this.buffer[22] = (byte) 0x00;
      this.buffer[23] = (byte) 0x00;
      break;

    case DMXOUT:
      this.buffer[4] = (byte) 0x01;
      this.buffer[5] = (byte) 0x00;

      // Type number (DMXOUT)
      this.buffer[6] = (byte) 0x01;
      this.buffer[7] = (byte) 0x01;

      // Sequence number
      this.buffer[8] = (byte) 0x00;
      this.buffer[9] = (byte) 0x00;
      this.buffer[10] = (byte) 0x00;
      this.buffer[11] = (byte) 0x00;

      // Unused header
      this.buffer[12] = (byte) 0x00;
      this.buffer[13] = (byte) 0x00;
      this.buffer[14] = (byte) 0x00;
      this.buffer[15] = (byte) 0x00;

      // Universe
      this.buffer[16] = (byte) 0xff;
      this.buffer[17] = (byte) 0xff;
      this.buffer[18] = (byte) 0xff;
      this.buffer[19] = (byte) 0xff;

      // One byte to start data
      this.buffer[20] = (byte) 0x00;
      break;
    }
  }

  @Override
  public void onSend(int[] colors) {
    switch (this.version) {
    case DMXOUT:
      copyPoints(colors, this.pointIndices, DMXOUT_HEADER_LENGTH);
      break;
    case PORTOUT:
      copyPoints(colors, this.pointIndices, HEADER_LENGTH);
      break;
    }
  }
}
