package com.shuzijun.plantumlparser.core;

import java.util.Objects;

/**
 * class之间的关系
 *
 * @author 关系
 */
public class PUmlRelation {

    private String source;

    private String target;

    private String relation;

    public void setSource(String source) {
        this.source = source;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    @Override
    public String toString() {
        return source + " " + relation + " " + target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PUmlRelation that = (PUmlRelation) o;

        if (!Objects.equals(source, that.source)) return false;
        if (!Objects.equals(target, that.target)) return false;
        return Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (target != null ? target.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);
        return result;
    }
}
