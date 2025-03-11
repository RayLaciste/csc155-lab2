package code;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.lang.Math;
import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL4.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.common.nio.Buffers;
import org.joml.*;

public class Code extends JFrame implements GLEventListener, KeyListener {
    // Car movement
    float carLocX, velocityX, rotAngle;
    private GLCanvas myCanvas;
    private double startTime = 0.0;
    private double elapsedTime;
    private int renderingProgram;
    private int vao[] = new int[1];
    private int vbo[] = new int[10];
    private float cameraX, cameraY, cameraZ;
    // allocate variables for display() function
    private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
    private Matrix4f pMat = new Matrix4f();  // perspective matrix
    private Matrix4f vMat = new Matrix4f();  // view matrix
    private Matrix4f mMat = new Matrix4f();  // model matrix
    private Matrix4f mvMat = new Matrix4f(); // model-view matrix
    private int mvLoc, pLoc;
    private float aspect;
    private double tf;

    // Textures
    private int carTexture;
    private int groundTexture;

    private int numObjVertices;
    private ImportedModel myModel;

    private Matrix4fStack mvStack = new Matrix4fStack(16);

    public Code() {
        setTitle("Chapter6 - program3");
        setSize(1000, 1000);
        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        myCanvas.addKeyListener(this);
        this.add(myCanvas);
        this.setVisible(true);
        Animator animator = new Animator(myCanvas);
        animator.start();
    }

    public static void main(String[] args) {
        new Code();
    }

    public void display(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_COLOR_BUFFER_BIT);
        gl.glClear(GL_DEPTH_BUFFER_BIT);
        elapsedTime = System.currentTimeMillis() - startTime;

        gl.glUseProgram(renderingProgram);
        gl.glDrawArrays(GL_LINES, 0, 4);

