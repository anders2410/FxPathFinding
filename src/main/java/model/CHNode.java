package model;

public class CHNode extends Node {

    private boolean contracted;
    private int importance;
    private int contractedNeighbours;

    public CHNode(int index, double longitude, double latitude) {
        super(index, longitude, latitude);
        contracted = false;
        importance = 0;
        contractedNeighbours = 0;
    }

    public CHNode(Node node) {
        super(node);
        contracted = false;
        importance = 0;
        contractedNeighbours = 0;
    }

    public boolean isContracted() {
        return contracted;
    }

    public void setContracted(boolean contracted) {
        this.contracted = contracted;
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public int getContractedNeighbours() {
        return contractedNeighbours;
    }

    public void setContractedNeighbours(int contractedNeighbours) {
        this.contractedNeighbours = contractedNeighbours;
    }
}
