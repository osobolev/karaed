package karaed.engine.sync;

import karaed.engine.KaraException;

interface BackvocalState {

    boolean isBackvocal();

    void setBackvocal(boolean on);

    static BackvocalState create() {
        return new BackvocalState() {

            private boolean inBackvocal = false;

            @Override
            public boolean isBackvocal() {
                return inBackvocal;
            }

            @Override
            public void setBackvocal(boolean on) {
                if (on) {
                    if (inBackvocal)
                        throw new KaraException("Nested '{'");
                } else {
                    if (!inBackvocal)
                        throw new KaraException("Extra '}'");
                }
                inBackvocal = on;
            }
        };
    }

    BackvocalState EMPTY = new BackvocalState() {

        @Override
        public boolean isBackvocal() {
            return false;
        }

        @Override
        public void setBackvocal(boolean on) {
        }
    };
}
