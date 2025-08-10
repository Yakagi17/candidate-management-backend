package com.candidatemanagement.model.criteria;

import java.math.BigDecimal;
import java.util.Set;

public class Criterion {

    private static final int DEFAULT_WEIGHT = 1;
    
    private String name;
    private int weight;
    private CriterionDetails details;

    public enum CriterionType {
        ENUMERATION, RANGE, ANY;

        public static boolean isValidType(String type) {
            for (CriterionType criterionType : CriterionType.values()) {
                if (criterionType.name().equalsIgnoreCase(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class CriterionDetails {
        private String type;
        private BigDecimal minValue;
        private BigDecimal maxValue;
        private Set<String> options;

        public CriterionDetails() {
            this.type = null;
            this.minValue = null;
            this.maxValue = null;
            this.options = null;
        }

        public CriterionDetails(String type, BigDecimal minValue, BigDecimal maxValue, Set<String> options) {
            this.type = type;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.options = options;
        }


        private CriterionDetails(String type, BigDecimal minValue, BigDecimal maxValue) {
            this(type, minValue, maxValue, null);
        }

        private CriterionDetails(String type, Set<String> options) {
            this(type, null, null, options);
        }

        private CriterionDetails(String type) {
            this(type, null, null, null);
        }

        public static CriterionDetails createObject(String type, BigDecimal minValue, BigDecimal maxValue, Set<String> options) {
            if (type == null || type.isBlank() || !CriterionType.isValidType(type)) {
                return null;
            }

            CriterionType criterionType = CriterionType.valueOf(type);

            return switch (criterionType) {
                case ENUMERATION -> new CriterionDetails(type, options);
                case RANGE -> new CriterionDetails(type, minValue, maxValue);
                case ANY -> new CriterionDetails(type);
            };
        }

        public String getType() {
            return type;
        }

        public BigDecimal getMinValue() {
            return minValue;
        }

        public BigDecimal getMaxValue() {
            return this.maxValue;
        }

        public Set<String> getOptions() {
            return this.options;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setMinValue(BigDecimal minValue) {
            this.minValue = minValue;
        }

        public void setMaxValue(BigDecimal maxValue) {
            this.maxValue = maxValue;
        }

        public void setOptions(Set<String> options) {
            this.options = options;
        }
    }

    public Criterion() {
        this.name = null;
        this.weight = DEFAULT_WEIGHT;
        this.details = null;
    }

    public Criterion(String name, int weight, CriterionDetails details) {
        this.name = name;
        this.weight = weight;
        this.details = details;
    }
    
    public Criterion(String name, CriterionDetails details) {
        this.name = name;
        this.weight = DEFAULT_WEIGHT;;
        this.details = details;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getWeight() {
        return weight;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public CriterionDetails getDetails() {
        return details;
    }

    public void setDetails(CriterionDetails details) {
        this.details = details;
    }
}
