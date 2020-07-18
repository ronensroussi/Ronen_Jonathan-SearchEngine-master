package Indexing.DocumentProcessing;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runnable.
 * Takes Documents, tokenizes and parses them.
 * Meant to work in a producer consumer architecture, however, it can be run in a serial manner by using {@link #parseOneDocument(Document)}  parseOneDocument}.
 * This object's state does not change as a result of parsing any number of documents.
 */
public class Parse implements Runnable{
    public static boolean debug = false;
    private static final boolean addComponentPartsOfCompoundWord = true;
    public boolean useStemming;
    private HashSet<String> stopWords;
    private BlockingQueue<Document> sourceDocumentsQueue;
    private BlockingQueue<TermDocument> sinkTermDocumentQueue;
    private String currString = "";
    private HashMap<String, String> months;
    private TokenType currType;
    private boolean bIteratorHasNext = true;
    private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {{
        put("^\\d{8}$", "yyyyMMdd");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
        put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
        put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
        put("^\\d{12}$", "yyyyMMddHHmm");
        put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
        put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
        put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
        put("^\\d{14}$", "yyyyMMddHHmmss");
        put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
        put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
        put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss");
        put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
        put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
        put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");
        put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy HH:mm:ss");
    }};

    private AtomicInteger docSerialId;

    //          Administrative

    /**
     * @param stopWords - a set of stopwords to ignore when parsing. if a term is generated when parsing and it consists of just a stopword, it will be eliminated.
     *                  the set is copied to a local copy.
     * @param sourceDocumentsQueue - a blocking queue of documents to parse. End of queue will be marked by a "poison" Document with all fields set to null.
     * @param sinkTermDocumentQueue - a blocking queue to be filled with TermDocuments. End of queue will be marked by a "poison" TermDocument with all Term fields set to null.
     */
    public Parse(@NotNull HashSet<String> stopWords,@NotNull  BlockingQueue<Document> sourceDocumentsQueue,@NotNull  BlockingQueue<TermDocument> sinkTermDocumentQueue, boolean useStemming) {
        this.useStemming = useStemming;
        this.stopWords = new HashSet<>(stopWords);
        this.sourceDocumentsQueue = sourceDocumentsQueue;
        this.sinkTermDocumentQueue = sinkTermDocumentQueue;
        docSerialId = new AtomicInteger(0);
        this.months = new HashMap<String, String>(24);
        months.put("JANUARY", "01"); months.put("January", "01");
        months.put("FEBRUARY", "02");months.put("February", "02");
        months.put("MARCH", "03");months.put("March", "03");
        months.put("APRIL", "04");months.put("April", "04");
        months.put("MAY", "05");months.put("May", "05");
        months.put("JUNE", "06");months.put("June", "06");
        months.put("JULY", "07");months.put("July", "07");
        months.put("AUGUST", "08");months.put("August", "08");
        months.put("SEPTEMBER", "09");months.put("September", "09");
        months.put("OCTOBER", "10");months.put("October", "10");
        months.put("NOVEMBER", "11");months.put("November", "11");
        months.put("DECEMBER", "12");months.put("December", "12");

    }

    /**
     * parsing thread main loop.
     * takes Documents, tokenizes and parses them into terms. does not perform stemming.
     * End of queue will be marked by a "poison" TermDocument with null docID.
     */
    private void parse() throws InterruptedException {

        boolean done = false;
        while (!done) { //extract from buffer until poison element is encountered
            Document currDoc = sourceDocumentsQueue.take();
            if (null == currDoc.getText()) done=true; //end of files (poison element)
            else if (!currDoc.getText().isEmpty()){
                sinkTermDocumentQueue.put(parseOneDocument(currDoc));
            }
        }
        TermDocument poison = new TermDocument(-1, null, null);
        sinkTermDocumentQueue.put(poison);

    }

    /**
     * fully parses a single Document and returns a Term Document.
     * Should be used directly when wanting to parse in a serial manner, rather than in a separate thread.
     * @param doc - the Document to parse.
     * @return - a parsed TermDocument.
     */
    public TermDocument parseOneDocument(@NotNull Document doc){

        String[] originalFields = doc.getAllParsableFields();
        List<Term>[] parsedFields = new List[originalFields.length];

        //TESTING
        if(debug) System.out.println("---------- Parsing document " + doc.getDocId());

        for (int i = 0; i < originalFields.length; i++) {
            parsedFields[i] = parseWorker(tokenize(originalFields[i]));
        }

        Date docDate = parseDocDate(doc.getDate());

        return new TermDocument(docSerialId.getAndIncrement(), doc, docDate, parsedFields);
    }

    //          Tokenizing

    /**
     * Tokenizes the strings within the given String.
     * @param string - the string to tokenize
     * @return a list of strings (tokens).
     */
    public List<String> tokenize(String string){
//        final String splitterRegex = "[\t-&(-,.-/:-@\\x5B-`{-~]"; //marks chars to split on. with '.' '$'
        List<String> lTokens = new ArrayList<>();
        int from = 0;
        int to = 0;
        int length = string.length();

        boolean foundLetters = false;
        boolean foundDigits = false;

        while (to < length) {

            //check alphanumeric
            if(isLetter(string.charAt(to))){
                foundLetters = true;
            }
            else if((string.charAt(to)>='0' && string.charAt(to)<='9')){
                foundDigits = true;
            }
            //split if alphanumeric
            if(foundLetters && foundDigits){
                lTokens.add(string.substring(from, to));
                from = to;
                foundDigits = false;
                foundLetters = false;
            }

            // split and keep delimiter if delimiter
            if(isDelimiter(string.charAt(to))){
                //add token before delimiter
                lTokens.add(string.substring(from, to));
                from = to;
                to++;
                //add the delimiter
                //TODO only keep delimiters that have semantic value.
                lTokens.add(string.substring(from, to));
                from = to;
            }
            else
                to++;
        }

        lTokens.add((string.substring(from,to)));

        return tokenizeSecondPass(lTokens);
    }

    private boolean isDelimiter(char c){
//        return ("" + c).matches("[\t-&(-,.-/:-@\\x5B-`{-~]");
        return (c <= '&') || (c >= '(' && c <= '/')|| (c >= ':' && c <= '@')
                || (c >= '[' && c <= '`') || (c >= '{' && c <= '~');
    }

    /**
     * helper funtion for {@code tokenize} which cleans up empty strings, and separates or cleans leftover delimiters.
     * removes delimiters stuck to end of strings.
     * @param textAsTokens - a list of tokenized strings to clean up
     * @return - a cleaned up list of tokens.
     */
    private ArrayList<String> tokenizeSecondPass(List<String> textAsTokens) {
        ArrayList<String> listOfTokens = new ArrayList<>(textAsTokens.size()/2);

        for (String string: textAsTokens
             ) {
            //clean up empty strings and strings that only contain a delimiter
            if( string.length() == 1 && isProtectedChar(string.charAt(0)) ) //TODO incorporate into tokenize (avoid creating delimiter tokens that will be thrown away)
                listOfTokens.add(string);
            else if(string.length() > 1){
                //removes  ' in words.
                //TODO possible small performance hotspot. can maybe avoid this? incorporate into tokenize?
                listOfTokens.add(string.replace("\'", ""));
            }
        }


        return listOfTokens;
    }

    private static boolean isProtectedChar(char c){
        return (isSymbol(c) || isWhitespace(c) || (c>='0' && c<='9') || (isLetter(c)));
        //TODO optimize by reversing this statement? (check that it is not a trash char like ',')
    }

    public static boolean isWhitespace(char c){
        return c == ' ' || c == '\n' || c == '\t';
    }
    public static boolean isSymbol(char c){
        return c == '-' || c == '.' ||  c == '$' || c == '%' || c == '/' || c == '\'';
    }
    public static boolean isLetter(char c){ return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');}
    public static boolean isNumeral(char c){ return (c>='0' && c<='9');}

    //          Parsing

    /**
     * takes a list of String tokens and parses them into Terms.
     * @param lTokens - tokens to parse
     * @return - list of parsed terms
     */
    public List<Term> parseWorker(List<String> lTokens){
        List<Term> lTerms = new ArrayList<>();
        ListIterator<String> iterator = lTokens.listIterator(0);

        safeIterateAndCheckType(iterator);

        /*
        goes over the list of tokens and parses them into Term s.
        always assumes the parsing process has left currString on the next token to be parsed.
         */
        while (bIteratorHasNext) {

            //             ROOT CASES
            // whitespace
            if(currType == TokenType.WHITESPACE) {
                safeIterateAndCheckType(iterator); //whitespace, do nothing
            }
            // WORD ->
            else if (TokenType.WORD == currType){
                String term = parseWord(iterator, currString, new StringBuilder(), lTerms).toString();
                if(!term.isEmpty()){
                    commitTermToList(term, lTerms);
                }
            }
            // NUMBER ->
            else if(currType == TokenType.NUMBER){
                commitTermToList(parseNumber(iterator, new StringBuilder(), false, lTerms).toString()  , lTerms);
            }
            // $ -> NUMBER(price) ->
            else if(TokenType.SYMBOL == currType && currString.equals("$")){
                safeIterateAndCheckType(iterator);
                if(TokenType.NUMBER == currType){
                    commitTermToList(parseNumber(iterator, new StringBuilder(), true, lTerms).toString()   , lTerms);
                }
                else if(TokenType.WHITESPACE == currType){
                    safeIterateAndCheckType(iterator);
                    if(TokenType.NUMBER == currType){
                        commitTermToList(parseNumber(iterator, new StringBuilder(), true, lTerms).toString()   , lTerms);
                    }
                }
                else{ //a word starting with a dollar sign
                    currString = '$'+currString;
                    String term = parseWord(iterator, currString, new StringBuilder(), lTerms).toString();
                    if(!term.isEmpty()){
                        commitTermToList(term, lTerms);
                    }
                }
            }

            else //if completely failed to identify a token (unlikely)
                safeIterateAndCheckType(iterator);

        }

        if(debug){
            System.out.println("-----------start parse output-------------");
            for (Term t:
                    lTerms) {
                System.out.println(t);
            }
            System.out.println("-----------end parse output-------------");
        }

        bIteratorHasNext = true;
        return lTerms;
    }

    /**
     * parses a number. always assigns currString to the next token to be parsed (wasn't successfully parsed here).
     * @param iterator iterator from which to get strings to work on
     * @param result - a string builder to add the result onto. may be empty or contain prior information.
     * @param has$ - indicates that the number should be treated as a price, regardless of the next token.
     *                should be set to true if a '$' was encountered before the number. should be set to false if unsure.
     * @return - the same string builder given in {@code result}, with parsed number, and any relevant tokens like "Dollars" or 'M'.
     */
    private StringBuilder parseNumber(@NotNull ListIterator<String> iterator,@NotNull StringBuilder result,
                                      boolean has$, List<Term> lTerms){
        long kmbtMultiplier = 1;
        String decimals = null;
        StringBuilder unformattedNumber = new StringBuilder(); //start concatenating number parts to build full number
        boolean isFractional = false;
        boolean isPercent = false;
        boolean isPrice = has$;
        boolean isCompound = false;
        String dateMonth = null;


        buildNumber(iterator, unformattedNumber);

        // NUMBER -> . -> NUMBER
        if(currType == TokenType.SYMBOL && currString.equals(".")){ //decimal point or end of line
            safeIterateAndCheckType(iterator);
            if(currType == TokenType.NUMBER){ // decimal digits
                decimals = currString;
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
            }
        }
        // NUMBER -> " " + NUMBER/NUMBER
        else if(currType == TokenType.WHITESPACE) {
            isFractional = tryParseFraction(iterator, unformattedNumber);
        }

        // NUMBER -> %
        if(currType == TokenType.SYMBOL && currString.equals("%")){
            isPercent = true;
            safeIterateAndCheckType(iterator);
        }
        // NUMBER -> "-"
        else if(TokenType.SYMBOL == currType && currString.equals("-")){
            // NUMBER + "-" -> WORD/NUMBER
            result.append(unformattedNumber.toString());
            concatTokensSeparatedByDashes(iterator,result, lTerms, unformattedNumber.toString(), addComponentPartsOfCompoundWord);
            if(!result.toString().equals(unformattedNumber.toString())) isCompound = true;
        }
        // NUMBER ->  " "
        else if(currType == TokenType.WHITESPACE && !currString.equals("\n")){
            safeIterateAndCheckType(iterator);

            // NUMBER + " " -> WORD
            if(currType == TokenType.WORD && (currString.equalsIgnoreCase("Thousand") || currString.equalsIgnoreCase("K")) ){
                kmbtMultiplier = 1000;
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
            }
            else if(currType == TokenType.WORD && (currString.equalsIgnoreCase("Million") || currString.equalsIgnoreCase("M")) ) {
                kmbtMultiplier = 1000000;
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
            }
            else if(currType == TokenType.WORD &&
                    (currString.equalsIgnoreCase("Billion") || currString.equalsIgnoreCase("B") || currString.equals("bn")) ){
                kmbtMultiplier = 1000000000;
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
            }
            else if(currType == TokenType.WORD && (currString.equalsIgnoreCase("Trillion") || currString.equalsIgnoreCase("T")) ){
                kmbtMultiplier = 1000000000000L;
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
            }
            // NUMBER + " " -> DOLLAR (and not NUMBER M/B/K DOLLARS)
            else if( has$ || (currType == TokenType.WORD && (currString.equalsIgnoreCase("Dollar") || currString.equalsIgnoreCase("Dollars")))){
                if (!has$){ //string contains a form of "dollars", not a '$'
                    safeIterateAndCheckType(iterator); // skip that "dollars"
                }
                isPrice = true; // mark term as price term
                currType = TokenType.classify(currString);
            }
            // NUMBER + " " -> PERCENT
            else if(currType == TokenType.WORD && (currString.equals("percent") || currString.equals("percentage"))){
                isPercent = true;
                safeIterateAndCheckType(iterator); //end of parsing number
            }

            // NUMBER + " " + K/M/B -> U.S. Dollars  or  NUMBER + " " + K/M/B -> DOLLARS
            // there is a " " again because of parsing the K/M/B
            if(currType == TokenType.WHITESPACE ){
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
                // NUMBER + " " + K/M/B -> U.S. Dollars
                if (currType == TokenType.WORD && currString.equals("U")){
                    isPrice = tryParseUSDollars(iterator);
                }
                // NUMBER + " " + K/M/B -> DOLLARS
                else if( has$ || (currType == TokenType.WORD && (currString.equalsIgnoreCase("Dollar") || currString.equalsIgnoreCase("Dollars")))){
                    if (!has$){ //string contains a form of "dollars", not a '$'
                        safeIterateAndCheckType(iterator); // skip that "dollars"
                    }
                    isPrice = true;
                }
                //end of number parsing. string at currString has not yet been successfully parsed
            }
            // NUMBER + " " -> U.S. Dollars
            else if (currType == TokenType.WORD && currString.equals("U")){
                isPrice = tryParseUSDollars(iterator);
            }
            // NUMBER -> MONTH
            else if(TokenType.WORD == currType && months.containsKey(currString)){
                dateMonth = currString;
                safeIterateAndCheckType(iterator);
            }
        }
        // "$" + NUMBER -> NUMBER Dollars
        else if(has$){
            isPrice = true;
        }
        // NUMBER -> NUMBER + K/M/B
        else if (currType == TokenType.WORD){
            if(currString.equalsIgnoreCase("K") ){
                kmbtMultiplier = 1000;
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
            }
            else if(currString.equalsIgnoreCase("M") ) {
                kmbtMultiplier = 1000000;
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
            }
            else if(currString.equalsIgnoreCase("B") || currString.equals("bn") ){
                kmbtMultiplier = 1000000000;
                safeIterateAndCheckType(iterator);
                currType = TokenType.classify(currString);
            }
        }

        if(!isCompound) finalizeNumber(unformattedNumber, result, kmbtMultiplier, decimals, isPrice, isPercent, isFractional, dateMonth);
        return result;
    }

    /**
     * iterates while concatenating number parts into a single numeric string
     * @param iterator - iterator over the list of tokens
     * @param unformattedNumber - result will be appended here.
     */
    private void buildNumber(@NotNull ListIterator<String> iterator, StringBuilder unformattedNumber) {
        while(currType == TokenType.NUMBER){
            unformattedNumber.append(currString);
            safeIterateAndCheckType(iterator); //may result in classifying the same string twice
        }
    }

    /**
     * iterates over the iterator once, and sets currString to the String from {@code iterator}. sets {@see #currType currType} to the TokenType for current string.
     * if iterator doesn't have next, sets {@see #currString currString} to "\n", and sets {@see #bIteratorHashNext bIteratorHashNext} to false to stop parseWorker.
     * @param iterator
     */
    private void safeIterateAndCheckType(ListIterator<String> iterator){
        if(iterator.hasNext()){
            currString = iterator.next();
            currType = TokenType.classify(currString);
        }
        else{
            currString = "\n";
            currType = TokenType.WHITESPACE;
            bIteratorHasNext = false;
        }
    }

    /**
     * assumes the previously encountered string was " ".
     * if successfull, also appends the " " at the start of the result.
     * * if unsuccessful, reverts to currString == " ", with iterator.next() pointing to the next token after " ".
     * @param iterator
     * @param result
     */
    private boolean tryParseFraction(@NotNull ListIterator<String> iterator,@NotNull StringBuilder result) {
        // assumes the previously encountered string was " ".
        safeIterateAndCheckType(iterator);
        currType = TokenType.classify(currString);
        if(currType == TokenType.NUMBER){
            StringBuilder firstNumber = new StringBuilder();
            buildNumber(iterator, firstNumber);
            if(currString.equals("/")){
                safeIterateAndCheckType(iterator);
                if(TokenType.NUMBER == currType){
                    result.append(' ');
                    result.append(firstNumber.toString());
                    result.append('/');
                    result.append(currString);
                    safeIterateAndCheckType(iterator);
                    return true;
                }
                else rewindIterator(iterator, 3); //these dont work without the "else" and i dont know why
            }
            else rewindIterator(iterator, 2);//these dont work without the "else" and i dont know why
        }
        else rewindIterator(iterator, 1);//these dont work without the "else" and i dont know why
        return false;
    }

    /**
     * assumes the previously encountered string was "U".
     * if successful, also appends the "U" at the start of the result.
     * if unsuccessful, reverts to currString == "U", with iterator.next() pointing to the next token after "U".
     * @param iterator
     */
    private boolean tryParseUSDollars(@NotNull ListIterator<String> iterator) {
        //assumes the previously encountered string was "U".
        safeIterateAndCheckType(iterator);
        currType = TokenType.classify(currString);
        if(currString.equals(".")){
            safeIterateAndCheckType(iterator);
            if (currString.equals("S")){
                safeIterateAndCheckType(iterator);
                if(currString.equals(".")){
                    safeIterateAndCheckType(iterator);
                    if(currString.equals(" ")){
                        safeIterateAndCheckType(iterator);
                        currType = TokenType.classify(currString);
                        if((currType == TokenType.WORD && (currString.equalsIgnoreCase("Dollar") || currString.equalsIgnoreCase("Dollars")))){
                            safeIterateAndCheckType(iterator); // move to next because parsing this term is idone
                            return true;
                        }
                        else rewindIterator(iterator, 5);
                    }
                    else rewindIterator(iterator, 4);
                }
                else rewindIterator(iterator, 3);
            }
            else rewindIterator(iterator, 2);
        }
        else rewindIterator(iterator, 1);
        return false;
    }

    /**
     * after all the information regarding the number being parsed has been collected, finalize it into a properly formatted number.
     * may lose some rightmost digits if a number is both a fraction and very large (trillions)
     * @param unformattedNumber
     * @param result formatting result will be appended onto here. will not override existing content.
     * @param kmbtMultiplier >= 1
     * @param decimals if null, will be treated as no decimals exist.
     * @param isPrice
     * @param isPercent
     * @param isFractional
     */
    private void finalizeNumber(@NotNull StringBuilder unformattedNumber,@NotNull StringBuilder result, long kmbtMultiplier,
                                String decimals, boolean isPrice, boolean isPercent, boolean isFractional, String dateMonth)
    {
        boolean isDate = dateMonth != null;
        String stringDollars = " Dollars";

        if(isFractional){
            result.append(unformattedNumber);
            if(isPercent){
                result.append('%');
            }
            else if (isPrice){
                result.append(stringDollars);
            }
        }
        else {
            if(null != decimals){
                unformattedNumber.append('.');
                unformattedNumber.append(decimals);
            }
            float number = Float.parseFloat(unformattedNumber.toString());
            number *= kmbtMultiplier;

            if(isPrice){
                String MKB = "";
                if (number<1000){ // too small to matter
                }
                else if (number>=1000 && number<1000000){ //thousands
                    number /= 1000;
                    MKB = " K";
                }
                else { //millions or larger
                    number /= 1000000;
                    MKB= " M";
                }

                appendWhileTrimmingDecimals(result, number);
                result.append(MKB);
                result.append(stringDollars);

            }
            else if(isDate){
                if(unformattedNumber.length() > 2){ // number is year
                    result.append(unformattedNumber); //insert number without format
                    result.append("-");
                    result.append(months.get(dateMonth));
                }
                else if (unformattedNumber.length() == 2){ //number is day
                    result.append(months.get(dateMonth));
                    result.append("-");
                    result.append(unformattedNumber);
                }
                else{ // number is day and <=9
                    result.append(months.get(dateMonth));
                    result.append("-0");
                    result.append(unformattedNumber);
                }
            }
            else{ //regular number
                String MKB = "";
                if (number<1000){ // too small to matter
                }
                else if (number>=1000 && number<1000000){ //thousands
                    number /= 1000;
                    MKB ="K";
                }
                else if (number>=1000000 && number<1000000000){ //millions
                    number /= 1000000;
                    MKB = "M";
                }
                else { //billions or larger
                    number /= 1000000000;
                    MKB = "B";
                }

                appendWhileTrimmingDecimals(result, number);
                result.append(MKB);

                if(isPercent){
                    result.append('%');
                }
            }
        }
    }

    /**
     * append number to {@code result}.
     * Removes decimals if the number is a natural number.
     * @param result - the number will be appended here
     * @param number - the number to append.
     */
    private void appendWhileTrimmingDecimals(@NotNull StringBuilder result, float number){
//        number -= number%0.00001; //remove anything beyond the 5 digits past the decimal point
        if( number % 1 == 0 ){
            long numberWithoutDecimals = ((long)number);
            result.append(numberWithoutDecimals);
        }
        else result.append(number);
    }

    /**
     * parses a term that starts with a word.
     * @param iterator iterator over the list of tokens.
     * @param word the word to start parsing from.
     * @param result the result will be appended here.
     * @param lTerms if more than one term is parsed, it will be added here.
     * @return - if it was just an ordinary word (or stopword) result contains that word. else it contains the parsed expression.
     */
    private StringBuilder parseWord(@NotNull ListIterator<String> iterator,@NotNull String word,@NotNull StringBuilder result, List<Term> lTerms){
        // MONTH -> MM-DD
        if (months.containsKey(word)){
            String month = months.get(word);
            safeIterateAndCheckType(iterator);
            if(TokenType.WHITESPACE == currType){
                safeIterateAndCheckType(iterator);
                if(TokenType.NUMBER == currType){
                    if(currString.length() > 2){ // number is year
                        result.append(currString);
                        result.append("-");
                        result.append(month);
                    }
                    else if (currString.length() == 2){ //number is day
                        result.append(month);
                        result.append("-");
                        result.append(currString);
                    }
                    else{ // number is day and <=9
                        result.append(month);
                        result.append("-0");
                        result.append(currString);
                    }
                    safeIterateAndCheckType(iterator);
                }
                else{ // just some word
                    result.append(word);
                }
            }
            else{ // just some word
                result.append(word);
                safeIterateAndCheckType(iterator);
            }
        }
        else { //regular word or stopword
            result.append(currString); //add word to result
            List<String> lCompoundWordParts = new ArrayList<>();
            String firstToken = currString; // word might be first part of a compound word

            safeIterateAndCheckType(iterator);

            //string together words (or numbers) separated by '-'
            if(currString.equals("-")){
                concatTokensSeparatedByDashes(iterator, result, lTerms, firstToken, addComponentPartsOfCompoundWord);
            }
            // range with "between X and Y" format
            else if (result.toString().equalsIgnoreCase("Between")){
                if(tryParseBetweenXandY(iterator, result, lCompoundWordParts)){
                    // add component parts to Term list (output) if they exist
                    for (String compoundWordPart:
                            lCompoundWordParts) {
                        commitTermToList(compoundWordPart, lTerms);
                    }
                }
            }
            // acronym
            else if(firstToken.length() == 1 && currString.equals(".")){
                tryParseAcronym(iterator, result);
            }
            //honorific
            else if(firstToken.equals("mr") || firstToken.equals("ms") || firstToken.equals("mrs")){
                tryParseHonorificName(iterator, result, lTerms);
            }
            //URL
            else if(firstToken.equals("www") && currString.equals(".")){
                tryParseURL(iterator, result);
            }
        }

        return result; // if it was just an ordinary word (or stopword) result contains that word. else it contains the parsed expression.
    }

    /**
     * strings together words or numbers separated by "-". length is unbounded.
     * assumnes the firstToken has already been added to result.
     * @param iterator
     * @param result
     * @param addComponents - whether or not to add the component parts of the compound expression into {@code lTerms}.
     * @param lTerms - inserts the individual parts of the token chain into here, if {@code addComponents} is true;
     * @param firstToken - puts the individual words/numbers here, so they can also be
     */
    private void concatTokensSeparatedByDashes(@NotNull ListIterator<String> iterator, @NotNull StringBuilder result, List<Term> lTerms,
                                               String firstToken, boolean addComponents) {
        List<String> lCompoundExpressionParts = new LinkedList<>();
        lCompoundExpressionParts.add(firstToken);
        while(currString.equals("-")){
            safeIterateAndCheckType(iterator);
            if(TokenType.WORD == currType || TokenType.NUMBER == currType){
                lCompoundExpressionParts.add(currString);
                result.append("-");
                result.append(currString);
                safeIterateAndCheckType(iterator);
            }
        }
        if(addComponents && lCompoundExpressionParts.size() > 1){
            for (String compoundWordPart:
                    lCompoundExpressionParts) {
                commitTermToList(compoundWordPart, lTerms);
            }
        }
    }

    /**
     * assumes the previously encountered string was "Between" or "between", and that that string was already appended to result.
     * rewinds iterator if unsuccessful. leaves iterator on the next string after this term if successfully parsed.
     * @param iterator
     * @param result
     * @param lCompoundWordParts - puts the X and Y of "between X and Y" here.
     * @return true if successfully parsed, else returns false.
     */
    private boolean tryParseBetweenXandY(@NotNull ListIterator<String> iterator, @NotNull StringBuilder result, @NotNull List<String> lCompoundWordParts){
        // assumes the previously encountered string was " ".
        safeIterateAndCheckType(iterator);
        String firstWord = "";
        if(currType == TokenType.NUMBER){ // first part of range
            firstWord = currString;
            safeIterateAndCheckType(iterator);
            if(TokenType.WHITESPACE == currType){
                safeIterateAndCheckType(iterator);
                if(currString.equalsIgnoreCase("and")){ // and
                    safeIterateAndCheckType(iterator);
                    if(TokenType.WHITESPACE == currType){
                        safeIterateAndCheckType(iterator);
                        if(TokenType.NUMBER == currType){ //second part of range
                            result.delete(0, result.length()); // clear the "between"
                            result.append(firstWord);
                            result.append('-');
                            result.append(currString);
                            safeIterateAndCheckType(iterator);
                            return true;
                        }
                        else rewindIterator(iterator, 5);
                    }
                    else rewindIterator(iterator, 4);
                }
                else rewindIterator(iterator, 3);
            }
            else rewindIterator(iterator, 2);
        }
        else rewindIterator(iterator, 1);
        return false;
    }

    /**
     * assumes the previously encountered string was ".".
     * leaves iterator on the next string after this term if successfully parsed.
     * @param iterator
     * @param result
     * @return true if successfully parsed, else returns false.
     */
    private boolean tryParseAcronym(ListIterator<String> iterator, StringBuilder result) {
        boolean isAcronym = false;
        // assumes the previously encountered string was ".".
        while(currString.equals(".")){
            safeIterateAndCheckType(iterator);
            if(TokenType.WORD == currType && currString.length() == 1){ //a single letter word
                result.append(currString);
                safeIterateAndCheckType(iterator);
                isAcronym = true;
            }
        }
        return isAcronym;
    }

    /**
     * assumes the previously encountered string was an honorific.
     * leaves iterator on the next string after this term if successfully parsed.
     * if successful, also commits the name part as a term.
     * @param iterator
     * @param result
     * @return true if successfully parsed, else returns false.
     */
    private boolean tryParseHonorificName(ListIterator<String> iterator, StringBuilder result, List<Term> lTerms) {
        boolean isName = false;
        //skip a period and all whitespaces
        if(currString.equals(".")) safeIterateAndCheckType(iterator);
        while (TokenType.WHITESPACE == currType) safeIterateAndCheckType(iterator);
        // found a word. assume it's a name
        if(TokenType.WORD == currType){
            isName = true;
            result.append(". ");
            result.append(currString);
            commitTermToList(currString,lTerms);
            safeIterateAndCheckType(iterator);
        }
        return isName;
    }

    /**
     * assumes the previously encountered string was ".".
     * rewinds iterator if unsuccessful. leaves iterator on the next string after this term if successfully parsed.
     * @param iterator
     * @param result
     * @return true if successfully parsed, else returns false.
     */
    private boolean tryParseURL(ListIterator<String> iterator, StringBuilder result) {
        short numFields = 1;
        short numTokensPopped = 0;
        StringBuilder URL_fields = new StringBuilder();
        // assumes the previously encountered string was ".".
        while(currString.equals(".")){
            safeIterateAndCheckType(iterator);
            numTokensPopped++;
            if(TokenType.WORD == currType){
                URL_fields.append(".");
                URL_fields.append(currString);
                safeIterateAndCheckType(iterator);
                numFields++;
                numTokensPopped++;
            }
        }
        //is URL
        if(numFields>=3){
            while(currString.equals("/")){
                safeIterateAndCheckType(iterator);
                if(TokenType.WORD == currType){
                    URL_fields.append("/");
                    URL_fields.append(currString);
                    safeIterateAndCheckType(iterator);
                }
            }
            //write parsed URL to result
            result.append(URL_fields);
        }
        //not a URL, rewind
        else rewindIterator(iterator, numTokensPopped);

        return numFields>=3;
    }


    /**
     * when a string is ready to be committed as a new term, it is passed here.
     * stemming (if necessary) will be done here.
     * if the string is a stopword, it will not be committed as a new Term.
     * @param term - the term to commit.
     * @param lTerms - the list of committed Terms to add to.
     */
    private void commitTermToList(String term, List<Term> lTerms){
        if(useStemming) {
            Stemmer stemmer = new Stemmer();
            stemmer.add(term.toCharArray(), term.length());
            stemmer.stem();
            term = stemmer.toString();
            if((isLetter(term.charAt(0)) || isNumeral(term.charAt(0))) && !stopWords.contains(term.toLowerCase())) lTerms.add(filterTerms(term));
        }
        else if ((isLetter(term.charAt(0)) || isNumeral(term.charAt(0))) && !stopWords.contains(term.toLowerCase())) lTerms.add(filterTerms(term));
    }

    /**
     * limits terms to either being completely lowercase, or completely uppercase.
     * @param term - a term to filter.
     * @return - the term, after filtering and putting it in a new instance of Term.
     */
    private Term filterTerms(String term){
        if(TokenType.WORD == TokenType.classify(term)){
            term = Character.isLowerCase(term.charAt(0)) ? term.toLowerCase() : term.toUpperCase();
        }
        return new Term(term);
    }

    /**
     * iterate {@param steps} number of steps backwards on a list iterator.
     * does this by calling iter.previous() steps+1 times.
     * checks that {@param iter} has previous before each use of previous.
     * uses {@link #safeIterateAndCheckType(ListIterator)} to iterate once forward at the end of the process.
     * end result wll be iterator.nextIndex is set to iter.previousIndex +1 as it would have been if the itertor hadn't been iterated over {@param steps} times.
     * @param iter - the iterator to rewind.
     * @param steps - the number of steps to rewind the iterator.
     */
    private void rewindIterator(ListIterator<String> iter, int steps){
        for (int i = 0; i < steps; i++) {
            currString = iter.previous();
        }

        if (iter.hasPrevious())
            currString = iter.previous();
        safeIterateAndCheckType(iter);
    }

    /**
     * parses a document's date string to a Date object.
     * @param dateString a string representing the date of the document.
     * @return a Date object, or null if the string is empty or null or if the date format is unrecognized or unparsable.
     */
    private static Date parseDocDate(@Nullable String dateString){
        if(dateString == null || dateString.isEmpty()) return null; //no date
        dateString = dateString.trim(); //clean trailing or leading whitespaces
        String dateFormat = determineDateFormat(dateString);
        if(dateFormat == null){ //not a standard date format
            dateString = tryConvertCustomDateFormatToStandard(dateString); //try to convert date from yyMMdd to yyyyMMdd
            if(dateString == null) return null; //unrecognized date format
            else{
                dateFormat = "yyyyMMdd";
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        simpleDateFormat.setLenient(false); // Don't automatically convert invalid date.
        Date date = null;
        try{
            date = simpleDateFormat.parse(dateString);
        }
        //shouldn't happen, because the string has already been checked to verify validity.
        //regardless, return null if a parse exception does happen.
        catch (ParseException e){
            return null;
        }
        return date;
    }

    /**
     * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
     * format is unknown. You can simply extend DateUtil with more formats if needed.
     * @param dateString The date string to determine the SimpleDateFormat pattern for.
     * @return The matching SimpleDateFormat pattern, or null if format is unknown.
     */
    private static String determineDateFormat(String dateString) {
        for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
            if (dateString.toLowerCase().matches(regexp)) {
                return DATE_FORMAT_REGEXPS.get(regexp);
            }
        }
        return null; // Unknown format.
    }

    /**
     * if the string is in the yyMMdd format present in the corpus, will convert to yyyyMMdd that is parseable.
     * @param dateString string representing a date in format yyyyMMdd
     * @return the date in yyyyMMdd, or null if dateString is not a string in format yyMMdd.
     */
    private static String tryConvertCustomDateFormatToStandard(String dateString){
        if(dateString == null || dateString.length() != 6 || TokenType.classify(dateString) != TokenType.NUMBER)
            return null;
        else{ //is 6 digits
            String yearTwoFirstDigits = (dateString.charAt(0) == '9' || dateString.charAt(0) == '8' )? "19" : "20";
            return yearTwoFirstDigits + dateString;
        }

    }

    /**
     * classifies a token (string) to a type of token
     */
    public enum TokenType {
        NUMBER, WORD, SYMBOL, WHITESPACE;

        /**
         * classifies a token (string) to a type of token
         * @param str - token to classify
         * @return - a TokenType enum value
         */
        public static TokenType classify(String str){
            if(str == null || str.isEmpty()) return null;
            int length = str.length();
            if (1 == length){ // one char long
                if(isWhitespace(str.charAt(0)))
                    return WHITESPACE;
                else if (str.charAt(0) <= '9' && str.charAt(0) >= '0')  //number
                    return NUMBER;
                else if(isLetter(str.charAt(0))) // one letter word
                    return WORD;
                else //is one char and not whitespace or number or letter, must be a symbol.
                    return SYMBOL;
            }
            else{ //more than one character
                if(str.charAt(0) <= '9' && str.charAt(0) >= '0'){ //first char is digit
                    for(int i=0; i<length; i++){
                        if(!(str.charAt(i)>='0' && str.charAt(i)<='9')) return WORD;
                    }
                    return NUMBER; // all chars are digits
                }
                else { //first char is letter
                    return WORD; //all chars are letters
                }
            }
        }
    }

    //          Administrative

    public void run() {
        try {
            parse();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * tries to get a set of stopwords from the given file.
     * @param pathTostopwordsFile - path to the stopwords file.
     * @return - a set of stopwords.
     */
    public static HashSet<String> getStopWords(String pathTostopwordsFile) {
        HashSet<String> stopWords = new HashSet<>();

        try {
            InputStream is = null;
            BufferedReader buffer = new BufferedReader(new FileReader(pathTostopwordsFile));
            String line = null;
            line = buffer.readLine();
            while(line != null){
                stopWords.add(line);
                line = buffer.readLine();
            }
            buffer.close();
        } catch (FileNotFoundException e) {
            System.out.println("stopwords file not found in the specified path. running without stopwords");
        } catch (IOException e){
            System.out.println("stopwords file not found in the specified path. running without stopwords");
        }
        return stopWords;
    }
}
