package DTO;

public class SearchRequest extends RequestDTO {
    private static final long serialVersionUID = 1L;
    private final String query;

    public SearchRequest(String query) {
        super(CommandType.SEARCH);
        this.query = query;
    }

    public String getQuery() { return query; }
}
