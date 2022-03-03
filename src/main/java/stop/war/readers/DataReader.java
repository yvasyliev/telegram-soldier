package stop.war.readers;

import java.io.IOException;

@FunctionalInterface
public interface DataReader<T> {
    T read(String path) throws IOException;
}
