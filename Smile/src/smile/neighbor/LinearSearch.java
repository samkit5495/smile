/******************************************************************************
 *                   Confidential Proprietary                                 *
 *         (c) Copyright Haifeng Li 2011, All Rights Reserved                 *
 ******************************************************************************/

package smile.neighbor;

import java.util.List;
import smile.math.distance.Distance;
import smile.sort.HeapSelect;

/**
 * Brute force linear nearest neighbor search. This simplest solution computes
 * the distance from the query point to every other point in the database,
 * keeping track of the "best so far". There are no search data structures to
 * maintain, so linear search has no space complexity beyond the storage of
 * the database. Although it is very simple, naive search outperforms space
 * partitioning approaches (e.g. K-D trees) on higher dimensional spaces.
 *
 * @param <T> the type of data objects.
 *
 * @author Haifeng Li
 */
public class LinearSearch<T> implements NearestNeighborSearch<T,T>, KNNSearch<T,T>, RNNSearch<T,T> {

    /**
     * The dataset of search space.
     */
    private T[] data;
    /**
     * The distance function used to determine nearest neighbors.
     */
    private Distance<T> distance;
    /**
     * Whether to exclude query object self from the neighborhood.
     */
    private boolean identicalExcluded = true;

    /**
     * Constructor. By default, query object self will be excluded from search.
     */
    public LinearSearch(T[] dataset, Distance<T> distance) {
        this.data = dataset;
        this.distance = distance;
    }

    @Override
    public String toString() {
        return String.format("Linear Search (%s)", distance);
    }

    /**
     * Set if exclude query object self from the neighborhood.
     */
    public void setIdenticalExcluded(boolean excluded) {
        identicalExcluded = excluded;
    }

    /**
     * Get whether if query object self be excluded from the neighborhood.
     */
    public boolean isIdenticalExcluded() {
        return identicalExcluded;
    }

    @Override
    public Neighbor<T,T> nearest(T q) {
        T neighbor = null;
        int index = -1;
        double dist = Double.MAX_VALUE;
        for (int i = 0; i < data.length; i++) {
            if (q == data[i] && identicalExcluded) {
                continue;
            }

            double d = distance.d(q, data[i]);

            if (d < dist) {
                neighbor = data[i];
                index = i;
                dist = d;
            }
        }

        return new SimpleNeighbor<T>(neighbor, index, dist);
    }

    @Override
    public Neighbor<T,T>[] knn(T q, int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("Invalid k: " + k);
        }

        if (k > data.length) {
            throw new IllegalArgumentException("Neighbor array length is larger than the dataset size");
        }

        SimpleNeighbor<T> neighbor = new SimpleNeighbor<T>(null, 0, Double.MAX_VALUE);
        @SuppressWarnings("unchecked")
        SimpleNeighbor<T>[] neighbors = (SimpleNeighbor<T>[]) java.lang.reflect.Array.newInstance(neighbor.getClass(), k);
        HeapSelect<Neighbor<T,T>> heap = new HeapSelect<Neighbor<T,T>>(neighbors);
        for (int i = 0; i < k; i++) {
            heap.add(neighbor);
            neighbor = new SimpleNeighbor<T>(null, 0, Double.MAX_VALUE);
        }

        for (int i = 0; i < data.length; i++) {
            if (q == data[i] && identicalExcluded) {
                continue;
            }

            double dist = distance.d(q, data[i]);
            Neighbor<T,T> datum = heap.peek();
            if (dist < datum.distance) {
                datum.distance = dist;
                datum.index = i;
                datum.key = data[i];
                datum.value = data[i];
                heap.heapify();
            }
        }

        heap.sort();
        return neighbors;
    }

    @Override
    public void range(T q, double radius, List<Neighbor<T,T>> neighbors) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Invalid radius: " + radius);
        }

        for (int i = 0; i < data.length; i++) {
            if (q == data[i] && identicalExcluded) {
                continue;
            }

            double d = distance.d(q, data[i]);

            if (d <= radius) {
                neighbors.add(new SimpleNeighbor<T>(data[i], i, d));
            }
        }
    }
}
