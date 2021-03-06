package webnf.ws;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Decoder<Frame> {

    public static final byte OPCODE_CONT = 0x0;
    public static final byte OPCODE_TEXT = 0x1;
    public static final byte OPCODE_BINARY = 0x2;
    public static final byte OPCODE_CLOSE = 0x8;
    public static final byte OPCODE_PING = 0x9;
    public static final byte OPCODE_PONG = 0xA;
    public final static short CLOSE_NORMAL = 1000;
    public final static short CLOSE_AWAY = 1001;
    public final static short CLOSE_MESG_BIG = 1009;

    public enum State {
        FRAME_START, READ_LENGTH, READ_2_LENGTH, READ_8_LENGTH, MASKING_KEY, PAYLOAD, CORRUPT
    }

    public final int maxSize;
    public final FrameCallsite<Frame> frameCreator;

    private State state = State.FRAME_START;
    private byte[] content;
    private int idx = 0;

    private int payloadLength;
    private int payloadRead;
    private int maskingKey;
    private boolean finalFlag;
    private int opcode = -1;
    private int fragmentedOpCode = -1;
    private int framePayloadIndex; // masking per frame

    // 8 bytes are enough
    // protect against long/short/int are not fully received
    private ByteBuffer tmpBuffer = ByteBuffer.allocate(8);

    public Decoder(int maxSize, FrameCallsite<Frame> frameCreator) {
        this.maxSize = maxSize;
        this.frameCreator = frameCreator;
    }

    private boolean isAvailable(ByteBuffer src, int length) {
        while (tmpBuffer.position() < length) {
            if (src.hasRemaining()) {
                tmpBuffer.put(src.get());
            } else {
                return false;
            }
        }
        tmpBuffer.flip(); // for read
        return true;
    }

    public Frame update(ByteBuffer buffer){
        while (buffer.hasRemaining()) {
            switch (state) {
                case FRAME_START:
                    byte b = buffer.get(); // FIN, RSV, OPCODE
                    finalFlag = (b & 0x80) != 0;

                    int tmpOp = b & 0x0F;
                    if (opcode != -1 && tmpOp != opcode) {
                        // TODO ping frame in fragmented text frame
                        throw new RuntimeException("opcode mismatch: pre: " + opcode + ", now: "
                                + tmpOp);
                    }
                    opcode = tmpOp;
                    state = State.READ_LENGTH;
                    break;
                case READ_LENGTH:
                    b = buffer.get(); // MASK, PAYLOAD LEN 1
                    boolean masked = (b & 0x80) != 0;
                    if (!masked) {
                        throw new RuntimeException("unmasked client to server frame");
                    }
                    payloadLength = b & 0x7F;
                    if (payloadLength == 126) {
                        state = State.READ_2_LENGTH;
                    } else if (payloadLength == 127) {
                        state = State.READ_8_LENGTH;
                    } else {
                        state = State.MASKING_KEY;
                    }
                    break;
                case READ_2_LENGTH:
                    if (isAvailable(buffer, 2)) {
                        payloadLength = tmpBuffer.getShort() & 0xFFFF;
                        tmpBuffer.clear();
                        if (payloadLength < 126) {
                            throw new RuntimeException(
                                    "invalid data frame length (not using minimal length encoding)");
                        }
                        state = State.MASKING_KEY;
                    }
                    break;
                case READ_8_LENGTH:
                    if (isAvailable(buffer, 8)) {
                        long length = tmpBuffer.getLong();
                        tmpBuffer.clear();
                        // if negative, that too big, drop it.
                        if (length < 65536) {
                            throw new RuntimeException("invalid data frame length. most significant bit is not zero or length fits in unsigned short.");
                        }
                        abortIfTooLarge(length);
                        payloadLength = (int) length;
                        state = State.MASKING_KEY;
                    }
                    break; // wait for more data from TCP
                case MASKING_KEY:
                    if (isAvailable(buffer, 4)) {
                        maskingKey = tmpBuffer.getInt();
                        tmpBuffer.clear();
                        if (content == null) {
                            content = new byte[payloadLength];
                        } else if (payloadLength > 0) {
                            abortIfTooLarge(content.length + payloadLength);
                            /*
                             * TODO if an attacker sent many fragmented frames, only one
                             * byte of data per frame, server end up reallocate many
                             * times. may not be a problem
                             */
                            // resize
                            content = Arrays.copyOf(content, content.length + payloadLength);
                        }
                        framePayloadIndex = 0; // reset
                        state = State.PAYLOAD;
                        // No break. since payloadLength can be 0
                    } else {
                        break; // wait for more data from TCP
                    }
                case PAYLOAD:
                    int read = Math.min(buffer.remaining(), payloadLength - payloadRead);
                    if (read > 0) {
                        buffer.get(content, idx, read);

                        byte[] mask = ByteBuffer.allocate(4).putInt(maskingKey).array();
                        for (int i = 0; i < read; i++) {
                            content[i + idx] = (byte) (content[i + idx] ^ mask[(framePayloadIndex + i) % 4]);
                        }

                        payloadRead += read;
                        idx += read;
                    }
                    framePayloadIndex += read;

                    // all read (this frame)
                    if (payloadRead == payloadLength) {
                        if (finalFlag) {
                            if (fragmentedOpCode > 0)
                                opcode = fragmentedOpCode;
                            return frameCreator.invoke((byte) opcode, content);
                        } else {
                            state = State.FRAME_START;
                            payloadRead = 0;
                            if (opcode > 0)
                                fragmentedOpCode = opcode;
                            opcode = -1;
                        }
                    }
                    break;
            }
        }
        return null; // wait for more bytes
    }

    public void abortIfTooLarge(long length) {
        if (length > maxSize) { // drop if message is too big
            throw new RuntimeException("Max payload length " + maxSize + ", get: " + length);
        }
    }

    public void reset() {
        state = State.FRAME_START;
        payloadRead = 0;
        idx = 0;
        opcode = -1;
        content = null;
        framePayloadIndex = 0;
    }
}