        int mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
        int pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
        aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));


        // push view matrix onto the stack
        mvStack.pushMatrix();
        mvStack.translate(-cameraX, -cameraY, -cameraZ);

        tf = elapsedTime / 1000.0;  // time factor

        // ---------------------- Car ----------------------
        mvStack.pushMatrix();

        carLocX = (float) Math.sin(tf) * 0.5f;
        velocityX = (float) Math.cos(tf);
        rotAngle = (float) Math.atan2(velocityX, 1.0f);

        mvStack.translate(carLocX, 0.2f, 0.0f)
                .rotateX((float) Math.toRadians(20.0f))
                .rotateY(-rotAngle)
                .rotateZ((float) Math.toRadians(5.0f));
        mvStack.pushMatrix();
        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // Texture
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, carTexture);

        // Render
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());
        mvStack.popMatrix();
        mvStack.popMatrix();

        // ---------------------- Ground ----------------------
        mvStack.pushMatrix();
        mvStack.translate(0.0f, 0f, 0.0f)
                .rotateX((float) Math.toRadians(20.0f));
        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // Texture
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, groundTexture);

        // Render
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDrawArrays(GL_TRIANGLES, 0, 6);
        mvStack.popMatrix();

        // ---------------------- Pyramid ----------------------
        mvStack.pushMatrix();
        mvStack.translate(1.0f, 0f, 0.0f);
        gl.glUniformMatrix4fv(mvLoc, 1, false, mvStack.get(vals));
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        // Texture
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, groundTexture);

        // Render
        gl.glEnable(GL_DEPTH_TEST);
        gl.glDrawArrays(GL_TRIANGLES, 0, 18);
        mvStack.popMatrix();

        mvStack.popMatrix();
    }

    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        myModel = new ImportedModel("car.obj");
        renderingProgram = Utils.createShaderProgram("code/vertShader.glsl", "code/fragShader.glsl");
        startTime = System.currentTimeMillis();

        setupVertices();
        cameraX = 0.0f;
        cameraY = 0.0f;
        cameraZ = 4f;

        carTexture = Utils.loadTexture("car.png");
        groundTexture = Utils.loadTexture("brick1.jpg");

        gl.glBindTexture(GL_TEXTURE_2D, groundTexture);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT); // Tile
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }

    private void setupVertices() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        // ---------------------- Car ----------------------
        numObjVertices = myModel.getNumVertices();
        Vector3f[] vertices = myModel.getVertices();
        Vector2f[] texCoords = myModel.getTexCoords();
        Vector3f[] normals = myModel.getNormals();

        float[] pvalues = new float[numObjVertices * 3];
        float[] tvalues = new float[numObjVertices * 2];
        float[] nvalues = new float[numObjVertices * 3];

        for (int i = 0; i < numObjVertices; i++) {
            pvalues[i * 3] = (float) (vertices[i]).x();
            pvalues[i * 3 + 1] = (float) (vertices[i]).y();
            pvalues[i * 3 + 2] = (float) (vertices[i]).z();
            tvalues[i * 2] = (float) (texCoords[i]).x();
            tvalues[i * 2 + 1] = (float) (texCoords[i]).y();
            nvalues[i * 3] = (float) (normals[i]).x();
            nvalues[i * 3 + 1] = (float) (normals[i]).y();
            nvalues[i * 3 + 2] = (float) (normals[i]).z();
        }

        // ---------------------- Cube ----------------------
        float[] cubeVertices =
                {
                        -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
                        1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
                        1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f,
                        -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f,
                        1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f,
                        -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f,
                        1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f
                };

        // ---------------------- Pyramid ----------------------
        float[] pyramidPositions =
                {	-1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f,   //front
                        1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f,   //right
                        1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 0.0f, //back
                        -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 0.0f, //left
                        -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, //LF
                        1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f  //RR
                };

        float[] pyrTextureCoordinates =
                {	0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
                        0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f,
                        0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                        1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
                };

        // ---------------------- Ground ----------------------
        float[] groundVertices =
                {
                        -5.0f, 0.0f, -5.0f,  // Bottom-left
                        5.0f, 0.0f, -5.0f,  // Bottom-right
                        -5.0f, 0.0f,  5.0f,  // Top-left
                        5.0f, 0.0f, -5.0f,  // Bottom-right
                        5.0f, 0.0f,  5.0f,  // Top-right
                        -5.0f, 0.0f,  5.0f   // Top-left
                };

        float[] groundTexCoords =
                {
                        0.0f, 0.0f,  // Bottom-left
                        5.0f, 0.0f,  // Bottom-right
                        0.0f, 5.0f,  // Top-left
                        5.0f, 0.0f,  // Bottom-right
                        5.0f, 5.0f,  // Top-right
                        0.0f, 5.0f   // Top-left
                };

        // --------------------------------------------
        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(vbo.length, vbo, 0);

        // ---------------------- Car ----------------------
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

        // ---------------------- Ground ----------------------
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
        FloatBuffer groundBuf = Buffers.newDirectFloatBuffer(groundVertices);
        gl.glBufferData(GL_ARRAY_BUFFER, groundBuf.limit() * 4, groundBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
        FloatBuffer groundTex = Buffers.newDirectFloatBuffer(groundTexCoords);
        gl.glBufferData(GL_ARRAY_BUFFER, groundTex.limit() * 4, groundTex, GL_STATIC_DRAW);

        // ---------------------- Pyramid ----------------------
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
        FloatBuffer pyrBuf = Buffers.newDirectFloatBuffer(pyramidPositions);
        gl.glBufferData(GL_ARRAY_BUFFER, pyrBuf.limit()*4, pyrBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
        FloatBuffer pyrTexBuf = Buffers.newDirectFloatBuffer(pyrTextureCoordinates);
        gl.glBufferData(GL_ARRAY_BUFFER, pyrTexBuf.limit()*4, pyrTexBuf, GL_STATIC_DRAW);
        // ---------------------- Cube ----------------------
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
        FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubeVertices);
        gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit() * 4, cubeBuf, GL_STATIC_DRAW);
    }

    public void dispose(GLAutoDrawable drawable) {
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}