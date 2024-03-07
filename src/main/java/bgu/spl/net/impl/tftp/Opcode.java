package bgu.spl.net.impl.tftp;

public enum Opcode {
    RRQ,
    WRQ,
    DATA,
    ACK,
    ERROR,
    DIRQ,
    LOGRQ,
    DELRQ,
    BCAST,
    DISC,
    UNDEFINED;

    public static Opcode getByOrdinal(byte value) {
        int index = Byte.toUnsignedInt(value);
        if (index >= 0 && index < values().length) {
            return values()[index];

        }
        return UNDEFINED;
    }

    public static Byte getByte(Opcode opcode){
        int num = opcode.ordinal();
        return (byte) num;
    }

}
