package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.IOException;

final class RunUpdate {

    private final SetupContext ctx;
    private final ToolRunner runner;

    RunUpdate(SetupContext ctx, ToolRunner runner) {
        this.ctx = ctx;
        this.runner = runner;
    }

    void update(Tool tool) throws IOException, InterruptedException {
        switch (tool) {
        case PYTHON:
            throw new IllegalArgumentException("Cannot update " + tool);
        case FFMPEG:
            updateFFMPEG();
            break;
        case PIP:
            updatePIP();
            break;
        default:
            updatePackage(tool);
            break;
        }
    }

    private void updatePIP() throws IOException, InterruptedException {
        runner.run().python("pip update", "-m", "pip", "install", "--upgrade", "pip");
    }

    private void updatePackage(Tool tool) throws IOException, InterruptedException {
        runner.run().pythonTool("pip", "-v", "install", "--upgrade", tool.packName());
    }

    private void updateFFMPEG() throws IOException, InterruptedException {
        ctx.updateFFMPEG(runner);
    }
}
