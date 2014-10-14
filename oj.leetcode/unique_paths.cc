#include <iostream>

class Solution {
public:
  /* f(m, n) = f(m-1, n) + f(m, n-1) */
  int uniquePaths(int m, int n) {
    /* if (m == 1 || n == 1) return 1;
     * return uniquePaths(m-1, n) + uniquePaths(m, n-1);
     */
    int **cache = new int*[m];
    for (int i = 0; i < m; ++i) {
      cache[i] = new int[n];
    }
    
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        if (i == 0 || j == 0) {
          cache[i][j] = 1;
        } else {
          cache[i][j] = cache[i][j-1] + cache[i-1][j];
        }
      }
    }
    
    int num = cache[m-1][n-1];
    for (int i = 0; i < m; ++i) {
      delete[] cache[i];
    }
    delete[] cache;

    return num;
  }
};

int main()
{
  Solution s;
  int n;

  n = s.uniquePaths(2, 1);
  std::cout << "2 X 1 = 1 " << (n == 1 ? "TRUE" : "FALSE") << "\n";

  n = s.uniquePaths(3, 2);
  std::cout << "3 X 2 = 3 " << (n == 3 ? "TRUE" : "FALSE") << "\n";

  n = s.uniquePaths(3, 3);
  std::cout << "3 X 3 = 6 " << (n == 6 ? "TRUE" : "FALSE") << "\n";

  n = s.uniquePaths(23, 12);
  std::cout << "23 X 12 = 193536720 " << (n == 193536720 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
