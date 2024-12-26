import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class Ex1{

    // Метод для генерації випадкового масиву
    public static int[] generateArray(int size, int minValue, int maxValue) {
        Random random = new Random();
        int[] array = new int[size];
        for (int i = 0; i < size; i++) {
            array[i] = random.nextInt(maxValue - minValue + 1) + minValue;
        }
        return array;
    }

    // Підхід Work Stealing (Fork/Join Framework)
    static class PairSumTask extends RecursiveTask<Long> {
        private final int[] array;
        private final int start;
        private final int end;

        public PairSumTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            if (end - start <= 10) { // Менше розміру підзадачі
                long sum = 0;
                for (int i = start; i < end - 1; i++) {
                    sum += array[i] + array[i + 1];
                }
                try {
                    sum += array[end - 1] + array[end];
                }catch (ArrayIndexOutOfBoundsException ignored){}
                return sum;
            } else {
                int mid = (start + end) / 2;
                //  Поділ на підзадачі
                PairSumTask leftTask = new PairSumTask(array, start, mid);
                PairSumTask rightTask = new PairSumTask(array, mid, end);
                leftTask.fork();
                rightTask.fork();
                long rightResult = rightTask.join();
                long leftResult = leftTask.join();
                return leftResult + rightResult;
            }
        }
    }

    // Підхід Work Dealing (ThreadPool)
    public static long parallelPairSum(int[] array, int threads) throws InterruptedException, ExecutionException {

        class ArraySum implements Callable<Integer>{
            private final int[] array;
            private final int start;
            private final int end;

            public ArraySum(int[] array, int start, int end){
                this.array = array;
                this.start = start;
                this.end = end;
            }
            public Integer call(){
                int sum = 0;
                for (int i = start; i < end - 1; i++) {
                    sum += array[i] + array[i + 1];
                }
                try {
                    sum += array[end - 1] + array[end];
                }catch (ArrayIndexOutOfBoundsException ignored){}

                return sum;
            }

        }
        // Створення ExecutorService
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        // Створення підмасивів кількість яких дорівнює кількості потоків
        int step = Math.max(1, array.length / threads);

        // створення списку Future
        List<Future<Integer>> futures = new ArrayList<>();

        // розбиття масиву на підмасиви та створення Future для обробки потоку
        for (int i = 0; i < threads; i++) {
            int start = i * step;
            int end = Math.min(array.length, (i + 1) * step);
            Callable<Integer> task = new ArraySum(array, start, end);
            futures.add(executor.submit(task));
        }

        // Збір результатів
        int finalResult = 0;
        for (Future<Integer> future : futures) {
            try {
                if (!future.isCancelled()) {
                    finalResult += future.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        }

        executor.shutdown();
        return finalResult;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int size = 100000; // Розмір масиву
        int minValue = 1;      // Мінімальне значення елементів
        int maxValue = 10;    // Максимальне значення елементів
        int threads = 4;       // Кількість потоків

        int[] array = generateArray(size, minValue, maxValue);
        //int[] a = {1, 2, 3, 4, 5, 6, 7, 8, 9 , 10, 11};
        System.out.println("Масив згенеровано.");
        int sum = 0;
        for (int i = 0; i < array.length - 1; i++) {
            sum += array[i] + array[i+1];
        }
        System.out.println("Expected result: " + sum);

        // Work Stealing
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        long startTime = System.currentTimeMillis();
        long resultWorkStealing = forkJoinPool.invoke(new PairSumTask(array, 0, array.length));
        long endTime = System.currentTimeMillis();
        System.out.println("Work Stealing Result: " + resultWorkStealing);
        System.out.println("Work Stealing Time: " + (endTime - startTime) + " ms");

        // Work Dealing
        startTime = System.currentTimeMillis();
        long resultWorkDealing = parallelPairSum(array, threads);
        endTime = System.currentTimeMillis();
        System.out.println("Work Dealing Result: " + resultWorkDealing);
        System.out.println("Work Dealing Time: " + (endTime - startTime) + " ms");
    }
}
