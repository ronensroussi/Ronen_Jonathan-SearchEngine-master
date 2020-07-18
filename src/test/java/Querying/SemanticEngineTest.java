package Querying;

import Querying.Semantics.SemanticEngine;
import de.jungblut.glove.impl.GloveBinaryWriter;
import de.jungblut.glove.impl.GloveTextReader;
import de.jungblut.glove.util.StringVectorPair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

class SemanticEngineTest {

   // private static final String pathToGloveFilesFolder = "C:\\Users\\John\\Downloads\\infoRetrieval\\GloVe\\customVectors";
    private static final String pathToGloveFilesFolder =" C:\\Users\\ronen\\IdeaProjects\\Ronen_Jonathan-SearchEngine\\resources\\Word2Vec";


    @Test
    void showSynonyms() throws IOException {
        SemanticEngine se = new SemanticEngine(pathToGloveFilesFolder, 10);
        List<String> neighbors;
//        System.out.println("---- dog ----");
//        neighbors = se.getNearestNeighborsStrings("dog");
//        for (String neighbor: neighbors
//             ) {
//            System.out.println(neighbor);
//        }
//        System.out.println("---- puppy ----");
//        neighbors = se.getNearestNeighborsStrings("puppy");
//        for (String neighbor: neighbors
//             ) {
//            System.out.println(neighbor);
//        }
//        System.out.println("---- poppy ----");
//        neighbors = se.getNearestNeighborsStrings("poppy");
//        for (String neighbor: neighbors
//             ) {
//            System.out.println(neighbor);
//        }
//        System.out.println("---- day ----");
//        neighbors = se.getNearestNeighborsStrings("day");
//        for (String neighbor: neighbors
//                ) {
//            System.out.println(neighbor);
//        }
//        System.out.println("---- DAY ----");
//        neighbors = se.getNearestNeighborsStrings("DAY");
//        for (String neighbor: neighbors
//                ) {
//            System.out.println(neighbor);
//        }
//        System.out.println("---- england ----");
//        neighbors = se.getNearestNeighborsStrings("england");
//        for (String neighbor: neighbors
//                ) {
//            System.out.println(neighbor);
//        }
//        System.out.println("---- England ----");
//        neighbors = se.getNearestNeighborsStrings("England");
//        for (String neighbor: neighbors
//                ) {
//            System.out.println(neighbor);
//        }
//        System.out.println("---- Britain ----");
//        neighbors = se.getNearestNeighborsStrings("Britain");
//        for (String neighbor: neighbors
//                ) {
//            System.out.println(neighbor);
//        }
        System.out.println("---- israel ----");
        neighbors = se.getNearestNeighborsStrings("israel");
        for (String neighbor: neighbors
                ) {
            System.out.println(neighbor);
        }
        System.out.println("---- russia ----");
        neighbors = se.getNearestNeighborsStrings("russia");
        for (String neighbor: neighbors
                ) {
            System.out.println(neighbor);
        }
        System.out.println("---- china ----");
        neighbors = se.getNearestNeighborsStrings("china");
        for (String neighbor: neighbors
                ) {
            System.out.println(neighbor);
        }
        System.out.println("---- petroleum ----");
        neighbors = se.getNearestNeighborsStrings("petroleum");
        for (String neighbor: neighbors
                ) {
            System.out.println(neighbor);
        }
        System.out.println("---- winners ----");
        neighbors = se.getNearestNeighborsStrings("winners");
        for (String neighbor: neighbors
                ) {
            System.out.println(neighbor);
        }

    }

}