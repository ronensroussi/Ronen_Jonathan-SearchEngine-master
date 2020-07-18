package Indexing.Index.IO;

import Indexing.Index.Posting;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;


/**
 * a basic implementation for writing postings to disk, meant for debugging and demonstration.
 * it is meant to write postings in a user-readable format, and is not meant to be efficient or to be read by any input stream.
 */
public class BasicPostingOutputStream extends APostingOutputStream  {

    //ADMINISTRATIVE

    /**
     * if the file doesn't exist, creates it.
     * if the file exists, clears it!
     * @param pathToFile
     * @throws IOException
     */
    public BasicPostingOutputStream(String pathToFile) throws IOException {
        super(pathToFile);
    }


    //OUTPUT

    protected String postingToStringTuple(Posting posting){
        return posting.toString()+";";
    }

    protected void postingToStringTuple(Posting posting, StringBuilder result){
        result.append(posting.toString());
        result.append(';');
    }


    @Override
    public long write(@NotNull List<Posting> postings) throws NullPointerException, IOException {
        long startIdx = getCursor();
        StringBuilder output = new StringBuilder();
        for (Posting p : postings
             ) {
            postingToStringTuple(p, output);
        }
        output.append('\n');

        byte[] outBytes = output.toString().getBytes();
        postingsFile.write(outBytes);

        filePointer += outBytes.length;

        return startIdx;
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {
        postingsFile.close();
    }
}
