package Querying.Semantics;


import de.jungblut.datastructure.KDTree;
import de.jungblut.datastructure.KDTree.VectorDistanceTuple;
import de.jungblut.glove.GloveRandomAccessReader;
import de.jungblut.glove.impl.CachedGloveBinaryRandomAccessReader;
import de.jungblut.glove.impl.GloveBinaryRandomAccessReader;
import de.jungblut.glove.impl.GloveBinaryReader;
import de.jungblut.glove.util.StringVectorPair;
import de.jungblut.math.DoubleVector;
import javafx.util.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;


public class SemanticEngine {

    private GloveRandomAccessReader reader;
    private final KDTree<String> tree;
    /**
     * SemanticEngine will get the top kNeighbors neighbors for words.
     */
    private final int kNeighbors;

    /**
     * constructor
     * @param kNeighbors SemanticEngine will get the top kNeighbors neighbors for words.
     * @param pathToGloveFilesFolder path to the folder where the binary GloVe vector files are stored.
     * @throws IOException if an error occurs while trying to get the binary GloVe files.
     */
    public SemanticEngine(String pathToGloveFilesFolder, int kNeighbors) throws IOException {
        this.kNeighbors = kNeighbors;
        // load GloVe vectors
        Path dir = Paths.get(pathToGloveFilesFolder);

        reader = new CachedGloveBinaryRandomAccessReader(
                new GloveBinaryRandomAccessReader(dir), 100l);
        tree = new KDTree<>();

        try (Stream<StringVectorPair> stream = new GloveBinaryReader().stream(dir)) {
            stream.forEach((pair) -> {
                tree.add(pair.vector, pair.value);
            });

        }
        // Balancing the KD tree...
        tree.balanceBySort();

    }

    /**
     * finds the nearest semantic neighbors (synonyms) for the given word.
     * @param word the word to find neighbors for.
     * @return the nearest semantic neighbors (synonyms) for the given word. If word is null, returns null. If no neighbors were found or word wasn't found, returns an empty list.
     * @throws IOException if an error occurs while retrieving the vector for word from the GloVe files.
     */
    public List<Pair<String, Double>> getNearestNeighbors(String word) throws IOException {
        if(word == null) return null;

        DoubleVector v = reader.get(word);
        if (v == null) {
            // word does'nt exist in vectors
            return new ArrayList<Pair<String, Double>>();
        } else {
            List<VectorDistanceTuple<String>> nearestNeighbours = tree
                    .getNearestNeighbours(v, kNeighbors + 1); //+1 because the same word will also be retrieved.

            // sort and remove the one that we searched for
            Collections.sort(nearestNeighbours, Collections.reverseOrder());
            // the best hit is usually the same item with distance 0
            if (nearestNeighbours.get(0).getValue().equals(word)) {
                nearestNeighbours.remove(0);
            }

            ArrayList<Pair<String, Double>> neighborStrings = new ArrayList<>(nearestNeighbours.size());
            for (VectorDistanceTuple<String> tuple : nearestNeighbours) {
                neighborStrings.add(new Pair<String, Double>(tuple.getValue(), tuple.getDistance()));
            }
            return neighborStrings;
        }
    }



    /**
     * finds the nearest semantic neighbors (synonyms) for the given words.
     * @param words the words to find neighbors for.
     * @return the nearest semantic neighbors (synonyms) for the given words. If the list is null, returns null. If no neighbors were found, returns an empty list.
     * @throws IOException if an error occurs while retrieving the vector for word from the GloVe files.
     */
    public List<Pair<String, Double>> getNearestNeighbors(List<String> words) throws IOException {
        if (words == null) return null;
        ArrayList<Pair<String, Double>> neighborStrings = new ArrayList<>();
        for (String word: words
                ) {
            neighborStrings.addAll(getNearestNeighbors(word));
        }
        return neighborStrings;
    }

    /**
     * finds the nearest semantic neighbors (synonyms) for the given words.
     * @param words the words to find neighbors for.
     * @return the nearest semantic neighbors (synonyms) for the given words. If the set is null, returns null. If no neighbors were found, returns an empty list.
     * @throws IOException if an error occurs while retrieving the vector for word from the GloVe files.
     */
    public List<Pair<String, Double>> getNearestNeighbors(Set<String> words) throws IOException {
        if (words == null) return null;
        ArrayList<Pair<String, Double>> neighborStrings = new ArrayList<>();
        for (String word: words
                ) {
            neighborStrings.addAll(getNearestNeighbors(word));
        }
        return neighborStrings;
    }


    /**
     * finds the nearest semantic neighbors (synonyms) for the given word.
     * @param word the word to find neighbors for.
     * @return the nearest semantic neighbors (synonyms) for the given word. If word is null, returns null. If no neighbors were found or word wasn't found, returns an empty list.
     * @throws IOException if an error occurs while retrieving the vector for word from the GloVe files.
     */
    public List<String> getNearestNeighborsStrings(String word) throws IOException {
        if(word == null) return null;

        DoubleVector v = reader.get(word);
        if (v == null) {
            // word does'nt exist in vectors
            return new ArrayList<String>();
        } else {
            List<VectorDistanceTuple<String>> nearestNeighbours = tree
                    .getNearestNeighbours(v, kNeighbors + 1); //+1 because the same word will also be retrieved.

            // sort and remove the one that we searched for
            Collections.sort(nearestNeighbours, Collections.reverseOrder());
            // the best hit is usually the same item with distance 0
            if (nearestNeighbours.get(0).getValue().equals(word)) {
                nearestNeighbours.remove(0);
            }

            ArrayList<String> neighborStrings = new ArrayList<>(nearestNeighbours.size());
            for (VectorDistanceTuple<String> tuple : nearestNeighbours) {
                neighborStrings.add(tuple.getValue());
            }
            return neighborStrings;
        }
    }



}
