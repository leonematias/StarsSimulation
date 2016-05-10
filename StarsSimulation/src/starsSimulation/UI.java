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
    private JTextField gTextField;
    private JTextField massTextField;
    private JTextField speedTextField;
    private JTextField particlesTextField;
    
    
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
        
        particlesTextField = new JTextField(String.valueOf(PARTICLES_COUNT));
        particlesTextField.setPreferredSize(new Dimension(200, 30));
        controlsPanel.add(new Label("Particles: "));
        controlsPanel.add(particlesTextField);
        
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
    private static float E2 = E * E;
    
    private float G = 10;
    private float MAX_MASS = 1;
    private float MAX_SPEED = 30;
    private int PARTICLES_COUNT = 1000;
    
    
    
    /**
     * Init scene
     */
    private void init() {
        particles = new ArrayList<Particle>();
    }
    
    private void restartScene() {
        restartScene = false;
        
        particles.clear();
        G = Float.parseFloat(gTextField.getText());
        MAX_MASS = Float.parseFloat(massTextField.getText());
        MAX_SPEED = Float.parseFloat(speedTextField.getText());
        PARTICLES_COUNT = Integer.parseInt(particlesTextField.getText());
        
        
        Random rand = new Random();
        int centerX = graphDim.width / 2;
        int centerY = graphDim.height / 2;
        float r = 3;
        int minMass = 1;
        for (int i = 0; i < PARTICLES_COUNT; i++) {
            float mass = minMass + rand.nextInt((int)MAX_MASS);
            Color color = rand.nextInt(10) < 7 ? Color.WHITE : Color.YELLOW;
            int dx = rand.nextInt(10) * (rand.nextBoolean() ? 1 : -1);
            int dy = rand.nextInt(10) * (rand.nextBoolean() ? 1 : -1);
            Particle p = new Particle(
                    centerX + dx, 
                    centerY + dy, 
                    r, 
                    mass, 
                    color);
            
            float vx = rand.nextFloat() * MAX_SPEED * (rand.nextBoolean() ? 1 : -1);
            float vy = rand.nextFloat() * MAX_SPEED * (rand.nextBoolean() ? 1 : -1);
            p.velocity.set(vx, vy);
            particles.add(p);
        }
    }

    
    /**
     * Main render method
     */
    private void render(Graphics2D g, float elapsedTime) {
        if(restartScene) {
            restartScene();
            elapsedTime = 0;
        }
   
        for (Particle p : particles) {
            p.reset();
        }
        
        for (int i = 0; i < particles.size(); i++) {
            Particle p1 = particles.get(i);
            for (int j = 0; j < particles.size(); j++) {
                if(i == j)
                    continue;
                
                Particle p2 = particles.get(j);
                p1.updateAccelation(p2, graphDim);
            }
            
        }
        
        for (Particle p : particles) {
            p.computePosition(elapsedTime, G);
            p.render(g, graphDim);
         
        }
      
    }
    
    
    
    
    private static class Particle {
        private Vector2 center;
        private float radius;
        private float mass;
        private Color color;
        private Vector2 acceleration;
        private Vector2 velocity;
        
        public Particle(int x, int y, float r, float mass, Color color) {
            this.center = new Vector2(x, y);
            this.radius = r;
            this.mass = mass;
            this.color = color;
            this.velocity = new Vector2();
            this.acceleration = new Vector2();
        }
        
        public void render(Graphics2D g, Dimension dim) {
            if(isOutside(dim))
                return;
            
            g.setColor(color);
            g.fillOval((int)(center.X - radius), (int)(center.Y - radius), (int)(radius * 2), (int)(radius * 2));
            
            //g.setColor(Color.BLACK);
            //g.drawOval((int)(center.X - radius), (int)(center.Y - radius), (int)(radius * 2), (int)(radius * 2));
        }
        
        public void reset() {
            this.acceleration.set(0, 0);
        }
        
        public boolean isOutside(Dimension dim) {
            if(center.X - radius < 0 || center.X + radius >= dim.width || center.Y - radius < 0 || center.Y + radius >= dim.height)
                return true;
            return false;
        }
        
        
        public void updateAccelation(Particle p2, Dimension dim) {
            /*if(this.isOutside(dim) || p2.isOutside(dim))
                return;   */   
            
            float dx = p2.center.X - this.center.X;
            float dy = p2.center.Y - this.center.Y;
            float r2 = dx * dx + dy * dy;              
            
            float div = (float)Math.pow((r2 + E2), 3/2);
            float ax = p2.mass * dx / div;
            float ay = p2.mass * dy / div;
            
            this.acceleration.X += ax;
            this.acceleration.Y += ay;
        }
        
        public void computePosition(float t, float g) {
            this.acceleration.mul(g);
            
            //V(t) = V(0) + a * t
            velocity.X += this.acceleration.X * t;
            velocity.Y += this.acceleration.Y * t;
            
            //P(t) = P(0) + V(t)
            center.X += velocity.X * t;
            center.Y += velocity.Y * t;
        }
        
    }
    

}
