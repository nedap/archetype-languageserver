package com.nedap.openehr.lsp.aql;

import com.google.common.collect.Lists;
import com.nedap.archie.aom.ArchetypeModelObject;
import com.nedap.archie.aom.CAttribute;
import com.nedap.archie.aom.CComplexObject;
import com.nedap.archie.paths.PathSegment;
import com.nedap.archie.query.AOMPathQuery;
import com.nedap.archie.query.APathQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PartialAOMPathQuery extends AOMPathQuery  {
    public PartialAOMPathQuery(String query) {
        super(query);
    }

    public PartialMatch findPartial(CComplexObject root) {
        List<ArchetypeModelObject> result = new ArrayList<>();
        boolean findThroughDifferentialPaths = true, matchSpecializedNodes = true;
        result.add(root);
        List<PathSegment> pathSegments = getPathSegments();

        for(int i = 0; i < pathSegments.size(); i++) {
            List<ArchetypeModelObject> previousResult = result;
            PathSegment segment = pathSegments.get(i);

            CAttribute differentialAttribute = null;
            if(findThroughDifferentialPaths) {
                differentialAttribute = findMatchingDifferentialPath(pathSegments.subList(i, pathSegments.size()), result);
            }
            if(differentialAttribute != null) {
                //skip a few pathsegments for this differential path match
                i = i + new APathQuery(differentialAttribute.getDifferentialPath()).getPathSegments().size()-1;
                PathSegment lastPathSegment = pathSegments.get(i);
                ArchetypeModelObject oneMatchingObject = findOneMatchingObject(differentialAttribute, lastPathSegment, matchSpecializedNodes);
                if(oneMatchingObject != null) {
                    result = Lists.newArrayList(oneMatchingObject);
                } else {
                    result = findOneSegment(segment, result, matchSpecializedNodes);
                }
            } else {
                result = findOneSegment(segment, result, matchSpecializedNodes);
            }
            List<ArchetypeModelObject> returnValue = previousResult.stream().filter((object) -> object != null).collect(Collectors.toList());
            if(result.isEmpty()) {
                //we have to return our partial match here
                return new PartialMatch(pathSegments, pathSegments.subList(i, pathSegments.size()), returnValue);
            }
        }
        List<ArchetypeModelObject> returnValue = result.stream().filter((object) -> object != null).collect(Collectors.toList());
        return new PartialMatch(pathSegments, new ArrayList<>(), returnValue);
    }

    class PartialMatch {
        private final List<PathSegment> entireQuery;
        private final List<PathSegment> remainingQuery;
        private final List<ArchetypeModelObject> matches;

        public PartialMatch(List<PathSegment> entireQuery, List<PathSegment> remainingQuery, List<ArchetypeModelObject> matches) {
            this.entireQuery = entireQuery;
            this.remainingQuery = remainingQuery;
            this.matches = matches;
        }

        public List<PathSegment> getEntireQuery() {
            return entireQuery;
        }

        public List<PathSegment> getRemainingQuery() {
            return remainingQuery;
        }

        public List<ArchetypeModelObject> getMatches() {
            return matches;
        }

        public boolean isFullMatch() {
            return remainingQuery.isEmpty();
        }
    }
}
