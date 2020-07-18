package Querying;

import java.util.List;

public class QueryResult {

    private String queryNum;
    private List<String> relevantDocs;
    private List<String[]> entities;
    private List<float[]> entRanking ;

    public QueryResult(String queryNum, List<String> relevantDocs) {
        this.queryNum = queryNum;
        this.relevantDocs = relevantDocs;
    }

    public String getQueryNum() {
        return queryNum;
    }

    public List<String> getRelevantDocs() {
        return relevantDocs;
    }

    public void setQueryNum(String  queryNum) {
        this.queryNum = queryNum;
    }

    public void setRelevantDocs(List<String> relevantDocs) {
        this.relevantDocs = relevantDocs;
    }

    public List<String[]> getEntities() {
        return entities;
    }

    public List<float[]> getEntRanking() {
        return entRanking;
    }

    public void setEntities(List<String[]> entities) {
        this.entities = entities;
    }

    public void setEntRanking(List<float[]> entRanking) {
        this.entRanking = entRanking;
    }
}
