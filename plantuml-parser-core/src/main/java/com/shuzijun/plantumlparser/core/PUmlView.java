package com.shuzijun.plantumlparser.core;

import static com.shuzijun.plantumlparser.core.Util.listToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PUml视图
 *
 * @author shuzijun
 */
public class PUmlView implements PUml {

    private List<PUmlClass> pUmlClassList = new ArrayList<>();

    private Set<PUmlRelation> pUmlRelationList = new HashSet<>();

    public void addPUmlClass(PUmlClass pUmlClass) {
        pUmlClassList.add(pUmlClass);
    }

    public void addPUmlRelation(PUmlRelation pUmlRelation) {
        pUmlRelationList.add(pUmlRelation);
    }

    @Override
    public String toString() {
        return "@startuml"
               + listToString(pUmlClassList, "\n", "\n", "")
               + listToString(pUmlRelationList.stream().toList(), "\n", "\n\n", "")
               + "\n@enduml";
    }
}
