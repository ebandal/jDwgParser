package decode;

public interface DecodeCallback {
    public void onDecoded(String name, Object value, int retBitOffset);
}
