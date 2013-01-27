package com.unstableapps.koushyachtescape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.andengine.engine.Engine;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.MoveXModifier;
import org.andengine.entity.modifier.MoveYModifier;
import org.andengine.entity.modifier.PathModifier;
import org.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.andengine.entity.modifier.PathModifier.IPathModifierListener;
import org.andengine.entity.modifier.PathModifier.Path;
import org.andengine.entity.modifier.RotationByModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.andengine.entity.scene.background.AutoParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.sprite.UncoloredSprite;
import org.andengine.entity.util.FPSLogger;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.shader.PositionTextureCoordinatesShaderProgram;
import org.andengine.opengl.shader.ShaderProgram;
import org.andengine.opengl.shader.constants.ShaderProgramConstants;
import org.andengine.opengl.shader.exception.ShaderProgramException;
import org.andengine.opengl.shader.exception.ShaderProgramLinkException;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.texture.render.RenderTexture;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.opengl.vbo.attribute.VertexBufferObjectAttributes;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.Constants;
import org.andengine.util.color.Color;
import org.andengine.util.debug.Debug;
import org.andengine.util.modifier.IModifier;
import org.andengine.util.modifier.LoopModifier;
import org.andengine.util.modifier.ease.EaseBounceInOut;
import android.opengl.GLES20;
import android.util.Log;
import android.widget.Toast;

public class KoushYachtEngine extends SimpleBaseGameActivity {

	private static final String TAG = "Koush Yacht Escape";
	public static final int CAMERA_WIDTH = 480;
	public static final int CAMERA_HEIGHT = 800;
	private static final int PIXELS_PER_SECOND = 60;
	private int PLAYER_OFFSET = 250; // how height above the bottom of display

	private Scene scene;
	private Camera mBoundChaseCamera;
	private MoveYModifier ytothetop;
	private MoveXModifier xtotap;

	private BitmapTextureAtlas mBitmapTextureAtlasYacht;
	private BitmapTextureAtlas mBitmapTextureAtlasHead;
	private BitmapTextureAtlas mBitmapTextureAtlasPirate;
	
	private TiledTextureRegion mYachtTextureRegion;
	private TiledTextureRegion mHeadTextureRegion;
	private TiledTextureRegion mPirateTextureRegion;
	private AnimatedSprite yacht;
	private BitmapTextureAtlas mAutoParallaxBackgroundTexture;
	private ITextureRegion mParallaxLayerFront;

	private ArrayList<AnimatedSprite> enemies;
	private Iterator<AnimatedSprite> itr;
	private AnimatedSprite currentenemy;
	
	private float timeAlive;
	
	@Override
	public EngineOptions onCreateEngineOptions() {
		this.mBoundChaseCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		
		return new EngineOptions(true, ScreenOrientation.PORTRAIT_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mBoundChaseCamera);
	}

	@Override
	public void onCreateResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		this.mBitmapTextureAtlasYacht = new BitmapTextureAtlas(this.getTextureManager(), 20, 94, TextureOptions.DEFAULT);
		this.mBitmapTextureAtlasHead = new BitmapTextureAtlas(this.getTextureManager(), 26, 32, TextureOptions.DEFAULT);
		this.mBitmapTextureAtlasPirate = new BitmapTextureAtlas(this.getTextureManager(), 26, 32, TextureOptions.DEFAULT);
		
