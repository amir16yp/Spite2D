package spite2d;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class ComplexTest {
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int MAX_PARTICLES = 500;
    private static final int MAX_BOUNCING_BALLS = 20;
    private static final int MAX_ROTATING_SHAPES = 15;
    
    // Thread-safe data structures
    private static final List<Particle> particles = new CopyOnWriteArrayList<>();
    private static final List<BouncingBall> bouncingBalls = new CopyOnWriteArrayList<>();
    private static final List<RotatingShape> rotatingShapes = new CopyOnWriteArrayList<>();
    private static final AtomicInteger fpsCounter = new AtomicInteger(0);
    private static final AtomicInteger particleCount = new AtomicInteger(0);
    private static final AtomicInteger ballCount = new AtomicInteger(0);
    private static final AtomicInteger shapeCount = new AtomicInteger(0);
    
    // Thread control
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final ExecutorService executor = Executors.newFixedThreadPool(6);
    private static final ReentrantLock renderLock = new ReentrantLock();
    
    // Performance tracking
    private static volatile long lastFpsUpdate = System.nanoTime();
    private static volatile double currentFPS = 0.0;
    private static volatile long totalFrames = 0;
    
    // Random generator for each thread
    private static final ThreadLocal<Random> threadRandom = ThreadLocal.withInitial(Random::new);
    
    public static void main(String[] args) {
        SpiteWindow window = new SpiteWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Spite2D - Complex Multithreaded Test");
        
        // Start background threads
        startParticleSystem();
        startPhysicsSimulation();
        startShapeAnimation();
        startPerformanceMonitor();
        startInputHandler(window);
        
        // Set up the main render callback
        window.setRenderCallback(() -> {
            renderLock.lock();
            try {
                renderFrame(window);
            } finally {
                renderLock.unlock();
            }
        });
        
        // Start the window
        window.start();
        
        // Main game loop
        long lastFrameTime = System.nanoTime();
        long targetFrameTime = 16_666_667L; // ~60 FPS
        
        while (window.isRunning() && running.get()) {
            long frameStartTime = System.nanoTime();
            
            // Update FPS counter
            fpsCounter.incrementAndGet();
            totalFrames++;
            
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
        
        // Cleanup
        shutdown();
    }
    
    private static void renderFrame(SpiteWindow window) {
        // Set background with gradient effect
        window.setBackground(new Color(20, 20, 40));
        
        // Draw background grid
        drawBackgroundGrid(window);
        
        // Draw particles
        drawParticles(window);
        
        // Draw bouncing balls
        drawBouncingBalls(window);
        
        // Draw rotating shapes
        drawRotatingShapes(window);
        
        // Draw UI overlay
        drawUIOverlay(window);
        
        // Draw performance metrics
        drawPerformanceMetrics(window);
    }
    
    private static void drawBackgroundGrid(SpiteWindow window) {
        window.setColor(new Color(50, 50, 80, 100));
        window.setStroke(new BasicStroke(1.0f));
        
        // Draw vertical lines
        for (int x = 0; x < WINDOW_WIDTH; x += 50) {
            window.drawLine(x, 0, x, WINDOW_HEIGHT);
        }
        
        // Draw horizontal lines
        for (int y = 0; y < WINDOW_HEIGHT; y += 50) {
            window.drawLine(0, y, WINDOW_WIDTH, y);
        }
    }
    
    private static void drawParticles(SpiteWindow window) {
        for (Particle particle : particles) {
            if (particle.isActive()) {
                window.setColor(particle.getColor());
                window.fillOval((int)particle.x, (int)particle.y, 
                               (int)particle.size, (int)particle.size);
            }
        }
    }
    
    private static void drawBouncingBalls(SpiteWindow window) {
        for (BouncingBall ball : bouncingBalls) {
            window.setColor(ball.getColor());
            window.fillOval((int)ball.x, (int)ball.y, 
                           (int)ball.size, (int)ball.size);
            
            // Draw ball trail
            window.setColor(new Color(ball.getColor().getRed(), 
                                    ball.getColor().getGreen(), 
                                    ball.getColor().getBlue(), 50));
            for (int i = 0; i < ball.trail.size() - 1; i++) {
                Point2D.Double p1 = ball.trail.get(i);
                Point2D.Double p2 = ball.trail.get(i + 1);
                window.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }
        }
    }
    
    private static void drawRotatingShapes(SpiteWindow window) {
        for (RotatingShape shape : rotatingShapes) {
            window.setColor(shape.getColor());
            
            // Apply transform for rotation
            window.translate(shape.x, shape.y);
            window.rotate(shape.rotation);
            
            switch (shape.type) {
                case TRIANGLE:
                    int[] xPoints = {0, -20, 20};
                    int[] yPoints = {-20, 20, 20};
                    window.fillPolygon(xPoints, yPoints, 3);
                    break;
                case SQUARE:
                    window.fillRect(-15, -15, 30, 30);
                    break;
                case STAR:
                    drawStar(window, 0, 0, 20, 10, 5);
                    break;
                case CROSS:
                    window.fillRect(-15, -5, 30, 10);
                    window.fillRect(-5, -15, 10, 30);
                    break;
            }
            
            // Reset transform
            window.setTransform(new AffineTransform());
        }
    }
    
    private static void drawStar(SpiteWindow window, int x, int y, int outerRadius, int innerRadius, int points) {
        int[] xPoints = new int[points * 2];
        int[] yPoints = new int[points * 2];
        
        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI * i / points;
            int radius = (i % 2 == 0) ? outerRadius : innerRadius;
            xPoints[i] = x + (int)(radius * Math.cos(angle));
            yPoints[i] = y + (int)(radius * Math.sin(angle));
        }
        
        window.fillPolygon(xPoints, yPoints, points * 2);
    }
    
    private static void drawUIOverlay(SpiteWindow window) {
        // Draw semi-transparent overlay
        window.setColor(new Color(0, 0, 0, 150));
        window.fillRect(10, 10, 300, 120);

        // Draw UI text
        window.setColor(Color.WHITE);
        window.setFont(new Font("Monospaced", Font.BOLD, 14));
        window.drawString("Spite2D Complex Test", 20, 30);
        window.drawString("Multithreaded Rendering Demo", 20, 50);
        
        window.setFont(new Font("Monospaced", Font.PLAIN, 12));
        window.drawString("Particles: " + particleCount.get(), 20, 70);
        window.drawString("Balls: " + ballCount.get(), 20, 85);
        window.drawString("Shapes: " + shapeCount.get(), 20, 100);
        window.drawString("Total Frames: " + totalFrames, 20, 115);
    }
    
    private static void drawPerformanceMetrics(SpiteWindow window) {
        window.setColor(Color.YELLOW);
        window.setFont(new Font("Monospaced", Font.BOLD, 16));
        window.drawString(String.format("FPS: %.1f", currentFPS), WINDOW_WIDTH - 150, 30);
        
        // Draw FPS bar
        window.setColor(Color.GREEN);
        int barWidth = (int)((currentFPS / 60.0) * 100);
        window.fillRect(WINDOW_WIDTH - 150, 40, barWidth, 10);
        window.setColor(Color.WHITE);
        window.drawRect(WINDOW_WIDTH - 150, 40, 100, 10);
    }
    
    private static void startParticleSystem() {
        executor.submit(() -> {
            Random random = threadRandom.get();
            
            while (running.get()) {
                try {
                    // Add new particles
                    if (particles.size() < MAX_PARTICLES) {
                        particles.add(new Particle(
                            random.nextDouble() * WINDOW_WIDTH,
                            random.nextDouble() * WINDOW_HEIGHT,
                            random.nextDouble() * 4 - 2,
                            random.nextDouble() * 4 - 2,
                            random.nextDouble() * 10 + 5,
                            new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                        ));
                        particleCount.incrementAndGet();
                    }
                    
                    // Update existing particles
                    particles.removeIf(particle -> !particle.update());
                    
                    Thread.sleep(50); // 20 FPS particle system
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }
    
    private static void startPhysicsSimulation() {
        executor.submit(() -> {
            Random random = threadRandom.get();
            
            while (running.get()) {
                try {
                    // Add new bouncing balls
                    if (bouncingBalls.size() < MAX_BOUNCING_BALLS) {
                        bouncingBalls.add(new BouncingBall(
                            random.nextDouble() * WINDOW_WIDTH,
                            random.nextDouble() * WINDOW_HEIGHT,
                            random.nextDouble() * 6 - 3,
                            random.nextDouble() * 6 - 3,
                            random.nextDouble() * 20 + 10,
                            new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                        ));
                        ballCount.incrementAndGet();
                    }
                    
                    // Update physics
                    for (BouncingBall ball : bouncingBalls) {
                        ball.update();
                    }
                    
                    Thread.sleep(16); // ~60 FPS physics
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }
    
    private static void startShapeAnimation() {
        executor.submit(() -> {
            Random random = threadRandom.get();
            
            while (running.get()) {
                try {
                    // Add new rotating shapes
                    if (rotatingShapes.size() < MAX_ROTATING_SHAPES) {
                        rotatingShapes.add(new RotatingShape(
                            random.nextDouble() * WINDOW_WIDTH,
                            random.nextDouble() * WINDOW_HEIGHT,
                            random.nextDouble() * 0.1 - 0.05,
                            RotatingShape.ShapeType.values()[random.nextInt(RotatingShape.ShapeType.values().length)],
                            new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                        ));
                        shapeCount.incrementAndGet();
                    }
                    
                    // Update rotations
                    for (RotatingShape shape : rotatingShapes) {
                        shape.update();
                    }
                    
                    Thread.sleep(16); // ~60 FPS animation
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }
    
    private static void startPerformanceMonitor() {
        executor.submit(() -> {
            while (running.get()) {
                try {
                    Thread.sleep(1000); // Update every second
                    
                    int frames = fpsCounter.getAndSet(0);
                    long now = System.nanoTime();
                    long elapsed = now - lastFpsUpdate;
                    currentFPS = frames * 1_000_000_000.0 / elapsed;
                    lastFpsUpdate = now;
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
    }
    
    private static void startInputHandler(SpiteWindow window) {
        executor.submit(() -> {
            // Add mouse listener for interactive features
            window.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // Add explosion effect at click point
                    Random random = threadRandom.get();
                    for (int i = 0; i < 20; i++) {
                        particles.add(new Particle(
                            e.getX(),
                            e.getY(),
                            random.nextDouble() * 10 - 5,
                            random.nextDouble() * 10 - 5,
                            random.nextDouble() * 8 + 3,
                            new Color(255, random.nextInt(256), 0)
                        ));
                    }
                }
            });
        });
    }
    
    private static void shutdown() {
        running.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
    
    // Data classes
    static class Particle {
        double x, y, vx, vy, size;
        Color color;
        int life;
        static final int MAX_LIFE = 100;
        
        public Particle(double x, double y, double vx, double vy, double size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.life = MAX_LIFE;
        }
        
        public boolean update() {
            x += vx;
            y += vy;
            vy += 0.1; // gravity
            life--;
            
            // Bounce off walls
            if (x < 0 || x > WINDOW_WIDTH) vx = -vx;
            if (y > WINDOW_HEIGHT) vy = -vy * 0.8;
            
            return life > 0 && y < WINDOW_HEIGHT + 100;
        }
        
        public boolean isActive() { return life > 0; }
        public Color getColor() { 
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 
                           (int)(255 * life / (double)MAX_LIFE)); 
        }
    }
    
    static class BouncingBall {
        double x, y, vx, vy, size;
        Color color;
        List<Point2D.Double> trail = new ArrayList<>();
        static final int MAX_TRAIL = 10;
        
        public BouncingBall(double x, double y, double vx, double vy, double size, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
        }
        
        public void update() {
            // Add current position to trail
            trail.add(new Point2D.Double(x, y));
            if (trail.size() > MAX_TRAIL) {
                trail.remove(0);
            }
            
            x += vx;
            y += vy;
            vy += 0.2; // gravity
            
            // Bounce off walls
            if (x < size/2 || x > WINDOW_WIDTH - size/2) {
                vx = -vx * 0.9;
                x = Math.max(size/2, Math.min(WINDOW_WIDTH - size/2, x));
            }
            if (y > WINDOW_HEIGHT - size/2) {
                vy = -vy * 0.8;
                y = WINDOW_HEIGHT - size/2;
            }
        }
        
        public Color getColor() { return color; }
    }
    
    static class RotatingShape {
        double x, y, rotation, rotationSpeed;
        ShapeType type;
        Color color;
        
        enum ShapeType { TRIANGLE, SQUARE, STAR, CROSS }
        
        public RotatingShape(double x, double y, double rotationSpeed, ShapeType type, Color color) {
            this.x = x;
            this.y = y;
            this.rotationSpeed = rotationSpeed;
            this.type = type;
            this.color = color;
        }
        
        public void update() {
            rotation += rotationSpeed;
            if (rotation > Math.PI * 2) rotation -= Math.PI * 2;
        }
        
        public Color getColor() { return color; }
    }
} 