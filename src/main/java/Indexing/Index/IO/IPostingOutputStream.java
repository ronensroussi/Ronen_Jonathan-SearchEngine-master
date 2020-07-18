package Indexing.Index.IO;

import Indexing.Index.Posting;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * describes functions for writing postings to a file.
 */
public interface IPostingOutputStream{

    /**
     *
     * @return a pointer to the index of the next byte to be written.
     */
    long getCursor();

    /**
     * writes all the postings in {@code postings} and then ends the line.
     * @param postings  - an array of postings to write.
     * @return - the index where the first byte of the first posting was written.
     * @throws NullPointerException - if {@code postings} contains a null pointer
     */
    long write(@NotNull List<Posting> postings) throws NullPointerException, IOException;

    /**
     * flushes the buffer
     * @throws IOException
     */
    void flush() throws IOException;

    void close() throws IOException;
}
