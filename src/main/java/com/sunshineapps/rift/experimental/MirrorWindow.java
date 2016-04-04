package com.sunshineapps.rift.experimental;

//import static org.lwjgl.glfw.Callbacks.errorCallbackPrint;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.ARBFramebufferObject;

public final class MirrorWindow {
    private final ClientCallback callback;
    private final long window;
    private final int windowW;
    private final int windowH;
    private final GLFWErrorCallback errorfun;
    private final GLFWKeyCallback keyfun;
    private final AtomicBoolean updateingWindow = new AtomicBoolean(false);
    
    private int mirrorFBId;
    private int blitWidth;
    
    //FPS
    private final FPSCounter fpsCounter = new FPSCounter();
    
    private final ScheduledExecutorService executorST = Executors.newSingleThreadScheduledExecutor();
    
    private Runnable displayJob = new Runnable() {
        public void run() {
            updateingWindow.compareAndSet(false, true);    //only if not already update pending!
        }
    };
    
    public MirrorWindow(final ClientCallback client, final String title, final int fps, final int width, final float asspectRatio) {
        callback = client;
        
        errorfun = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorfun);
        
        if (glfwInit() != GL_TRUE) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        windowW = width; 
        windowH = (int) (width / asspectRatio);
        blitWidth = windowW;
        
        glfwWindowHint(GLFW_RESIZABLE, GL_FALSE);
        
        window = glfwCreateWindow(windowW, windowH, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        keyfun = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if ( action != GLFW_RELEASE) {
                    return;
                }
                System.out.println("key="+key);
                switch (key) {
                    case GLFW_KEY_ESCAPE:
                        glfwSetWindowShouldClose(window, GL_TRUE);
                        break;
//                    case GLFW_KEY_R:
      //                  ovrHmd_RecenterPose(hmd);
  //                      break;
                    default:
                        callback.keyPressed(key);
                        break;
                            
                }
            }
        };
        glfwSetKeyCallback(window, keyfun);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);              //TODO swap is handled on rift, so this is just our mirror window now, actually it does affect FPS when on rift
        glfwShowWindow(window);
        
        fpsCounter.init();
        executorST.scheduleAtFixedRate(displayJob, 0, (long)(((float)1 / fps) * 1000), TimeUnit.MILLISECONDS);
    }

    public void init(final int mirrorFBId) {
        this.mirrorFBId = mirrorFBId;
    }
    
    public void setCyclopsMode(final boolean cyclops) {
        if (cyclops) {
            blitWidth = windowW/2;
            glfwSetWindowSize(window, blitWidth, windowH);
        } else {
            blitWidth = windowW;
            glfwSetWindowSize(window, windowW, windowH);
        }
    }
    
    public void render() {
        if (glfwWindowShouldClose(window) == GL_TRUE) {
            callback.shutdown();
            return;
        }

        if (updateingWindow.get() == true) {
            glfwPollEvents();

            if (mirrorFBId != 0) {
                // Blit mirror texture to back buffer
                ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_READ_FRAMEBUFFER, mirrorFBId);
                ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_DRAW_FRAMEBUFFER, 0);
                ARBFramebufferObject.glBlitFramebuffer(0, windowH, blitWidth, 0, 0, 0, blitWidth, windowH, GL_COLOR_BUFFER_BIT, GL_NEAREST);
                ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_READ_FRAMEBUFFER, 0);
                glfwSwapBuffers(window);
            }
            fpsCounter.frameDone();
            updateingWindow.set(false);
        }
    }
    
    public int getWindowW() {
        return windowW;
    }

    public int getWindowH() {
        return windowH;
    }
    
    public void close() {
        fpsCounter.shutdown();
        executorST.shutdown();
        glfwDestroyWindow(window);
        glfwTerminate();
        keyfun.free();
        errorfun.free();
    }

}
