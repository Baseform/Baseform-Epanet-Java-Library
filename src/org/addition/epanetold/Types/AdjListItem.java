package org.addition.epanetold.Types;

/* NODE ADJACENCY LIST ITEM */
// Sadjlist
public class AdjListItem {
    private int node;

    public AdjListItem(int node, int link) {
        this.node = node;
        this.link = link;
    }

    private int link;

    public int getNode() {
        return node;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public int getLink() {
        return link;
    }

    public void setLink(int link) {
        this.link = link;
    }
}
