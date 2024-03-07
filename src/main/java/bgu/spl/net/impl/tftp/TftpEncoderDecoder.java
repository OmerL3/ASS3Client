package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private final int size = 1 << 9;
    private byte[] bytes = new byte[size]; //start with 512 allegedly
    private OpcodeOperations opcodeOp;
    private int len = 0;


    public byte[] decodeNextByte(byte nextByte) {
        if (len <= 1) {
            if (len == 1) {
                opcodeOp = new OpcodeOperations(nextByte);
            }
        } else {
            if (opcodeOp.shouldWaitForZeroByte()) {
                if (nextByte == 0) {
                    return popBytes();
                }
            } else {
                return popBytes();
            }
        }
        pushByte(nextByte);
        return null;

    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }

    private byte[] popBytes() {
        byte[] result = bytes;
        len = 0;
        bytes = new byte[size];
        return result;
    }

    public byte[] encode(String op, String name) {
        OpcodeOperations opc = new OpcodeOperations(op);
        byte[] temp = opc.getInResponseFormat();
        byte[] encoded;
        if (name != null){
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            encoded = new byte[2+nameBytes.length];
            encoded[0]=temp[0];
            encoded[1]=temp[1];
            System.arraycopy(nameBytes,0,encoded,2,encoded.length);
        }
        else{
            encoded = temp;
        }
        return encode(encoded);
    }

    @Override
    public byte[] encode(byte[] msg) {
        byte[] encodedMessage = new byte[msg.length + 1];
        if (msg[2] == 6 || msg[2] == 10) return encodedMessage;
        System.arraycopy(msg, 0, encodedMessage, 0, msg.length);
        encodedMessage[msg.length] = 0;
        return encodedMessage;
    }
}
