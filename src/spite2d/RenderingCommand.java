package spite2d;

import com.jogamp.opengl.GL2;

// Interface for rendering commands that can be queued and executed on the OpenGL thread
interface RenderingCommand {
    void execute(GL2 gl);
}
