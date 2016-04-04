package com.sunshineapps.rift.experimental;

import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.Version;
import org.lwjgl.system.Library;


public final class Client implements ClientCallback {
    private RiftWindow130 rift;
    private MirrorWindow mirrorWindow;
    private final FPSCounter fpsCounter = new FPSCounter();
    private final Scene scene2 = new SceneGlobe();
    private final Scene scene3 = new SceneRoom();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean renderScene2 = new AtomicBoolean(true);
    private final AtomicBoolean renderScene3 = new AtomicBoolean(false);
    private float angle = 0.0f;
    
    public void run() {
        System.out.println("Java " + System.getProperty("java.version"));
        System.out.println("LWJGL " + Library.JNI_LIBRARY_NAME + "-" + Version.getVersion());
        
        // step 0 - create rift
        System.out.println("step 0 - create rift");
        rift = new RiftWindow130(this);
     
        // step 8 - create mirror window
        System.out.println("step 8 - create mirror window");
        mirrorWindow = new MirrorWindow(this, "Hello VR World!", 30, rift.getPixelWidth()/2, rift.getCanvasRatio());         // Our mirror window can be smaller and updated less frequently than the rift
        mirrorWindow.setCyclopsMode(true);                                                          // We only need one eye...
        
        // display
        try {
            rift.init();
            mirrorWindow.init(rift.getMirrorTexture(mirrorWindow.getWindowW(), mirrorWindow.getWindowH()));
            scene2.init();
            scene3.init();
            fpsCounter.init();
            while (shutdown.get() == false) {
                rift.render();
                mirrorWindow.render();
                fpsCounter.frameDone();
            }
        } finally {
            fpsCounter.shutdown();
            mirrorWindow.close();
            rift.shutdown();
        } 
    }
    
    @Override
    public void drawScene(final Matrix4f mat) {
        if (renderScene2.get()) {
            mat.translate(new Vector3f(0.0f, 0.0f, -130.0f)); 
            angle += .002;
            mat.rotate(angle, new Vector3f(0.0f, 1.0f, 0.0f));
            scene2.render();
        }
        if (renderScene3.get()) {
            scene3.render();
        }
    }
    
    @Override
    public void keyPressed(final int key) {
        System.out.println("key="+key);
        if (key == '2') {
            renderScene2.set(!renderScene2.get());
            renderScene3.set(false);
        } else if (key == '3') {
            renderScene3.set(!renderScene3.get());
            renderScene2.set(false);
        } else if (key == 300) {       //F11 toggle Performance HUD
            rift.toggleHUD();
        }
    }

    @Override
    public void shutdown() {
        shutdown.set(true);
    }
    
    public static void main(String[] args) {
        new Client().run();
    }

}