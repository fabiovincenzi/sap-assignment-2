package delivery_service_tests.infrastructure;

/** Lets a test wait for the asynchronous start of the Vert.x server (same helper as Lab 9). */
public class Synchroniser {

    private boolean syncDone;

    public Synchroniser() {
        syncDone = false;
    }

    public synchronized void awaitSync() throws InterruptedException {
        while (!syncDone) {
            wait();
        }
    }

    public synchronized void notifySync() {
        syncDone = true;
        notifyAll();
    }
}
