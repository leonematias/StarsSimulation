package starsSimulation;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 *
 * @author matias.leone
 */
public class UI {

    private final static int WIN_WIDTH = 1200;
    private final static int WIN_HEIGHT = 720;
    
    private JFrame frame;
    private JTextArea logArea;
    private Canvas renderPanel;
    private BufferedImage renderImg;
    private Graphics2D renderG;
    private Dimension graphDim;
    
    
    public static void main(String[] args) {
        new UI().renderLoop();
    }
    
    public UI() {
        frame = new JFrame("Gravity");
        frame.setMinimumSize(new Dimension(WIN_WIDTH, WIN_HEIGHT));
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        
        renderPanel = new Canvas();
        frame.add(renderPanel, BorderLayout.CENTER);
 
        
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation(screenDim.width / 2 - WIN_WIDTH / 2, screenDim.height / 2 - WIN_HEIGHT / 2);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        frame.setVisible(true);
        
        
        //Create rasterizer
        graphDim = renderPanel.getSize();
        renderImg = (BufferedImage)renderPanel.createImage(graphDim.width, graphDim.height);
        renderG = renderImg.createGraphics();
        
    }
    
    /**
     * Do render loop
     */
    public void renderLoop() {
        init();
        
        long startTime = System.nanoTime();
        long currentTime;
	float lastFps = startTime;
        int frameCount = 0;
        float elapsedTime = 0;
        int framesPerSecond = 0;
        Graphics g = renderPanel.getGraphics();
        
        while(true) {

            //Compute elapsed time and FPS
            currentTime = System.nanoTime();
            elapsedTime = (currentTime - startTime) / 1000000000.0f;
            startTime = currentTime;
            if(currentTime - lastFps >= 1000000000) {
		framesPerSecond = frameCount;
		frameCount = 0;
		lastFps = currentTime;
            }
            frameCount++;
            
            //Clean
            renderG.setColor(Color.WHITE);
            renderG.fillRect(0, 0, graphDim.width, graphDim.height);
            
            //Render
            render(renderG, elapsedTime);
            
            //Draw FPS
            renderG.setColor(Color.BLACK);
            renderG.drawString("FPS: " + framesPerSecond, 10, 20);
            
            //Draw buffer
            g.drawImage(renderImg, 0, 0, null);
            
            
            Thread.yield();
	} 
    }

    
    private java.util.List<Particle> particles;
    private static float G = 10;
    
    
    /**
     * Init scene
     */
    private void init() {
        particles = new ArrayList<Particle>();
        
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                particles.add(new Particle(50 + i * 120, 50 + j * 60, 15, 30, j % 2 == 0 ? Color.BLUE : Color.RED));
            }
        }
        
        particles.add(new Particle(500, 300, 30, 10000, Color.GREEN));
    }

    
    /**
     * Main render method
     */
    private void render(Graphics2D g, float elapsedTime) {
   
        for (Particle p : particles) {
            p.resetForce();
        }
        
        for (int i = 0; i < particles.size() - 1; i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                Particle.updateForce(p1, p2, graphDim);
            }
        }
        
        for (Particle p : particles) {
            p.computePosition(elapsedTime);
            p.render(g, graphDim);
         
        }
      
    }
    
    
    
    
    private static class Particle {
        private Vector2 center;
        private float radius;
        private float mass;
        private Color color;
        private Vector2 force;
        private Vector2 velocity;
        
        public Particle(int x, int y, float r, float mass, Color color) {
            this.center = new Vector2(x, y);
            this.radius = r;
            this.mass = mass;
            this.color = color;
            this.force = new Vector2();
            this.velocity = new Vector2();
        }
        
        public void render(Graphics2D g, Dimension dim) {
            if(isOutside(dim))
                return;
            
            g.setColor(color);
            g.fillOval((int)(center.X - radius), (int)(center.Y - radius), (int)(radius * 2), (int)(radius * 2));
            
            g.setColor(Color.BLACK);
            g.drawOval((int)(center.X - radius), (int)(center.Y - radius), (int)(radius * 2), (int)(radius * 2));
        }
        
        public void resetForce() {
            this.force.set(0, 0);
        }
        
        public boolean isOutside(Dimension dim) {
            if(center.X - radius < 0 || center.X + radius >= dim.width || center.Y - radius < 0 || center.Y + radius >= dim.height)
                return true;
            return false;
        }
        
        
        public static void updateForce(Particle p1, Particle p2, Dimension dim) {
            if(p1.isOutside(dim) || p2.isOutside(dim))
                return;
            
            
            float dx = p2.center.X - p1.center.X;
            float dy = p2.center.Y - p1.center.Y;
            float rSq = dx * dx + dy * dy;
            float r = (float)Math.sqrt(rSq);         
           
            
            //F = G * m1 * m2 / r^2
            float f = (G * p1.mass * p2.mass) / rSq;
            
            //Fx = F * cos(a) = F * dx / r
            float fx = f * dx / r;
            
            //Fy = F * sin(a) = F * dy / r
            float fy = f * dy / r;
            
            p1.force.X += fx;
            p1.force.Y += fy;
            
            p2.force.X -= fx;
            p2.force.Y -= fy;
            
        }
        
        public void computePosition(float t) {
            //F = m * a  =>  a = F / m
            float ax = force.X / mass;
            float ay = force.Y / mass;
            
            //V(t) = V(0) + a * t
            velocity.X += ax * t;
            velocity.Y += ay * t;
            
            //P(t) = P(0) + V(t)
            center.X += velocity.X * t;
            center.Y += velocity.Y * t;
        }
        
    }
    

}
