package cz.esw.serialization.json;

/**
 * @author Marek Cuchý
 */
public enum DataType {
    DOWNLOAD(1), UPLOAD(2), PING(3);
    final int number;

    DataType(int type) {
        this.number = type;
    }

    public int getNumber() {
        return number;
    }

    public static DataType getDataType(int enumNumber) {
        return switch (enumNumber) {
            case 0 -> DOWNLOAD;
            case 1 -> UPLOAD;
            case 2 -> PING;
            default -> throw new IllegalStateException("Unexpected value: " + enumNumber);
        };
    }
}
