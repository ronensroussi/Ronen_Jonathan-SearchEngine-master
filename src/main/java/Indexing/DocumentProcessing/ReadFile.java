package Indexing.DocumentProcessing;


import Indexing.Index.CityIndexEntry;
import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Runnable.
 * Reads all the files in #pathToDocumentsFolder (recursively) into the given buffer.
 * When finished, will insert a Document with all fields equalling null, to represent "end of file".
 */
public class ReadFile implements Runnable {
    private String pathToDocumentsFolder;
    static BlockingQueue<Document> documentBuffer;



    /**
     * @param pathToDocumentsFolder
     * @param documentBuffer        - a blocking queue where parsed documents will be outputted for further processing.
     */
    public ReadFile(String pathToDocumentsFolder, BlockingQueue<Document> documentBuffer) {
        this.pathToDocumentsFolder = pathToDocumentsFolder;
        this.documentBuffer = documentBuffer;
    }


    /**
     * Reads all the files in #pathToDocumentsFolder (recursively) and separate each file to Documents
     * then generate each Document to a Documant object that goes into the Buffer
     *
     */

    private void read() {




        File f = new File(pathToDocumentsFolder);
        Elements docs;
        File[] allSubFiles = f.listFiles();
        for (File file : allSubFiles) {
            if (file.isDirectory()) {
                File[] documentsFiles = file.listFiles();
                for (File fileToGenerate : documentsFiles) {
                    docs = separateDocs(fileToGenerate);
                    if (docs != null) {
                        generateDocs(docs);
                    }
                }

            }

        }
        try {
            // when done, insert poison element
            documentBuffer.put(new Document(null,null,null,null));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * separate documents from a file
     * @param fileToGenerate - a file that contains a bunch of Documents that needs to be separated
     * @return Elements object ( a list of Elements that each element is a document)
     */
    private Elements separateDocs(File fileToGenerate) {
        FileInputStream fi = null;
        Elements toReturn = null;

        try {
            fi = new FileInputStream(fileToGenerate);
            BufferedReader br = new BufferedReader(new InputStreamReader(fi, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line = null;
            line = br.readLine();
            while (line != null) {
                sb.append(line+"\n");
                line = br.readLine();
            }
            org.jsoup.nodes.Document doc = Jsoup.parse(sb.toString());
            toReturn = doc.select("DOC");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    /**
     * parses each element into a SaxParser that creates a Documents objects and insert them to the buffer.
     * @param elems - a list of separated documents that needs to be generated
     */
    private void generateDocs(Elements elems) {
        for (Element elm : elems) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = null;
            try {

                saxParser = factory.newSAXParser();
                UserHandler userhandler = new UserHandler();

                String st = new String (elm.toString().getBytes(),"UTF-8");
                InputStream is = new ByteArrayInputStream(st.getBytes());

                saxParser.parse(is,userhandler);
            } catch (SAXException e) {
                e.printStackTrace();

            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }




    public void run() {
        read();



    }


    //-------------------------------------------------------------------------------------------------------------------------------
    //-------------------------------------------------------------------------------------------------------------------------------
    /**
     * handler class that determines what to do with each tag in the document
     */
    class UserHandler extends DefaultHandler {
        //boolean startDoc = false;
        boolean docId = false;
        boolean title = false;
        boolean text = false;
        boolean others =false;
        boolean date = false;
        boolean city = false;
        boolean language=false;
        Document doc = null;
        StringBuilder textString;


        @Override
        public void startElement(
                String uri, String localName, String qName, Attributes attributes)
                throws SAXException {

            if (qName.equalsIgnoreCase("DOCNO")) {
                doc = new Document();
                docId = true;
            }
            else if (qName.equalsIgnoreCase("TI")){
                title=true;
            }
            else if (qName.equalsIgnoreCase("DATE") || qName.equalsIgnoreCase("DATE1")) {
                date = true;
            }else if (qName.equalsIgnoreCase("F") && (attributes.getValue("p").equalsIgnoreCase("104")))
            {
                city=true;

            }else if (qName.equalsIgnoreCase("F") && (attributes.getValue("p").equalsIgnoreCase("105")))
            {
                language=true;

            }
            else if (qName.equalsIgnoreCase("TEXT")) {
                text = true;
                textString = new StringBuilder();
            } else if( text) {

                others =true;
            }


        }

        @Override
        public void endElement(String uri,
                               String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase("DOC")) {
                try {
                    documentBuffer.put(doc);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else if (qName.equalsIgnoreCase("TEXT")){
                text = false;
                doc.setText(textString.toString());
                textString=null;
            }
        }


        @Override
        public void characters(char ch[], int start, int length) throws SAXException {

            if (docId) {
                StringBuilder sb = new StringBuilder();
                for (int i = start; i <length ; i++) {
                    if(ch[i]!=' ' && ch[i]!='\n')
                        sb.append(ch[i]);
                }
                doc.setDocId(sb.toString());
                sb=null;
                docId = false;
            } else if (date) {
                doc.setDate(new String(ch, start, length));
                date = false;


            }else if(city){
                StringBuilder s =new StringBuilder();
                for(int i=start ; i<length-1 ; i++ ){
                    if(ch[i]!=' ' && ch[i]!='\n'){
                        while(ch[i]!=' '){
                            if(ch[i]>='a'&& ch[i]<='z') {
                                s.append((char)(ch[i]-32));
                                i++;
                            }
                            else {
                                s.append(ch[i]);
                                i++;
                            }
                        }
                        break;
                    }
                }
                doc.setCity(s.toString().replace(" ",""));
                s=null;
                city=false;
            }
            else if (language) {
                StringBuilder s = new StringBuilder();
                for (int i = start; i < length - 1; i++) {
                    if ((ch[i] != ' ' && ch[i] != '\n') && ((ch[i]>='a' && ch[i]<='z')|| (ch[i]>='A' && ch[i]<='Z'))  ) {
                            s.append(ch[i]);
                    }else {
                        continue;
                    }
                }
                doc.setLanguage(s.toString().toUpperCase().intern());
                s=null;
                language=false;
            }
            else if(title){
                doc.setTitle(new String(ch, start, length));
                title=false;
            }
            else if(text && others){
                textString.append( new String(ch, start, length) );
                others=false;

            } else if (text) {
                //  System.out.println(new String(ch, start, length)); //@TODO fix it!! it is copying only until the first sub Tag
                textString.append(new String(ch, start, length));
            }

        }

    }
}
