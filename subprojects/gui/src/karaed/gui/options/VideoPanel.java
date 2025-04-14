package karaed.gui.options;

import karaed.engine.opts.OVideo;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

final class VideoPanel extends BasePanel<OVideo> {

    private final JCheckBox cbVideo = new JCheckBox("Use original video");

    VideoPanel(OptCtx ctx) throws IOException {
        super("Video", () -> ctx.option("video.json"), OVideo.class, OVideo::new);

        main.add(
            cbVideo, new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));

        cbVideo.setSelected(origData.useOriginalVideo());
    }

    @Override
    OVideo newData() {
        return new OVideo(cbVideo.isSelected());
    }
}
