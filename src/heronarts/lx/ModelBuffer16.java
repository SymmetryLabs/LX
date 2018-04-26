package heronarts.lx;

import heronarts.lx.model.LXModel;

public class ModelBuffer16 implements LXBuffer16 {
    private long[] array;

    public ModelBuffer16(LX lx) {
        initArray(lx.model);

        lx.addListener(new LX.Listener() {
            @Override
            public void modelChanged(LX lx, LXModel model) {
                initArray(model);
            }
        });
    }

    private void initArray(LXModel model) {
        this.array = new long[model.size];  // initialized to 0 by Java
    }

    public long[] getArray16() {
        return this.array;
    }
}
