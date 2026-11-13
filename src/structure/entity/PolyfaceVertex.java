package structure.entity;

/** VERTEX of a polyface mesh : either a point or a face record. */
public class PolyfaceVertex extends Entity {
    public double[] point;                      // vertex, or null
    public int[] indexes;                       // face record, or null
}
