package webnf.ws;

import java.io.DataOutputStream;
import java.io.IOException;

public class Util {

    public static void encodeFrame(DataOutputStream stream, byte opcode, byte[] data, int offset, int length) throws IOException {
        byte b0 = 0;
        b0 |= 1 << 7; // FIN
        b0 |= opcode;
        stream.writeByte(b0);

        if (length <= 125) {
            stream.writeByte(length);
        } else if (length <= 0xFFFF) {
            stream.writeByte(126);
            stream.writeShort(length);
        } else {
            stream.writeByte(127);
            stream.writeLong(length);
        }
        stream.write(data, offset, length);
    }

    public static void encodeFrame(DataOutputStream stream, byte opcode, byte[] data) throws IOException {
        encodeFrame(stream, opcode, data, 0, data.length);
    }
}
