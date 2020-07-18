package Indexing.Index.IO;

import java.io.*;
import java.util.Map;

/**
 * a basic implementation for writing city information (Maps of Integer to int[])to disk, meant for debugging and demonstration.
 * it is meant to write information in a user-readable format, and is not meant to be efficient or to be read by any input stream.
 */
public class BasicIntToIntArrayMapOutputStream {


    long filePointer = 0;
    BufferedWriter postingsFile;

    /**
     * if the file doesn't exist, creates it.
     * if the file exists, clears it!
     * @param pathToFile
     * @throws IOException
     */
    public BasicIntToIntArrayMapOutputStream(String pathToFile) throws IOException {
        this.postingsFile = new BufferedWriter(new PrintWriter(pathToFile));
    }

    public long getCursor() {
        return filePointer;
    }

    public long write(Map<Integer, int[]> map) throws NullPointerException, IOException {
        long startIdx = getCursor();

        for (Map.Entry<Integer, int[]> entry: map.entrySet()
             ) {
            postingsFile.write("Key=" + entry.getKey() + ", ");
            postingsFile.write("values=[");
            int [] arr = entry.getValue();
            for (int i = 0; i < arr.length ; i++) {
                postingsFile.write("" + String.valueOf(arr[i]) + (i != arr.length-1 ? ',' : ""));
            }
            postingsFile.write("] ; ");
        }
        postingsFile.write('\n');

        return startIdx;
    }


    public void flush() throws IOException {
        postingsFile.flush();
    }

    public void close() throws IOException {
        postingsFile.close();
    }

}
