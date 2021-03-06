package engine;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import map.Map;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

public class RenderingEngine {
	private long window_handle;
	@SuppressWarnings("unused")
	private GLCapabilities glcapabilities;
	private int building, road, car, program, vert, frag, quadVAO, quadVBO;
	
	public RenderingEngine() {
		glfwInit();
		
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		
		window_handle = glfwCreateWindow(600, 600, "6112 Project", NULL, NULL);
		
		glfwMakeContextCurrent(window_handle);
		glfwSwapInterval(1);		
		
		glfwShowWindow(window_handle);
		
		glcapabilities = GL.createCapabilities();
		
		building = loadTexture("building.png");
		road = loadTexture("road.png");
		car = loadTexture("car.png");
		compileProgram("./res/shader");
		glClearColor(0f, 1f, 1f, 1f);
		int width[] = new int[1];
		int height[] = new int[1];
		GLFW.glfwGetWindowSize(window_handle, width, height);
		
		glViewport(0, 0, width[0], height[0]);
		
		GL20.glUseProgram(program);
		GL20.glUniform1i(GL20.glGetUniformLocation(program, "buildingTexture"), 0);
		GL20.glUniform1i(GL20.glGetUniformLocation(program, "roadTexture"), 1);
		GL20.glUniform1i(GL20.glGetUniformLocation(program, "carTexture"), 2);
		GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glBindTexture(GL11.GL_TEXTURE_2D, building);
		GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glBindTexture(GL11.GL_TEXTURE_2D, road);
		GL13.glActiveTexture(GL13.GL_TEXTURE2); GL11.glBindTexture(GL11.GL_TEXTURE_2D, car);
		
        float quadVertices[] = {
            // positions        // texture Coords
            0f,  1.0f, 0.0f, 0.0f, 1.0f,
            0f, 0f, 0.0f, 0.0f, 0.0f,
             1.0f,  1.0f, 0.0f, 1.0f, 1.0f,
             1.0f, 0f, 0.0f, 1.0f, 0.0f,
        };
        // setup plane VAO
        quadVAO = GL30.glGenVertexArrays();
        quadVBO = GL15.glGenBuffers();
        GL30.glBindVertexArray(quadVAO);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVBO);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quadVertices, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * 4, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * 4, 12);
	}
	
	public boolean windowShouldClose() {return glfwWindowShouldClose(window_handle);}
	public void pollEvents() {glfwPollEvents();}
	public void clear() {glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);}
	public void swapBuffers() {GLFW.glfwSwapBuffers(window_handle);}
	
	public void renderMap(Map map) {
		GL20.glUniform2fv(GL20.glGetUniformLocation(program, "res"), new float[]{2f/map.rowLength,2f/map.colLength});
		
		for(int y = 0; y < map.colLength; y++) for(int x = 0; x < map.rowLength; x++) {
				GL20.glUniform2fv(GL20.glGetUniformLocation(program, "pos"), new float[] {x,y});
				GL20.glUniform1i(GL20.glGetUniformLocation(program, "Block"), map.map[y * map.rowLength + x]);
				glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}
	}
	
	public void renderCar(int x, int y) {
		GL20.glUniform2fv(GL20.glGetUniformLocation(program, "pos"), new float[] {x, y});
		GL20.glUniform1i(GL20.glGetUniformLocation(program, "Block"), 'C');
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	}
	
	
	public void dispose() {
		glDeleteTextures(building);
		glDeleteTextures(road);
		glDeleteTextures(car);
		GL15.glDeleteBuffers(quadVBO);
		GL30.glDeleteVertexArrays(quadVAO);
		
		System.out.println("Disposing of Window!");
		glfwDestroyWindow(window_handle);
		glfwTerminate();
	}
	
	public void compileProgram(String path){
		try{
			String vertex = "", fragment = "";
			File f = new File(path + ".vs");
			Scanner k = new Scanner(f); while(k.hasNextLine())vertex += k.nextLine() + "\n"; k.close();
			f = new File(path + ".fs");
			k = new Scanner(f); while(k.hasNextLine())fragment += k.nextLine() + "\n"; k.close();
			program = GL20.glCreateProgram();
			vert = compileShader(vertex, GL20.GL_VERTEX_SHADER);
			frag = compileShader(fragment, GL20.GL_FRAGMENT_SHADER);
			
			GL20.glAttachShader(program, vert);
			GL20.glAttachShader(program, frag);
			GL20.glLinkProgram(program);
			
			GL20.glGetProgramiv(program, GL20.GL_LINK_STATUS, SUCCESS);
			if(SUCCESS[0] != 1){System.out.println("ERROR::PROGRAM::LINK_FAILED\n" + GL20.glGetProgramInfoLog(program));}
			
			GL20.glDeleteShader(vert);
			GL20.glDeleteShader(frag);
		}catch(Exception e){e.printStackTrace();}
	}
	
	private static int[] SUCCESS = new int[1];
	private static int compileShader(String shader_code, int shader_type){
		int shader = GL20.glCreateShader(shader_type);
		GL20.glShaderSource(shader, shader_code);
		GL20.glCompileShader(shader);
		GL20.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, SUCCESS);
		if(SUCCESS[0] != 1){
			String shaderName;
			switch(shader_type){
			case GL20.GL_VERTEX_SHADER: shaderName = "Vertex"; break;
			case GL20.GL_FRAGMENT_SHADER: shaderName = "Fragment"; break;
			default: System.err.println("Shader type unknown!"); return -1;
			}
			System.err.println(shaderName + " Shader Compilation Failed!\n" + GL20.glGetShaderInfoLog(shader));
		}
		return shader;
	}
	
	private int loadTexture(String fileName) {
		int texture = glGenTextures();
		try {
			glBindTexture(GL_TEXTURE_2D, texture);
			
			BufferedImage image = ImageIO.read(new File("./res/" + fileName));
			
			int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
	
	        ByteBuffer buffer = MemoryUtil.memAlloc(image.getWidth() * image.getHeight() * 4);
	        boolean alpha = image.getColorModel().hasAlpha();
	
	        for(int y = 0; y < image.getHeight(); y++) for(int x = 0; x < image.getWidth(); x++) {
	                int pixel = pixels[y * image.getWidth() + x];
	                
	                buffer.put((byte) ((pixel >> 16) & 0xFF)); buffer.put((byte) ((pixel >> 8) & 0xFF)); buffer.put((byte) ((pixel) & 0xFF));
	                if(alpha) buffer.put((byte) ((pixel >> 24) & 0xFF));
	                else buffer.put((byte)(0xFF));
	            }
	        buffer.flip();
	        
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	        
	        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
	        MemoryUtil.memFree(buffer);
	        glBindTexture(GL_TEXTURE_2D, 0);
		} catch (IOException e) {e.printStackTrace();}
        return texture;
	}
}
