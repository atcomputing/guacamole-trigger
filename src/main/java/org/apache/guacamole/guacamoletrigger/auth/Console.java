package org.apache.guacamole.guacamoletrigger.auth;

// import java.lang.ProcessHandle;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader ;
import java.lang.StringBuilder;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Console {

    ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Logger logger = LoggerFactory.getLogger(Console.class);


    private Queue<String> buffer = new CircularFifoQueue<String>(120);

    private Consumer<String> stdout;
    private Consumer<String> stderr;
    private File cwd;
    private int commandTimeout;

    public Console(){
    }

    public Console ( Consumer<String> stdout, Consumer<String> stderr, File cwd,int commandTimeout) {

        this.commandTimeout = commandTimeout;
        this.cwd = cwd;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public int run (String command, Map<String,String> environment){

        // there should never be more the 1 job running same time per host
        ProcessBuilder builder = new ProcessBuilder();

        // TODO test windows
        if ( System.getProperty("os.name").toLowerCase().contains("win")) { // is windows
            builder.command("cmd.exe", "/c", "dir"); 
        } else {
            builder.command("sh", "-c", command);
        }
        builder.directory(cwd);
        builder.environment().putAll(environment);

        try {

            Process process = builder.start();
            StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream(),stdout );
            Executors.newSingleThreadExecutor().submit(stdoutGobbler);

            StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream(),stderr);
            Executors.newSingleThreadExecutor().submit(stderrGobbler);

            if ( ! process.waitFor(commandTimeout, TimeUnit.SECONDS)){
                // process.pid() only works in java 9 or higher
                // if (System.getProperty("os.name").startsWith("Linux")) {
                //     logger.error("kill -2 -{}", + process.pid());
                //     Runtime.getRuntime().exec("kill -2 -" + process.pid()).waitFor();
                // }
                // only works with jave 9 or higher
                // ProcessHandle handle = process.toHandle();
                // handle.descendants().forEach((child) -> child.destroy());
                // handle.destroy();

                // can leave zombie process
                //  process.destroy(); process.destroyForcibly ()
            }

            return process.exitValue();

        } catch (IOException e){
            logger.error("could not start: {}\n{}", command, e.getMessage());
        } catch (InterruptedException e) {
            logger.error("command interupted: {} \n{}", command, e.getMessage());
        }
        return 1;  // fail
    }
    public String getBufferOutput(){
        StringBuilder strBuilder = new StringBuilder();
        lock.readLock().lock();
        buffer.forEach((line) -> {
            strBuilder.append(line).append("\n");
        });
        lock.readLock().unlock();
        return strBuilder.toString();

    }

    public void clear(){
        lock.writeLock().lock();
        buffer.clear();
        lock.writeLock().unlock();
        return ;

    }

    private class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {

            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach( line ->{
                lock.writeLock().lock();
                buffer.add(line.substring(0, Math.min(line.length(), 240)));
                lock.writeLock().unlock();
                consumer.accept(line);
            });
        }
    }
}