		this.mYachtTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlasYacht, this, "koushyacht.png", 0, 0, 1, 1);
		this.mHeadTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlasHead, this, "koushead.png", 0, 0, 1, 1);
		this.mPirateTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(this.mBitmapTextureAtlasPirate, this, "koushead.png", 0, 0, 1, 1);
		
		this.mAutoParallaxBackgroundTexture = new BitmapTextureAtlas(this.getTextureManager(), 1024, 1024);
		this.mParallaxLayerFront = BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mAutoParallaxBackgroundTexture, this, "water.jpg", 0, 0);
		
		
		this.mBitmapTextureAtlasYacht.load();
		this.mBitmapTextureAtlasHead.load();
		this.mBitmapTextureAtlasPirate.load();
		this.mAutoParallaxBackgroundTexture.load();
	}

	@Override
	public Scene onCreateScene() {
		this.mEngine.registerUpdateHandler(new FPSLogger());

		scene = new Scene();
		this.mBoundChaseCamera.set(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

		final AutoParallaxBackground autoParallaxBackground = new AutoParallaxBackground(0, 0, 0, 5);
		final VertexBufferObjectManager vertexBufferObjectManager = this.getVertexBufferObjectManager();
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(-10.0f, new Sprite(0, CAMERA_HEIGHT - this.mParallaxLayerFront.getHeight(), this.mParallaxLayerFront, vertexBufferObjectManager)));
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(-10.0f, new Sprite(0, CAMERA_HEIGHT - this.mParallaxLayerFront.getHeight() * 2, this.mParallaxLayerFront, vertexBufferObjectManager)));
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(-10.0f, new Sprite(0, CAMERA_HEIGHT - this.mParallaxLayerFront.getHeight() * 3, this.mParallaxLayerFront, vertexBufferObjectManager)));
		autoParallaxBackground.attachParallaxEntity(new ParallaxEntity(-10.0f, new Sprite(0, CAMERA_HEIGHT - this.mParallaxLayerFront.getHeight() * 4, this.mParallaxLayerFront, vertexBufferObjectManager)));
		scene.setBackground(autoParallaxBackground);
	

		scene.registerUpdateHandler(new IUpdateHandler() {
			@Override
			public void reset() { }

			@Override
			public void onUpdate(final float pSecondsElapsed) {
				timeAlive += pSecondsElapsed;
				/* Get the scene-coordinates of the players feet. */
				itr = enemies.iterator();
				//Log.d(TAG, "There are: " + fireballs.size() + " fireballs");
				while(itr.hasNext()) {
					currentenemy = itr.next();
					if(currentenemy.isVisible() == false) {
						removeSprite(currentenemy, itr);						
					} else {
						if(currentenemy.collidesWith(yacht)) {
							showToastMessage("YOU LOSE!!!");
							KoushYachtEngine.this.finish();
							return;
						}
					}
				}
			}
		});
		
		yacht = new AnimatedSprite(mBoundChaseCamera.getWidth() / 2 - mYachtTextureRegion.getWidth() / 2 , CAMERA_HEIGHT - 200, this.mYachtTextureRegion, this.getVertexBufferObjectManager());
		scene.attachChild(yacht);
		
		scene.setOnSceneTouchListener(new IOnSceneTouchListener() {
			@Override
			public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
				switch(pSceneTouchEvent.getAction()) {				
				case TouchEvent.ACTION_DOWN:
				case TouchEvent.ACTION_MOVE:
					if(Math.abs(yacht.getX() - pSceneTouchEvent.getX()) < 20) {
						yacht.unregisterEntityModifier(xtotap);
						yacht.setPosition(pSceneTouchEvent.getX() - yacht.getWidth() / 2, yacht.getY());
					} else {
						yacht.unregisterEntityModifier(xtotap);
						xtotap = new MoveXModifier(0.3f, yacht.getX(), pSceneTouchEvent.getX() - yacht.getWidth() / 2);
						yacht.registerEntityModifier(xtotap);
					}
						
					break;
				
				}
				return true;
			}
		});
	
		enemies = new ArrayList<AnimatedSprite>();
		createSpriteSpawnTimeHandler();

		timeAlive = 0f;

		return scene;
	}
	
	
	private void showToastMessage(final String message) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(KoushYachtEngine.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	
	public void removeSprite(final AnimatedSprite _sprite, Iterator it) {
		    runOnUpdateThread(new Runnable() {
		        @Override
		        public void run() {
		            scene.detachChild(_sprite);
		        }
		    });
		    it.remove();
	}
	
	private void createSpriteSpawnTimeHandler() {
	    TimerHandler spriteTimerHandler;
	    float mEffectSpawnDelay = .8f;

	    spriteTimerHandler = new TimerHandler(mEffectSpawnDelay, true,
	    new ITimerCallback() {

	        @Override
	        public void onTimePassed(TimerHandler pTimerHandler) {
	            addTarget();
	        }
	    });

	    getEngine().registerUpdateHandler(spriteTimerHandler);
	}
	
	public void addTarget() {
	    Random rand = new Random();

	    int y = -10;
	    
	    int minX = 0;
	    int maxX = (int) (mBoundChaseCamera.getWidth() - mPirateTextureRegion
	        .getWidth());
	    int rangeX = maxX - minX;
	    int x = rand.nextInt(rangeX) + minX;
	    Log.d(TAG, "Creating at: " + x + "," + y);
	    AnimatedSprite target = new AnimatedSprite(x, y, mPirateTextureRegion, this.getVertexBufferObjectManager());
	    scene.attachChild(target);

	    int minDuration = 1;
	    int maxDuration = 7;
	    int rangeDuration = maxDuration - minDuration;
	    int actualDuration = rand.nextInt(rangeDuration) + minDuration;

	    MoveYModifier mod = new MoveYModifier(actualDuration, target.getY(),
	        mBoundChaseCamera.getHeight());
	    target.registerEntityModifier(mod.deepCopy());
	    final RotationByModifier rotMod = new RotationByModifier(maxDuration, 1300);
	    target.registerEntityModifier(rotMod);
	    
	    enemies.add(target);

	}


}

