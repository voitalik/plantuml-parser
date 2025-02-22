public record CircleRecord(double r, int x, int y) implements AutoCloseable {

    @Override
    public void close() throws Exception {

    }
}
