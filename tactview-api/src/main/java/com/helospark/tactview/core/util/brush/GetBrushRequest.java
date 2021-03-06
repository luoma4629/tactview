package com.helospark.tactview.core.util.brush;

import java.util.Objects;

import javax.annotation.Generated;

import com.helospark.tactview.core.timeline.effect.interpolation.pojo.Color;

public class GetBrushRequest {
    private String filename;
    private int width;
    private int height;
    private Color color;

    @Generated("SparkTools")
    private GetBrushRequest(Builder builder) {
        this.filename = builder.filename;
        this.width = builder.width;
        this.height = builder.height;
        this.color = builder.color;
    }

    public String getFilename() {
        return filename;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return "GetBrushRequest [filename=" + filename + ", width=" + width + ", height=" + height + ", color=" + color + "]";
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof GetBrushRequest)) {
            return false;
        }
        GetBrushRequest castOther = (GetBrushRequest) other;
        return Objects.equals(filename, castOther.filename) && Objects.equals(width, castOther.width) && Objects.equals(height, castOther.height) && Objects.equals(color, castOther.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, width, height, color);
    }

    @Generated("SparkTools")
    public static Builder builder() {
        return new Builder();
    }

    @Generated("SparkTools")
    public static final class Builder {
        private String filename;
        private int width;
        private int height;
        private Color color;

        private Builder() {
        }

        public Builder withFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder withWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder withHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder withColor(Color color) {
            this.color = color;
            return this;
        }

        public GetBrushRequest build() {
            return new GetBrushRequest(this);
        }
    }
}
