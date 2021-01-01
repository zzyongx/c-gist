#include <iostream>
#include <vector>

void printMatrix(std::vector< std::vector<int> > &matrix)
{
    for (size_t i = 0; i < matrix.size(); ++i) {
      for (size_t j = 0; j < matrix[i].size(); ++j) {
        std::cout << matrix[i][j] << " ";
      }
      std::cout << "\n";
    }
}    
  
class Solution {
public:
  void setRowZeroes(std::vector< std::vector<int> > &matrix, int row) {
    for (size_t i = 0; i < matrix[0].size(); ++i) {
      matrix[row][i] = 0;
    }
  }

  void setColZeroes(std::vector< std::vector<int> > &matrix, int col) {
    for (size_t i = 0; i < matrix.size(); ++i) {
      matrix[i][col] = 0;
    }
  }
  
  void setZeroes(std::vector<std::vector<int> > &matrix) {
    bool row0 = false, col0 = false;
    
    for (size_t i = 0; i < matrix.size(); ++i) {
      for (size_t j = 0; j < matrix[i].size(); ++j) {
        if (matrix[i][j] == 0) {
          if (i == 0 || j == 0) {
            if (i == 0) row0 = true;
            if (j == 0) col0 = true;
          } else {
            matrix[i][0] = matrix[0][j] = 0;
          }
        }
      }
    }
    
    for (size_t i = 1; i < matrix.size(); ++i) {
      if (matrix[i][0] == 0) setRowZeroes(matrix, i);
    }
    
    for (size_t i = 1; i < matrix[0].size(); i++) {
      if (matrix[0][i] == 0) setColZeroes(matrix, i);
    }

    if (row0) setRowZeroes(matrix, 0);
    if (col0) setColZeroes(matrix, 0);
  }
};

int main()
{
  int A[][4] = {
    {1, 0, 2, 3},
    {4, 5, 6, 2},
    {9, 7, 0, 4}
  };
  
  std::vector< std::vector<int> > matrix;
  matrix.push_back(std::vector<int>(A[0], A[0]+4));
  matrix.push_back(std::vector<int>(A[1], A[1]+4));
  matrix.push_back(std::vector<int>(A[2], A[2]+4));
  
  int B[][4] = {
    {0, 0, 0, 0},
    {4, 0, 0, 2},
    {0, 0, 0, 0}
  };

  Solution s;
  s.setZeroes(matrix);

  for (size_t i = 0; i < matrix.size(); ++i) {
    for (size_t j = 0; j < matrix[i].size(); ++j) {
      if (B[i][j] != matrix[i][j]) {
        std::cout << "setzeroes FALSE\n";
        return 0;
      }
    }
  }

  std::cout << "setzeros TRUE\n";
  return 0;  
}
