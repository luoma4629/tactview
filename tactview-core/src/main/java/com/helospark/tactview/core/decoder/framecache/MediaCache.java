package com.helospark.tactview.core.decoder.framecache;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.helospark.lightdi.annotation.Component;

@Component
public class MediaCache {
    private Map<MediaHashKey, NavigableMap<Integer, MediaHashValue>> backCache = new ConcurrentHashMap<>();

    public void cacheMedia(MediaHashKey key, MediaHashValue value) {
        NavigableMap<Integer, MediaHashValue> cachedFrames = backCache.get(key);
        if (cachedFrames == null) {
            cachedFrames = new ConcurrentSkipListMap<>();
            backCache.put(key, cachedFrames);
        }
        cachedFrames.put(value.frameStart, value);
    }

    public Optional<MediaHashValue> findInCache(MediaHashKey key, int frame) {
        NavigableMap<Integer, MediaHashValue> media = backCache.get(key);
        if (media == null) {
            return Optional.empty();
        } else {
            return media // todo: avoid linear search
                    .values()
                    .stream()
                    .filter(value -> frame >= value.frameStart && frame < value.frameStart + value.frames.size())
                    .findFirst();
        }
    }

    public static class MediaHashValue implements Comparable<MediaHashValue> {
        public int frameStart;
        public List<ByteBuffer> frames;

        public MediaHashValue(int frameStart, List<ByteBuffer> frames) {
            this.frameStart = frameStart;
            this.frames = frames;
        }

        @Override
        public int compareTo(MediaHashValue o) {
            return Integer.valueOf(frameStart).compareTo(o.frameStart); // may not work correctly for variable length
        }

    }

    public static class MediaHashKey {
        public String file;
        public int width;
        public int height;

        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof MediaHashKey)) {
                return false;
            }
            MediaHashKey castOther = (MediaHashKey) other;
            return Objects.equals(file, castOther.file) && Objects.equals(width, castOther.width) && Objects.equals(height, castOther.height);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file, width, height);
        }

        public MediaHashKey(String file, int width, int height) {
            this.file = file;
            this.width = width;
            this.height = height;
        }

    }
}
