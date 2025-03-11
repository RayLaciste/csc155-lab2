package code;

import java.io.*;
import java.lang.Math;
import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL4.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.common.nio.Buffers;
import org.joml.*;

public class Code extends JFrame implements GLEventListener {
    private GLCanvas myCanvas;
    private int renderingProgram;
    private int vao[] = new int[1];
    private int vbo[] = new int[4];
    private float cameraX, cameraY, cameraZ;
    private float objLocX, objLocY, objLocZ;

    // allocate variables for display() function
    private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
    private Matrix4f pMat = new Matrix4f();  // perspective matrix
    private Matrix4f vMat = new Matrix4f();  // view matrix
    private Matrix4f mMat = new Matrix4f();  // model matrix
    private Matrix4f mvMat = new Matrix4f(); // model-view matrix
    private int mvLoc, pLoc;
    private float aspect;

    private int carTexture;

    private int numObjVertices;
    private ImportedModel myModel;

    private Matrix4fStack mvStack = new Matrix4fStack();

    public Code() {
        setTitle("Chapter6 - program3");
        setSize(600, 600);
        myCanvas = new GLCanvas();
        myCanvas.addGLEventListener(this);
        this.add(myCanvas);
        this.setVisible(true);
    }

    public static void main(String[] args) {
        new Code();
    }

    public void display(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_COLOR_BUFFER_BIT);
        gl.glClear(GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(renderingProgram);

        int mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
        int pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");

        vMat.identity().setTranslation(-cameraX, -cameraY, -cameraZ);

        // Draw the Car
        mMat.identity();
        mMat.translate(objLocX, objLocY, objLocZ);

        mMat.rotateX((float) Math.toRadians(20.0f));
        mMat.rotateY((float) Math.toRadians(130.0f));
        mMat.rotateZ((float) Math.toRadians(5.0f));

        mvMat.identity()
                .rotateX((float)Math.toRadians(20.0f))
                .rotateZ((float)Math.toRadians(5.0f));
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, carTexture);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);
        gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());

        // Draw the Cube
        mMat.translation(0,	-2,	-2);

        mvMat.identity();
        mvMat.mul(vMat);
        mvMat.mul(mMat);

        gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    public void init(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        myModel = new ImportedModel("car.obj");
        renderingProgram = Utils.createShaderProgram("code/vertShader.glsl", "code/fragShader.glsl");

        float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

        setupVertices();
        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 4f;
        objLocX = 0.0f; objLocY = 0.0f; objLocZ = 0.0f;

        carTexture = Utils.loadTexture("car.png");
    }

    private void setupVertices() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

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

        // 36 vertices of the 12 triangles making up a 2 x 2 x 2 cube centered at the origin
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

        gl.glGenVertexArrays(vao.length, vao, 0);
        gl.glBindVertexArray(vao[0]);
        gl.glGenBuffers(vbo.length, vbo, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
        FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubeVertices);
        gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit() * 4, cubeBuf, GL_STATIC_DRAW);
    }

    public void dispose(GLAutoDrawable drawable) {
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
        pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
    }
}