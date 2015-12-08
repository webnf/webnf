package webnf.ws;

/**
 * Created by herwig on 9/24/15.
 */
public interface FrameCallsite<T> {
    T invoke(byte opcode, byte[] content);
}
