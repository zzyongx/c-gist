#include <iostream>
#include <vector>

class Solution {
public:
  std::vector< std::vector<int> > generateMatrix(int n) {
    std::vector< std::vector<int> > vv;
    for (int i = 0; i < n; ++i) {
      vv.push_back(std::vector<int>(n, 0));
    }

    int r = 0, c = 0;
    bool left = false, right = true, up = false, down = false;
    
    for (int i = 0; i < n * n; ++i) {
      vv[r][c] = i+1;

      if (right) {
        if (c+1 < n && vv[r][c+1] == 0) {
          c++;
        } else {
          right = false;
          down = true;
          r++;
        }
      } else if (down) {
        if (r+1 < n && vv[r+1][c] == 0) {
          r++;
        } else {
          down = false;
          left = true;
          c--;
        }
      } else if (left) {
        if (c-1 >= 0 && vv[r][c-1] == 0) {
          c--;
        } else {
          left = false;
          up = true;
          r--;
        }
      } else {
        if (r-1 >= 0 && vv[r-1][c] == 0) {
          r--;
        } else {
          up = false;
          right = true;
          c++;
        }
      }
    }
    return vv;
  }
};

int main()
{
  Solution s;
  int N = 7;

  std::vector< std::vector<int> > matrix = s.generateMatrix(N);
  for (int i = 0; i < N; ++i) {
    for (int j = 0; j < N; ++j) {
      std::cout << matrix[i][j] << "\t";
    }
    std::cout << "\n";
  }

  return 0;
}
