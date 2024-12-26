import java.io.File;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class Ex2 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Зчитуємо вхідні дані
        String directoryPath = "./files"; // Директорія за замовчуванням
        String fileExtension = ".pdf"; // Формат файлу за замовчуванням

        // Використання ExecutorService (Work Dealing)
        System.out.println("Using ExecutorService:");
        long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newFixedThreadPool(4);
        Callable<Integer> task = () -> countFiles(directoryPath, fileExtension);
        Future<Integer> countExecutor = executor.submit(task);

        System.out.println("Count: " + countExecutor.get());
        long endTime = System.currentTimeMillis();

        System.out.println("Work Stealing Time: " + (endTime - startTime) + " ms");
        executor.shutdown();

        // Використання ForkJoin Framework (Work Stealing)
        System.out.println("\nUsing ForkJoin Framework:");
        startTime = System.currentTimeMillis();
        ForkJoinPool forkJoinPool = new ForkJoinPool();

        FileCounterTask task1 = new FileCounterTask(new File(directoryPath), fileExtension);
        int countForkJoin = forkJoinPool.invoke(task1);

        endTime = System.currentTimeMillis();
        System.out.println("Count: " + countForkJoin);
        System.out.println("Work Stealing Time: " + (endTime - startTime) + " ms");

    }

    // Методи реалізації
    // -------------------------------------------------------------------------

    // Метод для використання ExecutorService (Work Dealing)
    public static int countFiles(String directoryPath, String extension) {
        File root = new File(directoryPath);
        if (!root.isDirectory()) {
            throw new IllegalArgumentException("Path is not a directory!");
        }

        int count = 0;
        File[] files = root.listFiles();
        if (files == null) return 0;

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countFiles(file.getAbsolutePath(), extension);
                } else if (file.getName().endsWith(extension)) {
                    count++;
                }
            }
        } finally {
            executor.shutdown();
        }

        return count;
    }

    // Метод для використання ForkJoin Framework (Work Stealing)
    static class FileCounterTask extends RecursiveTask<Integer> {
        private final File directory;
        private final String extension;

        public FileCounterTask(File directory, String extension) {
            this.directory = directory;
            this.extension = extension;
        }

        @Override
        protected Integer compute() {
            int count = 0;
            File[] files = directory.listFiles();
            if (files == null) return 0;

            // Створюємо підзадачі для підкаталогів
            List<FileCounterTask> subTasks = new ArrayList<>();
            for (File file : files) {
                if (file.isDirectory()) {
                    FileCounterTask subTask = new FileCounterTask(file, extension);
                    subTask.fork(); // Виконуємо задачу асинхронно
                    subTasks.add(subTask);
                } else if (file.getName().endsWith(extension)) {
                    count++;
                }
            }

            // Збираємо результати підзадач
            for (FileCounterTask subTask : subTasks) {
                count += subTask.join();
            }

            return count;
        }
    }
}

