package starsSimulation;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
    JTextField gTextField;
    JTextField massTextField;
    JTextField speedTextField;
    
    
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
 
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        gTextField = new JTextField(String.valueOf(G));
        gTextField.setPreferredSize(new Dimension(200, 30));
        controlsPanel.add(new Label("Gravity: "));
        controlsPanel.add(gTextField);
        
        massTextField = new JTextField(String.valueOf(MAX_MASS));
        massTextField.setPreferredSize(new Dimension(200, 30));
        controlsPanel.add(new Label("Mass: "));
        controlsPanel.add(massTextField);
        
        speedTextField = new JTextField(String.valueOf(MAX_SPEED));
        speedTextField.setPreferredSize(new Dimension(200, 30));
        controlsPanel.add(new Label("Velocity: "));
        controlsPanel.add(speedTextField);
        
        JButton restartButton = new JButton("Restart");
        restartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                restartScene = true;
            }
        });
        controlsPanel.add(restartButton);
        
        frame.add(controlsPanel, BorderLayout.NORTH);
        frame.pack();
        
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
            renderG.setColor(Color.BLACK);
            renderG.fillRect(0, 0, graphDim.width, graphDim.height);
            
            //Render
            render(renderG, elapsedTime);
            
            //Draw FPS
            renderG.setColor(Color.YELLOW);
            renderG.drawString("FPS: " + framesPerSecond, 10, 20);
            
            //Draw buffer
            g.drawImage(renderImg, 0, 0, null);
            
            
            Thread.yield();
	} 
    }

    
    private java.util.List<Particle> particles;
    private boolean restartScene = false;
    private static float E = 0.1f;
    
    private float G = 10;
    private float MAX_MASS = 1;
    private float MAX_SPEED = 10;
    
    
    
    /**
     * Init scene
     */
    private void init() {
        particles = new ArrayList<Particle>();
        
        restartScene();
    }
    
    private void restartScene() {
        restartScene = false;
        
        particles.clear();
        G = Float.parseFloat(gTextField.getText());
        MAX_MASS = Float.parseFloat(massTextField.getText());
        MAX_SPEED = Float.parseFloat(speedTextField.getText());
        
        
        Random rand = new Random();
        int centerX = graphDim.width / 2;
        int centerY = graphDim.height / 2;
        float r = 3;
        int minMass = 1;
        for (int i = 0; i < 1000; i++) {
            float mass = minMass + rand.nextInt((int)MAX_MASS);
            Color color = rand.nextInt(10) < 7 ? Color.WHITE : Color.YELLOW;
            Particle p = new Particle(centerX, centerY, r, mass, color);
            p.velocity.set(
                    rand.nextFloat() * MAX_SPEED * (rand.nextBoolean() ? 1 : -1),
                    rand.nextFloat() * MAX_SPEED * (rand.nextBoolean() ? 1 : -1)
                    );
            particles.add(p);
        }
    }

    
    /**
     * Main render method
     */
    private void render(Graphics2D g, float elapsedTime) {
        if(restartScene) {
            restartScene();
        }
   
        for (Particle p : particles) {
            p.resetForce();
        }
        
        for (int i = 0; i < particles.size() - 1; i++) {
            Particle p1 = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle p2 = particles.get(j);
                Particle.updateForce(G, p1, p2, graphDim);
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
            
            //g.setColor(Color.BLACK);
            //g.drawOval((int)(center.X - radius), (int)(center.Y - radius), (int)(radius * 2), (int)(radius * 2));
        }
        
        public void resetForce() {
            this.force.set(0, 0);
        }
        
        public boolean isOutside(Dimension dim) {
            if(center.X - radius < 0 || center.X + radius >= dim.width || center.Y - radius < 0 || center.Y + radius >= dim.height)
                return true;
            return false;
        }
        
        
        public static void updateForce(float g, Particle p1, Particle p2, Dimension dim) {
            if(p1.isOutside(dim) || p2.isOutside(dim))
                return;
            
            
            float dx = p2.center.X - p1.center.X;
            float dy = p2.center.Y - p1.center.Y;
            float rSq = dx * dx + dy * dy;
            float r = (float)(Math.max(Math.sqrt(rSq), E));                  
            
            
            //F = G * m1 * m2 / (r^2 + e^2)
            float f = (g * p1.mass * p2.mass) / (Math.max(rSq, E));
            
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
