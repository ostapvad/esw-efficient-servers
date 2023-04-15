package cz.esw.serialization.json;

/**
 * @author Marek Cuchý
 */
public enum DataType {
    DOWNLOAD(1), UPLOAD(2), PING(3);
    final int type;

    DataType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
