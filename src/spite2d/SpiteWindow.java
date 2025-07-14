// reference for cursor on what spitewindow is all about. you can use this package freely in the code

package spite2d;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.*;

public class SpiteWindow {
    private final Frame frame;
    private final GLCanvas canvas;
    private final WeakHashMap<BufferedImage, Integer> textureCache;
    private final AtomicBoolean running;

    // Graphics state (like Graphics2D)
    private Color color = Color.BLACK;
    private Color backgroundColor = Color.WHITE;
    private Font font = new Font("Dialog", Font.PLAIN, 12);
    private Stroke stroke = new BasicStroke(1.0f);
    private Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
    private AffineTransform transform = new AffineTransform();
    private RenderingHints renderingHints = new RenderingHints(null);

    private Runnable renderCallback;
    private float clearR = 0.0f, clearG = 0.0f, clearB = 0.0f, clearA = 1.0f;

    // Multithreading support
    private final ConcurrentLinkedQueue<RenderingCommand> commandQueue = new ConcurrentLinkedQueue<>();
    private final Object renderLock = new Object();
    private volatile boolean isRendering = false;
    private volatile GL2 currentGL = null;

    public SpiteWindow(int width, int height, String title) {
        this.textureCache = new WeakHashMap<>();
        this.running = new AtomicBoolean(false);

        // Create OpenGL profile
        GLProfile profile = GLProfile.get(GLProfile.GL2);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setDoubleBuffered(true);

        // Create canvas
        this.canvas = new GLCanvas(capabilities);
        this.canvas.addGLEventListener(new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                GL2 gl = drawable.getGL().getGL2();
                gl.glEnable(GL2.GL_TEXTURE_2D);
                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
                gl.glEnable(GL2.GL_LINE_SMOOTH);
                gl.glEnable(GL2.GL_POLYGON_SMOOTH);
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
                // Clean up textures
                GL2 gl = drawable.getGL().getGL2();
                for (Integer textureId : textureCache.values()) {
                    gl.glDeleteTextures(1, new int[]{textureId}, 0);
                }
                textureCache.clear();
            }

            @Override
            public void display(GLAutoDrawable drawable) {
                GL2 gl = drawable.getGL().getGL2();
                currentGL = gl;

                // Debug: Check if OpenGL is working
                System.out.println("DEBUG: OpenGL display called - canvas size: " + canvas.getWidth() + "x" + canvas.getHeight());

                // Clear screen
                gl.glClearColor(clearR, clearG, clearB, clearA);
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT);

                // Set up orthographic projection
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glOrtho(0, canvas.getWidth(), canvas.getHeight(), 0, -1, 1);

                gl.glMatrixMode(GL2.GL_MODELVIEW);
                gl.glLoadIdentity();

                // Apply current transform
                applyTransform(gl);

                // Execute all queued rendering commands
                executeQueuedCommands();

                // Call render callback
                if (renderCallback != null) {
                    renderCallback.run();
                }

                currentGL = null;
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                GL2 gl = drawable.getGL().getGL2();
                gl.glViewport(0, 0, width, height);
            }
        });

        // Create frame
        this.frame = new Frame(title);
        this.frame.setSize(width, height);
        this.frame.add(canvas);
        this.frame.setResizable(true);
        this.frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stop();
            }
        });
    }

    // ===== Graphics2D-like API =====

    public void setRenderCallback(Runnable callback) {
        this.renderCallback = callback;
    }

    /**
     * Repaint the window - call this from your game loop
     */
    public void repaint() {
        if (running.get() && canvas != null) {
            canvas.display();
        }
    }

    /**
     * Thread-safe method to queue rendering commands
     */
    public void queueCommand(RenderingCommand command) {
        commandQueue.offer(command);
    }

    /**
     * Execute all queued rendering commands on the OpenGL thread
     */
    private void executeQueuedCommands() {
        if (currentGL == null) return;

        RenderingCommand command;
        while ((command = commandQueue.poll()) != null) {
            try {
                command.execute(currentGL);
            } catch (Exception e) {
                System.err.println("Error executing rendering command: " + e.getMessage());
            }
        }
    }



    // Color and paint methods
    public void setColor(Color color) {
        queueCommand(gl -> {
            setGLColor(gl, color);
            this.color = color != null ? color : Color.BLACK;
        });
    }

    public Color getColor() {
        return color;
    }

    public void setBackground(Color color) {
        this.backgroundColor = color != null ? color : Color.WHITE;
        if (color != null) {
            this.clearR = color.getRed() / 255.0f;
            this.clearG = color.getGreen() / 255.0f;
            this.clearB = color.getBlue() / 255.0f;
            this.clearA = color.getAlpha() / 255.0f;
        }
    }

    public Color getBackground() {
        return backgroundColor;
    }

    // Font methods
    public void setFont(Font font) {
        this.font = font != null ? font : new Font("Dialog", Font.PLAIN, 12);
    }

    public Font getFont() {
        return font;
    }

    // Stroke methods
    public void setStroke(Stroke stroke) {
        this.stroke = stroke != null ? stroke : new BasicStroke(1.0f);
    }

    public Stroke getStroke() {
        return stroke;
    }

    // Transform methods
    public void translate(double tx, double ty) {
        transform.translate(tx, ty);
    }

    public void rotate(double theta) {
        transform.rotate(theta);
    }

    public void rotate(double theta, double x, double y) {
        transform.rotate(theta, x, y);
    }

    public void scale(double sx, double sy) {
        transform.scale(sx, sy);
    }

    public void shear(double shx, double shy) {
        transform.shear(shx, shy);
    }

    public void setTransform(AffineTransform transform) {
        this.transform = transform != null ? new AffineTransform(transform) : new AffineTransform();
    }

    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    public void transform(AffineTransform transform) {
        if (transform != null) {
            this.transform.concatenate(transform);
        }
    }

    // Drawing methods
    public void drawLine(int x1, int y1, int x2, int y2) {
        queueCommand(gl -> {
            setGLColor(gl, color);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex2f(x1, y1);
            gl.glVertex2f(x2, y2);
            gl.glEnd();
            gl.glEnable(GL2.GL_TEXTURE_2D);
        });
    }

    public void drawRect(int x, int y, int width, int height) {
        queueCommand(gl -> {
            setGLColor(gl, color);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glBegin(GL2.GL_LINE_LOOP);
            gl.glVertex2f(x, y);
            gl.glVertex2f(x + width, y);
            gl.glVertex2f(x + width, y + height);
            gl.glVertex2f(x, y + height);
            gl.glEnd();
            gl.glEnable(GL2.GL_TEXTURE_2D);
        });
    }

    public void fillRect(int x, int y, int width, int height) {
        queueCommand(gl -> {
            setGLColor(gl, color);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glBegin(GL2.GL_QUADS);
            gl.glVertex2f(x, y);
            gl.glVertex2f(x + width, y);
            gl.glVertex2f(x + width, y + height);
            gl.glVertex2f(x, y + height);
            gl.glEnd();
            gl.glEnable(GL2.GL_TEXTURE_2D);
        });
    }

    public void drawOval(int x, int y, int width, int height) {
        queueCommand(gl -> drawEllipse(gl, x, y, width, height, false));
    }

    public void fillOval(int x, int y, int width, int height) {
        queueCommand(gl -> drawEllipse(gl, x, y, width, height, true));
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        // Simplified round rectangle - just draw a regular rectangle for now
        drawRect(x, y, width, height);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        // Simplified round rectangle - just fill a regular rectangle for now
        fillRect(x, y, width, height);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        queueCommand(gl -> drawEllipticalArc(gl, x, y, width, height, startAngle, arcAngle, false));
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        queueCommand(gl -> drawEllipticalArc(gl, x, y, width, height, startAngle, arcAngle, true));
    }

    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints < 2) return;

        queueCommand(gl -> {
            setGLColor(gl, color);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glBegin(GL2.GL_LINE_STRIP);
            for (int i = 0; i < nPoints; i++) {
                gl.glVertex2f(xPoints[i], yPoints[i]);
            }
            gl.glEnd();
            gl.glEnable(GL2.GL_TEXTURE_2D);
        });
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints < 3) return;

        queueCommand(gl -> {
            setGLColor(gl, color);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glBegin(GL2.GL_LINE_LOOP);
            for (int i = 0; i < nPoints; i++) {
                gl.glVertex2f(xPoints[i], yPoints[i]);
            }
            gl.glEnd();
            gl.glEnable(GL2.GL_TEXTURE_2D);
        });
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (nPoints < 3) return;

        queueCommand(gl -> {
            setGLColor(gl, color);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glBegin(GL2.GL_POLYGON);
            for (int i = 0; i < nPoints; i++) {
                gl.glVertex2f(xPoints[i], yPoints[i]);
            }
            gl.glEnd();
            gl.glEnable(GL2.GL_TEXTURE_2D);
        });
    }

    public void drawString(String str, int x, int y) {
        if (str == null || str.isEmpty()) return;

        queueCommand(gl -> {
            // Create a BufferedImage to render the text
            FontMetrics fm = new Canvas().getFontMetrics(font);
            int width = fm.stringWidth(str);
            int height = fm.getHeight();

            BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = textImage.createGraphics();
            g2d.setFont(font);
            g2d.setColor(color);
            g2d.drawString(str, 0, fm.getAscent());
            g2d.dispose();

            // Draw the text image
            int textureId = getOrCreateTexture(gl, textImage);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId);
            gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2f(0, 0); gl.glVertex2f(x, y - fm.getAscent());
            gl.glTexCoord2f(1, 0); gl.glVertex2f(x + width, y - fm.getAscent());
            gl.glTexCoord2f(1, 1); gl.glVertex2f(x + width, y - fm.getAscent() + height);
            gl.glTexCoord2f(0, 1); gl.glVertex2f(x, y - fm.getAscent() + height);
            gl.glEnd();
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        });
    }

    public void drawString(String str, float x, float y) {
        drawString(str, (int)x, (int)y);
    }

    public void drawImage(BufferedImage img, int x, int y) {
        drawImage(img, x, y, null);
    }

    public void drawImage(BufferedImage img, int x, int y, Color bgcolor) {
        if (img == null) return;

        queueCommand(gl -> {
            int textureId = getOrCreateTexture(gl, img);

            gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId);
            gl.glBegin(GL2.GL_QUADS);
            gl.glTexCoord2f(0, 0); gl.glVertex2f(x, y);
            gl.glTexCoord2f(1, 0); gl.glVertex2f(x + img.getWidth(), y);
            gl.glTexCoord2f(1, 1); gl.glVertex2f(x + img.getWidth(), y + img.getHeight());
            gl.glTexCoord2f(0, 1); gl.glVertex2f(x, y + img.getHeight());
            gl.glEnd();
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        });
    }

    public void drawImage(BufferedImage img, int x, int y, int width, int height) {
        drawImage(img, x, y, width, height, null);
    }

    public void drawImage(BufferedImage img, int x, int y, int width, int height, Color bgcolor) {
        if (img == null) return;

        GL2 gl = canvas.getGL().getGL2();
        int textureId = getOrCreateTexture(gl, img);

        gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId);
        gl.glBegin(GL2.GL_QUADS);
        gl.glTexCoord2f(0, 0); gl.glVertex2f(x, y);
        gl.glTexCoord2f(1, 0); gl.glVertex2f(x + width, y);
        gl.glTexCoord2f(1, 1); gl.glVertex2f(x + width, y + height);
        gl.glTexCoord2f(0, 1); gl.glVertex2f(x, y + height);
        gl.glEnd();
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
    }

    // Shape drawing methods
    public void draw(Shape s) {
        if (s == null) return;

        queueCommand(gl -> {
            PathIterator pi = s.getPathIterator(transform);
            drawPathIterator(gl, pi, false);
        });
    }

    public void fill(Shape s) {
        if (s == null) return;

        queueCommand(gl -> {
            PathIterator pi = s.getPathIterator(transform);
            drawPathIterator(gl, pi, true);
        });
    }

    // Utility methods
    public void clearRect(int x, int y, int width, int height) {
        Color oldColor = color;
        setColor(backgroundColor);
        fillRect(x, y, width, height);
        setColor(oldColor);
    }

    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        // This would require reading pixels from the framebuffer
        // For now, we'll skip this implementation
    }

    public void dispose() {
        stop();
    }

    // ===== Private helper methods =====

    private void setGLColor(GL2 gl, Color color) {
        if (color == null) color = Color.BLACK;
        float alpha = composite instanceof AlphaComposite ?
                ((AlphaComposite) composite).getAlpha() : 1.0f;
        gl.glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f,
                color.getBlue() / 255.0f, (color.getAlpha() / 255.0f) * alpha);
    }

    private void applyTransform(GL2 gl) {
        double[] matrix = new double[6];
        transform.getMatrix(matrix);
        gl.glLoadMatrixd(new double[]{
                matrix[0], matrix[2], 0, 0,
                matrix[1], matrix[3], 0, 0,
                0, 0, 1, 0,
                matrix[4], matrix[5], 0, 1
        }, 0);
    }

    private void drawEllipse(GL2 gl, int x, int y, int width, int height, boolean fill) {
        setGLColor(gl, color);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        int segments = Math.max(16, Math.min(width, height) / 4);
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        float radiusX = width / 2.0f;
        float radiusY = height / 2.0f;

        if (fill) {
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex2f(centerX, centerY);
        } else {
            gl.glBegin(GL2.GL_LINE_LOOP);
        }

        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float px = centerX + (float)(radiusX * Math.cos(angle));
            float py = centerY + (float)(radiusY * Math.sin(angle));
            gl.glVertex2f(px, py);
        }

        gl.glEnd();
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    private void drawEllipticalArc(GL2 gl, int x, int y, int width, int height, int startAngle, int arcAngle, boolean fill) {
        setGLColor(gl, color);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        int segments = Math.max(16, Math.min(width, height) / 4);
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        float radiusX = width / 2.0f;
        float radiusY = height / 2.0f;

        double startRad = Math.toRadians(-startAngle);
        double endRad = Math.toRadians(-startAngle - arcAngle);

        if (fill) {
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex2f(centerX, centerY);
        } else {
            gl.glBegin(GL2.GL_LINE_STRIP);
        }

        for (int i = 0; i <= segments; i++) {
            double angle = startRad + (endRad - startRad) * i / segments;
            float px = centerX + (float)(radiusX * Math.cos(angle));
            float py = centerY + (float)(radiusY * Math.sin(angle));
            gl.glVertex2f(px, py);
        }

        gl.glEnd();
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    private void drawPathIterator(GL2 gl, PathIterator pi, boolean fill) {
        setGLColor(gl, color);
        gl.glDisable(GL2.GL_TEXTURE_2D);

        float[] coords = new float[6];
        boolean first = true;
        float firstX = 0, firstY = 0;
        float lastX = 0, lastY = 0;

        if (fill) {
            gl.glBegin(GL2.GL_POLYGON);
        } else {
            gl.glBegin(GL2.GL_LINE_STRIP);
        }

        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    if (!first && fill) {
                        gl.glEnd();
                        gl.glBegin(GL2.GL_POLYGON);
                    }
                    firstX = lastX = coords[0];
                    firstY = lastY = coords[1];
                    gl.glVertex2f(coords[0], coords[1]);
                    first = false;
                    break;
                case PathIterator.SEG_LINETO:
                    lastX = coords[0];
                    lastY = coords[1];
                    gl.glVertex2f(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    // Approximate quadratic curve with line segments
                    for (int i = 1; i <= 8; i++) {
                        float t = i / 8.0f;
                        float x = (1-t)*(1-t)*lastX + 2*(1-t)*t*coords[0] + t*t*coords[2];
                        float y = (1-t)*(1-t)*lastY + 2*(1-t)*t*coords[1] + t*t*coords[3];
                        gl.glVertex2f(x, y);
                    }
                    lastX = coords[2];
                    lastY = coords[3];
                    break;
                case PathIterator.SEG_CUBICTO:
                    // Approximate cubic curve with line segments
                    for (int i = 1; i <= 8; i++) {
                        float t = i / 8.0f;
                        float x = (1-t)*(1-t)*(1-t)*lastX + 3*(1-t)*(1-t)*t*coords[0] +
                                3*(1-t)*t*t*coords[2] + t*t*t*coords[4];
                        float y = (1-t)*(1-t)*(1-t)*lastY + 3*(1-t)*(1-t)*t*coords[1] +
                                3*(1-t)*t*t*coords[3] + t*t*t*coords[5];
                        gl.glVertex2f(x, y);
                    }
                    lastX = coords[4];
                    lastY = coords[5];
                    break;
                case PathIterator.SEG_CLOSE:
                    if (fill && !first) {
                        gl.glVertex2f(firstX, firstY);
                    }
                    break;
            }
            pi.next();
        }

        gl.glEnd();
        gl.glEnable(GL2.GL_TEXTURE_2D);
    }

    private int getOrCreateTexture(GL2 gl, BufferedImage img) {
        Integer textureId = textureCache.get(img);
        if (textureId != null) {
            return textureId;
        }

        // Convert BufferedImage to RGBA
        BufferedImage rgbaImage = convertToRGBA(img);

        // Get pixel data as bytes
        byte[] pixels = getImageBytes(rgbaImage);

        // Generate texture
        int[] textureIds = new int[1];
        gl.glGenTextures(1, textureIds, 0);
        textureId = textureIds[0];

        gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, rgbaImage.getWidth(),
                rgbaImage.getHeight(), 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE,
                ByteBuffer.wrap(pixels));
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

        textureCache.put(img, textureId);
        return textureId;
    }

    private BufferedImage convertToRGBA(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_ARGB) {
            return img;
        }

        BufferedImage rgbaImage = new BufferedImage(img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rgbaImage.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return rgbaImage;
    }

    private byte[] getImageBytes(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        byte[] pixels = new byte[width * height * 4]; // 4 bytes per pixel (RGBA)

        // Get all pixels at once
        int[] rgbPixels = new int[width * height];
        img.getRGB(0, 0, width, height, rgbPixels, 0, width);

        // Convert int pixels to byte array (RGBA format)
        for (int i = 0; i < rgbPixels.length; i++) {
            int pixel = rgbPixels[i];
            int offset = i * 4;

            // Extract RGBA components (note: BufferedImage stores as ARGB)
            pixels[offset] = (byte) ((pixel >> 16) & 0xFF); // Red
            pixels[offset + 1] = (byte) ((pixel >> 8) & 0xFF);  // Green
            pixels[offset + 2] = (byte) (pixel & 0xFF);         // Blue
            pixels[offset + 3] = (byte) ((pixel >> 24) & 0xFF); // Alpha
        }

        return pixels;
    }

    // ===== Window management =====

    public void start() {
        if (running.compareAndSet(false, true)) {
            System.out.println("DEBUG: SpiteWindow.start() called - making frame visible");
            frame.setVisible(true);
            System.out.println("DEBUG: SpiteWindow frame visible: " + frame.isVisible());
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            frame.dispose();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public int getWidth() {
        return canvas.getWidth();
    }

    public int getHeight() {
        return canvas.getHeight();
    }

    // === Mouse listener API for compatibility ===
    public void addMouseListener(java.awt.event.MouseListener l) {
        canvas.addMouseListener(l);
    }
    public void addMouseMotionListener(java.awt.event.MouseMotionListener l) {
        canvas.addMouseMotionListener(l);
    }
    public void addMouseWheelListener(java.awt.event.MouseWheelListener l) {
        canvas.addMouseWheelListener(l);
    }

    // === Keyboard and focus listener API for compatibility ===
    public void addKeyListener(java.awt.event.KeyListener l) {
        canvas.addKeyListener(l);
    }
    public void addFocusListener(java.awt.event.FocusListener l) {
        canvas.addFocusListener(l);
    }
} 