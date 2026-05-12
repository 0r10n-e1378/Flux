package core;

public class Loop implements Runnable {
    private final Runnable tick;
    private boolean running;
    private Thread thread;

    public Loop(Runnable tick) {
        this.tick = tick;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this, "GameLoop");
        thread.start();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            tick.run();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
}
