package com.helospark.tactview.core.timeline.proceduralclip.polygon.impl.bezier;

import javax.annotation.Generated;

import com.helospark.tactview.core.timeline.effect.interpolation.pojo.Color;

public class BezierPolygonRenderServiceRequest {
    int fuzzyEdge;
    BezierPolygon polygon;
    Color color;
    int expectedWidth;
    int expectedHeight;

    @Generated("SparkTools")
    private BezierPolygonRenderServiceRequest(Builder builder) {
        this.fuzzyEdge = builder.fuzzyEdge;
        this.polygon = builder.polygon;
        this.color = builder.color;
        this.expectedWidth = builder.expectedWidth;
        this.expectedHeight = builder.expectedHeight;
    }

    public int getFuzzyEdge() {
        return fuzzyEdge;
    }

    public BezierPolygon getPolygon() {
        return polygon;
    }

    public Color getColor() {
        return color;
    }

    public int getExpectedWidth() {
        return expectedWidth;
    }

    public int getExpectedHeight() {
        return expectedHeight;
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {
        private int fuzzyEdge;
        private BezierPolygon polygon;
        private Color color;
        private int expectedWidth;
        private int expectedHeight;

        private Builder() {
        }

        public Builder withFuzzyEdge(int fuzzyEdge) {
            this.fuzzyEdge = fuzzyEdge;
            return this;
        }

        public Builder withPolygon(BezierPolygon polygon) {
            this.polygon = polygon;
            return this;
        }

        public Builder withColor(Color color) {
            this.color = color;
            return this;
        }

        public Builder withExpectedWidth(int expectedWidth) {
            this.expectedWidth = expectedWidth;
            return this;
        }

        public Builder withExpectedHeight(int expectedHeight) {
            this.expectedHeight = expectedHeight;
            return this;
        }

        public BezierPolygonRenderServiceRequest build() {
            return new BezierPolygonRenderServiceRequest(this);
        }
    }
}
