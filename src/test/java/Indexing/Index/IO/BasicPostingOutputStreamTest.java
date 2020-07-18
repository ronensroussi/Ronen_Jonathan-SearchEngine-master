package Indexing.Index.IO;

import Indexing.Index.Posting;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class BasicPostingOutputStreamTest {

    static final String outputPath = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\BasicPostingOutputStreamTest";
    static RandomAccessFile raf;
    IPostingOutputStream out;


    public BasicPostingOutputStreamTest() throws IOException {

        try {
            raf = new RandomAccessFile(outputPath, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        raf.setLength(0);
        raf.close();

        try {
            out = new BasicPostingOutputStream(outputPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Test
    void writePostings() throws IOException {
        List<Posting> postings = new ArrayList<>(2);
        Posting p1 = new Posting(66, (short)13, true, false);
        Posting p2 = new Posting(129, (short)28, false, false);

        out.write(postings);
        out.write(postings);

        Scanner scanner = new Scanner(new File(outputPath));

        while (scanner.hasNext()){
            System.out.println(scanner.nextLine());
        }

        out.close();
    }

//    @Test
//    void writeln() throws IOException {
//        Posting p  = new Posting("doc01", (short)13, (short)78, (short)1900, "Beer Sheva", "hebrew", true, false);
//
//        clean();
//
//        out.writeln(p);
//        out.writeln(p);
//        out.writeln(p);
//
//        raf.seek(0);
//        String line = raf.readLine();
//        while (null != line){
//            System.out.println(line);
//            System.out.println("length: " + line.length());
//            line = raf.readLine();
//        }
//
//        out.close();
//    }
//
//    @Test
//    void write() throws IOException {
//        Posting p  = new Posting("dco01", (short)13, (short)78, (short)1900, "Beer Sheva", "hebrew", true, false);
//
//        clean();
//
//        out.write(p);
//        out.write(p);
//        out.writeln(p);
//
//        Scanner scanner = new Scanner(new File(outputPath));
//
//        while (scanner.hasNext()){
//            String line = scanner.nextLine();
//            System.out.println(line);
//            System.out.println("length: " + line.length());
//        }
//
//        out.close();
//    }


    @Test
    void testTime() throws IOException {
        int numPostings = 100;
        ArrayList<Posting> postingsForOneTerm = new ArrayList<>(numPostings);
        Posting p1 = new Posting(66, (short)13, true, false);
        Posting p2 = new Posting(129, (short)28, false, false);
        for (int i = 0; i <50 ; i++) {
            postingsForOneTerm.add(i, p1);
        }
        for (int i = 50; i <numPostings ; i++) {
            postingsForOneTerm.add(i, p2);
        }

        long startTime = System.currentTimeMillis();

        for (int j = 0; j <100000 ; j++) {
            out.write(postingsForOneTerm);
        }

        long time = (System.currentTimeMillis() - startTime);

        System.out.println("time for 100,000 terms with 100 postings each (ms): " + (time));
        System.out.println("time for 100,000 terms with 100 postings each, fifty times (m): " + (time)*50/1000/60);

    }
}