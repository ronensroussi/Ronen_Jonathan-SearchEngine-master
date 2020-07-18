package Indexing.Index.IO;


import java.io.*;
import java.util.*;

/**
 * reads postings from a file. meant to read postings in the format that {@link IntToIntArrayMapOutputStream IntToIntArrayMapOutputStream} writes.
 */
public class IntToIntArrayMapInputStream {
    private RandomAccessFile postingsFile;

    public IntToIntArrayMapInputStream(String pathToFile) throws FileNotFoundException {
        this.postingsFile = new RandomAccessFile(pathToFile, "r");
    }

    /**
     * reads a Map< Integer, int[]> in the format defined by {@link IntToIntArrayMapOutputStream IntToIntArrayMapOutputStream}
     * @param pointerToStartOfMap
     * @return
     * @throws IOException
     */
    public Map<Integer, int[]> readIntegerArraysMap(long pointerToStartOfMap) throws IOException {
        postingsFile.seek(pointerToStartOfMap);

        byte[] b_arr_numIntsToRead = new byte[4];
        postingsFile.read(b_arr_numIntsToRead);

        int i_numIntsToRead = readFourBytesAsInt(b_arr_numIntsToRead, 0);

        byte[] b_arr_dataIn =  new byte[i_numIntsToRead * 4];
        postingsFile.seek(pointerToStartOfMap+4);
        postingsFile.read(b_arr_dataIn);

        int[] i_arr_dataIn = new int[i_numIntsToRead];
        int arrNum = 0;
        for (int i = 0; i < b_arr_dataIn.length ; i += 4) {
            i_arr_dataIn[arrNum] = readFourBytesAsInt(b_arr_dataIn, i);
            arrNum++;
        }

        Map<Integer, int[]> res = new HashMap<>(arrNum-1);

        for (int i = 0; i < i_arr_dataIn.length ;) {
            int key = i_arr_dataIn[i];
            i++;

            int valueArraySize = i_arr_dataIn[i];
            i++;
            int[] value = Arrays.copyOfRange(i_arr_dataIn, i, i+valueArraySize);
            i += valueArraySize;

            res.put(key, value);
        }

        return res;
    }

    private static int readFourBytesAsInt(byte[] input, int offset) throws IOException {
        return  (input[offset]<<24) & 0xff000000|
                (input[offset+1]<<16) & 0x00ff0000|
                (input[offset+2]<< 8) & 0x0000ff00|
                (input[offset+3]) & 0x000000ff;
    }

    public void close(){
        try {
            postingsFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
