import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Myers {

    static String diff(int[] a, int[] b) {
        int n = a.length;
        int m = b.length;
        int max = n + m;

        int[] v = new int[2 * max + 2];

        List<int[]> trace = new ArrayList<>();

        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int x;

                if (k == -d || (k != d && v[max + k - 1] < v[max + k + 1])) {
                    x = v[max + k + 1];
                } else {
                    x = v[max + k - 1] + 1;
                }

                int y = x - k;

                while (x < n && y < m && a[x] == b[y]) {
                    x++;
                    y++;
                }

                v[max + k] = x;

                if (x >= n && y >= m) {
                    return backtrack(trace, n, m);
                }
            }

            trace.add(Arrays.copyOfRange(v, max - d, max + d + 1));
        }

        throw new IllegalStateException("unreachable: d = n + m always reaches the end");
    }

    private static String backtrack(List<int[]> trace, int n, int m) {
        StringBuilder script = new StringBuilder();
        int x = n;
        int y = m;

        for (int d = trace.size(); d > 0; d--) {
            int[] prev = trace.get(d - 1);
            int off = d - 1;
            int k = x - y;

            boolean down = k == -d || (k != d && prev[off + k - 1] < prev[off + k + 1]);

            int prevK = down ? k + 1 : k - 1;
            int prevX = prev[off + prevK];
            int prevY = prevX - prevK;

            while (x > prevX && y > prevY) {
                script.append(' ');
                x--;
                y--;
            }

            script.append(down ? '+' : '-');
            x = prevX;
            y = prevY;
        }

        while (x > 0) {
            script.append(' ');
            x--;
        }

        return script.reverse().toString();
    }
}