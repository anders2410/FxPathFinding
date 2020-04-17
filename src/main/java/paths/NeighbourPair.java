package paths;

import java.util.Objects;

public class NeighbourPair {
    private int inNeighbour;
    private int outNeighbour;

    public NeighbourPair(int inNeighbour, int outNeighbour) {
        this.inNeighbour = inNeighbour;
        this.outNeighbour = outNeighbour;
    }

    public int getInNeighbour() {
        return inNeighbour;
    }

    public int getOutNeighbour() {
        return outNeighbour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NeighbourPair that = (NeighbourPair) o;
        return inNeighbour == that.inNeighbour &&
                outNeighbour == that.outNeighbour;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inNeighbour, outNeighbour);
    }
}
