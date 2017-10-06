package com.example.akhil.paddlegame;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by akhil on 6/10/17.
 */
class GameView extends SurfaceView implements Runnable {

    boolean initialFlag = true;
    boolean finalFlag = false;
    boolean flag1 = false;
    boolean flag2 = false;
    Thread gameThread = null;

    SurfaceHolder ourHolder;

    volatile boolean playing;

    boolean paused = true;
    Canvas canvas;
    Paint paint;
    long fps;
    private long timeThisFrame;

    int screenX;
    int screenY;

    Paddle paddle;


    Ball ball;

    // Up to 200 bricks
    Brick[] bricks = new Brick[200];
    int numBricks = 0;

    int score = 0;
    int lives = 3;

    public GameView(Context context) {

        super(context);
        ourHolder = getHolder();
        paint = new Paint();

        Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
        //this will laod the resolution in to the point object
        Point size = new Point();
        display.getSize(size);

        screenX = size.x;
        screenY = size.y;

        paddle = new Paddle(screenX, screenY);

        ball = new Ball(screenX, screenY);
        createBricksAndRestart();
    }

    public void createBricksAndRestart() {

        ball.reset(screenX, screenY);

        int brickWidth = screenX / 8;
        int brickHeight = screenY / 10;

        // Build a wall of bricks
        numBricks = 0;
        for (int column = 0; column < 8; column++) {
            for (int row = 0; row < 4; row++) {
                bricks[numBricks] = new Brick(row, column, brickWidth, brickHeight);
                numBricks++;
            }
        }
        if (lives == 0) {

            score = 0;
            lives = 3;
        }
    }

    @Override
    public void run() {
        while (playing)
        {
            long startFrameTime = System.currentTimeMillis();
            // Update the frame
            if (!paused)
            {
                update();
            }
            draw();
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1)
            {
                fps = 1000 / timeThisFrame;
            }
        }
    }
    public void update()
    {
        paddle.update(fps);

        ball.update(fps);

        for (int i = 0; i < numBricks; i++)
        {
            if (bricks[i].getVisibility())
            {
                if (RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                    bricks[i].setInvisible();
                    ball.reverseYVelocity();
                    score = score + 10;
                }
            }
        }
        // Check for ball colliding with paddle
        if (RectF.intersects(paddle.getRect(), ball.getRect()))
        {
            ball.setRandomXVelocity();
            ball.reverseYVelocity();
            ball.clearObstacleY(paddle.getRect().top - 2);
        }
        // Bounce the ball back when it hits the bottom of screen
        if (ball.getRect().bottom > screenY)
        {
            ball.reverseYVelocity();
            ball.clearObstacleY(screenY - 2);

            // Lose a life
            lives--;
            if(lives == 1) finalFlag = true;
            if (lives == 0)
            {
                flag1 = true;
                paused = true;
                createBricksAndRestart();
            }
        }

        // Bounce the ball back when it hits the top of screen
        if (ball.getRect().top < 0)

        {
            ball.reverseYVelocity();
            ball.clearObstacleY(12);
        }

        // If the ball hits left wall bounce
        if (ball.getRect().left < 0)

        {
            ball.reverseXVelocity();
            ball.clearObstacleX(2);
        }

        // If the ball hits right wall bounce
        if (ball.getRect().right > screenX - 10) {

            ball.reverseXVelocity();
            ball.clearObstacleX(screenX - 22);

        }

        // Pause if cleared screen
        if (score == numBricks * 10)
        {
            paused = true;
            createBricksAndRestart();
        }
    }

    public void draw() {

        if (ourHolder.getSurface().isValid()) {
            // Lock the canvas ready to draw
            canvas = ourHolder.lockCanvas();

            //for the background color
            canvas.drawColor(Color.argb(255, 23, 55, 25));

            // Choose the brush color for drawing
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Draw the paddle
            canvas.drawRect(paddle.getRect(), paint);

            // Draw the ball
            canvas.drawRect(ball.getRect(), paint);

            // Change the brush color for drawing
            paint.setColor(Color.argb(255, 249, 129, 0));

            // Draw the bricks if visible
            for (int i = 0; i < numBricks; i++)
            {
                if (bricks[i].getVisibility())
                {
                    canvas.drawRect(bricks[i].getRect(), paint);
                }
            }
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Draw the score
            paint.setTextSize(40);
            canvas.drawText("Score: " + score + "   Lives: " + lives, 10, 50, paint);
            if(initialFlag)
            {
                paint.setTextSize(70);
                canvas.drawText("Touch left or right to move paddle",10,screenY/2 , paint);
            }
            if (score == numBricks * 10) {
                paint.setTextSize(90);
                canvas.drawText("YOU WON!", 10, screenY / 2, paint);
            }
            if (lives == 3 && finalFlag && flag1)
            {
                paint.setTextSize(90);
                canvas.drawText("YOU LOSE!", 10, screenY / 2,paint);
            }
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }
    public void pause() {
        playing = false;
        try
        {
            gameThread.join();
        }
        catch (InterruptedException e)
        {
            Log.e("Error:", "joining thread");
        }
    }
    public void resume() {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        flag1=false;
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK)
        {
            // Player has touched the screen
            case MotionEvent.ACTION_DOWN:
                paused = false;
                if (motionEvent.getX() > screenX / 2)
                {
                    initialFlag = false;
                    paddle.setMovementState(paddle.RIGHT);
                }
                else
                {
                    initialFlag = false;
                    paddle.setMovementState(paddle.LEFT);
                }
                break;
            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:
                paddle.setMovementState(paddle.STOPPED);
                break;
        }
        return true;
    }
}