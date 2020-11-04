package com.example.edgenode.service;

import java.util.Map;
import java.util.Set;

public interface FilterService {
    Map<String, Map<String, Double>> calcPopularity();

    Map<String,Map<String,Double>> calcPopularityNormal();

    Double getAbsoluteThreshold();

    Map<String, Set<String>> filter();
}
