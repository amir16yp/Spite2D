package spite2d;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class Main {
    public static void main(String[] args) {
        SpiteWindow window = new SpiteWindow(800, 600, "Spite2D - Graphics2D-like API Demo");
        
        // Set up the render callback (like getting a Graphics2D object)
        window.setRenderCallback(() -> {
            // This is like getting a Graphics2D object and drawing with it
            // All operations use AWT Color and BufferedImage objects
            
            // Set background
            window.setBackground(new Color(50, 50, 100));
            
            // Draw some basic shapes
            window.setColor(Color.RED);
            window.fillRect(50, 50, 100, 80);
            
            window.setColor(Color.GREEN);
            window.drawRect(200, 50, 100, 80);
            
            window.setColor(Color.BLUE);
            window.fillOval(350, 50, 100, 80);
            
            window.setColor(Color.YELLOW);
            window.drawOval(500, 50, 100, 80);
            
            // Draw lines and polygons
            window.setColor(Color.WHITE);
            window.drawLine(50, 200, 200, 250);
            
            int[] xPoints = {300, 350, 400, 350, 300};
            int[] yPoints = {200, 180, 200, 220, 200};
            window.fillPolygon(xPoints, yPoints, 5);
            
            // Draw arcs
            window.setColor(Color.ORANGE);
            window.drawArc(450, 180, 100, 80, 0, 180);
            window.fillArc(600, 180, 100, 80, 180, 180);
            
            // Draw text
            window.setColor(Color.WHITE);
            window.setFont(new Font("Arial", Font.BOLD, 24));
            window.drawString("Spite2D Graphics2D-like API", 50, 350);
            
            window.setFont(new Font("Dialog", Font.PLAIN, 16));
            window.drawString("Hardware accelerated with OpenGL", 50, 380);
            window.drawString("Using AWT Color and BufferedImage objects", 50, 410);
            
            // Draw shapes using Shape objects
            window.setColor(Color.MAGENTA);
            Rectangle2D rect = new Rectangle2D.Double(50, 450, 80, 60);
            window.draw(rect);
            
            Ellipse2D ellipse = new Ellipse2D.Double(200, 450, 80, 60);
            window.fill(ellipse);
            
            // Draw a complex shape
            window.setColor(Color.CYAN);
            GeneralPath path = new GeneralPath();
            path.moveTo(350, 450);
            path.lineTo(390, 450);
            path.lineTo(370, 490);
            path.closePath();
            window.fill(path);
            
            // Demonstrate transforms
            window.setColor(Color.PINK);
            window.translate(500, 450);
            window.rotate(Math.PI / 4);
            window.fillRect(-25, -25, 50, 50);
            window.setTransform(new AffineTransform()); // Reset transform
            
            // Draw an image (create a simple test image)
            BufferedImage testImage = createTestImage();
            window.drawImage(testImage, 600, 450);
            
            // Draw with different colors and alpha
            window.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red
            window.fillRect(50, 520, 100, 50);
            
            window.setColor(new Color(0, 255, 0, 128)); // Semi-transparent green
            window.fillRect(80, 520, 100, 50);
            
            window.setColor(new Color(0, 0, 255, 128)); // Semi-transparent blue
            window.fillRect(110, 520, 100, 50);
        });
        
        // Start the window
        window.start();
        
        // Simple game loop demonstration (similar to user's GameLoop)
        long lastFrameTime = System.nanoTime();
        long targetFrameTime = 16_666_667L; // ~60 FPS (1 second / 60)
        int frameCount = 0;
        long fpsStartTime = System.nanoTime();
        double currentFPS = 0.0;
        
        while (window.isRunning()) {
            long frameStartTime = System.nanoTime();
            
            // Update game logic here
            // Game.update();
            
            // Calculate FPS
            frameCount++;
            long currentTime = System.nanoTime();
            if (currentTime - fpsStartTime >= 1_000_000_000L) { // 1 second
                currentFPS = frameCount * 1_000_000_000.0 / (currentTime - fpsStartTime);
                frameCount = 0;
                fpsStartTime = currentTime;
            }
            
            // Store FPS for rendering
            final double fpsToDisplay = currentFPS;
            
            // Update render callback to include FPS display
            window.setRenderCallback(() -> {
                // This is like getting a Graphics2D object and drawing with it
                // All operations use AWT Color and BufferedImage objects
                
                // Set background
                window.setBackground(new Color(50, 50, 100));
                
                // Draw some basic shapes
                window.setColor(Color.RED);
                window.fillRect(50, 50, 100, 80);
                
                window.setColor(Color.GREEN);
                window.drawRect(200, 50, 100, 80);
                
                window.setColor(Color.BLUE);
                window.fillOval(350, 50, 100, 80);
                
                window.setColor(Color.YELLOW);
                window.drawOval(500, 50, 100, 80);
                
                // Draw lines and polygons
                window.setColor(Color.WHITE);
                window.drawLine(50, 200, 200, 250);
                
                int[] xPoints = {300, 350, 400, 350, 300};
                int[] yPoints = {200, 180, 200, 220, 200};
                window.fillPolygon(xPoints, yPoints, 5);
                
                // Draw arcs
                window.setColor(Color.ORANGE);
                window.drawArc(450, 180, 100, 80, 0, 180);
                window.fillArc(600, 180, 100, 80, 180, 180);
                
                // Draw text
                window.setColor(Color.WHITE);
                window.setFont(new Font("Arial", Font.BOLD, 24));
                window.drawString("Spite2D Graphics2D-like API", 50, 350);
                
                window.setFont(new Font("Dialog", Font.PLAIN, 16));
                window.drawString("Hardware accelerated with OpenGL", 50, 380);
                window.drawString("Using AWT Color and BufferedImage objects", 50, 410);
                
                // Draw FPS
                window.setColor(Color.YELLOW);
                window.setFont(new Font("Monospaced", Font.BOLD, 18));
                window.drawString(String.format("FPS: %.1f", fpsToDisplay), 50, 440);
                
                // Draw shapes using Shape objects
                window.setColor(Color.MAGENTA);
                Rectangle2D rect = new Rectangle2D.Double(50, 470, 80, 60);
                window.draw(rect);
                
                Ellipse2D ellipse = new Ellipse2D.Double(200, 470, 80, 60);
                window.fill(ellipse);
                
                // Draw a complex shape
                window.setColor(Color.CYAN);
                GeneralPath path = new GeneralPath();
                path.moveTo(350, 470);
                path.lineTo(390, 470);
                path.lineTo(370, 510);
                path.closePath();
                window.fill(path);
                
                // Demonstrate transforms
                window.setColor(Color.PINK);
                window.translate(500, 470);
                window.rotate(Math.PI / 4);
                window.fillRect(-25, -25, 50, 50);
                window.setTransform(new AffineTransform()); // Reset transform
                
                // Draw an image (create a simple test image)
                BufferedImage testImage = createTestImage();
                window.drawImage(testImage, 600, 470);
                
                // Draw with different colors and alpha
                window.setColor(new Color(255, 0, 0, 128)); // Semi-transparent red
                window.fillRect(50, 540, 100, 50);
                
                window.setColor(new Color(0, 255, 0, 128)); // Semi-transparent green
                window.fillRect(80, 540, 100, 50);
                
                window.setColor(new Color(0, 0, 255, 128)); // Semi-transparent blue
                window.fillRect(110, 540, 100, 50);
            });
            
            // Render the frame
            window.repaint();
            
            // Frame rate limiting
            long frameTime = System.nanoTime() - frameStartTime;
            if (frameTime < targetFrameTime) {
                try {
                    Thread.sleep((targetFrameTime - frameTime) / 1_000_000L);
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            lastFrameTime = frameStartTime;
        }
    }
    
    private static BufferedImage createTestImage() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Draw a simple pattern
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, 32, 32);
        g2d.setColor(Color.BLUE);
        g2d.fillRect(32, 0, 32, 32);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 32, 32, 32);
        g2d.setColor(Color.YELLOW);
        g2d.fillRect(32, 32, 32, 32);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Dialog", Font.BOLD, 8));
        g2d.drawString("TEST", 20, 35);
        
        g2d.dispose();
        return img;
    }
}