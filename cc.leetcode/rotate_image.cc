#include <cstdio>
#include <iostream>
#include <vector>

class Solution {
public:
  void rotate(std::vector< std::vector<int> > &matrix) {
    int N = matrix.size();
    for (int i = 0; i < N-1; ++i) {
      int NN = N-1-i;
      for (int j = 0; j < NN-i; ++j) {
        /* [i,i+j] [i+j,NN] [NN,NN-j] [NN-j,i]
         * printf("[%d,%d],[%d,%d],[%d,%d],[%d,%d]\n",
         *     i,i+j, i+j,NN, NN,NN-j, NN-j,i);
         */
        int t = matrix[NN-j][i];
        matrix[NN-j][i]  = matrix[NN][NN-j];
        matrix[NN][NN-j] = matrix[i+j][NN];
        matrix[i+j][NN]  = matrix[i][i+j];
        matrix[i][i+j]   = t;
      }
    }
  }
};

int main()
{
  int A1[] = {1,  2,  3,  4};
  int A2[] = {5,  6,  7,  8};
  int A3[] = {9,  10, 11, 12};
  int A4[] = {13, 14, 15, 16};
  std::vector< std::vector<int> > matrix;
  matrix.push_back(std::vector<int>(A1, A1+4));
  matrix.push_back(std::vector<int>(A2, A2+4));
  matrix.push_back(std::vector<int>(A3, A3+4));
  matrix.push_back(std::vector<int>(A4, A4+4));

  int B1[] = {13, 9,  5, 1};
  int B2[] = {14, 10, 6, 2};
  int B3[] = {15, 11, 7, 3};
  int B4[] = {16, 12, 8, 4};
  std::vector< std::vector<int> > expect;
  expect.push_back(std::vector<int>(B1, B1+4));
  expect.push_back(std::vector<int>(B2, B2+4));
  expect.push_back(std::vector<int>(B3, B3+4));
  expect.push_back(std::vector<int>(B4, B4+4));

  Solution s;
  s.rotate(matrix);

  for (size_t i = 0; i < matrix.size(); i++) {
    for (size_t j = 0; j < matrix[i].size(); j++) {
      if (matrix[i][j] != expect[i][j]) {
        std::cout << "rotate 4x4 [" << i << "," << j << "] " << matrix[i][j] << " FALSE\n";
        return 0;
      }
    }
  }
  
  std::cout << "rotate 4x4 TRUE\n";
  return 0;
}
