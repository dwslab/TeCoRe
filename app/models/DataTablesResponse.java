package models;

import java.util.List;

public class DataTablesResponse {

    public int draw;
    public int recordsTotal;
    public int recordsFiltered;
    public Statement[] data;
    public String error;

    public DataTablesResponse(int draw, int recordsTotal, int recordsFiltered, List<Statement> data, String error) {
        this.draw = draw;
        this.recordsTotal = recordsTotal;
        this.recordsFiltered = recordsFiltered;
        this.data = data.toArray(new Statement[data.size()]);
        this.error = error;
    }

    @Override
    public String toString() {
        return "DataTablesResponse{" +
                "draw=" + draw +
                ", recordsTotal=" + recordsTotal +
                ", recordsFiltered=" + recordsFiltered +
                ", data=" + data +
                ", error='" + error + '\'' +
                '}';
    }
}
