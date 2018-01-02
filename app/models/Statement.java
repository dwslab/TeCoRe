package models;

/**
 * Created by joerg on 27.02.17.
 */
public class Statement {

    public final String subject;
    public final String predicate;
    public final String obj;
    public final String from;
    public final String to;

    public Statement(String subject, String predicate, String obj, String from, String to) {
        this.subject = subject;
        this.predicate = predicate;
        this.obj = obj;
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Statement statement = (Statement) o;

        if (subject != null ? !subject.equals(statement.subject) : statement.subject != null) return false;
        if (predicate != null ? !predicate.equals(statement.predicate) : statement.predicate != null) return false;
        if (obj != null ? !obj.equals(statement.obj) : statement.obj != null) return false;
        if (from != null ? !from.equals(statement.from) : statement.from != null) return false;
        return to != null ? to.equals(statement.to) : statement.to == null;
    }

    @Override
    public int hashCode() {
        int result = subject != null ? subject.hashCode() : 0;
        result = 31 * result + (predicate != null ? predicate.hashCode() : 0);
        result = 31 * result + (obj != null ? obj.hashCode() : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }

}
