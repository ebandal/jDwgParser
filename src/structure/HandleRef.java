package structure;

import java.util.Arrays;

public class HandleRef {
    byte code;
    byte counter;
    byte[] handles;

    @Override
    public String toString() {
        return "HandleRef [code=" + code + ", counter=" + counter + ", handles=" + Arrays.toString(handles) + "]";
    }
    
}
