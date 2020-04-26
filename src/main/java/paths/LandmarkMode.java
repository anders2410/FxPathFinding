package paths;

public enum LandmarkMode {
    RANDOM {
        @Override
        public String toString() {
            return "random";
        }
    }, MAXCOVER {
        @Override
        public String toString() {
            return "maxCover";
        }
    }, FARTHEST {
        @Override
        public String toString() {
            return "farthest";
        }
    }, AVOID {
        @Override
        public String toString() {
            return "avoid";
        }
    }
}
