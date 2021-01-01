#include <iostream>
#include <vector>

class Solution {
public:
  int min(int a, int b) {
    return a < b ? a : b;
  }
  
  int minPathSum(std::vector< std::vector<int> > &grid) {
    size_t m = grid.size();
    size_t n = grid[0].size();
    int **cache = new int*[m];
    for (size_t i = 0; i < m; i++) {
      cache[i] = new int[n];
      for (size_t j = 0; j < n; j++) {
        if (i > 0 && j > 0) {
          cache[i][j] = grid[i][j] + min(cache[i][j-1], cache[i-1][j]);
        } else if (i > 0) {
          cache[i][j] = grid[i][j] + cache[i-1][j];
        } else if (j > 0) {
          cache[i][j] = grid[i][j] + cache[i][j-1];
        } else {
          cache[i][j] = grid[i][j];
        }
      }
    }
    
    int path = cache[m-1][n-1];
    for (size_t i = 0; i < m; ++i) {
      delete[] cache[i];
    }
    delete[] cache;

    return path;
  }
};

int main()
{
  int A1[] = {1, 2, 3};
  int A2[] = {4, 5, 6};
  int A3[] = {7, 8, 9};
  int A4[] = {1, 2, 3};

  std::vector< std::vector<int> > grid;
  grid.push_back(std::vector<int>(A1, A1+3));
  grid.push_back(std::vector<int>(A2, A2+3));
  grid.push_back(std::vector<int>(A3, A3+3));
  grid.push_back(std::vector<int>(A4, A4+3));

  Solution s;
  int n;

  n = s.minPathSum(grid);
  std::cout << "minPathSum "<< n << " " << (n == 18 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
